package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.pocket.sync.type.Remote;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;

/**
 * Creates an enum with all of the declared endpoint styles.
 */
public class RemoteStyleGenerator {

	public static ClassName classname(Config config) {
		return config.helper(config, "RemoteStyle");
	}

	public static String enumvalue(Remote remote) {
		if (remote == null) return "DEFAULT";
		return remote.getName().toUpperCase();
	}

	protected final TypeSpec.Builder typeSpec;
	protected final ClassName className;
	protected final Figments figments;
	protected final Config config;

	public RemoteStyleGenerator(Config config, Figments figments) {
		this.className = classname(config);
		this.typeSpec = TypeSpec.enumBuilder(className)
					.addModifiers(Modifier.PUBLIC)
					.addSuperinterface(ClassNames.STYLE);
		this.figments = figments;
		this.config = config;

		for (Remote type : figments.remotes()) {
			typeSpec.addEnumConstant(enumvalue(type),
					// Then this kind of verbose thing, but appears to be the way to add a javadoc to an enum constant
					TypeSpec.anonymousClassBuilder("").addJavadoc(JavaDescriptionPrinter.print(type.getDescription(), config)).build());
		}
		if (figments.remotes().isEmpty()) typeSpec.addEnumConstant(enumvalue(null));
	}

	public TypeSpec getTypeSpec() {
		return typeSpec.build();
	}

}
