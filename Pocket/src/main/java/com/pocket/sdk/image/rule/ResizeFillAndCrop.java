package com.pocket.sdk.image.rule;

/**
 * Fills the provided width and height. The image will be resized to match at least one dimension 
 * (either width or height) exactly. The other dimension will be centered and have any extra
 * cropped off.
 * 
 * This will guarantee the width and height are completely
 * filled with an image and the resulting image will be exactly the requested width and height.
 * 
 * This always allows upscaling of images.
 * 
 * OPT the ImageResizer could use a BitmapRegionDecoder to only pull the part of the image it needs to avoid using extra memory and having to crop.
 * 
 * @author max
 *
 */
public class ResizeFillAndCrop extends ImageResizeRule {

	/**
	 * Filling based on height, cropping off the extra width.
	 */
	private static final int CROP_WIDTH = 1;
	/**
	 * Filling based on width, cropping off the extra height.
	 */
	private static final int CROP_HEIGHT = 2;
	
	private final int mMinWidth;
	private final int mMinHeight;
	
	private int mCenterAndCropDimension;

	public ResizeFillAndCrop(int minWidthPx, int minHeightPx) {
		super(true);
		
		mMinWidth = minWidthPx;
		mMinHeight = minHeightPx;
	}

	public ResizeFillAndCrop(float fromWidth, float fromHeight) {
		this((int) fromWidth, (int) fromHeight);
	}

	@Override
	public int getInSampleSize(int fromWidth, int fromHeight) {
		float horizontalRatio = mMinWidth / (float) fromWidth;
		float verticalRatio = mMinHeight / (float) fromHeight;
		
		if (horizontalRatio > verticalRatio) {
			mCenterAndCropDimension = CROP_HEIGHT;
			return calculateInSampleSize(fromWidth, mMinWidth);
		} else {
			mCenterAndCropDimension = CROP_WIDTH;
			return calculateInSampleSize(fromHeight, mMinHeight);
		}
	}
	
	@Override
	public float getScale(int fromWidth, int fromHeight) {
		float horizontalRatio = mMinWidth / (float) fromWidth;
		float verticalRatio = mMinHeight / (float) fromHeight;
		
		if (horizontalRatio > verticalRatio) {
			mCenterAndCropDimension = CROP_HEIGHT;
			return calculateScale(fromWidth, mMinWidth);
		} else {
			mCenterAndCropDimension = CROP_WIDTH;
			return calculateScale(fromHeight, mMinHeight);
		}
	}
	
	@Override
	public int getResizedWidth(int fromWidth, int fromHeight, float scale) {
		return mMinWidth;
	}
	
	@Override
	public int getResizedHeight(int fromWidth, int fromHeight, float scale) {
		return mMinHeight;
	}

	@Override
	public String getWidthFileName() {
		return String.valueOf(mMinWidth);
	}

	@Override
	public String getHeightFileName() {
		return String.valueOf(mMinHeight);
	}

	@Override
	public int getConstructorWidth() {
		return mMinWidth;
	}

	@Override
	public int getConstructorHeight() {
		return mMinHeight;
	}

}
