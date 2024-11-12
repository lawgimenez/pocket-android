package com.pocket.app.settings.rotation;

import android.app.Activity;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import com.pocket.app.settings.rotation.interf.OSRotationLock;

public class AndroidOSRotationLock implements OSRotationLock {

    private Activity activity;
    private RotationLock rotationLock;
    private ContentObserver oSRotationLockObserver;
    private boolean isOsRotationLocked;

    public AndroidOSRotationLock(Activity activity, RotationLock rotationLock) {
        this.activity = activity;
        this.rotationLock = rotationLock;
        checkForOSRotationLock();
    }

    /**
     * Sets whether the OS level orientation lock is enabled. This is checked on initialization and whenever a content change is detected via mOSRotationLockObserver.
     */
    private void checkForOSRotationLock() {
        isOsRotationLocked = android.provider.Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 0;
    }

    @Override
    public void startObserving() {
        oSRotationLockObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                checkForOSRotationLock();
                if (isOsRotationLocked && rotationLock.isLocked()) {
                    // Unlock our lock so the OS level lock takes over
                    rotationLock.setLocked(false, activity);
                }
            }
        };
        activity.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, oSRotationLockObserver);
    }

    @Override
    public void stopObserving() {
        activity.getContentResolver().unregisterContentObserver(oSRotationLockObserver);
    }

    @Override
    public boolean isLocked() {
        return isOsRotationLocked;
    }

}
