package com.pocket.sync.thing;

/**
 * An interface for a builder object that can create a specific type of Thing.
 */

public interface ThingBuilder<T extends Thing> {
	T build();
	/** For all declared values in v, set them on the builder. */
	ThingBuilder<T> set(T v);
}
