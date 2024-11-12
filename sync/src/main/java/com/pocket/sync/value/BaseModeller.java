package com.pocket.sync.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.protect.StringEncrypter;
import com.pocket.util.java.Logs;
import com.pocket.util.java.JsonUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modeller methods that are easier to write by hand instead of code-generating.
 * @see com.pocket.sdk.api.generated.Modeller
 */
public class BaseModeller {

	public static final ObjectMapper OBJECT_MAPPER = JsonUtil.getObjectMapper();
	
	/**
	 * Create a [Type]
	 * @return a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> asList(JsonNode value, TypeParser<T> creator) {
		if (isNull(value)) {
			return null;

		} else if (value.isObject()) {
			// Server sometimes returns lists as maps.
			// In this case, ignore the keys and just get the values.
			Map<String, T> map = asMap(value, creator);
			return map != null ? new ArrayList<>(map.values()) : null;

		} else {
			ArrayNode array = (ArrayNode) value;
			ArrayList<T> list = new ArrayList<>();
			for (JsonNode node : array) {
				list.add(creator.create(node));
			}
			return list;
		}
	}
	
	/**
	 * Create a [Type]
	 * @return a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> asList(JsonParser parser, StreamingTypeParser<T> creator) throws IOException {
		if (parser.currentToken() == JsonToken.VALUE_NULL) {
			return null;
			
		} else if (parser.isExpectedStartObjectToken()) {
			// Server sometimes returns lists as maps.
			// In this case, ignore the keys and just get the values.
			Map<String, T> map = asMap(parser, creator);
			return map != null ? new ArrayList<>(map.values()) : null;

		} else if (parser.isExpectedStartArrayToken()) {
			ArrayList<T> list = new ArrayList<>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				list.add(creator.create(parser));
			}
			return list;
			
		} else {
			throw new RuntimeException("Unable to parse as list.");
		}
	}
	
	/**
	 * Create a [Type]
	 * @return a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> asList(JsonNode value, SyncableParser<T> creator, JsonConfig config, Allow... allowed) {
		if (isNull(value)) {
			return null;

		} else if (value.isObject()) {
			// Server sometimes returns lists as maps.
			// In this case, ignore the keys and just get the values.
			Map<String, T> map = asMap(value, creator, config, allowed);
			return map != null ? new ArrayList<>(map.values()) : null;

		} else {
			ArrayNode array = (ArrayNode) value;
			ArrayList<T> list = new ArrayList<>();
			for (JsonNode node : array) {
				list.add(creator.create(node, config, allowed));
			}
			return list;
		}
	}
	
	/**
	 * Create a [Type]
	 * @return a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> asList(JsonParser parser, StreamingThingParser<T> creator, JsonConfig config, Allow... allowed) throws IOException {
		if (parser.currentToken() == JsonToken.VALUE_NULL) {
			return null;
			
		} else if (parser.isExpectedStartObjectToken()) {
			// Server sometimes returns lists as maps.
			// In this case, ignore the keys and just get the values.
			Map<String, T> map = asMap(parser, creator, config, allowed);
			return map != null ? new ArrayList<>(map.values()) : null;

		} else if (parser.isExpectedStartArrayToken()) {
			ArrayList<T> list = new ArrayList<>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				list.add(creator.create(parser, config, allowed));
			}
			return list;
			
		} else {
			throw new RuntimeException("Unable to parse as list.");
		}
	}
	
	/**
	 * Implements a remap as described in the sync docs
	 * @param value The list of things (since v3 sometimes returns lists as key:value that is also supported)
	 * @param field The field within the thing to extract
	 * @param creator The creator for that field's value
	 * @param <T>
	 * @return A list of extracted values as a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> remap(JsonNode value, String field, TypeParser<T> creator) {
		if (isNull(value)) return null;
		
		boolean declared = false;
		ArrayList<T> list = new ArrayList<>();
		
		if (value.isObject()) {
			ObjectNode obj = (ObjectNode) value;
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				JsonNode v = it.next().getValue().get(field);
				list.add(v != null ? creator.create(v) : null);
				declared = declared || v != null;
			}
		} else {
			ArrayNode array = (ArrayNode) value;
			for (JsonNode node : array) {
				JsonNode v = node.get(field);
				list.add(v != null ? creator.create(v) : null);
				declared = declared || v != null;
			}
		}
		return declared ? list : null;
	}
	
	/**
	 * Implements a remap as described in the sync docs
	 * @param value The list of things (since v3 sometimes returns lists as key:value that is also supported)
	 * @param field The field within the thing to extract
	 * @param creator The creator for that field's value
	 * @param <T>
	 * @return A list of extracted values as a mutable list. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> List<T> remap(JsonNode value, String field, SyncableParser<T> creator, JsonConfig config, Allow... allowed) {
		if (isNull(value)) return null;
		
		boolean declared = false;
		ArrayList<T> list = new ArrayList<>();
		
		if (value.isObject()) {
			ObjectNode obj = (ObjectNode) value;
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				JsonNode v = it.next().getValue().get(field);
				list.add(v != null ? creator.create(v, config, allowed) : null);
				declared = declared || v != null;
			}
		} else {
			ArrayNode array = (ArrayNode) value;
			for (JsonNode node : array) {
				JsonNode v = node.get(field);
				list.add(v != null ? creator.create(v, config, allowed) : null);
				declared = declared || v != null;
			}
		}
		return declared ? list : null;
	}
	
	/**
	 * Create a Map<Type>
	 * @return a mutable map. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> Map<String, T> asMap(JsonNode value, TypeParser<T> creator) {
		if (isNull(value)) {
			return null;
		} else if (value.isArray() && value.size() == 0) {
			// Server sometimes returns an empty map as an empty list.
			return Collections.emptyMap();
			
		} else if (value.isObject()) {
			ObjectNode obj = (ObjectNode) value;
			LinkedHashMap<String, T> map = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> entry = it.next();
				map.put(entry.getKey(), creator.create(entry.getValue()));
			}
			return map;
			
		} else {
			throw new RuntimeException( "Unable to parse this as map: " + value.toString());
		}
	}
	
	public static <T> Map<String, T> asMap(JsonParser parser, StreamingTypeParser<T> creator) throws IOException {
		if (parser.currentToken() == JsonToken.VALUE_NULL) {
			return null;
			
		} else if (parser.isExpectedStartArrayToken()) {
			// Server sometimes returns an empty map as an empty list.
			if (parser.nextToken() == JsonToken.END_ARRAY) return Collections.emptyMap();
			else throw new RuntimeException("Map expected, but non-empty list found " + JsonUtil.errorLocation(parser));
			
		} else if (parser.isExpectedStartObjectToken()) {
			LinkedHashMap<String, T> map = new LinkedHashMap<>();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				final String key = parser.getText();
				parser.nextToken();
				map.put(key, creator.create(parser));
			}
			return map;
			
		} else {
			throw new RuntimeException("Unable to parse as map " + JsonUtil.errorLocation(parser));
		}
	}
	
	/**
	 * Create a Map<Type>
	 * @return a mutable map. doesn't do anything to ensure immutability since our current use case passes this value directly to a builder which will handle converting to an immutable value.
	 */
	public static <T> Map<String, T> asMap(JsonNode value, SyncableParser<T> creator, JsonConfig config, Allow... allowed) {
		if (isNull(value)) {
			return null;
		} else if (value.isArray() && value.size() == 0) {
			// Server sometimes returns an empty map as an empty list.
			return Collections.emptyMap();
			
		} else if (value.isObject()) {
			ObjectNode obj = (ObjectNode) value;
			LinkedHashMap<String, T> map = new LinkedHashMap<>();
			Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
			while (it.hasNext()) {
				Map.Entry<String, JsonNode> entry = it.next();
				map.put(entry.getKey(), creator.create(entry.getValue(), config, allowed));
			}
			return map;
			
		} else {
			throw new RuntimeException( "Unable to parse this as map: " + value.toString());
		}
	}
	
