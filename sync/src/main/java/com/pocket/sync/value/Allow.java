package com.pocket.sync.value;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Used in methods like {@link TypeParser#create(JsonNode, Allow...)} to control what's allowed
 * in the resulting thing.
 */
public enum Allow {
	/**
	 * Allow creating "unknown" implementations of interfaces and varieties.
	 * This can happen when the JSON (or other serialized form) is created with a newer version
	 * of the spec and includes a new interface implementation or a new thing in a variety.
	 * Specifically the use case in mind is if a remote starts sending some new types
	 * to old clients.
	 */
	UNKNOWN;

	/**
	 * @return true if `array` contains `value`.
	 */
	public static boolean contains(Allow[] array, Allow value) {
		return array != null && array.length != 0 && ArrayUtils.contains(array, value);
	}
}
