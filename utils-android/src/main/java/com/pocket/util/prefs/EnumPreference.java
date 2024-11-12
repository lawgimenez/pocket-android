package com.pocket.util.prefs;

public interface EnumPreference<E extends Enum<E>> extends Preference<E> {
	/** The current value. If {@link #set(E)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	E get();
	void set(E value);
}