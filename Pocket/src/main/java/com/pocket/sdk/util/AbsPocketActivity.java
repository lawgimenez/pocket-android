package com.pocket.sdk.util;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Property;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.transition.TransitionManager;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.ideashower.readitlater.BuildConfig;
import com.ideashower.readitlater.R;
import com.pocket.analytics.appevents.SavesEvents;
import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.PocketApp;
import com.pocket.app.ReviewPrompt;
import com.pocket.app.UserManager;
import com.pocket.app.listen.ListenView;
import com.pocket.app.session.Session;
import com.pocket.app.settings.Brightness;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.AppTheme;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk.dev.TeamTools;
import com.pocket.sdk.util.fragment.PocketFragmentManager;
import com.pocket.sdk.util.view.RainbowBar;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.ContextualRoot;
import com.pocket.sdk2.analytics.context.ContextualRootBinder;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.ui.view.themed.ThemeColors;
import com.pocket.ui.view.themed.Themed;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.android.ContextUtil;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.IntentUtils;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.WindowUtil.NavigationBarColorProperty;
import com.pocket.util.android.WindowUtil.StatusBarColorProperty;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;
import com.pocket.util.android.view.ManuallyUpdateTheme;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.java.Safe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

/**
 * The base activity for Pocket's screens. Automatically handles tracking and {@link AppLifecycle} events.
 * See {@link #isUserPresent()} to opt out.
 */
public abstract class AbsPocketActivity extends AppCompatActivity implements Session.Segment, ContextualRoot, Themed {

	public static final int DIALOG_SUBCLASS = 20; // Should be higher than any generic dialog ids
	public static final boolean DEBUG_LIFECYCLE = BuildConfig.DEBUG && true;
	
	protected PocketActivityContentView mContent;
	
	public static final String ACTION_SHUTDOWN = "com.ideashower.readitlater.ACTION_SHUTDOWN"; 
	public static final String ACTION_LOGOUT = "com.ideashower.readitlater.ACTION_LOGOUT";
	public static final String ACTION_LOGIN = "com.ideashower.readitlater.ACTION_LOGIN";
	
	public enum ActivityAccessRestriction {
		/**
		 * The user must be logged in. Activity will auto-finish if not logged in or on log out.
		 */
		REQUIRES_LOGIN,
		/**
		 * The user must be logged out. Activity will auto-finish if logged in or on log in.
		 */
		REQUIRES_LOGGED_OUT,
		/**
		 * A special case for the Login/Splash Activity. Similar to {@link #REQUIRES_LOGGED_OUT} as it will auto-finish  if logged in or on log in.
		 * The only difference is that it will auto launch the default activity as a replacement. The other types just close and offer no replacement.
		 */
		LOGIN_ACTIVITY,
		/**
		 * Behaves as ANY if the user is opted into the Guest Mode experience. Otherwise, functions the same as REQUIRES_LOGIN
		 */
		ALLOWS_GUEST,
		/**
		 * Can be launched at any time, not dependent on the user state.
		 */
		ANY
	}
	
	public static final int MENU_GROUP_APP = -1;
	public static final int MENU_GROUP_ACTIVITY = -2;
	public static final int MENU_ITEM_SETTINGS = 1;
	public static final int MENU_ITEM_HELP = 2;
	
	private static final ThemeChange THEME_CHANGE = new ThemeChange();
	private static final StatusBarColorProperty STATUS_BAR_COLOR = new StatusBarColorProperty();
	private static final NavigationBarColorProperty NAVIGATION_BAR_COLOR = new NavigationBarColorProperty();
	
	private final ArrayList<OnLifeCycleChangedListener> mOnLifeCycleChangedListeners = new ArrayList<>();
	private final ArrayList<OnBackPressedListener> mOnBackPressedListeners = new ArrayList<>();
	private final ArrayList<OnConfigurationChangedListener> mOnConfigurationChangedListeners = new ArrayList<>();

	private final ContextualRootBinder mActionContext = new ContextualRootBinder();

	protected boolean mIsHelpActivity = false;
	
	protected Handler mHandler;
	
	private BroadcastReceiver mAccessReceiver;
	private BroadcastReceiver mFullShutdownReceiver;
	private final String EXTRA_KILL_APP = "killApp";
	protected PocketActivityRootView mRoot;

	protected boolean mMenuVisible = true;
	
	private int mTheme;
	
	protected ArrayList<WeakReference<ManuallyUpdateTheme>> mViewsListeningForThemeChanges = new ArrayList<WeakReference<ManuallyUpdateTheme>>();

	private int mThemeFlag;
	
	private Disposable mThemeSubscription = Disposables.empty();
	
	/**
	 * Whether or not the ask overlay is visible or in the process of becoming visible (animating).
	 */
	private boolean mAskUrlOverlayVisible;

	private Toast mToasty;

	private final PocketFragmentManager mFragmentManager = new PocketFragmentManager(super.getSupportFragmentManager(), this);
	private boolean mIsContentSet;
	private Rect mWindowInsets = new Rect();

	public static final String EXTRA_UI_CONTEXT = "com.pocket.extra.uiContext";

	/**
	 * @deprecated get components through dagger/hilt dependency injection
	 */
	@Deprecated
	public PocketApp app() {
		return (PocketApp) getApplication();
	}

	/**
	 * @deprecated get components through dagger/hilt dependency injection
	 */
	@Deprecated
	public Pocket pocket() {
		return app().pocket();
	}
	
