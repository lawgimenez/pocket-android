package com.pocket.sdk.offline.downloader;

/** Long running operations can check this value periodically to see if it was cancelled and should stop. */
public interface Cancel {
	boolean isCancelled();
}
