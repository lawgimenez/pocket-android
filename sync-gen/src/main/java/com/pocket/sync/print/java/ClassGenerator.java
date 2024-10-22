package com.pocket.sync.print.java;

import com.pocket.sync.Figments;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

/**
 * Base class for generated classes
 */
public abstract class ClassGenerator {

	protected final TypeSpec.Builder typeSpec;
	protected final ClassName className;
	protected final Figments figments;
	protected final Config config;

	public ClassGenerator(ClassName name, Config config, Figments figments) {
		this(TypeSpec.classBuilder(name), name, config, figments);
	}
	
	public ClassGenerator(TypeSpec.Builder typeSpec, ClassName name, Config config, Figments figments) {
		this.className = name;
		this.typeSpec = typeSpec;
		this.figments = figments;
		this.config = config;
	}

	public TypeSpec.Builder getTypeBuilder() {
		return typeSpec;
	}

	public TypeSpec getTypeSpec() {
		return typeSpec.build();
	}

}
