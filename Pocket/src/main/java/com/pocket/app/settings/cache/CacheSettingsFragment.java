package com.pocket.app.settings.cache;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ideashower.readitlater.R;
import com.pocket.analytics.EngagementType;
import com.pocket.analytics.Tracker;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.CacheLimitPreferenceView;
import com.pocket.app.settings.view.preferences.CacheLimitPreferenceView.OnCacheLimitChangedListener;
import com.pocket.app.settings.view.preferences.Preference;
import com.pocket.app.settings.view.preferences.PreferenceViews;
import com.pocket.sdk.analytics.events.CacheSettingsEvents;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.LongPreference;

import java.util.ArrayList;


public class CacheSettingsFragment extends AbsPrefsFragment {
	
	public static FragmentLaunchMode getLaunchMode(Activity activity) {
		if (FormFactor.showSecondaryScreensInDialogs(activity)) {
			return FragmentLaunchMode.DIALOG;
		} else {
			return FragmentLaunchMode.ACTIVITY;
		}
	}
	
	public static CacheSettingsFragment newInstance() {
		CacheSettingsFragment frag = new CacheSettingsFragment();
		
		Bundle args = new Bundle();
		frag.setArguments(args);

		return frag;
	}
	
	public static void show(FragmentActivity activity) {
		if (getLaunchMode(activity) == FragmentLaunchMode.DIALOG) {
			FragmentUtil.addFragmentAsDialog(newInstance(), activity);
		} else {
			CacheSettingsActivity.startActivity(activity);
		}
	}

