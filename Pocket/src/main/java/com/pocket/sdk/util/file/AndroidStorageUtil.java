package com.pocket.sdk.util.file;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for getting all possible {@link AndroidStorageLocation}s.
 */
public abstract class AndroidStorageUtil {

    public enum ExternalType {
        INTERNAL_ONLY,
        EXTERNAL_AS_PUBLIC_INTERNAL,
        EXTERNAL_AS_REMOVABLE
    }

    public static InternalAndroidStorage getInternal(Context context) {
        return new InternalAndroidStorage(context);
    }

    public static ExternalAndroidStorage getExternal(Context context) {
        return new ExternalAndroidStorage(context);
    }

    public static List<RemovableAndroidStorage> getRemovable(Context context) {
        List<RemovableAndroidStorage> dirs = new ArrayList<>();
        File[] removeables = ContextCompat.getExternalFilesDirs(context, null);
        int len = removeables.length;
        for (int i = 1; i < len; i++) { // 0 index is always the same as External Storage, so pass over it.
            File dir = removeables[i];
            if (dir != null) {
                if (dir.getAbsolutePath().indexOf(context.getPackageName()) < 0) {
                    // Add a "Pocket" directory in case this storage location didn't make us one specifically, we don't want to write in a general directory.
                    dir = new File(dir, "Pocket");
                }
                dirs.add(new RemovableAndroidStorage(context, dir));
            }
        }
        return dirs;
    }

    public static List<AndroidStorageLocation> getAll(Context context) {
        List<AndroidStorageLocation> dirs = new ArrayList<>();
        dirs.add(getInternal(context));
        dirs.add(getExternal(context));
        dirs.addAll(getRemovable(context));
        return dirs;
    }

    public static RemovableAndroidStorage asRemovable(String path) {
        return new RemovableAndroidStorage(App.getContext(), new File(path));
    }

    /**
     * @return false can also occur if the directory is unavailable, so be sure to check its {@link AndroidStorageLocation#getState} first.
     */
    public static PermissionStatus checkExternalDirPermission(AndroidStorageLocation location) {
        if (ContextCompat.checkSelfPermission(App.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return PermissionStatus.NOT_GRANTED;
        } else if (ContextCompat.checkSelfPermission(App.getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return PermissionStatus.NOT_GRANTED;
        }

        try {
            File root = new File(location.getPath(), "permissioncheck");
            if (root.exists()) {
                if (!root.canWrite() || !root.canRead()) {
                    return PermissionStatus.GRANTED_BUT_FILE_SYSTEM_DENIED;
                }
            } else {
                try {
                    if (root.createNewFile()) {
                        return PermissionStatus.GRANTED_BUT_FILE_SYSTEM_DENIED;
                    }
                } catch (IOException e) {
                    return PermissionStatus.GRANTED_BUT_FILE_SYSTEM_DENIED;
                }
            }
        } catch (AssetDirectoryUnavailableException e) {
            return PermissionStatus.UNAVAILABLE;
        }

        return PermissionStatus.GRANTED;
    }
    
    public enum PermissionStatus {
        /** R/W access is granted */
        GRANTED,
        /** Permission not granted. Only for Android 6.0 with runtime permissions. Not really expecting though since external dirs should technically always have permission by default for our application dirs. */
        NOT_GRANTED,
        /** We have Android permission, but the file system denied access for some reason.... */
        GRANTED_BUT_FILE_SYSTEM_DENIED,
        /** Failed to check because it wasn't unavailable. */
        UNAVAILABLE
    }

    /*
           Determine which options to show and what to call them.
           We only want to show external as an option if it has been used before by the user
           or if the primary external location is truly different than internal's hardware.
           If it is just emulated space, then there is no space gains and it is better
           to use the sandboxed storage, so we don't show external as an option.

           However if we do show external, then we still call it "internal" but add some specific
           modifiers at the end of it like "sandboxed" and "unsandboxed".
            */

    /**
     * Determine what options the user will see in storage.
     *
     * If "external" is just emulated
     * space on the internal hardware, there is no benefit to offering it as an option because
     * it uses the same free space, but doesn't have the benefits of being sandboxed.
     *
     * However, since we offered it as an option in the past, we still show it as an option
     * for those people. In this case we will also call it "internal" but make a distinction
     * between internal and sandboxed and internal and public.
     *
     * If we aren't offering the emulated space as an option, then internal can simply be called
     * "internal" with no further specifics.
     *
     * @return
     */
    public static ExternalType getExternalType(Context context) {
        ExternalAndroidStorage external = AndroidStorageUtil.getExternal(context);
        if (external.isPartionOfInternal()) {
            if (App.getApp().prefs().CACHE_STORAGE_TYPE_EMULATED_VISIBLE.get()) {
                return ExternalType.EXTERNAL_AS_PUBLIC_INTERNAL;
            } else {
                return ExternalType.INTERNAL_ONLY;
            }
        } else {
            return ExternalType.EXTERNAL_AS_REMOVABLE;
        }
    }

    /**
     * The summary name for the selected preference
     * @param type
     * @return
     */
    public static String getStorageLocationSummary(AndroidStorageLocation.Type type, Context context) {
        AndroidStorageUtil.ExternalType externalType = AndroidStorageUtil.getExternalType(context);
        int internalName;
        int externalName;
        switch (externalType) {
            case INTERNAL_ONLY:
            case EXTERNAL_AS_REMOVABLE:
                internalName = R.string.storage_type_internal;
                externalName = R.string.storage_type_removable;
                break;
            case EXTERNAL_AS_PUBLIC_INTERNAL:
                internalName = R.string.storage_type_internal_specific_sandboxed;
                externalName = R.string.storage_type_internal_specific_unsandboxed;
                break;
            default:
                throw new RuntimeException("unknown type " + type);
        }

        if (type == AndroidStorageLocation.Type.INTERNAL) {
            return App.getStringResource(internalName);
        } else if (type == AndroidStorageLocation.Type.EXTERNAL) {
            return App.getStringResource(externalName);
        } else {
            return App.getStringResource(R.string.storage_type_removable);
            // Decided not to include the path. It is better for them to only see the path if they open the picker dialog which shows more info and warnings.
        }
    }
    
    /**
     * Is this app installed on an sd card?
     *
     * @return true if installed on external storage, false if stored on internal memory.
     */
    public static boolean isInstalledOnExternalStorage(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            ApplicationInfo ai = pi.applicationInfo;
            return ((ai.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
