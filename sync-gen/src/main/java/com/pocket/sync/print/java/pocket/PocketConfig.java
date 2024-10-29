package com.pocket.sync.print.java.pocket;

import com.pocket.sync.print.java.ClassNames;
import com.pocket.sync.print.java.Config;
import com.pocket.sync.print.java.StandardModeller;
import com.pocket.sync.type.Value;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link Config} for Pocket's sync engine generation
 */
public class PocketConfig extends Config {
	
	private static final ClassName MODELLER_UTIL = ClassName.get("com.pocket.sdk.api.value", "ModellerUtil");
	
	public PocketConfig(File srcOut, File usageFile) {
		super(new Builder("Pocket", srcOut, "com.pocket.sdk.api.generated")
				.compatFile(usageFile)
				.enableGraphQl()
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
				.modelValue("Integer", new StandardModeller("Integer",
						ClassName.get("java.lang", "Integer"),
						ClassName.get("java.lang", "Integer")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.asInt()", parameter).build(); // TODO maybe should be a long for future proofing...
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
				.modelValue("Boolean", new StandardModeller("Boolean",
						ClassName.get("java.lang", "Boolean"),
						ClassName.get("java.lang", "Boolean")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						// This method is complex enough that we'll just reference a pre made utility instead of trying to write it out here.
						return CodeBlock.builder().addStatement("return $T.asBoolean($N)", MODELLER_UTIL, parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						// This method is complex enough that we'll just reference a pre made utility instead of trying to write it out here.
						return CodeBlock.builder().addStatement("return $1T.asBoolean($2L)", MODELLER_UTIL, parser).build();
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
				.modelValue("String", new StandardModeller("String",
						ClassName.get("java.lang", "String"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.asText()", parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return $T.asString($L)", ClassNames.BASE_MODELLER, parser).build();
					}
					@Override
					public CodeBlock isBlank(String parameter) {
						return CodeBlock.builder().addStatement("return $T.isBlank($N)", ClassNames.STRING_UTILS, parameter).build();
					}
					@Override
					public Collection<String> supportedConversions() {
						return Collections.singletonList("Url");
					}
					@Override
					public CodeBlock from(Value other, String parameter, Config config) {
						if (other.getName().equals("Url")) {
							return CodeBlock.builder().addStatement("return $N.url", parameter).build();
						} else {
							return super.from(other, parameter, config);
						}
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
				.modelValue("Max300CharString", new StandardModeller("Max300CharString",
						ClassName.get("com.pocket.sdk.api.value", "Max300CharString"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder()
								.addStatement("var str = asString($N)", parameter)
								.addStatement("return str == null ? null : new $T(str)", java()).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder()
								.addStatement("var str = asString($N)", parser)
								.addStatement("return str == null ? null : new $T(str)", java()).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.getValue()", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($N.getValue())", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $1T($2N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("FileField", new StandardModeller("FileField",
						ClassName.get("com.pocket.sdk.api.value", "FileField"),
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
						return CodeBlock.builder().addStatement("return $N.path", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.path)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("EmailAddress", new StandardModeller("EmailAddress",
						ClassName.get("com.pocket.sdk.api.value", "EmailString"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("DateString", new StandardModeller("DateString",
						ClassName.get("com.pocket.sdk.api.value", "DateString"),
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
						return CodeBlock.builder().addStatement("return $N.date", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.date)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
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
				.modelValue("HexColor", new StandardModeller("HexColor",
						ClassName.get("com.pocket.sdk.api.value", "HexColor"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("Url", new StandardModeller("Url",
						ClassName.get("com.pocket.sdk.api.value", "UrlString"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder()
								.addStatement("String str = asString($L)", parameter)
								.addStatement("return isBlank(str) ? null : new $T(str)", java()).build(); // Don't allow blank urls, convert them to nulls
					}
					@Override public CodeBlock fromParser(String parser) {
						return CodeBlock.builder()
								.addStatement("String str = asString($L)", parser)
								.addStatement("return isBlank(str) ? null : new $T(str)", java()).build(); // Don't allow blank urls, convert them to nulls
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.url", parameter).build();
					}
					@Override
					public Collection<String> supportedConversions() {
						return Collections.singletonList("String");
					}
					@Override
					public CodeBlock from(Value other, String parameter, Config config) {
						if (other.getName().equals("String")) {
							return CodeBlock.builder().addStatement("return isBlank($N) ? null : new $T($N)", parameter, java(), parameter).build();
						} else {
							return super.from(other, parameter, config);
						}
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.url)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("ValidUrl", new StandardModeller("ValidUrl",
						ClassName.get("com.pocket.sdk.api.value", "ValidUrl"),
						ClassName.get("java.lang", "String")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder()
								.addStatement("var str = asString($N)", parameter)
								.addStatement("return isBlank(str) ? null : new $T(str)", java()).build(); // Don't allow blank urls, convert them to nulls
					}
					@Override public CodeBlock fromParser(String parser) {
						return CodeBlock.builder()
								.addStatement("var str = asString($N)", parser)
								.addStatement("return isBlank(str) ? null : new $T(str)", java()).build(); // Don't allow blank urls, convert them to nulls
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.getUrl()", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.getUrl())", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $1T($2N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("LocalizedString", new StandardModeller("LocalizedString",
						ClassName.get("com.pocket.sdk.api.value", "LocalizedString"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("AccessToken", new StandardModeller("AccessToken",
						ClassName.get("com.pocket.sdk.api.value", "AccessToken"),
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
				.modelValue("Password", new StandardModeller("Password",
						ClassName.get("com.pocket.sdk.api.value", "Password"),
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
						return CodeBlock.builder().addStatement("return $N.password", parameter).build();
					}
					@Override
					public boolean isDangerous() {
						return true;
					}
					@Override
					public CodeBlock redact(String value, String encrypter) {
						return CodeBlock.builder().addStatement("return new $1T($1T.REDACTED)", java()).build();
					}
					@Override
					public CodeBlock unredact(String value, String encrypter) {
						return CodeBlock.builder().addStatement("return new $1T($1T.REDACTED)", java()).build(); // Once redacted we will never undo it
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $1T($1T.REDACTED)", java()).build();
					}
				})
				.modelValue("EscapedString", new StandardModeller("EscapedString",
						ClassName.get("com.pocket.sdk.api.value", "EscapedString"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("HtmlString", new StandardModeller("HtmlString",
						ClassName.get("com.pocket.sdk.api.value", "HtmlString"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("Html", new StandardModeller("Html",
						ClassName.get("com.pocket.sdk.api.value", "HtmlBlob"),
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
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.value)", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.modelValue("Data", new StandardModeller("Data",
						ClassName.get("com.pocket.sdk.api.value", "RawData"),
						ClassNames.JSON_NODE) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return new $T($N)", java(), parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return asData($1T.toJsonNode(asString($2L)))", ClassNames.BASE_MODELLER, parser).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.data", parameter).build();
					}
					@Override
					public CodeBlock immutable(String parameter) {
						return CodeBlock.builder().addStatement("return new $T($N.data.deepCopy())", java(), parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.data.toString())", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder()
								.beginControlFlow("try")
								.addStatement("return new $T(OBJECT_MAPPER.readTree($N.readString()))", java(), fieldname)
								.nextControlFlow("catch (Throwable t)")
								.addStatement("throw new $T(t)", RuntimeException.class)
								.endControlFlow()
								.build();
					}
				})
				.modelValue("Timestamp", new StandardModeller("Timestamp",
						ClassName.get("com.pocket.sdk.api.value", "Timestamp"),
						ClassName.get("java.lang", "Long")) {
					@Override
					public CodeBlock fromJson(String parameter) {
						return CodeBlock.builder().addStatement("return new $T($N.asLong())", java(), parameter).build();
					}
					@Override
					public CodeBlock fromParser(String parser) {
						return CodeBlock.builder().addStatement("return new $1T(asLong($2L))", java(), parser).build();
					}
					@Override
					public CodeBlock toJson(String parameter) {
						return CodeBlock.builder().addStatement("return $N.unixSeconds", parameter).build();
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
				.modelValue("Markdown", new StandardModeller("Markdown",
						ClassName.get("com.pocket.sdk.api.value", "MarkdownString"),
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
						return CodeBlock.builder().addStatement("return $N.getValue()", parameter).build();
					}
					@Override
					public CodeBlock compress(String writer, String value) {
						return CodeBlock.builder().addStatement("$N.writeString($L.getValue())", writer, value).build();
					}
					@Override
					public CodeBlock uncompress(String fieldname) {
						return CodeBlock.builder().addStatement("return new $T($N.readString())", java(), fieldname).build();
					}
				})
				.timeValue("Timestamp")
		);
	}
}
