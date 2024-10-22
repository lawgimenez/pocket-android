package com.pocket.util.prefs;

import java.util.Set;

public interface StringSetPreference extends Preference<Set<String>> {
	/**
	 * The current value. If {@link #set(Set)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known.
	 * @return An immutable value, do not attempt to change this value directly. Make a copy if you need to modify it and use {@link #set(Set)} to update the stored value.
	 */
	Set<String> get();
	void set(Set<String> value);
}