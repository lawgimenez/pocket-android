package com.pocket.sync.print.java;

import com.pocket.sync.type.Value;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A {@link ValueModeller} with some simple default handling for some of the methods.
 * <ul>
 *     <li>{@link #isNullable()} is true.</li>
 *     <li>{@link #toJson(String)}'s implementation just returns the value as is.</li>
 *     <li>{@link #immutable(String)}'s implementation just returns the value as is.</li>
 *     <li>{@link #isBlank(String)}'s implementation is only blank if the parameter is null.</li>
 *     <li>{@link #supportedConversions()} returns empty</li>
 *     <li>{@link #from(com.pocket.sync.type.Value, String, Config)} throws an unsupported exception</li>
 *     <li>{@link #isDangerous()} returns false </li>
 * </ul>
 *
 * All of which can be overridden.  Everything else must still be implemented.
 */
public abstract class StandardModeller implements ValueModeller {
	
	private final String name;
	private final TypeName java;
	private final TypeName json;
	
	public StandardModeller(String name, TypeName java, TypeName json) {
		this.name = name;
		this.java = java;
		this.json = json;
	}
	
	@Override public TypeName java() { return java; }
	@Override public TypeName json() { return json; }
	@Override public boolean isNullable() { return true; }
	
	@Override
	public CodeBlock toJson(String parameter) {
		return CodeBlock.builder().addStatement("return $N", parameter).build();
	}
	
	@Override
	public CodeBlock immutable(String parameter) {
		return CodeBlock.builder().addStatement("return $N", parameter).build();
	}
	
	@Override
	public CodeBlock isBlank(String parameter) {
		return CodeBlock.builder().addStatement("return false").build(); // Null checks are included, so if this runs, it is always non-null, and not-blank
	}
	
	@Override
	public CodeBlock from(Value other, String parameter, Config config) {
		throw new UnsupportedOperationException("unable to convert from " + other);
	}
	
	@Override
	public Collection<String> supportedConversions() {
		return new ArrayList<>();
	}
	
	@Override
	public boolean isDangerous() {
		return false;
	}
	
	@Override
	public CodeBlock redact(String value, String encrypter) {
		return null;
	}
	
	@Override
	public CodeBlock unredact(String value, String encrypter) {
		return null;
	}
	
	@Override
	public boolean isBoolean() {
		return false;
	}
	
	@Override
	public CodeBlock asBoolean(String value) {
		return null;
	}

	@Override
	public String graphQlType() {
		return name;
	}
}
