package com.pocket.sdk.build;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.App;
import com.pocket.app.AppMode;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.util.android.AndroidLogger;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.IntentUtils;
import com.pocket.util.java.Logs;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;

/**
 *
 * Build specific settings and information. Most of these are built upon the BuildConfig that Android creates from our build gradle.
 */
@Singleton
public class AppVersion {
	
	// Keys hard coded at build time when making store specific builds.
	public static final String STORE_KEY_PLAY = "play";
    public static final String STORE_KEY_AMAZON = "amazon";

	// Name to report to Pocket for each store type.
	private static final String STORE_NAME_PLAY = "Google";
    private static final String STORE_NAME_AMAZON = "Amazon";
    private static final String STORE_NAME_WANDOUJIA = "Wandoujia";
    private static final String STORE_NAME_NOKIA = "Nokia";
    private static final String STORE_NAME_YANDEX = "Yandex";
    private static final String STORE_NAME_UNKNOWN = "Unknown";
    // Some additional store names possible via the auto detect
	private static final String STORE_NAME_1MOBILE = "1Mobile";
    private static final String STORE_NAME_SAMSUNG = "Samsung";

	// A packagename to look for to guess which store this apk might have come from.
	private static final String STORE_PNAME_PLAY = "com.android.vending";
	private static final String STORE_PNAME_AMAZON = "com.amazon.venezia";
	private static final String STORE_PNAME_WANDOUJIA = "com.wandoujia.phoenix2";
	private static final String STORE_PNAME_NOKIA = "com.nokia.nstore";
	private static final String STORE_PNAME_YANDEX = "com.yandex.store";
	private static final String STORE_PNAME_1MOBILE = "me.onemobile.android";
	private static final String STORE_PNAME_SAMSUNG = "com.sec.android.app.samsungapps";
	
	/**
	 * One of values like {@link #STORE_KEY_PLAY}. Set by ANT to hard code which store this build is for.
	 */
	private final String mBuildConfigKey;
	/**
	 * {@link #mBuildConfigKey} translated to a name to pass to Pocket. One of values like {@link #STORE_NAME_PLAY}.
	 */
	private final String mHardCodedStoreName;
	private final AppMode mMode;
	private final boolean mNeedsOldAmazonKeys;
	private final Lazy<ErrorHandler> errorHandler;
	/**
	 * Like {@link #mHardCodedStoreName} but guessed based on what app stores are available on this device at runtime.
	 */
	private String mGuessedStoreName;

	@Inject
	public AppVersion(Lazy<ErrorHandler> errorHandler, AppPrefs appPrefs) {
		this.errorHandler = errorHandler;
		mBuildConfigKey = BuildConfig.MARKET_KEY;
		mHardCodedStoreName = getHardCodedStoreNameForKey(mBuildConfigKey);
		mNeedsOldAmazonKeys = appPrefs.USER_NEEDS_OLD_AMAZON_KEYS.get();
		
		if (BuildConfig.DEBUG) {
			mMode = AppMode.DEV;
			Logs.logger(new AndroidLogger());
		} else if (BuildConfig.I_B) {
			mMode = AppMode.TEAM_ALPHA;
		} else if (!BuildConfig.DEBUG) {
			mMode = AppMode.PRODUCTION;
		} else {
			// Unknown type. Likely a bad build or someone trying to tinker
			mMode = null; // This will cause errors later. That is ok.
		}
	}
	
	public String getProductName() {
		return BuildConfig.UA_PM;
	}

	/**
	 * @return the API KEY/Consumer key used when connecting to the Pocket API.
	 */
	public String getConsumerKey() {
		if (mNeedsOldAmazonKeys) {
			// Old Amazon Keys
			if (FormFactor.isTablet()) {
				return BuildConfig.API_KEY_AMAZON_TABLET;
			} else {
				return BuildConfig.API_KEY_AMAZON_PHONE;
			}
			
		} else {
			// Normal
			if (FormFactor.isTablet()) {
				return BuildConfig.API_KEY_TABLET;
			} else {
				return BuildConfig.API_KEY_PHONE;
			}
		}
	}

	/**
	 * @return the API ID used when connecting to the Pocket API.
	 */
	public int getApiId() {
		return Integer.parseInt(getConsumerKey().substring(0, 4));
	}

