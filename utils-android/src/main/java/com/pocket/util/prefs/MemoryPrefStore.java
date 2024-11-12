package com.pocket.util.prefs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class MemoryPrefStore implements Store {
		
	private final Map<String, Object> values = new HashMap<>();
	private final PublishSubject<String> keyChanges = PublishSubject.create();
	
	@Override
	public Observable<String> changes() {
		return keyChanges;
	}
	
	private <T> Observable<T> changes(String key, AndroidPrefStore.Get<T> getter) {
		return changes()
				.filter(changed -> changed.equals(key))
				.map(k -> getter.get(key));
	}
	
	interface Get<T> {
		T get(String key);
	}
	
	private void put(String key, Object value) {
		values.put(key, value);
		keyChanges.onNext(key);
	}
	
	@Override
	public Set<String> keys() {
		return new HashSet<>(values.keySet());
	}
	
	@Override
	public boolean contains(String key) {
		return values.containsKey(key);
	}
	
	@Override
	public void remove(String key) {
		values.remove(key);
	}
	
	
	@Override
	public String getString(String key) {
		return (String) values.get(key);
	}
	
	@Override
	public void set(String key, String value) {
		put(key, value);
	}
	
	@Override
	public Observable<String> stringChanges(String key) {
		return changes(key, this::getString);
	}
	
	
	@Override
	public Set<String> getStringSet(String key) {
		Set<String> value = (Set<String>) values.get(key);
		return value != null ? Collections.unmodifiableSet(value) : null;
	}
	
	@Override
	public void set(String key, Set<String> value) {
		put(key, value);
	}
	
	@Override
	public Observable<Set<String>> stringSetChanges(String key) {
		return changes(key, this::getStringSet);
	}
	
	
	
	@Override
	public int getInt(String key) {
		Object v = values.get(key);
		return v != null ? (Integer) v : 0;
	}
	
	@Override
	public void set(String key, int value) {
		put(key, value);
	}
	
	@Override
	public Observable<Integer> intChanges(String key) {
		return changes(key, this::getInt);
	}
	
	
	
	@Override
	public float getFloat(String key) {
		Object v = values.get(key);
		return v != null ? (Float) v : 0;
	}
	
	@Override
	public void set(String key, float value) {
		put(key, value);
	}
	
	@Override
	public Observable<Float> floatChanges(String key) {
		return changes(key, this::getFloat);
	}
	
	
	
	@Override
	public long getLong(String key) {
		Object v = values.get(key);
		return v != null ? (Long) v : 0;
	}
	
	@Override
	public void set(String key, long value) {
		put(key, value);
	}
	
	@Override
	public Observable<Long> longChanges(String key) {
		return changes(key, this::getLong);
	}
	
	
	
	@Override
	public boolean getBoolean(String key) {
		Object v = values.get(key);
		return v != null ? (Boolean) v : false;
	}
	
	@Override
	public void set(String key, boolean value) {
		put(key, value);
	}
	
	@Override
	public Observable<Boolean> booleanChanges(String key) {
		return changes(key, this::getBoolean);
	}
	
	
	
	@Override
	public void clear() {
		values.clear();
	}
}