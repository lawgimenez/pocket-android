package com.pocket.app.reader.internal.article.image;

import android.graphics.Matrix;

public class ImageMatrix extends Matrix { // OPT better name

	private float[] mFloatValues = new float[]{0,0,0,
										0,0,0,
										0,0,0};
	private Values mValues = new Values();
	private float mBitmapWidth;
	private float mBitmapHeight;
	
	public void setBitmapSize(float width, float height){
		mBitmapWidth = width;
		mBitmapHeight = height;
	}
	
	public Values getValues(){
		getValues(mFloatValues);
		mValues.update(mFloatValues);
		return mValues;
	}
	
	public class Values {
		
		public float x; // OPT private?
		public float y;
		public float scale;
		public float scaledWidth;
		public float scaledHeight;
		public float fullWidth;
		public float fullHeight;
		
		private void update(float[] values){
			x = values[Matrix.MTRANS_X];
			y = values[Matrix.MTRANS_Y];
			scale = values[Matrix.MSCALE_X]; // REVIEW this is more reliable? works after rotation: scale = mapRadius(1f) - mapRadius(0f);
			fullWidth = mBitmapWidth;
			fullHeight = mBitmapHeight;
			scaledWidth = fullWidth * scale;
			scaledHeight = fullHeight * scale;
		}
		
		public float getFramePadding(int viewWidth){
			// If smaller than viewport, make sure the frame is the viewport width
			return scaledWidth < viewWidth ? (viewWidth - scaledWidth) / 2 : 0;
		}
		
	}

	public void set(ImageMatrix src) {
		setBitmapSize(src.getBitmapWidth(), src.getBitmapHeight());
		super.set(src);
	}
	
	private float getBitmapWidth() {
		return mBitmapWidth;
	}
	
	private float getBitmapHeight() {
		return mBitmapHeight;
	}
	
	
		
}
