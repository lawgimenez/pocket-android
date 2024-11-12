package com.pocket.sdk.api.value;

import com.pocket.sync.action.Time;

/**
 * The convention for timestamps in Pocket. A unix timestamp in seconds.
 */
public class Timestamp extends Time {

	/** A unix timestamp in seconds. */
	public final long unixSeconds;
	
	/**
	 * @param unixSeconds The unix timestamp in seconds.
	 * @see #fromMillis(long)
	 */
	public Timestamp(long unixSeconds) {
		super(unixSeconds);
		this.unixSeconds = unixSeconds;
	}
	
	/**
	 * @return As milliseconds.
	 */
	public long millis() {
		return unixSeconds * 1000L;
	}
	
	/**
	 * @return As seconds.
	 */
	public long seconds() {
		return unixSeconds;
	}
	
	/**
	 * @return Create a timestamp matching the current time.
	 */
	public static Timestamp now() {
		return new Timestamp(System.currentTimeMillis() / 1000L);
	}
	
	/**
	 * Create a timestamp from a milliseconds timestamp
	 * @param millis
	 * @return
	 */
	public static Timestamp fromMillis(long millis) {
		return new Timestamp(millis/1000L);
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		Timestamp timestamp = (Timestamp) o;
		
		if (unixSeconds != timestamp.unixSeconds) return false;
		
		return true;
	}
	
	@Override public int hashCode() {
		return (int) (unixSeconds ^ (unixSeconds >>> 32));
	}
	
	public static long get(Timestamp value) {
		return value != null ? value.unixSeconds : 0;
	}
}
