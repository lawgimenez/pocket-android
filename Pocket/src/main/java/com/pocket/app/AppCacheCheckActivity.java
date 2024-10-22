package com.pocket.app;

import android.os.Bundle;

import com.pocket.sdk.util.AbsPocketActivity;

/**
 * This Activity is a redirect so it can remain the default activity, even though it is no longer needed.
 * 
 *  DO NOT REMOVE THIS ACTIVITY or user's will lose their home screen shortcuts.
 * 
 * // WARNING: This is an exported activity. Extras could come from outside apps and may not be trust worthy.
 *
 */
public class AppCacheCheckActivity extends AbsPocketActivity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startDefaultActivity();
		finish();
	}

	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ANY;
	}
}
