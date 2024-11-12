package com.pocket.sdk.offline.cache;

import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.util.file.AndroidStorageLocation;

import java.io.File;
import java.io.IOException;

/**
 * Describes which directory {@link Asset}s are stored in and within that, manages the file names and directory structure.
 * Use its various methods to get the file names of where certain {@link Asset}s should be written or read from.
 */
public class AssetDirectory {

	/*
		To help visualize, this is the current structure:
		
		RIL_offline					:  All assets are found within this directory. They should remain here until they no longer have AssetUsers.
		RIL_offline/RIL_pages		:  .html files for web and article views are stored here.
		RIL_offline/RIL_assets		:  All image and css files are stored here.
		RIL_clean_up				:  When files are ready to be deleted or cleaned up they are moved into here.
		RIL_temp					:  When files are only for temporary use, they will be stored here and can be cleaned up at the end of a session.
	 */
	private static final String DEFAULT_FOLDER_NAME = "RIL_offline";
	private static final String PAGES_FOLDER_NAME = "RIL_pages";
	private static final String ASSETS_FOLDER_NAME = "RIL_assets";
	private static final String TEMP_FOLDER_NAME = "RIL_temp";
	protected static final String CLEANUP_FOLDER_NAME = "RIL_clean_up";

	private final AndroidStorageLocation mLocation;
	
	private final String mSeparator;

	/**
	 * Root directory of Pocket's storage directory. All other directories
	 * used in this class have this as its parent or some grand parent.
	 * This directory is a user preference. It may also be in removable
	 * storage and could be unavailable at any time. It also can change
	 * from app load to app load, so do not use hard coded paths.
	 */
	private final String mDirectoryRoot;
	/**
	 * Offline cache files and downloaded app assets. This directory
	 * can be cleared by the user through Settings. It is also
	 * cleared at logout and other times.
	 */
	private final String mDirectoryOfflineCache;
	/**
	 * Location of offline cache's markup files. See {@link com.pocket.sdk.offline.OfflineDownloading}
	 */
	private final String mDirectoryOfflineCachePages;
	/**
	 * Location of assets/files used through out the app. See {@link Asset}.
	 */
	private final String mDirectoryOfflineCacheAssets;
	/**
	 * Large files or directories to be removed asynchronously.
	 * Mostly will be cleared or removed offline caches pending deletion.
	 */
	private final String mDirectoryCleanup;
	/**
	 * A directory that can be used for temporary files. Contents will be automatically deleted at the start of each app instance.
	 */
	private final String mDirectoryTemp;

	/**
	 * @throws IOException If the location is unavailable or could not be found.
	 */
	public AssetDirectory(AndroidStorageLocation location) throws AssetDirectoryUnavailableException {
		mLocation = location;

		mSeparator = File.separator;
		mDirectoryRoot = mLocation.getPath();
		mDirectoryOfflineCache = mDirectoryRoot + mSeparator + DEFAULT_FOLDER_NAME;
		mDirectoryOfflineCachePages = mDirectoryOfflineCache + mSeparator + PAGES_FOLDER_NAME;
		mDirectoryOfflineCacheAssets = mDirectoryOfflineCache + mSeparator + ASSETS_FOLDER_NAME;
		mDirectoryCleanup = mDirectoryRoot + mSeparator + CLEANUP_FOLDER_NAME;
		mDirectoryTemp = mDirectoryRoot + mSeparator + TEMP_FOLDER_NAME;
	}

	public String getRoot() {
		return mDirectoryRoot;
	}

	/**
	 * The absolute path to the directory containing everything managed by {@link Assets}.
	 */
	public String getOfflinePath() {
		return mDirectoryOfflineCache;
	}
	
	public String getCleanupPath() {
		return mDirectoryCleanup;
	}
	
	public String getAssetsFolderName() {
		return ASSETS_FOLDER_NAME;
	}
	
	public String getTempDirectory() {
		return mDirectoryTemp;
	}
	
	/**
	 * The absolute path to the directory containing html files for this item.
	 */
	public String folderPathFor(Item item) {
		return folderPathFor(item.idkey());
	}
	
	/** The directory that contains all of the item's markup files. Within this each item is represented by a directory with its {@link Item#idkey()} as the name of the folder.*/
	public File getMarkupDirectory() {
		return new File(mDirectoryOfflineCachePages);
	}
	
	/**
	 * The absolute path to the directory containing html files for this item.
	 */
	public String folderPathFor(String itemIdKey) {
		return new File(getMarkupDirectory(), itemIdKey).getAbsolutePath();
	}
	
	/**
	 * The absolute file path and name for where this items web view html is/should-be stored.
	 */
	public String pathForWeb(Item item) {
		return folderPathFor(item) + mSeparator + "web.html";
	}
	
	/**
	 * The absolute file path and name for where this items article view html is/should-be stored.
	 */
	public String pathForText(Item item) {
		return folderPathFor(item) + mSeparator + "text.html";
	}
	
	/**
	 * The absolute to the directory where image and css assets are stored.
	 */
	String getAssetsPath() {
		return mDirectoryOfflineCacheAssets;
	}

	// Cleanup Methods
	
	public boolean isOfflineCacheMissing() {
		return !new File(mDirectoryOfflineCache).exists();
	}

	public AndroidStorageLocation getStorageLocation() {
		return mLocation;
	}
}

