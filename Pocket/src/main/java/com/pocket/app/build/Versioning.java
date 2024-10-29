package com.pocket.app.build;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import com.pocket.app.App;
import com.pocket.app.Jobs;
import com.pocket.app.VersionUtil;
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.offline.cache.AssetDirectory;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.util.java.Logs;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Helper for working with app updates and migrations that might need to occur on update.
 */
@Singleton
public class Versioning {
    
    private final int to;
    private final int from;
    private final boolean isFirstRun;
    private final boolean isUpgrade;
    /** This is the first run since an OS upgrade occurred. */
    private final boolean osUpgraded;
    private final Jobs jobs;
    private final Set<Runnable> upgradePrepTasks = new HashSet<>();
    private final Context context;
    private int resetAttempt = 0;

    @Inject
    public Versioning(@ApplicationContext Context context, AppVersion build, AppPrefs prefs, Jobs jobs) {
        this.context = context;
        this.jobs = jobs;
        to = build.getVersionCode(context);
        from = prefs.PREVIOUS_APP_VERSION.get();
        isFirstRun = from == 0;
        isUpgrade = !isFirstRun && to > from;
        prefs.PREVIOUS_APP_VERSION.set(to);
    
        if (isFirstRun) {
            prefs.ORIGINAL_BUILD_VERSION.set(build.getConfigName()); // Maybe not the best place for this...?
        }
    
        String buildKey = Build.VERSION.SDK_INT + Build.VERSION.INCREMENTAL + Build.VERSION.RELEASE;
        if (!buildKey.equals(prefs.OS_BUILD_KEY.get())) {
            osUpgraded = !isFirstRun;
            prefs.OS_BUILD_KEY.set(buildKey);
        } else {
            osUpgraded = false;
        }
        
        jobs.registerCreator(UpgradePrep.class, UpgradePrep::new);
    }
    
    public boolean isFirstRun() {
        return isFirstRun;
    }
    
    public boolean isUpgrade() {
        return isUpgrade;
    }
    
    public int from() {
        return from;
    }
    
    public int to() {
        return to;
    }
    
    /**
     * Is this app upgrading from a version that is before the provided version?
     * Upgrading here means {@link #from()} is less that the provided version.
     */
    public boolean upgraded(int major, int minor, int point, int build) {
        return isUpgrade && from < VersionUtil.toVersionCode(major,minor,point,build);
    }
    
    /**
     * Add work to run in a background process sometime after the app has upgraded.
     * There is no guarantee that this will run or run within a certain time frame.
     * This is not intended for your component's upgrade path, instead use {@link #upgraded(int, int, int, int)} to check
     * during your component's init to see if it needs to do any upgrades.
     * <p>
     * This is only intended as an opportunity to start long running upgrade processes while the app is in the background,
     * before the user opens the app. (See {@link #onAppUpdateReceiver()} for what triggers this).
     * <p>
     * Avoid network activity that would not honor the user's settings/preferences.
     * <p>
     * <b>Note</b>: This must be invoked during app init.
     * Calls after that may end up being ignored and have no effect.
     *
     * @param task The task to run, any exceptions thrown will be ignored.
     */
    public void addUpgradePrepTask(Runnable task) {
        upgradePrepTasks.add(task);
    }
    
    /** Invoke from {@link com.pocket.app.updated.UpdatedReceiver} which is a receiver for {@link android.content.Intent#ACTION_MY_PACKAGE_REPLACED}. */
    public void onAppUpdateReceiver() {
        jobs.scheduleImmediate(UpgradePrep.class);
    }
    
    /**
     * Delete all app data and reset it back to its original, logged out state.
     * All user data and settings will be deleted.
     * This will either kill the process or throw an exception
     * @param assetDirectory The current asset directory
     */
    @SuppressLint("ApplySharedPref")
    public void resetApp(AssetDirectory assetDirectory) {
        try {
            // The asset directory might be on external storage, so delete it separately in case it isn't handled below
            FileUtils.deleteQuietly(new File(assetDirectory.getOfflinePath()));
    
            // Delete app data. This is also suppose to kill the app process
            boolean reset = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
    
            // It is suppose to have killed the process by now, unless it failed.
            throw new RuntimeException("clear failed " + reset);
            
        } catch (Throwable e) {
            // If something goes wrong, we risk ending up in a half cleared state where maybe the prefs that control resetting are cleared, but we haven't cleared all the data.
            // Retry at least 3 times, pausing for some time between.
            if (resetAttempt++ < 3) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {}
                resetApp(assetDirectory);
            }
            // If we get here, then we really weren't able to clear the app.
            // As a fail last ditch attempt to avoid data leakage, try to manually clear the app data
            try {
                FileUtils.cleanDirectory(context.getFilesDir().getParentFile());
            } catch (Throwable ignore) {}
            // We can't be sure what state the app will be in at this point, it may work for them or not on next open, it may have some data still there
            // Crash, which will hopefully report this to us.
            // If we determine this is possible, then we can investigate a further fail safe here.
            throw new RuntimeException("app reset failed", e);
        }
    }
    
    public static class UpgradePrep extends Worker {
    
        UpgradePrep(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }
        
        @NonNull
        @Override
        public Result doWork() {
            HashSet<Runnable> tasks = new HashSet<>(App.from(getApplicationContext()).versioning().upgradePrepTasks);
            for (Runnable task : tasks) {
                try {
                    task.run();
                } catch (Throwable quietlyIgnored) {
                    Logs.printStackTrace(quietlyIgnored);
                }
            }
            return Result.success();
        }
        
    }
    
}
