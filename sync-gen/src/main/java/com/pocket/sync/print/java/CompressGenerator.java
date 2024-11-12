package com.pocket.sync.print.java;

import com.pocket.sync.type.ContextualReference;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.ListType;
import com.pocket.sync.type.MapType;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.path.Reference;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * {@link ThingGenerator} methods related to compression split out to a separate class for clarity.
 */
public class CompressGenerator {
	
	/**
	 * Adds methods and members needed for compression implementations
	 */
	public static void setup(TypeSpec.Builder typeSpec, Config config, ClassName className, Thing thing, Collection<Field> activeFields) {
		List<Field> compressableFields = fields(thing, activeFields, config);
		
		MethodSpec.Builder compress = MethodSpec.methodBuilder("compress")
				.addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ClassNames.BYTE_WRITER, "out");
		CodeBlock.Builder code = CodeBlock.builder();
		compress(config, compressableFields, code);
		compress.addCode(code.build());
		typeSpec.addMethod(compress.build());
		
		MethodSpec.Builder uncompress = MethodSpec.methodBuilder("uncompress")
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.returns(className)
				.addParameter(ClassNames.BYTE_READER, "_in");
		code = CodeBlock.builder();
		uncompress(config, thing, compressableFields, code);
		uncompress.addCode(code.build());
		typeSpec.addMethod(uncompress.build());
		
