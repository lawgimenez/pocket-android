package com.pocket.util.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.pocket.app.App;

public class FormFactor {
	
	public static final int SCREENLAYOUT_SIZE_XLARGE = 0x04;

	private static final int UNKNOWN = 0;
	private static final int PHONE = 1;
	private static final int LARGE_PHONE = 2; // SCREENLAYOUT_SIZE_LARGE like the Galaxy Note
	private static final int SMALL_TABLET = 3; // SCREENLAYOUT_SIZE_LARGE like the Kindle Fire
	private static final int MICRO_TABLET = 4; // smaller than SMALL_TABLET but not quite a phablet. The Kindle Fire HD 7" is like this.
	private static final int TABLET = 5;
	
	public static final int SMALL_TABLET_SMALLEST_WIDTH = 590; // Kindle Fire is 600
	public static final int MICRO_TABLET_SMALLEST_WIDTH = 525; // Kindle Fire HD is 533
	
	public static int mFormFactor;

	private static DisplayMetrics mMetrics;

    @Deprecated
	public static int get() {
		if (mFormFactor == UNKNOWN) {
			// It is very important to use the Application based Context/Resources for this check!
			// Within an Activity's Context/Resources the Configuration.screenLayout will be for its window, not the device itself
			// and if the window is resized (split screen, multi window, etc), then you won't be looking at the device screen size, but the window size.
			mFormFactor = determine(App.getContext());
		}
			
		if (mFormFactor == UNKNOWN) {
			// default to phone properites
			return PHONE;
		}

        return mFormFactor;
	}

	public static int get(Context context) {
		if (mFormFactor == UNKNOWN) {
			// It is very important to use the Application based Context/Resources for this check!
			// Within an Activity's Context/Resources the Configuration.screenLayout will be for its window, not the device itself
			// and if the window is resized (split screen, multi window, etc), then you won't be looking at the device screen size, but the window size.
			mFormFactor = determine(context);
		}

		if (mFormFactor == UNKNOWN) {
			// default to phone properites
			return PHONE;
		}

		return mFormFactor;
	}
	
	
	private static int determine(Context context) {
		int screenLayout = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		
		switch(screenLayout){
		case SCREENLAYOUT_SIZE_XLARGE: // Configuration.SCREENLAYOUT_SIZE_XLARGE was added in 2.3, so use the value to support 2.2 devices
			return TABLET;
			
		case Configuration.SCREENLAYOUT_SIZE_LARGE:
			return calculateSize(context);
			
		case Configuration.SCREENLAYOUT_SIZE_NORMAL:
		case Configuration.SCREENLAYOUT_SIZE_SMALL:
		default:
			return PHONE;
						
		}
	}
	
	/**
	 * Manually determine the size. Helpful if the size is not provided by the system or if the device is SCREENLAYOUT_SIZE_LARGE which
	 * is a weird inbetween state.  Both the Galaxy Note and Kindle Fire fall into this category but the Note should be given the
	 * phone layout and the Fire the tablet.
	 * 
	 * OPT in the future if 3.0 is ever the minimum api, we can start to use the smallestWidth identifier to solve this issue.
	 * 
	 * Relevant: http://developer.android.com/guide/practices/screens_support.html#range
	 * 
	 * @return
	 */
	private static int calculateSize(Context context) {
		float smallestDp = context.getResources().getConfiguration().smallestScreenWidthDp;
		
		if (smallestDp >= SMALL_TABLET_SMALLEST_WIDTH) {
			return SMALL_TABLET;
		
		} else if (smallestDp >= MICRO_TABLET_SMALLEST_WIDTH) {
			return MICRO_TABLET;
			
		} else {
			return LARGE_PHONE;
		}
	}

	public static void init() {
		get();		
	}

	/**
	 * Is this device a Tablet? Includes small and micro tablets.  Opposite of isPhone
	 * <b>Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.</b>
	 * 
	 * @return
	 */
	@Deprecated
	public static boolean isTablet() {
		return isTablet(get());
	}

	public static boolean isTablet(Context context) {
		return isTablet(get(context));
	}
	
	private static boolean isTablet(int formFactor) {
		return formFactor == TABLET || formFactor == SMALL_TABLET || formFactor == MICRO_TABLET;
	}
	
	/**
	 * Is this device a Tablet?  Tablet must be SCREENLAYOUT_SIZE_XLARGE, SCREENLAYOUT_SIZE_LARGE sized devices are returned as false.
	 * 
	 * A device like the Xoom
	 *
	 * <b>Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.</b>
	 * 
	 * @return
	 */
	public static boolean isTabletLarge() {
		return get() == TABLET;
	}
	
