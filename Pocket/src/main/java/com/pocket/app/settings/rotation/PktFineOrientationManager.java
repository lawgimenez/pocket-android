package com.pocket.app.settings.rotation;

import android.app.Activity;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.pocket.app.settings.rotation.interf.FineOrientationManager;

public class PktFineOrientationManager extends OrientationEventListener implements FineOrientationManager {

    private static final int FINE_ORIENTATION_THRESHOLD = 10;

    private static int lastShownForOrientation;

    private Activity activity;
    private OnNewRotationListener onNewRotationListener;

    public PktFineOrientationManager(Activity activity) {
        super(activity);
        this.activity = activity;
    }

    @Override
    public void onOrientationChanged(int orientation) {
        onFineOrientationChange(orientation);
    }

    /**
     * This method is triggered only while LOCKED because we no longer receive onConfigurationChange events for device orientation changes.
     * Instead, we activate an OrientationEventListener to listen for ALL changes in device orientation.
     * <p>
     * By comparing the current fine orientation with the last orientation the lock was shown in (within FINE_ORIENTATION_THRESHOLD), we can
     * determine if an orientation change has occurred.
     * <p>
     * If an orientation change is detected we show the lock button again.
     */
    private void onFineOrientationChange(int rotation) {

        if (rotation == ORIENTATION_UNKNOWN) { // returned if the device is flat and orientation cannot be determined
            return;
        }

        // Determine if the lock has already been shown for this orientation, within the FINE_ORIENTATION_THRESHOLD.
        boolean stillInLastShown = true;
        switch (lastShownForOrientation) {
            case Surface.ROTATION_0:
                stillInLastShown = rotation < 90 - FINE_ORIENTATION_THRESHOLD || rotation > 270 + FINE_ORIENTATION_THRESHOLD;
                break;
            case Surface.ROTATION_90:
                stillInLastShown = rotation < 180 - FINE_ORIENTATION_THRESHOLD && rotation > 0 + FINE_ORIENTATION_THRESHOLD;
                break;
            case Surface.ROTATION_180:
                stillInLastShown = rotation < 270 - FINE_ORIENTATION_THRESHOLD && rotation > 90 + FINE_ORIENTATION_THRESHOLD;
                break;
            case Surface.ROTATION_270:
                stillInLastShown = rotation < 360 - FINE_ORIENTATION_THRESHOLD && rotation > 180 + FINE_ORIENTATION_THRESHOLD;
                break;
        }

        if (!stillInLastShown) {
            lastShownForOrientation = getSurfaceRegion(rotation);

            onNewRotationListener.onNewRotation();
        }
    }

    /**
     * Sets the last Display rotation the lock widget was shown for (ROTATION_0, ROTATION_90, ROTATION_180, or ROTATION_270).
     */
    @Override
    public void markCurrentOrientation() {
        lastShownForOrientation = activity.getWindowManager().getDefaultDisplay().getRotation();
    }

    @Override
    public void setOnNewRotationListener(OnNewRotationListener listener) {
        onNewRotationListener = listener;
    }

    /**
     * Toggle whether the OrientationEventListener is active. This is only active while locked as we need to manually decide when to show the unlock button
     * rather than relying on configuration changes to tell us we've definitely rotated.
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    /**
     * Given a "fine" orientation value from the sensor, determine which Surface rotation value it falls within.
     */
    private static int getSurfaceRegion(int rotation) {
        int region = (Math.round(rotation / 90f)) * 90;
        if (region >= 360) {
            region -= 360;
        }
        switch (region) {
            case 0:
                return Surface.ROTATION_0;
            case 90:
                return Surface.ROTATION_90;
            case 180:
                return Surface.ROTATION_180;
            case 270:
                return Surface.ROTATION_270;
        }
        return Surface.ROTATION_0;
    }

}
