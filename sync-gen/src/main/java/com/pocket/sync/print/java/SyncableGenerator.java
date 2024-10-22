package com.pocket.sync.print.java;

import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.sync.Figments;
import com.pocket.sync.type.Auth;
import com.pocket.sync.type.CollectionType;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.ListType;
import com.pocket.sync.type.MapType;
import com.pocket.sync.type.OpenType;
import com.pocket.sync.type.Remap;
import com.pocket.sync.type.Remote;
import com.pocket.sync.type.Syncable;
import com.pocket.sync.type.Thing;
import com.pocket.sync.util.FigmentUtilsKt;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

/**
 * Shared generation code between both Things and Actions
 */
public class SyncableGenerator {

	private static final String PARAM_JSON_CONFIG = "_config";
	
	/**
	 * Adds a Builder class, and builder() method to the class.
	 * @param fields The fields to include in the builder
	 * @param className The thing/action's class name
	 * @param builderClassName The class name to use for the builder
	 * @param declaredClassName The class name of the Declared implementation class. See {@link #addDeclared(Collection, ClassName, ClassName, TypeSpec.Builder)}
	 * @param declaredMutableClassName The class name of the mutable Declared implementation class. See {@link #addDeclared(Collection, ClassName, ClassName, TypeSpec.Builder)}
	 * @param buildersuper The class name of the super interface of this builder
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addBuilder(Collection<Field> fields, Config config, ClassName className, ClassName builderClassName, ClassName declaredClassName, ClassName declaredMutableClassName, ClassName buildersuper, TypeSpec.Builder typeSpec) {
		TypeSpec.Builder builder = TypeSpec.classBuilder(builderClassName)
				.addSuperinterface(ParameterizedTypeName.get(buildersuper, className))
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PUBLIC)
						.build())
				.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PUBLIC)
						.addParameter(className, "src")
						.addStatement("set(src)")
						.build())
				.addField(FieldSpec.builder(declaredMutableClassName, "declared", Modifier.PRIVATE)
						.initializer("new $T()", declaredMutableClassName)
						.build());
		
		MethodSpec.Builder copier = MethodSpec.methodBuilder("set")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(builderClassName)
				.addParameter(className, "src");
		
		for (Field field : fields) {
			String name = GenUtil.toValidFieldName(field.getName());
			// When copying over from another builder, we can do a direct copy rather than going through the field() method which does extra work to ensure immutability
			copier.beginControlFlow("if (src.declared.$N)", name);
			copier.addStatement("declared.$N = true", name);
			copier.addStatement("this.$N = src.$N", name, name);
			copier.endControlFlow();
		}
		
		copier.addStatement("return this");
		
		builder.addMethod(copier.build());
		
		for (Field field : fields) {
			TypeName fieldType = GenUtil.toTypeName(field, config);
			String fieldname = GenUtil.toValidFieldName(field.getName());
			MethodSpec.Builder method = MethodSpec.methodBuilder(fieldname)
					.addParameter(fieldType, "value")
					.addStatement("declared.$N = true", fieldname)
					.addStatement("this.$N = $T.immutable(value)", fieldname, config.modeller())
					.addStatement("return this")
					.returns(builderClassName)
					.addJavadoc(fieldJavaDoc(field, config));
			if (field.getDerives().getFirstAvailable() != null) {
				method.addModifiers(Modifier.PRIVATE);
				method.addJavadoc("(This field is self derived and will be automatically calculated during {@link #build()})");
			} else {
				method.addModifiers(Modifier.PUBLIC);
			}
			builder.addMethod(method.build());
			builder.addField(fieldType, fieldname, Modifier.PROTECTED);
		}
		
		CodeBlock.Builder selfDerive = CodeBlock.builder();
		List<Field> selfDerivable = new ArrayList<>();
		for (Field field : fields) {
			if (field.getDerives().getFirstAvailable() == null) continue;
			selfDerivable.add(field);
		}
		selfDerivable = FigmentUtilsKt.sortByDependencies(selfDerivable);
		for (Field field : selfDerivable) {
			String fieldname = GenUtil.toValidFieldName(field);
			selfDerive.addStatement("Derive.$N(this)", fieldname);
		}
		builder.addMethod(MethodSpec.methodBuilder("build")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(className)
				.addCode(selfDerive.build())
				.addStatement("return new $T(this, new $T(declared))", className, declaredClassName)
				.build());
		
		typeSpec.addType(builder.build());
		
		typeSpec.addMethod(MethodSpec.methodBuilder("builder")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(builderClassName)
				.addStatement("return new Builder(this)")
				.build());
	}
	
	/**
	 * Adds a constructor that builds from a provided builder.
	 * @param fields The fields included in the builder.
	 * @param builderClassName The class name of the builder. See {@link #addBuilder(Collection, Config, ClassName, ClassName, ClassName, ClassName, ClassName, TypeSpec.Builder)}
	 * @param declaredClassName The class name of the Declared implementation class. See {@link #addDeclared(Collection, ClassName, ClassName, TypeSpec.Builder)}
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addConstructor(Collection<Field> fields, ClassName builderClassName, ClassName declaredClassName, TypeSpec.Builder typeSpec) {
		MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PRIVATE)
				.addParameter(builderClassName, "builder")
				.addParameter(declaredClassName, "declared")
				.addStatement("this.declared = declared");
		
		for (Field field : fields) {
			String name = GenUtil.toValidFieldName(field.getName());
			// When copying over from a builder, we can trust all values are already immutable since builders handle converting to immutables when values are set
			constructor.addStatement("this.$N = builder.$N", name, name);
		}
		
		typeSpec.addMethod(constructor.build());
	}
	
	/**
	 * Adds instance variables for each field
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addFields(Syncable parent, Collection<Field> actives, Config config, TypeSpec.Builder typeSpec) {
		for (Field field : parent.getFields().getAll()) {
			if (!actives.contains(field)) continue;
			TypeName type = GenUtil.toTypeName(field, config);
			String name = GenUtil.toValidFieldName(field.getName());
			CodeBlock description = JavaDescriptionPrinter.print(field.getDescription(), config);
			boolean isCollection = WhenType.is(field.getType(), boolean.class).collection(c -> true).otherwise(t -> false);

			FieldSpec.Builder builder = FieldSpec.builder(type, name, Modifier.PUBLIC, Modifier.FINAL)
					.addJavadoc(description)
					.addJavadoc(!isCollection ? "" : " <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>");
			if (!field.getAliases().isEmpty()) {
				builder.addJavadoc("This is an alias name. The following APIs make use of this field differently:")
						.addJavadoc("\n<ul>");
				Map<Remote, String> aliases = field.getAliases();
				for (Remote remote : aliases.keySet()) {
					builder.addJavadoc("\n<li>$S : $S</li>", remote.getName(), aliases.get(remote));
				}
				builder.addJavadoc("\n<ul>\n");
			}
			if (field.getDeprecated()) {
				builder.addAnnotation(Deprecated.class);
			}
			builder.addAnnotation(Nullable.class);
			typeSpec.addField(builder.build());

			OpenTypes.setupForField(typeSpec, parent, field, config);
		}
	}

	public static CodeBlock fieldJavaDoc(Field field, Config config) {
		CodeBlock doc = JavaDescriptionPrinter.print(field.getDescription(), config);
		if (!field.getAliases().isEmpty()) {
			CodeBlock.Builder builder = CodeBlock.builder()
					.add(doc)
					.add("This is an alias name. The following APIs make use of this field differently:")
					.add("\n<ul>");
			Map<Remote, String> aliases = field.getAliases();
			for (Remote remote : aliases.keySet()) {
				builder.add("\n<li>$S : $S</li>", remote.getName(), aliases.get(remote));
			}
			builder.add("\n<ul>\n");
			return builder.build();
		} else {
			return doc;
		}
	}
	
	/**
	 * Adds a Declared class and its mutable class
	 * @param fields The fields of this thing/action
	 * @param declaredClassName The class name of the Declared implementation class.
	 * @param declaredMutableClassName The class name of the mutable Declared implementation class.
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addDeclared(Collection<Field> fields, ClassName declaredClassName, ClassName declaredMutableClassName, TypeSpec.Builder typeSpec) {
		typeSpec.addField(declaredClassName, "declared", Modifier.PUBLIC, Modifier.FINAL);
		
		TypeSpec.Builder mutable = TypeSpec.classBuilder(declaredMutableClassName)
				.addModifiers(Modifier.PRIVATE, Modifier.STATIC);
		TypeSpec.Builder immutable = TypeSpec.classBuilder(declaredClassName)
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
		MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PRIVATE)
				.addParameter(declaredMutableClassName, "declared");
		for (Field field : fields) {
			String name = GenUtil.toValidFieldName(field.getName());
			mutable.addField(boolean.class, name, Modifier.PRIVATE);
			immutable.addField(boolean.class, name, Modifier.PUBLIC, Modifier.FINAL);
			constructor.addStatement("this.$N = declared.$N", name, name);
		}
		immutable.addMethod(constructor.build());
		typeSpec.addType(mutable.build());
		typeSpec.addType(immutable.build());
	}
	
	/**
	 * Add a from(Json) method
	 * @param fields The fields of this thing/action
	 * @param classname The class name of the thing/action
	 * @param builderClassName The class name of its Builder. See {@link #addBuilder(Collection, Config, ClassName, ClassName, ClassName, ClassName, ClassName, TypeSpec.Builder)}
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addFromJson(Collection<Field> fields, ClassName classname, ClassName builderClassName, Config config, TypeSpec.Builder typeSpec) {
		String allowed = "allowed";
		Function<FieldType, CodeBlock> optionalAllowedParam =
				type -> WhenType.is(type, CodeBlock.class)
						.thing(__ -> CodeBlock.of(", $N", allowed))
						.open(__ -> CodeBlock.of(", $N", allowed))
						.otherwise(__ -> CodeBlock.of(""));
		MethodSpec.Builder method = MethodSpec.methodBuilder("from")
				.addModifiers(Modifier.PUBLIC)
				.addModifiers(Modifier.STATIC)
				.returns(classname)
				.addParameter(ClassNames.JSON_NODE, "jsonNode")
				.addParameter(ClassNames.JSON_CONFIG, PARAM_JSON_CONFIG)
				.addParameter(ArrayTypeName.of(ClassNames.ALLOW), allowed)
				.varargs();
		
		CodeBlock.Builder code = CodeBlock.builder()
				.addStatement("if (jsonNode == null || jsonNode.isNull()) return null")
				.addStatement("$T json = jsonNode.deepCopy()", ClassNames.OBJECT_NODE)
				.addStatement("$T builder = new $T()", builderClassName, builderClassName)
				.addStatement("$T value", ClassNames.JSON_NODE);
		
		for (Field field : fields) {
			String fieldname = GenUtil.toValidFieldName(field.getName());
			FieldType type = field.getType();
			TypeName typeName = GenUtil.toTypeName(type, config);

			String fieldByAlias = GenUtil.aliasedNameCode(field, PARAM_JSON_CONFIG + ".getRemote()");

			if (field.getDerives().getRemap() != null) {
				Remap remap = field.getDerives().getRemap();
				FieldType innerType = ((ListType) type).getInner();
				code.add("$T<$T> $N = $T.remap(json.get($S), $S, ",
						ClassName.get(List.class), GenUtil.toTypeName(innerType, config), fieldname, config.modeller(), remap.getList().toString().substring(1), remap.getField());
				code.add(GenUtil.creatorFromJsonCode(innerType, config));
				code.add(GenUtil.addParamIfThing(innerType, PARAM_JSON_CONFIG));
				code.add(optionalAllowedParam.apply(innerType)).add(");\n");
				code.addStatement("if ($1N != null) builder.$1N($1N)", fieldname);
				
			} else {
				code.addStatement("value = json.get($N)", fieldByAlias);
				WhenType.is(type)
						.enumm(e -> {
							if (e.hasIntegerValues()) {
								code.addStatement("if (value != null) builder.$N($N.getSupportsIntEnums() ? $T.create(value) : $T.findFromName(value))", fieldname, PARAM_JSON_CONFIG, typeName, typeName);
							} else {
								code.addStatement("if (value != null) builder.$N($T.create(value))", fieldname, typeName);
							}
						})
						.otherwise(t -> {
							code.add("if (value != null) builder.$N(", fieldname);
							WhenType.is(t)
								.map(map -> code.add("$T.asMap(value, ", config.modeller()).add(GenUtil.creatorFromJsonCode(map.getInner(), config)).add(GenUtil.addParamIfThing(map.getInner(), PARAM_JSON_CONFIG)).add(optionalAllowedParam.apply(map.getInner())).add(")"))
								.list(list -> code.add("$T.asList(value, ", config.modeller()).add(GenUtil.creatorFromJsonCode(list.getInner(), config)).add(GenUtil.addParamIfThing(list.getInner(), PARAM_JSON_CONFIG)).add(optionalAllowedParam.apply(list.getInner())).add(")"))
								.open(open -> code.add(GenUtil.creatorFromJsonCode(open, config)).add(".create(value, $N, $N)", PARAM_JSON_CONFIG, allowed))
								.thing(thing -> code.add("$T.from(value, $N, $N)", typeName, PARAM_JSON_CONFIG, allowed))
								.value(value -> code.add("$T.as" + value.getName() + "(value)", config.modeller()))
								.otherwiseFail();
							code.add(");\n");
						});
			}
		}
		
		code.addStatement("return builder.build()");
		
		method.addCode(code.build());
		typeSpec.addMethod(
				method.build());
	}
	
	public static void addFromParser(Collection<Field> fields, ClassName className, ClassName builderClassName, Config config, TypeSpec.Builder typeSpec) {
		final String parser = "parser";
		final String allowed = "allowed";
		Function<FieldType, CodeBlock> optionalAllowedParam =
				type -> WhenType.is(type, CodeBlock.class)
						.thing(__ -> CodeBlock.of(", $N", allowed))
						.open(__ -> CodeBlock.of(", $N", allowed))
						.otherwise(__ -> CodeBlock.of(""));
		final MethodSpec.Builder method = MethodSpec.methodBuilder("from")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(className)
				.addParameter(ClassNames.JSON_PARSER, parser)
				.addParameter(ClassNames.JSON_CONFIG, PARAM_JSON_CONFIG)
				.addParameter(ArrayTypeName.of(ClassNames.ALLOW), allowed)
				.varargs()
				.addException(IOException.class);
		
		final String currentName = "currentName";
		final String builder = "builder";
		final CodeBlock.Builder code = CodeBlock.builder()
				.addStatement("if ($L == null) return null", parser)
				.addStatement("if ($1L.currentToken() == null) $1L.nextToken()", parser)  // If a parser is passed in that hasn't been started yet, we can start it for them.
				.addStatement("if ($1L.currentToken() == $2T.VALUE_NULL) return null", parser, ClassNames.JSON_TOKEN)
				.addStatement("if (!$1L.isExpectedStartObjectToken()) throw new $2T($3S + $4T.errorLocation($1L))", parser, ClassNames.RUNTIME_EX, "Unexpected start token ", ClassNames.JSON_UTIL)
				.addStatement("final $1T $2L = new $1T()", builderClassName, builder)
				.beginControlFlow("while ($1L.nextToken() != $2T.END_OBJECT && !$1L.isClosed())", parser, ClassNames.JSON_TOKEN)
				.addStatement("final $1T $2L = $3L.getCurrentName()", String.class, currentName, parser)
				.addStatement("$1L.nextToken()", parser)
				.beginControlFlow("if ($L == null)", currentName)
				.addStatement("$L.skipChildren()", parser);
		
		for (Field field : fields) {
			String fieldName = GenUtil.toValidFieldName(field.getName());
			FieldType type = field.getType();
			TypeName typeName = GenUtil.toTypeName(type, config);

			Collection<Field> remaps = FigmentUtilsKt.remappedBy(field);
			if (field.getDerives().getRemap() != null) {
				// We'll handle this as part of the field this remaps from.
			} else if (!remaps.isEmpty()) {
				code.nextControlFlow("else if ($1L.equals($2S))", currentName, field.getName())
						.add("// WARNING: LEAVING STREAMING MODE\n")
						.add("// To handle remaps we currently parse the field using the object mapper.\n")
						.add("// This way we can iterate through the list multiple times, so we can build both\n")
						.add("// the actual list and all the remapped lists.\n")
						.add("// We could do it fully in streaming mode to make sure we get the best performance,\n")
						.add("// but we would have to add fake fields to Item (or other remap targets) that aren't part of its state\n")
						.add("// or figure out another way to pass these extra passed values to the parent thing (like Get)\n")
						.add("// so it could build the remap fields from them.\n")
						.addStatement("final $1T value = $2T.OBJECT_MAPPER.readTree($3L)",
								JsonNode.class, config.modeller(), parser);
				FieldType innerType = ((ListType) type).getInner();
				code.add("if (value != null) builder.$N(", fieldName)
						.add("$T.asList(value, ", config.modeller())
						.add(GenUtil.creatorFromJsonCode(innerType, config))
						.add(GenUtil.addParamIfThing(innerType, PARAM_JSON_CONFIG))
						.addStatement("))");
				for (Field remapped : remaps) {
					//List<SearchMatch> search_matches = Modeller.remap(value, "highlights", SearchMatch.JSON_CREATOR);
					//if (search_matches != null) builder.search_matches(search_matches);
					String remapName = GenUtil.toValidFieldName(remapped.getName());
					Remap remap = remapped.getDerives().getRemap();
					FieldType remapType = ((ListType) remapped.getType()).getInner();
					code.add("$T<$T> $N = $T.remap(value, $S, ",
							ClassName.get(List.class), GenUtil.toTypeName(remapType, config), remapName, config.modeller(), remap.getField());
					code.add(GenUtil.creatorFromJsonCode(remapType, config));
					code.add(GenUtil.addParamIfThing(remapType, PARAM_JSON_CONFIG));
					code.add(");\n");
					code.addStatement("if ($1N != null) builder.$1N($1N)", remapName);
				}
				
			} else {
				
				code.nextControlFlow("else if ($1L.equals($2S))", currentName, field.getName())
						.add("$1L.$2N(", builder, fieldName);

				WhenType.is(type)
						.map(map -> code.add("$1T.asMap($2L, ", config.modeller(), parser)
								.add(GenUtil.streamingCreatorFromJsonCode(map.getInner(), config))
								.add(GenUtil.addParamIfThing(map.getInner(), PARAM_JSON_CONFIG))
								.add(optionalAllowedParam.apply(map.getInner()))
								.add(")"))
						.list(list -> code.add("$1T.asList($2L, ", config.modeller(), parser)
								.add(GenUtil.streamingCreatorFromJsonCode(list.getInner(), config))
								.add(GenUtil.addParamIfThing(list.getInner(), PARAM_JSON_CONFIG))
								.add(optionalAllowedParam.apply(list.getInner()))
								.add(")"))
						.open(open -> code.add("$1L.create($2L, $3N, $4N)", OpenTypes.creatorReference(open, config), parser, PARAM_JSON_CONFIG, allowed))
						.enumm(enumm -> code.add("$1T.from($2L)", typeName, parser))
						.thing(thing -> code.add("$1T.from($2L, $3N, $4N)", typeName, parser, PARAM_JSON_CONFIG, allowed))
						.value(value -> code.add("$1T.as$2L($3L)", config.modeller(), value.getName(), parser))
						.otherwiseFail();

				code.addStatement(")");
			}
		}
		
		code.nextControlFlow("else")
				.addStatement("$L.skipChildren()", parser)
				.endControlFlow()
				.endControlFlow()
				.addStatement("return builder.build()");
		
		method.addCode(code.build());
		typeSpec.addMethod(method.build());
	}
	
	/**
	 * Add a toJson() method
	 * @param fields The fields of this thing/action
	 * @param typeSpec The spec to add definitions to
	 * @param extra Some extra code to tack on before the return statement.
	 */
	public static void addToJson(Syncable definition, Collection<Field> fields, Config config, TypeSpec.Builder typeSpec, CodeBlock extra) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("toJson")
				.addParameter(ClassNames.JSON_CONFIG, PARAM_JSON_CONFIG)
				.addParameter(ArrayTypeName.of(ClassNames.INCLUDE), "includes")
				.varargs()
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.OBJECT_NODE);

		// Gather some data on types of fields we have so we know what we need to bother doing.
		boolean hasDangerousValues = false;
		boolean hasSubThings = false;
		for (Field field : fields) {
			if (GenUtil.isOrIsCollectionOfDangerousValues(field.getType(), config)) {
				hasDangerousValues = true;
			}
			hasSubThings = hasSubThings || WhenType.is(field.getType(), boolean.class)
					.collection(c -> WhenType.is(field.getType(), boolean.class)
							.open(t -> true)
							.thing(t -> true)
							.otherwise(t -> false)
					)
					.open(t -> true)
					.thing(t -> true)
					.otherwise(t -> false);
		}


		CodeBlock.Builder code = CodeBlock.builder();
		code.addStatement("$T json = $T.OBJECT_MAPPER.createObjectNode()", ClassNames.OBJECT_NODE, config.modeller());

		// Open type includes?
		code.beginControlFlow("if ($1T.contains(includes, $1T.OPEN_TYPE))", ClassNames.INCLUDE);
		code.addStatement("json.put($T.SERIALIZATION_TYPE_KEY, $N)", ClassNames.THING, (definition instanceof Thing ? "THING_TYPE" : "ACTION_NAME"));
		if (hasSubThings) {
			// Only need to bother doing the work to remove if we'll actually pass it onto anything else.
			code.addStatement("includes = $1T.removeAssumingPresent(includes, $1T.OPEN_TYPE)", ClassNames.INCLUDE);
		}
		code.endControlFlow();
		// Dangerous includes?
		if (hasDangerousValues) {
			// Only need to bother adding this extra work if we know we'll use this value
			code.addStatement("boolean includeDangerous = $1T.contains(includes, $1T.DANGEROUS)", ClassNames.INCLUDE);
		}

		
		List<Field> sorted = new ArrayList<>(fields);
		sorted.sort((o1, o2) -> o1.getName().compareTo(o2.getName())); // IMPORTANT!!!  Having the fields in a consistent alphabetical order is very important to ThingGenerator.idkey()!
		for (Field field : sorted) {
			String fieldname = GenUtil.toValidFieldName(field.getName());
			String fieldByAlias = GenUtil.aliasedNameCode(field, PARAM_JSON_CONFIG + ".getRemote()");

			if (hasDangerousValues && GenUtil.isOrIsCollectionOfDangerousValues(field.getType(), config)) {
				code.addStatement("if (includeDangerous && declared.$N) json.put($N, $T.toJsonValue($N, includes))", fieldname, fieldByAlias, config.modeller(), fieldname);
			} else {
				WhenType.is(field.getType())
						.enumm(e -> {
							if (e.hasIntegerValues()) {
								code
										.beginControlFlow("if ($N.getSupportsIntEnums())", PARAM_JSON_CONFIG)
										.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N))", fieldname, fieldByAlias, config.modeller(), fieldname)
										.nextControlFlow("else")
										.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N.name))", fieldname, fieldByAlias, config.modeller(), fieldname)
										.endControlFlow();
							} else {
								code.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N))", fieldname, fieldByAlias, config.modeller(), fieldname);
							}
						})
						.collectionOfType(CollectionType.class, OpenType.class, open -> code.addStatement("if (declared.$1N) json.put($2N, $3T.toJsonValue($4N, $5N, $6T.add(includes, $6T.OPEN_TYPE)))", fieldname, fieldByAlias, config.modeller(), fieldname, PARAM_JSON_CONFIG, ClassNames.INCLUDE))
						.open(open -> code.addStatement("if (declared.$1N) json.put($2N, $3T.toJsonValue($4N, $5N, $6T.add(includes, $6T.OPEN_TYPE)))", fieldname, fieldByAlias, config.modeller(), fieldname, PARAM_JSON_CONFIG, ClassNames.INCLUDE))
						.collection(c -> code.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N, $N, includes))", fieldname, fieldByAlias, config.modeller(), fieldname, PARAM_JSON_CONFIG))
						.thing(t -> code.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N, $N, includes))", fieldname, fieldByAlias, config.modeller(), fieldname, PARAM_JSON_CONFIG))
						.otherwise(t -> code.addStatement("if (declared.$N) json.put($N, $T.toJsonValue($N))", fieldname, fieldByAlias, config.modeller(), fieldname));
			}
		}
		
		if (extra != null) code.add(extra);
		
		code.addStatement("return json");
		method.addCode(code.build());
		typeSpec.addMethod(method.build());
	}
	
	/**
	 * Add a toString() method
	 * @param nameVar The name of the field that will contain the type/name of the thing or action
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addToString(String nameVar, TypeSpec.Builder typeSpec) {
		typeSpec.addMethod(MethodSpec.methodBuilder("toString")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(String.class)
				.addStatement("return toJson(new JsonConfig(REMOTE.style, true), Include.OPEN_TYPE).toString()")
				.build());
	}
	
	public static void addCreator(TypeSpec.Builder typeSpec, ClassName className) {
		typeSpec.addField(
				FieldSpec.builder(ParameterizedTypeName.get(ClassNames.SYNCABLE_CREATOR, className), "JSON_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::from", className)
						.build());
	}
	
	/**
	 * Add the remote() interface
	 * @param definition The definition
	 * @param actives A list of the active fields, used to filter out any you don't want to be included (though it will only use the ones with aliases)
	 * @param typeSpec The spec to add definitions to
	 */
	public static void addRemoteInfo(Syncable definition, Collection<Field> actives, TypeSpec.Builder typeSpec, Config config) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("remote")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(ClassNames.REMOTE);
		String constant = "REMOTE";
		CodeBlock.Builder init = CodeBlock.builder();
		Field hashTarget = definition.getFields().hashTarget();
		String endpoint = definition.getEndpoint().getMerged() != null ? definition.getEndpoint().getMerged().getAddress() : null;
		String requestMethod = (definition.getEndpoint().getMerged() != null && definition.getEndpoint().getMerged().getMethod() != null) ? definition.getEndpoint().getMerged().getMethod() : "GET";
		init.add("new $T($S, $T.$N, $T.$N, ", ClassNames.REMOTE, endpoint, ClassNames.REMOTE_METHOD, requestMethod, RemoteStyleGenerator.classname(config), RemoteStyleGenerator.enumvalue(definition.getRemote().getRemote()));
		if (hashTarget != null) {
			init.add("$S", hashTarget.getName());
		} else {
			init.add("null");
		}
		for (Field field : definition.getFields().getAll()) {
			if (!actives.contains(field)) continue;
			if (field.getAliases().isEmpty()) continue;
			for (Remote remote : field.getAliases().keySet()) {
				init.add(", $S, $S, $S", field.getName(), remote.getName(), field.getAliases().get(remote));
			}
		}
		init.add(")");
		typeSpec.addField(FieldSpec.builder(ClassNames.REMOTE, constant, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer(init.build())
				.build());
		method.addStatement("return $N", constant);
		typeSpec.addMethod(method.build());
	}
	
	/**
	 * Adds the toMap() method implementation
	 * @param extra Optional extra code to tack on before the return statement
	 */
	public static void addToMap(Collection<Field> fields, Config config, TypeSpec.Builder typeSpec, CodeBlock extra) {
		ParameterizedTypeName map = ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), ClassName.get(Object.class));
		ParameterizedTypeName hashmap = ParameterizedTypeName.get(ClassName.get(HashMap.class), ClassName.get(String.class), ClassName.get(Object.class));
		
		MethodSpec.Builder method = MethodSpec.methodBuilder("toMap")
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ArrayTypeName.of(ClassNames.INCLUDE), "includes")
				.varargs()
				.addAnnotation(Override.class)
				.returns(map)
				.addStatement("$T map = new $T()", map, hashmap)
				.addStatement("boolean includeDangerous = $T.contains(includes, $T.DANGEROUS)", ClassNames.ARRAY_UTILS, ClassNames.INCLUDE);
		for (Field f : fields) {
			if (GenUtil.isOrIsCollectionOfDangerousValues(f.getType(), config)) {
				method.addStatement("if (includeDangerous && declared.$2N) map.put($1S, $2N)", f.getName(), GenUtil.toValidFieldName(f.getName()));
			} else {
				method.addStatement("if (declared.$2N) map.put($1S, $2N)", f.getName(), GenUtil.toValidFieldName(f.getName()));
			}
		}
		if (extra != null) method.addCode(extra);
		method.addStatement("return map");
		typeSpec.addMethod(method.build());
	}
	
	/**
	 * Adds an equals check for this field. Assumes there is an e local variable with a Thing.Equality setting and this field is available as an instance variable.
	 */
	public static void addEqualsFor(MethodSpec.Builder equals, Field f) {
		String n = GenUtil.toValidFieldName(f.getName());
		WhenType.is(f.getType())
				.collectionOfDefinition(ListType.class, Thing.class, t -> equals.addStatement("if (!$T.listEquals(e, $N, that.$N)) return false", ClassNames.THING_UTIL, n, n))
				.collectionOfDefinition(MapType.class, Thing.class, t -> equals.addStatement("if (!$T.mapEquals(e, $N, that.$N)) return false", ClassNames.THING_UTIL, n, n))
				.thing(t -> equals.addStatement("if (!$T.fieldEquals(e, $N, that.$N)) return false", ClassNames.THING_UTIL, n, n))
				.otherwise(t -> equals.addStatement("if ($N != null ? !$N.equals(that.$N) : that.$N != null) return false", n, n, n, n));
	}
	
	/**
	 * Adds a hashcode addition for this field. Assumes there is an e local variable with a Thing.Equality setting and this field is available as an instance variable.
	 */
	public static void addHashCodeFor(MethodSpec.Builder hashCode, Field field, String hashcodeResultVar) {
		String n = GenUtil.toValidFieldName(field.getName());
		WhenType.is(field.getType())
				.collectionOfDefinition(ListType.class, Thing.class, t -> hashCode.addStatement("$N = 31 * $N + ($N != null ? $T.collectionHashCode(e, $N) : 0)", hashcodeResultVar, hashcodeResultVar, n, ClassNames.THING_UTIL, n))
				.collectionOfDefinition(MapType.class, Thing.class, t -> hashCode.addStatement("$N = 31 * $N + ($N != null ? $T.mapHashCode(e, $N) : 0)", hashcodeResultVar, hashcodeResultVar, n, ClassNames.THING_UTIL, n))
				.thing(t -> hashCode.addStatement("$N = 31 * $N + $T.fieldHashCode(e, $N)", hashcodeResultVar, hashcodeResultVar, ClassNames.THING_UTIL, n))
				.otherwise(t -> hashCode.addStatement("$N = 31 * $N + ($N != null ? $N.hashCode() : 0)", hashcodeResultVar, hashcodeResultVar, n, n));
	}

	public static void addAuthType(TypeSpec.Builder typeSpec, Auth auth, Config config, Figments figments) {
		ClassName clazz = AuthTypeGenerator.classname(config);
		MethodSpec.Builder method = MethodSpec.methodBuilder("auth")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotation(Override.class)
				.returns(clazz);
		method.addStatement("return $T.$N", clazz, AuthTypeGenerator.enumvalue(auth));
		typeSpec.addMethod(method.build());
	}

}

