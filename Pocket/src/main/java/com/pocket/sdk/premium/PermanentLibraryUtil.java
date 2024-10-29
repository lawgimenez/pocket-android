package com.pocket.sdk.premium;

import android.net.Uri;

import com.pocket.app.App;
import com.pocket.sdk.api.PocketServer;
import com.pocket.sdk.api.generated.thing.Item;

import java.net.URLEncoder;

import okio.ByteString;

public abstract class PermanentLibraryUtil {
	
	/**
	 * Returns the permanent library url that can be loaded directly into a web view.
	 * <p>
	 * Only use this if the user is a premium user, or this will return an invalid url.
	 * @param item
	 * @return
	 */
	public static String getLibraryWebViewUrl(Item item) {
		return getLibraryWebViewUri(item).toString();
	}

	public static Uri getLibraryWebViewUri(Item item) {
		return getLibraryWebViewUri(item.id_url.url, item.item_id);
	}

	public static Uri getLibraryWebViewUri(String givenUrl, String itemId) {
		givenUrl = URLEncoder.encode(givenUrl); // Encode

		String itemKey;
		String itemValue;
		if (itemId != null) {
			itemKey = "pl_i";
			itemValue = itemId;
		} else {
			itemKey = "pl_gu";
			itemValue = givenUrl;
		}
		String time = String.valueOf(System.currentTimeMillis() / 1000L);
		String uid = App.getApp().pktcache().getUID();
		String hash = hash(time, uid, itemValue);

		return Uri.parse(PocketServer.PERM_LIBRARY).buildUpon()
			.appendQueryParameter("pl_h", hash)
			.appendQueryParameter("pl_u", uid)
			.appendQueryParameter("pl_t", time)
			.appendQueryParameter(itemKey, itemValue)
			.appendQueryParameter("fallback_url", givenUrl)
			.build();
	}

	/**
	 * Creates a SHA256 hash as specified by the Pocket Permanent Library spec.
	 */
	public static String hash(String timestamp, String uid, String itemIdentifier) {
		String toHash = uid +
				PermanentLibraryUtilStrings.sDelim3 + // :
				timestamp +
				((String[]) PermanentLibraryUtilStrings.sSaltPieces)[18] + // :
				itemIdentifier +
				":" +
				PermanentLibraryUtilStrings.sSaltBaseKey;
		return ByteString.of(toHash.getBytes()).sha256().hex();
	}
}
