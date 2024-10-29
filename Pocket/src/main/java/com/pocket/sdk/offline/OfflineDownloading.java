package com.pocket.sdk.offline;

import android.content.Context;

import com.pocket.app.ActivityMonitor;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.app.list.list.ListManager;
import com.pocket.app.settings.UserAgent;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.enums.OfflinePreference;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.enums.PremiumFeature;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.http.CookieDelegate;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.image.ImageCache;
import com.pocket.sdk.notification.SystemNotifications;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.cache.DownloadAuthorization;
import com.pocket.sdk.offline.downloader.TextDownloader;
import com.pocket.sdk.offline.downloader.WebDownloader;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.thread.PriorityTaskPool;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.LongPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Manages making {@link Item} content viewable offline,
 * including downloading Article Views, Web Views, Images and Tweet Attribution related to the user's saved items.
 * <p>
 * Listens for items that become available for download and automatically kicks off an attempt to predownload them according to the user's preferences.
 * {@link #predownload()} can be used to trigger rescanning all items and attempting to download them all according to the user's preferences.
 * If an automatic downloading process fails to download everything, it may schedule a retry based on the users settings.
 * <p>
 * {@link #download(Item, PositionType, boolean, DownloaderCallback)} can be used to immediately download a view regardless of cache or user settings.
 * <p>
 * This uses {@link Assets} for file management. Use {@link #webViewLocation(Item)} and {@link #articleViewLocation(Item)} to
 * locate the html files after they are downloaded. Use {@link Item#offline_text} and {@link Item#offline_web} to see if they are available offline.
 */
@Singleton
public class OfflineDownloading implements AppLifecycle {
	
	private final Set<OnDownloadStateChangedListener> listeners = new HashSet<>();
	private final Assets assets;
	private final Pocket pocket;
	private final PocketCache pktcache;
	private final AppThreads threads;
	private final UserAgent userAgent;
	private final HttpClientDelegate http;
	private final ImageCache imageCache;
	private final CookieDelegate cookies;
	private final BooleanPreference prefArticle;
	private final BooleanPreference prefWifiOnly;
	private final LongPreference suspended;
	private final AtomicBoolean allowRetries = new AtomicBoolean(false);
	private final ActivityMonitor activities;
	private final ListManager listManager;

	/** Can be null between {@link LogoutPolicy#stopModifyingUserData()} and {@link LogoutPolicy#restart()}. */
	private ThreadPools pools;
	private DownloadingSession session;
	/** Facilitates downloading Items after they've been saved. Is active while in Guest Mode, or while logged in, after initial fetching. */
	private Subscription newItemsDownload;

	@Inject
	public OfflineDownloading(Pocket pocket,
			PocketCache pktcache,
			AppThreads threads,
			AppSync appSync,
			Assets assets,
			UserAgent userAgent,
			HttpClientDelegate http,
			CookieDelegate cookies,
			ImageCache imageCache,
			AppPrefs prefs,
			ActivityMonitor activities,
			ListManager listManager,
			@ApplicationContext Context context,
			SystemNotifications systemNotifications,
			AppLifecycleEventDispatcher dispatcher) {
		this.pocket = pocket;
		this.pktcache = pktcache;
		this.threads = threads;
		this.assets = assets;
		this.userAgent = userAgent;
		this.http = http;
		this.cookies = cookies;
		this.imageCache = imageCache;
		this.pools = new ThreadPools(threads);
		this.prefArticle = prefs.DOWNLOAD_TEXT;
		this.prefWifiOnly = prefs.DOWNLOAD_ONLY_WIFI;
		this.suspended = prefs.DOWNLOAD_SUSPENDED;
		this.activities = activities;
		this.listManager = listManager;
		
		Disposable forever;
		// Look for items to download at the end of each sync
		appSync.addWork((Runnable) this::predownload);

		// The new items subscription is turned ON if in guest mode, in order to facilitate offline downloading of new saves
		toggleDownloadNewItems(false);
		// After fetching, turn the new Items subscription back on
		appSync.addFetchedWork(() -> toggleDownloadNewItems(true));
		
		// Cancel downloading if they they go off the required network
		http.status().addListener(status -> {
			if (prefWifiOnly.get() && !status.isWifi()) {
				cancelPredownloading();
			}
		});
		
		// Cancel / restart downloading if their preferences change.
		forever = Observable.combineLatest(
				prefArticle.getWithChanges(), prefWifiOnly.getWithChanges(),
				(article, wifiOnly) -> true) // using true since we don't need a value here, just that a change occurred.
				.subscribe(prefsUpdated -> {
					boolean wasPredownloading = predownloadingCount() > 0;
					// Cancel any setup with the old settings
					cancelPredownloading();
					// Restart if needed
					if (wasPredownloading) predownload();
				});
		
		// Cancel downloading if cache is wiped
		assets.addCleanListener(new Assets.CleanListener() {
			@Override public void onTrimmed(AssetUser user) {}
			@Override public void onRemovedAll() {
				cancelAll();
			}
		});

		DownloadingService.initialize(context, threads, this, systemNotifications);
		dispatcher.registerAppLifecycleObserver(this);
	}

	private void toggleDownloadNewItems(boolean on) {
		newItemsDownload = Subscription.stop(newItemsDownload);
		if (on) newItemsDownload = pocket.subscribe(Changes.of(Item.class).when((before, after) -> (before == null || before.status != ItemStatus.UNREAD) && after.status == ItemStatus.UNREAD),
				item -> predownload(item, false, false, true));
	}

	/** @return The user's current preference for what to predownload */
	private synchronized OfflinePreference preference() {
		if (prefArticle.get()) {
			return OfflinePreference.ARTICLE_ONLY;
		} else {
			return null; // Nothing to download
		}
	}
	
	/** @return The user's current preference for what to predownload first */
	private synchronized ItemSortKey downloadOrder() {
		if (assets.isCacheLimitSet()) {
			if (assets.getCacheLimitPriority() == Assets.CachePriority.OLDEST_FIRST) {
				return ItemSortKey.OLDEST;
			} else {
				return ItemSortKey.NEWEST;
			}
		} else {
			return listManager.getSortFilterState().getValue().getSort();
		}
	}
	
	private boolean isPredownloadingForbidden(Item item, boolean isNew) {
		return !isPredownloadingAllowed(item, isNew);
	}
	
	/**
	 * Checks all conditions to see if predownloading of this item is allowed.
	 * If item is null, this is a general check, not item specific.
	 */
	private synchronized boolean isPredownloadingAllowed(Item item, boolean isNew) {
		if (pools == null) return false;
		if (!pktcache.isLoggedIn()) return false;
		if (!http.status().isStable(Milliseconds.MINUTE)) return false;
		if (prefWifiOnly.get() && !http.status().isWifi()) return false;
		if (suspended.get() + Milliseconds.HOUR >= System.currentTimeMillis()) {
			if (item == null || item.time_added == null) return false;
			// Only allow items added after the suspension
			if (item.time_added.millis() <= suspended.get()) return false;
		}
		if (preference() == null) return false;
		if (item != null && isNew && downloadOrder() == ItemSortKey.NEWEST) {
			// If the item is new and cache is set to keep newest, ignore checking if it is full.
			// We want to predownload the item anyway and later clean up an older one.
		} else {
			if (!assets.isDownloadAuthorized(DownloadAuthorization.ONLY_WHEN_SPACE_AVAILABLE)) return false;
			if (assets.isOfflineDownloadingRestricted()) return false;
		}
		return true;
	}
	
	/**
	 * Asynchronously starts the downloading process for any items in my list that have not yet been downloaded. Honors the users downloading preferences,
	 * and may not download anything if the conditions are not met right now.
	 */
	public void predownload() {
		predownload(null, false);
	}
	
	/**
	 * Same as {@link #predownload()} but filters to only items matching this host /domain name
	 */
	private synchronized void predownload(String host, boolean refresh) {
		if (isPredownloadingForbidden(null, false)) return;
		pocket.sync(pocket.spec().things().saves()
			.state(ItemStatusKey.UNREAD)
			.downloadable(preference())
			.downloadable_retries(allowRetries.getAndSet(false))
			.sort(downloadOrder())
			.host(host)
			.build())
			.onSuccess(d -> predownload(d.list, d.downloadable_retries, refresh));
	}
	
	/**
	 * Same as {@link #predownload(Item, boolean, boolean, boolean)} for all items in this collection
	 */
	private synchronized void predownload(Collection<Item> items, boolean allowRetries, boolean refresh) {
		if (items == null || items.isEmpty()) return;
		for (Item item : items) {
			predownload(item, allowRetries, refresh, false);
		}
	}
	
	/**
	 * Attempts to queue up predownloading for any views in this item that still need to be downloaded to match
	 * the user's preferences.<br />
	 * Like {@link #predownload()}, this won't do anything if the current conditions don't
	 * allow predownloading.<br />
	 * For new items ({@code isNewItem == true}) it might temporarily go over the cache limit, but
	 * the cache should clean up older items instead.<br />
	 * Also if the item is already downloaded to the user's preferences, this will do nothing.
	 */
	private synchronized void predownload(Item item,
			boolean allowRetries,
			boolean refresh,
			boolean isNewItem) {
		if (isPredownloadingForbidden(item, isNewItem)) return;
		Set<PositionType> downloadables = ItemUtil.downloadables(item, preference(), allowRetries);
		Priority priority = isNewItem ? Priority.NEW_ITEM : Priority.NORMAL;
		if (downloadables.contains(PositionType.ARTICLE)) {
			queue(item, PositionType.ARTICLE, priority, refresh, null);
		}
		if (downloadables.contains(PositionType.WEB)) {
			queue(item, PositionType.WEB, priority, refresh, null);
		}
	}
	
	public synchronized void addSessionListener(OnDownloadStateChangedListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Download this view as soon as possible and callback when complete.
	 * This will attempt to download it regardless of user preferences, cache limits or device conditions.
	 * This will also download it at a higher priority than predownloads.
	 */
	public void download(Item item, PositionType view, boolean refresh, DownloaderCallback callback) {
		queue(item, view, Priority.HIGH, refresh, callback);
	}
	
	/**
	 * Queue up an item's view to be downloaded. If already queue'd it won't duplicate the request, unless 'refresh' is true.
	 *
	 * @param item The Item to download a view of
	 * @param view The view to download
	 * @param priority The priority of this download.
	 * @param refresh Whether or not to request a completely refreshed view.
	 * @param callback A callback when downloaded or null if no callback is needed.
	 */
	private synchronized void queue(Item item, PositionType view, Priority priority, boolean refresh, DownloaderCallback callback) {
		if (pools == null) return;
		session = getSession();
		
		ItemDownload download = new ItemDownload(item, view);
		Downloader downloader = session.downloading.get(download);
		
		if (downloader == null || refresh) {
			downloader = new Downloader(priority, download, refresh, this, session);
			pools.coordinators.submit(downloader);
			session.submitted(downloader);
		} else if (priority == Priority.HIGH) {
			downloader.setPriority(Priority.HIGH.taskRunnablePriority);
		}
		downloader.addCallback(callback);
	}
	
	/**
	 * Returns the current session or creates a new one
	 */
	private synchronized DownloadingSession getSession() {
		if (session == null) {
			session = new DownloadingSession();
			onSessionChanged();
		}
		return session;
	}
	
	private synchronized boolean isSessionActive(DownloadingSession session) {
		return this.session == session;
	}
	
	private synchronized void endSession(DownloadingSession session) {
		if (isSessionActive(session)) {
			this.session = null;
		}
		onSessionChanged();
	}
	
	private synchronized void onSessionChanged() {
		for (OnDownloadStateChangedListener l : listeners) {
			l.onDownloadStateChange(this);
		}
	}
	
	/**
	 * Cancel any predownloads in progress.
	 * If there were any manually requested items via {@link #download(Item, PositionType, boolean, DownloaderCallback)}, they will continue.
	 * @see #cancelAll()
	 */
	public synchronized void cancelPredownloading() {
		if (session == null) return;
		for (Downloader task : new ArrayList<>(session.downloading.values())) {
			if (task.getPriority() != Priority.HIGH.taskRunnablePriority) {
				task.cancel();
				session.finished(task);
			}
		}
		if (session.downloading.isEmpty()) {
			endSession(session); // End the session
		} else {
			onSessionChanged();
		}
	}
	
	/**
	 * Cancel all downloading. Including predownloading and manually requested downloading.
	 * @see #cancelPredownloading()
	 */
	private synchronized void cancelAll() {
		if (pools == null) return;
		endSession(session);
		pools.cancel();
	}
	
	/**
	 * Disables automatic starting of downloading for some period of time.
	 * Use this if the user cancels a download manually.
	 */
	public synchronized void suspendAutoDownload() {
		suspended.set(System.currentTimeMillis());
	}
	
	/**
	 * If currently suspended from {@link #suspendAutoDownload()}, reenable auto downloading.
	 */
	public synchronized void releaseAutoDownload() {
		suspended.set(0);
	}
	
	/**
	 * Allow the next {@link #predownload()} call to retry any views that were previously marked
	 * {@link OfflineStatus#FAILED} or {@link OfflineStatus#PARTIAL}.
	 */
	public synchronized void allowRetries() {
		allowRetries.set(true);
	}
	
	/**
	 * @return The number of downloaders running and queued that were triggered via a predownload method
	 */
	public synchronized int predownloadingCount() {
		if (session == null) return 0;
		return session.itemsPredownloading.size();
	}
	
	/**
	 * @return The number of predownloading downloaders that have completed in the current session.
	 */
	public synchronized int predownloadedCount() {
		if (session == null) return 0;
		return session.itemsPredownloaded;
	}
	
	public synchronized boolean isDownloading() {
		return session != null && !session.downloading.isEmpty();
	}
	
	/**
	 * Where the web view html file will be if downloaded. (or if it is an image, where the image would be)
	 * This doesn't indicate that it IS there, just the path where it will be.
	 * See {@link Item#offline_web} for its status.
	 */
	public synchronized File webViewLocation(Item item) throws AssetDirectoryUnavailableException {
		if (item.offline_web == OfflineStatus.OFFLINE_AS_ASSET) {
			return Asset.createImage(item.open_url.url, assets.getAssetDirectoryQuietly()).local;
		} else {
			return new File(assets.getAssetDirectory().pathForWeb(item));
		}
	}
	
	/**
	 * Where the article html file will be if downloaded.
	 * This doesn't indicate that it IS there, just the path where it will be.
	 * See {@link Item#offline_text} for its status.
	 */
	public synchronized File articleViewLocation(Item item) throws AssetDirectoryUnavailableException {
		return new File(assets.getAssetDirectory().pathForText(item));
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override
			public void stopModifyingUserData() {
				cancelAll();
				pools.terminate();
				pools = null;
			}
			
			@Override
			public void deleteUserData() {
				endSession(session);
			}
			
			@Override
			public void restart() {
				pools = new ThreadPools(threads);
			}

			@Override
			public void onLoggedOut() {}
		};
	}
	
	public interface DownloaderCallback {
		/**
		 * The downloading process for this view has completed.
		 * There is no guarantee about what thread this will be invoked from.
		 *
		 * @param item The item which had a view download attempted. Note this state may be out of date, only use for identity.
		 * @param view The view that was attempted
		 * @param status The status of the download, may be null if cancelled or failed
		 */
		void onDownloaderResult(Item item, PositionType view, OfflineStatus status);
	}
	
	/**
	 * Threads available for offline downloading.
	 */
	private static class ThreadPools implements WebDownloader.Worker {
		/** Where the high level work is done like a {@link TextDownloader} or {@link WebDownloader}. */
		private final PriorityTaskPool coordinators;
		/** Worker threads for {@link WebDownloader}s to use. */
		private final PriorityTaskPool workers;
		
		public ThreadPools(AppThreads src) {
			coordinators = src.newPriorityPool("offline-coord", 4);
			workers = src.newPriorityPool("offline-work", 4);
		}
		
		@Override
		public void work(Runnable r) {
			workers.submit(TaskRunnable.simple(r::run));
		}
		
		void cancel() {
			coordinators.cancelAll();
			workers.cancelAll();
		}
		
		void terminate() {
			cancel();
			coordinators.terminate(1, TimeUnit.MILLISECONDS);
			workers.terminate(1, TimeUnit.MILLISECONDS);
		}
	}
	
	public interface OnDownloadStateChangedListener {
		/**
		 * <p>Invoked when the downloading state has changed in some way. This is a generic callback, and could be for a number of reasons such as starting, stopping, cancelling, progress, etc.</p>
		 * <p>This will always be invoked on the UI thread</p>
		 * <p>NOTE: Since this is so generic, be warned that it can be invoked pretty often while the app is downloading. So your operations in this callback should be light weight.</p>
		 */
		void onDownloadStateChange(OfflineDownloading offline);
	}
	
	/**
	 * Tracks what is being downloaded and how many have been completed during a batch of downloads.
	 */
	private static class DownloadingSession {
		/** All downloaders that are currently queued or running. */
		public final Map<ItemDownload, OfflineDownloading.Downloader> downloading = new HashMap<>();
		/** The number of predownloaders pending per item. Used to track the number of items being downloaded (not the number of downloaders, since there can be mutliple downloaders per item) */
		private final Map<Item, Integer> itemsPredownloading = new HashMap<>();
		/** The number of predownloading items that have competed. */
		private int itemsPredownloaded;
		
		/** Invoke when the downloader is created and submitted to the queue. */
		synchronized void submitted(Downloader downloader) {
			downloading.put(downloader.item, downloader);
			if (downloader.isPredownload) incrementPredownloading(downloader.item.getItem());
		}
		
		/** Invoke when the downloader is completed (regardless of result). */
		synchronized void finished(Downloader downloader) {
			downloading.remove(downloader.item);
			if (downloader.isPredownload) decrementPredownloading(downloader.item.getItem());
		}
		
		synchronized void incrementPredownloading(Item item) {
			Integer c = itemsPredownloading.get(item);
			itemsPredownloading.put(item, c != null ? c+1 : 1);
		}
		synchronized void decrementPredownloading(Item item) {
			Integer c = itemsPredownloading.remove(item);
			if (c == null) return;
			if (c <= 1) {
				itemsPredownloaded++;
			} else {
				itemsPredownloading.put(item, c-1);
			}
		}
	}
	
	public enum Priority {
		/** For downloaders that the user is actively waiting for in UI, like when opening or refreshing a page. These will be done asap. */
		HIGH(TaskRunnable.PRIORITY_HIGH),
		/** For predownloading new items. Thread will still run at normal priority, but we might temporarily go over the cache limit. */
		NEW_ITEM(TaskRunnable.PRIORITY_NORMAL),
		/** All predownloading will run as a normal priority. */
		NORMAL(TaskRunnable.PRIORITY_NORMAL);
		private final int taskRunnablePriority;
		Priority(int taskRunnablePriority) {
			this.taskRunnablePriority = taskRunnablePriority;
		}
	}
	
	/**
	 * A task that runs the appropriate downloader operation for a {@link ItemDownload} and handles updating the item's status and firing callbacks.
	 */
	private class Downloader extends TaskRunnable {
		
		final ItemDownload item;
		
		private final Set<DownloaderCallback> callbacks = new HashSet<>();
		private final boolean refresh;
		private final Object lock;
		private final DownloadingSession partOf;
		private final boolean isPredownload;
		private final boolean isNewItem;
		private OfflineStatus status;
		private boolean isComplete;
		
		private Downloader(Priority priority, ItemDownload item, boolean refresh, Object lock, DownloadingSession session) {
			super(priority.taskRunnablePriority);
			this.item = item;
			this.refresh = refresh;
			this.lock = lock;
			this.partOf = session;
			this.isNewItem = priority == Priority.NEW_ITEM;
			this.isPredownload = priority == Priority.NORMAL || this.isNewItem;
		}
		
		/**
		 * Adds a callback to fire when completed, or if already completed, invokes it now on this calling thread.
		 */
		public void addCallback(DownloaderCallback callback) {
			if (callback == null) return;
			synchronized (lock) {
				if (isComplete) {
					callback.onDownloaderResult(item.getItem(), item.getView(), status);
				} else {
					callbacks.add(callback);
				}
			}
		}
		
		@Override
		protected void backgroundOperation() throws Exception {
			synchronized (lock) {
				if (getPriority() == Priority.NORMAL.taskRunnablePriority) {
					// Double check downloading authorization hasn't changed since this was added to the queue.
					if (isPredownloadingForbidden(item.getItem(), isNewItem)) return;
				}
			}
			
			Item item = this.item.getItem();
			PositionType view = this.item.getView();
			
			Item latest = pocket.syncLocal(item).get();
			if (latest == null) latest = item; // This is likely a direct request for the item and the item might not exist locally.
			
			if (view == PositionType.ARTICLE) {
				// If this throws an error, we'll just leave the status as is was, we will always retry text views.
				status = new TextDownloader().download(
						latest,
						pocket,
						assets,
						userAgent,
						http.getClient(),
						com.pocket.sdk.api.generated.enums.FormFactor.find(FormFactor.getClassKey(false)),
						imageCache,
						refresh);
				
				pocket.sync(null, pocket.spec().actions().update_offline_status()
						.time(Timestamp.now())
						.item(item)
						.view(view)
						.status(status)
						.build())
							.get();
				
			} else if (view == PositionType.WEB) {
				WebDownloader.Result result;
				try {
					result = WebDownloader.download(item, refresh, assets, pktcache.hasFeature(PremiumFeature.PERMANENT_LIBRARY), http.getClient(), cookies, pools, this::isCancelled);
				} catch (Throwable t) {
					if (t instanceof AssetDirectoryUnavailableException) {
						throw t; // Skip changing the status
					}
					result = new WebDownloader.Failure();
				}
				
				String encoding = item.encoding;
				String mimeType = item.mime_type;
				
				if (result instanceof WebDownloader.Success) {
					status = OfflineStatus.OFFLINE;
					encoding = ((WebDownloader.Success)result).encoding;
					mimeType = ((WebDownloader.Success)result).mimeType;
					
				} else if (result instanceof WebDownloader.SuccessAsset) {
					status = OfflineStatus.OFFLINE_AS_ASSET;
					mimeType = ((WebDownloader.SuccessAsset)result).mimeType;
					
				} else if (result instanceof WebDownloader.PermanentFailure) {
					status = OfflineStatus.INVALID;
					
				} else if (result instanceof WebDownloader.Cancelled) {
					return; // Don't change the status
					
				} else if (result instanceof WebDownloader.Partial) {
					if (http.status().isStable(Milliseconds.MINUTE)) {
						// If this failed again while we had internet access, consider it good as we are going to get
						status = OfflineStatus.OFFLINE;
					} else {
						// Keep it as is and allow retry next time we have a good connection
						status = OfflineStatus.PARTIAL;
					}
					encoding = ((WebDownloader.Partial)result).encoding;
					mimeType = ((WebDownloader.Partial)result).mimeType;
					
				} else if (result instanceof WebDownloader.Failure) {
					status = OfflineStatus.FAILED;
					// Note: We could consider setting to invalid if it fails more than once on a stable connection, which might indicate it will never work
					
				} else {
					return; // This would mean we added a status and didn't implement it here. The result is undetermined.
				}
				
				pocket.sync(null, pocket.spec().actions().update_offline_status()
						.time(Timestamp.now())
						.item(item)
						.view(view)
						.status(status)
						.encoding(encoding)
						.mime(mimeType)
						.build())
						.get();
				
			}
		}
		
		@Override
		protected void backgroundOnComplete(boolean success, Throwable operationCrash) {
			Collection<DownloaderCallback> callbacks;
			
			synchronized (lock) {
				if (isSessionActive(partOf)) {
					// Update the session numbers
					partOf.finished(this);
					if (partOf.downloading.isEmpty()) {
						endSession(partOf); // End the session
					} else {
						onSessionChanged();
					}
				}
				isComplete = true;
				callbacks = new ArrayList<>(this.callbacks);
				this.callbacks.clear();
			}
			
			for (DownloaderCallback callback : callbacks) {
				callback.onDownloaderResult(item.getItem(), item.getView(), status);
			}
			
			if (operationCrash instanceof AssetDirectoryUnavailableException) {
				cancelAll();
				assets.checkForStorageIssues(AbsPocketActivity.from(activities.getVisible()), null);
			}
		}
	}
	
}
