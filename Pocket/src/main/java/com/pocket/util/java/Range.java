package com.pocket.util.java;

public class Range {
	
	public int min;
	public int max;
	
	public Range() {
		this(0,0);
	}
	
	public Range(int min, int max) {
		set(min, max);
	}
	
	public void set(int min, int max) {
		this.min = min;
		this.max = max;
	}
	
	@Override
	public String toString() {
		return min + "-" + max;
	}

	/**
	 * @param value
	 * @return A value <= the max and >= the min of the range. If the value is greater than the max, the max is returned. If the value is less than the min the min is returned. If the value is already within range it is returned.
	 */
	public int limit(int value) {
		if (value >= max) {
			return max;
		} else if (value <= min) {
			return min;
		}
		return value;
	}

	public boolean contains(int value) {
		return isWithin(min, max, value);
	}

	public static float limit(float min, float max, float value) {
		if (value < min) {
			return min;
		} else if (value > max) {
			return max;
		} else { 
			return value;
		}
	}
	
	public static int limit(int min, int max, int value) {
		if (value < min) {
			return min;
		} else if (value > max) {
			return max;
		} else {
			return value;
		}
	}
	
	public static boolean isWithin(float min, float max, float value) {
		return value <= max && value >= min;
	}


    public static float percentBetween(float percent, float min, float max) {
        float length = max - min;
        float position = percent * length;
        return min + position;
    }

}
