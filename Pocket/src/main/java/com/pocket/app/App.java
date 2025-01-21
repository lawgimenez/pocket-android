package com.pocket.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;

import com.ideashower.readitlater.R;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.pocket.analytics.Tracker;
import com.pocket.app.build.Versioning;
import com.pocket.app.help.Help;
import com.pocket.app.list.list.ListManager;
import com.pocket.app.premium.Premium;
import com.pocket.app.premium.PremiumFonts;
import com.pocket.app.reader.internal.article.DisplaySettingsManager;
import com.pocket.app.session.AppSession;
import com.pocket.app.session.ItemSessions;
import com.pocket.app.settings.SystemDarkTheme;
import com.pocket.app.settings.Theme;
import com.pocket.app.settings.UserAgent;
import com.pocket.app.settings.rotation.RotationLock;
import com.pocket.app.undobar.UndoBar;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.api.PocketServer;
import com.pocket.sdk.api.UserMessaging;
import com.pocket.sdk.api.generated.enums.AppTheme;
import com.pocket.sdk.api.generated.enums.DeviceOrientation;
import com.pocket.sdk.api.generated.enums.OnlineStatus;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.build.AlphaBuild;
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.dev.SentryManager;
import com.pocket.sdk.help.Troubleshooter;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.image.ImageCache;
import com.pocket.sdk.notification.SystemNotifications;
import com.pocket.sdk.notification.push.Push;
import com.pocket.sdk.offline.OfflineDownloading;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.sdk.util.ErrorReport;
import com.pocket.sdk.util.service.BackgroundSync;
import com.pocket.sdk.util.wakelock.WakeLockManager;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.sdk2.braze.BrazeManager;
import com.pocket.ui.view.notification.PktSnackbar;
import com.pocket.util.android.Clipboard;
import com.pocket.util.android.IntentUtils;
import com.pocket.util.java.Logs;
import com.pocket.util.prefs.Preferences;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@SuppressWarnings("unused")
@HiltAndroidApp
public class App extends Application implements Contextual, PocketApp {

	@Inject PocketSingleton pocketSingleton;
	@Inject PocketServer pocketServer;
	@Inject AppSession appSession;
	@Inject ImageCache imageCache;
	@Inject PocketCache pocketCache;
	@Inject UserManager userManager;
	@Inject WakeLockManager wakeLockManager;
	@Inject BackgroundSync backgroundSync;
	@Inject Troubleshooter troubleshooter;
	@Inject Listen listen;
	@Inject RotationLock rotationLock;
	@Inject DisplaySettingsManager displaySettingsManager;
	@Inject PremiumFonts premiumFonts;
	@Inject AppThreads appThreads;
	@Inject UserAgent userAgent;
	@Inject ItemSessions itemSessions;
	@Inject ErrorHandler errorHandler;
	@Inject ActivityMonitor activityMonitor;
	@Inject SystemDarkTheme systemDarkTheme;
	@Inject Theme theme;
	@Inject SystemNotifications systemNotifications;
	@Inject Push push;
	@Inject Clipboard clipboard;
	@Inject OfflineDownloading offlineDownloading;
	@Inject Assets assets;
	@Inject AppSync appSync;
	@Inject AppLifecycleEventDispatcher appLifecycleEventDispatcher;
	@Inject AppVersion appVersion;
	@Inject HttpClientDelegate httpClientDelegate;
	@Inject AppPrefs appPrefs;
	@Inject UndoBar undoBar;
	@Inject ReviewPrompt reviewPrompt;
	@Inject Premium premium;
	@Inject UserMessaging userMessaging;
	@Inject Device device;
	@Inject Versioning versioning;
	@Inject AppOpen appOpen;
	@Inject Tracker tracker;
	@Inject ListManager listManager;
	@Inject SaveExtension saveExtension;
	@Inject Preferences preferences;
	@Inject Pocket pocket;
	@Inject BrazeManager brazeManager;
	@Inject SentryManager sentryManager;
	@Inject AlphaBuild alpha;
	@Inject CustomTabs customTabs;

	// App State
    private static App sContext;

	/**
	 * The currently open Activity, if any.
	 */
	private static AbsPocketActivity sActivityContext;
	
	private static boolean sIsUserPresent = false;

	private static Set<OnUserPresenceChangedListener> sOnUserPresenceChangedListeners = new HashSet<>();

	private static AbsPocketActivity.OnLifeCycleChangedListener activityListener = new AbsPocketActivity.SimpleOnLifeCycleChangedListener() {
		@Override
		public void onActivityResult(AbsPocketActivity activity, int requestCode, int resultCode, Intent data) {
			getApp().dispatcher().dispatch((component) -> component.onActivityResult(activity, requestCode, resultCode, data));
		}
	};
	
	/** @deprecated Avoid using if possible, instead have your class receive a context (or whatever it is using a context to retrieve) as a dependency */
	@Deprecated
	public static Context getContext(){
		return sContext;
	}
	
