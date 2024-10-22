package com.pocket.sdk.offline.cache;

import com.pocket.app.App;
import com.pocket.util.prefs.BooleanPreference;

/**
 * Keeps track of whether the cache is under or over a limit set by the user.
 * <p>
 * It also keeps track of whether downloading is "locked". This lock is to prevent endless loops where
 * you are downloading, go over the limit, trigger a cache clean, go under the limit and then
 * restart downloading to fill the space, only to go over again and restart the cycle.
 * <p>
 * The lock is turned on when a cache limit is reached and then turned off when either an item is removed (archived, deleted, etc)
 * or when cache settings change.
 */
public class CacheState {

	private final BooleanPreference locked;
	private final Assets assets;
	
	private boolean mIsInitialized = false;
	private boolean mIsOverLimit = false;
	private boolean mIsOverDownloadStartBuffer = false;
	
	protected CacheState(Assets assets, BooleanPreference lockPref) {
		this.assets = assets;
		this.locked = lockPref;
	}
	
	/**
	 * Something related to cache state has changed, recheck the state and update as needed.
	 */
	protected synchronized void invalidate() {
		long spaceRemaining = assets.getCacheSpaceRemaining();
		boolean isOverLimit = spaceRemaining == 0;
		boolean isOverDownloadStartBuffer = spaceRemaining < Assets.CACHE_RESTART_DOWNLOADING_BUFFER;
		
		if (!mIsInitialized) {
			// This is just the first call Assets.start() makes to setup the initial state during app loading.
			mIsOverLimit = isOverLimit;
			mIsOverDownloadStartBuffer = isOverDownloadStartBuffer;
			mIsInitialized = true;
			return;
		}
		
		if (isOverLimit == mIsOverLimit && isOverDownloadStartBuffer == mIsOverDownloadStartBuffer) {
			// No change
			return;
		}
		
		mIsOverLimit = isOverLimit;
		mIsOverDownloadStartBuffer = isOverDownloadStartBuffer;
		
		if (!isOverLimit) {
			// Now under limit, or under download start buffer
			if (!isOfflineDownloadingRestricted()) {
				restartDownloading();
			}
			
		} else {
			// Now over limit
			setOfflineDownloadingLocked(true);
			if (assets.getCacheSizeToTrim() >= Assets.CACHE_BUFFER * 0.75) {
				// Start the cleaning immediately instead of waiting until the user leaves the app.
				assets.clean();
			}
		}
	}
	
	/**
	 * @return true if the cache is full or over the limit.
	 */
	protected boolean isOverLimit() {
		return mIsOverLimit;
	}
	
	/**
	 * @see Assets#isOfflineDownloadingRestricted()
	 */
	protected boolean isOfflineDownloadingRestricted() {
		if (!assets.isCacheLimitSet()) {
			return false;
		}
		return isOverLimit() 
			|| locked.get()
			|| assets.getCacheSpaceRemaining() < Assets.CACHE_RESTART_DOWNLOADING_BUFFER;
	}
	
	/**
	 * Invoke if item data was removed.
	 */
	protected void onItemRemoved() {
		if (!isOverLimit()) {
			setOfflineDownloadingLocked(false);
		}
	}
	
	/**
	 * Invoke if user's cache settings/preferences change
	 */
	protected void onCacheLimitSettingsChanged() {
		if (!isOverLimit()) {
			setOfflineDownloadingLocked(false);
		}
	}
	
	/**
	 * Downloading is no longer restricted, start it up again to fill up remaining space in the cache.
	 */
	private void restartDownloading() {
		App.getApp().offline().predownload();
	}

	private void setOfflineDownloadingLocked(boolean locked) {
		this.locked.set(locked);
		if (!isOfflineDownloadingRestricted()) {
			restartDownloading();
		}
	}
	
}
