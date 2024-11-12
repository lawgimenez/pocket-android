package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Enum;
import com.pocket.sync.type.EnumOption;
import com.pocket.sync.usage.UsageMode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * Creates a enum class for an {@link Enum}.
 */
public class EnumGenerator extends ClassGenerator {
	
	public EnumGenerator(Enum enumm, Figments figments, Config config) {
		super(TypeSpec.classBuilder(config.enumm(enumm)), config.enumm(enumm), config, figments);
		
		ClassName valueClass;
		ClassName superClass;
		String ref;
		String jsonMethod;
		String jsonParserMethod;
		if (enumm.hasIntegerValues()) {
			valueClass = ClassNames.INTEGER;
			superClass = ClassNames.INTEGER_ENUM;
			ref = "$L";
			jsonMethod = "asInt";
			jsonParserMethod = "asInteger";

			typeSpec.addMethod(MethodSpec.methodBuilder("find")
					.addJavadoc("If this value matches one of the known names at compile time, return the instance.")
					.addModifiers(Modifier.PUBLIC)
					.addModifiers(Modifier.STATIC)
					.returns(className)
					.addParameter(ClassNames.STRING, "name")
					.addStatement("if ($T.isBlank(name)) return null", config.modeller())
					.addCode(CodeBlock.builder()
							.beginControlFlow("for ($T value : KNOWN.values())", className)
							.addStatement("if (name.equalsIgnoreCase(value.name)) return value")
							.endControlFlow()
							.addStatement("return null")
							.build())
					.build())
					.addMethod(MethodSpec.methodBuilder("findFromName")
					.addModifiers(Modifier.PUBLIC)
					.addModifiers(Modifier.STATIC)
					.returns(className)
					.addParameter(ClassNames.JSON_NODE, "json")
					.addCode(CodeBlock.builder()
							.addStatement("if (json == null || json.isNull()) return null")
							.addStatement("return find(json.asText())", jsonMethod)
							.build())
					.build())
					.addMethod(MethodSpec.methodBuilder("findFromName")
							.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
							.returns(className)
							.addParameter(ClassNames.JSON_PARSER, "parser")
							.addException(IOException.class)
							.addCode(CodeBlock.builder()
									.addStatement("if (parser.currentToken() == $T.VALUE_NULL) return null", ClassNames.JSON_TOKEN)
									.addStatement("return find(Modeller.asString(parser))", config.modeller(), jsonParserMethod)
									.build())
							.build());
		} else {
			valueClass = ClassNames.STRING;
			superClass = ClassNames.STRING_ENUM;
			ref = "$S";
			jsonMethod = "asText";
			jsonParserMethod = "asString";
		}
		
		typeSpec.addModifiers(Modifier.PUBLIC)
				.superclass(superClass)
				.addJavadoc(JavaDescriptionPrinter.print(enumm.getDescription(), config))
				.addMethod(MethodSpec.constructorBuilder()
						.addModifiers(Modifier.PRIVATE)
						.addParameter(valueClass, "value")
						.addParameter(int.class, "id")
						.addParameter(ClassNames.STRING, "name")
						.addStatement("super(value, id, name)")
						.build())
				.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class), valueClass, className),
						"KNOWN", Modifier.PRIVATE, Modifier.STATIC)
						.initializer("new $T<>()", ClassName.get(HashMap.class))
						.addJavadoc("All enums that have been created via create() during this process. Includes those known at compile time and any unknown ones that were created at runtime.")
						.build())
				.addMethod(MethodSpec.methodBuilder("make")
						.addModifiers(Modifier.PRIVATE)
						.addModifiers(Modifier.STATIC)
						.returns(className)
						.addParameter(valueClass, "value")
						.addParameter(int.class, "id")
						.addParameter(ClassNames.STRING, "name")
						.addJavadoc("Creates a new value. If the value is empty or null, or if the value already exists, this throws an error.")
						.addCode(CodeBlock.builder()
								.addStatement("if ($T.isBlank(value)) throw new $T($S)", config.modeller(), ClassNames.ILLEGAL_ARG_EXCEPTION, "empty value")
								.addStatement("$T e = KNOWN.get(value)", className)
								.beginControlFlow("if (e == null)")
								.addStatement("e = new $T(value, id, name)", className)
								.addStatement("KNOWN.put(e.value, e)")
								.nextControlFlow("else")
								.addStatement("throw new $T($S)", ClassNames.ILLEGAL_ARG_EXCEPTION, "already exists")
								.endControlFlow()
								.addStatement("return e")
								.build())
						.build())
				.addMethod(MethodSpec.methodBuilder("create")
						.addModifiers(Modifier.PUBLIC)
						.addModifiers(Modifier.STATIC)
						.returns(className)
						.addParameter(valueClass, "value")
						.addJavadoc("Find or create an instance from this value. If the value is empty or null, this returns null and no value is created.")
						.addCode(CodeBlock.builder()
								.addStatement("if ($T.isBlank(value)) return null", config.modeller())
								.addStatement("$T e = KNOWN.get(value)", className)
								.beginControlFlow("if (e == null)")
								.addStatement("e = new $T(value, 0, value.toString())", className)
								.addStatement("KNOWN.put(e.value, e)")
								.endControlFlow()
								.addStatement("return e")
								.build())
						.build())
				.addMethod(MethodSpec.methodBuilder("find")
						.addJavadoc("If this value matches one of the known values at compile time, return the instance.")
						.addModifiers(Modifier.PUBLIC)
						.addModifiers(Modifier.STATIC)
						.returns(className)
						.addParameter(valueClass, "value")
						.addCode(CodeBlock.builder()
								.beginControlFlow("for ($T e : VALUES)", className)
								.addStatement("if (e.value.equals(value)) return e")
								.endControlFlow()
								.addStatement("return null")
								.build())
						.build())
				.addMethod(MethodSpec.methodBuilder("create")
						.addModifiers(Modifier.PUBLIC)
						.addModifiers(Modifier.STATIC)
						.returns(className)
						.addParameter(ClassNames.JSON_NODE, "json")
						.addCode(CodeBlock.builder()
								.addStatement("if (json == null || json.isNull()) return null")
								.addStatement("return create(json.$N())", jsonMethod)
								.build())
						.build())
				.addMethod(MethodSpec.methodBuilder("from")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.returns(className)
						.addParameter(ClassNames.JSON_PARSER, "parser")
						.addException(IOException.class)
						.addCode(CodeBlock.builder()
								.addStatement("if (parser.currentToken() == $T.VALUE_NULL) return null", ClassNames.JSON_TOKEN)
								.addStatement("return create($T.$L(parser))", config.modeller(), jsonParserMethod)
								.build())
						.build())
				.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassNames.TYPE_CREATOR, className), "JSON_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::create", className)
						.build())
				.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassNames.STREAMING_TYPE_CREATOR, className), "STREAMING_JSON_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::from", className)
						.build())
				.addMethod(MethodSpec.methodBuilder("values")
						.addModifiers(Modifier.PUBLIC)
						.addModifiers(Modifier.STATIC)
						.returns(ParameterizedTypeName.get(ClassName.get(Collection.class), className))
						.addJavadoc("An immutable list of all enum values that were known at compile time. Similar to a normal java enum.values(). Does not include those found at runtime via create()")
						.addStatement("return VALUES")
						.build());
		if (enumm.getDeprecated()) {
			typeSpec.addAnnotation(Deprecated.class);
		}
		
		CodeBlock.Builder uncompress = CodeBlock.builder();
		for (EnumOption v : enumm.getOptions()) {
			if (config.mode.mode(v) == UsageMode.SKIP) continue;
			int id = config.mode.id(v);
			final FieldSpec.Builder field = FieldSpec.builder(className, valueFieldName(v), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
					.initializer("make(" + ref + ", $L, $S)", v.getValue(), id, v.getName())
					.addJavadoc(JavaDescriptionPrinter.print(v.getDescription(), config));
			if (v.getDeprecated()) {
				field.addAnnotation(Deprecated.class);
			}
			typeSpec.addField(field.build());
			uncompress.addStatement("case $L: return $N", id, valueFieldName(v));
		}
		
		typeSpec.addMethod(MethodSpec.methodBuilder("uncompress")
				.addModifiers(Modifier.PUBLIC)
				.addModifiers(Modifier.STATIC)
				.addParameter(ClassNames.BYTE_READER, "from")
				.returns(className)
				.addStatement("$T id = from.readInt()", int.class)
				.beginControlFlow("switch(id)")
				.addCode("case 0: return create(").addCode(enumm.hasIntegerValues() ? "from.readInt()" : "from.readString()", config.modeller()).addCode(");\n")
				.addCode(uncompress.build())
				.addStatement("default: throw new $T()", RuntimeException.class)
				.endControlFlow()
				.build());
		typeSpec.addField(
				FieldSpec.builder(ParameterizedTypeName.get(ClassNames.BYTE_CREATOR, className), "BYTE_CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
						.initializer("$T::uncompress", className)
						.build());
		
		// public static final Collection<PremiumFeature> VALUES = Collections.unmodifiableCollection(KNOWN.values());
		// Must be at end so KNOWN is filled.
		typeSpec.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Collection.class), className), "VALUES", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
				.initializer("$T.unmodifiableCollection(KNOWN.values())", ClassName.get(Collections.class))
				.build());
	}
	
	public static String valueFieldName(EnumOption value) {
		return GenUtil.toValidConstantName(value.getName());
	}
	
	
	private boolean isNumber(EnumOption v) {
		try {
			NumberUtils.createInteger(v.getValue());
			return true;
		} catch (Throwable ignore){
			return false;
		}
	}
	
}
