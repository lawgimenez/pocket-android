package com.pocket.util.android;

import android.webkit.WebView;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public class WebViewUtil {

	/**
	 * Gets the currently selected text if any.
	 *
	 * @param webview The webview to get the text selection from.
	 * @param callback A callback after it has retrieved the text selection. Be aware this could potentially call back off the ui thread.
	 */
	public static void getSelectedText(WebView webview, SelectedTextCallback callback) {
		// Chromium based webview, use javascript

		webview.evaluateJavascript("window.getSelection().toString()", s -> {
			s = StringUtils.trimToNull(s);
			s = s.replaceAll("([^\\\\]|^)\"", "$1"); // Remove unescaped quotes.
			s = StringEscapeUtils.unescapeJava(s); // This string comes from web escaped for java.
			callback.onTextSelectionRetrieved(s);
		});
	}

	public interface SelectedTextCallback {
		void onTextSelectionRetrieved(String selectedText);
	}
	
	public static void selectAll(WebView webView) {
		webView.evaluateJavascript("document.execCommand(\"selectAll\");", null);
	}
}
