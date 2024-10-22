package com.pocket.sync.action;

/**
 * An interface for a builder object that can create a specific type of {@link Action}.
 */
public interface ActionBuilder<T extends Action> {
	T build();
	/** For all declared values in v, set them on the builder. */
	ActionBuilder<T> set(T v);
}
