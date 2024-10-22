package com.pocket.sdk2.view;

import com.pocket.app.App;
import com.pocket.sdk.image.Image;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.DownloadAuthorization;
import com.pocket.ui.util.LazyBitmap;

public class LazyAssetBitmap implements LazyBitmap {
	
	private final Asset asset;
	private final AssetUser assetUser;
	
	public LazyAssetBitmap(String url, AssetUser assetUser) {
		this(Asset.createImage(url, App.getApp().assets().getAssetDirectoryQuietly()), assetUser);
	}
	
	public LazyAssetBitmap(Asset asset, AssetUser assetUser) {
		this.asset = asset;
		this.assetUser = assetUser;
	}
			
	@Override
	public void fill(int widthPx, int heightPx, Loaded loaded, Canceller canceller) {
		if (widthPx == 0 || heightPx == 0) {
			return;
		}
		
		Image.build(asset, assetUser != null ? assetUser : AssetUser.forSession())
				.setDownloadAuthorization(DownloadAuthorization.ALWAYS)
				.setCanceller(info -> canceller == null || !canceller.isCancelled())
				.fill(widthPx, heightPx)
				.getAsync((request, wrapper, result) -> {
					if (wrapper != null && wrapper.hasValidBitmap()) {
						loaded.onBitmapLoaded(wrapper.getBitmap());
					}
					
				});
	}
	
}
