package com.pocket.util.android.drawable;


/*******************************************************************************
Inspired by Chris Banes' BitmapLruCache https://github.com/chrisbanes/Android-BitmapMemoryCache

Which has an Apache 2.0 license.

 *******************************************************************************/


import android.graphics.Bitmap;

import com.pocket.sdk.image.ImageCache;
import com.pocket.util.java.Logs;

public class CacheableBitmapWrapper {

	private final Bitmap mBitmap;
	private final String mKey;

	// Number of ImageViews currently showing bitmap
	private int mImageViewsCount;

	// Number of caches currently referencing the wrapper
	private int mCacheCount;

	public CacheableBitmapWrapper(Bitmap bitmap, String localPath) {
		if (null == bitmap) {
			throw new IllegalArgumentException("Bitmap can not be null");
		}

		mKey = localPath;
		mBitmap = bitmap;
		mImageViewsCount = 0;
		mCacheCount = 0;
	}

	/**
	 * @return true - if the bitmap is currently being displayed by a
	 *         {@link CacheableImageView}.
	 */
	public boolean isBeingDisplayed() {
		return mImageViewsCount > 0;
	}

	/**
	 * Returns the currently reference Bitmap
	 * 
	 * @return Bitmap - referenced Bitmaps
	 */
	public Bitmap getBitmap() {
		return mBitmap;
	}

	/**
	 * Returns true when this wrapper has a bitmap and the bitmap has not been
	 * recycled.
	 * 
	 * @return true - if the bitmap has not been recycled.
	 */
	public boolean hasValidBitmap() {
		return !mBitmap.isRecycled();
	}

	/**
	 * Used to signal to the wrapper whether it is being referenced by a cache
	 * or not.
	 * 
	 * @param added
	 *            - true if the wrapper has been added to a cache, false if
	 *            removed.
	 */
	void setCached(boolean added) {
		if (added) {
			mCacheCount++;
		} else {
			mCacheCount--;
		}
		checkState();
	}

	/**
	 * Used to signal to the wrapper whether it is being used or not. Being used
	 * could be that it is being displayed by an ImageView.
	 * 
	 * @param beingUsed
	 *            - true if being used, false if not.
	 */
	public void setBeingUsed(boolean beingUsed) {
		if (beingUsed) {
			mImageViewsCount++;
		} else {
			mImageViewsCount--;
		}
		checkState();
	}

	/**
	 * Checks whether the wrapper is currently referenced, and is being
	 * displayed. If neither of those conditions are met then the bitmap is
	 * recycled and freed.
	 */
	private void checkState() {
		if (ImageCache.DEBUG) Logs.d("ImageCache", "bitmap checkState " + mImageViewsCount + "," + mCacheCount + " " + mKey);
		
		if (mCacheCount <= 0 && mImageViewsCount <= 0 && hasValidBitmap()) {
			if (ImageCache.DEBUG) Logs.d("ImageCache", "bitmap recycled " + mKey);
			if (!mBitmap.isRecycled()) {
				mBitmap.recycle();
			}
		}
	}

}

