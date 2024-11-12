package com.pocket.app;

import android.content.Context;
import android.util.Log;

import androidx.collection.CircularArray;

import com.pocket.app.build.Versioning;
import com.pocket.app.settings.UserAgent;
import com.pocket.sdk.AndroidPocket;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.PocketServer;
import com.pocket.sdk.api.endpoint.AndroidDeviceInfo;
import com.pocket.sdk.api.endpoint.AppInfo;
import com.pocket.sdk.api.endpoint.DeviceInfo;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.SnowplowAppId;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.source.LoggingPocket;
import com.pocket.sdk.api.source.PocketRemoteSource;
import com.pocket.sdk.api.source.SnowplowSource;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.build.AppVersion;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.util.thread.PriorityTaskPool;
import com.pocket.sdk.util.thread.WakefulTaskPool;
import com.pocket.sdk2.api.legacy.IdkeyMigration;
import com.pocket.sdk2.api.legacy.LegacyMigration;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.threads.ThreadPools;
import com.pocket.sync.thing.Thing;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.prefs.EnumPreference;
import com.pocket.util.prefs.Preferences;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Maintains a single instance of {@link Pocket} for use in the entire {@link App}.
 * Logout will be managed by {@link UserManager}.
 */
@Singleton
public class PocketSingleton {
	
	private static final String ASSET_USER_THING_TYPE = "thing";
	private static final String ASSET_USER_ITEM_TYPE = "item";
	private final CircularArray<String> requestLog = new CircularArray<>(25);
	private final AppMode mode;
	private final Pocket pocket;
	private final Assets assets;
	private final EnumPreference<LoggingLevel> teamLoggingLevel;

	@Inject
	public PocketSingleton(
			Assets assets,
			AppMode mode,
			UserAgent userAgent,
			@ApplicationContext Context context,
			AppThreads threads,
			AppVersion build,
			HttpClientDelegate http,
			PocketServer pktserver,
			Preferences prefs,
			Device device,
			@Nullable LegacyMigration legacy,
			Versioning versioning,
			ErrorHandler errorReporter
	) {
		this.mode = mode;
		this.assets = assets;
		if (mode.isForInternalCompanyOnly()) {
			this.teamLoggingLevel = prefs.forApp("dcfig_lg_lg", LoggingLevel.class, (mode.isDevBuild() ? LoggingLevel.DEV : LoggingLevel.OFF));
		} else {
			this.teamLoggingLevel = null;
		}
		
		AppInfo appId = new AppInfo(
				build.getConsumerKey(),
				"Pocket",
				build.getProductName(),
				build.getVersionName(context),
				build.getStoreName(true, context),
				build.getStoreName(false, context));
		
		SnowplowSource.Config snowplowConfig =
				new SnowplowSource.Config(pktserver.snowplowCollector(), pktserver.snowplowPostPath(), mode.isForInternalCompanyOnly()
						? SnowplowAppId.POCKET_ANDROID_DEV
						: SnowplowAppId.POCKET_ANDROID);
		Pocket.Config.Builder config = new AndroidPocket.Config.Builder(context, appId, deviceIdentity(context, userAgent.mobile(), device))
				.remote(new PocketRemoteSource(http.getClient(), pktserver.api(), pktserver.articleView(), snowplowConfig))
				.threads(new WakefulPools(threads));
		if (legacy != null) {
			config.migration(legacy.storage());
		} else if (versioning.upgraded(7,27,0,0)) {
			config.migration(new IdkeyMigration(assets, errorReporter));
		}

		if (teamLoggingLevel == null || teamLoggingLevel.get() == LoggingLevel.OFF) {
			pocket = new Pocket(config.build());
		} else {
			LoggingPocket.Logger logger = log -> {
				Log.i("PktLogging", log);
				addToRequestLog(log);
			};
			switch (teamLoggingLevel.get()) {
				case QA: pocket = LoggingPocket.qa(config.build(), logger); break;
				case DEV: pocket = LoggingPocket.dev(config.build(), logger); break;
				case DEBUG: pocket = LoggingPocket.debug(config.build(), logger); break;
				case DEBUG_COMPACT: pocket = LoggingPocket.debugCompact(config.build(), logger); break;
				case PROFILING: pocket = LoggingPocket.profiling(config.build(), logger); break;
				default: throw new RuntimeException("unexpected type " + teamLoggingLevel.get());
			}
		}
		
		AssetCleaner cleaner = new AssetCleaner(pocket, assets);
		assets.addAssetUserCleaner(cleaner);
		assets.addCleanListener(cleaner);
	}
	
