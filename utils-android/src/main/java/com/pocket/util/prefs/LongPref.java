package com.pocket.util.prefs;

import io.reactivex.Observable;

/** Implementation of {@link LongPreference} */
public class LongPref implements LongPreference {
	
	private final String key;
	private final Store store;
	private final long defaultValue;
	
	public LongPref(String key, long defaultValue, Store store) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public long get() {
		return isSet() ? store.getLong(key) : defaultValue;
	}
	
	@Override
	public void set(long value) {
		store.set(key, value);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<Long> changes() {
		return store.longChanges(key);
	}
	
	@Override
	public Observable<Long> getWithChanges() {
		return changes().startWith(get());
	}
}