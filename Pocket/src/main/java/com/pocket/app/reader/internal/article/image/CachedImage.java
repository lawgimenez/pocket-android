package com.pocket.app.reader.internal.article.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.pocket.app.App;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.util.java.Logs;

import java.lang.ref.SoftReference;

public class CachedImage {

	private final Asset mAsset;
	private final String mPath;
	private SoftReference<Bitmap> mImage;
	private final Object mLock = new Object();
	
	public CachedImage(String url) {
		mAsset = Asset.createImage(url, App.getApp().assets().getAssetDirectoryQuietly());
		if (mAsset == null) throw new IllegalArgumentException("Asset not found for URL");
		mPath = mAsset.local.getAbsolutePath();
	}
	
	public Bitmap get(){
		Bitmap bitmap = getCachedBitmap();
		if (bitmap != null) return bitmap;
		return getFromDisk();
	}
	
	private Bitmap getCachedBitmap(){
		synchronized (mLock) {
			if(mImage != null && mImage.get() != null){
				if(mImage.get().isRecycled()){
					mImage = null;
					return null;
				} else {
					return mImage.get();
				}
			} else {
				return null;
			}
		}
	}
	
	private Bitmap getFromDisk(){
		try {
			BitmapFactory.Options options = new BitmapFactory.Options(); // OPT reuse?
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			options.inPurgeable = true;
			
			Bitmap bitmap = BitmapFactory.decodeFile(mPath, options);
			
			synchronized (mLock) {
				if(bitmap != null){
					return bitmap;
				} else {
					mImage = null;
					return null;
				}
			}
		} catch (OutOfMemoryError ex) {
			Logs.printStackTrace(ex);
			return null;
		}
	}
	
}
