package com.pocket.sync.print.java.tests;

import com.pocket.sync.print.java.ClassNames;
import com.pocket.sync.print.java.Config;
import com.pocket.sync.print.java.StandardModeller;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import java.io.File;

/**
 * TODO Documentation
 */
public class SyncTestsConfig extends Config {
	
	public SyncTestsConfig(File srcOut, File usageFile) {
		super(new Builder("SyncTests", srcOut, "com.pocket.sync.test.generated")
				.compatFile(usageFile)
				.enableGraphQl()
				.modelValue("Integer", new StandardModeller("Integer",
						ClassName.get("java.lang", "Integer"),
						ClassName.get("java.lang", "Integer")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return value.asInt()").build(); // TODO maybe should be a long for future proofing...
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return $T.asInteger($L)", ClassNames.BASE_MODELLER, parser).build(); // TODO maybe should be a long for future proofing...
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeInt($L)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return $N.readInt()", fieldname).build();
					}
					@Override
					public String graphQlType() {
						return "Int";
					}
				})
				.modelValue("String", new StandardModeller("String",
						ClassName.get("java.lang", "String"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return $L.asText()", parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return $T.asString($L)", ClassNames.BASE_MODELLER, parser).build();
					}
					@Override
					public CodeBlock isBlank(String parameter) {
						return CodeBlock.builder().addStatement("return $T.isBlank(value)", ClassNames.STRING_UTILS).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return $N.readString()", fieldname).build();
					}
				})
				.modelValue("Timestamp", new StandardModeller("Time",
						ClassName.get("com.pocket.sync.action", "Time"),
						ClassName.get("java.lang", "Long")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return new $T(value.asLong())", java()).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return new $1T(asLong($2L))", java(), parser).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return value.value").build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeLong($L.unixSeconds)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readLong())", java(), fieldname).build();
					}
				})
				.modelValue("Dangerous", new StandardModeller("Dangerous",
						ClassName.get("com.pocket.sdk.api.value", "Dangerous"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return new $T(asString($N))", java(), parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return new $1T(asString($2L))", java(), parser).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.value", parameter).build();
					}
					@Override
					public boolean isDangerous() {
						return true;
					}
					@Override
					public CodeBlock redact(String value, String encrypter) {
						return CodeBlock.builder().addStatement("return new $T($N.encrypt($N.value))", java(), encrypter, value).build();
					}
					@Override
					public CodeBlock unredact(String value, String encrypter) {
						return CodeBlock.builder().addStatement("return new $T($N.decrypt($N.value))", java(), encrypter, value).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("Boolean", new StandardModeller("Boolean",
						ClassName.get("java.lang", "Boolean"),
						ClassName.get("java.lang", "Boolean")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return asBoolean($N)", parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return asBoolean($L)", parser).build();
					}
					@Override public boolean isBoolean() { return true; }
					@Override
					public CodeBlock asBoolean(String value) {
						return CodeBlock.builder().addStatement("return $N", value).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeBoolean($L)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return $N.readBoolean()", fieldname).build();
					}
				})
				.modelValue("Float", new StandardModeller("Float",
						ClassName.get("java.lang", "Double"),
						ClassName.get("java.lang", "Double")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.asDouble()", parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return asDouble($L)", parser).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeDouble($L)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return $N.readDouble()", fieldname).build();
					}
				})
				.modelValue("ID", new StandardModeller("ID",
						ClassName.get("com.pocket.sdk.api.value", "IdString"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return new $T(asString($N))", java(), parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return new $1T(asString($2L))", java(), parser).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.id", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.id)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
		);
		
		
		
	}
}
