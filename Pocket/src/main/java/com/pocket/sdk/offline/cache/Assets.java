package com.pocket.sdk.offline.cache;

import android.content.Context;

import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.file.AndroidStorageLocation;
import com.pocket.sdk.util.file.AndroidStorageUtil;
import com.pocket.util.java.BytesUtil;
import com.pocket.util.java.Logs;
import com.pocket.util.java.PktFileUtils;
import com.pocket.util.java.StringUtils2;
import com.pocket.util.java.UnicodeUtils;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.EnumPreference;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.LongPreference;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Observable;

/**
 * Manages files that are downloaded for offline use.
 * <p>
 * Files in this package are called {@link Asset}s. There are a few kinds of assets:
 * <ul>
 *     <li>{@link Asset#MARKUP} : html files for web and article views.</li>
 *     <li>{@link Asset#IMAGE} : image files, used for a variety of things in the app like thumbnails and offline web and article views.</li>
 *     <li>{@link Asset#STYLESHEET} : css files that offline web views use.</li>
 * </ul>
 *
 * <h3>Asset Users</h3>
 * This maintains a database of all assets on disk as well as the {@link AssetUser}s referencing them.
 * When a component downloads and stores an asset, it will declare an {@link AssetUser}, which describes who/what wants
 * the asset to be on disk. One asset can have multiple {@link AssetUser}s referencing it.
 * For example, if a user has multiple items saved from the same domain, each web page on that
 * domain will share some css files and images. Each file will only be downloaded once but each item that needs it will be its own {@link AssetUser}.
 * <p>
 * As components no longer need that asset, they release their {@link AssetUser}s.
 * When an asset no longer has any {@link AssetUser}s referencing it, the asset file can be deleted by the {@link CacheCleaner}.
 *
 * <h3>Cache Limits</h3>
 * This also supports a feature that lets users set a maximum amount of disk space that this offline cache can use.
 * The database is also used to keep track of how much storage space it is using.
 * When a cache limit is in effect – not unlimited – this will allow assets to stored up to the {@link #getActualCacheLimit()}.
 * Note: this maintains a buffer space, so the limit is actually less than what the user sets, see that method for details.
 * <p>
 * When that limit is reached a few things will occur:
 * <ul>
 *     <li>Downloading will become "locked", meaning certain {@link DownloadAuthorization}s will no longer be allowed.</li>
 *     <li>It will start a {@link #clean()} and trim assets until it is back under the limit.</li>
 * </ul>
 * This will remain in the "locked" state, even after the cache goes back under the limit.
 * It will only be unlocked in the following cases:
 * <ul>
 *     <li>Process restart, the app will start up unlocked each time.</li>
 *     <li>When cache limit settings change.</li>
 *     <li>When an asset user is unregistered.</li>
 * </ul>
 * This lock is to prevent endless loops where you are downloading, go over the limit, trigger a cache clean, go under the limit and then
 * restart downloading to fill the space, only to go over again and restart the cycle. This way, it will only attempt to start downloading
 * again when something about the cache actually changed.
 * <p>
 * The {@link CacheCleaner} is what handles trimming assets to stay within the cache limits, and handles deleting unused assets.
 *
 * <h3>Working with Assets</h3>
 * The ui app code will rarely interact with this class directly, but instead will use the feature based components such as:
 * <ul>
 * 	<li>{@link com.pocket.sdk.image.Image} which helps you download, store load and resize images.</li>
 * 	<li>{@link com.pocket.sdk.offline.OfflineDownloading} which manages downloading offline article views and web pages and all of the assets needed to display those pages.</li>
 * </ul>
 * If you need to make a component that downloads assets, follow these general usage guidelines:
 * <p>
 * First ask yourself... Is what you are downloading an "asset"? Assets are deleted on logout and contribute to the cache limit.
 * If the files you are downloading are meant to be permanently available or are part of the app, you
 * may consider a different downloading and storage api, and not manage it as an asset.
 * <p>
 * If its an asset, then:
 * <ul>
 *     <li>Before downloading or writing a file, check {@link #isDownloadAuthorized(DownloadAuthorization)} to see if given the current cache size and user limits, if your asset is allowed to be downloaded.</li>
 *     <li>Make sure you register that you need this asset with one of the {@link #registerAssetUser(Asset, AssetUser)} like methods.</li>
 *     <li>When you are done with the asset and don't need it on disk, use of of the {@link #unregisterAssets(AssetUser)} methods.</li>
 *     <li>To write an asset to disk use one of the `write()` methods such as {@link #write(Asset, byte[])}.
 *     		or if you had to do the write yourself, invoke one of the written... methods like {@link #written(Asset, long)} to report the asset is now on disk.</li>
 *     <li>Assets are only deleted by the {@link CacheCleaner} when they no longer have asset users, so no need to delete them yourself.</li>
 * </ul>
 *
 * @see Asset
 * @see AssetDirectory
 * @see AssetUser
 * @see DownloadAuthorization
 * @see CacheCleaner
 */
