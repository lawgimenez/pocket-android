package com.pocket.util.android.drawable;

/*******************************************************************************
Inspired by Chris Banes' BitmapLruCache https://github.com/chrisbanes/Android-BitmapMemoryCache

Which has an Apache 2.0 license.

 *******************************************************************************/

import android.graphics.Bitmap;
import android.os.Debug;

import com.pocket.sdk.image.ImageCache;
import com.pocket.util.java.Logs;

import java.util.Map.Entry;

import androidx.collection.LruCache;

public class BitmapLruCache extends LruCache<String, CacheableBitmapWrapper> {

	public BitmapLruCache(int maxSize) {
		super(maxSize);
	}

	@Override
	protected int sizeOf(String key, CacheableBitmapWrapper value) {
		if (value.hasValidBitmap()) {
			Bitmap bitmap = value.getBitmap();
			return bitmap.getRowBytes() * bitmap.getHeight();
		}
		return 0;
	}

	public CacheableBitmapWrapper cache(String key, CacheableBitmapWrapper value) {
		// Notify the wrapper that it's being cached
		value.setCached(true);
		if (ImageCache.DEBUG) Logs.d("ImageCache", "cache entry Added " + key);
		if (ImageCache.DEBUG) Logs.i("ImageCache", "free native heap: " + Debug.getNativeHeapFreeSize());
		return super.put(key, value);
	}

	@Override
	protected void entryRemoved(boolean evicted, String key,
			CacheableBitmapWrapper oldValue, CacheableBitmapWrapper newValue) {
		if (ImageCache.DEBUG) Logs.d("ImageCache", "cache entry Removed " + key);
		if (ImageCache.DEBUG) Logs.i("ImageCache", "free native heap: " + Debug.getNativeHeapFreeSize());
		// Notify the wrapper that it's no longer being cached
		oldValue.setCached(false);
	}

	/**
	 * This method iterates through the cache and removes any Bitmap entries
	 * which are not currently being displayed. A good place to call this would
	 * be from {@link android.app.Application#onLowMemory()
	 * Application.onLowMemory()}.
	 */
	public void trimMemory() {
		if (ImageCache.DEBUG) Logs.d("ImageCache", "cache trimming memory");
		
		for (Entry<String, CacheableBitmapWrapper> entry : snapshot()
				.entrySet()) {
			CacheableBitmapWrapper value = entry.getValue();
			if (null == value || !value.isBeingDisplayed()) {
				remove(entry.getKey());
			}
		}
	}
	
	
}

