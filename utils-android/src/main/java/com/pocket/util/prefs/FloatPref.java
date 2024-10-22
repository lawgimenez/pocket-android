package com.pocket.util.prefs;

import io.reactivex.Observable;

/** Implementation of {@link FloatPreference} */
public class FloatPref implements FloatPreference {
	
	private final String key;
	private final Store store;
	private final float defaultValue;
	
	public FloatPref(String key, float defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public float get() {
		return isSet() ? store.getFloat(key) : defaultValue;
	}
	
	@Override
	public void set(float value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<Float> changes() {
		return store.floatChanges(key);
	}
	
	@Override
	public Observable<Float> getWithChanges() {
		return changes().startWith(get());
	}
}