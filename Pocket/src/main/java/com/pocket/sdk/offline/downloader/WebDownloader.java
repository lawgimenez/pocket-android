package com.pocket.sdk.offline.downloader;

import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.http.CookieDelegate;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.sdk.network.eclectic.EclecticHttpUtil;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetDirectory;
import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.downloader.processor.AssetDownloader;
import com.pocket.sdk.offline.downloader.processor.AssetHandler;
import com.pocket.sdk.offline.downloader.processor.StreamingMarkupProcessor;
import com.pocket.sdk.premium.PermanentLibraryUtil;
import com.pocket.util.java.Logs;
import com.pocket.util.java.BytesUtil;
import com.pocket.util.java.Milliseconds;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.util.Map;

/**
 * Downloads a web page for offline viewing.
 * <p>
 * The HTML markup, which can be opened to view it, will be saved to {@link AssetDirectory#pathForWeb(Item)}.
 * It will also download any images and stylesheets needed to display the page. Those will be stored as {@link Asset}s and will use {@link Asset#local} for their location.
 * See {@link Assets} for more details on file storage.
 * <p>
 * The html markup will be rewritten in order to reference those images and stylesheet assets as relative links to their location.
 * <p>
 * Web downloading follows this general pattern:
 * <ol>
 *     <li>Download the markup of the item's url</li>
 *     <li>Process that markup to find any assets to download, and rewrite their links to point to their relative position of where they will be stored locally</li>
 *     <li>Download those assets and write them to disk</li>
 *     <li>If the asset is more markup (stylesheets), recursively continue this processing, again reading the markup to find more assets, downloading and processing them.</li>
 *     <li>Once all assets have been downloaded, written and processed, the web download is complete</li>
 * </ol>
 *
 * <h2>Errors</h2>
 * When downloading web pages and all of its files there is a lot that can go wrong, for example:
 * <ul>
 *     <li>The device may be not be online, or have a poor or intermittent connection</li>
 *     <li>The device may be connected but there is some firewall local or otherwise blocking of all or specific sites</li>
 *     <li>The web page may not exist anymore such as a 404 error</li>
 *     <li>The web page may just be temporarily having issues</li>
 *     <li>The web page may just be invalid or broken and return any number of errors or the network may just time out</li>
 * </ul>
 * When looking at the errors that java and the network can return to us, it will not always be clear if it is failing because it is permanently
 * broken and unavailable, if the server might fix itself later, or if its just the device network and we can try again later.
 * We could analyze the network to see if it is connected or why it might be failing but that won't give us absolute certainty of why one request failed
 * unless there is a clear status code, but in may cases may not be.
 * <p>
 * This uncertainty applies to the initial web page load, as well as all of its assets. Some of the assets found may not exist and may not
 * be important to the page, or they may be critical to it. So in many cases the result may leave us with a good bit of uncertainty of what {@link OfflineStatus}
 * to assign it at the end of this process.
 * <p>
 * To help you decide, this will return several different types {@link Result}. See each subclass for recommendations of what status to assign and how to handle.
 * <p>
 * See {@link #download(Item, boolean, Assets, boolean, EclecticHttp, CookieDelegate, Worker, Cancel)} to use.
 */
public class WebDownloader {
	
	private static final int MAX_IMAGES_PER_ITEM = 150;
	private static final int MAX_STYLESHEETS_PER_ITEM = 20;
	private static final long MAX_FILE_SIZE_PER_FILE = BytesUtil.mbToBytes(4); // REVIEW seems like we could support more than 4?
	
	/**
	 * An interface to run work that could benefit from being run as tasks in a thread pool, but doesn't strictly have to.
	 * This leave it up to the calling implementation to decide.
	 * Up to the implementation, these could just run them immediately on their current thread, run them on another thread or submit to a thread pool.
	 * Using a thread pool will allow the web downloader to complete faster and more efficiently.
	 * Tasks must be always invoked in some way but order and threads don't matter.
	 */
	public interface Worker {
		void work(Runnable r);
	}
	
