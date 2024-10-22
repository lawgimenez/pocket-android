package com.pocket.util.java;

import android.content.Context;

import com.ideashower.readitlater.R;
import com.pocket.app.App;

public class BytesUtil {

	private static final long DEFAULT_PER_ITEM_AUTO_KB = 444;
	private static final long DEFAULT_PER_ITEM_ARTICLE_ONLY_KB = 333;
	
	public static final double KB = 1024L; 
	public static final double MB = 1024L * 1024L; 
	public static final double GB = 1024L * 1024L * 1024L; 
	
	/**
	 * Returns the bytes of a kb value. For example, 100.5 kb returns 102,912 bytes.
	 * @param value
	 * @return
	 */
	public static long kbToBytes(float kb) {
		return (long) (kb * KB);
	}

	/**
	 * Returns the bytes of a mb value. For example, 100.5 mb returns 105,381,888 bytes.
	 * @param value
	 * @return
	 */
	public static long mbToBytes(float mb) {
		return (long) (mb * MB);
	}
	
	/**
	 * Returns the bytes of a gb value. For example, 100.5 gb returns 107,911,053,312 bytes.
	 * @param value
	 * @return
	 */
	public static long gbToBytes(float gb) {
		return (long) (gb * GB);
	}
	
	/**
	 * Returns the kb of the bytes. For example, 102,912 bytes return 100.5 kb. 
	 * @param bytes
	 * @return
	 */
	public static double bytesToKb(long bytes) {
		return bytes / KB;
	}
	
	/**
	 * Returns the mb of the bytes. For example, 105,381,888 bytes return 100.5 mb. 
	 * @param bytes
	 * @return
	 */
	public static double bytesToMb(long bytes) {
		return bytes / MB;
	}
	
	/**
	 * Returns the gb of the bytes. For example, 107,911,053,312 bytes return 100.5 gb. 
	 * @param bytes
	 * @return
	 */
	public static double bytesToGb(long bytes) {
		return bytes / GB;
	}
	
	// REVIEW use their actual averages to be more accurate per person ocne they have saved enough items?
	public static long getAverageBytesPerItem() {
		if (App.getApp().prefs().DOWNLOAD_TEXT.get()) {
			return kbToBytes(DEFAULT_PER_ITEM_ARTICLE_ONLY_KB);
			
		} else {
			return kbToBytes(DEFAULT_PER_ITEM_AUTO_KB); // Not expected unless they have downloading completely off but just return value for auto.
		}
	}

	public static String bytesToCleanString(Context context, long bytes) {
		if (BytesUtil.bytesToMb(bytes) < 1000) {
			return String.valueOf((int) BytesUtil.bytesToMb(bytes)) + " "  + context.getString(R.string.setting_cache_mb);
		} else {
			return String.format("%.1f", BytesUtil.bytesToGb(bytes)) + " "  + context.getString(R.string.setting_cache_gb);
		}
	}
	
}