	/**
	 * Is this device a small tablet?  Device will be on the larger end of SCREENLAYOUT_SIZE_LARGE Like a Nexus 7.
	 * <b>Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.</b>
	 * 
	 * @return
	 */
	public static boolean isTabletSmall() {
		return get() == SMALL_TABLET || get() == MICRO_TABLET;
	}
	
	
	/**
	 * Is this device a handset sized device? This will also include phablet devices like the Galaxy Note.
	 * <b>Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.</b>
	 * 
	 * @return
	 */
	public static boolean isPhone() {
		return get() == PHONE || get() == LARGE_PHONE;
	}
	
	
	/**
	 * Should a subscreen be shown as a pop up dialog or a full screen activity?
	 * @param context The Activity that is considering how to show a UI, or if no Activity is available, the current context.
	 *                   It will handle null by defaulting to application context, but you should try to pass an activity since this is a ui decision.
	 * @return true if should be opened as a popup
	 */
	public static boolean showSecondaryScreensInDialogs(Context context) {
		int type = determine(findWindowContext(context));
		return type == TABLET || type == SMALL_TABLET;
	}
	
	/**
	 * Tries to find a context that best reflects the current window size.
	 * For best results, pass in an Activity.
	 * @param context
	 * @return
	 */
	private static Context findWindowContext(Context context) {
		Activity activity = ContextUtil.getActivity(context);
		if (activity == null) {
			// Let's try to grab what we know is the currently visible activity.
			activity = App.from(context).activities().getVisible();
		}
		if (activity != null) {
			context = activity;
		}
		if (context == null) {
			// Not really expecting this case, but fallback to the application context.
			context = App.getContext();
		}
		return context;
	}
	
	/**
	 * Converts dp to px, if on phone, will use the phone value, if on tablet it will use the tablet value.
	 */
	public static int dpToPx(float phone, float tablet) {
		if (isTablet()) {
			return dpToPx(tablet);
		} else {
			return dpToPx(phone);
		}
	}
	
	/**
	 * Convert a Density Independant Pixel size to actual pixels based on the density of the device.  Example: 10dp on a xhdpi device will be converted to 20px
	 * @param dp The density indendant size to convert
	 * @return The px value
	 */
	public static int dpToPx(float dp) {
		return (int) dpToPxF(dp);
	}

    public static float dpToPxF(float dp) {
        if (mMetrics == null) {
            mMetrics = App.getContext().getResources().getDisplayMetrics();
        }
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mMetrics);
    }
	
	/**
	 * Convert pixels to a density independant pixel value for the device.  For example: 20px on a xhdpi device will be converted to 10dp.
	 * @param px The pixel value to convert
	 * @return The dp value
	 */
	public static float pxToDp(int px) {
		if (mMetrics == null) {
			mMetrics = App.getContext().getResources().getDisplayMetrics();
		}
		return px / mMetrics.density;
	}

	/**
	 * OPT NO NO NO NO bad developer... don't ever only target one device.  Are you really really sure you want to do this?
	 * @return
	 */
	public static boolean isKindleFire(boolean only7Inch) {
		if (!"Amazon".equalsIgnoreCase(Build.MANUFACTURER))
			return false;
		
		if (!"Kindle Fire".equalsIgnoreCase(Build.PRODUCT) && !"Kindle Fire".equalsIgnoreCase(Build.MODEL))
			return false;
		
		if (!only7Inch)
			return true;
		
		if ("Kindle Fire".equalsIgnoreCase(Build.MODEL) || // First Gen 7"
				"KFOT".equalsIgnoreCase(Build.MODEL) || // Second Gen 7"
				"KFTT".equalsIgnoreCase(Build.MODEL)) // Second Gen HD 7"
				return true;
		
		return false;
	}
	
	/**
	 * Get a human readable description of the kind of device this is.
	 * @return
	 */
	public static String getLabel() {
		int form = get();
		switch (form) {
		case PHONE:
			return "Handset";
		case LARGE_PHONE:
			return "Phablet";
		case SMALL_TABLET:
			return "Small Tablet";
		case MICRO_TABLET:
			return "Micro Tablet";
		case TABLET:
			return "Tablet";
		case UNKNOWN:
		default:
			return "Unknown";
		}
	}

	/**
	 * Gets the CSS class name for this form factor. Either "tablet", "smalltablet", or ("phone" or "null" depending on nullPhone).
	 * 
	 * @param nullPhone true if a phone should return null, false if "phone"
	 * @return
	 */
	public static String getClassKey(boolean nullPhone) {
		if (FormFactor.isTabletLarge()) {
			return "tablet";
			
		} else if (FormFactor.isTabletSmall()) {
			return "smalltablet";
			
		} else {
			return nullPhone ? null : "phone";
		}
	}

	/**
	 * Returns the current width of the window in dp. Note: this allocates
	 * objects, so don't use this in repetitive or performance needed areas
	 * like drawing code. Instead, grab the value and hold it until size
	 * change events occur.
	 *
	 * @param activity measure this Activity's window
	 */
	public static float getWindowWidthDp(Activity activity) {
		return pxToDp(getWindowWidthPx(activity));
	}

	/**
	 * Returns the current width of the window in px. Note: this allocates
	 * objects, so don't use this in repetitive or performance needed areas
	 * like drawing code. Instead, grab the value and hold it until size
	 * change events occur.
	 *
	 * @param activity measure this Activity's window
	 */
	public static int getWindowWidthPx(Activity activity) {
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		return metrics.widthPixels;
	}
}