	/**
	 * Perform the web download process for this item and return a result when complete. See main {@link WebDownloader} docs for more details.
	 * All parameters are required.
	 * @param item The item describing the web page to download.
	 * @param refresh If true, then any assets that are already downloaded will be redownloaded, if false, if an asset is already on disk it will use that.
	 * @param assets Provides information about where to write files
	 * @param usePermanentLibrary true to download from the Pocket user's Permanent Library (a premium feature) or false to download from the original web site. If 'refresh' is true, this value will be ignored and it will always download from the web.
	 * @param http Access to the internet
	 * @param cookies A Cookie Store to use
	 * @param workers See {@link Worker} for more details
	 * @param cancel This can be a very long running process. It will occasionally check this value to see if it should continue. If it is cancelled it will stop processing. It makes no attempts to clean up files already downloaded. This is just meant to help interrupt this process if needed.
	 * @return A status reflecting the result of the operation
	 * @throws AssetDirectoryUnavailableException If such an exception is thrown by any of the internal processes related to storing the markup file.
	 */
	public static Result download(Item item, boolean refresh, Assets assets, boolean usePermanentLibrary, EclecticHttp http, CookieDelegate cookies, Worker workers, Cancel cancel) throws Exception {
		if (cancel.isCancelled()) return new Cancelled();
		
		// Create a downloader that will handle network requests
		Downloader downloader = new DownloaderImpl(http, cookies, cancel);
		
		// Setup an asset downloader which will handle downloading any additional assets needed, like images and stylesheets
		AssetDownloader assetsDownloader = new AssetDownloader(assets, refresh, MAX_FILE_SIZE_PER_FILE, MAX_STYLESHEETS_PER_ITEM, MAX_IMAGES_PER_ITEM, workers, downloader);
				
		// Register the asset user for the markup
		AssetUser assetUser = AssetUser.forItem(item.time_added, item.idkey());
		assets.registerAssetUser(assets.getAssetDirectory().pathForWeb(item), assetUser);
		
		// Connect to the url and download, process, and save the page.
		// If it is html, the content will be processed to look for additional assets to download.
		// Those assets will be passed to the assetDownloader to download.
		// It will also modify the markup of those links to point to where they will live on disk.
		// If the url points to an image, the assetDownloader will be used to download it.
		String url;
		if (usePermanentLibrary && !refresh) {
			url = PermanentLibraryUtil.getLibraryWebViewUrl(item);
		} else {
			url = ItemUtil.unhashBang(item.open_url.url);
		}
		File output = new File(assets.getAssetDirectory().pathForWeb(item));
		Result markupResult = downloader.download(url, (endUrl, stream, mimeType, charset) -> {
			// Content is image?
			if (StringUtils.startsWithIgnoreCase(mimeType, "image/")) {
				Asset asset = Asset.createImage(url, assets.getAssetDirectory());
				if (asset == null) return new PermanentFailure();
				assetsDownloader.download(asset, assetUser, cancel);
				return new SuccessAsset(asset, mimeType);
			}
			// Content is markup?
			if (!StringUtils.containsIgnoreCase(mimeType, "text/")) return new PermanentFailure();
			AssetHandler handler = AssetHandler.forWebPage(endUrl, assetUser, assets.getAssetDirectory(), (a, u) -> assetsDownloader.download(a, u, cancel));
			final StreamingMarkupProcessor.Result result = new StreamingMarkupProcessor(endUrl, cancel, assets.getTempDirectory(), output, MAX_FILE_SIZE_PER_FILE, handler)
						.processHtml(charset, stream.okioBuffer());
			if (result instanceof StreamingMarkupProcessor.HtmlSuccess) {
				final StreamingMarkupProcessor.HtmlSuccess success = (StreamingMarkupProcessor.HtmlSuccess) result;
				assets.written(output.getAbsolutePath(), success.size);
				return new Success(mimeType, success.encoding);
			} else {
				return new PermanentFailure();
			}
		});
		
		// Handle failure cases
		if (cancel.isCancelled()) new Cancelled();
		if (markupResult instanceof Failure) return markupResult;
		if (markupResult instanceof PermanentFailure) return markupResult;
		
		// At this point if it was text based, it has been successfully downloaded,
		// its markup processed, and any additional assets have been passed
		// to the asset downloader. So next we'll wait until the asset downloader
		// finishes downloading everything.
		Map<Asset, AssetDownloader.Status> assetResults = assetsDownloader.await(5000, (elapsedStart, held, elapsedChange) -> {
			Logs.v("WebDownloader", "Long running downloader " + " " + elapsedStart + " " + elapsedChange + " " + url + " holding: " + held);
			if (elapsedChange > Milliseconds.minutesToMillis(5)) return false; // If things haven't changed in a long time, cancel. We could probably even make this timeout shorter.
			return !cancel.isCancelled();
		});
		if (assetResults == null) return new Failure();
		
		// If the url is an image, we need to confirm the image was downloaded successfully
		if (markupResult instanceof SuccessAsset) {
			AssetDownloader.Status result = assetResults.get(((SuccessAsset) markupResult).asset);
			if (result == AssetDownloader.Status.DOWNLOADED) return markupResult;
			if (result == AssetDownloader.Status.FAILED_PERMANENT) return new PermanentFailure();
			if (result == AssetDownloader.Status.FAILED_RETRYABLE) return new Failure();
			return new Failure(); // Not expected, but if some other status or missing, just fail
		}
		
		// If html or text, make sure we have all of the other assets.
		Success result = (Success) markupResult;
		if (assetResults.values().contains(AssetDownloader.Status.FAILED_RETRYABLE) || assetResults.values().contains(null)) { // Only look for non-permanent failures because if an asset has a permanent failure, retrying will never fix anything, so just mark it as successful, its the best we'll ever get. Web sites can reference missing images and stylesheets and still render fine, it might just be bad html on their side.
			return new Partial(result.mimeType, result.encoding);
		} else {
			return result;
		}
	}
	
