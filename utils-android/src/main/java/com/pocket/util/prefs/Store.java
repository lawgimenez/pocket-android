package com.pocket.util.prefs;


import java.util.Set;

import io.reactivex.Observable;

/**
 * A store of preference values. Typically persisted.
 * Mostly intended as an abstraction of Android's SharedPreferences class that can be
 * used in non-android contexts like unit tests.
 */
public interface Store {
	Observable<String> changes();
	
	boolean contains(String key);
	void remove(String key);
	void clear();
	Set<String> keys();
	
	String getString(String key);
	void set(String key, String value);
	Observable<String> stringChanges(String key);
	
	/** @return null if not present or an immutable set. */
	Set<String> getStringSet(String key);
	/** Update the value. Note: Implementations to take care to make sure that changes to the value passed here don't change the internal stored value. Make a copy of the set before holding a reference to it. */
	void set(String key, Set<String> value);
	/** Note that sets emitted are immutable. */
	Observable<Set<String>> stringSetChanges(String key);
	
	int getInt(String key);
	void set(String key, int value);
	Observable<Integer> intChanges(String key);
	
	float getFloat(String key);
	void set(String key, float value);
	Observable<Float> floatChanges(String key);
	
	long getLong(String key);
	void set(String key, long value);
	Observable<Long> longChanges(String key);
	
	boolean getBoolean(String key);
	void set(String key, boolean value);
	Observable<Boolean> booleanChanges(String key);
	
}