@Singleton
public class Assets implements AppLifecycle {

    /** Possible values for {@link #getCacheLimitPriority()} */
	public static class CachePriority {
		/** Prioritize newest items. */
		public static final int NEWEST_FIRST = 0;
		/** Prioritize oldest items. */
		public static final int OLDEST_FIRST = 1;
	}
	
	/** @see #getActualCacheLimit() for details on this */
	public static final long CACHE_BUFFER = BytesUtil.mbToBytes(100);
	/** The smallest the cache limit is allowed to be. */
	public static final long CACHE_LIMIT_MIN = CACHE_BUFFER + BytesUtil.mbToBytes(100);
	/** The largest the cache limit is allowed to be before it is just considered unlimited. */
	public static final long CACHE_LIMIT_MAX = BytesUtil.gbToBytes(2);
	/** The minimum amount of space available in the cache before general pre-caching offline downloading is allowed to start again. */
	protected static final long CACHE_RESTART_DOWNLOADING_BUFFER = BytesUtil.mbToBytes(10);
	
	/** Synchronization lock for {@link #cacheState} and related values */
	private final Object cacheStateLock = new Object();
	/** Assets currently downloading. */
	private final ArrayList<OnCacheSizeChangedListener> cacheSizeListeners = new ArrayList<>(1);
	/** Deletes assets that are no longer used. */
	private final CacheCleaner cleaner = new CacheCleaner();
	/** Helper for prompting the user how to fix various storage/disk errors that could occur. */
	private final StorageErrorResolver storageErrorResolver;
	/** Synchronization lock for {@link #assetDirectory} and related values. */
	private final Object assetDirectoryLock = new Object();
	private final List<CleanListener> cleanListeners = new ArrayList<>();
	
	private final AppThreads threads;
	private final Context appContext;
	private final AssetsDatabase database;
	private final CacheState cacheState;
	private final LongPreference cacheSizeUsed;
	private final LongPreference dbSizeApprox;
	private final IntPreference userCacheSort;
	private final LongPreference userSizeLimit;
	private final EnumPreference<AndroidStorageLocation.Type> storageType;
	private final StringPreference removableStoragePath;
	private final BooleanPreference isNoMediaSetup;
	private final IntPreference directoryIncrement;
	
	/** Current location of all assets. Use {@link #getAssetDirectory()} to access. */
	private AssetDirectory assetDirectory;
	private boolean cleanedTemp;

