package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Auth;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

/**
 * Creates an enum with all of the declared auth types.
 */
public class AuthTypeGenerator {

	public static ClassName classname(Config config) {
		return config.helper(config, "AuthType");
	}

	public static String enumvalue(Auth auth) {
		String name = auth != null ? auth.getName() : "DEFAULT";
		name = name.replaceAll("Auth$", ""); // Trim off trailing "Auth" since it is redundant in an enum context.
		StringBuilder b = new StringBuilder();
		for (int i = 0, len = name.length(); i < len; i++) {
			char c = name.charAt(i);
			if (i > 0 && Character.isUpperCase(c)) {
				b.append('_');
			}
			b.append(Character.toUpperCase(c));
		}
		return b.toString();
	}

	protected final TypeSpec.Builder typeSpec;
	protected final ClassName className;
	protected final Figments figments;
	protected final Config config;

	public AuthTypeGenerator(Config config, Figments figments) {
		this.className = classname(config);
		this.typeSpec = TypeSpec.enumBuilder(className)
					.addModifiers(Modifier.PUBLIC)
					.addSuperinterface(ClassNames.AUTH_TYPE);
		this.figments = figments;
		this.config = config;

		for (Auth type : figments.auths()) {
			typeSpec.addEnumConstant(enumvalue(type),
					// Then this kind of verbose thing, but appears to be the way to add a javadoc to an enum constant
					TypeSpec.anonymousClassBuilder("").addJavadoc(JavaDescriptionPrinter.print(type.getDescription(), config)).build());
		}
		if (figments.auths().isEmpty()) typeSpec.addEnumConstant(enumvalue(null));
	}

	public TypeSpec getTypeSpec() {
		return typeSpec.build();
	}

}
