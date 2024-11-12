package com.pocket.util.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for accessing fields that might be null or whose parent fields might not exist.
 * <p>
 * For example, when accessing a value like feed[0].item.posts[0].profile.avatar_url
 * any of those could be null, or out of bounds in the array. If you just want null
 * or a default value in that case, these utilities can save a lot of verbosity by avoiding
 * handling all of the null checks along the way and just returning a default value if
 * any parent or target value is null or inaccessible.
 * <p>
 * Useful for Thing generated classes that have immutable, nullable, fields.
 */
public class Safe {
	
	/** Returns the value or 0 if the value is null. */
	public static int value(Integer value) {
		return value != null ? value : 0;
	}
	
	/** Returns the value or 0 if the value is null. */
	public static long value(Long value) {
		return value != null ? value : 0;
	}
	
	/** Returns the value or 0 if the value is null. */
	public static float value(Float value) {
		return value != null ? value : 0;
	}
	
	/** Returns the value or 0 if the value is null. */
	public static double value(Double value) {
		return value != null ? value : 0;
	}
	
	/** Returns the value or false if the value is null. */
	public static boolean value(Boolean value) {
		return value != null ? value : false;
	}
	
	/** Obtains this value or null if any exceptions were throw such as null pointers or index out of bounds or others. */
	public static <V> V get(Get<V> getter) {
		try {
			return getter.get();
		} catch (Throwable npe) {
			return null;
		}
	}
	
	/** A variant of {@link #get(Get)} that will return false if the value could not be obtained. */
	public static boolean getBoolean(Get<Boolean> getter) {
		return value(get(getter));
	}
	
	/** A variant of {@link #get(Get)} that will return 0 if the value could not be obtained. */
	public static int getInt(Get<Integer> getter) {
		return value(get(getter));
	}
	
	/** A variant of {@link #get(Get)} that will return 0 if the value could not be obtained. */
	public static long getLong(Get<Long> getter) {
		return value(get(getter));
	}
	
	public interface Get<V> {
		/** Do whatever is needed to obtain the value or throw an exception if it cannot be obtained. */
		V get() throws Exception;
	}

	public static <V> List<V> nonNullCopy(List<V> list) {
		if (list != null) return new ArrayList<>(list);
		return new ArrayList<>();
	}
	
	public static <K,V> Map<K,V> nonNullCopy(Map<K,V> map) {
		if (map != null) return new HashMap<>(map);
		return new HashMap<>();
	}

}
