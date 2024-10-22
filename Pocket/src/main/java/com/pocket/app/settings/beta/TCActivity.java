package com.pocket.app.settings.beta;

import android.os.Bundle;

import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;

/**
 * A configuration activity for beta testing. Let's the team or beta testers tweak non-production settings of the app.
 */
public class TCActivity extends AbsPocketActivity {

	@Override
	public CxtView getActionViewName() {
		return CxtView.DEVCONFIG;
	}

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ANY;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		

		if (savedInstanceState == null) {	
			// New instance
			setContentFragment(BetaConfigFragment.newInstance(), null, BetaConfigFragment.getLaunchMode());
		} else {
			// Fragment is restored
		}
	}
	
	@Override public boolean isListenUiEnabled() {
		return false;
	}
}
