package com.pocket.app;

import com.pocket.analytics.Tracker;
import com.pocket.app.build.Versioning;
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
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.help.Troubleshooter;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.image.ImageCache;
import com.pocket.sdk.notification.SystemNotifications;
import com.pocket.sdk.notification.push.Push;
import com.pocket.sdk.offline.OfflineDownloading;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.util.service.BackgroundSync;
import com.pocket.sdk.util.wakelock.WakeLockManager;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.util.android.Clipboard;

/**
 * Pocket's main, full featured, Android app
 *
 * @deprecated use dagger/hilt dependency injection instead
 */
@Deprecated
public interface PocketApp {
	
	Pocket pocket();
	PocketServer pktserver();
	AppMode mode();
	AppSession session();
	/** @deprecated see {@link com.pocket.sdk2.api.legacy.PocketCache} */
	@Deprecated
	PocketCache pktcache();
	UserManager user();
	BackgroundSync backgroundSync();
	WakeLockManager wakelocks();
	Troubleshooter troubleshooter();
	Listen listen();
	RotationLock rotationLock();
	DisplaySettingsManager displaySettings();
	PremiumFonts premiumFonts();
	AppThreads threads();
	UserAgent userAgent();
	ItemSessions itemSessions();
	ErrorHandler errorReporter();
	ActivityMonitor activities();
	SystemDarkTheme systemDarkTheme();
	Theme theme();
	SystemNotifications notifications();
	Push push();
	Clipboard clipboard();
	ImageCache imageCache();
	OfflineDownloading offline();
	Assets assets();
	AppSync appSync();
	PocketSingleton pocketSingleton();
	AppLifecycleEventDispatcher dispatcher();
	AppVersion build();
	HttpClientDelegate http();
	/** @deprecated See deprecation note in {@link AppPrefs} */
	@Deprecated
	AppPrefs prefs();
	UndoBar undo();
	ReviewPrompt reviewPrompt();
	Premium premium();
	UserMessaging messaging();
	Device device();
	Versioning versioning();
	AppOpen appOpen();
	Tracker tracker();
	ListManager listManager();
	SaveExtension saveExtension();
}
