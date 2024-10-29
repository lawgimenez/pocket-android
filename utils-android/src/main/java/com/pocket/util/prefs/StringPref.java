package com.pocket.util.prefs;

import io.reactivex.Observable;

/** Implementation of {@link StringPreference} */
public class StringPref implements StringPreference {
	
	private final String key;
	private final Store store;
	private final String defaultValue;
	
	public StringPref(String key, String defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public String get() {
		return isSet() ? store.getString(key) : defaultValue;
	}
	
	@Override
	public void set(String value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<String> changes() {
		return store.stringChanges(key);
	}
	
	@Override
	public Observable<String> getWithChanges() {
		return changes().startWith(get());
	}
}