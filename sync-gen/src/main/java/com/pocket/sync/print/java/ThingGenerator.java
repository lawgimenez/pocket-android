package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.ContextualReference;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.Interface;
import com.pocket.sync.type.ReferenceType;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.path.Flavor;
import com.pocket.sync.type.path.Path;
import com.pocket.sync.type.path.Reference;
import com.pocket.sync.util.FigmentUtilsKt;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;


/**
 * Creates a class for an{@link Thing}.
 */
public class ThingGenerator extends ClassGenerator {

	private final ClassName idBuilderClassName;
	private final ClassName builderClassName;
	private final ClassName mutableClassName;
	private final ClassName declaredClassName;
	private final ClassName declaredMutableClassName;
	protected final Thing model;
	private final Collection<Field> activeFields;
	private final Collection<Field> activeIds = new ArrayList<>();
	private final Collection<Field> activeStates = new ArrayList<>();

	public ThingGenerator(Thing model, Figments figments, Config config) {
		this(model, figments, config, config.thing(model));
	}

	public ThingGenerator(Thing model, Figments figments, Config config, ClassName className) {
		super(className, config, figments);
		this.model = model;
		idBuilderClassName = GenUtil.createInnerClassName(className, "IdBuilder");
		builderClassName = builderClass(config, model);
		mutableClassName = GenUtil.createInnerClassName(className, "Mutable");
		declaredClassName = GenUtil.createInnerClassName(className, "Declared");
		declaredMutableClassName = GenUtil.createInnerClassName(className, "DeclaredMutable");
		typeSpec.addModifiers(Modifier.PUBLIC, Modifier.FINAL)
				.addJavadoc(JavaDescriptionPrinter.print(model.getDescription(), config));
		if (model.getDeprecated()) {
			typeSpec.addAnnotation(Deprecated.class);
		}
		
		activeFields = config.mode.getActiveFields(model);
		for (Field field : activeFields) {
			if (field.getIdentifying()) {
				activeIds.add(field);
			} else {
				activeStates.add(field);
			}
		}
	}

	public ThingGenerator setup() {
		// These are broken up to keep their generation logic separate
		// and easier to read but most of them require each other to be in the class
		// so they aren't meant to be optional. For example, toString uses toJson().
		GraphQlGeneratorKt.addGraphQlSupport(model, typeSpec, config);
		SyncableGenerator.addAuthType(typeSpec, model.getAuth().getAuth(), config, figments);
		SyncableGenerator.addFields(model, activeFields, config, typeSpec);
		SyncableGenerator.addConstructor(activeFields, builderClassName, declaredClassName, typeSpec);
		SyncableGenerator.addDeclared(activeFields, declaredClassName, declaredMutableClassName, typeSpec);
		SyncableGenerator.addFromJson(activeFields, className, builderClassName, config, typeSpec);
		SyncableGenerator.addFromParser(activeFields, className, builderClassName, config, typeSpec);
		SyncableGenerator.addToJson(model, activeFields, config, typeSpec, null);
		SyncableGenerator.addToString("THING_TYPE", typeSpec);
		SyncableGenerator.addCreator(typeSpec, className);
		OpenTypes.setupForSyncable(typeSpec, config, model);
		typeSpec.addField(
				FieldSpec.builder(ParameterizedTypeName.get(ClassNames.STREAMING_THING_CREATOR, className), "STREAMING_JSON_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::from", className)
						.build());
		addModelInterface();
		addEquals();

		addBuilders();
		mutable();
		idkey();
		reactions();
		subthings();
		flat();
		with();
		derive();
		redact();
		CompressGenerator.setup(typeSpec, config, className, model, activeFields);
		return this;
	}
	
	private void idkey() {
		typeSpec.addField(FieldSpec.builder(ClassNames.STRING, "_idkey", Modifier.PRIVATE)
				.addJavadoc("Lazy init'd and cached during idkey()")
				.build());

		typeSpec.addMethod(MethodSpec.methodBuilder("idkey")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addStatement("if (_idkey != null) return _idkey")
				.addStatement("$1T writer = new $1T()", ClassNames.BYTE_WRITER)
				.addStatement("writer.writeString(THING_TYPE)")
				.addStatement("writer.writeString(identity().toJson(NO_ALIASES, $T.DANGEROUS).toString())", ClassNames.INCLUDE) // Note: This relies on toJson having the fields ordered alphabetically.
				.addStatement("_idkey = writer.sha256()")
				.addStatement("return _idkey")
				.returns(String.class)
				.build());
	}
	
