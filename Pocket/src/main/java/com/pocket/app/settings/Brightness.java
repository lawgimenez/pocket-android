package com.pocket.app.settings;

import android.app.Activity;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Window;
import android.view.WindowManager;

import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.java.Logs;

public class Brightness {

	// OPT does software dimming this slow down drawing? what about hardware acceleration? is this worth the extra dimness?
	// OPT i disabled software dimming for now as it seemed to be too dark in many cases and possibly? causes extra drawing calls.
	//     if you turn it back on, uncomment it PocketActivityRootView
	
	// OPT if we stick with only hardware, clean up this code to remove the uneeded software dimming code.
	
	private static boolean mUsingCustomBrightness = false;
	private static float mHardware; // 0 to 1 // 0 is dim, 1 is bright
	private static int mSoftware; // SOFTWARE_RANGE to 0 // SOFTWARE_RANGE is dim, 0 is bright, used as overlay alpha
	
	/**
	 * Set the screen brightness
	 * 
	 * @param hardware percent of hardware brightness. range of 0 to 1.0f. 1.0f is full brightness.
	 * 
	 */
	public static void setBrightness(float hardware) {
		mUsingCustomBrightness = true;
		
		// Set Hardware Brightness
		hardware = Math.min(hardware, 1.0f);
		hardware = Math.max(hardware, 0.02f); // QUESTION what is the min value we can use here? // create a fail safe in case it does turn off the screen?
		mHardware = hardware;
	}
	
	public static void applyBrightnessIfSet (AbsPocketActivity activity) {
		if (!mUsingCustomBrightness)
			return;
		
		applyBrightnessToActivity(activity, mHardware, mSoftware);
	}
	
	private static void applyBrightnessToActivity(AbsPocketActivity activity, float hardware, int software) {
		// Hardware
		Window window = activity.getWindow();
		WindowManager.LayoutParams lp = window.getAttributes();
		lp.screenBrightness = mHardware;
		window.setAttributes(lp);
		
		//float nb = window.getAttributes().screenBrightness;
	
		// Software
		activity.setBrightnessOverlay(software);
	}

	/**
	 * @return Returns the hardware brightness last set in {@link #setBrightness(float)}
	 */
	public static float getBrightness() {
		return mHardware;
	}
	
}
