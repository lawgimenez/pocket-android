package com.pocket.app.add;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.pocket.util.java.Logs;
import com.pocket.util.java.UrlFinder;

import java.util.ArrayList;

public class IntentItemUtil {
	/**
	 * Parses an Intent for {@link AddActivity} to extract an item to save and various types of meta data.
	 */
	public static IntentItem from(Intent intent) {
		// WARNING: This is used in an exported activity. Extras could come from outside apps and may not be trust worthy.
		// Get a list of urls to choose from from the Intent
		final ArrayList<String> urls = findUrlsFromIntent(intent);
		// Determine which url to save
		String url = null;
		if (!urls.isEmpty()) {
			url = urls.get(0);
		}

		final String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);

		return new IntentItem(url, title);
	}

	/**
	 * Finds the url(s) that are to be saved.
	 */
	@NonNull private static ArrayList<String> findUrlsFromIntent(Intent intent) {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			// A getpocket.com/save url
			ArrayList<String> urls = new ArrayList<>(1);
			if (intent.getData() != null) {
				String saveUrl;
				try {
					saveUrl = intent.getData().getQueryParameter("url");
					urls.add(saveUrl);
				} catch (Throwable t) {
					// Not matching the format we are expecting
					Logs.printStackTrace(t);
				}
			}
			return urls;

		} else {
			// SEND Action or other, search the extras
			ArrayList<String> urls =
					UrlFinder.getUrlsFromText(intent.getStringExtra(Intent.EXTRA_TEXT));
			return urls != null ? urls : new ArrayList<>();
		}
	}
	
	private IntentItemUtil() {
		throw new AssertionError("No instances.");
	}
}
