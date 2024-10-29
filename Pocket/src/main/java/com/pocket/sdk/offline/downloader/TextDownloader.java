package com.pocket.sdk.offline.downloader;

import com.pocket.app.settings.UserAgent;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.endpoint.ApiException;
import com.pocket.sdk.api.generated.enums.FormFactor;
import com.pocket.sdk.api.generated.enums.OfflineStatus;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.ArticleResource;
import com.pocket.sdk.api.generated.thing.ArticleView;
import com.pocket.sdk.api.generated.thing.Image;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.image.ImageCache;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpUtil;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetDirectory;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.premium.PermanentLibraryUtil;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Downloads an Article View for an {@link Item} and any images needed.
 * <p>
 * The HTML markup, which can be opened to view it, will be saved to {@link AssetDirectory#pathForText(Item)}.
 * It will also download any images needed to display it. Those will be stored as {@link Asset}s and will use {@link Asset#local} for their location.
 * See {@link Assets} for more details on file storage.
 */
public class TextDownloader {
	
	/**
	 * Downloads the article view as described in main docs {@link TextDownloader}.
	 *
	 * All parameters are required
	 * @param item The item to download it for
	 * @param pocket A pocket instance used for syncing the article view content
	 * @param assets Info about where to write files
	 * @param userAgent Info about this user's user agent preferences
	 * @param http Internet access
	 * @param formFactor Info about this device
	 * @param imageCache Access to image downloading
	 * @param refresh true to download this again even if already downloaded, false to return success if already downloaded
	 * @return The result of the operation
	 * @throws Exception
	 */
	public OfflineStatus download(
			Item item,
			Pocket pocket,
			Assets assets,
			UserAgent userAgent,
			EclecticHttp http,
			FormFactor formFactor,
			ImageCache imageCache,
			boolean refresh) throws Exception {

		String uid = null;
		Account account = pocket.sync(pocket.spec().things().loginInfo().build()).get().account;
		if (account != null) {
			uid = account.user_id;
		}

		File output = new File(assets.getAssetDirectory().pathForText(item));
		AssetUser assetUser = AssetUser.forItem(item.time_added, item.idkey());
		
		if (!refresh && output.exists()) return OfflineStatus.OFFLINE;
		ArticleView.Builder builder = pocket.spec().things().articleView()
				.url(item.id_url)
				.getItem(true)
				.promptSubs(true)
				.msg(true)
				.formfactor(formFactor);
		if (uid != null) {
			builder.u(uid);
		}

		// Permanent Library Params
		// TODO why doesn't it check App.getApp().pktcache().hasFeature(PremiumFeature.LIBRARY)
		if (account != null) {
			String encodedUrl = URLEncoder.encode(item.id_url.url);
			String itemValue;
			if (item.item_id != null) {
				builder.pl_i(item.item_id);
				itemValue = item.item_id;
			} else {
				builder.pl_gu(encodedUrl);
				itemValue = encodedUrl;
			}
			String time = String.valueOf(System.currentTimeMillis() / 1000L);
			String hash = PermanentLibraryUtil.hash(time, uid, itemValue);
			builder.pl_h(hash)
					.pl_u(uid)
					.pl_t(time)
					.fallback_url(encodedUrl);
		}
		
		if (refresh) builder.refresh(true);
		
		ArticleView article;
		try {
			article = pocket.syncRemote(builder.build()).get();
			
			String markup = article.article;
			item = article.item;
			Set<String> imagesToDownload = new HashSet<>();
			if (article.resources != null) {
				int i = 0;
				for (ArticleResource res : article.resources) {
					imagesToDownload.add(res.url.url);
					markup = StringUtils.replace(markup,
							"{%pkt_resource_path_" + i + "}",
							Asset.createImage(res.url.url, assets.getAssetDirectoryQuietly()).local.getAbsolutePath()); // We use an absolute path because of the way the article view is loaded via javascript. The base path for the inline style is the location of articleview-mobile.js, which is in the asset bundle, not the offline cache. So we end up needing an absolute path regardless.
				}
			}
			if (item.images != null) {
				for (Image res : item.images) {
					imagesToDownload.add(res.src);
				}
			}
			if (!imagesToDownload.isEmpty()) {
				CountDownLatch latch = new CountDownLatch(imagesToDownload.size());
				for (String url : imagesToDownload) { // loop on new instance to avoid concurrent mod
					imageCache.build(url, assetUser).cache((request, isCached) -> latch.countDown());
				}
				latch.await(2, TimeUnit.MINUTES); // If this takes awhile or gets stuck, just let it continue asynchronously
				// Note: Looking at previous TextDownloader logic, it appears that it doesn't fail if an image fails, it just ignores it. So we do that as well.
			}
			
			assets.writeMarkup(item, PositionType.ARTICLE, markup, "UTF-8");
			return OfflineStatus.OFFLINE;
			
		} catch (Throwable t) {
			int code = ApiException.unwrapStatusCode(t);
			if (code == 415 || code == 413) {
				return OfflineStatus.INVALID;
			} else {
				throw t;
			}
		}
	}
	
}
