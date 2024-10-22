package com.pocket.sync.value;

import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Used by methods like {@link com.pocket.sync.thing.Thing#toJson(Include...)} to control which values are included.
 */
public enum Include {
	/**
	 * Also include private and sensitive fields. This might include passwords or access tokens that must remain private.
	 * <b>Use of this needs to be carefully considered and reviewed.
	 * None of this information should be exposed in logs or written/persisted in plain text.
	 * This also means these values must not be passed to classes that you have no oversight of that might log or expose this information.
	 * </b>
	 */
	DANGEROUS,
	/**
	 * Include a {@link Thing#SERIALIZATION_TYPE_KEY} value with the {@link Thing#type()} value.
	 * Will not include this on sub things.
	 */
	OPEN_TYPE;

	public static Include[] EMPTY = new Include[0];

	/**
	 * Adds the include into the array, unless it is already present then returns the given array as is.
	 */
	public static Include[] add(Include[] into, Include include) {
		return ArrayUtils.contains(into, include) ? into : ArrayUtils.add(into, include);
	}

	/**
	 * Remove `value` from the `includes` array, assuming that is present. (Don't use this method if you don't know if it is present or not)
	 * This does a micro optimization to avoid extra work and array creation if there is only 1 element, it will return an empty array.
	 * This is because where this is used in code generation the vast majority of the time it can do this faster return.
	 * @return A different array instance with that value removed.
	 */
	public static Include[] removeAssumingPresent(Include[] includes, Include value) {
		return includes.length < 2 ? EMPTY : ArrayUtils.removeElement(includes, value);
	}

	/**
	 * @return true if `array` contains `value`.
	 */
	public static boolean contains(Include[] array, Include value) {
		return array != null && array.length != 0 && ArrayUtils.contains(array, value);
	}
}
