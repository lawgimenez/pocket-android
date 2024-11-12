package com.pocket.util.prefs;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;

/**
 * A {@link Preferences} that wraps another and when getting preference instances, it adds a prefix to the key and invokes it on the parent.
 * All methods like {@link #userKeys()}, {@link #clear()} etc will only affect preferences with that prefix.
 * This is an implementation of a nested preferences for use in {@link Preferences#group(String)}.
 */
public class PrefixPreferences implements Preferences {
	
	private final Preferences wrapped;
	private final String prefix;
	
	public PrefixPreferences(Preferences wrapped, String key) {
		this.prefix = key;
		this.wrapped = wrapped;
	}
	
	private String prefix(String key) {
		return prefix + key;
	}
	
	@Override
	public void remove(String key) {
		wrapped.remove(prefix(key));
	}
	
	@Override
	public Set<String> userKeys() {
		Set<String> keys = new HashSet<>();
		int prefixLen = prefix.length();
		for (String key : wrapped.userKeys()) {
			if (key.startsWith(prefix)) keys.add(key.substring(prefixLen));
		}
		return keys;
	}
	
	@Override
	public Set<String> appKeys() {
		Set<String> keys = new HashSet<>();
		int prefixLen = prefix.length();
		for (String key : wrapped.appKeys()) {
			if (key.startsWith(prefix)) keys.add(key.substring(prefixLen));
		}
		return keys;
	}
	
	@Override
	public void clearUser() {
		for (String key : wrapped.userKeys()) {
			if (key.startsWith(prefix)) wrapped.remove(key);
		}
	}
	
	@Override
	public void clear() {
		clearUser();
		for (String key : wrapped.appKeys()) {
			if (key.startsWith(prefix)) wrapped.remove(key);
		}
	}
	
	@Override
	public BooleanPreference forUser(String key, boolean defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public BooleanPreference forApp(String key, boolean defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public FloatPreference forUser(String key, float defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public FloatPreference forApp(String key, float defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public IntPreference forUser(String key, int defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public IntPreference forApp(String key, int defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public LongPreference forUser(String key, long defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public LongPreference forApp(String key, long defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public StringPreference forUser(String key, String defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public StringPreference forApp(String key, String defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public StringSetPreference forUser(String key, Set<String> defaultValue) {
		return wrapped.forUser(prefix(key), defaultValue);
	}
	
	@Override
	public StringSetPreference forApp(String key, Set<String> defaultValue) {
		return wrapped.forApp(prefix(key), defaultValue);
	}
	
	@Override
	public <E extends Enum<E>> EnumPreference<E> forUser(String key, Class<E> clazz, E defaultValue) {
		return wrapped.forUser(prefix(key), clazz, defaultValue);
	}
	
	@Override
	public <E extends Enum<E>> EnumPreference<E> forApp(String key, Class<E> clazz, E defaultValue) {
		return wrapped.forApp(prefix(key), clazz, defaultValue);
	}
	
	@Override
	public Preferences group(String name) {
		return new PrefixPreferences(this, name);
	}
	
	@Override
	public Observable<String> changes() {
		return wrapped.changes().filter(key -> key.startsWith(prefix));
	}
}
