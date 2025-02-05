package com.pocket.util.android.drawable;

import android.graphics.Color;

public abstract class ColorUtil {
	
	/**
	 * Same as {@code Color#argb(alpha, b, b, b)}
	 * @param alpha
	 * @param brightness 0 is black, 255 is white
	 * @return
	 */
	public static final int gray(int alpha, int brightness) {
		return Color.argb(alpha, brightness, brightness, brightness);
	}

	/**
	 * Convert [0...1] to [0...255]
	 * @param percent
	 * @return
	 */
	public static final int to255(float percent) {
		return (int) (255 * percent);
	}

	/**
	 * Returns the color with the alpha channel set to the provided value. 
	 */
	public static int setAlpha(float alpha, int color) {
		return setAlpha(to255(alpha), color);
	}
	
	/**
	 * Returns the color with the alpha channel set to the provided value.
	 */
	public static int setAlpha(int alpha, int color) {
		return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
	}

	public static String toString(int color) {
		return Color.alpha(color) + "|" + Color.red(color) + "|" + Color.green(color) + "|" +Color.blue(color);
	}
}
