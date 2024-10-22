package com.pocket.sdk.api.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.sync.value.BaseModeller;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Extra methods that the generated Modeller can use that would have been a pain to write out in the PocketConfig class.
 */
public class ModellerUtil {
	
	public static Boolean asBoolean(String value) {
		if (value == null || value.isEmpty()) return false;
		if (value.equals("1") || StringUtils.equalsIgnoreCase(value, "true")) return true;
		if (value.equals("0") || StringUtils.equalsIgnoreCase(value, "false")) return false;
		throw new RuntimeException("value is not boolean " + value);
	}
	
	public static Boolean asBoolean(JsonNode value) {
		if (BaseModeller.isNull(value)) {
			return null;
			
		} else if (value.isBoolean()) {
			return value.asBoolean(false);
			
		} else if (value.isTextual()) {
			return asBoolean(value.asText());
			
		} else if (value.isInt()) {
			if (value.asInt() == 1) {
				return true;
			} else if (value.asInt() == 0) {
				return false;
			}
		}
		
		throw new RuntimeException("value is not boolean " + value);
	}
	
	public static Boolean asBoolean(JsonParser parser) throws IOException {
		final JsonToken token = parser.currentToken();
		if (token == JsonToken.VALUE_NULL) {
			return null;
			
		} else if (token == JsonToken.VALUE_TRUE) {
			return true;
		} else if (token == JsonToken.VALUE_FALSE) {
			return false;
			
		} else if (token == JsonToken.VALUE_STRING) {
			return asBoolean(parser.getText());
			
		} else if (token == JsonToken.VALUE_NUMBER_INT) {
			final int value = parser.getIntValue();
			if (value == 1) return true;
			else if (value == 0) return false;
		}
		
		throw new RuntimeException("value is not boolean " + parser.getText());
	}
}
