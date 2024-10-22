package com.pocket.util.java;


public class Milliseconds {
	public static final long SECOND = 1000;
	public static final long MINUTE = SECOND * 60;
	public static final long HOUR = MINUTE * 60;
	public static final long DAY = HOUR * 24;
	public static final long WEEK = DAY * 7;
	public static final long YEAR = DAY * 365;
	
	/** @return The provided number of seconds as milliseconds */
	public static long seconds(int seconds) {
		return SECOND * seconds;
	}
	
	/** @return The provided number of minutes as milliseconds */
	public static long minutesToMillis(int minutes) {
		return MINUTE * minutes;
	}
	
	/** @return The provided number of millis as minutes */
	public static long millisToMinutes(long millis) {
		return (long) (millis / (double) MINUTE);
	}

	/**
	 * How long has it been since a previous time.
	 * 
	 * @param time
	 * @return
	 */
	public static long since(long time) {
		return System.currentTimeMillis() - time;
	}

	/**
	 * Converts a millis timestamp to a seconds one.
	 * @param millis
	 * @return
	 */
    public static long toSeconds(long millis) {
        return (long) (millis / (double) SECOND);
    }
	
	public static long fromNanos(double nanos) {
		return (long) (nanos / 1000000);
	}
}
