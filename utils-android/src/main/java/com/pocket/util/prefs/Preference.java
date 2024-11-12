package com.pocket.util.prefs;


import io.reactivex.Observable;

/**
 * Represents a value, of some type, typically persisted across app lifecycles / processes.
 * Mostly meant as an abstraction on Android's shared preferences that can be used in different
 * non-android use cases such as unit tests.
 * <p>
 * Note: Implementations will typically have set(), get(), and defaultValue() methods but
 * we can't provide them here since generics do not allow primitives and some implementations
 * want to use them. If we defined them here with generics, they would be required to use boxed values.
 * @param <T> The boxed type of the value
 */
public interface Preference<T> {
	/** @return true if this preference as been explicitly changed/set in the past, false if it has not and is returning its defaultValue. */
	boolean isSet();
	/** An observable anytime this preference's value changes in the future */
	Observable<T> changes();
	/** An observable that emits the current value on subscribe plus anytime this preference's value changes in the future. */
	Observable<T> getWithChanges();
}