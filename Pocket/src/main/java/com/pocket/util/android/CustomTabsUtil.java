package com.pocket.util.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.api.value.UrlString;

@SuppressWarnings("unused") // Let's keep it around for the future, so it's easy to add custom tabs back in.
public final class CustomTabsUtil {
	
	private CustomTabsUtil() {
		throw new AssertionError("No instances.");
	}
	
	public static boolean viewUrl(Context context, UrlString url) {
		if (url == null) return false;
		
		final CustomTabsIntent intent = new CustomTabsIntent.Builder()
				.setToolbarColor(getToolbarColor(context))
				.build();
		
		if (IntentUtils.isActivityIntentAvailable(context, intent.intent)) {
			intent.launchUrl(context, Uri.parse(url.url));
			return true;
		} else {
			new AlertDialog.Builder(context)
					.setTitle(R.string.dg_browser_not_found_t)
					.setMessage(R.string.dg_browser_not_found_m)
					.setNeutralButton(R.string.ac_ok, null)
					.show();
			return false;
		}
	}
	
	private static int getToolbarColor(Context context) {
		final ColorStateList stateList = ContextCompat.getColorStateList(context, com.pocket.ui.R.color.pkt_themed_teal_2);
		if (stateList != null) {
			return stateList.getColorForState(Theme.getState(App.from(context).theme().get(context)), stateList.getDefaultColor());
			
		} else {
			return ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_bg);
		}
	}
	
	public static void warmUp(Context context) {
		final String packageName = CustomTabsClient.getPackageName(context, null);
		if (packageName == null) return;
		
		CustomTabsClient.connectAndInitialize(context, packageName);
	}
}
