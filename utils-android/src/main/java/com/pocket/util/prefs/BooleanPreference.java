package com.pocket.util.prefs;

/** A {@link Preference} with a boolean value. */
public interface BooleanPreference extends Preference<Boolean> {
	/** The current value. If {@link #set(boolean)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	boolean get();
	void set(boolean value);
}