	public static <T> Map<String, T> asMap(JsonParser parser, StreamingThingParser<T> creator, JsonConfig config, Allow... allowed) throws IOException {
		if (parser.currentToken() == JsonToken.VALUE_NULL) {
			return null;
			
		} else if (parser.isExpectedStartArrayToken()) {
			// Server sometimes returns an empty map as an empty list.
			if (parser.nextToken() == JsonToken.END_ARRAY) return Collections.emptyMap();
			else throw new RuntimeException("Map expected, but non-empty list found " + JsonUtil.errorLocation(parser));
			
		} else if (parser.isExpectedStartObjectToken()) {
			LinkedHashMap<String, T> map = new LinkedHashMap<>();
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				final String key = parser.getText();
				parser.nextToken();
				map.put(key, creator.create(parser, config, allowed));
			}
			return map;
			
		} else {
			throw new RuntimeException("Unable to parse as map " + JsonUtil.errorLocation(parser));
		}
	}
	
	public static String toJsonValue(StringEnum value) {
		return value == null ? null : value.value;
	}
	
	public static Integer toJsonValue(IntegerEnum value) {
		return value == null ? null : value.value;
	}

	public static ObjectNode toJsonValue(Thing value, JsonConfig config, Include... include) {
		return value != null ? value.toJson(config, include) : null;
	}
	
	public static ObjectNode toObjectNode(String json) {
		return (ObjectNode) toJsonNode(json);
	}
	
	public static JsonNode toJsonNode(String json) {
		if (StringUtils.isBlank(json)) return null;
		try {
			return OBJECT_MAPPER.readTree(json);
		} catch (IOException e) {
			Logs.printStackTrace(e);
			return null;
		}
	}
	
	public static boolean isNull(JsonNode value) {
		return value == null || value.isNull();
	}
	
	/** For Pocket, use {@link com.pocket.sdk.api.value.ModellerUtil#asBoolean(JsonParser)} instead. */
	public static Boolean asBoolean(JsonParser parser) throws IOException {
		if (parser == null) return null;
		switch (parser.currentToken()) {
			case VALUE_NULL: return null;
			case VALUE_FALSE: return false;
			case VALUE_TRUE: return true;
			case VALUE_STRING:
				return parser.getValueAsBoolean();
		}
		throw new RuntimeException("Not an boolean " + JsonUtil.errorLocation(parser));
	}
	
	public static Integer asInteger(JsonParser parser) throws IOException {
		if (parser == null) return null;
		switch (parser.currentToken()) {
			case VALUE_NULL: return null;
			case VALUE_NUMBER_INT:
			case VALUE_STRING:
				return parser.getValueAsInt();
		}
		throw new RuntimeException("Not an integer " + JsonUtil.errorLocation(parser));
	}
	
	public static Long asLong(JsonParser parser) throws IOException {
		if (parser == null) return null;
		switch (parser.currentToken()) {
			case VALUE_NULL: return null;
			case VALUE_NUMBER_INT:
			case VALUE_STRING:
				return parser.getValueAsLong();
		}
		throw new RuntimeException("Not an long " + JsonUtil.errorLocation(parser));
	}
	
	public static Double asDouble(JsonParser parser) throws IOException {
		if (parser == null) return null;
		switch (parser.currentToken()) {
			case VALUE_NULL: return null;
			case VALUE_NUMBER_INT:
			case VALUE_NUMBER_FLOAT:
			case VALUE_STRING:
				return parser.getValueAsDouble();
		}
		throw new RuntimeException("Not an double " + JsonUtil.errorLocation(parser));
	}
	
	public static String asString(JsonParser parser) throws IOException {
		if (parser == null) return null;
		switch (parser.currentToken()) {
			case VALUE_NULL: return null;
			case VALUE_STRING:
			case VALUE_FALSE:
			case VALUE_TRUE:
			case VALUE_NUMBER_FLOAT:
			case VALUE_NUMBER_INT:
				return parser.getValueAsString();
		}
		throw new RuntimeException("Not a string " + JsonUtil.errorLocation(parser));
	}
	
	/** Returns a value that can be safely assumed not to change. Might be the same instance of a copy. */
	public static <T> List<T> immutable(List<T> value) {
		if (value == null) {
			return null;
		} else if (value.isEmpty()) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(new ArrayList<>(value)); // We have to make a copy to ensure that no one has a reference to the backing list, since Collections.unmodifiable just wraps it, doesn't protect against changes made to the original collection, just future changes to this new wrapped instance.
		}
	}
	
	/** Returns a value that can be safely assumed not to change. Might be the same instance of a copy. */
	public static <T> Map<String, T> immutable(Map<String, T> value) {
		if (value == null) {
			return null;
		} else if (value.isEmpty()) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(new LinkedHashMap<>(value)); // We have to make a copy to ensure that no one has a reference to the backing list, since Collections.unmodifiable just wraps it, doesn't protect against changes made to the original collection, just future changes to this new wrapped instance.
		}
	}
	
	/** Returns a value that can be safely assumed not to change. Might be the same instance of a copy. */
	public static ObjectNode immutable(ObjectNode value) {
		if (value == null) {
			return null;
		} else {
			return value.deepCopy();
		}
	}
	
	/** Returns a value that can be safely assumed not to change. Might be the same instance of a copy. */
	public static <T extends Thing> T immutable(T value) {
		return value;
	}
	
	/** Returns a value that can be safely assumed not to change. Might be the same instance of a copy. */
	public static <T extends EnumType> T immutable(T value) {
		return value;
	}
	
	public static boolean isEmpty(Collection v) {
		return v == null || v.isEmpty();
	}
	
	public static boolean isEmpty(Map v) {
		return v == null || v.isEmpty();
	}

	/**
	 * Helper for {@link Thing#with(Changes.ThingMatch, Thing)} implementations.
	 *
	 * @param value The field value to check if it should be replaced.
	 * @param match The matching logic
	 * @param replace The replacement thing if matches
	 * @param checkChildren If the type of valueClass potentially contains identifiable things
	 * @return The replacement for "value" if something matched either directly or in a sub thing. Null if no replacement was found/made.
	 */
	public static <V extends Thing> V with(V value, Changes.ThingMatch match, Thing replace, boolean checkChildren) {
		if (value == null) return null;
		if (value.isIdentifiable()) {
			if (match.matches(value)) return (V) replace;
		} else if (checkChildren) {
			return (V) value.with(match, replace);
		}
		return null;
	}

	/**
	 * Helper for {@link Thing#with(Changes.ThingMatch, Thing)} implementations.
	 *
	 * @param value The field value to check if it should be replaced.
	 * @param innerClass The thing class of the inner type of value. If value is a List<Dog>, this should be Dog.class
	 * @param match The matching logic
	 * @param replace The replacement thing if matches
	 * @param checkChildren If the type of valueClass potentially contains identifiable things
	 * @return The replacement for "value" if something matched either directly or in a sub thing. Null if no replacement was found/made.
	 */
	public static <V extends Thing> List<V> with(List<V> value, Class<V> innerClass, Changes.ThingMatch match, Thing replace, boolean checkChildren) {
		if (value == null || value.isEmpty()) return null;
		int index;
		if (innerClass.isAssignableFrom(replace.getClass()) && (index = indexOf(match, value)) > -1) {
			return replaceElement(value, index, (V) replace);
		} else if (checkChildren) {
			index = 0;
			for (V v : value) {
				if (v == null) continue;
				V mod = (V) v.with(match, replace);
				if (mod != null) return replaceElement(value, index, mod);
				index++;
			}
		}
		return null;
	}

	/**
	 * Same as {@link #with(List, Class, Changes.ThingMatch, Thing, boolean)} but for maps
	 */
	public static <V extends Thing> Map<String, V> with(Map<String, V> value, Class<V> innerClass, Changes.ThingMatch match, Thing replace, boolean checkChildren) {
		if (value == null || value.isEmpty()) return null;
		String key;
		if (innerClass.isAssignableFrom(replace.getClass()) && (key = keyOf(match, value)) != null) {
			return replaceElement(value, key, (V) replace);
		} else if (checkChildren) {
			for (Map.Entry<String, V> v : value.entrySet()) {
				if (v.getValue() == null) continue;
				V mod = (V) v.getValue().with(match, replace);
				if (mod != null) return replaceElement(value, v.getKey(), mod);
			}
		}
		return null;
	}


	/**
	 * @return A copy of the list with the provided element at the specified position.
	 */
	public static <T extends Thing> List<T> replaceElement(List<T> list, int at, T replacement) {
		List<T> mod = new ArrayList<>(list);
		mod.set(at, replacement);
		return mod;
	}
	
	/**
	 * @return A copy of the map with the provided element at the specified key.
	 */
	public static <T extends Thing> Map<String, T> replaceElement(Map<String, T> list, String at, T replacement) {
		Map<String, T> mod = new HashMap<>(list);
		mod.put(at, replacement);
		return mod;
	}
	
	public static Map<String, String> asMap(String... keyvals) {
		if (keyvals == null) return null;
		if (keyvals.length == 0) return Collections.emptyMap();
		if (keyvals.length % 2 != 0) throw new IllegalArgumentException("Must have even number of strings");
		int size = keyvals.length/2;
		Map<String, String> map = new LinkedHashMap<>(size);
		for (int i = 0; i < size; i++) {
			map.put(keyvals[i], keyvals[++i]);
		}
		return Collections.unmodifiableMap(map);
	}
	
	/** @return true if at least one thing matches */
	public static <T extends Thing> boolean contains(Changes.ThingMatch<T> match, List<T> in) {
		return indexOf(match, in) >= 0;
	}
	
	/** @return true if at least one thing matches */
	public static <T extends Thing> boolean contains(Changes.ThingMatch<T> match, Map<String, T> in) {
		return keyOf(match, in) != null;
	}
	
	/** @return the index of the first thing it finds that matches, or -1 */
	public static <T extends Thing> int indexOf(Changes.ThingMatch<T> match, List<T> in) {
		if (in == null || in.isEmpty()) return -1;
		for (int i = 0, len = in.size(); i < len; i++) {
			if (match.matches(in.get(i))) return i;
		}
		return -1;
	}
	
	/** @return the key of the first thing it finds that matches, or null */
	public static <T extends Thing> String keyOf(Changes.ThingMatch<T> match, Map<String, T> in) {
		if (in == null || in.isEmpty()) return null;
		for (Map.Entry<String, T> e : in.entrySet()) {
			if (match.matches(in.get(e.getKey()))) return e.getKey();
		}
		return null;
	}
	
	public static <T extends Thing> T redact(T value, StringEncrypter encrypter) {
		if (value == null) return null;
		return (T) value.redact(encrypter);
	}
	
	public static <T extends Thing> T unredact(T value, StringEncrypter encrypter) {
		if (value == null) return null;
		return (T) value.unredact(encrypter);
	}
	

}
