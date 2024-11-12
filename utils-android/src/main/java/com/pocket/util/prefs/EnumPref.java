package com.pocket.util.prefs;

import io.reactivex.Observable;

/** Implementation of {@link EnumPreference} */
public class EnumPref<E extends Enum<E>> implements EnumPreference<E> {
	
	private final String key;
	private final Store store;
	private final E defaultValue;
	private final Class<E> clazz;
	
	public EnumPref(Class<E> clazz, String key, E defaultValue, Store store) {
		this.clazz = clazz;
		this.key = key;
		this.defaultValue = defaultValue;
		this.store = store;
	}
	
	@Override
	public E get() {
		return isSet() ? from(store.getString(key)) : defaultValue;
	}
	
	private E from(String value) {
		return value != null ? Enum.valueOf(clazz, value) : null;
	}
	
	@Override
	public void set(E value) {
		store.set(key, value != null ? value.toString() : null);
	}
	
	@Override
	public boolean isSet() {
		return store.contains(key);
	}
	
	@Override
	public Observable<E> changes() {
		return store.stringChanges(key).map(this::from);
	}
	
	@Override
	public Observable<E> getWithChanges() {
		return changes().startWith(get());
	}
}