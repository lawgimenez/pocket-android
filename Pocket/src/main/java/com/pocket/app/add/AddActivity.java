package com.pocket.app.add;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.core.app.ActivityCompat;

import com.ideashower.readitlater.R;
import com.pocket.analytics.Tracker;
import com.pocket.analytics.UiEntityType;
import com.pocket.analytics.UrlContent;
import com.pocket.analytics.api.UiEntityable;
import com.pocket.app.App;
import com.pocket.app.SaveExtension;
import com.pocket.app.session.Session;
import com.pocket.app.tags.ItemsTaggingActivity;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.DeepLinks;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.util.android.IntentUtils;

/**
 * This Activity receives the {@link Intent#ACTION_SEND} intent when another app selects "Add to Pocket" in a share menu.
 * <p>
 * It can also handle {@link Intent#ACTION_VIEW} for getpocket.com/save links that indicate a url to be saved.
 * <p>
 * It is a transparent Activity that will search the incoming intent for a url and attempt to save it. A {@link Toast} will be
 * displayed to describe the success or failure of the save.
 * <p>
 *     TODO more documentation
 */
public class AddActivity extends AbsPocketActivity implements Session.Segment, Contextual {

	private static final long TIMEOUT_MS = 6500;
	private Runnable timeoutRunnable = this::finish;

	private SaveExtensionAnalytics analytics;
	private @Nullable AddOverlayView overlay;

	@Override
	public boolean isUserPresent() {
		return false;
	}

	@Override public CxtView getActionViewName() {
		return CxtView.SAVE_EXTENSION;
	}

	@Override public ActionContext getActionContext() {
		return new ActionContext.Builder()
				.cxt_view(getActionViewName())
				.cxt_ui(app().saveExtension().isOn() ? CxtUi.INTENT_W_OVERLAY : CxtUi.INTENT)
				.build();
	}

