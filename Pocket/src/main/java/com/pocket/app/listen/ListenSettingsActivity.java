package com.pocket.app.listen;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;

public class ListenSettingsActivity extends AbsPocketActivity {
	public static void startActivity(Context context) {
		context.startActivity(new Intent(context, ListenSettingsActivity.class));
	}
	
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {
			// New instance
			setContentFragment(ListenSettingsFragment.newInstance(), null, ListenSettingsFragment.getLaunchMode(this));
		} else {
			// Fragment is restored
		}
	}
	
	@Override public CxtView getActionViewName() {
		return CxtView.LISTEN_SETTINGS;
	}
	
	@Override protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.REQUIRES_LOGIN;
	}
	
	@Override public boolean isListenUiEnabled() {
		return false;
	}
}
