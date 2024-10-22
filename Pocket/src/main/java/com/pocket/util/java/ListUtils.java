package com.pocket.util.java;

import java.util.Collection;
import java.util.List;

public abstract class ListUtils {
	
	/**
	 * Safe checking of empty when list might be null.
	 * 
	 * @param list
	 * @return
	 */
	public static <E> boolean isEmpty(List<E> list) {
		return list == null || list.isEmpty();
	}
	
	/**
	 * Safe checking of size when list might be null.
	 * 
	 * @param list
	 * @return 0 if null or empty, otherwise the size.
	 */
	public static int size(List<?> list) {
		return list == null ? 0 : list.size();
	}

	/**
	 * Null safe version of contains
	 * @param collection if null, always returns false
	 * @param obj if null, always returns false (so not helpful if you are searching for a null entry)
	 * @return
	 */
	public static boolean contains(Collection<?> collection, Object obj) {
		if (collection == null || obj == null) {
			return false;
		} else {
			return collection.contains(obj);
		}
	}
}