	@Override
	public void onCreate() {
		sContext = this;
		AndroidThreeTen.init(this);
		super.onCreate();

		Logs.mode(appVersion.mode());

		brazeManager.setup();
		alpha.setup();

		Forgetter.INSTANCE.forget(pocket, preferences);

		// Pirate Check
		if (!mode().isDevBuild() &&
				(0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE))) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}

		if (mode().isDevBuild() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectUnsafeIntentLaunch()
					.penaltyLog()
					.build()
			);
		}
		
		// Pirate Check
		if (!getPackageName().startsWith("com.ideashower.readitlater") && !getPackageName().startsWith("com.pocket")) {
			android.os.Process.killProcess(android.os.Process.myPid());
		}

		// PktSnackbar errors submit support info emails on long press
		PktSnackbar.init((context, message, t) ->
				Help.requestHelp(context,
						Help.getSupportEmail(),
						"Help with Pocket",
						message,
						true, true,
						new ErrorReport(t, message),
						null));
						
		// Note: Recompile and Reverse Engineer Detection is currently hidden in Assets.start():
    }

	@Override public Pocket pocket() { return pocketSingleton.getInstance(); }
	@Override public PocketSingleton pocketSingleton() { return pocketSingleton; }
	@Override public PocketServer pktserver() { return pocketServer; }
	@Override public AppSession session() { return appSession; }
	@Override public ImageCache imageCache() { return imageCache; }
	@Override public PocketCache pktcache() { return pocketCache; }
	@Override public UserManager user() { return userManager; }
	@Override public WakeLockManager wakelocks() { return wakeLockManager; }
	@Override public BackgroundSync backgroundSync() { return backgroundSync; }
	@Override public AppMode mode() { return appVersion.mode(); }
	@Override public Troubleshooter troubleshooter() { return troubleshooter; }
	@Override public Listen listen() { return listen; }
	@Override public RotationLock rotationLock() { return rotationLock; }
	@Override public DisplaySettingsManager displaySettings() { return displaySettingsManager; }
	@Override public PremiumFonts premiumFonts() { return premiumFonts; }
	@Override public AppThreads threads() { return appThreads; }
	@Override public UserAgent userAgent() { return userAgent; }
	@Override public ItemSessions itemSessions() { return itemSessions; }
	@Override public ErrorHandler errorReporter() { return errorHandler; }
	@Override public ActivityMonitor activities() { return activityMonitor; }
	@Override public SystemDarkTheme systemDarkTheme() { return systemDarkTheme; }
	@Override public Theme theme() { return theme; }
	@Override public SystemNotifications notifications() { return systemNotifications; }
	@Override public Push push() { return push; }
	@Override public Clipboard clipboard() { return clipboard; }
	@Override public OfflineDownloading offline() { return offlineDownloading; }
	@Override public Assets assets() { return assets; }
	@Override public AppSync appSync() { return appSync; }
	@Override public AppLifecycleEventDispatcher dispatcher() { return appLifecycleEventDispatcher;}
	@Override public AppVersion build() { return appVersion;}
	@Override public HttpClientDelegate http() { return httpClientDelegate;}
	@Override public AppPrefs prefs() { return appPrefs;}
	@Override public UndoBar undo() { return undoBar;}
	@Override public ReviewPrompt reviewPrompt() { return reviewPrompt;}
	@Override public Premium premium() { return premium;}
	@Override public UserMessaging messaging() { return userMessaging;}
	@Override public Device device() { return device; }
	@Override public Versioning versioning() { return versioning; }
	@Override public AppOpen appOpen() { return appOpen; }
	@Override public Tracker tracker() { return tracker; }
	@Override public ListManager listManager() { return listManager; }
	@Override public SaveExtension saveExtension() { return saveExtension; }

	/**
	 * Call when an Activity resumes or pauses.
	 * 
	 * @param activity The activity that resumed, or null if pausing.
	 */
	public static void onActivityChange(AbsPocketActivity activity){
		AbsPocketActivity previous = sActivityContext;
		if (previous != null) {
			previous.removeOnLifeCycleChangeListener(activityListener);
		}
		sActivityContext = activity;
		if (activity != null) {
			// onResume
			activity.addOnLifeCycleChangedListener(activityListener);
			
			setUserPresent(true, activity);
			
		} else {
			// onPause
			getApp().prefs().WHEN_LAST_ACTIVITY_PAUSED.set(System.currentTimeMillis());
		}
		
		if (activity != null) {
			getApp().dispatcher().dispatch((component) -> component.onActivityResumed(activity));
		} else {
			getApp().dispatcher().dispatch((component) -> component.onActivityPaused(previous));
		}
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		dispatcher().dispatch((component) -> component.onConfigurationChanged(newConfig));
	}

	public static App from(Context context) {
		return (App) context.getApplicationContext();
	}
	
	/**
	 * Convenience method for getting a string resource.
	 * 
	 * @deprecated access from a context you get in a constructor or parameter, avoid static access
	 */
	@Deprecated
	public static String getStringResource(int id) {
		if (id == 0) {
			return null;
		}
		
		return sContext.getString(id);
	}

	/**
	 * Returns the Application context
	 * REVIEW there are duplicates of this functionality.
	 * @return
	 * @deprecated Avoid static access of {@link App} whenever possible. Instead pass component dependencies into classes. If static access is unavoidable, try to use {@link #from(Context)} instead. If in a screen, can use {@link AbsPocketActivity#app()} or {@link AbsPocketFragment#app()}
	 */
	@Deprecated
	public static App getApp(){
		return sContext;
	}
	
	/**
	 * Returns the currently focused Pocket Activity's context if there is one.
	 * @return
	 */
	public static AbsPocketActivity getActivityContext() { // REVIEW move to ActivityMonitor
		return sActivityContext;
	}
	
	/**
	 * A priacy check to see if the app is Debuggable when it shouldn't be. If so it kills the app.
	 */
	public static void checkIfDebuggable() {
		if (getApp().mode().isDevBuild()) return;
		
		if(0 != ( sContext.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE )){
			if(sActivityContext != null)
				sActivityContext.finishAllActivities(true);
			else
				android.os.Process.killProcess(android.os.Process.myPid());
		}
	}
	
	/**
	 * Flag whether or not there is a Pocket Activity currently on screen and in focus of the user.
	 */
	public static void setUserPresent(boolean present, AbsPocketActivity activity) {
		if (sIsUserPresent != present) {
			
			Interaction i = Interaction.on(activity).merge(activity.getLaunchUiContext());
			Pocket pocket = activity.pocket();
			if (present) {
				pocket.sync(null, pocket.spec().actions().opened_app().context(i.context).time(i.time).build());
			} else {
				pocket.sync(null, pocket.spec().actions().closed_app().context(i.context).time(i.time).build());
			}
			
			sIsUserPresent = present;
			
			for (OnUserPresenceChangedListener listener : sOnUserPresenceChangedListeners) {
				listener.onUserPresenceChanged(present);
			}
			if (present) {
				getApp().dispatcher().dispatch((component) -> component.onUserPresent());
			} else {
				getApp().dispatcher().dispatch((component) -> component.onUserGone(activity));
			}
		}
	}
	
	public static void addOnUserPresenceChangedListener(OnUserPresenceChangedListener listener) {
		sOnUserPresenceChangedListeners.add(listener);
	}

	public static void removeOnUserPresenceChangedListener(OnUserPresenceChangedListener listener) {
		sOnUserPresenceChangedListeners.remove(listener);
	}
	
	public interface OnUserPresenceChangedListener {
		void onUserPresenceChanged(boolean isInApp);
	}

    /**
	 * Convenience for {@link #viewUrl(Context, String, boolean)} with showDialogOnFail = true; 
	 *
	 * @param context context to start the other app with
	 * @param url url to open
	 * @return true if the browser activity was started, false if no app available to handle.
	 */
	public static boolean viewUrl(Context context, String url) {
		return viewUrl(context, url, true);
	}
	
	/**
	 * Convenience method for opening a url in another app.
	 * 
	 * @param context context to start the other app with
	 * @param url url to open
	 * @param showDialogOnFail true to show a standard error dialog if no browser app available, false to fail silently.
	 * @return true if the browser activity was started, false if no app available to handle.
	 */
	public static boolean viewUrl(Context context, String url, boolean showDialogOnFail) {
		// TODO have this skip the PocketUrlRedirectActivity filter
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		if (!(context instanceof Activity)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		if (IntentUtils.isActivityIntentAvailable(context, intent)) {
			context.startActivity(intent);
			return true;
			
		} else if (showDialogOnFail) {
			new AlertDialog.Builder(context)
				.setTitle(R.string.dg_browser_not_found_t)
				.setMessage(R.string.dg_browser_not_found_m)
				.setNeutralButton(R.string.ac_ok, null)
					.show();
			return false;
		} else {
			return false;
		}
	}
	
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		dispatcher().dispatch((component) -> component.onLowMemory());
	}

	/**
	 * REVIEW duplicate of {@link #getActivityContext()} ?
	 * @return
	 */
	public static boolean isUserPresent() {
		return sActivityContext != null;
	}

	@Override
	public ActionContext getActionContext() {
		ActionContext.Builder builder = new ActionContext.Builder();

		// cxt_online
		if (!http().status().isOnline()) {
			builder.cxt_online(OnlineStatus.OFFLINE);
		} else if (http().status().isWifi()) {
			builder.cxt_online(OnlineStatus.WIFI);
		} else {
			builder.cxt_online(OnlineStatus.MOBILE);
		}

		// cxt_orient
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			builder.cxt_orient(DeviceOrientation.LANDSCAPE);
		} else {
			builder.cxt_orient(DeviceOrientation.PORTRAIT);
		}

		//cxt_theme
		final Activity activity = activities().getAvailableContext();
		if (activity instanceof AbsPocketActivity) {
			switch (((AbsPocketActivity) activity).getThemeInt()) {
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
		}

		// sid
		builder.sid(String.valueOf(appSession.getSid()));

		// item sessions
		final Long itemSessionId = itemSessions().getSessionId();
		if (itemSessionId != null) builder.item_session_id(String.valueOf(itemSessionId));

		return builder.build();
	}
	
}
