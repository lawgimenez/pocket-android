package com.pocket.util.android;

import android.os.Bundle;
import android.os.Parcelable;

/**
 * TODO Documentation
 */

public class BundleUtil {
	
	/**
	 * Safely unbundle a custom parcelable class, avoiding class not found errors.
	 *
	 * @param bundle
	 * @param key
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T extends Parcelable> T getParcelable(Bundle bundle, String key, Class<T> clazz) {
		if (bundle == null || !bundle.containsKey(key)) {
			return null;
		}
		ClassLoader before = bundle.getClassLoader();
		bundle.setClassLoader(clazz.getClassLoader());
		T value = bundle.getParcelable(key);
		bundle.setClassLoader(before);
		return value;
	}

}
