package com.pocket.util.prefs;

public interface StringPreference extends Preference<String> {
	/** The current value. If {@link #set(String)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	String get();
	void set(String value);
}