package com.pocket.sdk.offline.cache;


/**
 * A declaration of how important the download is. Used by {@link Assets#isDownloadAuthorized(DownloadAuthorization)} to 
 * determine if the download should be allowed based on the current size of the cache and any limits imposed on it.
 */
public enum DownloadAuthorization {
	/**
	 * If the download is explicitly requested by the user or is waiting to appear, use this
	 * authorization to ensure it is always downloaded.
	 */
	ALWAYS,
	/**
	 * If the download is simply to make available for offline use later, is being done in the background,
	 * or is low priority, use this authorization and it will only be downloaded if the cache is not full.
	 */
	ONLY_WHEN_SPACE_AVAILABLE
}
	