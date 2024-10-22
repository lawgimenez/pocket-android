package com.pocket.sdk.preferences;

import android.content.Context;
import android.content.pm.ActivityInfo;

import com.pocket.app.settings.beta.BetaConfigFragment;
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.util.android.FormFactor;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.FloatPreference;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.LongPreference;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Observable;

/**
 * The app's {@link com.pocket.util.prefs.Preference}s.
 * <p>
 * Historically these were all in one large static class with all of the preferences as global static final fields.
 * That was refactored to have these fields be instance fields to help aid in making the app more testable in unit tests.
 * <p>
 * Ideally instead of adding new preferences here, we start passing a {@link Preferences} instance to components
 * and components create and hold the preferences they need and if another part of the app needs that preference
 * they get it through that component.  However, it is possible there will be some preferences that are "global"
 * to the app and may still want to live here. We'll have to see how this all plays out.
 *
 * @deprecated Avoid using if possible. Instead decide what component should "own" a preference and have it accept a {@link com.pocket.util.prefs.Preferences} in its constructor, create the preference and if other components need access to it, have that component expose some kind of API instead of other components working with the preference directly.
 */
@Deprecated
@Singleton
public class AppPrefs {
	
	private final Preferences prefs;
	
	// OPT make all of these obfuscated, since the string names don't really matter and the shared pref file is visible
	// this could give a hacker clues.
	
	/** There might be some older versions still using these keys and if so, changing them would log them out.  should add some logging to see if we can remove this or not. */
	public final BooleanPreference USER_NEEDS_OLD_AMAZON_KEYS;
	
	// User Set Options/Preferences
	
	public final BooleanPreference ALWAYS_OPEN_ORIGINAL;
	public final BooleanPreference DOWNLOAD_TEXT;
	public final LongPreference DOWNLOAD_SUSPENDED;
	public final BooleanPreference DOWNLOAD_ONLY_WIFI;
	public final BooleanPreference USE_MOBILE_AGENT;
	/** Managed by {@link com.pocket.app.settings.UserAgent} */
	public final StringPreference USER_AGENT_MOBILE;
	/** Managed by {@link com.pocket.app.settings.UserAgent} */
	public final StringPreference USER_AGENT_DESKTOP;

	/** We don't offer this as an option any more, but users that upgraded from old versions may be grandfathered in. After removing old upgrade paths, there is no way to turn this on, it is only available for users that had it enabled a while back. */
	public final BooleanPreference CACHE_STORAGE_TYPE_EMULATED_VISIBLE;
	public final LongPreference CACHE_SIZE_USER_LIMIT_TEMP;
	public final IntPreference CACHE_SORT_TEMP;
	
	// Android State
	public final IntPreference PREVIOUS_APP_VERSION;
	public final IntPreference ORIENTATION;
	public final LongPreference WHEN_LAST_ACTIVITY_PAUSED;
	public final LongPreference WHEN_LAST_SESSION_ENDED;
	public final BooleanPreference READ_EXTERNAL_STORAGE_REQUESTED;
	public final BooleanPreference ALLOW_LAUNCH_FIX;
	
	// Reader State
	public final BooleanPreference START_IN_ARTICLE_VIEW;
	
	public final FloatPreference ARTICLE_TTS_SPEED;
	public final FloatPreference ARTICLE_TTS_PITCH;
	public final StringPreference ARTICLE_TTS_COUNTRY;
	public final StringPreference ARTICLE_TTS_LANGUAGE;
	public final StringPreference ARTICLE_TTS_VARIANT;
	public final StringPreference ARTICLE_TTS_VOICE;
	public final BooleanPreference ARTICLE_TTS_WARNED_NETWORK_VOICES;
	public final BooleanPreference ARTICLE_TTS_AUTO_PLAY;

	public final IntPreference LISTEN_MAX_WORD_COUNT;
	public final IntPreference LISTEN_MIN_WORD_COUNT;
	public final BooleanPreference LISTEN_USE_STREAMING_VOICE;
	public final BooleanPreference LISTEN_AUTO_ARCHIVE;
	public final BooleanPreference SHOW_LISTEN_DATA_ALERT;
	public final FloatPreference LISTEN_LOWEST_REPORTED_FAILING_SPEED;
	
