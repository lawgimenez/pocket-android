package com.pocket.app.settings;

import android.app.ProgressDialog;
import android.content.Context;

import androidx.annotation.StringRes;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.util.dialog.AlertMessaging;
import com.pocket.sdk.util.file.AndroidStorageUtil;
import com.pocket.sdk.util.service.BackgroundSync;
import com.pocket.util.java.SimpleResultCallback;

public class OptionsDialogs {
	
	// TODO include cleanup in app cache removal refactor, this doesn't need to be complicated anymore

	public static void backgroundSyncingChange(final int syncType, final SimpleResultCallback callback, final Context context) {
		App app = App.from(context);
		BackgroundSync bgsync = app.backgroundSync();
		CxtUi cxt_ui = CxtUi.SETTINGS;
		
		if (syncType == BackgroundSync.SYNC_INSTANT){
			if (app.push().isAvailable()) {
				if (app.http().status().isOnline()) {
					
					final ProgressDialog dialog = AlertMessaging.progress(context, R.string.dg_c2dm_registering, false);
					app.push().register(cxt_ui, (success, message) -> {
						if (!success){
							if (message != null)
								showAlert(context, R.string.dg_c2dm_failed_t, message);
							else
								showAlert(context, R.string.dg_c2dm_failed_t, R.string.dg_c2dm_failed_general_m);
							
							callback.callback(false);
							
						} else {
							bgsync.setBackgroundSyncing(BackgroundSync.SYNC_INSTANT, cxt_ui);
							callback.callback(true);
						}
						AlertMessaging.dismissSafely(dialog, context);
					});
					
				} else {
					showAlert(context, R.string.dg_c2dm_failed_t, R.string.dg_c2dm_not_connected_m);
					callback.callback(false);
				}
				
			} else {
				showAlert(context, R.string.dg_c2dm_unsupported_t, R.string.dg_c2dm_unsupported_m);
				callback.callback(false);
			}
			
		} else {
			if (bgsync.isTimerSync(syncType) && AndroidStorageUtil.isInstalledOnExternalStorage(context)) {
				// Not supported
				showAlert(context, R.string.dg_timer_sync_no_change_t, R.string.dg_timer_sync_no_change_m);

			} else {
				bgsync.setBackgroundSyncing(syncType, cxt_ui);
				callback.callback(true);
			}
		}
	}

	private static void showAlert(Context context, @StringRes int title, @StringRes int message) {
		showAlert(context, title, context.getString(message));
	}

	private static void showAlert(Context context, @StringRes int title, String message) {
		if (AlertMessaging.isContextUnavailable(context)) return;
		AlertMessaging.show(context, context.getText(title), message, context.getText(R.string.ac_ok), null, null, null, true);
	}
		
}
