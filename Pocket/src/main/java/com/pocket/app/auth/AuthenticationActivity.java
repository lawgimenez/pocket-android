package com.pocket.app.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.activity.FailedLaunchFixException;
import com.pocket.util.android.fragment.FragmentUtil;

import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

/** The initial screen for first run of the app. Will have intro and sign in or sign up UI. */
@AndroidEntryPoint
public class AuthenticationActivity extends AbsPocketActivity {

	public static void startActivity(Context context, boolean skipOnboarding) {
		var intent = new Intent(context, AuthenticationActivity.class)
				.putExtra(AuthenticationFragment.ARG_SKIP_ONBOARDING, skipOnboarding);
		context.startActivity(intent);
	}

	private static final String FRAGMENT_MAIN = "main";
	
	private boolean mAttemptingLaunchFix = false;
	private Fragment mFrag;
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.LOGIN;
	}
	
	@Override
	protected void checkClipboardForUrl() {
		// Do not check in this Activity
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (mFrag == null) {
			mFrag = getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		launchFix();
		
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {

			mFrag = AuthenticationFragment.newInstance(getIntent().getBooleanExtra(AuthenticationFragment.ARG_SKIP_ONBOARDING, false));

			setContentFragment(mFrag, FRAGMENT_MAIN, FragmentUtil.FragmentLaunchMode.ACTIVITY);
			
		} else {
			// Fragment is restored
			if (mFrag == null) {
				mFrag = getSupportFragmentManager().findFragmentByTag(FRAGMENT_MAIN);
			}
		}
	}

	@Override
	public boolean supportsRotationLock() {
		return false;
	}
	
	private void launchFix(){
		/*
		 * This is to fix a problem described here: http://stackoverflow.com/questions/5318885/app-loses-its-ability-to-remember-its-stack-when-launched-from-another-applicatio/5330740#5330740
		 * Basically, when the app is launched from the installer or market and then relaunched from the home screen it gets
		 * in a weird state where it no longer opens the previous activity, but rather puts a new splash activity on the top of the stack
		 * This checks for a certain flag which appears to be given incorrectly when this happens and then if it
		 * finds it, it finishes this instance because the actual current activity should be underneath it in the stack
		 * 
		 * It also includes a fail safe check to make sure this doens't lock someone out of the app.
		 */
		Intent intent = getIntent();
		if(intent != null){
			if((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT){
				if(app().prefs().ALLOW_LAUNCH_FIX.get()){
					mAttemptingLaunchFix = true;
					finish();
					overridePendingTransition(0, 0);
				}
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		checkForFailedLaunchFix();
		super.onDestroy();
	}
	
	private void checkForFailedLaunchFix(){ // REVIEW BUG this is not working any more on kitkat?
		// Fail safe check for launchFix()
		try { 
			// If it tried a launch fix and there is no RIL activity open by the time this is called, then something went wrong
			if (mAttemptingLaunchFix && app().activities().getVisible() == null){
				// Failed.
				Intent intent = getIntent();
				if(intent != null){
					int flags = intent.getFlags();
					Set<String> cats = intent.getCategories();
					String categories = "";
					if(cats != null){
						for(String cat : cats){
							categories = categories + cat;
						}
					}
					throw new FailedLaunchFixException("Failed launch fix with flags: "+flags+" and categories: " + categories );
					
				} else {
					throw new FailedLaunchFixException("Failed launch fix with empty intent" );
				}
			}
		} catch (FailedLaunchFixException e) {
			// Failed.  Disable and report
			app().prefs().ALLOW_LAUNCH_FIX.set(false);
			app().errorReporter().reportError(e);
		}
	}

	@Override
	protected ActivityAccessRestriction getAccessType(){
		return ActivityAccessRestriction.LOGIN_ACTIVITY;
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		if (app().pktcache().isLoggedIn()){
			finish();
		}
	}

	@Override protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// for some reason getting the fragment by tag isn't working on all devices
		for (Fragment fragment : getSupportFragmentManager().getFragments()) {
			if (fragment instanceof AuthenticationFragment) {
				((AuthenticationFragment) fragment).onNewIntent(intent);
			}
		}
	}
}
