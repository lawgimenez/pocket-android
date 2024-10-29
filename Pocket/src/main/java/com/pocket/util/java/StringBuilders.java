package com.pocket.util.java;

import java.util.ArrayList;

import com.pocket.app.App;

/**
 * A singleton for recycling {@link StringBuilder} instances. To obtain one call {@link #get()}
 * and return it to the recycler when you are done with {@link #recycle(StringBuilder)}.
 * <p>
 * <b>Be sure to not use it after calling recycle!</b>
 */
public class StringBuilders {

	private static final ArrayList<StringBuilder> mBuilders = new ArrayList<StringBuilder>();
	
	private static final Object LOCK = new Object();
	
	public static StringBuilder get() {
		StringBuilder builder;
		synchronized (LOCK) {
			builder = mBuilders.isEmpty() ? null : mBuilders.remove(0);
		}
		
		if (builder != null) {
			return builder;
		}
		
		return new StringBuilder();
	}
	
	public static void recycle(StringBuilder builder) {
		reset(builder);
		synchronized (LOCK) {
			mBuilders.add(builder);
		}
	}

	public static void reset(StringBuilder builder) {
		builder.setLength(0);
	}
}
