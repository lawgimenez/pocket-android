package com.pocket.util.java;

import android.util.Patterns;
import android.webkit.URLUtil;

import com.pocket.analytics.events.SystemLog;
import com.pocket.app.App;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlFinder {
	
	private static Pattern mPattern;
	
	/**
	 * Convenience for {@link #getUrlsFromText(String, int)} with no limit.
	 */
	public static ArrayList<String> getUrlsFromText(String textToSearch) {
		return getUrlsFromText(textToSearch, 0);
	}
	
	/**
	 * Returns a list of UrlMatchs for any urls found within the provided text. If the provided text is null
	 * then this method will return null. If no urls are found it will return an empty list. All urls are validated with
	 * URLUtil.isValidUrl(url)) before being added to the list. If the url is not valid it will not be included in the list. 
	 * 
	 * @param limit The maximum number of urls to find. Pass 0 for no limit.
	 */
	public static ArrayList<String> getUrlsFromText(String textToSearch, int limit) {
		if (textToSearch == null) {
			return null;
		}
		
		final boolean limitResults = limit > 0;
		final ArrayList<String> urls = new ArrayList<>();
		
		try {
			if (mPattern == null) {
				mPattern = Patterns.WEB_URL;
			}
			  
			Matcher matcher = mPattern.matcher(textToSearch);
			while ((!limitResults || limit > 0) && matcher.find()){
				String url = matcher.group();
				if (URLUtil.isValidUrl(url)) {
					urls.add(url);
					limit--;
				} else {
					try {
						// Does this check ever fail? Does Patterns.WEB_URL ever match invalid urls?
						var log = new SystemLog("url-finder.matched-invalid-url", url);
						App.getApp().tracker().track(log);
					} catch (Exception ignored) {
						// Don't crash if logging fails.
					}
				}
			}
			
		} catch (Throwable t) {
			return null;
		}
		
		return urls;
	}
	
	/**
	 * Returns the first match from a getUrlsFromText() call. If you pass null text this will return null.
	 * If no valid urls are found this will return null.
	 */
	public static String getFirstUrlOrNull(String textToSearch) {
		ArrayList<String> urls = getUrlsFromText(textToSearch, 1);
		if (urls == null || urls.isEmpty()) {
			return null;
		}
		
		return urls.get(0);
	}
}
