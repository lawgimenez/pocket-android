package com.pocket.app.settings.premium;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;

import androidx.fragment.app.Fragment;

/**
 * A thin class to allow {@link PremiumSettingsFragment} to be launched as a fullscreen activity.
 */
public class PremiumSettingsActivity extends AbsPocketActivity {
	
	public static void startActivity(Context context) {
		context.startActivity(newStartIntent(context));
	}
	
	public static Intent newStartIntent(Context context) {
		return new Intent(context, PremiumSettingsActivity.class);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {	
			// New instance
			Fragment frag = PremiumSettingsFragment.newInstance();
			setContentFragment(frag, null, PremiumSettingsFragment.getLaunchMode(this));
		} else {
			// Fragment is restored
		}
	}
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.PREMIUM_SETTINGS;
	}

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.REQUIRES_LOGIN;
	}

}
