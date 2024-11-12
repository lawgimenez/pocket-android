package com.pocket.app.settings.cache;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;

import androidx.fragment.app.Fragment;

/**
 * A thin class to allow {@link CacheSettingsFragment} to be launched as a fullscreen activity.
 */
public class CacheSettingsActivity extends AbsPocketActivity {
	
	public static void startActivity(Context context) {
		context.startActivity(newStartIntent(context));
	}
	
	public static Intent newStartIntent(Context context) {
		Intent intent = new Intent(context, CacheSettingsActivity.class);
		return intent;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {	
			// New instance
			Fragment frag = CacheSettingsFragment.newInstance();
			setContentFragment(frag);
		} else {
			// Fragment is restored
		}
	}
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.CACHE_SETTINGS;
	}

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ALLOWS_GUEST;
	}

}
