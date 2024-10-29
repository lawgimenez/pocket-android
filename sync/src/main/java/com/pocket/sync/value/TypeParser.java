package com.pocket.sync.value;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Methods for creating types from some kind of information such as json.
 */
public interface TypeParser<T> {
	/**
	 * Create a new instance of this type from this json.
	 * @param value
	 * @return
	 */
	T create(JsonNode value);
}
