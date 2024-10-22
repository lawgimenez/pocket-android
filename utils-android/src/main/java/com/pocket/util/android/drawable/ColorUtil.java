package com.pocket.util.android.drawable;

import android.graphics.Color;
import android.graphics.LinearGradient;

import java.util.SortedSet;
import java.util.TreeSet;

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
	 * Same as {@code Color#argb((int) (255 * percent), b, b, b)}
	 * @param alpha
	 * @param brightness 0 is black, 255 is white
	 * @return
	 */
	public static final int gray(float alpha, int brightness) {
		return gray(to255(alpha), brightness);
	}
	
	/**
	 * Same as {@code ColorUtil.gray(255, brightness)}
	 * @param brightness 0 is black, 255 is white
	 * @return
	 */
	public static final int gray(int brightness) {
		return gray(255, brightness);
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
	 * A helper for making the colors and positions arrays for a gradient. For example when
	 * using the constructor {@link LinearGradient#LinearGradient(float, float, float, float, int[], float[], android.graphics.Shader.TileMode)}.
	 * <p>
	 * Allows the creation of these to be a little more readable.
	 * <p>
	 * Just create a new builder, add your colors with {@link #addColorAt(int, float)} and then extract the arrays with {@link #getColors()} and {@link #getPositions()}.
	 * @author max
	 *
	 */
	public static class GradientBuilder {
		
		private final SortedSet<GradientPoint> points = new TreeSet<GradientPoint>();
		
		public GradientBuilder() {}
		
		/**
		 * Add a new color at the specified location in the gradient. You do not need to invoke this
		 * in any specific order. The gradient will sort it automatically based on the position value.
		 * @param color
		 * @param position
		 * @return
		 */
		public GradientBuilder addColorAt(int color, float position) {
			points.add(new GradientPoint(color, position));
			return this;
		}
		
		public float[] getPositions() {
			float[] positions = new float[points.size()];
			int i = 0;
			for (GradientPoint point : points) {
				positions[i] = point.position;
				i++;
			}
			return positions;
		}
		
		public int[] getColors() {
			int[] colors = new int[points.size()];
			int i = 0;
			for (GradientPoint point : points) {
				colors[i] = point.color;
				i++;
			}
			return colors;
		}
		
		private class GradientPoint implements Comparable<GradientPoint> {
			private final int color;
			private final float position;
			
			private GradientPoint(int color, float location) {
				this.color = color;
				this.position = location;
			}

			@Override
			public int compareTo(GradientPoint another) {
				if (another.position == position) {
					return 0;
				} else if (another.position < position) {
					return 1;
				} else {
					return -1;
				} 
			}
			
		}
		
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
	
	public static String toHexString(int color) {
		return Integer.toHexString(color);
	}

    /**
     * Function taken from android.animation.ArgbEvaluator.
     *
     * @param progress (0-1) The amount of endColor to blend into the startColor. 0 returns the startColor, 1 returns the endColor.
     * @param startColor
     * @param endColor
     * @return
     */
    public static int mix(float progress, int startColor, int endColor) {
        int startInt = startColor;
        int startA = (startInt >> 24);
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = endColor;
        int endA = (endInt >> 24);
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(progress * (endA - startA))) << 24) |
                (int)((startR + (int)(progress * (endR - startR))) << 16) |
                (int)((startG + (int)(progress * (endG - startG))) << 8) |
                (int)((startB + (int)(progress * (endB - startB))));
    }

}
