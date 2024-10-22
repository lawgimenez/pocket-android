package com.pocket.util.prefs;

public interface IntPreference extends Preference<Integer> {
	/** The current value. If {@link #set(int)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	int get();
	void set(int value);
}