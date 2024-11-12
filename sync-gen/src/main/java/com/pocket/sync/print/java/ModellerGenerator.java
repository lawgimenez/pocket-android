package com.pocket.sync.print.java;

import com.fasterxml.jackson.core.JsonParser;
import com.pocket.sync.Figments;
import com.pocket.sync.type.DefinitionKt;
import com.pocket.sync.type.Value;
import com.pocket.sync.usage.UsageMode;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.WildcardTypeName;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Modifier;

/**
 * A Modeller class helps work with {@link Value}s in java code.
 * Other generated classes will statically reference the methods in this class.
 */
public class ModellerGenerator extends ClassGenerator {
	
	public ModellerGenerator(Config config, Figments figments) {
		super(config.modeller(), config, figments);
		typeSpec.addModifiers(Modifier.PUBLIC)
				.superclass(ClassNames.BASE_MODELLER);

		typeSpec.addField(FieldSpec.builder(ThingsSpecGenerator.classname(config), "THINGS", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				.initializer("new $T()", ThingsSpecGenerator.classname(config))
				.build());

		CodeBlock.Builder listSwitch = CodeBlock.builder() // Builds the else if statements for toJsonValue(List)
				.beginControlFlow("if (v instanceof $T)", ClassNames.THING)
					.addStatement("out.add((($T) v).toJson(_config, includes))", ClassNames.THING)
				.nextControlFlow("else if (v instanceof $T)", ClassNames.STRING_ENUM)
					.addStatement("out.add((($T) v).getValue())", ClassNames.STRING_ENUM)
				.nextControlFlow("else if (v instanceof $T)", ClassNames.INTEGER_ENUM)
					.addStatement("out.add((($T) v).getValue())", ClassNames.INTEGER_ENUM);
		
		CodeBlock.Builder mapSwitch = CodeBlock.builder() // Builds the else if statements for toJsonValue(Map)
				.beginControlFlow("if (v instanceof $T)", ClassNames.THING)
					.addStatement("out.set(key, (($T) v).toJson(_config, includes))", ClassNames.THING)
				.nextControlFlow("else if (v instanceof $T)", ClassNames.STRING_ENUM)
					.addStatement("out.put(key, (($T) v).getValue())", ClassNames.STRING_ENUM)
				.nextControlFlow("else if (v instanceof $T)", ClassNames.INTEGER_ENUM)
					.addStatement("out.put(key, (($T) v).getValue())", ClassNames.INTEGER_ENUM);

		for (Value value : DefinitionKt.sortedBySource(figments.values())) {
			if (config.mode.mode(value) == UsageMode.SKIP) continue;
			ValueModeller modeller = config.value(value);
			if (modeller == null) throw new RuntimeException("Modeller missing for " + value + " make sure it has been defined in your Config");
			TypeName java = modeller.java();
			TypeName json = modeller.json();
			
			typeSpec.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassNames.TYPE_CREATOR, java), creatorName(value), Modifier.PUBLIC, Modifier.STATIC)
					.initializer("Modeller::"+fromJsonName(value))
					.build());
			
