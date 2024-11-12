package com.pocket.sdk.offline.downloader.processor;

import com.pocket.app.App;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetDirectory;
import com.pocket.sdk.offline.cache.AssetDirectoryUnavailableException;
import com.pocket.sdk.offline.cache.AssetUser;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A {@link com.pocket.sdk.offline.downloader.processor.StreamingMarkupProcessor.LiteralHandler}
 * that takes a path, converts it to an {@link Asset} and replaces the path text with a relative
 * path to where the asset will live on disk compared to its parent web view.
 * <p>
 * Used by the {@link com.pocket.sdk.offline.downloader.WebDownloader}.
 * Use a static method to create a new instance
 */
public class AssetHandler implements StreamingMarkupProcessor.LiteralHandler {
	
	private final File offlineDirectory;
	private final URL baseUrl;
	private final String assetsFolderName;
	private final Asset baseAsset;
	private final AssetUser baseAssetUser;
	private final AssetDirectory assetDirectory;
	private final AssetsOut out;
	
	/**
	 * Use for handling assets found within the page's html content.
	 * @param url The url of the page
	 * @param assetUser The asset user for the page
	 * @param assetDirectory The asset directory being used for downloading
	 * @param out Where to output assets that are found
	 */
	public static AssetHandler forWebPage(String url, AssetUser assetUser, AssetDirectory assetDirectory, AssetsOut out) throws MalformedURLException, AssetDirectoryUnavailableException {
		return new AssetHandler(new URL(url), null, assetUser, assetDirectory, out);
	}
	
	/**
	 * Use for handling assets found within another asset, such as when processing a stylesheet's content
	 * @param baseAsset The asset being processed
	 * @param assetDirectory The asset directory being used for downloading
	 * @param out Where to output assets that are found
	 */
	public static AssetHandler forPageAsset(Asset baseAsset, AssetDirectory assetDirectory, AssetsOut out) throws AssetDirectoryUnavailableException {
		return new AssetHandler(baseAsset.url, baseAsset, AssetUser.forParentAsset(baseAsset), assetDirectory, out);
	}
	
	private AssetHandler(URL baseUrl, Asset baseAsset, AssetUser assetUser, AssetDirectory assetDirectory, AssetsOut out) throws AssetDirectoryUnavailableException {
		this.offlineDirectory = new File(assetDirectory.getOfflinePath());
		this.assetDirectory = assetDirectory;
		this.out = out;
		this.baseAsset = baseAsset;
		this.baseUrl = baseUrl;
		this.assetsFolderName = App.getApp().assets().getAssetDirectory().getAssetsFolderName();
		this.baseAssetUser = assetUser;
	}
	
	@Override
	public String capture(String literal, int type) {
		String capture = StringUtils.trimToNull(literal);
		if (capture == null || capture.contains(assetsFolderName)) return literal;
		try {
			Asset asset = Asset.create(new URL(baseUrl, capture), type, type == Asset.STYLESHEET, assetDirectory);
			String replacement = relativePath(asset, baseAsset, offlineDirectory);
			out.found(asset, baseAssetUser);
			return replacement;
		} catch (MalformedURLException e) {
			return literal;
		}
	}
	
	/**
	 * @return A relative path to this asset from the base asset. If the base asset is null, it is from {@link AssetDirectory#pathForWeb(Item)}
	 */
	private static String relativePath(Asset literal, Asset baseAsset, File root) {
		int jumps;
		if (baseAsset == null) {
			// This is the most common case, like an asset directly on a webpage
			// Here we make a relative path that jumps out of the web.html and then into the assets directory and up to the asset
			jumps = 2;
		} else {
			// When there is a base asset, that means an asset found within another asset, such as an image within a css file.
			// In that example, the base asset is the css file.
			// Here we make a relative path that jumps out of the css file and then up to the asset
			jumps = truncatedPath(baseAsset, root).split(File.separator).length-1;
		}
		return StringUtils.repeat("../", jumps) + truncatedPath(literal, root);
	}
	
	/** @return The path within the root that this file is in. This assumes it is in this root! */
	private static String truncatedPath(Asset asset, File root) {
		return asset.local.getAbsolutePath().substring(root.getAbsolutePath().length()+1);
	}
	
	public interface AssetsOut {
		/** This asset was found in the markup. Typically this will be used as a call to go download the asset. */
		void found(Asset asset, AssetUser user);
	}
	
}
