package com.pocket.app.settings.rotation;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.Surface;

import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.prefs.IntPreference;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles the locking and unlocking of rotation for {@link RotationLockComponents}.
 */
@Singleton
public class RotationLock {
    
    private final IntPreference orientation;

    @Inject
    public RotationLock(AppPrefs prefs) {
        orientation = prefs.ORIENTATION;
    }
    

    public boolean isLocked() {
        int requestedOrientation = orientation.get();
        return requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED && requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    }

    public void setLocked(boolean lock, Activity activity) {

        int requestedOrientation;

        if (lock) {
            // if the user locks then we'll need to know their current screen orientation
            requestedOrientation = getFullOrientation(activity.getWindowManager().getDefaultDisplay().getRotation(), activity.getResources().getConfiguration().orientation);
        } else {
            // if we've unlocked, set the requested orientation to be the device sensor

            // NOTE: It has been SCREEN_ORIENTATION_SENSOR since the dawn of time, but in order to get the Android P OS lock feature to appear on screen,
            // it needs to be SCREEN_ORIENTATION_UNSPECIFIED. It is possible that SCREEN_ORIENTATION_UNSPECIFIED works for all OSs but doesn't seem worth
            // finding out when we can just change the behaviour going forward. TODO research this and test
            requestedOrientation = ApiLevel.isPOrGreater() ? ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED : ActivityInfo.SCREEN_ORIENTATION_SENSOR;
        }

        orientation.set(requestedOrientation);

        applyCurrentOrientation(activity);
    }

    /**
     * @param activity applies the current screen orientation to the provided Activity.
     */
    public void applyCurrentOrientation(Activity activity) {
        activity.setRequestedOrientation(orientation.get());
    }

    /**
     * @return the more recently locked screen orientation.
     */
    public int getOrientation() {
        return orientation.get();
    }

    /**
     * A device can be in one of 4 orientations, depending on whether it is in landscape / portrait, and upside down / right side up.
     * This returns the full orientation based on the rotation of the device and the current display configuration.
     *
     * @param rotation    The rotation. See {@link android.view.Display#getRotation}
     * @param orientation Either {@link Configuration#ORIENTATION_LANDSCAPE}, {@link Configuration#ORIENTATION_PORTRAIT}
     * @return {@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE},  {@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE},  {@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}, or  {@link ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT}
     */
    private static int getFullOrientation(int rotation, int orientation) {

        boolean reversed;

        switch (orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                reversed = rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270;
                return !reversed ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;

            case Configuration.ORIENTATION_PORTRAIT:
                reversed = rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_90;
                return !reversed ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

            default:
                return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; // Shouldn't happen but used as default
        }
    }
}