	public final BooleanPreference LISTEN_HAS_SHOWN_INTRO_A;
	public final BooleanPreference LISTEN_HAS_SHOWN_INTRO_B;
	public final LongPreference LISTEN_LAST_SHOWN_INTRO_TIME;

	public final BooleanPreference READER_AUTO_FULLSCREEN;
	public final StringPreference TTS_ENGINE;
	public final StringPreference TTS_ENGINES_LAST_KNOWN;
	
	// Settings Keys
	
	public final StringPreference OS_BUILD_KEY;
	
	/**
	 * The {@link AppVersion} config name at the time of first run.
	 */
	public final StringPreference ORIGINAL_BUILD_VERSION;
	/**
	 * Have we successfully performed the sync that upgrades to the extended attribution verison of the app? Only used for upgrade migration for the tweet attribution update.
	 */
	public final LongPreference SESSION_ID;
	public final BooleanPreference ROTATION_LOCK;
	public final IntPreference SCREEN_WIDTH_SHORT;
	public final IntPreference SCREEN_WIDTH_LONG;
	
	public final Preferences GSF_EVENT_COUNTER_PREFIX;

	public final StringPreference PKT_CACHE;
	
	public final BooleanPreference CONTINUE_READING_ENABLED;
	public final StringPreference CONTINUE_READING_SHOWN_ID;
	
	// Below are dev only options, these are managed by App-Dev's BetaConfigFragment. These should never be used in a production build.
	
	public final BooleanPreference DEVCONFIG_SNACKBAR_ALWAYS_SHOW_URL_CR;
	public final IntPreference DEVCONFIG_PREMIUM;
	public final BooleanPreference DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_A;
	public final BooleanPreference DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_B;

