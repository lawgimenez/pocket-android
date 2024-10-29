package com.pocket.sdk.util.file;

import android.content.Context;

/**
 * Internal device memory. {@link Context#getFilesDir()}
 */
public class InternalAndroidStorage extends AndroidStorageLocation {

    protected InternalAndroidStorage(Context context) {
        super(context, Type.INTERNAL);
    }

    @Override
    public String getPath() {
        return getContext().getFilesDir().getAbsolutePath();
    }

    @Override
    public State getState() {
        return State.READY; // Internal storage should always be available.
    }

    @Override
    public AndroidStorageUtil.PermissionStatus checkPermissions() {
        return AndroidStorageUtil.PermissionStatus.GRANTED;
    }
}
