package com.pocket.sdk.image;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.app.settings.UserAgent;
import com.pocket.sdk.http.CookieDelegate;
import com.pocket.sdk.http.HttpClientDelegate;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.sdk.network.eclectic.EclecticHttpUtil;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.util.java.Logs;
import com.pocket.util.android.drawable.BitmapLruCache;
import com.pocket.util.android.drawable.CacheableBitmapWrapper;
import com.pocket.util.android.drawable.MemoryAwareBitmapFactory;
import com.pocket.util.android.thread.TaskPool;
import com.pocket.util.java.BytesUtil;
import com.pocket.util.java.FileLocks;
import com.pocket.util.java.PktFileUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Performs {@link Image.Request}s which download, store and resize images.
 * Do not use directly, instead use the {@link Image} api.
 */
@Singleton
public final class ImageCache implements AppLifecycle {
	
	public static final boolean DEBUG = false;
	private static final long MAX_FILE_SIZE = BytesUtil.mbToBytes(4); // REVIEW if we can increase this? but it was copied over from FileDownloader during the refactor
	
	private final AppThreads threads;
	private final Assets assets;
	private final UserAgent userAgent;
	private final FileLocks fileLocks;
	private final BitmapLruCache cache;
	/** Handles the first pass at looking at a request and deciding what to do. */
	private final TaskPool routing;
	/** Handles resizing already downloaded images. */
	private final TaskPool resizing;
	/** Handles downloading and writing images. */
	private final TaskPool downloading;
	private final HttpClientDelegate http;
	private final CookieDelegate cookies;

	@Inject
	public ImageCache(
			AppThreads threads,
			Assets assets,
			UserAgent userAgent,
			HttpClientDelegate http,
			@ApplicationContext Context context,
			CookieDelegate cookies,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.http = http;
		this.threads = threads;
		this.assets = assets;
		this.userAgent = userAgent;
		this.cookies = cookies;
		
		fileLocks = new FileLocks();
		
		final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
	    // Use 1/5th of the available memory for this memory cache.
		int fraction = memClass > 24 ? 5 : 8;
	    final int cacheSize = 1024 * 1024 * memClass / fraction;
	    cache = new BitmapLruCache(cacheSize);
		
		routing = threads.newPriorityPool("img-route", 5);
	    resizing = threads.newPriorityPool("img-resize", 2);
		downloading = threads.newPriorityPool("img-download", 4);
	}
	
	@Override
	public void onLowMemory() {
		trim();
	}
	
	/**
	 * Start building a new request for an image.
	 */
	public Image.Builder build(String srcUrl, AssetUser user) {
		return Image.build(srcUrl, user, this, threads);
	}
	
	/**
	 * Start building a new request for an image.
	 */
	public Image.Builder build(Asset src, AssetUser user) {
		return Image.build(src, user, this, threads);
	}
	
	/**
	 * Checks if the image is already on disk, safely using locks to avoid resizing/caching partial images that are in the process of being written.
	 */
	private boolean exists(Asset asset) {
		FileLocks.Lock lock = null;
		try {
			lock = fileLocks.lock(asset.local);
			return asset.local.exists();
		} catch (Throwable ignore) {
			return asset.local.exists(); // If interrupted just fallback to just checking without the lock
		} finally {
			if (lock != null) lock.release();
		}
	}
	