	/**
	 * Return a unique name for this build type to distinguish it from others. Typically this
	 * would just be the market config key.
	 */
	public String getConfigName() {
		return mBuildConfigKey;
	}

	@Deprecated
	private PackageInfo getPackageInfo() {
		PackageManager pm = App.getContext().getPackageManager();
		
		try {
			return pm.getPackageInfo(App.getContext().getPackageName(), 0);
		} catch (NameNotFoundException e) {
			errorHandler.get().reportError(e);
			return null;
		}
	}

	private PackageInfo getPackageInfo(Context context) {
		PackageManager pm = context.getPackageManager();

		try {
			return pm.getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			errorHandler.get().reportError(e);
			return null;
		}
	}
	
	/**
	 * A user facing version name/number such as 3.0.1
	 * @return
	 */
	@Deprecated
	public String getVersionName() {
		PackageInfo info = getPackageInfo();
		return info != null ? info.versionName : "";
	}

	public String getVersionName(Context context) {
		PackageInfo info = getPackageInfo(context);
		return info != null ? info.versionName : "";
	}
	
	/**
	 * The incremental version code such as 42
	 * @return
	 */
	@Deprecated
	public int getVersionCode() {
		PackageInfo info = getPackageInfo();
		return info != null ? info.versionCode : -1;
	}

	public int getVersionCode(Context context) {
		PackageInfo info = getPackageInfo(context);
		return info != null ? info.versionCode : -1;
	}

	/**
	 * One of values like {@link #STORE_KEY_PLAY}. For reporting purposes to Pocket's server.
	 * 
	 * @param useHardCoded true to use hard coded build provided value (accurate) or false to use the best guess.
	 * @return
	 */
	@Deprecated
	public final String getStoreName(boolean useHardCoded) {
		if (useHardCoded) {
			return mHardCodedStoreName;
			
		} else {
			if (mGuessedStoreName == null) {
				mGuessedStoreName = findStoreName(App.getContext());
			}
			return mGuessedStoreName;
		}
	}

	public final String getStoreName(boolean useHardCoded, Context context) {
		if (useHardCoded) {
			return mHardCodedStoreName;

		} else {
			if (mGuessedStoreName == null) {
				mGuessedStoreName = findStoreName(context);
			}
			return mGuessedStoreName;
		}
	}
	
	/**
	 * Guesses which store this apk came from (or is managed by) based on what stores are
	 * available on this device.
	 *  
	 * @return
	 */
	private String findStoreName(Context context) {
		ArrayList<String> stores = new ArrayList<String>();
		
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_YANDEX)) {
			stores.add(STORE_NAME_YANDEX);
		}
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_WANDOUJIA)) {
			stores.add(STORE_NAME_WANDOUJIA);
		}
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_AMAZON) || FormFactor.isKindleFire(false)) {
			stores.add(STORE_NAME_AMAZON);
		}	
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_1MOBILE)) {
			stores.add(STORE_NAME_1MOBILE);
		}	
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_NOKIA)) {
			stores.add(STORE_NAME_NOKIA);
		}
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_SAMSUNG)) {
			stores.add(STORE_NAME_SAMSUNG);
		}
		if (IntentUtils.isAppInstalled(context, STORE_PNAME_PLAY)) {
			stores.add(STORE_NAME_PLAY);
		}
		if (stores.isEmpty()) {
			return STORE_NAME_UNKNOWN;
		}
		
		boolean pickOne = true;
		if (pickOne) {
			return stores.get(0);
		} else {
			String value = "";
			for (String store : stores) {
				if (value.length() > 0) {
					value += "|";
				}
				value += store;
			}
			return value;
		}
	}
	
	private static String getHardCodedStoreNameForKey(String key) {
		if (key.equals(STORE_KEY_PLAY)) {
			return STORE_NAME_PLAY;
		} else {
			return STORE_NAME_UNKNOWN; // Could be a beta or an unexpected name
		}
	}

    public boolean isAmazonBuild() {
        return STORE_KEY_AMAZON.equals(mBuildConfigKey);
    }
	
	public AppMode mode() {
		return mMode;
	}
}