	@Inject
	public AppPrefs(Preferences prefs, @ApplicationContext Context context) {
		this.prefs = prefs;

		USER_NEEDS_OLD_AMAZON_KEYS = prefs.forUser("oldamzky", false);

		// the default value for alwaysOpenOriginal is the inverse of the old value autoOpenBestView
		// If you read this comment in the year 2024 or later, you can just
		// make the default value false and add autoOpenBestView to the Forgetter
		ALWAYS_OPEN_ORIGINAL = prefs.forUser("alwaysOpenOriginal", !prefs.forUser("autoOpenBestView", true).get());

		DOWNLOAD_TEXT = prefs.forUser("downloadText", true);
		DOWNLOAD_SUSPENDED = prefs.forUser("downloadSuspend", 0L);
		DOWNLOAD_ONLY_WIFI = prefs.forUser("autoOnlyWifi", true);
		USE_MOBILE_AGENT = prefs.forUser("userAgentMobile", false);
		USER_AGENT_MOBILE = prefs.forApp("uamobile", (String) null);
		USER_AGENT_DESKTOP = prefs.forApp("uadesktop", (String) null);

		CACHE_STORAGE_TYPE_EMULATED_VISIBLE = prefs.forUser("storagetype_emu", false);
		CACHE_SIZE_USER_LIMIT_TEMP = prefs.forUser("cacheLimitTemp", 0L);
		CACHE_SORT_TEMP = prefs.forUser("cacheSortTemp", Assets.CachePriority.NEWEST_FIRST);
		
		PREVIOUS_APP_VERSION = prefs.forApp("previousAppVersion", 0);
		ORIENTATION = prefs.forApp("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		WHEN_LAST_ACTIVITY_PAUSED = prefs.forApp("whenLastPaused", 0L);
		WHEN_LAST_SESSION_ENDED = prefs.forApp("whenLastSessionEnded", 0L);
		READ_EXTERNAL_STORAGE_REQUESTED = prefs.forApp("readExternalStorageRequested", false);
		ALLOW_LAUNCH_FIX = prefs.forApp("allowLaunchFix", true);
		
		START_IN_ARTICLE_VIEW = prefs.forUser("readerStartInArticleView", true);
		
		ARTICLE_TTS_SPEED = prefs.forUser("articleTTSSpeed", 1f);
		ARTICLE_TTS_PITCH = prefs.forUser("articleTTSPitch", 1f);
		ARTICLE_TTS_COUNTRY = prefs.forUser("articleTTSCountry", (String) null);
		ARTICLE_TTS_LANGUAGE = prefs.forUser("articleTTSLanguage", (String) null);
		ARTICLE_TTS_VARIANT = prefs.forUser("articleTTSVariant", (String) null);
		ARTICLE_TTS_VOICE = prefs.forUser("articleTTSVoice", (String) null);
		ARTICLE_TTS_WARNED_NETWORK_VOICES = prefs.forUser("articleTTSWarnedNetVoice", false);
		ARTICLE_TTS_AUTO_PLAY = prefs.forUser("articleTTSautoPlay", true);

		LISTEN_MAX_WORD_COUNT = prefs.forUser("lstn_maxwc", 24000);
		LISTEN_MIN_WORD_COUNT = prefs.forUser("lstn_minwc", 0);
		LISTEN_USE_STREAMING_VOICE = prefs.forUser("lstn_strmvc", true);
		LISTEN_AUTO_ARCHIVE = prefs.forUser("lstn_autoarch", false);
		SHOW_LISTEN_DATA_ALERT = prefs.forUser("lstn_dtalrt", true);
		LISTEN_LOWEST_REPORTED_FAILING_SPEED = prefs.forApp("lstn_failspd", Float.MAX_VALUE);
		
		LISTEN_HAS_SHOWN_INTRO_A = prefs.forUser("lstn_dscvr_a", false);
		LISTEN_HAS_SHOWN_INTRO_B = prefs.forUser("lstn_dscvr_b", false);
		LISTEN_LAST_SHOWN_INTRO_TIME = prefs.forUser("lstn_dscvr_tmstmp", 0L);

		READER_AUTO_FULLSCREEN = prefs.forUser("autoFullscreen", true);
		TTS_ENGINE = prefs.forUser("ttsEngine", (String) null);
		TTS_ENGINES_LAST_KNOWN = prefs.forApp("ttsEnginesKnown", (String) null);
		
		
		OS_BUILD_KEY = prefs.forApp("osBuildKey", (String) null);

		
		ORIGINAL_BUILD_VERSION = prefs.forApp("bv", (String) null);
		
		
		SESSION_ID = prefs.forApp("sid", 0L);
		ROTATION_LOCK = prefs.forUser("enableRotationLock", !FormFactor.isTablet(context));
		SCREEN_WIDTH_SHORT = prefs.forApp("screenWidthShort", 0);
		SCREEN_WIDTH_LONG = prefs.forApp("screenWidthLong", 0);
		
		GSF_EVENT_COUNTER_PREFIX = prefs.group("gsfevc_");

		PKT_CACHE = prefs.forApp("pktcache", (String) null);
		
		CONTINUE_READING_ENABLED = prefs.forUser("continueReadingEnabled", true);
		CONTINUE_READING_SHOWN_ID = prefs.forUser("continueReadingShownId", (String) null);
		
		DEVCONFIG_SNACKBAR_ALWAYS_SHOW_URL_CR = prefs.forApp("dcfig_always_show_url_cr", false);
		DEVCONFIG_PREMIUM = prefs.forUser("dcfig_ps", BetaConfigFragment.DEVCONFIG_PREMIUM_ACTUAL);
		DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_A = prefs.forApp("dcfig_lstn_dscvr_a", false);
		DEVCONFIG_LISTEN_DISCOVERABILITY_FORCE_B = prefs.forApp("dcfig_lstn_dscvr_b", false);
	}
	
	
	/**
	 * Retrieves a preferences current value and removes it.
	 * Useful for upgrade paths that need to reference a preference that is no longer going to be used to migrate it.
	 */
	public boolean deprecateUserBoolean(String key, boolean defaultValue) {
		boolean value = prefs.forUser(key, defaultValue).get();
		prefs.remove(key);
		return value;
	}
	
	/**
	 * Retrieves a preferences current value and removes it.
	 * Useful for upgrade paths that need to reference a preference that is no longer going to be used to migrate it.
	 */
	public String deprecateUserString(String key, String defaultValue) {
		String value = prefs.forUser(key, defaultValue).get();
		prefs.remove(key);
		return value;
	}

	public Observable<String> changes() {
		return prefs.changes();
	}
}