		typeSpec.addField(
				FieldSpec.builder(ParameterizedTypeName.get(ClassNames.BYTE_CREATOR, className), "BYTE_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::uncompress", className)
						.build());
	}
	
	/**
	 * Returns the fields from a thing in the order they should be written/read when compressing/uncompressing.
	 */
	private static List<Field> fields(Thing thing, Collection<Field> activeFields, Config config) {
		List<Field> fields = new ArrayList<>(activeFields);
		
		// Filter out self derived fields that would just recalculate themselves anyways.
		// However... keep any that are or could be part of an identity, or if they reference themselves.
		// If we lost those, then they would lose their identity and not be able to be restored properly.
		Iterator<Field> it = fields.iterator();
		while (it.hasNext()) {
			Field field = it.next();
			
			if (field.getIdentifying()) continue; // Directly part of identity. Keep
			if (!thing.isIdentifiable()) continue; // Things that are not identifiable could be part of something else's identity if their identity is made up of this thing. Keep.
			
			if (field.getDerives().getFirstAvailable() != null) {
				
				// Does it include itself within the chain of values it considers?
				boolean referencesItself = false;
				for (ContextualReference cr : (field.getDerives().getFirstAvailable()).getOptions()) {
					Reference ref = cr.getCurrent();
					if (ref.getPath() != null && ref.getPath().getSyncable().toString().equals("."+field.getName())) {
						referencesItself = true;
						break;
					}
				}
				
				// If not, then we can safely recalculate it after restoring, so no need to include it in the compressed data.
				if (!referencesItself) it.remove();
			}
		}
		
		// Sort by compat id to ensure it doesn't change over updates and new fields are always added to the tail
		Collections.sort(fields, (f1, f2) -> Integer.compare(config.mode.id(f1), config.mode.id(f2)));
		
		return fields;
	}
	
	/**
	 * Generates code for the compress() method.
	 */
	public static void compress(Config config, List<Field> fields, CodeBlock.Builder code) {
		// The number of possible fields we know at the time this code was generated
		int count = fields.size();
		code.addStatement("out.writeInt($L)", count);
		
		for (Field field : fields) {
			FieldType type = field.getType();
			String fieldname = GenUtil.toValidFieldName(field);

			WhenType.is(type)
					.collection(c -> code.addStatement("$T $N = false", boolean.class, nullableElements(fieldname)))
					.otherwiseIgnore();

			// Index    Value
			// All:
			// 0      1 = declared   0 = undeclared
			// 1      1 = not-null   0 = null
			// Booleans:
			// 2      1 = true		 0 = false
			// Collections:
			// 2      1 = not-empty  0 = empty
			// 3      1 = nullable   0 = no nulls
			
			code.beginControlFlow("if (out.writeBit(declared.$N))", fieldname);

			WhenType.is(type)
					.collection(c -> {
						code.beginControlFlow("if (out.writeBit($N != null))", fieldname);
						code.beginControlFlow("if (out.writeBit(!$N.isEmpty()))", fieldname);
						if (type instanceof ListType) {
							code.addStatement("out.writeBit(($N = $N.contains(null)))", nullableElements(fieldname), fieldname);
						} else if (type instanceof MapType) {
							code.addStatement("out.writeBit(($N = $N.containsValue(null)))", nullableElements(fieldname), fieldname);
						}
						code.endControlFlow();
						code.endControlFlow();
					})
					.value(v -> {
						if (config.value(v).isBoolean()) {
							code.beginControlFlow("if (out.writeBit($N != null))", fieldname);
							code.addStatement("out.writeBit($T.asBoolean($N))", config.modeller(), fieldname);
							code.endControlFlow();
						} else {
							code.addStatement("out.writeBit($N != null)", fieldname);
						}
					})
					.otherwise(t -> code.addStatement("out.writeBit($N != null)", fieldname));

			code.endControlFlow();
		}
		code.addStatement("out.finishByte()");
		
		// For any non-null fields, write their data
		for (Field field : fields) {
			FieldType type = field.getType();
			String fieldname = GenUtil.toValidFieldName(field);

			// If a boolean type, we don't have anything else to write
			if (isBoolean(field, config)) continue;

			// Write data
			WhenType.is(type)
					.collection(c -> code.beginControlFlow("if ($1N != null && !$1N.isEmpty())", fieldname))
					.otherwise(t -> code.beginControlFlow("if ($N != null)", fieldname));
			writeData(config, code, fieldname, type);
			code.endControlFlow();
		}
	}
	
	/**
	 * Writes if null, then the data
	 */
	private static void writeValue(Config config, CodeBlock.Builder code, String fieldname, FieldType type) {
		// Write 1/0 for non-null vs null
		// Then if non-null, its data
		code.beginControlFlow("if ($N != null)", fieldname);
			code.addStatement("out.writeBoolean(true)");
			writeData(config, code, fieldname, type);
		code.nextControlFlow("else");
			code.addStatement("out.writeBoolean(false)");
		code.endControlFlow();
	}
	
	/**
	 * Writes the non-null data
	 */
	private static void writeData(Config config, CodeBlock.Builder code, String fieldname, FieldType type) {
		WhenType.is(type)
				.open(open -> {
					code.addStatement("out.writeString($N.type())", fieldname);
					code.addStatement("$N.compress(out)", fieldname);
				})
				.thing(thing -> code.addStatement("$N.compress(out)", fieldname))
				.enumm(enumm -> {
					code.addStatement("out.writeInt($N.id)", fieldname);
					code.beginControlFlow("if ($N.id == 0)", fieldname);
					// Dynamic enum, need to write the actual value
					if (enumm.hasIntegerValues()) {
						code.addStatement("out.writeInt($N.value)", fieldname);
					} else {
						code.addStatement("out.writeString($N.value)", fieldname);
					}
					code.endControlFlow();
				})
				.collection(collection -> {
					FieldType innerType = collection.getInner();
					TypeName innerclass = GenUtil.toTypeName(innerType, config);
					code.addStatement("out.writeInt($N.size())", fieldname);

					if (type instanceof ListType) {
						code.beginControlFlow("for ($T _e : $N)", innerclass, fieldname);
						code.beginControlFlow("if ($N)", nullableElements(fieldname));
						writeValue(config, code, "_e", innerType);
						code.nextControlFlow("else");
						writeData(config, code, "_e", innerType);
						code.endControlFlow();
						code.endControlFlow();

					} else if (type instanceof MapType) {
						code.beginControlFlow("for ($T<$T,$T> _e : $N.entrySet())", Map.Entry.class, String.class, innerclass, fieldname);
						code.addStatement("$T _k = _e.getKey()", String.class);
						code.addStatement("$T _v = _e.getValue()", innerclass);
						code.addStatement("out.writeString(_k)");
						code.beginControlFlow("if ($N)", nullableElements(fieldname));
						writeValue(config, code, "_v", innerType);
						code.nextControlFlow("else");
						writeData(config, code, "_v", innerType);
						code.endControlFlow();
						code.endControlFlow();
					}
				})
				.value(v -> code.add(config.value(v).compress("out", fieldname)))
				.otherwiseFail();
	}
	
	public static void uncompress(Config config, Thing thing, List<Field> fields, CodeBlock.Builder code) {
		code.addStatement("$1T _builder = new $1T()", ThingGenerator.builderClass(config, thing));
		
		// The number of possible fields we knew about when storing this data
		code.addStatement("$T _fields = _in.readInt()", int.class);
		
		for (Field field : fields) {
			if (isBoolean(field, config)) continue;
			String fieldname = GenUtil.toValidFieldName(field);
			WhenType.is(field.getType())
					.collection(c -> code.addStatement("$T $N = 0", int.class, shouldReadFieldName(fieldname)))
					.otherwise(t -> code.addStatement("$T $N = false", boolean.class, shouldReadFieldName(fieldname)));
		}
		
		code.beginControlFlow("_fields_break : ");
		int fieldIndex = 0;
		for (Field field : fields) {
			code.addStatement("if ($L >= _fields) break _fields_break", fieldIndex++);
			
			String fieldname = GenUtil.toValidFieldName(field);

			// 1 = declared   0 = undeclared
			// 1 = not-null   0 = null
			// Booleans:
			// 1 = true		  0 = false
			// Collections:
			// 1 = not-empty  0 = empty
			// 1 = nullable   0 = no nulls

			if (isBoolean(field, config)) {
				code.addStatement("if (_in.readBit()) _builder.$N(_in.readBit() ? _in.readBit() : null)", fieldname);
			} else {
				WhenType.is(field.getType())
						.collection(c -> {
							code.beginControlFlow("if (_in.readBit())");  // declared?
								code.beginControlFlow("if (_in.readBit())");  // non-null?
									code.beginControlFlow("if (_in.readBit())");  // non-empty?
										code.addStatement("$N = _in.readBit() ? 2 : 1", shouldReadFieldName(fieldname)); // contains nulls?
									code.nextControlFlow("else");
										if (c instanceof ListType) {
											code.addStatement("_builder.$N($T.emptyList())", fieldname, Collections.class);
										} else if (c instanceof MapType) {
											code.addStatement("_builder.$N($T.emptyMap())", fieldname, Collections.class);
										}
									code.endControlFlow();
								code.nextControlFlow("else");
									code.addStatement("_builder.$N(null)", fieldname);
								code.endControlFlow();
							code.endControlFlow();
						})
						.otherwise(t -> code.addStatement("if (_in.readBit()) if (!($N = _in.readBit())) _builder.$N(null)", shouldReadFieldName(fieldname), fieldname));
			}
		}
		code.endControlFlow();
		code.addStatement("_in.finishByte()");
		
		for (Field field : fields) {
			if (isBoolean(field, config)) continue;
			String fieldname = GenUtil.toValidFieldName(field);

			WhenType.is(field.getType())
					.list(list -> code.addStatement("if ($N > 0) _builder.$N(_in.readList($L, $N == 2))", shouldReadFieldName(fieldname), fieldname, GenUtil.creatorFromBuffer(list.getInner(), config), shouldReadFieldName(fieldname)))
					.map(map -> code.addStatement("if ($N > 0) _builder.$N(_in.readMap($L, $N == 2))", shouldReadFieldName(fieldname), fieldname, GenUtil.creatorFromBuffer(map.getInner(), config), shouldReadFieldName(fieldname)))
					.otherwise(type -> {
						code.add("if ($N) _builder.$N(", shouldReadFieldName(fieldname), fieldname);
						WhenType.is(type)
								.open(open -> code.add("$L.create(_in)", OpenTypes.creatorReference(open, config)))
								.thing(t -> code.add("$T.uncompress(_in)", config.thing(t)))
								.enumm(enumm -> code.add("$T.uncompress(_in)", config.enumm(enumm)))
								.value(value -> code.add("$T.$N.create(_in)", config.modeller(), ModellerGenerator.byteCreatorName(value)))
								.otherwiseIgnore();
						code.add(");\n");
					});
		}

		code.addStatement("return _builder.build()");
	}

	private static boolean isBoolean(Field field, Config config) {
		return WhenType.is(field.getType(), boolean.class).value(v -> config.value(v).isBoolean()).otherwise(t -> false);
	}
	
	private static String shouldReadFieldName(String fieldname) {
		return "_read_"+fieldname;
	}
	
	private static String nullableElements(String fieldname) {
		return "_nullable_elements_"+fieldname;
	}
	
	
}
