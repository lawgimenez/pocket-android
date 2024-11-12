package com.pocket.sync.spec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.AuthType;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.Remote;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Include;

import java.util.Map;

/**
 * Represents either a {@link Thing} or {@link Action}
 */
public interface Syncable {

	/** An {@link JsonConfig} Object which assumes no aliases, for use where parsing based on {@link com.pocket.sync.source.RemoteStyle} is not necessary. */
	JsonConfig NO_ALIASES = new JsonConfig(null, true);

    /**
	 * A unique name for this type of {@link Thing}. There can be many different ones of the same type.
	 * For example, a "cat", or "animal". Similar to a class name.
	 */
	default String name() {
	    return this instanceof Thing ? ((Thing) this).type() : ((Action) this).action();
    }

    /**
	 * A representation of this as JSON.
	 * <p>
	 * By default this may strip out some sensitive values like passwords or access tokens
	 * to protect against accidental exposure in logs or areas they shouldn't be visible.
	 * If you must have those values, you can include one or many of the {@link Include} flags,
	 * but you must take extreme care not to leak this information incorrectly in logs or other areas.
	 * Of course all fields could contain info that is private to the user, so care should always be taken,
	 * but these other fields require additional oversight.
	 */
	ObjectNode toJson(JsonConfig config, Include... include);

	/**
	 * A representation of this as a new Map.
	 * <p>
	 * By default this may strip out some sensitive values like passwords or access tokens
	 * to protect against accidental exposure in logs or areas they shouldn't be visible.
	 * If you must have those values, you can include one or many of the {@link Include} flags,
	 * but you must take extreme care not to leak this information incorrectly in logs or other areas.
	 * Of course all fields could contain info that is private to the user, so care should always be taken,
	 * but these other fields require additional oversight.
	 */
	Map<String, Object> toMap(Include... include);

	/**
	 * Note: Implementations will strip out and never expose potential sensitive fields like credentials, tokens, passwords etc. in this method.
	 * If you must have those fields, see {@link #toJson(JsonConfig, Include...)} or {@link #toMap(Include...)} but be careful not to expose that information in logs or persisted storage.
	 */
	@Override
	String toString();

	/** An {@link AuthType} if required. */
	AuthType auth();

	/** {@link Remote} info for how to work with this remotely. */
	Remote remote();

}
