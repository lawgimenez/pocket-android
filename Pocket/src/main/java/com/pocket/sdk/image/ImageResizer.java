package com.pocket.sdk.image;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.net.Uri;

import com.pocket.app.App;
import com.pocket.sdk.image.rule.ImageResizeRule;
import com.pocket.sdk.image.rule.InvalidImageException;
import com.pocket.sdk.image.rule.InvalidResizeRuleException;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.util.java.Logs;
import com.pocket.util.android.drawable.MemoryAwareBitmapFactory;
import com.pocket.util.java.FileLocks;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Takes a source image on disk and creates a copy on disk in a specified size.
 */
public class ImageResizer extends ImageTask {
	
	private final Image.Request mRequest;
	private final Asset mAsset;
	private final ImageResizeRule mSize;
	private final Callback mCallback;
	private boolean mImageResourceExists;
	
	private String mSourcePath;
	private File mSourceFile;
	private String mSizedPath;
	private File mSizedFile;
	
	ImageResizer(Image.Request request, Callback callback) {
		super(request);
		
		mRequest = request;
		mAsset = request.asset;
		mSize = request.resize;
		mCallback = callback;
	}
	
	/**
	 * Check if this image has already been resized (and is still valid).
	 * 
	 * @param request The ImageRequest to check
	 * @return true if the image is already available on disk. false if this size is not yet ready.
	 */
	static boolean isImageResized(Image.Request request) {
		if (request.resize == null) {
			return true; // No resize needed.
		}
		return assetExistsAndPassesAgeTest(request.asset.local, new File(request.assetSizedPath));
	}
	
	/**
	 * Does this asset exist and is still up-to-date? If the source image has been modified since the
	 * resized image was created, then it is not up-to-date. 
	 * 
	 * @param sourceFile The original source image.
	 * @param sizedFile The path to the resized image.
	 * @return True if the image exists and is still up-to-date. False if it doesn't exist or the source has been modified since the resized image was created.
	 */
	private static boolean assetExistsAndPassesAgeTest(File sourceFile, File sizedFile) {
		if (sizedFile.exists()) {
			/*
			 * For thread safety, we can't just rely a simple File.exists() check as that won't
			 * catch the reproducible race condition where the file exists, but is still being
			 * written out, so it isn't fully formed yet. So if there is a lock, wait on it
			 * before we make a decision.
			 */
			if (!waitForLock(sizedFile)) {
				return false;
			}

			// Make sure it's not older than the source image (if the source has since be redownloaded)
			long thumbModified = sizedFile.lastModified();
			long sourceModified = sourceFile.lastModified();
			
			return thumbModified == 0 || sourceModified == 0 || thumbModified >= sourceModified;

		} else {
			return false;
		}
	}

	/**
	 * Obtain a shared lock on a file and immediately release it.
	 * @param file
	 * @return
	 */
	private static final boolean waitForLock(File file) {
		FileLocks.Lock lock = null;

		try {
			lock = App.getApp().imageCache().getImageFileLocks().lock(file);
			lock.release();
			return true;

		} catch (Throwable t) {
			Logs.printStackTrace(t);
			return false; // If something goes wrong here, let's assume the file doesn't exist for the sake of this method.

		} finally {
			FileLocks.releaseQuietly(lock);
		}
	}
	
