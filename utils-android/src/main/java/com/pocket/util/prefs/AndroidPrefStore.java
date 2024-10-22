package com.pocket.util.prefs;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Observable;

/**
 * A {@link Store} backed by Android {@link SharedPreferences}
 */
public class AndroidPrefStore implements Store {
		
	private final SharedPreferences prefs;
	
	public AndroidPrefStore(SharedPreferences prefs) {
		this.prefs = prefs;
	}
	
	@Override
	public Observable<String> changes() {
		return Observable.create(emitter -> {
			final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
				if (key == null) return; // OnSharedPreferenceChangeListener.onSharedPreferenceChanged makes a callback with a null key whenever clear() is called, which we don't need to emit.
				emitter.onNext(key);
			};
			emitter.setCancellable(() -> prefs.unregisterOnSharedPreferenceChangeListener(listener));
			prefs.registerOnSharedPreferenceChangeListener(listener);
		});
	}
	
	private <T> Observable<T> changes(String key, Get<T> getter) {
		return changes()
				.filter(changed -> changed.equals(key))
				.map(k -> getter.get(key));
	}
	
	interface Get<T> {
		T get(String key);
	}
	
	@Override
	public boolean contains(String key) {
		return prefs.contains(key);
	}
	
	@Override
	public void remove(String key) {
		prefs.edit().remove(key).apply();
	}
	
	@Override
	public Set<String> keys() {
		return prefs.getAll().keySet();
	}
	
	@Override
	public String getString(String key) {
		return prefs.getString(key, null);
	}
	
	@Override
	public void set(String key, String value) {
		prefs.edit().putString(key, value).apply();
	}
	
	@Override
	public Observable<String> stringChanges(String key) {
		return changes(key, this::getString);
	}
	
	
	@Override
	public Set<String> getStringSet(String key) {
		Set<String> v = prefs.getStringSet(key, null);
		return v != null ? Collections.unmodifiableSet(v) : null;
	}
	
	@Override
	public void set(String key, Set<String> value) {
		value = value != null ? new HashSet<>(value) : null; // Make a copy so the set we are writing won't throw concurrent mod exceptions
		prefs.edit().putStringSet(key, value).apply();
	}
	
	@Override
	public Observable<Set<String>> stringSetChanges(String key) {
		return changes(key, this::getStringSet);
	}
	
	
	@Override
	public int getInt(String key) {
		return prefs.getInt(key, 0);
	}
	
	@Override
	public void set(String key, int value) {
		prefs.edit().putInt(key, value).apply();
	}
	
	@Override
	public Observable<Integer> intChanges(String key) {
		return changes(key, this::getInt);
	}
	
	
	@Override
	public float getFloat(String key) {
		return prefs.getFloat(key, 0);
	}
	
	@Override
	public void set(String key, float value) {
		prefs.edit().putFloat(key, value).apply();
	}
	
	@Override
	public Observable<Float> floatChanges(String key) {
		return changes(key, this::getFloat);
	}
	
	
	@Override
	public long getLong(String key) {
		return prefs.getLong(key, 0);
	}
	
	@Override
	public void set(String key, long value) {
		prefs.edit().putLong(key, value).apply();
	}
	
	@Override
	public Observable<Long> longChanges(String key) {
		return changes(key, this::getLong);
	}
	
	
	@Override
	public boolean getBoolean(String key) {
		return prefs.getBoolean(key, false);
	}
	
	@Override
	public void set(String key, boolean value) {
		prefs.edit().putBoolean(key, value).apply();
	}
	
	@Override
	public Observable<Boolean> booleanChanges(String key) {
		return changes(key, this::getBoolean);
	}
	
	@Override
	public void clear() {
		prefs.edit().clear().apply();
	}
}