	@Inject
	public Assets(
			AppThreads threads,
			@ApplicationContext Context context,
			Preferences prefs,
			ErrorHandler errorHandler,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.threads = threads;
		this.appContext = context;
		this.cacheState = new CacheState(this, prefs.forUser("cacheDLock", false));
		this.storageErrorResolver = new StorageErrorResolver(threads);
		this.cacheSizeUsed = prefs.forUser("cacheUsed", 0L);
		this.dbSizeApprox = prefs.forUser("dbSize", 0L);
		this.userCacheSort = prefs.forUser("cacheSort", Assets.CachePriority.NEWEST_FIRST);
		this.userSizeLimit = prefs.forUser("cacheLimit", 0L);
		this.storageType = prefs.forApp("storagetype", AndroidStorageLocation.Type.class, null);
		this.removableStoragePath = prefs.forApp("rstoragepath", (String) null);
		this.isNoMediaSetup = prefs.forUser("sdCardSetup", false);
		this.directoryIncrement = prefs.forApp("path_inc", 1);
		
		database = new AssetsDatabase(context, threads, errorHandler, new AssetsDatabase.Assets() {
			@Override
			public void setCacheSize(AssetsDatabase.Size size) {
				synchronized (cacheStateLock) {
					cacheSizeUsed.set(size.assets);
					dbSizeApprox.set(size.db);
					cacheState.invalidate();
					for (OnCacheSizeChangedListener listener : cacheSizeListeners) {
						listener.onCacheSizeChanged(size.assets);
					}
				}
			}
			@Override
			public AssetDirectory getAssetDirectory() throws AssetDirectoryUnavailableException {
				return Assets.this.getAssetDirectory();
			}
		});
		
		cacheState.invalidate();
	}
	
