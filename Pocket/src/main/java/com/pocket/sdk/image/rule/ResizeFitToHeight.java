package com.pocket.sdk.image.rule;

public class ResizeFitToHeight extends ImageResizeRule {

	private final int mHeight;

	public ResizeFitToHeight(boolean allowUpscale, int heightPx) {
		super(allowUpscale);
		
		mHeight = heightPx;
	}
	
	@Override
	public int getInSampleSize(int fromWidth, int fromHeight) {
		return calculateInSampleSize(fromHeight, mHeight);
	}
	
	@Override
	public float getScale(int fromWidth, int fromHeight) {
		return calculateScale(fromHeight, mHeight);
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
		return "0";
	}

	@Override
	public String getHeightFileName() {
		return String.valueOf(mHeight);
	}
	
	@Override
	public int getConstructorWidth() {
		return 0;
	}

	@Override
	public int getConstructorHeight() {
		return mHeight;
	}

}
