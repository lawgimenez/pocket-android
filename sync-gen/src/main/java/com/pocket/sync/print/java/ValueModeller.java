package com.pocket.sync.print.java;

import com.pocket.sync.type.Value;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;

import java.util.Collection;

/**
 * Instructions for how to represent a {@link com.pocket.sync.type.Value} in java code.
 */
public interface ValueModeller {
	
	/** Return the class/type name of what this value is in java. */
	TypeName java();
	
	/** Return the class/type name of what this value should be when putting into json */
	TypeName json();
	
	/** Whether or not this value can be null.*/
	boolean isNullable();
	
	/** Whether or not this value contains sensitive details that need extra protection. See Thing.Include in the com.pocket.sync library for more details. */
	boolean isDangerous();
	
	/**
	 * Code that will take a non-null instance of the value and return a redacted value that is safe for storing to disk.
	 * This will only be called if {@link #isDangerous()} is true.
	 * The code block must end with a return of the value.
	 * @param value A variable name referencing an instance of this value. It will be non-null.
	 * @param encrypter A variable name referencing an instance of StringEncrypter that can be used if needed.
	 * @return Conversion code or null if there is no redaction needed for this value type.
	 */
	CodeBlock redact(String value, String encrypter);
	
	/**
	 * Code that will take a non-null instance of the value and return a value that undoes/reverses what {@link #redact(String, String)} did.
	 * The code block must end with a return of the value.
	 * If {@link #redact(String, String)} returns null, this method will not be needed or invoked.
	 * @param value A variable name referencing an instance of this value. It will be non-null.
	 * @param encrypter A variable name referencing an instance of StringEncrypter that can be used if needed.
	 * @return Conversion code or null if there is no redaction needed for this value type.
	 */
	CodeBlock unredact(String value, String encrypter);
	
	/**
	 * Code that will take a jackson json JsonNode with the parameter name
	 * and return a new immutable instance of the type defined in {@link #java()}
	 * Can assume the parameter is non-null and the json type is not null.
	 * The code block must end with a return of the value.
	 * @param parameter The name of the JsonNode
	 * @return Conversion code.
	 */
	CodeBlock fromJson(String parameter);
	
	/**
	 * Code that will take a jackson json JsonParser with the parameter name
	 * and return a new immutable instance of the type defined in {@link #java()}
	 * Can assume the parameter is non-null, it is set on the current token and the current token is not a null token.
	 * The code block must end with a return of the value.
	 * @param parser The name of the JsonParser
	 * @return Conversion code.
	 */
	CodeBlock fromParser(String parser);
	
	/**
	 * Code that will take a the type defined in {@link #java()} with the parameter name
	 * and return an instance of the type defined in {@link #json()}
	 * Can assume the parameter is non-null.
	 * The code block must end with a return of the value.
	 * @param parameter The name of the value
	 * @return Conversion code.
	 */
	CodeBlock toJson(String parameter);
	
	/**
	 * Code that will take a the type defined in {@link #java()} with the parameter name
	 * and return an instance an immutable instance of the same type.
	 * Can assume the parameter is non-null. If already immutable can just return the parameter as is.
	 * The code block must end with a return of the value.
	 * @param parameter The name of the value
	 * @return Conversion code.
	 */
	CodeBlock immutable(String parameter);
	
	/**
	 * Code that will take a the type defined in {@link #java()} with the parameter name
	 * and return true or false whether or not the value is blank.
	 * Blank here is whatever you want blank to be if this value is used as part of a {@link com.pocket.sync.type.FirstAvailable} function.
	 * The code block must end with a return of the value.
	 * @param parameter The name of the value
	 * @return Conversion code.
	 */
	CodeBlock isBlank(String parameter);
	
	/**
	 * Code that will take a variable name `parameter`, of a type `other` and convert it to the the {@link #java()} type.
	 * Can assume the parameter is non-null.
	 * The code block must end with a return of the value.
	 * May throw an exception if converting from that type is not supported.
	 * @param other The type of value to convert from
	 * @param parameter The name of the value in code
	 * @param config Config details if needed for referencing other classes
	 * @return Conversion code.
	 */
	CodeBlock from(Value other, String parameter, Config config);
	
	/**
	 * @return A list of other {@link Value#getName()} that {@link #from(Value, String, Config)} supports. Can be empty.
	 */
	Collection<String> supportedConversions();
	
	/**
	 * Return code that will use methods on the ByteWriter in a variable with the provided name (in `writer`) to write out this non-null value which is in a variable named by `value`.
	 */
	CodeBlock compress(String writer, String value);
	
	/**
	 * Return code that will access values on the ByteReader in a variable with the provided name to read in and return this non-null value.
	 */
	CodeBlock uncompress(String fieldname);
	
	/**
	 * Return code that will return a `true` or `false` for the variable `value`'s value.
	 * Only needed if {@link #isBoolean()} is true.
	 */
	CodeBlock asBoolean(String value);
	
	/**
	 * @return true if this value can be presented by a boolean. If true, you must also implement
	 */
	boolean isBoolean();

	/**
	 * @return If your API supports graphql, the Scalar name of this value in GraphQL.
	 */
	String graphQlType();
}