	@Override
	public void onUserGone(Context context) {
		clean();
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override public void stopModifyingUserData() {} // The assets database will stop during clear() below
			
			@Override
			public void deleteUserData() {
				database.clear(appContext);
				synchronized (assetDirectoryLock) {
					if (assetDirectory != null) {
						resetOfflineDirectory();
					}
				}
			}
			
			@Override public void restart() {}

			@Override public void onLoggedOut() {}
		};
	}
	
	/** Same as {@link #getAssetDirectory()} but returns null instead of throwing an exception if unavailable. */
	public AssetDirectory getAssetDirectoryQuietly() {
		try {
			return getAssetDirectory();
		} catch (Throwable ignore) {
			return null;
		}
	}
	
	/**
	 * @return The directory that holds all of the assets.
	 * @throws AssetDirectoryUnavailableException This can occur when the users preferred
	 * 			directory is unavailable. This can happen when it is on removable or mountable storage
	 * 			like the external storage directory or an sd card.
	 * 			If this is unexpected (most cases should be), and you want to ask the user
	 * 			to resolve the issue, use {@link #checkForStorageIssues(AbsPocketActivity, StorageErrorResolver.Callback)}
	 */
	public AssetDirectory getAssetDirectory() throws AssetDirectoryUnavailableException {
		synchronized (assetDirectoryLock) {
			if (assetDirectory == null) {
				AndroidStorageLocation location;
				if (!storageType.isSet()) {
					// Set the default as Internal since it is private and most reliable (not unmountable).
					location = AndroidStorageUtil.getInternal(appContext);
					assetDirectory = new AssetDirectory(location);
					setStorageLocation(location);
					
				} else {
					// Load from user preferences
					AndroidStorageLocation.Type type = storageType.get();
					switch (type) {
						case INTERNAL: location = AndroidStorageUtil.getInternal(appContext); break;
						case EXTERNAL: location = AndroidStorageUtil.getExternal(appContext); break;
						case REMOVABLE: location = AndroidStorageUtil.asRemovable(removableStoragePath.get()); break;
						default: throw new RuntimeException("unknown type " + type);
					}
					assetDirectory = new AssetDirectory(location);
				}
				if (location.isMediaScannable() && !isNoMediaSetup.get()) {
					// Install a no media file
					AssetDirectory instance = assetDirectory;
					threads.async(() -> {
						if (isNoMediaSetup.get() || instance != assetDirectory) return;
						try {
							new File(instance.getOfflinePath()).mkdirs();
							File nomedia = new File(instance.getRoot(), ".nomedia");
							isNoMediaSetup.set((nomedia.createNewFile() || nomedia.exists()) && instance == assetDirectory);
						} catch (IOException e) {
							Logs.printStackTrace(e);
						}
					});
				}
			}
			return assetDirectory;
		}
	}
	
	/**
	 * @return A directory for temporary files that are automatically cleaned up on some regular interval.
	 * Clean up will only run once per app session and before anyone accesses this.
	 * That will ensure we don't accidently clean up in-use files.
	 * If this is invoked before or during that clean up, this will block until the clean up is completed.
	 */
	public File getTempDirectory() throws AssetDirectoryUnavailableException {
		synchronized (assetDirectoryLock) {
			File tmp = new File(getAssetDirectory().getTempDirectory());
			if (!cleanedTemp) {
				if (tmp.exists()) {
					try {
						FileUtils.cleanDirectory(tmp);
					} catch (IOException ignore) {}
				}
				cleanedTemp = true;
			}
			return tmp;
		}
	}
	
	/**
	 * Moves all assets into the clean up directory, which will later be cleaned up by the {@link com.pocket.sdk.offline.cache.Assets.CacheCleaner}.
	 */
	private void resetOfflineDirectory() {
		if (assetDirectory == null) return;
		
		int key = directoryIncrement.get();
		key++;
		if (key == Integer.MAX_VALUE) {
			key = 0;
		}
		directoryIncrement.set(key);
		
		File currentName = new File(assetDirectory.getOfflinePath());
		File newName = new File(assetDirectory.getCleanupPath() + key);
		
		currentName.renameTo(newName);
		
		assetDirectory = null;
	}
	
	
	/**
	 * Changes the storage location of the cache and Pocket's files. <b>WARNING: This will erase all of those files and
	 * start fresh at the new location. <i>Files are not moved!</i></b>.
	 * Also see {@link #clearOfflineContent(Runnable, Runnable)} which you will need to do if you are changing locations.
	 */
	void setStorageLocation(final AndroidStorageLocation location) throws AssetDirectoryUnavailableException {
		storageType.set(location.getType());
		if (location.getType() == AndroidStorageLocation.Type.REMOVABLE || location.getType() == AndroidStorageLocation.Type.EXTERNAL) {
			removableStoragePath.set(location.getPath());
		}
		isNoMediaSetup.set(false);
		threads.async(() -> new File(assetDirectory.getOfflinePath()).mkdirs());
	}
	
	public void addCleanListener(CleanListener listener) {
		cleanListeners.add(listener);
	}
	
	public interface CleanListener {
		void onTrimmed(AssetUser user);
		void onRemovedAll();
	}
	
	/**
	 * Delete all asset files and directories.
	 * @param resetCallback A callback for when everything has been flagged to be cleaned. If you don't need to wait for the files to actual delete you can use this, since the files will be cleaned up eventually.
	 * @param completeCallback A callback for when the files have also been cleaned and deleted. If you want to wait for the files to actually be deleted, use this.
	 */
	public void clearOfflineContent(Runnable resetCallback, Runnable completeCallback) {
		threads.async(() -> {
			synchronized (assetDirectoryLock) {
				resetOfflineDirectory();
			}
			database.clear(appContext);
			for (CleanListener listener : cleanListeners) {
				try {
					listener.onRemovedAll();
				} catch (Throwable t) {
					// Don't let implementations crash us
					Logs.printStackTrace(t);
				}
			}
			unlockDownloading();
			if (resetCallback != null) resetCallback.run();
			cleaner.clearCleanDirectory();
			if (completeCallback != null) completeCallback.run();
		});
	}
	
	public void registerAssetUser(Asset asset, AssetUser user) {
		registerAssetUser(asset.local.getAbsolutePath(), user);
	}
	
	public void registerAssetUser(String assetLocalPath, AssetUser user) {
		database.add(user, assetLocalPath);
	}
	
	public void unregisterAssets(AssetUser user) {
		database.removeUser(user);
	}
	
	/** See {@link AssetsDatabase#await()} */
	public void awaitAssetDatabaseChanges() throws InterruptedException {
		database.await();
	}
	
	/**
	 * Are new item downloaders allowed to be created?
	 * <p>
	 * If restrictions are in place, the only item downloaders that should be allowed to run are ones that fill in gaps
	 * where higher priority based on user preferences need to be downloaded.
	 */
	public boolean isOfflineDownloadingRestricted() {
		return cacheState.isOfflineDownloadingRestricted();
	}

	/**
	 * @return false if the download is requested to not run due to cache restrictions. true if its ok to download.
	 */
	public boolean isDownloadAuthorized(DownloadAuthorization auth) {
		if (cacheState.isOverLimit()) {
			return auth == DownloadAuthorization.ALWAYS;
		} else {
			// If the cache isn't full, everyone is welcome to the party
			return true;
		}
	}

	/**
	 * @return If the cache is over the limit, then this returns the number of bytes needing to be removed. If unlimited or within limits, then 0.
	 */
	public long getCacheSizeToTrim() {
		long cacheLimit = getActualCacheLimit();
		if (cacheLimit <= 0) {
			return 0; // Unlimited
		} else {
			long cacheSize = getCacheSize();
			long bytes = cacheSize - cacheLimit;
			return bytes > 0 ? bytes : 0;
		}
	}
	
	/**
	 * @return The number of bytes remaining in the cache before it hits the limit, 0 if the cache is full or overfull (see {@link #getCacheSizeToTrim()}), or -1 if unlimited.
	 */
	public long getCacheSpaceRemaining() {
		if (!isCacheLimitSet()) {
			return -1;
		} else if (isCacheFull()) {
			return 0;
		} else {
			return getActualCacheLimit() - getCacheSize();
		}
	}
	
	
	/**
	 * @return The last known size of the cache in bytes. <b>Included in the cache size is the database size, which can get very large for big lists.</b> 
	 * @see #addOnCacheSizeChangedListener(OnCacheSizeChangedListener)
	 */
	public long getCacheSize() {
		return cacheSizeUsed.get() + dbSizeApprox.get();
	}

	/**
	 * @return true if the user has set a cache limit, false if it is unlimited/unrestricted.
	 */
	public boolean isCacheLimitSet() {
		return getCacheLimit() > 0;
	}
	
	public boolean isCacheFull() {
		return getCacheSizeToTrim() > 0;
	}
	
	/**
	 * @return The cache limit set by the user or <= 0 if unlimited / not set. <b>Only used for using facing numbers.</b> 
	 * This is what the user perceives the cache limit to be. We should strongly avoid going over this.
	 * <b>See {@link #getActualCacheLimit()} for the actual internal cache limit.</b>
	 * 
	 * @see #getActualCacheLimit() 
	 */
	public long getCacheLimit() {
		return userSizeLimit.get();
	}
	
	/**
	 * The user's preference for what to keep if stuff has to be trimmed from the cache
	 * @return One of {@link Assets.CachePriority}
	 */
	public int getCacheLimitPriority() {
		return userCacheSort.get();
	}
	
	/**
	 * Sets the user's preferred limit.
	 * @param bytes The max bytes or 0 for unlimited
	 * @param sort One of {@link Assets.CachePriority}
	 */
	public void setCacheLimit(long bytes, int sort) {
		userSizeLimit.set(bytes);
		userCacheSort.set(sort);
		cacheState.onCacheLimitSettingsChanged();
	}
	
	public AndroidStorageLocation.Type getStorageType() {
		return storageType.get();
	}
	public Observable<AndroidStorageLocation.Type> getStorageTypeChanges() {
		return storageType.changes();
	}
	
	/**
	 * The actual usable cache space within {@link #getCacheLimit()} after including the {@link #CACHE_BUFFER}.
	 * <p>
	 * For example, if the user sets the limit to 500 MB, the actual usable space is 400 MB because of the 100 MB buffer.
	 * <h1>Why?</h1>
	 * <p>
	 * The buffer is used to ensure we never/rarely get close to the users limit and allows us to be more flexible
	 * with what we download rather than having to be overly strict.
	 * <p>
	 * Without a buffer, if the cache is full, up to the user's limit, if we download a temporary file, we would
	 * have to remove a cached file to make room for it. Then when the temp file is removed, the cached file would
	 * have to be redownloaded. Being that strict will make the implementation much more complex and make the app have to consume more data
	 * and cpu time. 
	 * <p>
	 * The buffer allows us to flex the cache and be more relaxed with downloading and cleaning without risking
	 * going over the user's requested limit.
	 * 
	 * @return The cache actual limit or <= 0 if unlimited / not set.
	 */
	public long getActualCacheLimit() {
		long limit = getCacheLimit();
		if (limit > 0) {
			return limit - CACHE_BUFFER;
		} else {
			return 0;
		}
	}
	
	/**
	 * Register a listener for updates to the cache size.
	 * @param listener
	 * @see #getCacheSize() to get the current value now
	 */
	public void addOnCacheSizeChangedListener(OnCacheSizeChangedListener listener) {
		synchronized (cacheStateLock) {
			cacheSizeListeners.add(listener);
		}
	}
	
	/**
	 * Remove a listener added via {@link #addOnCacheSizeChangedListener(OnCacheSizeChangedListener)}
	 * @param listener
	 */
	public void removeOnCacheSizeChangedListener(OnCacheSizeChangedListener listener) {
		synchronized (cacheStateLock) {
			cacheSizeListeners.remove(listener);
		}
	}
	
	public interface OnCacheSizeChangedListener {
		/**
		 * The known size of the offline cache has changed
		 * @param size The new size in bytes
		 */
		public void onCacheSizeChanged(long size);
	}

	public void unlockDownloading() {
		cacheState.onItemRemoved();
	}

	/**
	 * Writes the markup to disk and registers an item asset user for it.
	 */
	public void writeMarkup(Item item, PositionType view, String text, String charset) throws Exception {
		String path;
		if (view == PositionType.ARTICLE) {
			path = getAssetDirectory().pathForText(item);
		} else if (view == PositionType.WEB) {
			path = getAssetDirectory().pathForWeb(item);
		} else {
			throw new RuntimeException("unexpected view " + view);
		}
		registerAssetUser(path, AssetUser.forItem(item.time_added, item.idkey()));
		write(path, text, charset);
	}
	
	public void write(Asset asset, String text, String charset) throws Exception {
		write(asset.local.getAbsolutePath(), text, charset);
	}
	
	private void write(String localPath, String text, String charset) throws Exception {
		charset = charset != null ? charset : "UTF-8";
		File file = PktFileUtils.createFile(localPath);
		if (StringUtils2.equalsIgnoreCaseOneOf(charset, "UTF-8", "UTF8")) {
			// Java UTF-8 bug, fix:
			// From: http://tripoverit.blogspot.com/2007/04/javas-utf-8-and-unicode-writing-is.html
			byte[] outBytes = UnicodeUtils.convert(text.getBytes("UTF-16"), "UTF-8");
			IOUtils.write(outBytes, new FileOutputStream(file));
		} else {
			FileUtils.write(file, text, charset);
		}
		written(localPath, FileUtils.sizeOf(file));
	}
	
	public void write(Asset asset, byte[] data) throws IOException {
		File file = PktFileUtils.createFile(asset.local.getAbsolutePath());
		FileUtils.writeByteArrayToFile(file, data);
		written(asset, FileUtils.sizeOf(file));
	}
	
	public void written(Asset asset, long sizeInBytes) {
		written(asset.local.getAbsolutePath(), sizeInBytes);
	}
	
	public void written(String fileLocalPath, long sizeInBytes) {
		database.setBytes(fileLocalPath, sizeInBytes);
	}
	
	public void clean() {
		threads.async(cleaner::clean, null);
	}
	
	public List<AssetUser> getAssetUsers(String assetUserType) {
		return database.getAssetUsers(assetUserType);
	}
	
	public void addAssetUserCleaner(AssetUserCleanup cleanup) {
		cleaner.addAssetUserCleaner(cleanup);
	}
	
	public interface AssetUserCleanup {
		void cleanupAssetUsers(Assets assets);
	}

	/** See {@link AssetsDatabase#fixIdKeys} */
	@Deprecated
	public void fixIdKeys(Map<String, String> oldToNew, Set<String> markupOldKeys) throws InterruptedException {
		database.fixIdKeys(oldToNew, markupOldKeys);
		awaitAssetDatabaseChanges();
	}

	private class CacheCleaner {
		
		final List<AssetUserCleanup> cleanups = new ArrayList<>();
		
		synchronized void addAssetUserCleaner(AssetUserCleanup cleanup) {
			cleanups.add(cleanup);
		}
		
		/**
		 * Looks for asset users, assets and files that can be cleaned up and removed.
		 * synchronized so only one clean process will run at once and if you call it while another is running, it will run again after the current one runs.
		 */
		public synchronized void clean() throws Exception {
			// Clean up temp files created by the offline cache
			clearCleanDirectory();
			// Clean up the temp cache managed by Android. This is mostly temp files from the WebView
			FileUtils.deleteQuietly(appContext.getCacheDir());
			
			// Allow implementations to check if there are asset users to release
			for (AssetUserCleanup cleanup : cleanups) {
				cleanup.cleanupAssetUsers(Assets.this);
			}
			// Clean up any unused parent assets
			database.cleanUnusedParentAssets();
			
			// Delete any assets that no longer have asset users
			deleteFiles(database.cleanUnusedAssets());
			
			// Trigger the temp folder to clean up if it hasn't already in this session
			getTempDirectory();
			
			// If the cache is full, trim items until it is back under the limit
			if (isCacheFull()) {
				final AssetsDatabase.Trimmed trimmed = database.trim(getCacheSizeToTrim());
				deleteFiles(trimmed.assets);
				for (AssetUser user : trimmed.users) {
					for (CleanListener l : cleanListeners) {
						try {
							l.onTrimmed(user);
						} catch (Throwable t) {
							// Don't let implementations crash us
							Logs.printStackTrace(t);
						}
					}
				}
			}
		}
		
		private void deleteFiles(Collection<File> files) {
			TreeSet<File> dirs = new TreeSet<>(Collections.reverseOrder());
			for (File file : files) {
				try {
					File parent = file.getParentFile();
					FileUtils.deleteQuietly(file);
					if (parent != null) {
						dirs.add(parent);
					}
				} catch (Throwable unexpected) {
					// Not expected, but just fail quietly if it does so it doesn't prevent other files from being removed properly.
					Logs.printStackTrace(unexpected);
				}
			}
			// Remove any now empty directories
			for (File dir : dirs) {
				removeDirectoryIfEmpty(dir);
			}
		}
	
		/**
		 * If this directory is empty, delete it and any parent directories recursively that also will become empty.
		 * @param dir
		 */
		private void removeDirectoryIfEmpty(File dir) {
			if (dir == null) return;
			
			// Avoid deleting our own cache directory
			String path = dir.getAbsolutePath();
			AssetDirectory assetDir;
			try {
				assetDir = getAssetDirectory();
			} catch (AssetDirectoryUnavailableException e) {
				return; // Can't compare at the moment, so ignore for now.
			}
			if (path.equals(assetDir.getAssetsPath())
					|| path.equals(assetDir.getOfflinePath())) {
				// Don't remove these special directories
				return;
			}
			
			if (!dir.exists()) {
				// Already gone
				return;
			}
			
			String[] list = dir.list(); // Sadly, this is slow and there is no obvious alternative.
			if (list == null) {
				// Not expected because this method should only be called if it is a directory.
				Logs.throwIfNotProduction(dir + " is not a directory");
				FileUtils.deleteQuietly(dir); // On production just delete the file
				
			} else if (dir.list().length == 0) {
				// Empty, can delete.
				File parent = dir.getParentFile();
				FileUtils.deleteQuietly(dir);
				removeDirectoryIfEmpty(parent);
				
			} else {
				// Still unused, don't delete.
			}
		}
		
		/**
		 * Removes clean up directories in the current storage location and also
		 * scans all possible other storage locations and removes any remaining
		 * pocket files left over in those locations.
		 */
		public void clearCleanDirectory() {
			List<AndroidStorageLocation> locations = AndroidStorageUtil.getAll(appContext);
			
			for (AndroidStorageLocation location : locations) {
				if (!location.isAvailable()) {
					continue;
				}
				
				try {
					boolean isCurrentLocation = location.equalsIncludingPath(getAssetDirectory().getStorageLocation());
					
					File[] files = new File(location.getPath()).listFiles();
					if (files != null) {
						for (int i = 0; i < files.length; i++) {
							String path = files[i].getAbsolutePath();
							if (files[i].isDirectory()
									&& (!isCurrentLocation || path.contains(AssetDirectory.CLEANUP_FOLDER_NAME))) { // OPT check with AppCacheAssetManager for the actual location to get an exact match
								
								// If it is a clean up dir, or not the current location, we can delete it.
								FileUtils.deleteQuietly(files[i]);
							}
						}
					}
				} catch (Throwable t) {
					// Not really expecting this, but if there is an I/O error, just ignore for now, it will try again next time.
					Logs.printStackTrace(t);
				}
			}
		}
		
	}
	
	/**
	 * Creates a .nf file for the asset. This is used to mark a file as not available for download
	 * or to flag that it had problems resizing the image. It means to ignore the file.
	 *
	 * @param asset
	 * @return if the file was created
	 */
	public boolean makeNFFile(Asset asset) {
		try {
			FileUtils.forceMkdir(asset.local.getParentFile());
			new File(asset.local.getAbsolutePath() + ".nf").createNewFile();
			// TODO this is not tracked by assets ... which means they will never be cleaned up. is there a better way to track these?
			return true;
			
		} catch (IOException e) {
			Logs.printStackTrace(e);
		}
		return false;
	}
	
	/**
	 * Has this asset been marked as not found / invalid?
	 * @param asset
	 * @return
	 */
	public boolean isNF(Asset asset) {
		return new File(asset.local.getAbsolutePath() + ".nf").exists();
	}
	
	/**
	 * Check if the assets cache storage directory is setup, available and ready for use and prompt the user if there are problems.
	 * See {@link StorageErrorResolver} for more details.
	 * @param callback
	 */
	public void checkForStorageIssues(AbsPocketActivity activity, StorageErrorResolver.Callback callback) {
		storageErrorResolver.resolve(this, callback, activity);
	}
	
	/**
	 * @return A user facing string that can be appended to a help email to describe what their current settings are related to cache limits, storage, etc.
	 */
	public String getHelpInfo() {
		String info = "";
		info += "Storage Type: " + storageType.get() + "\n";
		if (storageType.get() == AndroidStorageLocation.Type.REMOVABLE) {
			info += "Storage Location: " + StringUtils.defaultString(removableStoragePath.get(), "Unknown") + "\n";
		}
		info += "Cache Size Limit: " + (getCacheLimit() > 0 ? BytesUtil.bytesToCleanString(App.getContext(), getCacheLimit()) : "No Limit") + "\n";
		info += "Cache Size: " + BytesUtil.bytesToCleanString(App.getContext(), App.getApp().assets().getCacheSize()) + "\n";
		return info;
	}
	
}