	public Pocket getInstance() {
		return pocket;
	}

	private void addToRequestLog(String log) {
		if (requestLog.size() == 25) {
			requestLog.popFirst();
		}
		requestLog.addLast(log);
	}
	
	private DeviceInfo deviceIdentity(Context context, String mobile, Device device) {
		DeviceInfo info = new AndroidDeviceInfo(context, mobile);
		if (mode.isForInternalCompanyOnly()) {
			// There is an Alpha setting that allows overriding these values for testing.
			info = new DeviceInfo(info.os, info.os, device.manufacturer(), device.model(), device.product(), info.deviceType, info.locale, info.userAgent);
		}
		return info;
	}
	
	/**
	 * @return an {@link AssetUser} that will be released during cache cleaning when the pocket instance no longer remembers this item.
	 * 		Its trim priority is based on its {@link Item#time_added} value and the current {@link Assets#getCacheLimitPriority()} user preference.
	 */
	public AssetUser assetUser(Timestamp timeAdded, String idKey) {
		long time = Timestamp.get(timeAdded);
		if (time > 0) {
			switch (assets.getCacheLimitPriority()) {
				case Assets.CachePriority.NEWEST_FIRST: return new AssetUser(ASSET_USER_ITEM_TYPE, idKey, time);
				case Assets.CachePriority.OLDEST_FIRST: return new AssetUser(ASSET_USER_ITEM_TYPE, idKey, AssetUser.PRIORITY_HIGH - time);
			}
		}
		return new AssetUser(ASSET_USER_ITEM_TYPE, idKey, AssetUser.PRIORITY_LOW);
	}
	
	/**
	 * @return an {@link AssetUser} that will be released during cache cleaning when the pocket instance no longer remembers this thing.
	 * 		Has a high priority for keeping when the cache trims.
	 */
	public AssetUser assetUser(Thing thing) {
		return new AssetUser(ASSET_USER_THING_TYPE, thing.idkey(), AssetUser.PRIORITY_HIGH);
	}
	
