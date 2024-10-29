package com.pocket.app.settings.rotation;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Handler;

import com.pocket.app.settings.rotation.interf.FineOrientationManager;
import com.pocket.app.settings.rotation.interf.OSRotationLock;
import com.pocket.app.settings.rotation.interf.RotationLockView;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.prefs.BooleanPreference;

import io.reactivex.disposables.Disposable;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

/**
 * Rotation Lock is a feature which lets the user easily lock or unlock Pocket's display orientation in order to prevent accidental screen rotation while in the app.
 * The device can be in one of 4 orientations, depending on whether it is in landscape or portrait, and upside down or right side up.
 * <p>
 * While UNLOCKED, if a rotation is detected via the Activity's onConfigurationChanged callback, a rotation lock button
 * will briefly appear on the screen. Tapping this button will lock Pocket into the current orientation.
 * <p>
 * While LOCKED, a device sensor OrientationEventListener is activated, and checks for orientation changes within the threshold of a rotation change.
 * If that is detected the lock button is again shown in case they want to unlock. Unlocking releases it to auto rotation again.
 * <p>
 * The lock status persists through app sessions via a SharedPreference.
 * <p>
 * A user may also disable this feature altogether in the settings screen. Disabling the feature in settings will release the lock if currently locked.
 * <p>
 * Modern versions of Android have rotation locks as an OS feature. While a user has an OS rotation lock active our rotation lock feature will be disabled and not show, as we cannot disable the OS level lock.
 * If they activate an OS lock while our lock is locked, we release ours and let the OS handle it.
 */
public class RotationLockComponents extends AbsPocketActivity.SimpleOnLifeCycleChangedListener implements AbsPocketActivity.OnConfigurationChangedListener {
    
    private Activity activity;

    private Handler handler;

    private RotationLock rotationLock;

    private BooleanPreference userPref;
    private RotationLockView lockView;
    private OSRotationLock osRotationLock;
    private FineOrientationManager fineOrientationManager;
    private Disposable listener;
    private int currentOrientation;

    public RotationLockComponents(Activity activity, BooleanPreference userPref, OSRotationLock osRotationLock, RotationLockView lockView, FineOrientationManager fineOrientationManager, RotationLock rotationLock) {

        // setup initial state
        this.activity = activity;
        this.handler = new Handler();
        this.rotationLock = rotationLock;
        setupCurrentOrientation();

        this.userPref = userPref;
        this.osRotationLock = osRotationLock;
        this.lockView = lockView;
        this.fineOrientationManager = fineOrientationManager;

        fineOrientationManager.setOnNewRotationListener(() -> {
            if (isLockingAllowed()) {
                lockView.show(rotationLock.isLocked());
            }
        });

        listener = userPref.changes().subscribe(enabled -> {
            // If it is locked and the setting has turned off, we want to unlock the rotation.
            if (!enabled && rotationLock.isLocked()) {
                handler.post(() -> rotationLock.setLocked(false, activity));
            }
        });

        lockView.setOnToggleClick(checked -> {
            // lock the current orientation
            rotationLock.setLocked(checked, this.activity);
            fineOrientationManager.setEnabled(checked);

            fineOrientationManager.markCurrentOrientation();
            lockView.show(rotationLock.isLocked());
        });

        // we don't show the lock on initialization, so mark the current orientation as already shown
        fineOrientationManager.markCurrentOrientation();
    }

    /**
     * Whether or not the rotation lock widget will show on rotation. If it is currently locked this will always return true.
     */
    private boolean isLockingAllowed() {
        // always show if currently locked, so we have a way to get out of a lock
        if (rotationLock.isLocked()) {
            return true;
        }
        // don't show the lock if user settings say no OR we're in an OS lock, since we can't get out of that anyway
        if (!userPref.get() || osRotationLock.isLocked()) {
            return false;
        } else {
            // otherwise, all good to show
            return true;
        }
    }

    private void setupCurrentOrientation() {

        int requestedOrientation = activity.getRequestedOrientation();

        // if our current activity orientation equals that in the RotationLock, do nothing.
        if (requestedOrientation == rotationLock.getOrientation()) {
            return;
        }

        // if the user has locked OR the current Activity orientation is not using the sensor (meaning it changed through some outside config change)
        // then apply our locked orientation to the Activity.
        if (rotationLock.isLocked() || requestedOrientation != SCREEN_ORIENTATION_UNSPECIFIED) {
            rotationLock.applyCurrentOrientation(activity);
        }
    
        currentOrientation = activity.getResources().getConfiguration().orientation;
    }

    @Override
    public void onActivityRestart(AbsPocketActivity activity) {
        setupCurrentOrientation();
        lockView.hide();
        fineOrientationManager.markCurrentOrientation();
    }

    @Override
    public void onActivityResume(AbsPocketActivity activity) {
        fineOrientationManager.setEnabled(rotationLock.isLocked());
        osRotationLock.startObserving();
    }

    @Override
    public void onActivityPause(AbsPocketActivity activity) {
        fineOrientationManager.setEnabled(false);
        osRotationLock.stopObserving();
    }

    @Override
    public void onActivityDestroy(AbsPocketActivity activity) {
        handler.post(() -> {
            activity.removeOnConfigurationChangedListener(this);
            activity.removeOnLifeCycleChangeListener(this);
            if (listener != null) {
                listener.dispose();
                listener = null;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        final int previousOrientation = currentOrientation;
        currentOrientation = configuration.orientation;
        
        handler.post(() -> {
            if (isLockingAllowed() && previousOrientation != currentOrientation) {
                lockView.show(rotationLock.isLocked());
            }
        });
    }

}
