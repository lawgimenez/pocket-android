package com.pocket.sdk.util.file;

import android.content.Context;
import android.os.Environment;

import com.pocket.app.App;
import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;

import java.io.File;

/**
* External device memory, or on older devices potentially removable storage. {@link Context#getExternalFilesDir(String)}
*/
public class ExternalAndroidStorage extends AndroidStorageLocation {

    protected ExternalAndroidStorage(Context context) {
        super(context, Type.EXTERNAL);
    }

    @Override
    public String getPath() throws AssetDirectoryUnavailableException {
        File file = getContext().getExternalFilesDir(null);
        if (file != null) {
            return file.getAbsolutePath();
        } else {
            throw new AssetDirectoryUnavailableException("");
        }
    }

    @Override
    public AndroidStorageUtil.PermissionStatus checkPermissions() {
        return AndroidStorageUtil.checkExternalDirPermission(this);
    }

    @Override
    public State getState() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            if (checkPermissions() == AndroidStorageUtil.PermissionStatus.GRANTED) {
                return State.READY;
            } else {
                return State.MISSING_PERMISSION;
            }
        } else {
            return State.UNAVAILABLE;
        }
    }

    /**
     * @return true if this is a partion of the internal device memory, false if a separate storage device or card.
     */
    public boolean isPartionOfInternal() {
        return Environment.isExternalStorageEmulated();
    }

}
