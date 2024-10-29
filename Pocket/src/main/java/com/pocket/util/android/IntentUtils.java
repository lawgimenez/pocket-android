package com.pocket.util.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.util.java.StringUtils2;

import java.util.ArrayList;
import java.util.List;

public abstract class IntentUtils {

	/**
	 * Convenience method for checking if an Intent that starts an Activity will actually
	 * find an Activity to open or not.
	 *  
	 * @param context
	 * @param intent
	 * @return
	 */
	public static boolean isActivityIntentAvailable(Context context, Intent intent) {
	    final PackageManager packageManager = context.getPackageManager();
	    List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
	    return list != null && list.size() > 0;
	}
	
	/**
	 * Returns all apps that can accept this intent. If there is a preferred one it will be the first in the list.
	 * 
	 * @param context
	 * @return Always returns a list, never null, though it may be empty.
	 */
	public static List<ResolveInfo> getMatchingApps(Intent intent, Context context) {
		final PackageManager pm = context.getPackageManager();
		final List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.MATCH_ALL);
		
		if (apps.size() > 1) {
			ResolveInfo preferred = null;
			List<IntentFilter> filters = new ArrayList<IntentFilter>();
		    List<ComponentName> activities = new ArrayList<ComponentName>();
		    for (ResolveInfo app : apps) { 
		    	filters.clear();
		    	activities.clear();
		    	pm.getPreferredActivities(filters, activities, app.activityInfo.packageName);
		    	if (activities.size() > 0) {
		    		preferred = app;
		    		break;
		    	}
		    }
		    
		    if (preferred != null) {
		    	apps.remove(preferred);
		    	apps.add(0, preferred);
		    }
		}
		
		return apps;
	}
	
	/**
	 * Returns all apps that are likely a browser. If there is a preferred one it will be the first in the list.
	 * 
	 * @param context
	 * @return A list with any browsers found. Never null.
	 */
	public static List<ResolveInfo> getAllAvailableBrowsers(Context context) {
		return IntentUtils.getMatchingApps(new Intent(Intent.ACTION_VIEW, Uri.parse("http://ideashower.com")), context);
	}
	
	public static boolean isAppInstalled(Context context, String packageName) {
		PackageManager pm = context.getPackageManager();
		PackageInfo info;
		try {
			info = pm.getPackageInfo(packageName, 0);
		} catch (NameNotFoundException e) {
			info = null;
		}
		
		return info != null;
	}

    /**
     * Start an intent safely without risking crashing if there are no matching apps available.
	 * A message will be toasted in that case.
     * <p>
     * Also automatically adds the NEW_TASK flag if the context is a service.
     *
     * @param context The context to start the activity with.
     * @param intent The intent to start.
     * @param clearWhenTaskReset Whether or not to add {@link Intent#FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET}. Should be true for most cases when opening other apps. This will ensure that Pocket is still in the recents and is reopened rather than this new app.
     * @return true if started, false if the error was shown.
     */
    public static boolean safeStartActivity(Context context, Intent intent, boolean clearWhenTaskReset) {
        if (clearWhenTaskReset) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }

		if (context instanceof Service) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}

        if (isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent);
            return true;

        } else {
            Toast.makeText(context, R.string.ts_no_apps_for_intent, Toast.LENGTH_LONG)
                .show();
            return false;
        }
    }

    public static ComponentName getDefaultBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        ResolveInfo app = App.getContext().getPackageManager().resolveActivity(intent, 0);
        return app != null ? new ComponentName(app.activityInfo.packageName, app.activityInfo.name) : null;
    }

    /**
     * Attempts to open the intent with the default browser or at least a common browser, avoiding re-opening
     * with our own app.
     *
     * @param context
     * @param viewIntent This must be a VIEW intent with an http/https uri as the data.
     * @return true if it found a browser to open with and it was started, false if it couldn't find an app to view it with.
     */
    public static boolean openWithDefaultBrowser(Context context, Intent viewIntent, boolean allowResolverActivity) {
        if (!viewIntent.getAction().equals(Intent.ACTION_VIEW) || viewIntent.getDataString() == null) {
            return false;
        }

        ComponentName componentName = IntentUtils.getDefaultBrowser();
        if (componentName == null || componentName.getPackageName().equals(context.getPackageName())) {
            return false;
        }

		if (!allowResolverActivity && componentName.getClassName().equals("com.android.internal.app.ResolverActivity")) {
			// Try to pick a default browser for them
			List<ResolveInfo> browsers = getAllAvailableBrowsers(context);
			// First try known ones like Chrome
			boolean found = false;
			for (ResolveInfo info : browsers) {
				if (StringUtils2.equalsIgnoreCaseOneOf(info.activityInfo.packageName, "com.android.chrome", "com.chrome.beta")) {
					componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
					found = true;
					break;
				}
			}
			if (!found && !browsers.isEmpty()) {
				// Just use the first one... I know.. i know it is janky. but this is the best we can do
				ResolveInfo info = null;
				for (ResolveInfo browser : browsers) {
					if (browser.activityInfo.packageName.equals(App.getContext().getPackageName())) {
						continue;
					} else {
						info = browser;
						break;
					}
				}
				if (info != null) {
					componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
					found = true;
				}
			}
			if (!found) {
				// We couldn't do it.
				return false;
			}
		}

        viewIntent = new Intent(viewIntent).setComponent(componentName);

        if (isActivityIntentAvailable(context, viewIntent)) {
            context.startActivity(viewIntent);
            return true;
        } else {
            return false;
        }
    }

	public static Intent getGoogleTranslateIntent() {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_PROCESS_TEXT);
		intent.setTypeAndNormalize("text/plain");
		intent.setComponent(new ComponentName("com.google.android.apps.translate",
				"com.google.android.apps.translate.copydrop.gm3.TapToTranslateActivity"));
		return intent;
	}

    public static boolean hasGoogleTranslate(Context context) {
        return IntentUtils2.INSTANCE.isIntentUsable(
				context,
				getGoogleTranslateIntent()
		);
    }

	public static void googleTranslate(Context context, String text) {
        if (!hasGoogleTranslate(context)) {
            // doesn't seem possible, uninstalling the app causes a configuration change,
            // which updates the menus in the Reader, but returning here just in case
            return;
        }
        Intent intent = getGoogleTranslateIntent();
        intent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(intent);
    }
}
