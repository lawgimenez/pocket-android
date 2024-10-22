package com.pocket.sdk.image.rule;




public abstract class ImageResizeRule {
	
	protected boolean mAllowUpScale;
	
	public ImageResizeRule(boolean allowUpscale) {
		mAllowUpScale = allowUpscale;
	}
	
	/**
	 * Get an inSampleSize
	 * 
	 * @param srcWidth The source/original width of the image to resize.
	 * @param srcHeight The source/original height of the image to resize.
	 * @return
	 */
	public abstract int getInSampleSize(int fromWidth, int fromHeight);
	
	/**
	 * Get the scale needed to resize from the provided size to the requested size.
	 * @param fromWidth
	 * @param fromHeight
	 * @return
	 */
	public abstract float getScale(int fromWidth, int fromHeight);

	
	/**
	 * Get the inSampleSize that would fit one dimension into the other. This will
	 * be the closest power of 2 that is >= toSize.
	 * 
	 * @param fromSize The source image size.
	 * @param toSize The size it is resizing to.
	 * @return
	 */
	protected int calculateInSampleSize(int fromSize, int toSize) {
		int inSampleSize = 1;
		
		if (fromSize < toSize && !mAllowUpScale) {
			toSize = fromSize; // Don't scale
		}
		
		// Check for errors and cancel (avoid infinite loop in the inSampleSize loop below)
		if (toSize < 1) {
			return inSampleSize; // dimension is invalid
		}
		
		/*
		 *  OPT? if the to and from sizes are such that the inital /2 would
		 *  be too drastic, and if it is a low memory device, consider doing
		 *   a inSampleSize that is not a power of two. It will be potentially
		 *   slower, but could make a big difference.
		 *   
		 *   For example, if a src image is 3998 and the requested size is 2000,
		 *   The next power of two down is 1999.
		 *   limiting it strictly to powers of 2 means it will have to decode the entire
		 *   3999 px bitmap, which would fail on many older devices.
		 *   
		 *   If the difference between using a power of two or not is over 1000 to 2000 px,
		 *   then it should try for a more accurate adjustment.
		 *   
		 *   actually maybe this isn't possible since insamplesize is an int, not a float.
		 */
		
		// Calculate the correct inSampleSize/scale value.  It should be a power of 2
		int inSampledDimension = fromSize;
		while (inSampledDimension / 2 >= toSize) {
			inSampledDimension /= 2;
			inSampleSize *= 2;
		}
		
		return inSampleSize;
	}
	
	protected float calculateScale(int fromSize, int toSize) {
		if (fromSize < toSize && !mAllowUpScale) {
			return 1;
		}
		
		return (float) toSize / fromSize;
	}
	
	/**
	 * @return The width passed to the constructor
	 */
	public abstract int getConstructorWidth();
	/**
	 * @return The height passed to the constructor
	 */
	public abstract int getConstructorHeight();
	
	/**
	 * The width the image will have after being resized.
	 * 
	 * @param fromWidth
	 * @param fromHeight
	 * @param scale
	 * @return
	 */
	public abstract int getResizedWidth(int fromWidth, int fromHeight, float scale);
	
	/**
	 * The height the image will have after being resized.
	 * @param fromWidth
	 * @param fromHeight
	 * @param scale
	 * @return
	 */
	public abstract int getResizedHeight(int fromWidth, int fromHeight, float scale);
	
	/**
	 * Return a width value to use in a file name for the resized image.
	 * @return
	 */
	public abstract String getWidthFileName();
	
	/**
	 * Return a height value to use in a file name for the resized image.
	 * @return
	 */
	public abstract String getHeightFileName();
	
	/**
	 * A class that contains a scaling value and the inSampleSize needed to resize a source image to the request size.
	 * 
	 * @author max
	 *
	 */
	public static class Scaling {
		
		public final int inSampleSize;
		public final float postInSampledScale;
		
		public Scaling (int px, int inSampleSize, float scale) {
			this.inSampleSize = inSampleSize;
			this.postInSampledScale = scale;
		}
		
	}
	
}
