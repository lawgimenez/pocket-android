package com.pocket.sdk.util.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.app.help.Help;
import com.pocket.app.settings.beta.TCActivity;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.ui.view.progress.RainbowProgressCircleView;
import com.pocket.util.java.UserFacingErrorMessage;

/**
 * Helper for blocking access to an Activity while the app is still in "Fetching" mode.
 * Use {@link #blockInteractionUntilFetched(AbsPocketActivity, OnDismissedListener)}
 */
public class FetchingDialog {
	
	/**
	 * Invoke during Activity.onStart to setup. It will automatically dismiss itself onStop or on Fetching complete.
	 */
	public static boolean blockInteractionUntilFetched(AbsPocketActivity activity, OnDismissedListener listener) {
		if (activity == null || activity.isFinishing() || activity.app().appSync().hasFetched()) {
			return false;
		}
		if (!activity.app().pktcache().isLoggedIn()) {
			// If they are signed out, we don't need to block ui since
			// fetch only happens after signing in.
			return false;
		}
		if (activity.app().pktcache().wasSignup()) {
			// If they just signed up, we don't need to block ui since
			// they won't have anything to fetch so it can happen quickly/quietly in the background.
			return false;
		}
		new FetchingDialog(activity, listener);
		return true;
	}
	
	private final AppSync appSync;
	private final Dialog dialog;
	private final AbsPocketActivity activity;
	private final RainbowProgressCircleView progressView;
	private final OnDismissedListener dismissListener;
	private final AbsPocketActivity.OnLifeCycleChangedListener activityListener = new AbsPocketActivity.SimpleOnLifeCycleChangedListener() {
		@Override
		public void onActivityStop(AbsPocketActivity activity) {
			finish();
		}
	};
	private boolean isDismissed;
	
	private FetchingDialog(AbsPocketActivity activity, OnDismissedListener listener) {
		this.appSync = activity.app().appSync();
		this.dialog = new Dialog(activity, R.style.FetchingDialog);
		this.dismissListener = listener;
		this.activity = activity;
		View view = LayoutInflater.from(activity).inflate(R.layout.view_loading_dialog, null, false);
		TextView textView = view.findViewById(R.id.message_loading);
		this.progressView = view.findViewById(R.id.progress_loading);
		textView.setText(R.string.dg_fetching);
		progressView.setProgressIndeterminate(true);
		dialog.setContentView(view);
		dialog.setCancelable(false);
		dialog.getWindow().setDimAmount(0.66f);
		activity.addOnLifeCycleChangedListener(activityListener);
		
		// Block touches, otherwise there is a tiny gap between the display of the dialog where the activity is touchable.
		activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		
		dialog.show();
		fetch();
	}
	
	private void fetch() {
		appSync.sync(
			this::finish,
			error -> {
				if (!activity.isFinishing()) {
					AlertDialog alert = new AlertDialog.Builder(activity)
							.setTitle(R.string.dg_fetch_error_t)
							.setMessage(R.string.dg_fetch_error_m)
							.setNegativeButton(R.string.ac_close_app, (d, w) -> activity.finishAllActivities(false))
							.setNeutralButton(R.string.ac_get_help, (d, w) -> {
								Help.requestHelp(Help.Type.FETCH, new ErrorReport(error, UserFacingErrorMessage.find(error)), activity);
								activity.finishAllActivities(false);
							})
							.setPositiveButton(R.string.ac_retry, (d, w) -> fetch())
							.show();
					
					// Since this is a blocking dialog, there is no way to get into team settings when this is showing,
					// which can be useful for sending logs when there is a problem. So enable it via long press.
					if (activity.app().mode().isForInternalCompanyOnly()) {
						alert.getButton(Dialog.BUTTON_NEUTRAL).setOnLongClickListener(v -> {
							activity.startActivity(new Intent(activity, TCActivity.class));
							return true;
						});
					}
				}
			},
			progress -> {
				if (!activity.isFinishing()) progressView.setProgress(progress);
			});
	}
	
	private void finish() {
		if (isDismissed) {
			return;
		}
		isDismissed = true;
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		if (dismissListener != null) {
			dismissListener.onDismissed();
		}
		// Post to avoid concurrentmod errors during callbacks
		new Handler().post(() -> activity.removeOnLifeCycleChangeListener(activityListener));
	}
	
	public interface OnDismissedListener {
		void onDismissed();
	}
	
}