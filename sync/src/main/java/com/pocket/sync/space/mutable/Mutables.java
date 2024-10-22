package com.pocket.sync.space.mutable;

import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Methods that generated {@link Thing}'s {@link MutableThing} implementation needs to function.
 */
public interface Mutables {
	
	/**
	 * Imprint the provided thing into the mutable space and return the mutable instance of this thing.
	 * If the thing doesn't exist yet, it creates a new mutable instance in the space, otherwise it returns the existing instance with the latest values updated.
	 * @param root The {@link MutableThing#root()} to use for this value.
	 */
	<T extends Thing> MutableThing<T> imprint(T value, MutableThing root);
	
	/**
	 * Imprint a list of things and return a list of mutable instances. See {@link #imprint(Thing, MutableThing parent)} for details.
	 * If the list is null or empty the provided value is returned back.
	 */
	<T extends Thing> List<MutableThing<T>> imprint(List<T> value, MutableThing root);
	
	/**
	 * Imprint a map of things and return a list of mutable instances. See {@link #imprint(Thing, MutableThing parent)} for details.
	 * If the map is null or empty the provided value is returned back.
	 */
	<T extends Thing> Map<String, MutableThing<T>> imprint(Map<String, T> value, MutableThing root);
	
	/**
	 * Mark that this thing has changed due to an imprint.
	 */
	void flagChanged(MutableThing thing);
	
	/** Register that the `parent` thing references the `child` */
	void link(MutableThing parent, MutableThing child);
	/** Register that the `parent` thing references things in this collection */
	void link(MutableThing parent, Collection<? extends MutableThing> children);
	/** Register that the `parent` thing references things in this collection */
	void link(MutableThing parent, Map<String, ? extends MutableThing> children);
	/** Unregister a reference */
	void unlink(MutableThing parent, MutableThing child);
	/** Unregister references */
	void unlink(MutableThing parent, Collection<? extends MutableThing> children);
	/** Unregister references */
	void unlink(MutableThing parent, Map<String, ? extends MutableThing> children);
	
	/**
	 * @return Return an immutable instance of the thing. (or null if null)
	 */
	static <T extends Thing> T build(MutableThing<T> value) {
		return value != null ? value.build() : null;
	}
	
	/**
	 * Return a list of immutable versions of the provided mutable instances.
	 */
	static <T extends Thing> List<T> build(List<MutableThing<T>> value) {
		if (value == null) return null;
		if (value.isEmpty()) return Collections.emptyList();
		List<T> list = new ArrayList<>(value.size());
		for (MutableThing<T> t : value) {
			list.add(t != null ?  t.build() : null);
		}
		return Collections.unmodifiableList(list);
	}
	
	/**
	 * Return a map of immutable versions of the provided mutable instances.
	 */
	static <T extends Thing> Map<String, T> build(Map<String, MutableThing<T>> value) {
		if (value == null) return null;
		if (value.isEmpty()) return Collections.emptyMap();
		HashMap<String, T> map = new HashMap<>(value.size());
		for (Map.Entry<String, MutableThing<T>> t : value.entrySet()) {
			MutableThing<T> v = t.getValue();
			map.put(t.getKey(), v != null ? v.build() : null);
		}
		return Collections.unmodifiableMap(map);
	}
	
	/**
	 * Check if this is referencing a different thing as before?
	 * @return true if the reference changed, false if it is still the same thing referenced.
	 */
	static <T extends Thing> boolean changed(MutableThing<T> from, T to) {
		return ObjectUtils.notEqual(from != null ? from.identity() : null, to != null ? to.identity() : null);
	}
	
	/**
	 * Check if this collection has changed what things it is referencing or the order of them?
	 * @return true if the references changed, false if it is still the same things referenced and in the same order.
	 */
	static <T extends Thing> boolean changed(List<MutableThing<T>> from, List<T> to) {
		if (from == null) {
			return to != null;
		} else if (to == null) {
			return true;
		} else if (from.size() != to.size()) {
			return true;
		}
		for (int i = 0, len = to.size(); i < len; i++) {
			if (changed(from.get(i), to.get(i))) return true;
		}
		return false;
	}
	
	/**
	 * Check if this collection has changed what things it is referencing or the order of them?
	 * @return true if the references changed, false if it is still the same things referenced and in the same order.
	 */
	static <T extends Thing> boolean changed(Map<String, MutableThing<T>> from, Map<String, T> to) {
		if (from == null) {
			return to != null;
		} else if (to == null) {
			return true;
		} else if (from.size() != to.size()) {
			return true;
		}
		// Algorithm partly based on AbstractMap.equals
		for (Map.Entry<String, MutableThing<T>> e : from.entrySet()) {
			String key = e.getKey();
			MutableThing<T> value = e.getValue();
			if (value == null) {
				if (!(to.get(key) == null && to.containsKey(key))) {
					return false;
				}
			} else if (!value.identity().equals(to.get(key))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return true if a different value, false if the same
	 */
	static boolean changed(Object from, Object to) {
		return ObjectUtils.notEqual(from, to);
	}
	
}
