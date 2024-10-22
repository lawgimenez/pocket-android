package com.pocket.util.android.drawable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.os.SystemClock;

import com.pocket.app.App;
import com.pocket.sdk.image.ImageCache;
import com.pocket.util.java.Logs;
import com.pocket.util.java.FileLocks;

import org.apache.commons.io.IOUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class MemoryAwareBitmapFactory {
	
	/**
	 * Same as decode() but catches any unfixable OOM errors or IOExceptions and returns null.
	 * 
	 * @param path
	 * @return
	 */
	public static Bitmap decodeQuietly(String path, Options opts) {
		try {
			return decode(path, opts);
		} catch (OutOfMemoryError oome) {
			// Ok to ignore completely since decode() prints the stacktrace

		} catch (IOException | InterruptedException e) {
			Logs.printStackTrace(e);
		}
		return null;
	}
	
	
	/**
	 * Decodes a bitmap from a path. If a OutOfMemoryError occurs when loading, it will attempt to clear some
	 * memory and try again.
	 * 
	 * If OOM errors occur and all attempts to clear memory fail, this will throw a OutOfMemoryError so you can decide how
	 * to handle.
	 * 
	 * If the bitmap cannot be decoded for any other reason it will just return null.
	 * 
	 * Note: Since this may block the thread during retry attempts, this is not allowed to be used on the UI thread!
	 * 
	 * @param path
	 * @return
	 * @throws OutOfMemoryError If if failed to clear memory enough to decode.
	 * @throws IOException If file not found or if there was a problem decoding.
	 */
	public static Bitmap decode(String path, Options opts) throws OutOfMemoryError, IOException, InterruptedException {
		FileInputStream is = null;
		FileLocks.Lock lock = null;

		try {
			is = new FileInputStream(path);
			lock = App.getApp().imageCache().getImageFileLocks().lock(path);
			return decodeFileDescriptor(is.getFD(), opts, 1);
			
		} finally {
			FileLocks.releaseQuietly(lock);
			IOUtils.closeQuietly(is);
		}
	}
	
	public static Bitmap decodeFileDescriptor(FileDescriptor fd, Options opts) throws OutOfMemoryError {
		return decodeFileDescriptor(fd, opts, 1);
	}
	
	private static Bitmap decodeFileDescriptor(FileDescriptor fd, Options opts, int attempt) throws OutOfMemoryError {
		Bitmap bitmap = null;
		boolean retry = false;
		
		try {
			bitmap = BitmapFactory.decodeFileDescriptor(fd, null, opts);
			
		} catch (OutOfMemoryError oome) {
			retry = handleOOME(oome, attempt);
		}
		
		if (bitmap != null) {
			return bitmap;
			
		} else if (retry) {
			waitBeforeRetry(attempt);
			return decodeFileDescriptor(fd, opts, attempt+1);
			
		} else {
			return null;
		}
	}
	
	public static Bitmap decodeRegion(Rect region, BitmapRegionDecoder decoder, Options opts) throws OutOfMemoryError {
		return decodeRegion(decoder, region, opts, 1);
	}
	
	private static Bitmap decodeRegion(BitmapRegionDecoder decoder, Rect region, Options opts, int attempt) throws OutOfMemoryError {
		Bitmap bitmap = null;
		boolean retry = false;
		
		try {
			bitmap = decoder.decodeRegion(region, opts);
			
		} catch (OutOfMemoryError oome) {
			retry = handleOOME(oome, attempt);
			
		}
		
		if (bitmap != null) {
			return bitmap;
			
		} else if (retry) {
			waitBeforeRetry(attempt);
			return decodeRegion(decoder, region, opts, attempt+1);
			
		} else {
			return null;
		}
	}
	
	public static Bitmap decodeURL(URL url, Options opts) throws OutOfMemoryError, IOException {
		return decodeURL(url, opts, 1);
	}
	
	private static Bitmap decodeURL(URL url, Options opts, int attempt) throws OutOfMemoryError, IOException {
		Bitmap bitmap = null;
		boolean retry = false;
		InputStream instream = null;
		
		try {
			// OPT can we reuse the input stream here?
			instream = (InputStream) url.getContent();
			bitmap = BitmapFactory.decodeStream(instream, null, opts);
			
		} catch (OutOfMemoryError oome) {
			retry = handleOOME(oome, attempt);
			
		} finally {
			IOUtils.closeQuietly(instream);
		}
		
		if (bitmap != null) {
			return bitmap;
			
		} else if (retry) {
			waitBeforeRetry(attempt);
			return decodeURL(url, opts, attempt+1);
			
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param oome
	 * @param attempt
	 * @return whether or not to retry or give up
	 * @throws OutOfMemoryError
	 */
	private static boolean handleOOME(OutOfMemoryError oome, int attempt) throws OutOfMemoryError {
		if (ImageCache.DEBUG) Logs.e("ImageCache", "OOME fix attmpt " + attempt);
		
		switch (attempt) {
		case 1:
			Logs.printStackTrace(oome);
			System.gc();
			return true;
			
		case 2:
			Logs.printStackTrace(oome);
			App.getApp().imageCache().trim();
			System.gc();
			return true;
			
		default:
			// If we are still having memory issues, throw the error so the calling method can decide how to handle.
			throw oome;
				
		}
	}

	/**
	 * Let the GC work
	 */
	private static void waitBeforeRetry(int attempt) {
		SystemClock.sleep(attempt * 1000); 
	}

	
	
	
	
}
