package com.pocket.sdk.util.file;

import android.content.Context;
import android.os.StatFs;

import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;
import com.pocket.sdk.offline.cache.Assets;

import java.io.IOException;

/**
 * Describes a directory in the Android device's file system that is specifically for Pocket.
 *
 * @see AndroidStorageUtil For obtaining instances.
 */
public abstract class AndroidStorageLocation {

    public enum State {
        READY,
        /** The location is missing or we don't have access to it. Be aware that if permissions are revoked, some locations may appear to be unmounted and will return this state. */
        UNAVAILABLE,
        MISSING_PERMISSION
    }
    
    /**
     * <b>NOTE</b> These names are USER FACING via {@link Assets#getHelpInfo()}
     */
    public enum Type {
        /**
         * The device's internal, sandboxed storage. Always available.
         */
        INTERNAL,
        /**
         * The device's primary external storage, it could be emulated space on the internal drive.
         */
        EXTERNAL,
        /**
         * Anything else outside of the primary storage choices.
         */
        REMOVABLE
    }

    private final Context mContext;
    private final Type mType;

    protected AndroidStorageLocation(Context context, Type type) {
        mContext = context;
        mType = type;
    }

    /**
     * @return The absolute path to the root of Pocket's directory on this storage device.
     * @throws IOException May occur if the directory is unavailable or unmounted. For checking availability use {@link #getState()}.
     */
    public abstract String getPath() throws AssetDirectoryUnavailableException;

    /**
     * @return The current state of this location.
     */
    public abstract State getState();

    /**
     * @return The type of permissions if we have permission to read/write, false otherwise. If the directory is unavailable, this could also be false, query {@link #getState()} to check for that case.
     */
    public abstract AndroidStorageUtil.PermissionStatus checkPermissions();

    /**
     * Same as {@link #getPath()} but removes the Pocket specific part. This is helpful to
     * displaying to the user what device this location represents.
     * @param context
     * @return
     * @throws IOException May occur if the directory is unavailable or unmounted. For checking availability use {@link #getState()}.
     */
    public String getPathOfParentOfAppDirectory(Context context) throws AssetDirectoryUnavailableException {
        String path = getPath();
        int index = path.indexOf(context.getPackageName());
        if (index > 0) {
            return path.substring(0, index-1);
        } else {
            return path;
        }
    }

    /**
     * @return Convienance method for checking if it is the {@link State#READY} state.
     */
    public boolean isAvailable() {
        return getState() == State.READY;
    }

    protected Context getContext() {
        return mContext;
    }

    /**
     * @return The free space in this location in bytes.
     * @throws Exception If the location is unavailable or doesn't have permission granted.
     */
    public long getFreeSpaceBytes() throws Exception {
        StatFs internal = new StatFs(getPath());
        return internal.getAvailableBlocksLong() * internal.getBlockSizeLong();
    }

    public Type getType() {
        return mType;
    }

    public boolean isMediaScannable() {
        return mType == Type.EXTERNAL
            || mType == Type.REMOVABLE;
    }

    /**
     * Checks equality, including the path.
     * Not using the normal equals() because getPath() can throw
     * an exception and this explicit method makes it clearer that
     * there is an exception to handle. Having equals() throw
     * an exception would be unexpected.
     *
     * @param o
     * @return
     * @throws IOException
     */
    public boolean equalsIncludingPath(Object o) throws AssetDirectoryUnavailableException {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AndroidStorageLocation location = (AndroidStorageLocation) o;

        if (mType != location.mType) return false;
        if (!getPath().equals(location.getPath())) return false;

        return true;
    }

    /**
     * @see #equalsIncludingPath(Object)
     */
    @Deprecated
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return mType.hashCode();
    }

    @Override
    public String toString() {
        String path;
        try {
            path = getPath();
        } catch (AssetDirectoryUnavailableException t) {
            path = "unknown";
        }
        return mType + path;
    }
}