	public interface Downloader {
		/**
		 * Connect to this url and pass its response to the streamer to handle.
		 * Only pass to the streamer if the status code is 200 and no other problems are found.
		 */
		Result download(String url, Streamer streamer);
	}
	public interface Streamer {
		/**
		 * Handle a download response for a url.
		 * @param endUrl See {@link EclecticHttp.Response#endUrl()}
		 * @param stream See {@link EclecticHttp.ResponseParser#readResponse(EclecticHttp.Stream, EclecticHttp.Response)}'s stream field.
		 * @param mimeType The reported mime-type of the response
		 * @param charset The reported mime-type of the response
		 * @return A result
		 */
		Result process(String endUrl, EclecticHttp.Stream stream, String mimeType, String charset) throws Exception;
	}
	
	private static class DownloaderImpl implements Downloader {
		private final EclecticHttp http;
		private final CookieDelegate cookies;
		private final Cancel cancel;
		
		private DownloaderImpl(EclecticHttp http, CookieDelegate cookies, Cancel cancel) {
			this.http = http;
			this.cookies = cookies;
			this.cancel = cancel;
		}
		
		@Override
		public Result download(String url, Streamer streamer) {
			try {
				EclecticHttpRequest r = http.buildRequest(url)
						.setHeader("User-Agent", App.getApp().userAgent().preferred())
						.setHeader("Accept-Encoding", "gzip");
				cookies.addCookiesToRequest(r, http);
				return (Result) http.get(r, (stream, response) -> {
					if (cancel.isCancelled()) return new Cancelled();
					switch (response.getStatusCode()) {
						case 200:
							if (NumberUtils.toLong(response.getHeader("Content-Length")) > MAX_FILE_SIZE_PER_FILE) return new PermanentFailure();
							EclecticHttpUtil.ContentType contentType = EclecticHttpUtil.getContentType(response);
							String mimeType = contentType != null && contentType.mimeType != null ? contentType.mimeType : "text/html";
							String charset = contentType != null ? contentType.encoding : null;
							if (StringUtils.containsAny(mimeType, "video", "audio")) return new PermanentFailure();
							return streamer.process(response.endUrl(), stream, mimeType, charset);
						case 404:
						case 403:
						case 301: // REVIEW why is this an invalid? why not redirect?
							return new PermanentFailure();
						default:
							return new Failure();
					}
				}).getResponse();
			} catch (Throwable t) {
				return new Failure();
			}
		}
	}
	
	
	/**
	 * The result of an attempt to download the web view for an item. See subclasses of this interface for potential types.
	 */
	public interface Result {}
	
	/**
	 * The download was cancelled by request. Leave the previous status as is. It is possible some assets have been downloaded.
	 */
	public static class Cancelled implements Result {}
	
	/**
	 * The page and all of its assets have been downloaded successfully.
	 * Assign it {@link OfflineStatus#OFFLINE}.
	 * This will also be the result if some of its assets failed but we know for certain those assets are permanently unavailable.
	 */
	public static class Success implements Result {
		final public String mimeType;
		final public String encoding;
		public Success(String mimeType, String encoding) {
			this.mimeType = mimeType;
			this.encoding = encoding;
		}
	}
	
	/**
	 * The page is not an html webpage, but instead is an asset like an image.
	 * It has been downloaded successfully.
	 * Can assign it {@link OfflineStatus#OFFLINE_AS_ASSET}.
	 * See {@link #mimeType} for the type of asset.
	 */
	public static class SuccessAsset implements Result {
		final public Asset asset;
		final public String mimeType;
		public SuccessAsset(Asset asset, String mimeType) {
			this.asset = asset;
			this.mimeType = mimeType;
		}
	}
	
	/**
	 * The page was failed and will never be able to be downloaded.
	 * Assign it {@link OfflineStatus#INVALID}
	 */
	public static class PermanentFailure implements Result {}
	
	/**
	 * The page was failed to download.
	 * It may potentially be able to be retried later or it might just always be broken. We can't tell.
	 * If the internet connection is unavailable, set it to whatever the existing status is and retry it next time.
	 * Otherwise, assign it {@link OfflineStatus#FAILED} and only retry it if the user invokes some kind of manual refresh/sync.
	 * If it fails a second time you could consider marking it {@link OfflineStatus#INVALID} to avoid retrying it continually.
	 */
	public static class Failure implements Result {}
	
	/**
	 * The page was downloaded successfully but some of its assets failed to download,
	 * and it was unclear if they are temporarily or permanently unavailable.
	 * (If we knew the missing ones were permanently unavailable we would have returned {@link Success})
	 * It could be viewable offline but it is unknown if the missing assets are important or not.
	 * Assign it {@link OfflineStatus#PARTIAL} and only retry it if the user invokes some kind of manual refresh/sync.
	 * If it fails a second time you could consider marking it {@link OfflineStatus#OFFLINE} to avoid retrying it continually.
	 */
	public static class Partial implements Result {
		final public String mimeType;
		final public String encoding;
		public Partial(String mimeType, String encoding) {
			this.mimeType = mimeType;
			this.encoding = encoding;
		}
	}
	
}
