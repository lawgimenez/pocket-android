package com.pocket.util.java;


import org.apache.commons.lang3.StringUtils;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class StopWatch {

	private long currentStart;
	private long total;
	private long min;
	private long max;
	private long lastLap;
	private int intervals;
	
	public StopWatch() {
		warmup();
	}
	
	private void warmup() {
		resume();
		pause();
		reset();
	}
	
	public void resume() {
		if (currentStart == 0) {
			currentStart = System.nanoTime();
			intervals++;
		}
	}
	
	public void pause() {
		if (currentStart > 0) {
			long lap = System.nanoTime()-currentStart;
			total += lap;
			if (intervals == 1) {
				max = lap;
				min = lap;
			} else {
				max = Math.max(max, lap);
				min = Math.min(min, lap);
			}
			lastLap = lap;
		}
		currentStart = 0;
	}
	
	/**
	 * Manually record a lap in nanoseconds.
	 * Can be useful for combining results in a multithreaded case.
	 */
	public synchronized void addLap(long lap) {
		intervals++;
		total += lap;
		if (intervals == 1) {
			max = lap;
			min = lap;
		} else {
			max = Math.max(max, lap);
			min = Math.min(min, lap);
		}
		lastLap = lap;
	}
	
	/**
	 * Combine results.
	 * Can be useful for combining results in a multithreaded case.
	 */
	public synchronized void merge(StopWatch src) {
		currentStart = 0;
		total += src.total;
		min = Math.min(min, src.min);
		max = Math.max(max, src.max);
		lastLap = src.lastLap;
		intervals += src.intervals;
	}
	
	public long length() {
		return millis(lengthNanos());
	}
	
	public long lengthNanos() {
		long length = total;
		if (currentStart > 0) {
			length += System.nanoTime()-currentStart;
		}
		return length;
	}
	
	public long intervals() {
		return intervals;
	}
	
	public long avg() {
		return millis(avgNanos());
	}
	
	public long avgNanos() {
		if (intervals > 0) {
			return (long) (lengthNanos() / (double) intervals);
		} else {
			return -1;
		}
	}
	
	public long min() {
		return millis(min);
	}
	
	public long minNanos() {
		return min;
	}
	
	public long max() {
		return millis(max);
	}
	
	public long maxNanos() {
		return max;
	}
	
	public void reset() {
		currentStart = 0;
		total = 0;
		intervals = 0;
		min = 0;
		max = 0;
	}
	
	public long lastLap() {
		return millis(lastLap);
	}
	
	public long lastLapNanos() {
		return lastLap;
	}
	
	/**
	 * Returns the current {@link #length()} and resets the time back to 0.
	 * If already running, it continues running, if stopped it stays stopped.
	 * @return
	 */
	public long extract() {
		return millis(extractNanos());
	}
	
	public long extractNanos() {
		long length = lengthNanos();
		total = 0;
		if (currentStart > 0) {
			currentStart = System.nanoTime();
		}
		return length;
	}
	
	@Override
	public String toString() {
		return prettyPrint();
	}
	
	public String log() {
		return "laps:" + intervals +
				" min:"+ min() +
				" max:"+ max() +
				" avg:"+ avg() +
				" total:"+ length();
	}
	
	public String logNanos() {
		return "laps:" + intervals +
				" min:"+ minNanos() +
				" max:"+ maxNanos() +
				" avg:"+ avgNanos() +
				" total:"+ lengthNanos();
	}
	
	/**
	 * Produces a log of all values in fixed column widths so when comparing multiple outputs they line up in a readable way.
	 * For example, here are four example outputs of this method in a row:
	 * <pre>
	 * 300      laps |     11.905ms min |    110.004ms max |     26.501ms avg |   7950.223ms total
	 * 300      laps |     11.611ms min |     56.118ms max |     22.565ms avg |   6769.625ms total
	 * 300      laps |      0.000ms min |      0.037ms max |      0.002ms avg |      0.496ms total
	 * 300      laps |      0.000ms min |      0.006ms max |      0.000ms avg |      0.105ms total
	 * </pre>
	 */
	public synchronized String prettyPrint() {
		StringBuilder b = new StringBuilder();
		b.append(StringUtils.rightPad(String.valueOf(intervals), 8)).append(" laps | ");
		b.append(formatted(lastLap, 6,3)).append("ms lap | "); // The previous lap
		b.append(formatted(minNanos(), 6,3)).append("ms min | ");
		b.append(formatted(maxNanos(), 6,3)).append("ms max | ");
		b.append(formatted(avgNanos(), 6,3)).append("ms avg | ");
		b.append(formatted(lengthNanos(), 6,3)).append("ms total");
		return b.toString();
	}
	
	public static String formatted(long nanoseconds, int integerPlacesMin, int decimalPlacesMin) {
		DecimalFormat df = new DecimalFormat(StringUtils.repeat('#', integerPlacesMin) + "." + StringUtils.repeat('#', decimalPlacesMin));
		df.setRoundingMode(RoundingMode.HALF_UP);
		String str = df.format(millisPrecise(nanoseconds));
		String[] parts = StringUtils.split(str, ".");
		return StringUtils.leftPad(parts[0], integerPlacesMin, ' ')
				+ "."
				+ StringUtils.rightPad(parts.length > 1 ? parts[1] : "", decimalPlacesMin, '0');
	}
	
	private static long millis(long nanos) {
		return (long) millisPrecise(nanos);
	}
	
	private static double millisPrecise(long nanos) {
		return nanos / 1000000.0;
	}
	
	
}