	/**
	 * Returning the name of the current view for action cxt_view
	 */
	public CxtView getActionViewName() { return null; }

	/**
	 * Whether or not this activity should trigger {@link App#onActivityChange(AbsPocketActivity)} and be considered
	 * that the user is present in the app and trigger {@link AppLifecycle#onUserPresent()} and related flows like opened_app events.
	 * Defaults to true, override to opt-out or adjust.
	 * <b>The value here should not change between calls.</b> If it does, then we need to store a value from the first call in onResume to when it is checked again in onPause
	 */
	public boolean isUserPresent() {
		return true;
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && app().mode().isForInternalCompanyOnly()) {
			new TeamTools(this).show();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	/**
	 * Since we seem to do this type casting a lot, this is a helper method for cleaner code. Pass a context
	 * and if the context is a RilAppActivity it will return one, other wise it returns null.
	 * 
	 * @param context 
	 * @return the context casted to a RilAppActivity if it is not null and is one, otherwise null.
	 */
	public static AbsPocketActivity from(Context context) {
		Activity activity = ContextUtil.getActivity(context);
        if (activity instanceof AbsPocketActivity) {
            return (AbsPocketActivity) activity;
        } else {
            return null;
        }
	}

	@SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onCreate " + (savedInstanceState != null ? "restore " : "new ") + this.toString());

		FormFactor.init();

		super.onCreate(savedInstanceState);
		
		mThemeFlag = getDefaultThemeFlag();
		mTheme = app().theme().get(this);
		mHandler = new Handler();
        
		setActivityTheme(app().theme().get(this));
		
		// Create the root layout structure that will wrap the view supplied by subclasses.
		if (mIsContentSet) {
			Logs.throwIfNotProduction("You must call the super.onCreate() of AbsPocketActivity before calling setContentView");
		}
		super.setContentView(R.layout.activity_root);
		mRoot = findViewById(R.id.pocket_root);
		mRoot.attach(this);
		mContent = mRoot.getContentView();

		setBackgroundDrawable();
		
		if (!isFinishing()) {
			installLogoutReceiver(getAccessType());
		}
		
		if (savedInstanceState != null) {
			getPocketFragmentManager().onRestoreInstanceState(savedInstanceState);
		}
		
		onCreateOrRestart();
		
		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityCreate(savedInstanceState, this);
		}

		String label = getString(this.getApplicationInfo().labelRes);
		int colorPrimary = ContextCompat.getColor(this, com.pocket.ui.R.color.pkt_coral_2);

		setTaskDescription(new ActivityManager.TaskDescription(label, null, colorPrimary));

		ViewCompat.setOnApplyWindowInsetsListener(mRoot, (v, insets) -> {
			mWindowInsets.set(insets.getSystemWindowInsetLeft(),
					insets.getSystemWindowInsetTop(),
					insets.getSystemWindowInsetRight(),
					insets.getSystemWindowInsetBottom());

			updateAskUrlOverlayPadding(PktSnackbar.getCurrent());
			return insets;
		});

