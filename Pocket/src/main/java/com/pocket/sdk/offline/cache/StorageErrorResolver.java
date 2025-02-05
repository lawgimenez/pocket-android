package com.pocket.sdk.offline.cache;

import android.app.AlertDialog;
import android.widget.Toast;

import com.ideashower.readitlater.R;
import com.pocket.app.AppThreads;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.dialog.ProgressDialogFragment;
import com.pocket.sdk.util.file.AndroidStorageLocation;
import com.pocket.sdk.util.file.AndroidStorageUtil;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.java.BytesUtil;
import com.squareup.phrase.Phrase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * When an error occurs while accessing Pocket's file system, use
 * {@link #resolve(Assets, Callback, AbsPocketActivity)} to attempt to find and resolve the 
 * problem. It will look at potential issues such as denied permissions or removable storage 
 * states, and then interact with the user in order to fix them.
 * <p>
 * Multiple calls to {@link #resolve(Assets, Callback, AbsPocketActivity)} will avoid duplicating
 * messaging.
 */
public class StorageErrorResolver {

    private static final long LOW_SPACE_THRESHOLD = BytesUtil.mbToBytes(2);
    
    enum Problem {
        STORAGE_UNAVAILABLE,
        STORAGE_PERMISSIONS,
        LOW_SPACE,
        CACHE_MISSING
    }
    
    private final AppThreads threads;
    private final Set<Callback> pendingCallbacks = new HashSet<>();
    private TaskRunnable pendingCheck;
    
    public StorageErrorResolver(AppThreads threads) {
        this.threads = threads;
    }
    
    /**
     * Look for issues and if found and the app is in the foreground, display a dialog with how to resolve them.
     * @param callback
     * @param activity Where to display the dialog. If null, no dialog will be displayed.
     */
    public synchronized void resolve(Assets assets, Callback callback, AbsPocketActivity activity) {
        if (callback != null) pendingCallbacks.add(callback);

        if (pendingCheck == null) pendingCheck = threads.async(() -> {
            Problem problem;
            try {
                AssetDirectory assetsManager = assets.getAssetDirectory();
                AndroidStorageLocation location = assetsManager.getStorageLocation();
                switch (location.getState()) {
                    case READY:
                        if (location.getFreeSpaceBytes() <= LOW_SPACE_THRESHOLD) {
                            problem = Problem.LOW_SPACE;
                        } else if (assetsManager.isOfflineCacheMissing()) {
                            problem = Problem.CACHE_MISSING;
                        } else {
                            problem = null;
                        }
                        break;
                    case UNAVAILABLE:
                        problem = Problem.STORAGE_UNAVAILABLE;
                        break;
                    case MISSING_PERMISSION:
                        problem = Problem.STORAGE_PERMISSIONS;
                        break;
                    default:
                        problem = null;
                        break;
                }
            } catch (Throwable t) {
                problem = Problem.STORAGE_UNAVAILABLE;
            }
            
            Problem result = problem; // Need to make final for the lambda
            threads.runOrPostOnUiThread(() -> {
                if (result == null) {
                    callback(false, false);
                } else {
                    if (activity == null || !activity.app().pktcache().isLoggedIn() || activity.app().user().isStoppingData()) {
                        callback(true, false);
                    } else {
                        switch (result) {
                            case LOW_SPACE:
                                showFreeSpaceDialog(activity);
                                break;
                            case CACHE_MISSING:
                                showCacheMissingDialog(activity);
                                break;
                            case STORAGE_UNAVAILABLE:
                                showStorageUnavailableDialog(activity);
                                break;
                            case STORAGE_PERMISSIONS:
                                showMissingPermissionDialog(activity);
                                break;
                            default:
                                callback(true, false);
                                break;
                        }
                    }
                }
            });
        });
    }

    private void showMissingPermissionDialog(AbsPocketActivity context) {
        showRetryOrResetCacheDialog(context, R.string.dg_offline_cache_is_missing_permission_t, R.string.dg_offline_cache_is_missing_permission_m);
    }

    private void showCacheMissingDialog(AbsPocketActivity context) {
        showRetryOrResetCacheDialog(context, R.string.bg_offline_cache_is_missing_t, R.string.bg_offline_cache_is_missing_m);
    }

    private void showStorageUnavailableDialog(AbsPocketActivity context) {
        showRetryOrResetCacheDialog(context, R.string.bg_offline_cache_storage_is_unavailable_t, R.string.bg_offline_cache_storage_is_unavailable_m);
    }

    private void showRetryOrResetCacheDialog(final AbsPocketActivity context, final int title, final int message) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(dialog -> callback(true, false))
                .setNeutralButton(R.string.ac_retry, (dialog, which) -> callback(true, true))
                .setPositiveButton(R.string.ac_create_new_cache, (dialog, which) -> new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle(R.string.dg_confirm_t)
                        .setMessage(R.string.dg_confirm_create_new_cache_m)
                        .setOnCancelListener(d -> callback(true, false))
                        .setNeutralButton(R.string.ac_cancel, (d, w) -> showRetryOrResetCacheDialog(context, title, message)) // Reshow original dialog
                        .setPositiveButton(R.string.ac_yes, (d, w) -> {
                            // Change to internal storage
                            try {
                                context.app().assets().setStorageLocation(AndroidStorageUtil.getInternal(context));
                                final ProgressDialogFragment progress = ProgressDialogFragment.getNew(R.string.dg_changing_data_location, false);
                                progress.showOnCurrentActivity();
                                context.app().assets().clearOfflineContent(() -> {
                                    Toast.makeText(context, R.string.storage_location_changed, Toast.LENGTH_LONG).show();
                                    callback(true, true);
                                }, null);
                            } catch (AssetDirectoryUnavailableException e) {
                                // Unavailable again, reshow error.
                                showRetryOrResetCacheDialog(context, title, message);
                            }
                        })
                        .show())
                .show();
    }

    private void showFreeSpaceDialog(final AbsPocketActivity context) {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.dg_out_of_space_t)
                .setMessage(R.string.dg_out_of_space_m)
                .setOnCancelListener(dialog -> callback(true, false))
                .setNeutralButton(R.string.ac_close_app, (dialog, which) -> {
                    callback(true, false);
                    context.finishAllActivities(false);
                })
                .setNegativeButton(R.string.ac_clear_cache, (dialog, which) -> {
                    final ProgressDialogFragment progress = ProgressDialogFragment.getNew(R.string.dg_changing_data_location, false);
                    progress.showOnCurrentActivity();
                    context.app().assets().clearOfflineContent(null, () -> {
                        progress.dismissAllowingStateLoss();
                        new AlertDialog.Builder(context)
                            .setCancelable(false)
                            .setTitle(R.string.dg_clearing_cache)
                            .setMessage(
                                    Phrase.from(context, R.string.dg_after_clear_m)
                                            .put("name_of_storage_setting", context.getString(R.string.setting_cache_set_offline_storage_limits))
                                            .format()
                            )
                            .setNeutralButton(R.string.ac_ok, (d, w) -> callback(true, true))
                            .show();
                    });

                })
                .show();
    }

    private void callback(boolean issueFound, boolean retry) {
        ArrayList<Callback> callbacks;
        synchronized (this) {
            callbacks = new ArrayList<>(pendingCallbacks);
            pendingCallbacks.clear();
            pendingCheck = null;
        }
        for (Callback callback : callbacks) {
            callback.onStorageCheckComplete(issueFound, retry);
        }
    }

    public interface Callback {
        /**
         * Storage check has completed. The issue may or may not be resolved.
         * There are several reasons why the issue may persist:
         * <ul>
         *     <li>The app is not in the foreground and the user could not be prompted to fix.</li>
         *     <li>The user has not resolved the issue properly</li>
         *     <li>There is some other issue not found by the checker</li>
         * </ul>
         * At this time, if the {@code retry} value is true, then you should retry
         * whatever triggered the error and if it occurs again, invoke {@link #resolve(Assets, Callback, AbsPocketActivity)}
         * again. If it is false, then no attempt to retry should be made for risk of
         * getting into an infinite loop.
         *
         * @param issueFound true if an issue was found, false if no problems with storage or cache were found. Also false if no fix was attempted because the user is not present.
         * @param retry See above for details.
         */
        void onStorageCheckComplete(boolean issueFound, boolean retry);
    }


}
