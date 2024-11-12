package com.pocket.sync.print.java;

import com.pocket.sync.type.Definition;
import com.pocket.sync.type.DefinitionType;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.Syncable;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.path.Path;
import com.pocket.sync.type.path.PathSegment;
import com.pocket.sync.type.path.Reference;
import com.pocket.sync.type.path.ReferenceSegment;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.SourceVersion;

/**
 * Helper methods for generating java code from in-memory representations of the GraphQL definitions.
 */
public class GenUtil {
	
	public static ClassName innerclass(ClassName parent, String... inner) {
		for (String i : inner) {
			parent = createInnerClassName(parent, i);
		}
		return parent;
	}
	
	public static ClassName createInnerClassName(ClassName parent, String inner) {
		List<String> names = new ArrayList<>(parent.simpleNames());
		if (names.size() == 1) {
			return ClassName.get(parent.packageName(), parent.simpleName(), inner);
		} else {
			names.add(inner);
			String first = names.remove(0);
			String[] inners = new String[names.size()];
			names.toArray(inners);
			return ClassName.get(parent.packageName(), first, inners);
		}
	}
	
	/**
	 * Uppercase the first letter
	 * For any non valid character, replace it with an underscore
	 * For any underscores in the middle of the name, remove them and Uppercase the next letter
	 * @param value
	 * @return
	 */
	public static String toValidClassName(String value) {
		String result = "";
		boolean uppercaseNext = true;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (result.length() == 0) {
				if (!Character.isJavaIdentifierStart(c)) {
					continue;
				}
			} else if (!Character.isJavaIdentifierPart(c)) {
				c = '_';
				uppercaseNext = false;
			} else if (c == '_' && result.length() > 0) {
				uppercaseNext = true;
				continue;
			}
			if (uppercaseNext) {
				c = Character.toUpperCase(c);
				uppercaseNext = false;
			}
			result = result + c;
		}
		if (SourceVersion.isKeyword(result)) return result+"_";
		return result;
	}
	
	/**
	 * Same as {@link #toValidClassName(String)} but lowercases the first letter.
	 * @param value
	 * @return
	 */
	public static String toValidMethodName(String value) {
		value = toValidClassName(value);
		StringBuilder builder = new StringBuilder(value);
		builder.setCharAt(0, Character.toLowerCase(value.charAt(0)));
		return builder.toString();
	}

	public static TypeName toTypeName(Field field, Config config) {
		return toTypeName(field.getType(), config);
	}

	public static TypeName toTypeName(FieldType type, Config config) {
		return WhenType.is(type, TypeName.class)
				.map(m -> {
                    ClassName map = ClassName.get(Map.class);
                    ClassName string = ClassName.get(String.class);
                    TypeName inner = toTypeName(m.getInner(), config);
                    return ParameterizedTypeName.get(map, string, inner);
				})
		        .list(a -> {
                    ClassName list = ClassName.get(List.class);
                    TypeName inner = toTypeName(a.getInner(), config);
                    return ParameterizedTypeName.get(list, inner);
                })
                .variety(v -> config.variety(v))
				.enumm(e -> config.enumm(e))
                .thing(t -> config.thing(t))
                .value(v -> config.value(v).java())
                .otherwiseFail();
	}

	public static String toValidFieldName(Field field) {
		return toValidFieldName(field.getName());
	}

	public static String toValidFieldName(String value) {
		// Lowercase the first letter
		// For any non valid character, replace it with an underscore
		StringBuilder builder = new StringBuilder();
		int nextCase = -1; // 0 mean leave alone, 1 means upcase next, -1 means lowercase next
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (builder.length() == 0) {
				if (!Character.isJavaIdentifierStart(c)) {
					if (Character.isJavaIdentifierPart(c)) {
						builder.append("_");
					} else {
						continue;
					}
				}
			} else if (!Character.isJavaIdentifierPart(c)) {
				nextCase = 0;
				c = '_';
			}

			if (nextCase == 1) {
				c = Character.toUpperCase(c);
			} else if (nextCase == -1) {
				c = Character.toLowerCase(c);
			}
			nextCase = 0;
			builder.append(c);
		}
		
		final String result = builder.toString();
		if (SourceVersion.isKeyword(result)) {
			// just add an underscore to the end of it.
			return result+"_";
		}
		return result;
	}

	/**
	 * Converts a string to string suitable for use as a constant name.
	 * Where the format is ALL_CAPS_SEPARATED_BY_UNDERSCORES.
	 */
	public static String toValidConstantName(String value) {
		value = StringUtils.replace(value, "-", "_");
		value = GenUtil.toValidFieldName(value);
		// toValidFieldName() might add _ to the end if it matched a java keyword, but since we are all caps, we don't need to, so strip it out if it was added.
		if (value.endsWith("_") && SourceVersion.isKeyword(value.substring(0, value.length()-1))) {
			value = value.substring(0, value.length()-1);
		}
		value = value.toUpperCase();
		return value;

	}

	public static CodeBlock creatorFromJsonCode(FieldType type, Config config) {
		CodeBlock.Builder code = CodeBlock.builder();
		WhenType.is(type)
				.open(open -> code.add(OpenTypes.creatorReference(open, config)))
				.value(v -> code.add("$T.$N_CREATOR", config.modeller(), v.getName().toUpperCase()))
				.otherwise(t -> code.add("$T.JSON_CREATOR", toTypeName(type, config)));
		return code.build();
	}

	/**
	 * Returns a {@link CodeBlock} consisting of a comma, followed by the parameter name, if
	 * The supplied {@link FieldType} is a Thing (ie not a value or enum).
	 *
	 * @param type the {@link FieldType} to consider.
	 * @param paramName The name of the parameter to add.
	 * @return A {@link CodeBlock} with the supplied parameter, or an empty {@link CodeBlock}
	 */
	public static CodeBlock addParamIfThing(FieldType type, String paramName) {
		CodeBlock.Builder code = CodeBlock.builder();
		WhenType.is(type)
				.value(v -> {})
				.enumm(e -> {})
				.otherwise(t -> code.add(", $N", paramName));
		return code.build();
	}

	public static CodeBlock streamingCreatorFromJsonCode(FieldType type, Config config) {
		CodeBlock.Builder code = CodeBlock.builder();
		WhenType.is(type)
				.open(open -> code.add(OpenTypes.creatorReference(open, config)))
				.value(v -> code.add("$T.$N_STREAMING_CREATOR", config.modeller(), v.getName().toUpperCase()))
				.thing(t -> code.add("$T.STREAMING_JSON_CREATOR", config.thing(t)))
				.enumm(e -> code.add("$T.STREAMING_JSON_CREATOR", config.enumm(e)))
				.otherwiseFail();
		return code.build();
	}
	
	public static CodeBlock creatorFromBuffer(FieldType type, Config config) {
		CodeBlock.Builder code = CodeBlock.builder();
		WhenType.is(type)
				.open(open -> code.add(OpenTypes.creatorReference(open, config)))
				.value(value -> code.add("$T.$N_BYTE_CREATOR", config.modeller(), value.getName().toUpperCase()))
				.thing(thing -> code.add("$T.BYTE_CREATOR", config.thing(thing)))
				.enumm(enumm -> code.add("$T.BYTE_CREATOR", config.enumm(enumm)))
				.otherwiseFail();
		return code.build();
	}

	/**
	 * Produces code that can safely access checking if a path is declared, avoiding null pointers and out of index errors.
	 * Returns code whose result is a boolean value of whether or not the path is declared.
	 * Examples:
	 * A path of `.field` would return `declared.field`
	 * A path of `.field.field` would return `field != null && field.declared.field`
	 * A path of `.field[1]` would return `field != null && field.declared.field && field.size() > 1`
	 * A path of `.field["key"]` would return `field != null && field.field != null && field.contains("key")`
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field` will throw an error. See ___ for handling that case.
	 *
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 */
	public static CodeBlock safePathDeclared(Thing root, String rootVar, Path path) {
		Reference typed = new Reference(root, path);
		CodeBlock safeChecks = safePathChecks(root, rootVar, path);
		CodeBlock.Builder code = CodeBlock.builder();
		
		ReferenceSegment end = typed.end();
		
		if (safeChecks.isEmpty()) {
			switch(end.getSegment().getType()) {
				case FIELD:
					if (rootVar != null) {
						code.add("$N != null && $N.declared.$N", rootVar, rootVar, end.getSegment().getValue());
					} else {
						code.add("declared.$N", end.getSegment().getValue());
					}
					break;
				case MAP_KEY:
				case ARRAY_INDEX:
					throw new RuntimeException("unexpected non deep reference");
			}
		} else {
			code.add(safeChecks);
			code.add(" && ");
			switch(end.getSegment().getType()) {
				case FIELD:
					code.add("$L.declared.$N", unsafeReference(rootVar, end.getSegment().getParent()), end.getSegment().getValue());
					break;
				case MAP_KEY:
					code.add("$L.contains($S)", unsafeReference(rootVar, end.getSegment().getParent()), end.getSegment().getValue());
					break;
				case ARRAY_INDEX:
					code.add("$L.size() > $L", unsafeReference(rootVar, end.getSegment().getParent()), end.getSegment().getValue());
					break;
			}
		}
		return code.build();
	}
	
	/**
	 * Produces code that can safely access checking if a path is declared, avoiding null pointers and out of index errors.
	 * Handles crossing the boundary of collections such as `.field[1].field`.
	 * Returns code that results in a variable being assigned with whether or not the path is declared.
	 * Examples:
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field.field` uses temporary variables and will return code like:
	 * ```
	 * FieldType f_tmp_1 = t != null && t.field != null && t.field.size() > 1 ? t.field.get(1) : null;
	 * boolean f = field_1 != null && field_1.field != null ? field_1.field.declared.field;
	 * ```
	 * @param var The name of the variable to create and assign this value to. It will be typed correctly to the value.
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 * @param config Gen config used for determining class types
	 * @param outputs TODO document what this is
	 */
	public static CodeBlock safePathDeclaredVariable(String var, Thing root, String rootVar, Path path, Config config, String[] outputs) {
		List<List<ReferenceSegment>> parts = new Reference(root, path).splitCollections();
		parts = new ArrayList<>(parts); // mutable
		CodeBlock.Builder code = CodeBlock.builder();
		List<ReferenceSegment> last = parts.remove(parts.size()-1);
		if (!parts.isEmpty()) {
			String tmp = var+"_tmp";
			code.add(safePathValueVariable(tmp, root, rootVar, last.get(0).getParent().getSegment().path(), config));
			rootVar = tmp;
		}
		
		ReferenceSegment start = last.get(0);
		ReferenceSegment end = last.get(last.size()-1);
		Path subpath = Path.Companion.from(end.getSegment().path().toString().substring(start.getSegment().getParent().toString().length()));
		code.add("$T $N = ", boolean.class, var);
		code.add(safePathDeclared((Thing) start.getRoot(), rootVar, subpath));
		code.add(";\n");
		
		if (outputs != null) {
			outputs[0] = rootVar;
			outputs[1] = subpath.toString();
		}
		
		return code.build();
	}
	
	/**
	 * Produces code that can safely access a path's value, avoiding null pointers and out of index errors.
	 * Returns code that results in the path's value or null if unreachable.
	 * Examples:
	 * A path of `.field.field` would return `t != null && t.field != null && t.field.field != null ? t.field.field : null`
	 * A path of `.field[1]` would return `t != null && t.field != null && t.field.size() > 1 ? t.field.get(1) : null`
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field` will throw an error. See {@link #safePathValueVariable(String, Thing, String, Path, Config)} for handling that case.
	 *
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 */
	public static CodeBlock safePathValue(Thing root, String rootVar, Path path) {
		Reference typed = new Reference(root, path);
		CodeBlock safety = safePathChecks(root, rootVar, path);
		ReferenceSegment end = typed.end();
		
		CodeBlock.Builder code = CodeBlock.builder();
		
		if (!safety.isEmpty()) {
			code.add(safety);
			code.add(" ? ");
		}
		
		switch (end.getSegment().getType()) {
			case FIELD:
				code.add("$N", unsafeReference(rootVar, end.getSegment().path()));
				break;
			case MAP_KEY:
				code.add("$N.get($S)", unsafeReference(rootVar, end.getParent().getSegment().path()), end.getSegment().getValue());
				break;
			case ARRAY_INDEX:
				code.add("$N.get($L)", unsafeReference(rootVar, end.getParent().getSegment().path()), end.getSegment().getValue());
				break;
		}
		
		if (!safety.isEmpty()) {
			code.add(" : null");
		}
		return code.build();
	}
	
	/**
	 * Produces code that can safely access a path's value, avoiding null pointers and out of index errors.
	 * Handles crossing the boundary of collections such as `.field[1].field`.
	 * Returns code that results in a variable being assigned with the path's value or null if unreachable.
	 * Examples:
	 *
	 * A path of `.field.field` would return `FieldType f = t != null && t.field != null && t.field.field != null ? t.field.field : null;`
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field.field` uses temporary variables and will return code like:
	 *
	 * ```
	 * FieldType f_tmp_1 = t != null && t.field != null && t.field.size() > 1 ? t.field.get(1) : null;
	 * FieldType f = field_1 != null && field_1.field != null ? field_1.field.field : null;
	 * ```
	 * Where f is the final value.
	 *
	 * @param var The name of the variable to create and assign this value to. It will be typed correctly to the value.
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 * @param config Gen config used for determining class types
	 */
	public static CodeBlock safePathValueVariable(String var, Thing root, String rootVar, Path path, Config config) {
		List<List<ReferenceSegment>> parts = new Reference(root, path).splitCollections();
		CodeBlock.Builder code = CodeBlock.builder();
		for (int i = 0; i < parts.size(); i++) {
			List<ReferenceSegment> p = parts.get(i);
			String name = i+1 < parts.size() ? var+"_tmp_"+(i+1) : var;
			ReferenceSegment start = p.get(0);
			ReferenceSegment end = p.get(p.size()-1);
			Path subpath = Path.Companion.from(end.getSegment().path().toString().substring(start.getSegment().getParent().toString().length()));
			code.add("$T $N = ", GenUtil.toTypeName(end.getType(), config), name);
			code.add(safePathValue((Thing) start.getRoot(), rootVar, subpath));
			code.add(";\n");
			rootVar = name;
		}
		return code.build();
	}
	
	/**
	 * Produces code that checks if a path can safely be accessed, avoiding null pointers and out of index errors.
	 * Returns code as a boolean that the full path can be safely accessed.
	 * Examples:
	 * A path of `.field.field` would return `t != null && t.field != null && t.field.field != null`
	 * A path of `.field[1]` would return `t != null && t.field != null && t.field.size() > 1`
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field` will throw an error. See {@link #safePathCheckVariable(String, Thing, String, Path, Config)} for handling that case.
	 *
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 */
	public static CodeBlock safePathChecks(Thing root, String rootVar, Path path) {
		Reference typed = new Reference(root, path);
		CodeBlock.Builder code = CodeBlock.builder();
		if (rootVar != null) {
			code.add("$N != null", rootVar);
		}
		for (int i = 0; i < path.getParts().size(); i++) {
			ReferenceSegment segment = typed.getPath().getSyncable().get(i);
			ReferenceSegment next = i + 1 < typed.getPath().getSyncable().size() ? typed.getPath().getSyncable().get(i) : null;
			
			if (segment.getMode() == ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD) throw new RuntimeException("Reactive collection searches not supported.");
			
			if (next == null) {
				switch (segment.getSegment().getType()) {
					case ARRAY_INDEX:
						if (i > 0 || rootVar != null) code.add(" && ");
						code.add("$N.size() > $L", unsafeReference(rootVar, segment.getParent().getSegment().path()), segment.getSegment().getValue());
						break;
					
					case FIELD:
					case MAP_KEY:
						// Nothing further to add
						break;
				}
			} else {
				switch (segment.getSegment().getType()) {
					case FIELD:
						if (i > 0 || rootVar != null) code.add(" && ");
						code.add("$N != null", unsafeReference(rootVar, segment.getSegment().path()));
						break;
					case MAP_KEY:
					case ARRAY_INDEX:
						throw new RuntimeException("Crosses boundary of collection. Use safeFieldReferenceVariable instead.");
				}
			}
		}
		return code.build();
	}

	/**
	 * Creates a direct field reference string from a path.
	 * Assumes only field parts in the path.
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 */
	public static String unsafeReference(String rootVar, Path path) {
		StringBuilder b = new StringBuilder();
		if (rootVar != null) {
			b.append(rootVar);
			if (path.getParts().size() > 0) b.append(".");
		}
		for (int i = 0, len = path.getParts().size(); i < len; i++) {
			PathSegment part = path.getParts().get(i);
			switch (part.getType()) {
				case FIELD:
					if (i > 0) b.append(".");
					b.append(toValidFieldName(part.getValue()));
					break;
				case MAP_KEY:
					b.append(".get(\"").append(part.getValue()).append("\")");
					break;
				case ARRAY_INDEX:
					b.append(".get(").append(part.getValue()).append(")");
					break;
			}
		}
		return b.toString();
	}
	
	/**
	 * Produces code that checks if a path can safely be accessed, avoiding null pointers and out of index errors.
	 * Handles crossing the boundary of collections such as `.field[1].field`.
	 * Returns code as that assigns a boolean variable true if the path can be safely accessed, false if not.
	 * Examples:
	 *
	 * A path of `.field.field` would return `FieldType f = t != null && t.field != null && t.field.field != null`
	 *
	 * A path that crosses the boundary of a collection, such as `.field[1].field.field` uses temporary variables and will return code like:
	 *
	 * ```
	 * FieldType f_tmp_1 = t != null && t.field != null && t.field.size() > 1 ? t.field.get(1) : null;
	 * boolean f = field_1 != null && field_1.field != null;
	 * ```
	 * Where f is the final value.
	 *
	 * @param var The name of the variable to create and assign this value to. It will be typed correctly to the value.
	 * @param root The thing this field belongs to
	 * @param rootVar The name of a variable that represents this thing, or null if in the context the thing is "this"
	 * @param path The path to the field to access
	 * @param config Gen config used for determining class types
	 */
	public static CodeBlock safePathCheckVariable(String var, Thing root, String rootVar, Path path, Config config) {
		List<List<ReferenceSegment>> parts = new Reference(root, path).splitCollections();
		CodeBlock.Builder code = CodeBlock.builder();
		for (int i = 0; i < parts.size(); i++) {
			List<ReferenceSegment> p = parts.get(i);
			String name = i+1 < parts.size() ? var+"_tmp_"+(i+1) : var;
			ReferenceSegment start = p.get(0);
			ReferenceSegment end = p.get(p.size()-1);
			Path subpath = Path.Companion.from(end.getSegment().path().toString().substring(start.getSegment().getParent().toString().length()));
			code.add("boolean $N = ", name);
			code.add(safePathChecks((Thing) start.getRoot(), rootVar, subpath));
			code.add(";\n");
			rootVar = name;
			
		}
		return code.build();
	}
	
	/**
	 * Produces a code block that opens an if block. The if statement evaluates whether or not a path has changed value.
	 * Depending on how simple or complex the path is, this may be one line or it may be several with local variables introduced.
	 * Important: This check assumes the "is" value is non-null and "was" is nullable. This was primarily written for Thing.reactions().
	 *
	 * Example:
	 * <pre>
	 *     Given parameters of:
	 *     	wasVar = "was"
	 *     	isVar = "is"
	 *     	path = ".field"
	 *
	 *     It would produce:
	 *	     if (is.declared.field && (was == null || !was.declared.field || ObjectUtils.notEqual(was.field, is.field))) {
	 * </pre>
	 *
	 * @param root The type of thing being compared
	 * @param wasVar The variable name of the previous instance of this thing
	 * @param isVar The variable name of the current instance of this thing
	 * @param diffVar The variable name of an instance of RichDiff this code can access
	 * @param typedPath The path to the value to compare
	 * @param var A variable name prefix that can be used for any local variables that need to be created
	 */
	public static CodeBlock ifPathValueChanged(Thing root, String wasVar, String isVar, String diffVar, Reference typedPath, String var, Config config) {
		return ifPathValue(true, root, wasVar, isVar, diffVar, typedPath, var, config);
	}
	
	/**
	 * Same as {@link #ifPathValueChanged(Thing, String, String, String, Reference, String, Config)} but will flip the condition it is checking and the if statement will
	 * check if the path did NOT change. (Again with the same assumptions about was/is nullability)
	 */
	public static CodeBlock ifPathValueNotChanged(Thing root, String wasVar, String isVar, String diffVar, Reference typedPath, String var, Config config) {
		return ifPathValue(false, root, wasVar, isVar, diffVar, typedPath, var, config);
	}
	
	private static CodeBlock ifPathValue(boolean changed, Thing root, String wasVar, String isVar, String diffVar, Reference typedPath, String var, Config config) {
		Path path = typedPath.toPath();
		if (typedPath.splitCollectionSearches().size() > 1) {
			// Collection search
			// This means it is going to look within a collection of Things to see if a specific value within any of those Things changed
			if (typedPath.splitCollectionSearches().size() > 2) throw new RuntimeException("only supports 1 deep collection search");
			
			// We want to get 2 references,
			// The first is a reference to the collection itself
			// The second is a reference, that starts within the Thing that the collection is of, to the value within that thing to check.
			List<Reference> split = typedPath.splitCollectionSearches();
			Reference collection = split.get(0); // A reference to the collection
			Reference value = split.get(1); // From the context of within that collection, a reference the value to check
			
			CodeBlock.Builder b = CodeBlock.builder();
			String tmp = var + "_tmp";
			b.addStatement("boolean $N = false", var);
			b.add(safePathValueVariable(tmp, root, wasVar, collection.toPath(), config));
			b.beginControlFlow("if ($N != null)", tmp);
				b.beginControlFlow("for ($T t : $N)", config.thing((Thing) value.start().getRoot()), tmp);
					b.addStatement("$T i = diff.find(t)", ParameterizedTypeName.get(ClassNames.CHANGE, config.thing((Thing) value.start().getRoot())));
					b.beginControlFlow("if (i != null)");
						b.add(ifPathValueChanged((Thing) value.start().getRoot(), "i.previous", "i.latest", null, value, var, config));
							b.addStatement("$N = true", var);
							b.addStatement("break");
						b.endControlFlow();
					b.endControlFlow();
				b.endControlFlow();
			b.endControlFlow();
			b.beginControlFlow("if ($N)", var);
			return b.build();
			
		} else if (typedPath.splitCollections().size() > 1) {
			// Requires local variables
			CodeBlock.Builder b = CodeBlock.builder();
			String[] output = new String[2];
			String dIs = var+"__is_declared";
			String dWas = var+"__was_declared";
			b.addStatement("boolean $N = false", var);
			b.add(safePathDeclaredVariable(dIs, root, isVar, path, config, output));
			String isValueRoot = output[0];
			Path isValueSubpath = output[1] != null ? Path.Companion.from(output[1]) : path;
			b.beginControlFlow("if ($N)", dIs);
			b.add("if ($N == null) $N = true; \n", wasVar, var);
			b.beginControlFlow("else");
			b.add(safePathDeclaredVariable(dWas, root, wasVar, path, config, output));
			String wasValueRoot = output[0];
			Path wasValueSubpath = output[1] != null ? Path.Companion.from(output[1]) : path;
			b.addStatement("$N = $N && $T.notEqual($N, $N)", var, dWas, ClassNames.OBJECT_UTIL, unsafeReference(isValueRoot, isValueSubpath),  unsafeReference(wasValueRoot, wasValueSubpath));
			b.endControlFlow();
			b.endControlFlow();
			b.beginControlFlow(changed ? "if ($N)" : "if (!$N)", var);
			return b.build();
		} else {
			// Simple direct field check
			CodeBlock.Builder b = CodeBlock.builder();
			b.add("if (");
			if (!changed) b.add("!(");
			b.add("$L && ($N == null || !($L) || $T.notEqual($L, $L))",
					safePathDeclared(root, isVar, path), wasVar, safePathDeclared(root, wasVar, path), ClassNames.OBJECT_UTIL,
						safePathValue(root, wasVar, path), safePathValue(root, isVar, path));
			if (!changed) b.add(")");
			b.add(") {\n");
			b.indent();
			return b.build();
		}
	}









	public static boolean containsDangerousValues(FieldType type, Config config) {
		return containsDangerousValues(type, config, new HashSet<>());
	}
	public static boolean containsDangerousValues(Thing thing, Config config) {
		return containsDangerousValues(thing, config, new HashSet<>());
	}
	/** Uses a set to keep track of what it has already checked to avoid infinite loops. */
	private static boolean containsDangerousValues(FieldType type, Config config, Set<Definition> checked) {
		return WhenType.is(type, boolean.class)
				.value(v -> config.value(v).isDangerous())
				.open(open -> {
					if (open instanceof DefinitionType) {
						Definition d = ((DefinitionType) open).getDefinition();
						if (!checked.add(d)) return false; // Already checked
						if (d instanceof Syncable) {
							for (Field field : config.mode.getActiveFields((Syncable) d)) {
								if (containsDangerousValues(field.getType(), config, checked)) return true;
							}
						}
					}
					// Also need to see if any of the implementations of this have dangerous values
					for (Thing t : open.compatible()) {
						if (containsDangerousValues(t, config, checked)) return true;
					}
					return false;
				})
				.collection(collection -> containsDangerousValues(collection.getInner(), config, checked))
				.thing(thing -> containsDangerousValues(thing, config, checked))
				.otherwise(t -> false);
	}
	private static boolean containsDangerousValues(Thing thing, Config config, Set<Definition> checked) {
		if (!checked.add(thing)) return false; // Already checked
		for (Field field : config.mode.getActiveFields(thing)) {
			if (containsDangerousValues(field.getType(), config, checked)) return true;
		}
		return false;
	}

	public static boolean isOrIsCollectionOfDangerousValues(FieldType type, Config config) {
		return WhenType.is(type, boolean.class)
				.value(v -> config.value(v).isDangerous())
				.collection(c -> WhenType.is(c.getInner(), boolean.class)
					.value(v -> config.value(v).isDangerous())
					.otherwise(t -> false))
				.otherwise(t -> false);


	}

	/**
	 * Creates a String, for use in a code statment, which calls a Thing's static REMOTE field's toAlias method,
	 * IF the Field has aliases, otherwise just returns the field name itself.  This is used when parsing to Json,
	 * allowing generated code to give the aliased name, instead of the local field name.
	 *
	 * For example, a field with NO aliases can be referenced as the String:
	 *
	 * "no_aliases_field"
	 *
	 * Whereas, when parsing a similar field to Json which has a potential alias, generated code can say:
	 *
	 * REMOTE.toAlias("has_aliases_field", config.getRemote())
	 *
	 * Both of which return a String.
	 *
	 * @param field the {@link Field}
	 * @param remoteParam a reference to the RemoteStyle to be supplied to the Remote.toAlias method.
	 * @return a String, for use in a code statement, which gives the name of the field as a String
	 * literal, or a method call get to get its alias.
	 */
	public static String aliasedNameCode(Field field, String remoteParam) {
		String fieldByAlias = "\"" + field.getName() + "\"";
		if (!field.getAliases().isEmpty()) {
			fieldByAlias = "REMOTE.toAlias(" + fieldByAlias + ", " + remoteParam + ")";
		}
		return fieldByAlias;
	}
}
