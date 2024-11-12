package com.pocket.sdk.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.pocket.app.App;
import com.pocket.util.prefs.BooleanPreference;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper for checking/getting runtime permissions.
 * Create an instance during Activity.onCreate.
 * Result of this process will be returned to the {@link Callback#onPermissionResponse(boolean, String[], int[])} callback.
 */
public class PermissionRequester {

    private final AbsPocketActivity mActivity;
    private final String[] mPermissions;
    private final int mRequestCode;
    private final Callback mCallback;

    /**
     * Important: This must be created during a Activity.onCreate() to ensure it
     * receives callbacks if the activity is recreated later. TODO add a check to ensure this is only invoked during onCreate.
     *
     * @param context
     * @param requestCode
     * @param callback
     * @param permissions
     */
    public PermissionRequester(Context context, int requestCode, Callback callback, String... permissions) {
        mActivity = AbsPocketActivity.from(context);
        mPermissions = permissions;
        mRequestCode = requestCode;
        mCallback = callback;

        mActivity.addOnLifeCycleChangedListener(new AbsPocketActivity.SimpleOnLifeCycleChangedListener() {
            @Override
            public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                if (requestCode == mRequestCode) {
                    boolean allGranted = true;
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                            break;
                        }
                    }
                    mCallback.onPermissionResponse(allGranted, permissions, grantResults);
                }
            }
        });
    }

    public void request() {
        ArrayList<String> missing = new ArrayList<>();
        for (String permission : mPermissions) {
            if (ContextCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
            BooleanPreference requestedPref = getPermissionRequestedPref(permission);
            if (requestedPref != null) {
                requestedPref.set(true);
            }
        }
        if (missing.isEmpty()) {
            int[] results = new int[mPermissions.length];
            Arrays.fill(results, PackageManager.PERMISSION_GRANTED);
            mCallback.onPermissionResponse(true, mPermissions, results);

        } else {
            ActivityCompat.requestPermissions(mActivity, mPermissions, mRequestCode);
        }
    }

    public interface Callback {
        public void onPermissionResponse(boolean allGranted, String[] permissions, int[] results);
    }

    /**
     * Gets a BooleanPref that tracks whether or not the current install has ever requested the given permission.  This is necessary in order to know whether we should
     * display a prompt to the user when they have selected "Don't ask again" for the permission.
     *
     * Since shouldShowRequestPermissionRationale will return false both is the user has never requested the permission as well as if they've selected the
     * "Don't ask again" option, it is not enough to just check the current permission state.
     *
     * @param permission the {@link Manifest.permission} String
     * @return the pref that pertains to the given permission.
     */
    private BooleanPreference getPermissionRequestedPref(String permission) {
        switch (permission) {
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return App.from(mActivity).prefs().READ_EXTERNAL_STORAGE_REQUESTED;
        }
        return null;
    }
}