        app().activities().onActivityCreate(this);
    }

	@Override
	public final void setContentView(View view) {
		// Overridden to insert into our root layout instead.
		mIsContentSet = true;
		mContent.addView(view);
	}
	
	@Override
	public final void setContentView(int layoutResID) {
		// Overridden to insert into our root layout instead.
		mIsContentSet = true;
		getLayoutInflater().inflate(layoutResID, mContent);
	}
	
	@Override
	public final void setContentView(View view, LayoutParams params) {
		// Overridden to insert into our root layout instead.
		mIsContentSet = true;
		mContent.addView(view, params);
	}
	
	/**
	 * Similar to {@link #setContentView(View)} but allows you to supply a Fragment as your root layout.
	 * <p>
	 * Sets the fragment tag as null, if you want to set a tag, use {@link #setContentFragment(Fragment, String)}.
	 * @param fragment
	 */
	public final void setContentFragment(Fragment fragment) {
		setContentFragment(fragment, null);
	}
	
	/**
	 * Similar to {@link #setContentView(View)} but allows you to supply a Fragment as your root layout.
	 * @param fragment
	 * @param tag
	 */
	public final void setContentFragment(Fragment fragment, String tag) {
		mIsContentSet = true;
		FragmentUtil.addFragment(fragment, this, R.id.content, tag, false);
	}
	
	/**
	 * A convenience method to show this fragment based on its launch mode.
	 * <p>
	 * {@link FragmentLaunchMode#ACTIVITY} will add the fragment as the main content, same as {@link #setContentFragment(Fragment)}.
	 * <p>
	 * {@link FragmentLaunchMode#ACTIVITY_DIALOG} depends on the result of {@link FormFactor#showSecondaryScreensInDialogs(Context)}.
	 * If false, it is handled the same as the {@link FragmentLaunchMode#ACTIVITY} mode. If true, the content view will be set
	 * to a full screen rainbow bar and the fragment show as a dialog over it. Note in this case, the fragment must be an instance of
	 * {@link DialogFragment} or an exception will be thrown. Another note is that if this fragment is dismissed, the activity will also
	 * automatically finish itself because there is no other content to view.
	 * <p>
	 * <b>Warning</b> No other {@link FragmentLaunchMode}'s are supported by this method.
	 * 
	 * @param fragment
	 * @param tag
	 * @param mode
	 */
	public final void setContentFragment(Fragment fragment, String tag, FragmentLaunchMode mode) {
		if (mode == FragmentLaunchMode.ACTIVITY) {
 			setContentFragment(fragment);
 			
 		} else if (mode == FragmentLaunchMode.ACTIVITY_DIALOG
 		|| mode == FragmentLaunchMode.DIALOG) { // If dialog, just launch as activity dialog.
 			if (FormFactor.showSecondaryScreensInDialogs(this)) {
	 			// This is a special case where we launch it as dialog with a rainbow covering the background.
 				// There is no additional view layout to the activity.
	 			RainbowBar rainbow = new RainbowBar(this);
				rainbow.getRainbow().setBorderVisible(false);
				rainbow.setLayoutParams(new ViewGroup.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				setContentView(rainbow);
				
				FragmentUtil.addFragmentAsDialog((DialogFragment) fragment, this, tag);
				
				// If this fragment is dismissed (from a back button for example), finish this activity since there is no other content to show.
				getPocketFragmentManager().addOnBackStackChangedListener(new OnBackStackChangedListener() {
					
					@Override
					public void onBackStackChanged() {
						PocketFragmentManager frags = getPocketFragmentManager();
						if (frags.getBackStackEntryCount() == 0 || frags.getFragments().isEmpty()) {
							finish();
						}
					}
				});
				
 			} else {
 				setContentFragment(fragment, tag);
 			}
			
		} else {
			throw new RuntimeException("unexpected mode");
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getPocketFragmentManager().onSaveInstanceState(outState);
	}
	
	/**
	 * The default Theme Flag to use when creating this Activity.
	 * Subclasses can override this to change the flag for their activity, or change it at runtime with {@link #setThemeFlag(int)}.
	 * See {@link #getThemeFlag()} for the current value.
	 */
	protected int getDefaultThemeFlag() {
		return Theme.FLAG_ALLOW_ALL;
	}
	
	@Override
	public int[] getThemeState(@NonNull View view) {
		return app().theme().getState(view);
	}

	@NonNull @Override
	public ThemeColors getThemeColors(@NonNull Context context) {
		return getThemeColors(app().theme().get(context));
	}

	@NonNull @Override
	public Observable<ThemeColors> getThemeColorsChanges(@NonNull Context context) {
		return app().theme()
				.observeFor(context)
				.map(this::getThemeColors);
	}

	@NonNull private ThemeColors getThemeColors(int theme) {
		switch (theme) {
			case Theme.DARK:
				return ThemeColors.DARK;
			case Theme.LIGHT:
			default:
				return ThemeColors.LIGHT;
		}
	}

	/**
	 * Something has modified the theme (dark/light mode), the UI should update as needed.
	 *
	 * @param newTheme
	 */
	public void onThemeChanged(int newTheme) {
		TransitionManager.beginDelayedTransition(mRoot, THEME_CHANGE);
		setActivityTheme(newTheme);
		ViewUtil.refreshDrawableStateDeep(mRoot.getRootView());
		
		// Manually update any web views
		for (WeakReference<ManuallyUpdateTheme> reference : mViewsListeningForThemeChanges) {
			ManuallyUpdateTheme view = reference.get();
			if (view != null) {
				view.updateThemeManually();
			}
		}
		
		setBackgroundDrawable();
		mTheme = newTheme;
		
		// Dispatch to any visible fragments
		mFragmentManager.onThemeChanged(newTheme);
		
		invalidateStatusBarColor();
	}

	/**
	 * Update the system bar colors to the current theme.
	 */
	public void invalidateStatusBarColor() {
		final int statusBarColor = Theme.getStatusBarColor(mTheme, this);
		animateThemeColorChange(STATUS_BAR_COLOR, statusBarColor);

		int systemUiFlags = 0;
		if (mTheme != Theme.DARK) {
			systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		}
		if (ApiLevel.isLightNavigationBarAvailable()) {
			if (mTheme != Theme.DARK) {
				systemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
			}
			animateThemeColorChange(NAVIGATION_BAR_COLOR, statusBarColor);
			if (showNavigationBarDivider()) {
				getWindow().setNavigationBarDividerColor(
						Theme.getNavigationBarDividerColor(mTheme, this));
			}
		}
		getWindow().getDecorView().setSystemUiVisibility(systemUiFlags);
	}
	
	private void animateThemeColorChange(Property<Window, Integer> property, Integer value) {
		final ObjectAnimator animator = ObjectAnimator.ofInt(getWindow(), property, value);
		animator.setDuration(ThemeChange.DURATION);
		animator.setEvaluator(ThemeChange.ARGB_EVALUATOR);
		animator.start();
	}
	
	/** Override to hide the navigation bar divider. */
	protected boolean showNavigationBarDivider() {
		return true;
	}

	/**
	 * Sets the background drawable (window) for the activity to getActivityBackground.  If getActivityBackground is null, it will not change anything.
	 */
	private void setBackgroundDrawable() {
		Drawable bg = getActivityBackground();
		if (bg != null) {
			setBackgroundDrawable(bg);
		}
	}
	
	/**
	 * Sets the activities background (window background)
	 * @param drawable
	 */
	protected void setBackgroundDrawable(Drawable drawable) {
		getWindow().setBackgroundDrawable(drawable);
	}
	
	private void setActivityTheme(int newTheme) {
		setTheme(themeOverride() != 0 ? themeOverride() : Theme.isDark(newTheme) ? R.style.Theme_PocketDefault_Dark : R.style.Theme_PocketDefault_Light);
	}

	/**
	 * @return a style resource to use as the activity theme.
	 */
	protected @StyleRes int themeOverride() {
		return 0;
	}
	
	@Override
	protected void onRestart() {
		if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onRestart " + this.toString());
		
		installLogoutReceiver(getAccessType());
		
		super.onRestart();
		
		int theme = app().theme().get(this);
		if (mTheme != theme) {
			mTheme = theme;
			mFragmentManager.onThemeChanged(theme);
		}
		
		onCreateOrRestart();
		
		// Dispatch to any visible fragments
		mFragmentManager.onActivityRestart();
		
		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityRestart(this);
		}
	}

	protected void onCreateOrRestart() {
		Brightness.applyBrightnessIfSet(this);
        invalidateStatusBarColor();
	}
	
	@Override
	public FragmentManager getSupportFragmentManager() {
		return mFragmentManager;
	}
	
	public PocketFragmentManager getPocketFragmentManager() {
		return mFragmentManager;
	}
	
	/**
	 * TODO REVIEW
	 * This mechanism is to make sure all activities are finished when the user logs out,
	 * so that no old activities with data from the logged out user persist or are restored.
	 * The idea here is that it registers for a ACTION_LOGOUT broadcast and finishes when it
	 * receives it. This has been in here since the beginning of the app and for many many years.
	 *
	 * However, recently we discovered there is a case this does not properly handle.
	 * If the activity is removed from memory, it will not receive the broadcast and
	 * has the potential to be restored later.  An easy way to experiment with this is
	 * "Don't keep activities" in Developer Options.
	 *
	 * For now, the easiest fix seemed to simply be to use {@link Activity#finishAffinity()}
	 * in {@link UserManager#logout(AbsPocketActivity)} and that is the fix that was implemented for
	 * starters. But really, it means we likely don't need this complexity any more.
	 *
	 * At some point we should review how this works and see if we can remove these broadcasts.
	 */
	private void installLogoutReceiver(ActivityAccessRestriction accessType) {

		// if the access restriction is ALLOWS_GUEST and this user is opted into
		// allow guest mode, allow them to view this screen without logging in.
		// Otherwise switch it to REQUIRES_LOGIN.
		if (accessType == ActivityAccessRestriction.ALLOWS_GUEST) {
			accessType = ActivityAccessRestriction.REQUIRES_LOGIN;
		}

		if (accessType != ActivityAccessRestriction.ANY){
			// Register this activity to be finished if the login state changes
		
	        String action = null;
		    		
			if (accessType == ActivityAccessRestriction.REQUIRES_LOGIN){
				if (!app().pktcache().isLoggedIn() && !mIsHelpActivity){
					finish();
				
				} else {
					action = ACTION_LOGOUT;
					
				}
				
			} else {
				if (app().pktcache().isLoggedIn()) {
					if(accessType == ActivityAccessRestriction.LOGIN_ACTIVITY){
						// This shouldn't be called unless it needs to launch, since a login should finish any previously open instances.
						startDefaultActivity();
					}
					
					finish();
				
					
				} else {
					action = ACTION_LOGIN;
					
				}
				
			}
			
			if (action != null && mAccessReceiver == null){
				IntentFilter intentFilter = new IntentFilter();
			    intentFilter.addAction(action);
			    
			    mAccessReceiver = new BroadcastReceiver() {
	
		            @Override
					public void onReceive(Context context, Intent intent) {
						finish();
					}
		            
		        };
				
				LocalBroadcastManager.getInstance(AbsPocketActivity.this)
			    	.registerReceiver(mAccessReceiver, intentFilter);
			}
		}
		
		// Register a full shutdown receiver
		if (mFullShutdownReceiver == null){
			IntentFilter shutdownIntentFilter = new IntentFilter();
			shutdownIntentFilter.addAction(ACTION_SHUTDOWN);
			mFullShutdownReceiver = new BroadcastReceiver() {
				
				@Override
				public void onReceive(Context context, Intent intent) {
					finish();
					if(intent.getBooleanExtra(EXTRA_KILL_APP, false))
						android.os.Process.killProcess(android.os.Process.myPid());
				}
			};
			
			LocalBroadcastManager.getInstance(AbsPocketActivity.this)
					.registerReceiver(mFullShutdownReceiver, shutdownIntentFilter);
		}
		
	}
	
	/**
	 * This will send a RilAppActivity.ACTION_SHUTDOWN action to all RilAppActivitys in the Task. The action
	 * will cause all of the open RilAppActivitys to finish().
	 * 
	 * @param killApp if it should also force close/kill the process. This will completely close the app abruptly.
	 */
	public void finishAllActivities(boolean killApp) {
		// Used to completely kill the app, such as when a database error occurs
		Intent broadcastIntent = new Intent();
		broadcastIntent.putExtra(EXTRA_KILL_APP, killApp);
		broadcastIntent.setAction(ACTION_SHUTDOWN);
		LocalBroadcastManager.getInstance(AbsPocketActivity.this)
			.sendBroadcast(broadcastIntent);
	}
	
	/**
	 * Declare what state the user must be in, in order to use this Activity.
	 *  
	 * @return One of the {@link ActivityAccessRestriction} values.
	 */
	protected abstract ActivityAccessRestriction getAccessType();
	
	@Override
	protected void onStart() {
		super.onStart();
		mThemeSubscription = app().theme().observeFor(this).subscribe(this::onThemeChanged);
		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityStart(this);
		}
	}
	
	@Override
    public void onResume() {
		if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onResume " + this.toString());

		if (isUserPresent()) App.onActivityChange(this);
        app().session().startSegment(this);
        
		super.onResume();

		checkIfEligibleForReviewPrompt();
		
		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityResume(this);
		}
    }

    @Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			checkClipboardForUrl();
		}
	}
    
	@Override
	public void onBackPressed() {
		if (getRoot().onBackPressed()) {
			return; // Handled
		}
		
		if (mFragmentManager.onBackPressed()) {
			return; // Handled
		}

		if (!mOnBackPressedListeners.isEmpty()) {
			for (OnBackPressedListener listener : new ArrayList<>(mOnBackPressedListeners)) { // Iterates on a copy to allow listeners to remove themselves during callback/iteration without a concurrent mod exceptions.
				if (listener.onBackPressed()) {
					return; // Handled
				}
			}
		}
		
		super.onBackPressed();
	}

	// can be called by children to skip the onBackPressed logic in this class
	protected void superBackPressed() {
		super.onBackPressed();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (App.getActivityContext() == null) {
			// User went to a screen that is not in our app.
			App.setUserPresent(false, this);
		}
		
		mThemeSubscription.dispose();
		
		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityStop(this);
		}
	}

	private final String UTM_SOURCE = "utm_source";
	private final String POCKET_UTM_SOURCE = "pocket_mylist";

	/**
	 * Checks the clipboard for a url.
	 * If there is a url available it asks the user if they want to save it to their list.
	 */
	protected void checkClipboardForUrl() {
		if (!app().pktcache().isLoggedIn()) {
			// Don't check if not logged in, because they have to log in to save.
			return;
		}

		String url = app().clipboard().getUrl();
		String utmSource;
		try {
			utmSource = Uri.parse(url).getQueryParameter(UTM_SOURCE);
		} catch (Exception e) {
			utmSource = null;
		}
		if (url != null && !Objects.equals(utmSource, POCKET_UTM_SOURCE)) {
			showAskUrl(url);
		}
	}
	
	private void showAskUrl(final String url) {

		// TODO if you share Copy Link, don't show this for that link

		PktSnackbar ask = PktSnackbar.make(this, PktSnackbar.Type.DEFAULT_DISMISSABLE, null, url.replaceAll("https?:\\/\\/(www.)?", ""), null);
		ask.bind().onAction(com.pocket.ui.R.string.ac_save, new View.OnClickListener() {

			@StringRes int message;

			@Override
			public void onClick(View v) {
				app().tracker().track(SavesEvents.INSTANCE.clipboardPromptSaveClicked(url));

				ask.bind().dismiss();
				mAskUrlOverlayVisible = false;

				app().threads().asyncThen(() -> {
					Item item = ItemUtil.create(url, pocket().spec());
					boolean alreadySaved;
					try {
						alreadySaved = pocket().syncLocal(item).get() != null;
					} catch (SyncException e) {
						alreadySaved = false;
					}

					try {
						pocket().sync(null, pocket().spec().actions().add()
								.url(new UrlString(url))
								.time(Timestamp.now())
								.build())
								.get();

						if (alreadySaved) {
							message = R.string.ts_add_already;
						} else {
							message =  R.string.ts_add_added;
						}
					} catch (SyncException ignore) {
						message = R.string.ts_add_error;
					}
				}, (success, operationCrash) -> {
					PktSnackbar confirm = PktSnackbar.make(AbsPocketActivity.this, message != R.string.ts_add_added ? PktSnackbar.Type.ERROR_DISMISSABLE : PktSnackbar.Type.DEFAULT_DISMISSABLE, null, getText(message), null);
					updateAskUrlOverlayPadding(confirm);
					onClipboardUrlPromptViewLayout(confirm);
					confirm.bind().onDismiss(__ -> onClipboardUrlPromptViewDismissed(confirm));
					confirm.show();

					// Hide in 3 seconds
					mHandler.postDelayed(() -> confirm.bind().dismiss(), Milliseconds.SECOND * 3);
				});
			}
		}).singleLineMessage(true).title(getText(R.string.lb_add_copied_url));

		updateAskUrlOverlayPadding(ask);
		onClipboardUrlPromptViewLayout(ask);
		ask.bind().onDismiss(__ -> onClipboardUrlPromptViewDismissed(ask));
		
		// Show
		mAskUrlOverlayVisible = true;
		ask.show();
		
		// Hide in 10 seconds
		mHandler.postDelayed(() -> {
			ask.bind().dismiss();
			mAskUrlOverlayVisible = false;
		}, Milliseconds.SECOND * 10);
	}
	
	/**
	 * If needed, this can be overridden to move the clipboard prompt to
	 * a better spot for a specific layout.
	 *
	 * @param view The view to move to a different position in the layout.
	 */
	protected void onClipboardUrlPromptViewLayout(PktSnackbar view) {}
	
	/**
	 * If needed this can be overridden to add custom dismissal handling.
	 *
	 * @param view the view that was just dismissed
	 */
	protected void onClipboardUrlPromptViewDismissed(PktSnackbar view) {}

	/**
	 * Changes the anchor View used for the URL clipboard View.
	 */
	protected void setClipboardUrlAnchor(PktSnackbar bar, View anchor) {
		PktSnackbar.setAnchor(this, bar, anchor);
	}
	
	protected void updateAskUrlOverlayPadding(View askView) {
		if (askView != null) {
			final int paddingDefault = (int) getResources().getDimension(com.pocket.ui.R.dimen.pkt_space_sm);
			askView.setPadding(paddingDefault + mWindowInsets.left, paddingDefault, paddingDefault + mWindowInsets.right, paddingDefault + mWindowInsets.bottom);
		}
	}
	
	private void checkIfEligibleForReviewPrompt() {
		ReviewPrompt reviewPrompt = app().reviewPrompt();
		if (reviewPrompt.shouldShow() && !mAskUrlOverlayVisible) {
			if (app().build().isAmazonBuild()) {
				showAmazonStoreReviewPrompt();
			} else {
				final ReviewManager manager = ReviewManagerFactory.create(this);
				manager.requestReviewFlow().addOnCompleteListener(requestFlow -> {
					if (requestFlow.isSuccessful()) {
						manager.launchReviewFlow(this, requestFlow.getResult());
						reviewPrompt.onShow(); // track review prompt show analytics
						reviewPrompt.onReview();
					} else {
						reviewPrompt.onReviewPromptError();
					}
				});
			}
		}
		reviewPrompt.onResumeAnotherScreen();
	}

	private void showAmazonStoreReviewPrompt() {
		ReviewPrompt reviewPrompt = app().reviewPrompt();
		PktSnackbar review = PktSnackbar.make(this, PktSnackbar.Type.DEFAULT_DISMISSABLE, null, null, null);
		review.bind()
				.title(getText(R.string.tx_love_pocket))
				.message(getText(R.string.tx_tell_others))
				.onAction(R.string.ac_write_review, v -> {
					review.bind().dismiss();
					Uri appStoreUri = Uri.parse("market://details?id=com.ideashower.readitlater.pro");
					final Intent intent = new Intent(Intent.ACTION_VIEW, appStoreUri);
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					if (IntentUtils.isActivityIntentAvailable(this, intent)) {
						startActivity(intent);
					} else {
						new AlertDialog.Builder(this)
								.setTitle(R.string.dg_market_not_found_t)
								.setMessage(R.string.dg_market_not_found_m)
								.setNeutralButton(R.string.ac_ok, null)
								.show();
					}
					reviewPrompt.onReview();
				})
				.onDismiss(reason -> {
					if (reason == PktSnackbar.DismissReason.USER) {
						reviewPrompt.onDismiss();
					}
					onClipboardUrlPromptViewDismissed(review);
				});
		updateAskUrlOverlayPadding(review);
		onClipboardUrlPromptViewLayout(review);
		review.show();
		reviewPrompt.onShow();

		// Hide in 10 seconds
		mHandler.postDelayed(() -> review.bind().dismiss(), Milliseconds.SECOND * 10);
	}

	@Override
    public void onPause() {
		if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onPause " + this.toString());

		if (isUserPresent()) App.onActivityChange(null);
		app().session().closeSegment(this);

    	super.onPause();
    	
    	for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityPause(this);
		}
    }
    
	@Override
	protected void onDestroy() {
		if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onDestroy " + this.toString());
		
		super.onDestroy();
		
		unregisterRecievers();

		for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
			listener.onActivityDestroy(this);
		}
		mHandler.removeCallbacksAndMessages(null); // Otherwise we can accidentally keep the activity around for a few seconds after destroy.
	}

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        for (OnLifeCycleChangedListener listener : mOnLifeCycleChangedListeners) {
            listener.onActivityLowMemory(this);
        }
    }

    @Override
	public void finish() {
		unregisterRecievers();
		super.finish();
	}

	private void unregisterRecievers(){
		if(mAccessReceiver != null){
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccessReceiver);
			mAccessReceiver = null;
		}
		
		if(mFullShutdownReceiver != null){
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mFullShutdownReceiver);
			mFullShutdownReceiver = null;
		}
	}
	
	/**
	 * Return the window background for this activity, or null to just use the current window background.
	 * @return
	 */
	protected Drawable getActivityBackground(){
		return new ColorDrawable(app().theme().getThemeBGColor(this));
	}
	
	@Override
	public boolean onSearchRequested(){
		if (!isMenuVisible())
			return false;
		
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		populateMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}
		
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean visible = isMenuVisible();
		menu.setGroupVisible(MENU_GROUP_APP, visible);
		menu.setGroupVisible(MENU_GROUP_ACTIVITY, visible);
		return super.onPrepareOptionsMenu(menu);
	}
	
	protected boolean isMenuVisible() {
		return mMenuVisible;
	}
	
	protected void populateMenu(Menu menu) {
		menu.add(MENU_GROUP_APP, MENU_ITEM_SETTINGS, 1, getString(R.string.mu_settings))
			.setIcon(R.drawable.ic_menu_settings);
		
		if (!mIsHelpActivity) {
			menu.add(MENU_GROUP_APP, MENU_ITEM_HELP, 2, getString(R.string.mu_help))
				.setIcon(R.drawable.ic_menu_help);
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setBackgroundDrawable();

        for (OnConfigurationChangedListener listener : mOnConfigurationChangedListeners) {
            listener.onConfigurationChanged(newConfig);
        }
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	/** NOTE currently disabled. if required again, uncomment out code below
	 * Set the software dimming level.  
	 * 
	 * @param alpha 0 - 255. 0 for no dimming, 255 for complete black out.
	 */
	public void setBrightnessOverlay(int alpha) {
		/*
		if (mSoftwareBrightnessOverlay == null && alpha > 0) {
			mSoftwareBrightnessOverlay = (ImageView) ((ViewStub) findViewById(R.id.stub_brightness)).inflate();
			mSoftwareBrightnessOverlay.setVisibility(View.VISIBLE);
		}
		mSoftwareBrightnessOverlay.setAlpha(alpha);
		*/
	}
	
	/**
	 * Launches the default, starting Activity for the Pocket app.
	 */
	public void startDefaultActivity() {
		Class<? extends Activity> activity = app().user().getDefaultActivity();
		startActivity(new Intent(this, activity));
	}

	/**
	 * Set the content view to be invisible.
	 */
	public void hideContent() {
		mContent.setVisibility(View.INVISIBLE);
	}

	public void registerViewForThemeChanges(ManuallyUpdateTheme view) {
		mViewsListeningForThemeChanges.add(new WeakReference<ManuallyUpdateTheme>(view));
	}
	
	
	/**
	 * Set what themes are currently allowed to display in the app. Will refresh views if it causes a theme change.
	 * 
	 * One of {@link Theme.FLAG_ALLOW_ALL}, {@link Theme.FLAG_ONLY_DARK}, {@link Theme.FLAG_ONLY_LIGHT}
	 * @param flag
	 */
	public void setThemeFlag(int flag) {
		int currentTheme = app().theme().get(this);
		mThemeFlag = flag;
		int newTheme = app().theme().get(this);
		
		if (newTheme != currentTheme) {
			onThemeChanged(newTheme);
		}
	}

	/**
	 * Gets a flag for which themes are currently allowed.  Should be one of the FLAG_ values in Theme
	 * To change the flag for your activity override {@link #getDefaultThemeFlag()}
	 * @return
	 */
	public final int getThemeFlag() {
		return mThemeFlag;
	}
	
	/**
	 * Uses a shared toast message for a  RilAppActivity. This is helpful for when toasts might happen fast enough
	 * to overlap. This will ensure that the new toast is visible right away instead of waiting for the previous
	 * toast to finish before becoming visible.
	 * 
	 * This method does not call show() on the new Toast.
	 * 
	 * @param context should be a RilAppActivity, but can pass a context for coding convenience.
	 * @param text
	 * @param res if text is null, it will use a resource id, otherwise it is ignored
	 * @param duration
	 * @return
	 */
	@SuppressLint("ShowToast")
	public static Toast toast(Context context, String text, int res, int duration) {
		AbsPocketActivity activity = (AbsPocketActivity) context;
		if (activity.mToasty == null) {
			if (text != null) {
				activity.mToasty = Toast.makeText(context, text, duration);
			} else {
				activity.mToasty = Toast.makeText(context, res, duration);
			}
		}
		// REVIEW why isn't this part in a else?
		activity.mToasty.setDuration(duration);
		if (text != null) {
			activity.mToasty.setText(text);
		} else {
			activity.mToasty.setText(res);
		}
		return activity.mToasty;
	}
	
	protected boolean supportsRotationLock() {
		return true;
	}
	
	public PocketActivityRootView getRoot() {
		return mRoot;
	}

	/**
	 * Searches this Activities' fragments to find which one holds a certain view. If the parent view is found, it is returned. Otherwise null.
	 * @param view
	 * @return
	 */
	public Fragment getFragmentParentOfView(View view) {
		List<Fragment> fragments = getPocketFragmentManager().getVisibleFragments();
		for (Fragment frag : fragments) {
			if (FragmentUtil.isDetachedOrFinishing(frag)) {
				continue;
			}
			
			View root = FragmentUtil.getRootView(frag);
			if (root == null) {
				continue;
			}
			
			if (ViewUtil.containsView(root, view)) {
				return frag;
			}
		}
		return null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		for (OnLifeCycleChangedListener listener :  mOnLifeCycleChangedListeners) {
			listener.onActivityResult(this, requestCode, resultCode, data);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		for (OnLifeCycleChangedListener listener :  mOnLifeCycleChangedListeners) {
			listener.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	public int getThemeInt() {
		return app().theme().get(this);
	}
	
	/**
	 * @return The {@link ActionContext} for why this Activity was started or null if none was set.
	 */
	public ActionContext getLaunchUiContext() {
		return Safe.get(() -> Parceller.get(getIntent(), EXTRA_UI_CONTEXT, ActionContext.JSON_CREATOR)); // Wrapped in a safe call because we saw some crashes where we were receiving an intent that had a parcelable of a class that was removed in previous versions, so there would be no way to unparcel it since the class doesn't exist. This seems like an OS bug, since on app update, all of its task stack should have been cleared. For now, just using Safe to ignore it. If we find a deeper issue we can revisit. https://appcenter.ms/orgs/pocket-app/apps/Android-Production-Google-Play-Amazon-App-Store/crashes/errors/3888297086u/reports/2518229398449999999-1ea2fcfd-b63e-4cd9-8c0d-7bdd066d7e86/raw
	}
	
	/**
	 * Add a listener for life cycle events like onCreate and onPause.
	 * <b>Warning</b> if the activity is taken out of memory and recreated,
	 * this will not automatically be readded for you. This can break
	 * callbacks such as onActivityResult or onRequestPermissionsResult
	 * if you don't readd the listener during onCreate
	 *
	 * @param listener
	 * @see SimpleOnLifeCycleChangedListener
	 */
	public void addOnLifeCycleChangedListener(OnLifeCycleChangedListener listener) {
		mOnLifeCycleChangedListeners.add(listener);
	}

	public void removeOnLifeCycleChangeListener(OnLifeCycleChangedListener listener) {
		mOnLifeCycleChangedListeners.remove(listener);
	}

	public void addOnBackPressedListener(OnBackPressedListener listener) {
		mOnBackPressedListeners.add(listener);
	}
	
	public void removeOnBackPressedListener(OnBackPressedListener listener) {
		mOnBackPressedListeners.remove(listener);
	}

	public interface OnLifeCycleChangedListener {
		void onActivityCreate(Bundle savedInstanceState, AbsPocketActivity activity);
		void onActivityRestart(AbsPocketActivity activity);
		void onActivityStart(AbsPocketActivity activity);
		void onActivityResume(AbsPocketActivity activity);
		void onActivityPause(AbsPocketActivity activity);
		void onActivityStop(AbsPocketActivity activity);
		void onActivityDestroy(AbsPocketActivity activity);
		void onActivityLowMemory(AbsPocketActivity activity);
		void onActivityResult(AbsPocketActivity activity, int requestCode, int resultCode, Intent data);

		void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);

//		void onFocusedFragmentChange(AbsPocketActivity activity, Fragment focus);
	}

	public interface OnBackPressedListener {
		/**
		 * @return true if handled.
		 */
		boolean onBackPressed();
	}

	public void addOnConfigurationChangedListener(OnConfigurationChangedListener listener) {
		mOnConfigurationChangedListeners.add(listener);
	}
	
	public void removeOnConfigurationChangedListener(OnConfigurationChangedListener listener) {
		mOnConfigurationChangedListeners.remove(listener);
	}

	public interface OnConfigurationChangedListener {
		void onConfigurationChanged(Configuration newConfig);
	}
	
	/**
	 * A version of {@link OnLifeCycleChangedListener} that has no-op implementations of all the methods 
	 * so you can just override the ones you actually need.
	 */
	public abstract static class SimpleOnLifeCycleChangedListener implements OnLifeCycleChangedListener {

		@Override
		public void onActivityCreate(Bundle savedInstanceState, AbsPocketActivity activity) {}

		@Override
		public void onActivityRestart(AbsPocketActivity activity) {}

		@Override
		public void onActivityStart(AbsPocketActivity activity) {}

		@Override
		public void onActivityResume(AbsPocketActivity activity) {}

		@Override
		public void onActivityPause(AbsPocketActivity activity) {}
		
		@Override
		public void onActivityStop(AbsPocketActivity activity) {}

		@Override
		public void onActivityDestroy(AbsPocketActivity activity) {}

        @Override
        public void onActivityLowMemory(AbsPocketActivity activity) {}

		@Override
		public void onActivityResult(AbsPocketActivity activity, int requestCode, int resultCode, Intent data) {}

		@Override
		public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {}
	}

	/**
	 * A {@link AbsPocketFragment} has been shown overlaying the activity so that the fragment is now the
	 * main focus of the user.
	 * @see #onRegainedFocus()
	 */
	public void onLostFocus() {}
	
	/**
	 * A {@link AbsPocketFragment} that was covering this activity is gone. This activity is once again
	 * the user's main focus.
	 * @see #onLostFocus()
	 */
	public void onRegainedFocus() {}

	@Override
	public ActionContext getActionContext() {
		ActionContext.Builder builder = new ActionContext.Builder();

		//cxt_theme
		switch (getThemeInt()) {
			case Theme.LIGHT:
				builder.cxt_theme(AppTheme.LIGHT);
				break;
			case Theme.DARK:
				builder.cxt_theme(AppTheme.DARK);
				break;
			default:
				builder.cxt_theme(AppTheme.UNKNOWN);
				break;
		}

		// cxt_view
		CxtView view = null;
		ArrayList<Fragment> frags = getPocketFragmentManager().getVisibleFragments();
		for (Fragment frag : frags) {
			if (frag instanceof AbsPocketFragment) {
				view = ((AbsPocketFragment) frag).getActionViewName();
				if (view != null) {
					break;
				}
			}
		}
		if (view == null) {
			view = getActionViewName();
		}
		if (view != null) {
			builder.cxt_view(view);
		}

		return builder.build();
	}

	@Override
	public ActionContext getActionContextFor(View view) {
		return mActionContext.getActionContextFor(view);
	}

	@Override
	public void bindViewContext(View view, Contextual context) {
		mActionContext.bindViewContext(view, context);
	}
	
	/**
	 * Does this activity want to show the Listen UI.
	 * By default it is a minimized player at the bottom which can be expanded to a fullscreen view.
	 * Return false not to show Listen UI in this activity.
	 */
	public boolean isListenUiEnabled() {
		return true;
	}
	
	/**
	 * Expand the Listen UI if it is already visible or mark it to automatically expand when it becomes visible.
	 * If your intention is for the Listen UI to expand as soon as possible make sure Listen is started
	 * and start it if it isn't.
	 * <p>
	 * This won't start Listen.
	 */
	public void expandListenUi() {
		getRoot().expandListen();
	}
	
	public Observable<ListenView.State> getListenViewStates() {
		return getRoot().getListenViewStates();
	}

	/**
	 * Default implementation to display a snackbar on this Activity.  Simply calls show.
	 *
	 * @param bar the snackbar
	 */
	public void showSnackbar(PktSnackbar bar) {
		PktSnackbar.setAnchor(this, bar, null);
		bar.show();
	}
}
