package com.pocket.util.android;

import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;

/**
 * Allows for easy and readable Android OS Api Level checks. Also, since the Build.VERSION_CODES aren't available
 * on all Api Levels, this knows what int values to check against.
 *
 * More: http://developer.android.com/guide/appendix/api-levels.html
 */
public class ApiLevel {

	private static final int NOUGAT = 24;
	private static final int OREO = 26;
	private static final int P = 28;
	private static final int Q = 29;

	/**
	 * @return true if the version of Android running is Nougat (24) (7.0) or newer
	 */
	public static boolean isNougatOrGreater() {
		return Build.VERSION.SDK_INT >= NOUGAT;
	}
	
	
	
	/**
	 * @return true if the version of Android running before Oreo (26) (8.0.0).
	 */
	public static boolean isPreOreo() { return Build.VERSION.SDK_INT < OREO; }

	/**
	 * @return true if the version of Android running is Oreo (26) (8.0.0) or newer
	 */
	public static boolean isOreoOrGreater() {
		return Build.VERSION.SDK_INT >= OREO;
	}


	/**
	 * @return true if the version of Android running before P (28) (9.0.0).
	 */
	public static boolean isPreP() { return Build.VERSION.SDK_INT < P; }

	/**
	 * @return true if the version of Android running is P (28) (9.0.0) or newer
	 */
	public static boolean isPOrGreater() {
		return Build.VERSION.SDK_INT >= P;
	}


	@ChecksSdkIntAtLeast(api = Q)
	public static boolean hasSystemDarkTheme() {
		return Build.VERSION.SDK_INT >= Q;
	}

	/**
	 * Light Navigation bar is technically available since Oreo MR1 (API 27),
	 * but the navigation bar divider is only available to set from themes.xml.
	 * {@link android.view.Window#setNavigationBarDividerColor(int)} is only available since P
	 * and we want to be able to switch (and even animate it) in code.
	 */
	public static boolean isLightNavigationBarAvailable() {
		return isPOrGreater();
	}
}