	@Override
	public void backgroundOperation() {
		mSourcePath = mAsset.local.getAbsolutePath();
		mSourceFile = mAsset.local;
		mSizedPath = mRequest.assetSizedPath;
		mSizedFile = new File(mSizedPath);
		
		// Check local store for resized image
		mImageResourceExists = assetExistsAndPassesAgeTest(mSourceFile, mSizedFile);
		
		// Check to see if we should return
		if (mImageResourceExists){
			mCallback.onImageResizerCompleted(this, true);
			return;
		}
			
		// Check local store for source image
		if (!mSourceFile.exists()) {
			return;
		}
		
		// Alternate path
		boolean isImageInvalid;
		boolean success;
		Bitmap resized = null;
		try {
			resized = getResizedBitmap(mSourcePath, mSize);
			isImageInvalid = false;
			
		} catch (InvalidImageException e) {
			isImageInvalid = true;
			Logs.printStackTrace(e);
			
		} catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				isImageInvalid = false;
			} else {
				isImageInvalid = true; // REVIEW is this a good assumption?
			}
			Logs.printStackTrace(e);
			
		} catch (InvalidResizeRuleException e) {
			isImageInvalid = false;
			Logs.printStackTrace(e);
			
		} catch (OutOfMemoryError oome) {
			isImageInvalid = false;
			// Don't need to print because the decoding methods will have already done that.
		} catch (Throwable t) {
			isImageInvalid = false;
			Logs.printStackTrace(t);
		}
		
		if (resized != null) {	
			success = writeToDisk(mRequest, resized);
		} else {
			success = false;
			if (mSourceFile.length() == 0) {
				isImageInvalid = true;
			}
		}
		
		// Clean up
		if (resized != null && !resized.isRecycled()) {
			resized.recycle();
		}
		
		// Handle results
		if (success) {
			mImageResourceExists = true;
			
		} else if (isImageInvalid) {	
			// The image is not usable for some reason. Need to flag this so we don't constantly have to resize it.
			App.getApp().assets().makeNFFile(mAsset);
			
		} else {
			// Some sort of exception was thrown, could be an OutOfMemoryError. Ignore for now.
		}
		
		// Always return
		mCallback.onImageResizerCompleted(this, success);
	}
	
	/** REVIEW unused method
	 * Load a bitmap to a specific size. Flattens alpha to a white background.
	 * 
	 * @param srcPath The path to the bitmap.
	 * @param rule A {@link ImageResizeRule} describing how to resize the image.
	 * @return
	 * @throws InvalidImageException 
	 * @throws IOException 
	 * @throws InvalidResizeRuleException 
	 */
	public static Bitmap getResizedBitmap(Uri uri, ImageResizeRule rule) throws IOException, InvalidImageException, OutOfMemoryError, InvalidResizeRuleException {
		String srcPath = uri.toString();
		if (srcPath.startsWith("file://")) {
			srcPath = srcPath.substring(7);
		} else {
			// TODO handle these. Could be a content provider. in this case we need to open an inputstream with a content resolver
		}
		return getResizedBitmap(srcPath, rule, Color.WHITE);
	}
	
	/**
	 * Load a bitmap to a specific size. Flattens alpha to a white background.
	 * 
	 * @param srcPath The path to the bitmap.
	 * @param rule A {@link ImageResizeRule} describing how to resize the image.
	 * @return
	 * @throws InvalidImageException 
	 * @throws IOException 
	 * @throws InvalidResizeRuleException 
	 */
	public static Bitmap getResizedBitmap(String srcPath, ImageResizeRule rule) throws IOException, InvalidImageException, OutOfMemoryError, InvalidResizeRuleException {
		return getResizedBitmap(srcPath, rule, Color.WHITE);
	}
	
	/**
	 * Same as {@link #getResizedBitmap(String, ImageResizeRule)} but quietly catches all errors. Returns null if it
	 * could not be decoded or resized for any reason.
	 * 
	 * Note: Remember to check if the image is already on disk to see if you can avoid resizing it.
	 * 
	 * @param srcPath
	 * @param rule
	 * @return
	 */
	public static Bitmap getResizedBitmapQuietly(String srcPath, ImageResizeRule rule) {
		try {
			return getResizedBitmap(srcPath, rule);
		} catch (Throwable t) {
			Logs.printStackTrace(t);
		}
		return null;
	}
	
	/**
	 * Load a bitmap to a specific size.
	 * 
	 * @param srcPath The path to the bitmap.
	 * @param rule A {@link ImageResizeRule} describing how to resize the image.
	 * @param replaceAlphaColor If you want a flattened image (no alpha), supply a color to use as a background. To keep alpha, pass {@link Color.TRANSPARENT}.
	 * @return
	 * @throws IOException 
	 * @throws InvalidImageException 
	 * @throws InvalidResizeRuleException 
	 */
	public static Bitmap getResizedBitmap(String srcPath, ImageResizeRule rule, int replaceAlphaColor) throws IOException, InvalidImageException, OutOfMemoryError, InvalidResizeRuleException {
		Bitmap theImage = null;
		Bitmap inSampleSized = null;
		FileInputStream is = null;
		int sourceWidth = -1;
		int sourceHeight = -1;
		final boolean flattenAlpha = replaceAlphaColor != Color.TRANSPARENT;
		try {
			is = new FileInputStream(srcPath);
			
			final BitmapFactory.Options preOpts = new BitmapFactory.Options();
			preOpts.inJustDecodeBounds = true;
			BitmapFactory.decodeFileDescriptor(is.getFD(), null, preOpts);
			sourceWidth = preOpts.outWidth;
			sourceHeight = preOpts.outHeight;
			
			if (sourceWidth > 0 && sourceHeight > 0) {
				final BitmapFactory.Options readOpts = new BitmapFactory.Options();
				readOpts.inDither = false;
				readOpts.inSampleSize = rule.getInSampleSize(sourceWidth, sourceHeight);
				readOpts.inScaled = false; // This is false because we work in pixel dimensions, not screen dimensions.
				readOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
				makeOptionsMutable(readOpts);
				
				// The inSampleSized Bitmap will be at least as large as we need, but not the exact size yet.
				inSampleSized = MemoryAwareBitmapFactory.decodeFileDescriptor(is.getFD(), readOpts);
				if (readOpts.outWidth <= 0 || readOpts.outHeight <= 0) {
					throw new InvalidImageException(readOpts.outWidth, readOpts.outHeight);
				}
				IOUtils.closeQuietly(is);
				
				if (inSampleSized != null) {
					boolean hasAlpha = inSampleSized.hasAlpha() && !readOpts.outMimeType.endsWith("jpeg");
					if (flattenAlpha && hasAlpha && inSampleSized.isMutable()) {
						Canvas c = new Canvas(inSampleSized);
						c.drawColor(Color.WHITE, PorterDuff.Mode.DST_ATOP);
						hasAlpha = false;
					}
					
					Matrix m = new Matrix();
					float scale = rule.getScale(readOpts.outWidth, readOpts.outHeight);
					
					int sW = rule.getResizedWidth(readOpts.outWidth, readOpts.outHeight, scale);
					int sH = rule.getResizedHeight(readOpts.outWidth, readOpts.outHeight, scale);
					
					// Round up via truncation!
					int sampleW = (int) (0.999f + sW / scale);
					int sampleH = (int) (0.999f + sH / scale);
					
					boolean needsFilter = scale != 1.0f;
					if (needsFilter) {
						m.postScale(scale, scale);
					}
					
					int sampleX = (readOpts.outWidth - sampleW) >> 1;
					int sampleY = (readOpts.outHeight - sampleH) >> 1;
					
					if (sampleX < 0) sampleX = 0;
					if (sampleY < 0) sampleY = 0;
					
					
					// The existing image is large enough to copy from pixel-for-pixel
					try {
						theImage = Bitmap.createBitmap(inSampleSized, sampleX, sampleY, sampleW, sampleH, m, needsFilter);
					} catch (IllegalArgumentException e) {
						throw new InvalidResizeRuleException(e);
					}
					
					// Maybe we cropped off any alpha-containing pieces
					// and the API is smart enough to pass that info to us.
					if (flattenAlpha && hasAlpha && theImage.hasAlpha()) {
						// Nope?
						Bitmap whiteImage = Bitmap.createBitmap(theImage.getWidth(), theImage.getHeight(), theImage.getConfig());
						whiteImage.eraseColor(Color.WHITE);
						Canvas c = new Canvas(whiteImage);
						c.drawBitmap(theImage, new Matrix(), null);
						theImage.recycle();
						theImage = whiteImage;
						whiteImage = null;
					}
					
				} else {
					// The file could not be decoded.
					theImage = null;
				}
				
			} else {
				// The bitmap has an invalid size.
				theImage = null;
			}
			
		} finally {
			IOUtils.closeQuietly(is);
			// Could be the same image. So only recycle if it isn't the one we are returning.
			if (inSampleSized != null && inSampleSized != theImage && !inSampleSized.isRecycled()) {
				inSampleSized.recycle();
			}
		}
		
		return theImage;
	}
	
	public static Bitmap getResizedBitmapQuietly(BitmapRegionDecoder decoder, ImageResizeRule rule, int replaceAlphaColor, Rect region) {
		try {
			return getResizedBitmap(decoder, rule, replaceAlphaColor, region);
		} catch (Throwable t) {
			Logs.printStackTrace(t);
		}
		return null;
	}
	
	/**
	 * Load a bitmap to a specific size.
	 * 
	 * @param srcPath The path to the bitmap.
	 * @param rule A {@link ImageResizeRule} describing how to resize the image.
	 * @param replaceAlphaColor If you want a flattened image (no alpha), supply a color to use as a background. To keep alpha, pass {@link Color.TRANSPARENT}.
	 * @return
	 * @throws InvalidImageException 
	 * @throws InvalidResizeRuleException
	 */
	public static Bitmap getResizedBitmap(BitmapRegionDecoder decoder, ImageResizeRule rule, int replaceAlphaColor, Rect region) throws InvalidImageException, InvalidResizeRuleException { // REVIEW this is nearly an exact copy of the above method, we could share a lot of code.
		Bitmap theImage = null;
		Bitmap inSampleSized = null;
		int sourceWidth = -1;
		int sourceHeight = -1;
		final boolean flattenAlpha = replaceAlphaColor != Color.TRANSPARENT;
		try {
			sourceWidth = region.width();
			sourceHeight = region.height();
			
			if (sourceWidth > 0 && sourceHeight > 0) {
				final BitmapFactory.Options readOpts = new BitmapFactory.Options();
				readOpts.inDither = false;
				readOpts.inSampleSize = rule.getInSampleSize(sourceWidth, sourceHeight);
				readOpts.inScaled = false; // This is false because we work in pixel dimensions, not screen dimensions.
				readOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
				makeOptionsMutable(readOpts);
				
				// The inSampleSized Bitmap will be at least as large as we need, but not the exact size yet.
				inSampleSized = MemoryAwareBitmapFactory.decodeRegion(region, decoder, readOpts);
				
				if (readOpts.outWidth <= 0 || readOpts.outHeight <= 0) {
					throw new InvalidImageException(readOpts.outWidth, readOpts.outHeight);
				}
				
				if (inSampleSized != null) {
					boolean hasAlpha = inSampleSized.hasAlpha() && !readOpts.outMimeType.endsWith("jpeg");
					if (flattenAlpha && hasAlpha && inSampleSized.isMutable()) {
						Canvas c = new Canvas(inSampleSized);
						c.drawColor(Color.WHITE, PorterDuff.Mode.DST_ATOP);
						hasAlpha = false;
					}
					
					Matrix m = new Matrix();
					float scale = rule.getScale(readOpts.outWidth, readOpts.outHeight);
					
					int sW = rule.getResizedWidth(readOpts.outWidth, readOpts.outHeight, scale);
					int sH = rule.getResizedHeight(readOpts.outWidth, readOpts.outHeight, scale);
					
					// Round up via truncation!
					int sampleW = (int) (0.999f + sW / scale);
					int sampleH = (int) (0.999f + sH / scale);
					
					boolean needsFilter = scale != 1.0f;
					if (needsFilter) {
						m.postScale(scale, scale);
					}
					
					int sampleX = (readOpts.outWidth - sampleW) >> 1;
					int sampleY = (readOpts.outHeight - sampleH) >> 1;
					
					if (sampleX < 0) sampleX = 0;
					if (sampleY < 0) sampleY = 0;
					
					
					// The existing image is large enough to copy from pixel-for-pixel
					try {
						theImage = Bitmap.createBitmap(inSampleSized, sampleX, sampleY, sampleW, sampleH, m, needsFilter);
					} catch (IllegalArgumentException e) {
						throw new InvalidResizeRuleException(e);
					}
					
					// Maybe we cropped off any alpha-containing pieces
					// and the API is smart enough to pass that info to us.
					if (flattenAlpha && hasAlpha && theImage.hasAlpha()) {
						// Nope?
						Bitmap whiteImage = Bitmap.createBitmap(theImage.getWidth(), theImage.getHeight(), theImage.getConfig());
						whiteImage.eraseColor(Color.WHITE);
						Canvas c = new Canvas(whiteImage);
						c.drawBitmap(theImage, new Matrix(), null);
						theImage.recycle();
						theImage = whiteImage;
						whiteImage = null;
					}
					
				} else {
					// The file could not be decoded.
					theImage = null;
				}
				
			} else {
				// The bitmap has an invalid size.
				theImage = null;
			}
			
		} finally {
			// Could be the same image. So only recycle if it isn't the one we are returning.
			if (inSampleSized != null && inSampleSized != theImage && !inSampleSized.isRecycled()) {
				inSampleSized.recycle();
			}
		}
		
		return theImage;
	}
	
	/**
	 * Get mutable bitmaps when we can.
	 * 
	 * @param options
	 */
	@TargetApi(11)
	private static void makeOptionsMutable(BitmapFactory.Options options) {
		options.inMutable = true;
	}
	
	public Asset getAsset() {
		return mAsset;
	}

	public boolean imageResourceExists() {
		return mImageResourceExists;
	}
	
	public Image.Request getRequest() {
		return mRequest;
	}

	/**
	 * Write a bitmap to a request's asset sized path. 
	 * 
	 * @param request
	 * @param resizedBitmap
	 * @return true if successfully written, false if failed.
	 */
	public static boolean writeToDisk(Image.Request request, Bitmap resizedBitmap) {
		return writeToDisk(request.assetSizedPath, resizedBitmap, request.assetUser);
	}
	
	public static boolean writeToDisk(String path, Bitmap bitmap, AssetUser user) {
		FileOutputStream os = null;
		FileLocks.Lock lock = null;

		try {

			File file = new File(path);

			FileUtils.forceMkdir(file.getParentFile()); // ensure the path's directory structure exists

			os = new FileOutputStream(path);
			lock = App.getApp().imageCache().getImageFileLocks().lock(path);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os);
			os.flush();
			os.getFD().sync();

			long bytes = FileUtils.sizeOf(file);
			Assets assets = App.getApp().assets();
			assets.registerAssetUser(path, user);
			assets.written(path, bytes);
			return true;
			
		} catch (Throwable t) {
			Logs.printStackTrace(t);
			return false;
			
		} finally {
			FileLocks.releaseQuietly(lock);
			IOUtils.closeQuietly(os);
		}
	}
	
	public interface Callback {
		void onImageResizerCompleted(ImageResizer resizer, boolean success);
	}

}
