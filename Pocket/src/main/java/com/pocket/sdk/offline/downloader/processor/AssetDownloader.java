package com.pocket.sdk.offline.downloader.processor;

import com.pocket.sdk.image.Image;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.downloader.Cancel;
import com.pocket.sdk.offline.downloader.WebDownloader;
import com.pocket.util.java.KeyLatch;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper for a {@link WebDownloader} to manage downloading all of its assets.
 * Invoke {@link #download(Asset, AssetUser, Cancel)} for an additional assets it needs.
 * Then invoke {@link #await(long, KeyLatch.CheckIn)} to wait until all of those assets have a result.
 */
public class AssetDownloader  {
	
	private final Assets assets;
	private final boolean refresh;
	private final WebDownloader.Downloader downloader;
	private final long maxFileSize;
	private final Map<Asset, Status> results = new HashMap<>();
	private final Set<Asset> stylesheets = new HashSet<>();
	private final Set<Asset> images = new HashSet<>();
	private final int maxStylesheets;
	private final int maxImages;
	private final KeyLatch latch = new KeyLatch(true);
	private final WebDownloader.Worker workers;
	
	/**
	 * @param refresh If true it will always redownload/process assets, even if they are already found on disk. If false, if the asset has already been downloaded it will mark it as {@link Status#DOWNLOADED} and leave as is.
	 * @param maxFileSize The maximum file size of any asset allowed. If the asset is larger than this it will mark it as {@link Status#FAILED_PERMANENT}
	 * @param maxStylesheets The maximum number of stylesheets allowed to be downloaded for this single web downloader. Any stylesheets requested after reaching this maximum will be {@link Status#FAILED_PERMANENT}.
	 * @param maxImages The maximum number of images allowed to be downloaded for this single web downloader. Any images requested after reaching this maximum will be {@link Status#FAILED_PERMANENT}.
	 */
	public AssetDownloader(Assets assets, boolean refresh, long maxFileSize, int maxStylesheets, int maxImages, WebDownloader.Worker workers, WebDownloader.Downloader downloader) {
		this.assets = assets;
		this.refresh = refresh;
		this.downloader = downloader;
		this.workers = workers;
		this.maxFileSize = maxFileSize;
		this.maxStylesheets = maxStylesheets;
		this.maxImages = maxImages;
	}
	
	/**
	 * Register this asset user and download the asset. Also processes the asset if needed to find additional assets.
	 * This may be asynchronous or blocking.
	 * @param asset The asset to download
	 * @param user The user to register for this asset.
	 * @param cancel Something to check if this work should continue
	 */
	public synchronized void download(Asset asset, AssetUser user, Cancel cancel) {
		// Have we already handled this asset?
		if ((asset.type == Asset.IMAGE && images.contains(asset))
		 || (asset.type == Asset.STYLESHEET && stylesheets.contains(asset))) {
			assets.registerAssetUser(asset, user);
			return;
		}
		
		// Have we reached the maximum for this asset type?
		if ((asset.type == Asset.IMAGE && images.size() > maxImages) || (asset.type == Asset.STYLESHEET && stylesheets.size() > maxStylesheets)) {
			results.put(asset, Status.FAILED_PERMANENT);
			return;
		}
		
		assets.registerAssetUser(asset, user);
		latch.hold(asset); // Note: After a hold is placed we must be careful to catch all cases and ensure that onAssetResult is invoked eventually. Otherwise it will never release.
		results.put(asset, null);
		if (!refresh && asset.local.exists()) {
			onAssetResult(asset, Status.DOWNLOADED);
			
		} else {
			if (asset.type == Asset.IMAGE) {
				images.add(asset);
				Image.build(asset, user)
						.callbackThread(Image.CallbackThread.BACKGROUND)
						.cache((request, result) -> onAssetResult(asset, result == Image.Result.SUCCESS ? Status.DOWNLOADED : (result == Image.Result.FAILED_PERMANENTLY ? Status.FAILED_PERMANENT : Status.FAILED_RETRYABLE)));
				
			} else if (asset.type == Asset.STYLESHEET) {
				stylesheets.add(asset);
				String url = asset.url.toString();
				workers.work(() -> {
					WebDownloader.Result result = downloader.download(url, (endUrl, stream, mimeType, charset) -> {
						if (!StringUtils.containsIgnoreCase(mimeType, "text/css")) return new WebDownloader.PermanentFailure();
						AssetHandler handler = AssetHandler.forPageAsset(asset, assets.getAssetDirectory(), (a, u) -> download(a, u, cancel)); // TODO should we use endUrl here? Otherwise could the urls be incorrect?
						final StreamingMarkupProcessor.Result processingResult = new StreamingMarkupProcessor(url, cancel, assets.getTempDirectory(), asset.local, maxFileSize, handler)
								.processStylesheet(stream.okioBuffer());
						if (processingResult instanceof StreamingMarkupProcessor.Success) {
							final StreamingMarkupProcessor.Success
									success = (StreamingMarkupProcessor.Success) processingResult;
							assets.written(asset, success.size);
							return new WebDownloader.Success(mimeType, charset);
						} else {
							return new WebDownloader.PermanentFailure();
						}
					});
					if (result instanceof WebDownloader.Success) {
						onAssetResult(asset, Status.DOWNLOADED);
					} else if (result instanceof WebDownloader.PermanentFailure) {
						onAssetResult(asset, Status.FAILED_PERMANENT);
					} else {
						onAssetResult(asset, Status.FAILED_RETRYABLE);
					}
				});
				
			} else {
				onAssetResult(asset, Status.FAILED_PERMANENT);
			}
		}
	}
	
	private synchronized void onAssetResult(Asset asset, Status status) {
		latch.release(asset);
		results.put(asset, status);
	}
	
	/**
	 * Blocks until all assets have results and returns the results.
	 * If the thread is interrupted or the latch return false, returns null.
	 * See {@link KeyLatch#await(long, KeyLatch.CheckIn)} for parameter details
	 */
	public Map<Asset, Status> await(long timeout, KeyLatch.CheckIn checkIn) {
		try {
			if (latch.await(timeout, checkIn)) {
				synchronized (this) {
					return new HashMap<>(results);
				}
			} else {
				return null;
			}
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	public enum Status {
		DOWNLOADED,
		FAILED_RETRYABLE,
		FAILED_PERMANENT
	}
	
	
}