	@Override protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ANY; // toasts an error and finishes if user is not a guest or logged in
	}

	@Override protected void checkClipboardForUrl() {
		// Do not check in this Activity
	}

	@Override protected boolean supportsRotationLock() {
		return false;
	}

	@Override public boolean isListenUiEnabled() {
		return false;
	}

	@Override protected Drawable getActivityBackground() {
		return null;
	}

	@Override protected @StyleRes int themeOverride() {
		return app().saveExtension().isOn() ? R.style.Theme_Transparent_StandaloneDialogActivity2 : R.style.Theme_Transparent2;
	}

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// WARNING: This is an exported activity. Extras could come from outside apps and may not be trust worthy.
		if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && !DeepLinks.Parser.isPocketSaveUrl(getIntent().getDataString())) {
			IntentUtils.openWithDefaultBrowser(this, getIntent(), false);
			finish();
			return;
		}

		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(getWindow().getAttributes());
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.MATCH_PARENT;
		lp.windowAnimations = android.R.style.Animation_Dialog;
		getWindow().setAttributes(lp);
		
		analytics = new SaveExtensionAnalytics(pocket(),
				getActionContext(),
				app().tracker(),
				app().pktcache().hasPremium());

		// cancel on outside touches
		findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
			analytics.clickOutsideDismiss();
			finish();
			return true;
		});

		if (!app().pktcache().isLoggedIn()) {
			showToast(PktSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_logged_out, R.string.ac_login, v -> {
				startDefaultActivity();
				finish();
			});
		} else {
			commitSave(IntentItemUtil.from(getIntent()));
		}
	}

	@Override public void onStop() {
		super.onStop();
		finish(); // if the user navigates away, just finish
	}

	@Override public void finish() {
		super.finish();
		cancelTimeout();
	}

	private void showToast(PktSnackbar.Type type, @StringRes int notificationText, @StringRes int actionText, View.OnClickListener actionListener) {
		if (app().saveExtension().isOn()) {
			// if save extension is enabled, we use our own Pocket toast style
			PktSnackbar.make(this,
					type,
					getResources().getText(notificationText),
					null,
					actionText,
					actionListener)
					.show();
			startTimeout();
		} else {
			// otherwise, show a standard Android toast, so we don't need to take over the whole screen then finish
			Toast.makeText(this, notificationText, Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void commitSave(IntentItem intentItem) {
		if (intentItem.getUrl() != null) {
			setupTracking(intentItem.getUrl());

			SaveExtension saveExtension = app().saveExtension();
			if (saveExtension.isOn()) {
				overlay = new AddOverlayView(this);
				overlay.bind()
						.analytics(analytics)
						.onSavedClick(v -> startPocketActivity())
						.onTagClick(null);
				// show the overlay immediately
				animateIn(overlay);

				analytics.onCommitSave(overlay);

			} else {
				analytics.onCommitSave(getRoot());
			}
			AddItemFromIntentUtil.add(intentItem, app(), Interaction.on(this), this::onSaved);
			
		} else {
			showToast(PktSnackbar.Type.ERROR_EXCLAIM_OUTSIDE, R.string.ts_add_invalid_url, 0, null);
		}
	}
	
	private void setupTracking(String url) {
		Tracker tracker = app().tracker();
		
		tracker.bindContent(getRoot(), new UrlContent(url));
		tracker.bindUiEntityType(getRoot(), UiEntityType.SCREEN);
		
		String id = UiEntityable.identifierFromReferrer(ActivityCompat.getReferrer(this));
		tracker.bindUiEntityIdentifier(getRoot(), id);
	}

	private void onSaved(Item item, AddItemFromIntentUtil.ErrorStatus status) {
		// If there's an error we show a message and don't show any actions.
		if (status == AddItemFromIntentUtil.ErrorStatus.ADD_INVALID_URL) {
			showToast(PktSnackbar.Type.ERROR_EXCLAIM_OUTSIDE, R.string.ts_add_invalid_url, 0, null);
			if (overlay != null) overlay.setVisibility(View.GONE);
			return;
		}

		if (overlay != null) {
			overlay.bind().onTagClick(v -> startTagActivity(item));
			startTimeout();
			if (status == AddItemFromIntentUtil.ErrorStatus.ADD_ALREADY_IN) {
				showToast(PktSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_already_overlay, 0, null);
			}
			
		} else {
			// Extension is disabled.
			if (status == AddItemFromIntentUtil.ErrorStatus.ADD_ALREADY_IN) {
				showToast(PktSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_already_overlay, 0, null);
			} else {
				showToast(PktSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_saved_to_ril, 0, null);
			}
		}
	}

	private void animateIn(View view) {
		setContentView(view);
		ScaleAnimation anim = new ScaleAnimation(
				0.0f, 1.0f,
				0.0f, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setDuration(200);
		view.startAnimation(anim);
	}

	private void startTimeout() {
		app().threads().getHandler().postDelayed(timeoutRunnable, TIMEOUT_MS);
	}

	private void cancelTimeout() {
		if (timeoutRunnable != null) {
			app().threads().getHandler().removeCallbacks(timeoutRunnable);
			timeoutRunnable = null;
		}
		findViewById(android.R.id.content).setOnTouchListener(null);
	}

	private void startPocketActivity() {
		Intent intent = DeepLinks.newPocketIntent(this);
		Parceller.put(intent, AbsPocketActivity.EXTRA_UI_CONTEXT, getActionContext());

		AbsPocketActivity visible = getVisiblePocketMultiWindow();
		if (visible != null && visible != this) {
			// Likely a multi window scenario, launch within the existing Pocket stack
			visible.startActivity(intent);
		} else {
			// Launch as an overlay
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
					Intent.FLAG_ACTIVITY_SINGLE_TOP |
					Intent.FLAG_ACTIVITY_CLEAR_TASK |
					Intent.FLAG_ACTIVITY_NO_ANIMATION |
					Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		finish();
	}

	private void startTagActivity(Item item) {
		final AbsPocketActivity visible = getVisiblePocketMultiWindow();
		if (visible != null && visible != this) {
			// Likely a multi window scenario, launch within the existing Pocket stack
			ItemsTaggingFragment.show(visible, item, getActionContext());
		} else {
			// Launch as an overlay
			Intent intent = ItemsTaggingActivity.newStartIntent(this, true, item, getActionContext());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
					Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
					Intent.FLAG_ACTIVITY_SINGLE_TOP |
					Intent.FLAG_ACTIVITY_CLEAR_TASK |
					Intent.FLAG_ACTIVITY_NO_ANIMATION |
					Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
		finish();
	}

	private AbsPocketActivity getVisiblePocketMultiWindow() {
		return (AbsPocketActivity) App.from(this).activities().getVisible();
	}
}