	private boolean mIsSaved;
	private View mSaveButton;
	private LongPreference sizeTemp;
	private IntPreference sortTemp;
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.CACHE_SETTINGS;
	}
	
	@Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
		return UiEntityIdentifier.CACHE_SETTINGS;
	}
	
	@Override
	protected int getTitle() {
		return 0;
	}

	@Override
	protected View getBannerView() {
		return null;
	}
	
	@Override
	public void onViewCreatedImpl(@NonNull View view, Bundle savedInstanceState) {
		sortTemp = app().prefs().CACHE_SORT_TEMP;
		sizeTemp = app().prefs().CACHE_SIZE_USER_LIMIT_TEMP;
		
		/*
		 * In order to do the Save Changes, we display Preferences with fake/temp PrefKeys instead of the actual ones.
		 * Set the temp values to the current real ones.
		 */
		sizeTemp.set(app().assets().getCacheLimit());
		sortTemp.set(app().assets().getCacheLimitPriority());

		super.onViewCreatedImpl(view, savedInstanceState);

		appbar.bind().withCloseIcon()
				.addButtonAction(com.pocket.ui.R.string.ac_save, v -> {
			if (!confirmSaveChanges(true)) {
				finishWithoutPrompt();
			}
		});
		mSaveButton = appbar.getActionView(0);

		updateSaveButton();
		
		CacheSettingsEvents.VIEW.send();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// Trigger cleaning or migration if needed.
		// The user may be coming to this screen to check the status of the settings they set.
		app().assets().clean();
	}
	
	@Override
	protected void createPrefs(ArrayList<Preference> prefs) {
		prefs.add(PreferenceViews.newHeader(this, R.string.setting_cache_set_offline_storage_limits, false));
		
		prefs.add(new CacheLimitPreference());
	
		prefs.add(PreferenceViews.newMultipleChoiceBuilder(this, sortTemp, R.string.setting_cache_priority)
				.addChoice(R.string.setting_cache_priority_newest)
				.addChoice(R.string.setting_cache_priority_oldest)
				.setOnItemSelectedListener(n -> {
					prefAdapter.notifyDataSetChanged(); // Trigger the CacheLimitPreference to update its UI if needed
					updateSaveButton();
				})
				.setIdentifier(UiEntityIdentifier.SETTING_CACHE_PRIORITY)
				.build());
	}
	
	private void updateSaveButton() {
		mSaveButton.setEnabled(hasChanges());
	}
	
	private class CacheLimitPreference extends Preference {

		private final Tracker tracker = app().tracker();
		
		public CacheLimitPreference() {
			super(CacheSettingsFragment.this);
		}

		@Override
		public boolean update() {
			return false;
		}

		@Override
		public void onClick(View v) {}

        @Override
        public boolean onLongClick(View v) {
            return false;
        }

        @Override
		public boolean isEnabled() {
			return true;
		}
		
		@Override
		public boolean isClickable() {
			return false;
		}
		
		@Override
		public PrefViewType getType() {
			return PrefViewType.CACHE_LIMIT;
		}
		
		@Override
		public void applyToView(View layout) {
			CacheLimitPreferenceView view = (CacheLimitPreferenceView) layout;
			
			view.setLimit(sizeTemp.get());
			view.setOnCacheLimitChangedListener(new OnCacheLimitChangedListener() {
				
				@Override
				public void onCacheLimitChanged(long bytes) {
					sizeTemp.set(bytes);
					updateSaveButton();
					tracker.bindUiEntityValue(view, Long.toString(bytes));
					tracker.trackEngagement(view, EngagementType.GENERAL, null, null, null);
				}
				
			});
			
			String itemOrder;
			if (sortTemp.get() == Assets.CachePriority.OLDEST_FIRST) {
				itemOrder = getStringSafely(R.string.setting_cache_priority_inline_oldest);
			} else {
				itemOrder = getStringSafely(R.string.setting_cache_priority_inline_newest);
			}
			view.setItemOrder(itemOrder);

			view.setUiEntityIdentifier(UiEntityIdentifier.SETTING_CACHE_SIZE.value);
		}
		
	}
	
	@Override
	public void finish() {
		if (confirmSaveChanges(false)) {
			// Cancel finish
		} else {
			super.finish();
		}
	}
	
	@Override
	public boolean onBackPressed() {
		if (confirmSaveChanges(false)) {
			return true;
		} else {
			return super.onBackPressed();
		}
	}
	
	private boolean hasChanges() {
		return sortTemp.get() != app().assets().getCacheLimitPriority()
			|| sizeTemp.get() != app().assets().getCacheLimit();
	}

	private boolean confirmSaveChanges(boolean isSave) {
		if (mIsSaved) {
			return false;
		}
		
		if (hasChanges()) {
			if (isSave) {
				confirmSave();
			} else {
				confirmDiscard();
			}
			return true;
			
		} else {
			return false;
		}
	}
		
	private void confirmSave() {
		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.setting_cache_confirm_change_t)
			.setMessage(getString(R.string.setting_cache_confirm_change_m) + " " + getString(R.string.setting_cache_may_take_a_few_minutes))
			.setPositiveButton(R.string.ac_yes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					save();
				}
				
			})
			.setNegativeButton(R.string.ac_cancel, null)
			.show();
	}
	
	private void confirmDiscard() {
		new AlertDialog.Builder(getActivity())
			.setTitle(R.string.dg_changes_not_saved_t)
			.setMessage(R.string.setting_cache_not_saved_m)
			.setPositiveButton(R.string.ac_discard_changes, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finishWithoutPrompt();
				}
			})
			.setNegativeButton(R.string.ac_continue_editing, null)
			.show();
	}
	
	private void save() {
		if (sortTemp.get() != app().assets().getCacheLimitPriority()) {
			CacheSettingsEvents.sendPriorityChange(sortTemp.get());
		}
		if (sizeTemp.get() != app().assets().getCacheLimit()) {
			CacheSettingsEvents.sendLimitChange(sizeTemp.get());
		}
		
		// Update the real values from the temp ones
		app().assets().setCacheLimit(sizeTemp.get(), sortTemp.get());
		
		// Assets should be listening to changes to these and react as needed.
		
		// Run the cleaner now instead of waiting until the end of a session, as the user likely wants things to change asap.
		app().assets().clean();
		
		finishWithoutPrompt();
		
		Toast.makeText(getActivity(), R.string.ts_changes_saved, Toast.LENGTH_SHORT)
			.show();
	}
	
	private void finishWithoutPrompt() {
		mIsSaved = true;
		finish();
	}
	
}