package com.pocket.util.prefs;

public interface FloatPreference extends Preference<Float> {
	/** The current value. If {@link #set(float)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	float get();
	void set(float value);
}