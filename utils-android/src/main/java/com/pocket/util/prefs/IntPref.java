package com.pocket.util.prefs;

import io.reactivex.Observable;

/** Implementation of {@link IntPreference} */
public class IntPref implements IntPreference {
	
	private final String key;
	private final Store store;
	private final int defaultValue;
	
	public IntPref(String key, int defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public int get() {
		return isSet() ? store.getInt(key) : defaultValue;
	}
	
	@Override
	public void set(int value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<Integer> changes() {
		return store.intChanges(key);
	}
	
	@Override
	public Observable<Integer> getWithChanges() {
		return changes().startWith(get());
	}
}