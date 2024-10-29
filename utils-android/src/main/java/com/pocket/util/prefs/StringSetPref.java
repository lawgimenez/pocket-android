package com.pocket.util.prefs;

import java.util.Collections;
import java.util.Set;

import io.reactivex.Observable;

/** Implementation of {@link StringSetPreference} */
public class StringSetPref implements StringSetPreference {
	
	private final String key;
	private final Store store;
	private final Set<String> defaultValue;
	
	public StringSetPref(String key, Set<String> defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue != null ? Collections.unmodifiableSet(defaultValue) : null;
		this.store = store;
	}
	
	@Override
	public Set<String> get() {
		return isSet() ? store.getStringSet(key) : defaultValue;
	}
	
	@Override
	public void set(Set<String> value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<Set<String>> changes() {
		return store.stringSetChanges(key);
	}
	
	@Override
	public Observable<Set<String>> getWithChanges() {
		return changes().startWith(get());
	}
}