	public String dumpRequestLog() {
		if (!mode.isForInternalCompanyOnly()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("Latest Requests\n");
		for (int i = 0; i < requestLog.size(); i++) {
			String log = requestLog.get(i);
			builder.append("\n")
					.append(log)
					.append("\n");
		}
		return builder.toString();
	}
	
	/**
	 * Using wakeful pools will help ensure that any async processes are completed before our process is killed
	 * (It can still be killed for whatever reason, but this increases data safety)
	 */
	private static class WakefulPools implements ThreadPools {
		
		private final AppThreads threads;
		
		private WakefulPools(AppThreads threads) {
			this.threads = threads;
		}
		
		@Override
		public Pool newPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut) {
			WakefulTaskPool pool = threads.newWakefulPool("pocket", corePoolSize, maximumPoolSize, keepAliveTime, unit);
			pool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
			return new Pool() {
				@Override
				public void submit(Runnable task) {
					pool.execute(task);
				}
				@Override
				public void stop(long timeout, TimeUnit unit) {
					pool.shutdown();
					try {
						pool.awaitTermination(timeout, unit);
					} catch (InterruptedException ignore) {}
					pool.shutdownNow();
				}
			};
		}
		
		@Override
		public PrioritizedPool newPrioritizedPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut) {
			PriorityTaskPool pool = threads.newPriorityPool(name, corePoolSize, maximumPoolSize, keepAliveTime, unit);
			pool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
			return (task, priority) -> {
				TaskRunnable tr = TaskRunnable.simple(task::run);
				tr.setPriority(Integer.MAX_VALUE-priority); // Their priority basis is flipped
				pool.submit(tr);
			};
		}
	}
	
	public enum LoggingLevel {
		OFF,
		QA,
		DEV,
		DEBUG,
		DEBUG_COMPACT,
		PROFILING
	}

	/** Only exposed for {@link com.pocket.app.settings.beta.BetaConfigFragment} to use. Changing this won't have any impact until next run. */
	public EnumPreference<LoggingLevel> teamLoggingLevelPref() {
		return teamLoggingLevel;
	}

	static class AssetCleaner implements Assets.AssetUserCleanup, Assets.CleanListener {
		
		private final Pocket pocket;
		private final Assets assets;
		
		AssetCleaner(Pocket pocket, Assets assets) {
			this.pocket = pocket;
			this.assets = assets;
			
			// Monitor when an item's offline state changes from having offline files to none,
			// and delete it's markup folder. In theory this could cause a race condition where we delete it while
			// it is being redownloaded but ignoring that possibility for now.
			pocket.setup(() -> pocket.subscribe(Changes.of(Item.class).when(
					(before, after) ->
						before != null && (ItemUtil.isViewableOffline(before.offline_text) || ItemUtil.isViewableOffline(before.offline_web))
						&& (!ItemUtil.isViewableOffline(before.offline_text) && !ItemUtil.isViewableOffline(before.offline_web))
					),
					item -> clearMarkupFolder(item.idkey())));
		}
		
		/**
		 * Delete the markup folder (and its article and web markup)
		 * @param itemIdKey The {@link Item#idkey()}
		 */
		private void clearMarkupFolder(String itemIdKey) {
			try {
				FileUtils.deleteDirectory(new File(assets.getAssetDirectory().folderPathFor(itemIdKey))); // TODO this is technically a disk op on the ui thread, since our publisher thread is the android ui thread.  wrap this in a thread call. leaving a note instead of doing it since it will need testing.
			} catch (Throwable ignore){}
		}
		
		@Override
		public void cleanupAssetUsers(Assets assets) {
			cleanAssets();
			cleanMarkups();
		}
		
		/**
		 * Finds thing asset users that are registered in the asset database,
		 * but are not in the Pocket instance any more, and unregisters them as asset users.
		 */
		private void cleanAssets() {
			List<AssetUser> users = assets.getAssetUsers(ASSET_USER_THING_TYPE);
			users.addAll(assets.getAssetUsers(ASSET_USER_ITEM_TYPE));
			
			String[] idkeys = new String[users.size()];
			for (int i = 0, len = users.size(); i < len; i++) {
				idkeys[i] = users.get(i).user;
			}
			
			boolean[] results;
			try {
				results = pocket.contains(idkeys).get();
			} catch (Throwable unexpected) {
				throw new RuntimeException(unexpected);
			}
			
			boolean removedItems = false;
			for (int i = 0, len = users.size(); i < len; i++) {
				if (!results[i]) {
					AssetUser as = users.get(i);
					assets.unregisterAssets(as);
					if (ASSET_USER_ITEM_TYPE.equals(as.type)) {
						removedItems = true;
						clearMarkupFolder(as.user);
					}
				}
			}
			if (removedItems) assets.unlockDownloading();
		}
		
		/**
		 * Finds items that have markup folders but are not in the Pocket instance any more,
		 * and delete's their markup folder.
		 */
		private void cleanMarkups() {
			// Grab list of item markup folders (which should be their idkeys as names)
			String[] idkeys = null;
			try {
				idkeys = assets.getAssetDirectory().getMarkupDirectory().list();
			} catch (Throwable ignore) {}
			if (idkeys == null) return;
			
			boolean[] results;
			try {
				results = pocket.contains(idkeys).get();
			} catch (Throwable unexpected) {
				throw new RuntimeException(unexpected);
			}
			
			for (int i = 0, len = idkeys.length; i < len; i++) {
				if (!results[i]) clearMarkupFolder(idkeys[i]);
			}
		}
		
		@Override
		public void onTrimmed(AssetUser user) {
			if (user.type.equals(ASSET_USER_ITEM_TYPE)) {
				pocket.sync(null, pocket.spec().actions().update_offline_status()
						.time(Timestamp.now())
						.status(OfflineStatus.NOT_OFFLINE)
						.item_idkey(user.user)
						.build());
			}
		}
		
		@Override
		public void onRemovedAll() {
			pocket.sync(null, pocket.spec().actions().reset_offline_statuses().time(Timestamp.now()).build());
		}
	}
	
}