	private void reactions() {
		MethodSpec.Builder method = MethodSpec.methodBuilder("reactions")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addParameter(ClassNames.THING, "was_t")
				.addParameter(ClassNames.THING, "is_t")
				.addParameter(ClassNames.RICH_DIFF, "diff")
				.addParameter(ClassNames.REACTIONS, "output");
		
		/*
			For fields that are reactive triggers this will produce logic like:
				
				if (is.declared.field && (was == null || !was.declared.field || ObjectUtils.notEqual(was.field, is.field))) {
					// changed for reactive
				}
			
			For fields that are derived fields, it will produce logic like:
			
				if (!is.declared.field) {
					// derive for the first time
				}
		 */
		
		CodeBlock.Builder code = CodeBlock.builder();
		
		for (Field field : activeFields) {
			if (!field.getDerives().isDerived()) continue;
			code.beginControlFlow("if (!is.declared.$N)", GenUtil.toValidFieldName(field.getName()));
			code.addStatement("output.thing(this, $S)", field.getName());
			code.endControlFlow();
		}

		// Find all of the reactives in any figment that reference this thing.
		List<Reference> triggers = new ArrayList<>(FigmentUtilsKt.triggers(model));
		// Ordering has no functional difference here, but we convert to a sorted list to ensure
		// consistent code generation output that won't be randomly different between builds and machines.
		// The sort order is again not functional, just something that will be consistent.
		Collections.sort(triggers, (r1, r2) -> r1.toString().compareTo(r2.toString())); // Sorts on the reference string (Type.path)
		for (Reference t : triggers) {
			// Find all fields that react to this specific trigger.
			List<Field> reactives = new ArrayList<>(FigmentUtilsKt.reactiveTo(t));

			// Only care about concrete types that might react to this.
			reactives.removeIf(field -> field.getContext().getCurrent() instanceof Interface);

			// Sort to avoid random output order
			Collections.sort(reactives, (r1, r2) -> r1.toString().compareTo(r2.toString())); // Sorts on the reactive path

			switch (t.getReference().getFlavor()) {
				case TYPE:
					for (Field f : reactives) {
						code.addStatement("output.type($S, $S)", f.getContext().getCurrent().getName(), f.getName());
					}
					break;
				case TYPE_FIELD:
				case SELF_FIELD:
					code.add(GenUtil.ifPathValueChanged(model, "was", "is", "diff", t, GenUtil.toValidFieldName(t.getPath().getSyncable().toString()) + "__changed", config));
					for (Field f : reactives) {
						if (t.getReference().getFlavor() == Flavor.TYPE_FIELD) {
							code.addStatement("output.type($S, $S)", f.getContext().getCurrent().getName(), f.getName());
						} else {
							code.addStatement("output.thing(this, $S)", f.getName());
						}
					}
					code.endControlFlow();
					break;
				case SELF:
					for (Field f : reactives) {
						Reference ref = new Reference(f.getContext().getCurrent(), new Path().field(f.getName()));
						code.add(GenUtil.ifPathValueNotChanged(model, "was", "is", "diff", ref, "", config));
						code.addStatement("output.thing(this, $S)", f.getName());
						code.endControlFlow();
					}
					break;
			}
		}
		
		CodeBlock block = code.build();
		if (!block.isEmpty()) {
			method.addStatement("$T was = ($T) was_t", className, className);
			method.addStatement("$T is = ($T) is_t", className, className);
			method.addCode(block);
		}
		
		typeSpec.addMethod(method.build());
	}
	
