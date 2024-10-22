package com.pocket.util.prefs;

import java.util.Set;

import io.reactivex.Observable;

public class Prefs implements Preferences {
	
	private final Store user;
	private final Store app;
	
	public Prefs(Store user, Store app) {
		this.user = user;
		this.app = app;
	}
	
	@Override
	public void clearUser() {
		user.clear();
	}
	
	@Override
	public void clear() {
		user.clear();
		app.clear();
	}
	
	@Override
	public void remove(String key) {
		user.remove(key);
		app.remove(key);
	}
	
	@Override
	public Set<String> appKeys() {
		return app.keys();
	}
	
	@Override
	public Set<String> userKeys() {
		return user.keys();
	}
	
	
	
	@Override
	public BooleanPreference forUser(String key, boolean defaultValue) {
		return new BooleanPref(key, defaultValue, user);
	}
	
	@Override
	public BooleanPreference forApp(String key, boolean defaultValue) {
		return new BooleanPref(key, defaultValue, app);
	}
	
	
	@Override
	public FloatPreference forUser(String key, float defaultValue) {
		return new FloatPref(key, defaultValue, user);
	}
	
	@Override
	public FloatPreference forApp(String key, float defaultValue) {
		return new FloatPref(key, defaultValue, app);
	}
	
	
	@Override
	public IntPreference forUser(String key, int defaultValue) {
		return new IntPref(key, defaultValue, user);
	}
	
	@Override
	public IntPreference forApp(String key, int defaultValue) {
		return new IntPref(key, defaultValue, app);
	}
	
	
	@Override
	public LongPreference forUser(String key, long defaultValue) {
		return new LongPref(key, defaultValue, user);
	}
	
	@Override
	public LongPreference forApp(String key, long defaultValue) {
		return new LongPref(key, defaultValue, app);
	}
	
	
	@Override
	public StringPreference forUser(String key, String defaultValue) {
		return new StringPref(key, defaultValue, user);
	}
	
	@Override
	public StringPreference forApp(String key, String defaultValue) {
		return new StringPref(key, defaultValue, app);
	}
	
	
	@Override
	public StringSetPreference forUser(String key, Set<String> defaultValue) {
		return new StringSetPref(key, defaultValue, user);
	}
	
	@Override
	public StringSetPreference forApp(String key, Set<String> defaultValue) {
		return new StringSetPref(key, defaultValue, app);
	}
	
	
	@Override
	public <E extends Enum<E>> EnumPreference<E> forUser(String key, Class<E> clazz, E defaultValue) {
		return new EnumPref<>(clazz, key, defaultValue, user);
	}
	
	@Override
	public <E extends Enum<E>> EnumPreference<E> forApp(String key, Class<E> clazz, E defaultValue) {
		return new EnumPref<>(clazz, key, defaultValue, app);
	}
	
	
	@Override
	public Preferences group(String name) {
		// TODO also add some protection so someone can't create a preference with this prefix
		return new PrefixPreferences(this, name);
	}
	
	@Override
	public Observable<String> changes() {
		return Observable.merge(user.changes(), app.changes());
	}
}