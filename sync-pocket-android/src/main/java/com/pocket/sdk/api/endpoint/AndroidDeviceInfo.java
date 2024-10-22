package com.pocket.sdk.api.endpoint;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

/**
 * {@link DeviceInfo} for an Android device.
 */
public class AndroidDeviceInfo extends DeviceInfo {
	
	public AndroidDeviceInfo(Context context) {
		this(context, null);
	}
	
	/**
	 * @param context The context to extract device info from
	 * @param userAgent See note about X-Device-User-Agent in {@link Endpoint}
	 */
	public AndroidDeviceInfo(Context context, String userAgent) { // REVIEW we could look at connecting this with the UserAgent code as a way for all Android clients to get the user agent setup, but for now, leaving it as optional.
		super("Android",
		Build.VERSION.RELEASE,
		Build.MANUFACTURER,
		Build.MODEL,
		Build.PRODUCT,
		shapeFrom(context),
		localeFrom(context),
		userAgent);
	}
	
	public static String localeFrom(Context context) {
		return localeFrom(context.getResources().getConfiguration().locale);
	}
	
	private static String localeFrom(Locale locale) {
		return locale.getLanguage() + "-" + locale.getCountry();
	}
	
	private static String shapeFrom(Context context) {
		// REVIEW test if we can just change to just the simple if statement within SCREENLAYOUT_SIZE_LARGE, not sure we need the switch statement anymore, just need to verify it will be equal in result
		switch (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
			case Configuration.SCREENLAYOUT_SIZE_XLARGE:
				return "tablet";
			case Configuration.SCREENLAYOUT_SIZE_LARGE:
				if (context.getResources().getConfiguration().smallestScreenWidthDp >= 525) {
					return "tablet";
				} else {
					return "mobile";
				}
			case Configuration.SCREENLAYOUT_SIZE_NORMAL:
			case Configuration.SCREENLAYOUT_SIZE_SMALL:
			default:
				return "mobile";
			
		}
	}

}
