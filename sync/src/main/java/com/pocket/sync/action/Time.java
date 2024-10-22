package com.pocket.sync.action;

import org.jetbrains.annotations.NotNull;

/**
 * A moment in time. Up to implementations to decide what the value means.
 * For example, some might decide it means seconds, milliseconds or nanoseconds past epoch.
 */

public class Time implements Comparable<Time> {
	
	public final long value;
	
	public Time(long value) {
		this.value = value;
	}
	
	@Override
	public int compareTo(@NotNull Time o) {
		return (value < o.value) ? -1 : ((value == o.value) ? 0 : 1);
	}
}
