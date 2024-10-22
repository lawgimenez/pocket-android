package com.pocket.sdk.image.rule;


public class ResizeFitToWidth extends ImageResizeRule {

	private final int mWidth;

	public ResizeFitToWidth(boolean allowUpscale, int widthPx) {
		super(allowUpscale);
		mWidth = widthPx;
	}

	@Override
	public int getInSampleSize(int fromWidth, int fromHeight) {
		return calculateInSampleSize(fromWidth, mWidth);
	}
	
	@Override
	public float getScale(int fromWidth, int fromHeight) {
		return calculateScale(fromWidth, mWidth);
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
	 return String.valueOf(mWidth);
	}

	@Override
	public String getHeightFileName() {
		return "0";
	}
	
	@Override
	public int getConstructorWidth() {
		return mWidth;
	}

	@Override
	public int getConstructorHeight() {
		return 0;
	}
	
}