	/**
	 * Make an image request. If your ImageRequest is set to return bitmap, it will check if the
	 * image is cached in memory. If so it will return it with setBeingUsed() already called on your behalf.
	 * <p>
	 * Do not use directly, use {@link Image} for getting images.
	 * <p>
	 * Otherwise it will return null and asynchronously fetch the image based on your request settings.
	 * Whether it calls back also depends on the settings in the request.
	 * @return If in the memory cache, the image, otherwise null if it has to load it.
	 */
	CacheableBitmapWrapper getImage(Image.Request request) {
		if (request.returnBitmap) {
			CacheableBitmapWrapper wrapper = cache.get(request.assetSizedPath);
			if (wrapper != null && wrapper.hasValidBitmap()) {
				if (DEBUG) Logs.i("ImageCache", "getImage returning cached" + request.toString());
				wrapper.setBeingUsed(true);
				return wrapper;
			}
		}
		
		routing.submit(new ImageTask(request) {
			
			@Override
			protected void backgroundOperation() {
				try {
					Asset asset = request.asset;
					// Note: ImageResizer's writing method handles registering an AssetUser
					if (asset == null) {
						// Invalid request
						callback(request, Image.Result.FAILED_PERMANENTLY);
						
					} else if (assets.isNF(asset)) {
						if (DEBUG) Logs.i("ImageCache", "request is .nf" + request.toString());
						// File has been flagged to be ignored.
						callback(request, Image.Result.FAILED_PERMANENTLY);
					
					} else if (exists(asset) && !request.refresh) {
						if (request.resize != null) {
							if (ImageResizer.isImageResized(request)) {
								callback(request, Image.Result.SUCCESS);
							} else {
								resizing.submit(new ImageResizer(request, (r, success) -> callback(request, success ? Image.Result.SUCCESS : Image.Result.FAILED)));
							}
						} else {
							assets.registerAssetUser(asset, request.assetUser);
							callback(request, Image.Result.SUCCESS);
						}
					
					} else if (request.tryFetchFromSource && assets.isDownloadAuthorized(request.downloadAuthorization)) {
						EclecticHttp client = http.getClient();
						String url = ImageCacheHelper.convertToPocketImageCacheUrl(asset.url.toString());
						EclecticHttpRequest httpRequest = client.buildRequest(url)
								.setHeader("User-Agent", userAgent.preferred())
								.setHeader("Accept-Encoding", "gzip"); // REVIEW not sure if we need this, but it was part of FileDownloader before the refactor that setup this new code.
						cookies.addCookiesToRequest(httpRequest, client);
					
						downloading.submit(new ImageTask(request) {
							
							@Override
							protected void backgroundOperation() {
								try {
									Integer result = (Integer) client.get(httpRequest, (in, response) -> {
										switch (response.getStatusCode()) {
											case 200:
												if (EclecticHttpUtil.getContentLength(response) > MAX_FILE_SIZE) return -1;
												FileLocks.Lock lock = null;
												try {
													// Note: Since this buffers to disk, it will hold a lock that waits for both network and disk activity
													// This does mean that if multiple requests for the same image occur at the same time, it could end up holding up the thread queue waiting
													// for the lock to release, but until we see that use case actually happen and cause slow downs, leaving this implementation as is.
													lock = fileLocks.lock(asset.local);
													if (asset.local.exists()) return 1; // Must have already been downloaded in between the last check from another request
													return writeImage(asset, request.assetUser, in.okioBuffer(), MAX_FILE_SIZE) ? 1 : -1;
												} finally {
													if (lock != null) lock.release();
												}
											case 404:
											case 403:
											case 301: // REVIEW why is this an invalid? why not redirect?
												return -1;
											default:
												return 0;
										}
									}).getResponse();
								
									if (result == 1) {
										try {
											if (request.resize != null) {
												if (ImageResizer.isImageResized(request)) {
													callback(request, Image.Result.SUCCESS);
												} else {
													resizing.submit(new ImageResizer(request, (r, success) -> callback(request, success ? Image.Result.SUCCESS : Image.Result.FAILED)));
												}
											} else {
												callback(request, Image.Result.SUCCESS);
											}
										} catch (Throwable catchall) {
											// Make sure we call back no matter what.
											callback(request, Image.Result.FAILED);
										}
									} else if (result == -1) {
										assets.makeNFFile(asset);
										callback(request, Image.Result.FAILED_PERMANENTLY);
									} else {
										callback(request, Image.Result.FAILED);
									}
								} catch (Throwable catchall) {
									// Make sure we call back no matter what.
									callback(request, Image.Result.FAILED);
								}
							}
						});
					} else {
						if (DEBUG) Logs.i("ImageCache", "request doesn't exist but not authorized to download" + request.toString());
						callback(request, Image.Result.FAILED);
					}
				} catch (Throwable catachall) {
					// Make sure we call back no matter what.
					callback(request, Image.Result.FAILED);
				}
			}
		});
		return null;
	}
	
	/**
	 * Writes an image to disk from a stream and registers assets/users
	 * @param asset Info about this image
	 * @param assetUser The asset user to register for this image if successfully written
	 * @param source The data stream. This will NOT be automatically closed.
	 * @param maxSize The maximum number of bytes to allow, if the stream ends up being larger than this it will cancel the write and return false
	 * @return true if written, false if larger than maxSize
	 * @throws IOException If an error occurs
	 */
	private boolean writeImage(Asset asset, AssetUser assetUser, BufferedSource source, long maxSize) throws IOException {
		File file = PktFileUtils.createFile(asset.local.getAbsolutePath());
		
		BufferedSink sink = Okio.buffer(Okio.sink(file));
		Buffer buffer = sink.buffer();
		
		// This code mimics the internals of BufferedSink.writeAll() but allows us to cancel if too large
		long totalBytesRead = 0;
		try {
			long readCount;
			do {
				readCount = source.read(buffer, 8192L);
				sink.emitCompleteSegments();
				totalBytesRead += readCount;
			} while (readCount != -1 && totalBytesRead < maxSize);
			sink.flush();
		} finally {
			sink.close();
		}
		
		if (totalBytesRead > maxSize) {
			FileUtils.deleteQuietly(file);
			return false;
		} else {
			assets.registerAssetUser(asset, assetUser);
			assets.written(asset, totalBytesRead);
			return true;
		}
	}
	
	private void callback(final Image.Request request, Image.Result result) {
		final Image.ImageReadyCallback callback = request.callback;
		if (callback != null && !callback.isImageRequestStillValid(request)) {
			if (DEBUG) Logs.i("ImageCache", "request invalid" + request.toString());
			// request is no longer needed, so we don't need to allocate or cache the bitmap for now.
			
		} else {
			if (DEBUG) Logs.i("ImageCache", "request calling back" + request.toString());
			
			// Load bitmap for return if needed
			Bitmap bitmap;
			final CacheableBitmapWrapper wrapper;
			if (request.returnBitmap && callback != null && result == Image.Result.SUCCESS) {
				bitmap = MemoryAwareBitmapFactory.decodeQuietly(request.assetSizedPath, null);
				if (bitmap != null) {
					wrapper = new CacheableBitmapWrapper(bitmap, request.assetSizedPath);
					cache.cache(request.assetSizedPath, wrapper);
				} else {
					wrapper = null;
				}
			} else {
				wrapper = null;
			}
			
			// Perform callback if needed
			if (callback != null) {
				if (wrapper != null) {
					wrapper.setBeingUsed(true);
				}
				callback.onImageRequestFinished(request, result, wrapper);
			}
		}
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override
			public void stopModifyingUserData() {
				routing.cancelAllUntilEmpty();
				downloading.cancelAllUntilEmpty();
				resizing.cancelAllUntilEmpty();
			}

			@Override public void deleteUserData() {
				cache.evictAll();
			}

			@Override public void restart() {}

			@Override public void onLoggedOut() {}
		};
	}

	/**
	 * Removes cached images that are not currently being displayed
	 */
	public void trim() {
		if (cache != null) {
			cache.trimMemory();
		}
	}
	
	public FileLocks getImageFileLocks() {
		return fileLocks;
	}
	
}

