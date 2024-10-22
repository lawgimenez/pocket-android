package com.pocket.util.java;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class JsonUtil {
	
	private static ObjectMapper mMapper;
	private static JsonFactory mJsonFactory;

	public static int getValueAsInt(JsonNode node, String fieldName, int defaultValue){
		JsonNode value = node.get(fieldName);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asInt(defaultValue);
	}
	
	public static long getValueAsLong(JsonNode node, String fieldName, long defaultValue){
		JsonNode value = node.get(fieldName);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asLong(defaultValue);
	}
	
	public static String getValueAsText(JsonNode node, String fieldName, String defaultValue){
		JsonNode value = node.get(fieldName);
		if(value == null || value.isNull())
			return defaultValue;
		
		return getNodeAsText(value);
	}

	public static boolean getValueAsBoolean(JsonNode node, String fieldName, boolean defaultValue) {
		JsonNode value = node.get(fieldName);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asBoolean(defaultValue);
	}

	public static double getValueAsDouble(JsonNode node, String fieldName, double defaultValue) {
		JsonNode value = node.get(fieldName);
		if(value == null || value.isNull())
			return defaultValue;

		return value.asDouble(defaultValue);
	}

	/**
	 * Gets the value as boolean, handling the case where it is "true"/"false" or "0"/"1".
	 *
	 * @param node
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public static boolean getValueAsBooleanSafe(JsonNode node, String fieldName, boolean defaultValue) {
		if (node == null) {
			return defaultValue;
		} else {
			Boolean b = getValueAsBooleanSafe(node.get(fieldName));
			if (b == null) {
				return defaultValue;
			} else {
				return b;
			}
		}
	}
	
	public static Boolean getValueAsBooleanSafe(JsonNode value) {
		if (value == null || value.isNull()) {
			return null;
		} else if (value.isBoolean()) {
			return value.asBoolean();
		} else if (value.isTextual()) {
			String text = value.asText();
			if (text.equals("1") || text.equalsIgnoreCase("true")) {
				return true;
			} else if (text.equals("0") || text.equalsIgnoreCase("false")) {
				return false;
			}
		} else if (value.isNumber()) {
			if (value.asDouble() == 1) {
				return true;
			} else if (value.asDouble() == 0) {
				return false;
			}
		}
		return null;
	}
	
	public static int getValueAsInt(ArrayNode node, int index, int defaultValue){
		JsonNode value = node.get(index);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asInt(defaultValue);
	}
	
	public static long getValueAsLong(ArrayNode node, int index, long defaultValue){
		JsonNode value = node.get(index);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asLong(defaultValue);
	}
	
	public static String getValueAsText(ArrayNode node, int index, String defaultValue){
		JsonNode value = node.get(index);
		if(value == null || value.isNull())
			return defaultValue;
		
		return getNodeAsText(value);
	}
	
	private static String getNodeAsText(JsonNode node){
		if (node.isValueNode())
			return node.asText();
		else
			return node.toString();
	}

	public static boolean getValueAsBoolean(ArrayNode node, int index, boolean defaultValue) {
		JsonNode value = node.get(index);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asBoolean(defaultValue);
	}
	
	public static double getValueAsDouble(ArrayNode node, int index, double defaultValue) {
		JsonNode value = node.get(index);
		if(value == null || value.isNull())
			return defaultValue;
		
		return value.asDouble(defaultValue);
	}

	public static ObjectMapper getObjectMapper() {
		if (mMapper == null) {
			mMapper = new ObjectMapper();
			configureMapper(mMapper);
		}
		
		return mMapper;
	}
	
	private static void configureMapper(ObjectMapper mapper) {
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	public static ObjectNode newObjectNode() {
		return getObjectMapper().createObjectNode();
	}

	public static ArrayNode newArrayNode() {
		return getObjectMapper().createArrayNode();
	}
	
	public static ObjectNode stringToObjectNode(String json) {
		if (StringUtils.isBlank(json)) return null;
		
		ObjectMapper mapper = getObjectMapper();
		
		try {
			return (ObjectNode) mapper.readTree(json);
		} catch (JsonProcessingException e) {
			Logs.printStackTrace(e);
		} catch (IOException e) {
			Logs.printStackTrace(e);
		}
		return null;
	}
	
	public static ArrayNode stringToArrayNode(String json) {
		if (StringUtils.isBlank(json))
			return null;
		
		ArrayNode node = null;
		ObjectMapper mapper = getObjectMapper();
		
		try {
			node = (ArrayNode) mapper.readTree(json);
		} catch (JsonProcessingException e) {
			Logs.printStackTrace(e);
		} catch (IOException e) {
			Logs.printStackTrace(e);
		}
		return node;
	}

	public static ObjectNode getValueAsObject(ArrayNode array, int index) {
		return returnObjectNode(array.get(index));
	}

	public static ObjectNode getValueAsObject(ObjectNode obj, String key) {
		return returnObjectNode(obj.get(key));
	}
	
	private static ObjectNode returnObjectNode(JsonNode node) {
		if (node == null || !node.isObject())
			return null;
		
		return (ObjectNode) node;
		
	}
	
	public static ArrayNode getValueAsArray(ArrayNode array, int index) {
		return returnArrayNode(array.get(index));
	}

	public static ArrayNode getValueAsArray(ObjectNode obj, String key) {
		return returnArrayNode(obj.get(key));
	}
	
	private static ArrayNode returnArrayNode(JsonNode node) {
		if (node == null || !node.isArray())
			return null;
		
		return (ArrayNode) node;
		
	}
	
	/**
	 * JsonParser.getText() will return a null value as "null". This ensures null values are returned as null instead of "null"
	 * 
	 * @param jp
	 * @return 
	 * @throws IOException 
	 * @throws JsonParseException 
	 */
	public static String getText(JsonParser jp) throws JsonParseException, IOException {
		return jp.getCurrentToken() != JsonToken.VALUE_NULL ? jp.getText() : null;
	}

	public static JsonFactory getJsonFactory(){
		if(mJsonFactory == null)
			mJsonFactory = new JsonFactory();
		
		return mJsonFactory;
	}

	/**
	 * Returns an {@link ObjectNode} starting from the current location. The parser should be set to the opening { of the object.
	 * 
	 * The parser will not be closed and its location will end up after the object end?. REVIEW end or after?
	 * 
	 * @param jp
	 * @return
	 */
	public static ObjectNode getObject(JsonParser jp) {
		try {
			JsonNode value = getObjectMapper().readTree(jp);
			if (value == null || value.isNull()) {
				return null;
			} else {
				return (ObjectNode) value;
			}
		
		} catch (Exception e) {
			Logs.printStackTrace(e);
		}
		return null;
	}
	
	/**
	 * Returns an {@link ArrayNode} starting from the current location. The parser should be set to the opening [ of the array.
	 * 
	 * The parser will not be closed and its location will end up after the array end?. REVIEW end or after?
	 * 
	 * @param jp
	 * @return
	 */
	public static ArrayNode getArray(JsonParser jp) {
		try {
			return (ArrayNode) getObjectMapper().readTree(jp);
		
		} catch (Exception e) {
			Logs.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Returns a multiline string of the json nicely formatted for display, including sorting
	 * the keys by alphabetical order for easier scanning.
	 * This is mostly for development use, it is not optimized for performance at all.
	 * @param json
	 * @return
	 */
	public static String prettyPrint(ObjectNode json) {
		try {
			return getObjectMapper()
					.writerWithDefaultPrettyPrinter()
					.with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
					.writeValueAsString(getObjectMapper().readValue(json.toString(), HashMap.class));
		} catch (IOException e) {
			Logs.printStackTrace(e);
			return null;
		}
	}

	public static JsonNode copy(JsonNode value) {
		return value.deepCopy();
	}
	
	/**
	 * Creates a diff between two objects.
	 * @param from The old
	 * @param to The new
	 * @return A set of additions, removals and changes from old to new
	 */
	public static List<String> diff2(JsonNode from, JsonNode to, EqualsFlag equalsFlag) {
		List<String> changes = new ArrayList<>();
		String path = "";
		diff2(changes, path, from, to, equalsFlag);
		return changes;
	}
	
	public static void diff2(List<String> changes, String path, JsonNode from, JsonNode to, EqualsFlag equalsFlag) {
		if (equals(from, to, equalsFlag)) {
			return;
		} else if (to == null) {
			changes.add(path + " : " + from.asText() + " -> MISSING");
		} else if (from == null) {
			changes.add(path + " : MISSING -> " + to.toString());
		} else if (!from.getNodeType().equals(to.getNodeType())) {
			changes.add(path + " : " + from.toString() + " -> " + to.toString());
		} else if (from instanceof ArrayNode) {
			int max = Math.max(from.size(), to.size());
			for (int i = 0; i < max; i++) {
				JsonNode f = i < from.size() ? from.get(i) : null;
				JsonNode t = i < from.size() ? to.get(i) : null;
				diff2(changes, path+"["+i+"]", f, t, equalsFlag);
			}
		} else if (from instanceof ObjectNode) {
			Iterator<String> it = to.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				String subPath = path+"."+key;
				JsonNode toValue = to.get(key);
				JsonNode fromValue = from.get(key);
				if (!from.has(key)) {
					changes.add(subPath + ": MISSING -> " + toValue.toString());
				} else {
					diff2(changes, subPath, fromValue, toValue, equalsFlag);
				}
			}
			it = from.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				String subPath = path+"."+key;
				JsonNode fromValue = from.get(key);
				if (!to.has(key)) {
					changes.add(subPath + " : " + fromValue.toString() + " -> MISSING");
				}
			}
		} else {
			changes.add(path + " : " + from.toString() + " -> " + to.toString());
		}
	}
	
	public static boolean isNull(JsonNode jsonNode) {
		return jsonNode == null || jsonNode.isNull();
	}
	
	public static <T extends JsonNode> T sortKeys(T in, ObjectMapper mapper) {
		if (in instanceof ObjectNode) {
			ObjectNode out = mapper.createObjectNode();
			SortedMap<String, JsonNode> sorted = new TreeMap<>();
			Iterator<String> it = in.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				JsonNode value = in.get(key);
				sorted.put(key, value);
			}
			for (Map.Entry<String, JsonNode> field : sorted.entrySet()) {
				out.set(field.getKey(), sortKeys(field.getValue(), mapper));
			}
			return (T) out;
			
		} else if (in instanceof ArrayNode) {
			ArrayNode out = mapper.createArrayNode();
			for (int i = 0; i < in.size(); i++) {
				out.add(sortKeys(in.get(i), mapper));
			}
			return (T) out;
			
		} else {
			return in;
		}
	}
	
	public static JsonNode stringAllValues(JsonNode in, ObjectMapper mapper) {
		if (in instanceof ObjectNode) {
			ObjectNode out = mapper.createObjectNode();
			Iterator<String> it = in.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				out.put(key, stringAllValues(in.get(key), mapper));
			}
			return out;
			
		} else if (in instanceof ArrayNode) {
			ArrayNode out = mapper.createArrayNode();
			for (int i = 0; i < in.size(); i++) {
				out.add(stringAllValues(in.get(i), mapper));
			}
			return out;
			
		} else if (in.isTextual()) {
			return in;
			
		} else {
			return new TextNode(in.asText());
		}
	}
	
	/**
	 * Produces a new instance or null, where it has all of the fields of `into` and `from`, preferring `from`'s values for
	 * and fields they both have. Returns null only if both are null. Safe to pass null for either one.
	 */
	public static ObjectNode merge(ObjectNode into, ObjectNode from) {
		if (into == null && from == null) {
			return null;
		}
		ObjectNode obj = newObjectNode();
		if (into != null) {
			obj.setAll(into);
		}
		if (from != null) {
			obj.setAll(from);
		}
		return obj;
	}
	
	public enum EqualsFlag {
		/** Numerical nodes like int and double are compared in value, so 1 equals 1.0 */
		ANY_NUMERICAL,
		/**
		 * Same as Numerical, but also checks for numbers as strings, so "1" equals 1.
		 * Also allows 0 to equal false and 1 to equal true (including "0" and "1")
		 */
		ANY_TYPE
	}
	
	public static boolean equals(JsonNode o1, JsonNode o2, EqualsFlag flag) {
		if (o1 == null) {
			return o2 == null;
		} else if (o2 == null) {
			return false;
		}
		
		if (flag == null) {
			return o1.equals(o2);
		} else if (flag == EqualsFlag.ANY_NUMERICAL) {
			return o1.equals(NUMERIC_COMPARE , o2);
		} else if (flag == EqualsFlag.ANY_TYPE) {
			return o1.equals(ANY_COMPARE , o2);
		} else {
			throw new RuntimeException("unknown flag " + flag);
		}
	}
	
	private static final Comparator<JsonNode> NUMERIC_COMPARE = (o1, o2) -> {
		if (o1.equals(o2)) {
			return 0;
		}
		boolean isNumeric1 = o1 instanceof NumericNode || NumberUtils.isParsable(o1.asText());
		boolean isNumeric2 = o2 instanceof NumericNode || NumberUtils.isParsable(o2.asText());
		if (isNumeric1 && isNumeric2) {
			double d1 = o1.asDouble();
			double d2 = o2.asDouble();
			return d1 == d2 ? 0 : 1;
		} else {
			return 1;
		}
	};
	
	private static final Comparator<JsonNode> ANY_COMPARE = (o1, o2) -> {
		if (o1.equals(o2)) {
			return 0;
		}
		String t1 = o1.asText();
		String t2 = o2.asText();
		boolean isNumeric1 = o1 instanceof NumericNode || NumberUtils.isParsable(t1);
		boolean isNumeric2 = o2 instanceof NumericNode || NumberUtils.isParsable(t2);
		if (isNumeric1 && isNumeric2) {
			double d1 = o1.asDouble();
			double d2 = o2.asDouble();
			return d1 == d2 ? 0 : 1;
		} else {
			Boolean b1 = JsonUtil.getValueAsBooleanSafe(o1);
			Boolean b2 = JsonUtil.getValueAsBooleanSafe(o2);
			if (b1 != null || b2 != null) {
				if (b1 != null && b2 != null) {
					return Boolean.compare(b1, b2);
				} else {
					return 0;
				}
			}
			String str1 = o1.isNull() ? null : t1;
			String str2 = o1.isNull() ? null : t2;
			return StringUtils.equals(str1, str2) ? 0 : 1;
		}
	};
	
	/**
	 * Returns some information about current location that can be helpful for error logging.
	 */
	public static String errorLocation(JsonParser parser) {
		try {
			return " TOKEN: " + parser.currentToken() +
				   " VALUE: " + parser.getValueAsString() +
				   " NAME: " + parser.getCurrentName() +
				   " LOCATION: " + parser.getCurrentLocation();
		} catch (Throwable t) {
			return "?";
		}
	}
	
}
