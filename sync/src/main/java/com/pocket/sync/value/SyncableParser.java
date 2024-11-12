package com.pocket.sync.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.sync.source.JsonConfig;

/**
 * Methods for creating types from some kind of information such as json.
 */
public interface SyncableParser<T> {
	/** Create a new instance of this type from this json. */
	T create(JsonNode value, JsonConfig config, Allow... allowed);
}
