package com.pocket.sdk.offline.cache;

import com.pocket.sdk.util.AbsPocketActivity;

/**
 * This means that the directory that the user's offline cache and Pocket assets are
 * stored on is not currently available or the directory is missing or corrupted.
 * If you want to prompt the user to resolve this error, use {@link Assets#checkForStorageIssues(AbsPocketActivity, StorageErrorResolver.Callback)}
 */
@SuppressWarnings("serial")
public class AssetDirectoryUnavailableException extends Exception {

	public AssetDirectoryUnavailableException(String message){
		super(message);
	}
	
}
