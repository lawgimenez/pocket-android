package com.pocket.util.prefs;

import java.util.Set;

import io.reactivex.Observable;

/**
 * An app's persisted {@link Preference}s.
 * <p>
 * Divides preferences into two different {@link Store}s, one that represents preferences that are for the user
 * and should be cleared with {@link #clearUser()} when the user logs out, and another set of preferences that
 * are for the app and likely are never cleared (but can be with {@link #clear()}.
 * <p>
 * To obtain an instance of a user preference use one of the forUser methods.
 * To obtain an instance of a user preference use one of the forApp methods.
 */
public interface Preferences {
	
	/** Reset all user based preferences, returning them back to unset. */
	void clearUser();
	/** Reset all preferences, returning them back to unset. */
	void clear();
	/** Reset the preference with this key, returning it back to unset. */
	void remove(String key);
	/** A set of all known keys for user preferences */
	Set<String> userKeys();
	/** A set of all known keys for app preferences */
	Set<String> appKeys();
	
	BooleanPreference forUser(String key, boolean defaultValue);
	BooleanPreference forApp(String key, boolean defaultValue);
	
	IntPreference forUser(String key, int defaultValue);
	IntPreference forApp(String key, int defaultValue);
	
	FloatPreference forUser(String key, float defaultValue);
	FloatPreference forApp(String key, float defaultValue);
	
	LongPreference forUser(String key, long defaultValue);
	LongPreference forApp(String key, long defaultValue);
	
	StringPreference forUser(String key, String defaultValue);
	StringPreference forApp(String key, String defaultValue);
	
	StringSetPreference forUser(String key, Set<String> defaultValue);
	StringSetPreference forApp(String key, Set<String> defaultValue);
	
	<E extends Enum<E>> EnumPreference<E> forUser(String key, Class<E> clazz, E defaultValue);
	<E extends Enum<E>> EnumPreference<E> forApp(String key, Class<E> clazz, E defaultValue);
	
	/**
	 * Create a nested set of preferences.
	 * TODO flush out docs and expectations a bit more.
	 * @param name
	 * @return
	 */
	Preferences group(String name);
	
	/**
	 * @return An observable of when preferences change, the emitted value is the key of the preference.
	 */
	Observable<String> changes();
}