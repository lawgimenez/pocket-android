package com.pocket.util.java.function;

/**
 * A functional interface that takes a value and does not return a value
 * @param <V> the input value type
 */
public interface Take<V> {
	void apply(V value);
}
