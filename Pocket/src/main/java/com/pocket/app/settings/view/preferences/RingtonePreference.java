package com.pocket.app.settings.view.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings.System;
import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.PreferenceViews.EnabledCondition;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.lang3.StringUtils;

/**
 * A special case {@link ActionPreference} for managing a ringtone as a {@link StringPreference}.
 * <p>
 * Lets the user choose between available ringtones on the device and stores the ringtone's uri as
 * the preference value and the ringtone's title as the selected option.
 */
public class RingtonePreference extends ActionPreference {
	
	private static final int REQUEST_CODE = 55;
	
	protected final StringPreference mPref;
	private String mSelected;
	private Uri mSelectedUri;
	
	public RingtonePreference(AbsPrefsFragment settings, StringPreference pref, String label, EnabledCondition condition, UiEntityIdentifier identifier) {
		super(settings, label, null, null, null, condition, identifier);
		
		if (pref == null) {
			throw new NullPointerException("mPref may not be null");
		}
		
		mPref = pref;
		
		setSelected(pref.get());
	}
	
	private void setSelected(String uriString) {
		mSelectedUri = uriString != null ? Uri.parse(uriString) : null;
		mSelected = uriString;
	}
	
	private void setSelected(Uri uri) {
		mSelectedUri = uri;
		mSelected = uri != null ? uri.toString() : null;
	}

	@Override
	public void onClick(View view) {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, System.DEFAULT_NOTIFICATION_URI);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSelectedUri);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
		mSettings.startActivityForResult(intent, REQUEST_CODE);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) {
			return;
		}
		
		setSelected((Uri) data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI));
		
		mPref.set(mSelected);
		mSettings.onPreferenceChange(true);
	}

	@Override
	public boolean update() {
		String newVal = mPref.get();
		if (!StringUtils.equals(newVal, mSelected)) {
			setSelected(newVal);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String getSummary() {
		Context context = mSettings.getActivity();
		if (mSelectedUri == null) {
			return context.getString(R.string.setting_notify_sound_silent_sum); 
		}
		Ringtone ringtone = RingtoneManager.getRingtone(context, mSelectedUri);
		return ringtone.getTitle(context);
	}
	
	@Override
	public boolean isClickable() {
		return true;
	}
	
}
