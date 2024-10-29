package com.pocket.sdk.util.file;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import androidx.core.os.EnvironmentCompat;

/**
 * Removable storage location. {@link Context#getExternalFilesDirs(String}
 */
public class RemovableAndroidStorage extends AndroidStorageLocation {

    private final String mPath;
    private final File mFile;

    /**
     * @param context
     * @param file The absolute path to Pocket's specific directory at this location.
     */
    protected RemovableAndroidStorage(Context context, File file) {
        super(context, Type.REMOVABLE);
        mFile = file;
        mPath = file.getAbsolutePath();
    }

    @Override
    public String getPath() {
        return mPath;
    }

    @Override
    public State getState() {
        if (Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(mFile))) {
            if (checkPermissions() == AndroidStorageUtil.PermissionStatus.GRANTED) {
                return State.READY;
            } else {
                return State.MISSING_PERMISSION;
            }
        } else {
            return State.UNAVAILABLE;
        }
    }

    @Override
    public AndroidStorageUtil.PermissionStatus checkPermissions() {
        return AndroidStorageUtil.checkExternalDirPermission(this);
    }
}
