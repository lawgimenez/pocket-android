package com.pocket.sdk.image.rule;

/**
 * Resizes an image to fit within the bounds set by the width and height. If the source image is larger than the bounds, then it will
 * shrink the image, maintaining the aspect ratio, so that both width and height are equal or less than the requested size.
 * 
 * If the image is already smaller than the requested size no resizing will occur.
 * 
 * @author max
 *
 */
public class ResizeFitWithin extends ImageResizeRule {

	private final int mMaxWidth;
	private final int mMaxHeight;

	public ResizeFitWithin(int maxWidthPx, int maxHeightPx) {
		super(false);
		
		mMaxWidth = maxWidthPx;
		mMaxHeight = maxHeightPx;
	}

	@Override
	public int getInSampleSize(int fromWidth, int fromHeight) {
		float horizontalRatio = mMaxWidth / (float) fromWidth;
		float verticalRatio = mMaxHeight / (float) fromHeight;
		
		if (horizontalRatio < verticalRatio) {
			return calculateInSampleSize(fromWidth, mMaxWidth);
		} else {
			return calculateInSampleSize(fromHeight, mMaxHeight);
		}
	}
	
	@Override
	public float getScale(int fromWidth, int fromHeight) {
		float horizontalRatio = mMaxWidth / (float) fromWidth;
		float verticalRatio = mMaxHeight / (float) fromHeight;
		
		if (horizontalRatio < verticalRatio) {
			return calculateScale(fromWidth, mMaxWidth);
		} else {
			return calculateScale(fromHeight, mMaxHeight);
		}
	}
	
	@Override
	public int getResizedWidth(int fromWidth, int fromHeight, float scale) {
		return (int) (fromWidth * scale);
	}
	
	@Override
	public int getResizedHeight(int fromWidth, int fromHeight, float scale) {
		return (int) (fromHeight * scale);
	}

	@Override
	public String getWidthFileName() {
		return String.valueOf(mMaxWidth);
	}

	@Override
	public String getHeightFileName() {
		return String.valueOf(mMaxHeight);
	}
	
	@Override
	public int getConstructorWidth() {
		return mMaxWidth;
	}

	@Override
	public int getConstructorHeight() {
		return mMaxHeight;
	}
	
}