	private void addBuilders() {
		SyncableGenerator.addBuilder(activeFields, config, className, builderClassName, declaredClassName, declaredMutableClassName, ClassNames.THING_BUILDER, typeSpec);
		
		if (!model.isIdentifiable()) return;
		
		TypeSpec.Builder idBuilder = TypeSpec.classBuilder(idBuilderClassName)
					.addSuperinterface(ParameterizedTypeName.get(ClassNames.THING_BUILDER, className))
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addMethod(MethodSpec.constructorBuilder()
							.addModifiers(Modifier.PUBLIC)
							.build())
					.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PUBLIC)
						.addParameter(className, "src")
						.addStatement("set(src)")
						.build())
					.addField(FieldSpec.builder(builderClassName, "builder", Modifier.PRIVATE, Modifier.FINAL)
							.initializer("new $T()", builderClassName)
							.build());
		
		MethodSpec.Builder idCopier = MethodSpec.methodBuilder("set")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(idBuilderClassName)
				.addParameter(className, "src");
		
		for (Field field : activeIds) {
			String name = GenUtil.toValidFieldName(field.getName());
			// When copying over from another builder, we can do a direct copy rather than going through the field() method which does extra work to ensure immutability
			idCopier.beginControlFlow("if (src.declared.$N)", name);
			idCopier.addStatement("builder.declared.$N = true", name);
			idCopier.addStatement("builder.$N = src.$N", name, name);
			idCopier.endControlFlow();
		}
		idCopier.addStatement("return this");
		idBuilder.addMethod(idCopier.build());
		
		for (Field field : activeIds) {
			TypeName fieldType = GenUtil.toTypeName(field, config);
			String fieldname = GenUtil.toValidFieldName(field.getName());
			idBuilder.addMethod(MethodSpec.methodBuilder(fieldname)
					.addModifiers(Modifier.PUBLIC)
					.addParameter(fieldType, "value")
					.addStatement("builder.$N(value)", fieldname)
					.addStatement("return this")
					.returns(idBuilderClassName)
					.addJavadoc(SyncableGenerator.fieldJavaDoc(field, config))
					.build());
		}
		
		CodeBlock.Builder selfDerive = CodeBlock.builder();
		List<Field> selfDerivable = new ArrayList<>();
		for (Field field : activeIds) {
			if (field.getDerives().getFirstAvailable() == null) continue;
			selfDerivable.add(field);
		}
		FigmentUtilsKt.sortByDependencies(selfDerivable);
		for (Field field : selfDerivable) {
			String fieldname = GenUtil.toValidFieldName(field);
			selfDerive.addStatement("Derive.$N(builder)", fieldname);
		}
		idBuilder.addMethod(MethodSpec.methodBuilder("build")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(className)
				.addCode(selfDerive.build())
				.addStatement("return new $T(builder, new $T(builder.declared))", className, declaredClassName)
				.build());
		
		typeSpec.addType(idBuilder.build());
	}
	
	public void mutable() {
		/*
			The MutableSpace API takes a bit of wrapping your head around.
			It is one of those cases where high performance has the cost of more complexity.
			Before making significant changes here, highly recommend going and reading/refreshing yourself on
			the java docs for the sync engine classes: MutableSpace, MutableThing and Mutables,
		 */

		TypeSpec.Builder impl = TypeSpec.classBuilder(mutableClassName)
				.addSuperinterface(ParameterizedTypeName.get(ClassNames.MUTABLE, className))
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addField(FieldSpec.builder(builderClassName, "builder", Modifier.PRIVATE, Modifier.FINAL)
						.initializer("new $T()", builderClassName)
						.build())
				.addField(className, "_identity", Modifier.PRIVATE, Modifier.FINAL)
				.addField(className, "_built", Modifier.PRIVATE)
				.addField(className, "_previous", Modifier.PRIVATE)
				.addField(ClassNames.MUTABLE, "_root", Modifier.PRIVATE);
		
		CodeBlock.Builder copier = CodeBlock.builder();
		CodeBlock.Builder imprint = CodeBlock.builder();
		CodeBlock.Builder build = CodeBlock.builder();
		for (Field field : activeFields) {
			String name = GenUtil.toValidFieldName(field.getName());

			// If there is any chance the field contains identifiable things, need to have it represented by a MutableThing
			boolean requiresMutable = WhenType.is(field.getType(), boolean.class)
					.collection(c -> WhenType.is(c.getInner(), boolean.class)
						.identifiable(t -> true)
						.containsIdentifiable(t -> true)
						.potentiallyIdentifiable(t -> true)
						.containsPotentiallyIdentifiable(t -> true)
						.otherwise(t -> false))
					.identifiable(t -> true)
					.containsIdentifiable(t -> true)
					.potentiallyIdentifiable(t -> true)
					.containsPotentiallyIdentifiable(t -> true)
					.otherwise(t -> false);

			if (requiresMutable) {
				WhenType.is(field.getType())
						.map(map -> {
							ParameterizedTypeName mutable = ParameterizedTypeName.get(ClassNames.MUTABLE, GenUtil.toTypeName(map.getInner(), config));
							impl.addField(ParameterizedTypeName.get(ClassName.get(Map.class), ClassNames.STRING, mutable),
									name, Modifier.PRIVATE);
						})
						.list(map -> {
							ParameterizedTypeName mutable = ParameterizedTypeName.get(ClassNames.MUTABLE, GenUtil.toTypeName(map.getInner(), config));
							impl.addField(ParameterizedTypeName.get(ClassName.get(List.class), mutable),
									name, Modifier.PRIVATE);
						})
						.open(open -> {
							ParameterizedTypeName mutable = ParameterizedTypeName.get(ClassNames.MUTABLE, GenUtil.toTypeName(open, config));
							impl.addField(mutable, name, Modifier.PRIVATE);
						})
						.thing(thing -> {
							ParameterizedTypeName mutableThing = ParameterizedTypeName.get(ClassNames.MUTABLE, config.thing(thing));
							impl.addField(mutableThing,
									name, Modifier.PRIVATE);
						})
						.otherwiseFail();
			}

			// Constructor/copy
			copier.beginControlFlow("if (src.declared.$N) ", name);
			copier.addStatement("builder.declared.$N = true", name);
			if (requiresMutable) {
				copier.addStatement("$N = mutables.imprint(src.$N, _root)", name, name);
				copier.addStatement("mutables.link(this, $N)", name);
			} else {
				copier.addStatement("builder.$N = src.$N", name, name);
			}
			copier.endControlFlow();
			
			// Imprint
			imprint.beginControlFlow("if (value.declared.$N)", name);
			imprint.addStatement("builder.declared.$N = true", name);
			if (requiresMutable) {
				imprint.addStatement("changed = changed || $T.changed($N, value.$N)", ClassNames.MUTABLES, name, name);
				imprint.addStatement("if (changed) mutables.unlink(this, $N)", name);   // NOTE this unlink and linking setup is probably doing more work than needed in some cases, but trying to decide if the links change might be an equal amount of work
				imprint.addStatement("$N = mutables.imprint(value.$N, _root)", name, name);
				imprint.addStatement("if (changed) mutables.link(this, $N)", name);
			} else {
				imprint.addStatement("changed = changed || $T.changed(builder.$N, value.$N)", ClassNames.MUTABLES, name, name);
				imprint.addStatement("builder.$N = value.$N", name, name);
			}
			imprint.endControlFlow();
			
			// Build
			if (requiresMutable) {
				build.addStatement("builder.$N = $T.build($N)", name, ClassNames.MUTABLES, name);
			}
		}
		
		MethodSpec.Builder references = MethodSpec.methodBuilder("references")
				.addModifiers(Modifier.PUBLIC)
				.returns(ParameterizedTypeName.get(ClassName.get(Collection.class), WildcardTypeName.subtypeOf(ClassNames.MUTABLE)))
				.addAnnotation(Override.class)
				.addStatement("$T<$T> _out = new $T<>()", List.class, ClassNames.MUTABLE, ArrayList.class);
		for (Field f : activeFields) {
			String fieldname = GenUtil.toValidFieldName(f.getName());
			WhenType.is(f.getType())
					.list(list -> WhenType.is(list.getInner())
							.identifiable(thing -> references.addStatement("if ($1N != null) _out.addAll($1N)", fieldname))
							.potentiallyIdentifiable(t -> {
								references.beginControlFlow("if ($1N != null)", fieldname);
								references.beginControlFlow("for ($2T _e : $1N)", fieldname, ClassNames.MUTABLE);
								references.addStatement("if (_e == null) continue");
								references.beginControlFlow("if (_e.isIdentifiable())");
								references.addStatement("_out.add(_e)");
								if (WhenType.containsPotentiallyIdentifiable(t)) {
									references.nextControlFlow("else");
									references.addStatement("_out.addAll(_e.references())");
								}
								references.endControlFlow(); // if
								references.endControlFlow(); // for
								references.endControlFlow(); // if != null
							})
							.containsPotentiallyIdentifiable(t -> references.addStatement("if ($1N != null) for ($2T _e : $1N) if (_e != null) _out.addAll(_e.references())", fieldname, ClassNames.MUTABLE))
							.otherwiseIgnore())
					.map(map -> WhenType.is(map.getInner())
							.identifiable(thing -> references.addStatement("if ($1N != null) _out.addAll($1N.values())", fieldname))
							.potentiallyIdentifiable(t -> {
								references.beginControlFlow("if ($1N != null)", fieldname);
								references.beginControlFlow("for ($2T _e : $1N.values())", fieldname, ClassNames.MUTABLE);
								references.addStatement("if (_e == null) continue");
								references.beginControlFlow("if (_e.isIdentifiable())");
								references.addStatement("_out.add(_e)");
								if (WhenType.containsPotentiallyIdentifiable(t)) {
									references.nextControlFlow("else");
									references.addStatement("_out.addAll(_e.references())");
								}
								references.endControlFlow(); // if
								references.endControlFlow(); // for
								references.endControlFlow(); // if != null
							})
							.containsPotentiallyIdentifiable(t -> references.addStatement("if ($1N != null) for ($2T _e : $1N.values()) if (_e != null) _out.addAll(_e.references())", fieldname, ClassNames.MUTABLE))
							.otherwiseIgnore())
					.identifiable(thing -> references.addStatement("if ($1N != null) _out.add($1N)", fieldname))
					.potentiallyIdentifiable(t -> {
						if (WhenType.containsPotentiallyIdentifiable(t)) {
							references.beginControlFlow("if ($1N != null)", fieldname);
							references.beginControlFlow("if ($1N.isIdentifiable())", fieldname);
							references.addStatement("_out.add($1N)", fieldname);
							references.nextControlFlow("else");
							references.addStatement("_out.addAll($1N.references())", fieldname);
							references.endControlFlow();
							references.endControlFlow();
						} else {
							references.addStatement("if ($1N != null && $1N.isIdentifiable()) _out.add($1N)", fieldname);
						}
					})
					.containsPotentiallyIdentifiable(thing -> references.addStatement("if ($1N != null) _out.addAll($1N.references())", fieldname))
					.otherwiseIgnore();
		}
		references.addStatement("return _out");
		impl.addMethod(references.build());
		
		MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PRIVATE)
				.addParameter(className, "src")
				.addParameter(ClassNames.MUTABLES, "mutables")
				.addStatement("_identity = src.identity()");
				
		if (model.isIdentifiable()) {
			constructor.addStatement("_root = this");
		} else {
			constructor
					.addParameter(ClassNames.MUTABLE, "root")
					.addStatement("_root = root");
		}

		// Handle MutableThing.build()'s caching rule.
		CodeBlock.Builder buildCache = CodeBlock.builder();
		if (model.isIdentifiable() || !WhenType.containsPotentiallyIdentifiable(model)) {
			buildCache.addStatement("if (_built != null) return _built");
		}
		
		typeSpec.addType(impl
				.addMethod(constructor
						.addCode(copier.build())
						.build())
				.addMethod(MethodSpec.methodBuilder("root")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(ClassNames.MUTABLE)
						.addStatement("return _root")
						.build())
				.addMethod(MethodSpec.methodBuilder("imprint")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.addParameter(className, "value")
						.addParameter(ClassNames.MUTABLES, "mutables")
						.addStatement("boolean changed = false")
						.addCode(imprint.build())
						.addStatement("if (changed) mutables.flagChanged(this)")
						.build())
				.addMethod(MethodSpec.methodBuilder("build")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(className)
						.addCode(buildCache.build())
						.addCode(build.build())
						.addStatement("_built = builder.build()")
						.addStatement("return _built")
						.build())
				.addMethod(MethodSpec.methodBuilder("identity")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(className)
						.addStatement("return _identity")
						.build())
				.addMethod(MethodSpec.methodBuilder("previous")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(className)
						.addStatement("$T v = _previous", className)
						.addStatement("_previous = null")
						.addStatement("return v")
						.build())
				.addMethod(MethodSpec.methodBuilder("invalidate")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.addStatement("if (_built != null) _previous = _built")
						.addStatement("_built = null")
						.build())
				.addMethod(MethodSpec.methodBuilder("equals")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.addParameter(Object.class, "o")
						.returns(boolean.class)
						.addStatement("if (this == o) return true")
						.addStatement("if (o == null || getClass() != o.getClass()) return false")
						.addStatement("return _identity.equals((($T)o)._identity)", mutableClassName)
						.build())
				.addMethod(MethodSpec.methodBuilder("hashCode")
						.addModifiers(Modifier.PUBLIC)
						.addAnnotation(Override.class)
						.returns(int.class)
						.addStatement("return _identity.hashCode()")
						.build())
				.build());


		typeSpec.addMethod(MethodSpec.methodBuilder("mutable")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addParameter(ClassNames.MUTABLES, "mutables")
				.addParameter(ClassNames.MUTABLE, "root")
				.returns(mutableClassName)
				.addStatement(model.isIdentifiable() ? "return new $T(this, mutables)" : "return new $T(this, mutables, root)", mutableClassName)
				.build());
	}
	
	private void addEquals() {
		typeSpec.addMethod(MethodSpec.methodBuilder("hashCode")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(int.class)
				.addStatement("return hashCode($T.IDENTITY)", ClassNames.EQUALITY)
				.build());

		String hashcodeResultVar = "_result";
		MethodSpec.Builder hashCode = MethodSpec.methodBuilder("hashCode")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addParameter(ClassNames.EQUALITY, "e")
				.returns(int.class)
				.addStatement("int $N = 0", hashcodeResultVar)
				.addStatement("if (e == null) e = $T.IDENTITY", ClassNames.EQUALITY);
		if (!model.isIdentifiable()) {
			hashCode.addStatement("if (e == $T.IDENTITY) e = $T.STATE ", ClassNames.EQUALITY, ClassNames.EQUALITY); // Non identifible things should be compared as stateful objects instead, they have no identity and can only compare state.
		}
		for (Field id : activeIds) {
			SyncableGenerator.addHashCodeFor(hashCode, id, hashcodeResultVar);
		}
		if (model.isIdentifiable()) {
			hashCode.addStatement("if (e == $T.IDENTITY) return $N", ClassNames.EQUALITY, hashcodeResultVar);
		}
		for (Field state : activeStates) {
			SyncableGenerator.addHashCodeFor(hashCode, state, hashcodeResultVar);
		}
		hashCode.addStatement("return $N", hashcodeResultVar);
		typeSpec.addMethod(hashCode.build());

		typeSpec.addMethod(MethodSpec.methodBuilder("equals")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(boolean.class)
				.addParameter(Object.class, "o")
				.addStatement("return equals($T.IDENTITY, o)", ClassNames.EQUALITY)
				.build());
		
		MethodSpec.Builder equals = MethodSpec.methodBuilder("equals")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(boolean.class)
				.addParameter(ClassNames.EQUALITY, "e")
				.addParameter(Object.class, "o")
				.addStatement("if (e == null) e = $T.IDENTITY", ClassNames.EQUALITY);
		equals.addStatement("if (this == o) return true")
				.addStatement("if (o == null || getClass() != o.getClass()) return false")
				.addStatement("$T that = ($T) o", className, className);
		
		equals.beginControlFlow("if (e == $T.STATE_DECLARED)", ClassNames.EQUALITY);
		for (Field field : activeFields) {
			equals.addCode("if (that.declared.$1N && declared.$1N) ", GenUtil.toValidFieldName(field.getName()));
			SyncableGenerator.addEqualsFor(equals, field);
		}
		equals.addStatement("return true");
		equals.endControlFlow();
		
		for (Field field : activeIds) {
			SyncableGenerator.addEqualsFor(equals, field);
		}
		
		if (model.isIdentifiable()) {
			equals.addStatement("if (e == $T.IDENTITY) return true", ClassNames.EQUALITY);
		}
		
		for (Field field : activeStates) {
			SyncableGenerator.addEqualsFor(equals, field);
		}
		
		equals.addStatement("return true");
		
		typeSpec.addMethod(equals.build());
	}
	
	private void addModelInterface() {
		typeSpec.addSuperinterface(ClassNames.THING);

		typeSpec.addField(FieldSpec.builder(ClassName.get(String.class), "THING_TYPE", Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
				.initializer("$S", model.getName())
				.build());

		typeSpec.addMethod(MethodSpec.methodBuilder("type")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(String.class)
				.addStatement("return THING_TYPE")
				.build());
		
		SyncableGenerator.addRemoteInfo(model, activeFields, typeSpec, config);

		typeSpec.addMethod(MethodSpec.methodBuilder("getCreator")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.SYNCABLE_CREATOR)
				.addStatement("return JSON_CREATOR")
				.build());
		
		typeSpec.addMethod(MethodSpec.methodBuilder("getStreamingCreator")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.STREAMING_THING_CREATOR)
				.addStatement("return STREAMING_JSON_CREATOR")
				.build());
		
		typeSpec.addMethod(MethodSpec.methodBuilder("getByteCreator")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.BYTE_CREATOR)
				.addStatement("return BYTE_CREATOR")
				.build());
		
		typeSpec.addField(className, "_identity", Modifier.PRIVATE);
		MethodSpec.Builder identity = MethodSpec.methodBuilder("identity")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.addStatement("if (_identity != null) return _identity")
				.returns(className);
		if (model.isIdentifiable()) {
			if (WhenType.potentiallyHasNestedIdentity(model)) {
				// Needs to reduce its referenced things to their identities as well.
				/*
				HasIdentityWithThing identity = new IdBuilder(this).build();
				Set<Thing> subs = FlatUtils.references(this);
				for (Thing sub : subs) {
					HasIdentityWithThing c = identity.with(sub.identity());
					identity = c != null ? c : identity;
				}
				return identity;
				 */
				identity.addStatement("$T identity = new $T(this).build()", className, idBuilderClassName);
				identity.addStatement("$T<Thing> subs = $T.references(this)", ClassNames.SET, ClassNames.FLAT_UTILS);
				identity.beginControlFlow("for (Thing sub : subs)");
				identity.addStatement("$T c = identity.with(sub::equals, sub.identity())", className);
				identity.addStatement("identity = c != null ? c : identity");
				identity.endControlFlow();
				identity.addStatement("_identity = identity");
				identity.addStatement("_identity._identity = _identity"); // Copy it over to the new instance so it doesn't have to recalculate it
				identity.addStatement("return identity");
			} else {
				identity.addStatement("_identity = new $T(this).build()", idBuilderClassName);
				identity.addStatement("_identity._identity = _identity"); // Copy it over to the new instance so it doesn't have to recalculate it
				identity.addStatement("return _identity");
			}
		} else {
			identity.addStatement("return this");
		}
		typeSpec.addMethod(identity.build());
		
		
		typeSpec.addMethod(MethodSpec.methodBuilder("isIdentifiable")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(boolean.class)
				.addStatement(model.isIdentifiable() ? "return true" : "return false")
				.build());

		typeSpec.addMethod(MethodSpec.methodBuilder("rootValue")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(String.class)
				.addStatement("return " + (model.rootValue() != null ? "\"" + model.rootValue().getName() + "\"" : "null"))
				.build());
		
		SyncableGenerator.addToMap(activeFields, config, typeSpec, null);
	}
	
	
	private void subthings() {
		MethodSpec.Builder method = MethodSpec.methodBuilder("subthings")
				.addParameter(ClassNames.FLATTENER, "out")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class);
		
		for (Field f : activeFields) {
			String fieldname = GenUtil.toValidFieldName(f.getName());
			WhenType.is(f.getType())
					.collection(collection -> WhenType.is(collection.getInner())
							.open(o -> WhenType.is(o)
								.potentiallyIdentifiable(t -> {
									boolean checkChildren = WhenType.containsPotentiallyIdentifiable(t);
									method.beginControlFlow("if ($N != null)", fieldname);
									method.beginControlFlow("for ($T _e : $N)", ClassNames.THING, fieldname);
									method.addStatement("if (_e == null) continue");
									method.beginControlFlow("if (_e.isIdentifiable())");
									method.addStatement("out.add(_e, $L)", checkChildren ? "true" : "false");
									if (checkChildren) {
										method.nextControlFlow("else");
										method.addStatement("_e.subthings(out)");
									}
									method.endControlFlow();
									method.endControlFlow();
									method.endControlFlow();
								})
								.containsPotentiallyIdentifiable(t -> method.addStatement("if ($1N != null) out.searchAll($1N)", fieldname))
								.otherwiseIgnore())
							.identifiable(t -> method.addStatement("if ($N != null) out.addAll($N, $L)", fieldname, fieldname, WhenType.containsPotentiallyIdentifiable(t) ? "true" : "false"))
							.containsIdentifiable(t -> method.addStatement("if ($N != null) out.searchAll($N)", fieldname, fieldname))
							.otherwiseIgnore())
					.open(o -> WhenType.is(o)
							.potentiallyIdentifiable(t -> method.addStatement("if ($1N != null && $1N.isIdentifiable()) out.add($1N, $2L)", fieldname, WhenType.containsPotentiallyIdentifiable(t) ? "true" : "false"))
							.containsPotentiallyIdentifiable(t -> method.addStatement("if ($1N != null) $1N.subthings(out)", fieldname))
							.otherwiseIgnore())
					.identifiable(t -> method.addStatement("if ($N != null) out.add($N, $L)", fieldname, fieldname, WhenType.containsPotentiallyIdentifiable(t) ? "true" : "false"))
					.containsIdentifiable(t -> method.addStatement("if ($N != null) $N.subthings(out)", fieldname, fieldname))
					.otherwiseIgnore();

		}
		typeSpec.addMethod(method.build());
	}
	
	private void flat() {
		final MethodSpec.Builder method = MethodSpec.methodBuilder("flat")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.returns(className);

		if (!WhenType.containsPotentiallyIdentifiable(model)) {
			method.addStatement("return this");
			
		} else {
			method.addStatement("$T builder = builder()", builderClassName);
			for (Field f : activeFields) {
				String fieldname = GenUtil.toValidFieldName(f.getName());

				// If it is identifiable, we want to convert it using identity()
				// If it isn't identifiable but contains identifiable, convert it using flat()
				// For non open types, we know whether it can be either of those cases now.
				// For open types, to make sure we handle mixes of identifiable and non-identifiable, we will check isIdentifiable() at runtime.
				// If we know a field doesn't have any of these possibilities we can skip having code for it.

				WhenType.is(f.getType())
						.collection(c -> {
							TypeName innerTypeClass = GenUtil.toTypeName(c.getInner(), config);

							// Based on its inner type, figure out the method that will convert it. If it doesn't need to be converted, don't add any code.
							CodeBlock.Builder convert = CodeBlock.builder();
							WhenType.is(c.getInner())
											.identifiable(t -> convert.add("v.identity()"))
											.potentiallyIdentifiable(t -> convert.add("($T) (v.isIdentifiable() ? v.identity() : v.flat())", GenUtil.toTypeName(c.getInner(), config)))
											.containsPotentiallyIdentifiable(t -> convert.add("v.flat()"))
											.otherwiseIgnore();

							if (!convert.build().isEmpty()) {
								WhenType.is(c)
										.list(l -> {
											method.beginControlFlow("if ($1N != null && !$1N.isEmpty())", fieldname);
											method.addStatement("$T<$T> _list = new $T<>($N)", List.class, innerTypeClass, ArrayList.class, fieldname);
											method.beginControlFlow("for (int i = 0, len = _list.size(); i < len; i++)");
											method.addStatement("$T v = _list.get(i)", innerTypeClass);
											method.addCode("if (v != null) _list.set(i, ").addCode(convert.build()).addCode(");\n");
											method.endControlFlow();
											method.addStatement("builder.$N(_list)", fieldname);
											method.endControlFlow();
										})
										.map(m -> {
											method.beginControlFlow("if ($1N != null && !$1N.isEmpty())", fieldname);
											method.addStatement("$T<$T,$T> _map = new $T<>($N)", Map.class, String.class, innerTypeClass, HashMap.class, fieldname);
											method.beginControlFlow("for ($T<$T,$T> e : _map.entrySet())", Map.Entry.class, String.class, innerTypeClass);
											method.addStatement("$T v = e.getValue()", innerTypeClass);
											method.addCode("if (v != null) _map.put(e.getKey(), ").addCode(convert.build()).addCode(");\n");
											method.endControlFlow();
											method.addStatement("builder.$N(_map)", fieldname);
											method.endControlFlow();
										})
										.otherwiseFail();
							}
						})
						.identifiable(thing -> method.addStatement("if ($1N != null) builder.$1N($1N.identity())", fieldname))
						.potentiallyIdentifiable(t -> method.addStatement("if ($1N != null) builder.$1N(($2T) ($1N.isIdentifiable() ? $1N.identity() : $1N.flat()))", fieldname, GenUtil.toTypeName(t, config)))
						.containsPotentiallyIdentifiable(t -> method.addStatement("if ($1N != null) builder.$1N($1N.flat())", fieldname))
						.otherwiseIgnore();

			}
			method.addStatement("return builder.build()");
		}
		
		typeSpec.addMethod(method.build());
	}

	private void with() {
		MethodSpec.Builder method = MethodSpec.methodBuilder("with")
				.addParameter(ClassNames.THING_MATCH, "match")
				.addParameter(ClassNames.THING, "replace")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(className);

		CodeBlock.Builder code = CodeBlock.builder();
		String replaced = "_replaced";
		for (Field f : activeFields) {
			String fieldname = GenUtil.toValidFieldName(f.getName());

			/*
				Object _replaced = null;
				replaced = BaseModeller.with(interface_of_ids, match, replace, InterfaceAllIdentifiable.class, false);
				if (replaced != null) return new Builder(this).interface_of_ids((InterfaceAllIdentifiable) replaced).build();
			 */

			// The type that can be replaced/searched or null if this field doesn't have a type that supports replacing.
			FieldType replaceable = WhenType.is(f.getType(), FieldType.class)
					.collection(c -> WhenType.is(c.getInner(), FieldType.class)
							.identifiable(t -> c.getInner())
							.potentiallyIdentifiable(t -> c.getInner())
							.containsPotentiallyIdentifiable(t -> c.getInner())
							.otherwise(t -> null))
					.identifiable(t -> f.getType())
					.potentiallyIdentifiable(t -> f.getType())
					.containsPotentiallyIdentifiable(t -> f.getType())
					.otherwise(t -> null);

			if (replaceable != null) {
				boolean collection = WhenType.is(f.getType(), boolean.class)
						.collection(t -> true)
						.otherwise(t -> false);
				boolean checkChildren = WhenType.containsPotentiallyIdentifiable(replaceable);

				if (collection) {
					code.addStatement("$N = $T.with($N, $T.class, match, replace, $L)", replaced, ClassNames.BASE_MODELLER, fieldname, GenUtil.toTypeName(replaceable, config), checkChildren ? "true" : "false");
				} else {
					code.addStatement("$N = $T.with($N, match, replace, $L)", replaced, ClassNames.BASE_MODELLER, fieldname, checkChildren ? "true" : "false");
				}
				code.addStatement("if ($1N != null) return new $2T(this).$3N(($4T) $1N).build()", replaced, builderClassName, fieldname, GenUtil.toTypeName(f, config));
			}
		}

		if (!code.build().isEmpty()) method.addStatement("Object $N", replaced);
		method.addCode(code.build());
		
		method.addStatement("return null");
		typeSpec.addMethod(method.build());
	}

	public static ClassName deriveClass(Config config, Thing thing) {
		return GenUtil.createInnerClassName(config.thing(thing), "Derive");
	}
	
	public static ClassName builderClass(Config config, Thing thing) {
		return GenUtil.createInnerClassName(config.thing(thing), "Builder");
	}
	
	/**
	 * If this thing has any derived methods that can be written completely programmatically
	 * such as {@link com.pocket.sync.type.FirstAvailable}, this will add a static
	 * class called Derive that has a method for each derived field that can be derived without a spec.
	 */
	private void derive() {
		List<Field> derives = new ArrayList<>();
		for (Field field : activeFields) {
			if ((field.getDerives().getFirstAvailable() == null)) continue;
			derives.add(field);
		}
		if (derives.isEmpty()) return;
		derives = FigmentUtilsKt.sortByDependencies(derives);

		TypeSpec.Builder derive = TypeSpec.classBuilder(deriveClass(config, model))
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addJavadoc("Methods for deriving fields. Only includes fields that can be purely derived without a spec. If a field isn't included here, it requires a spec. See {@link $T#derive()}", ClassNames.SPEC);
		
		for (Field field : derives) {
			if ((field.getDerives().getFirstAvailable() == null)) continue;
			
			String fieldname = GenUtil.toValidFieldName(field);
			CodeBlock.Builder code = CodeBlock.builder();
			int i = 1;
			for (ContextualReference cr : field.getDerives().getReactiveTo()) {
				Reference tp = cr.getCurrent();
				String declaredVar = "_tmp_declared"+i++;
				code.add(GenUtil.safePathDeclaredVariable(declaredVar, model, "builder", tp.getReference().getPath(), config, null));
				code.beginControlFlow("if ($N)", declaredVar);
				String var = "_tmp";
				code.add(GenUtil.safePathValueVariable(var, model, "builder", tp.getReference().getPath(), config));
				code.beginControlFlow("if (!$T.isBlank($N))", config.modeller(), var);
				FieldType endType = tp.end().getType();
				if (field.getType().equals(endType)) {
					code.addStatement("return builder.$N($N)", fieldname, var);
				} else {
					var typeDef = ((ReferenceType) field.getType()).getDefinition();
					var endTypeDef = ((ReferenceType) endType).getDefinition();
					if (typeDef.equals(endTypeDef)) {
						// If types reference the same definition and differ only in the required
						// flag, then we don't need to convert in code.
						code.addStatement("return builder.$N($N)", fieldname, var);
					} else {
						code.addStatement("return builder.$N($T.as$L($N))", fieldname, config.modeller(), typeDef.getName(), var);
					}
				}
				code.endControlFlow();
				code.endControlFlow();
			}
			code.addStatement("return builder");
			
			derive.addMethod(MethodSpec.methodBuilder(fieldname)
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addParameter(builderClassName, "builder")
					.returns(builderClassName)
					.addCode(code.build())
					.build());
			
		}
		
		typeSpec.addType(derive.build());
	}
	
	public void redact() {
		MethodSpec.Builder redact = MethodSpec.methodBuilder("redact")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassNames.ENCRYPTER, "e")
				.returns(className);
		MethodSpec.Builder unredact = MethodSpec.methodBuilder("unredact")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassNames.ENCRYPTER, "e")
				.returns(className);
		
		if (GenUtil.containsDangerousValues(model, config)) {
			redact.addStatement("$T _builder = builder()", builderClassName);
			unredact.addStatement("$T _builder = builder()", builderClassName);
			List<Field> sorted = new ArrayList<>(activeFields);
			sorted.sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
			for (Field field : sorted) {
				String name = GenUtil.toValidFieldName(field);
				if (GenUtil.containsDangerousValues(field.getType(), config)) {
					WhenType.is(field.getType())
							.list(list -> {
								TypeName innerClass = GenUtil.toTypeName(list.getInner(), config);
								redact.beginControlFlow("if ($1N != null && !$1N.isEmpty())", name);
									redact.addStatement("$T<$T> _list = new $T<>($N.size())", List.class, innerClass, ArrayList.class, name);
									redact.beginControlFlow("for ($T _v : $N)", innerClass, name);
										redact.addStatement("_list.add($T.redact(_v, e))", config.modeller());
									redact.endControlFlow();
									redact.addStatement("_builder.$N(_list)", name);
								redact.endControlFlow();

								unredact.beginControlFlow("if ($1N != null && !$1N.isEmpty())", name);
									unredact.addStatement("$T<$T> _list = new $T<>($N.size())", List.class, innerClass, ArrayList.class, name);
									unredact.beginControlFlow("for ($T _v : $N)", innerClass, name);
										unredact.addStatement("_list.add($T.unredact(_v, e))", config.modeller());
									unredact.endControlFlow();
									unredact.addStatement("_builder.$N(_list)", name);
								unredact.endControlFlow();
							})
							.map(map -> {
								TypeName innerClass = GenUtil.toTypeName(map.getInner(), config);
								redact.beginControlFlow("if ($1N != null && !$1N.isEmpty())", name);
									redact.addStatement("$T<$T,$T> _map = new $T<>($N.size())", Map.class, String.class, innerClass, HashMap.class, name);
									redact.beginControlFlow("for ($T<$T,$T> _e : $N.entrySet())", Map.Entry.class, String.class, innerClass, name);
										redact.addStatement("_map.put(_e.getKey(), $T.redact(_e.getValue(), e))", config.modeller());
									redact.endControlFlow();
									redact.addStatement("_builder.$N(_map)", name);
								redact.endControlFlow();

								unredact.beginControlFlow("if ($1N != null && !$1N.isEmpty())", name);
									unredact.addStatement("$T<$T,$T> _map = new $T<>($N.size())", Map.class, String.class, innerClass, HashMap.class, name);
									unredact.beginControlFlow("for ($T<$T,$T> _e : $N.entrySet())", Map.Entry.class, String.class, innerClass, name);
										unredact.addStatement("_map.put(_e.getKey(), $T.unredact(_e.getValue(), e))", config.modeller());
									unredact.endControlFlow();
									unredact.addStatement("_builder.$N(_map)", name);
								unredact.endControlFlow();
							})
							.otherwise(t -> {
								redact.addStatement("if ($1N != null) _builder.$1N($2T.redact($1N, e))", name, config.modeller());
								unredact.addStatement("if ($1N != null) _builder.$1N($2T.unredact($1N, e))", name, config.modeller());
							});
				}
				
			}
			redact.addStatement("return _builder.build()");
			unredact.addStatement("return _builder.build()");
			
		} else {
			redact.addStatement("return this");
			unredact.addStatement("return this");
		}
		
		typeSpec.addMethod(redact.build());
		typeSpec.addMethod(unredact.build());
	}
	
}