			typeSpec.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassNames.STREAMING_TYPE_CREATOR, java), streamingCreatorName(value), Modifier.PUBLIC, Modifier.STATIC)
					.initializer("Modeller::"+fromJsonName(value))
					.build());
			
			typeSpec.addField(FieldSpec.builder(ParameterizedTypeName.get(ClassNames.BYTE_CREATOR, java), byteCreatorName(value), Modifier.PUBLIC, Modifier.STATIC)
					.initializer("Modeller::"+uncompressName(value))
					.build());
			
			typeSpec.addMethod(MethodSpec.methodBuilder(fromJsonName(value))
					.addParameter(ClassNames.JSON_NODE, "value")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addStatement("if (isNull(value)) return null")
					.addCode(modeller.fromJson("value"))
					.returns(java)
					.build());
			
			typeSpec.addMethod(MethodSpec.methodBuilder(fromJsonName(value))
					.addParameter(JsonParser.class, "parser")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addStatement("if (parser.currentToken() == $T.VALUE_NULL) return null", ClassNames.JSON_TOKEN)
					.addCode(modeller.fromParser("parser"))
					.returns(java)
					.addException(IOException.class)
					.build());
			
			if (modeller.isDangerous()) {
				typeSpec.addMethod(MethodSpec.methodBuilder(toJsonName())
						.addParameter(java, "value")
						.addParameter(ArrayTypeName.of(ClassNames.INCLUDE), "includes")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addJavadoc("See {@link $1T#toJson($2T, $3T)} for warnings about dangerous values and the protections you should used when accessing them. This method should not be accessed at all unless you mean to, but to ensure you do this will throw error if you don't pass in Include.DANGEROUS to ensure you mean it. If you don't want dangerous values, don't even access this value keep it undeclared instead.", ClassNames.THING, ClassNames.JSON_CONFIG, ArrayTypeName.of(ClassNames.INCLUDE))
						.addStatement("if (!$T.contains(includes, $T.DANGEROUS)) throw new $T($S)", ClassNames.ARRAY_UTILS, ClassNames.INCLUDE, ClassName.get(RuntimeException.class), "invalid usage")
						.addStatement("if (value == null) return null")
						.addCode(modeller.toJson("value"))
						.returns(json)
						.build());
				
				CodeBlock redactCode = modeller.redact("v", "e");
				CodeBlock unredactCode = modeller.unredact("v", "e");
				if (redactCode == null || unredactCode == null) throw new RuntimeException("Dangerous field missing redaction code");
				
				typeSpec.addMethod(MethodSpec.methodBuilder("redact")
						.addParameter(java, "v")
						.addParameter(ClassNames.ENCRYPTER, "e")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addStatement("if (v == null) return null")
						.addCode(redactCode)
						.returns(java)
						.build());
				typeSpec.addMethod(MethodSpec.methodBuilder("unredact")
						.addParameter(java, "v")
						.addParameter(ClassNames.ENCRYPTER, "e")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addStatement("if (v == null) return null")
						.addCode(unredactCode)
						.returns(java)
						.build());
				
				
			} else {
				typeSpec.addMethod(MethodSpec.methodBuilder(toJsonName())
						.addParameter(java, "value")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addStatement("if (value == null) return null")
						.addCode(modeller.toJson("value"))
						.returns(json)
						.build());
			}
			
			typeSpec.addMethod(MethodSpec.methodBuilder(immutableName())
					.addParameter(java, "value")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addStatement("if (value == null) return null")
					.addCode(modeller.immutable("value"))
					.returns(java)
					.build());
			
			if (modeller.isBoolean()) {
				typeSpec.addMethod(MethodSpec.methodBuilder(asBooleanName())
						.addParameter(java, "from")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addCode(modeller.asBoolean("from"))
						.returns(boolean.class)
						.build());
			}
			typeSpec.addMethod(MethodSpec.methodBuilder(uncompressName(value))
					.addParameter(ClassNames.BYTE_READER, "from")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addCode(modeller.uncompress("from"))
					.returns(java)
					.build());
			
			typeSpec.addMethod(MethodSpec.methodBuilder(isBlankName())
					.addParameter(java, "value")
					.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					.addStatement("if (value == null) return true")
					.addCode(modeller.isBlank("value"))
					.returns(boolean.class)
					.build());
			
			for (String name : modeller.supportedConversions()) {
				Value type = figments.get(name);
				typeSpec.addMethod(MethodSpec.methodBuilder(fromValueName(value))
						.addParameter(config.value(type).java(), "value")
						.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
						.addCode(modeller.from(type, "value", config))
						.returns(java)
						.build());
			}
			
			if (modeller.isDangerous()) {
				listSwitch.nextControlFlow("else if (v instanceof $T)", java)
						.addStatement("out.add($N(($T) v, includes))", toJsonName(), java);
				mapSwitch.nextControlFlow("else if (v instanceof $T)", java)
						.addStatement("out.put(key, $N(($T) v, includes))", toJsonName(), java);
			} else {
				listSwitch.nextControlFlow("else if (v instanceof $T)", java)
						.addStatement("out.add($N(($T) v))", toJsonName(), java);
				mapSwitch.nextControlFlow("else if (v instanceof $T)", java)
						.addStatement("out.put(key, $N(($T) v))", toJsonName(), java);
			}
		}
		
		listSwitch.nextControlFlow("else if (v == null)")
				.addStatement("out.add($T.getInstance())", ClassNames.NULL_NODE)
				.nextControlFlow("else")
				.addStatement("throw new $T($S + v)", RuntimeException.class, "unknown type ")
				.endControlFlow();
		mapSwitch.nextControlFlow("else if (v == null)")
				.addStatement("out.put(key, $T.getInstance())", ClassNames.NULL_NODE)
				.nextControlFlow("else")
				.addStatement("throw new $T($S + v)", RuntimeException.class, "unknown type ")
				.endControlFlow();
		typeSpec.addMethod(MethodSpec.methodBuilder("toJsonValue")
				.addParameter(List.class, "value")
				.addParameter(ClassNames.JSON_CONFIG, "_config")
				.addParameter(ArrayTypeName.of(ClassNames.INCLUDE), "includes")
				.varargs()
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("if (value == null) return null")
				.addStatement("$T out = OBJECT_MAPPER.createArrayNode()", ClassNames.ARRAY_NODE)
				.beginControlFlow("for (Object v : value)")
				.addCode(listSwitch.build())
				.endControlFlow()
				.addStatement("return out")
				.returns(ClassNames.ARRAY_NODE)
				.build());
		typeSpec.addMethod(MethodSpec.methodBuilder("toJsonValue")
				.addParameter(ParameterizedTypeName.get(ClassName.get(Map.class), ClassName.get(String.class), WildcardTypeName.subtypeOf(Object.class)), "value")
				.addParameter(ClassNames.JSON_CONFIG, "_config")
				.addParameter(ArrayTypeName.of(ClassNames.INCLUDE), "includes")
				.varargs()
				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
				.addStatement("if (value == null) return null")
				.addStatement("$T out = OBJECT_MAPPER.createObjectNode()", ClassNames.OBJECT_NODE)
				.beginControlFlow("for (Map.Entry<String,?> entry : value.entrySet())")
				.addStatement("String key = entry.getKey()")
				.addStatement("Object v = entry.getValue()")
				.addCode(mapSwitch.build())
				.endControlFlow()
				.addStatement("return out")
				.returns(ClassNames.OBJECT_NODE)
				.build());
	}
	
	public static String creatorName(Value value) {
		return value.getName().toUpperCase() + "_CREATOR";
	}
	
	public static String streamingCreatorName(Value value) {
		return value.getName().toUpperCase() + "_STREAMING_CREATOR";
	}
	
	public static String byteCreatorName(Value value) {
		return value.getName().toUpperCase() + "_BYTE_CREATOR";
	}
	
	private static String fromJsonName(Value value) {
		return "as"+value.getName();
	}
	
	private static String uncompressName(Value value) {
		return "uncompressAs"+value.getName();
	}
	
	private static String toJsonName() {
		return "toJsonValue";
	}
	
	private static String asBooleanName() {
		return "asBoolean";
	}
	
	private static String immutableName() {
		return "immutable";
	}
	
	private static String isBlankName() {
		return "isBlank";
	}
	
	private static String fromValueName(Value to) {
		return "as" + to.getName();
	}
}
