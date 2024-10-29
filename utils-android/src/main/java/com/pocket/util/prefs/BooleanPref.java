package com.pocket.util.prefs;


import io.reactivex.Observable;

/** Implementation of {@link BooleanPreference} */
public class BooleanPref implements BooleanPreference {
		
	private final String key;
	private final Store store;
	private final boolean defaultValue;
	
	public BooleanPref(String key, boolean defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public boolean get() {
		return isSet() ? store.getBoolean(key) : defaultValue;
	}
	
	@Override
	public void set(boolean value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<Boolean> changes() {
		return store.booleanChanges(key);
	}
	
	@Override
	public Observable<Boolean> getWithChanges() {
		return changes().startWith(get());
	}
}