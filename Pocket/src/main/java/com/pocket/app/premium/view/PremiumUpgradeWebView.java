package com.pocket.app.premium.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.pocket.app.App;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.thing.AccountUtil;
import com.pocket.sdk.util.DeepLinks;
import com.pocket.util.android.webkit.BaseWebView;

/**
 * A view that displays the purchase flow on the web and provides callbacks for when a purchase finishes.
 */
public class PremiumUpgradeWebView extends BaseWebView {
	
	private static final String URL = "https://getpocket.com/android/purchase";
	
	private WebViewPurchaseCallbacks purchaseListener;
	private boolean isInflated;

	private boolean isPurchaseConfirmed;
	
	public PremiumUpgradeWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public PremiumUpgradeWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PremiumUpgradeWebView(Context context) {
		super(context);
	}
	
	public void inflate(WebViewPurchaseCallbacks listener) {
		if (!isInflated) {
			purchaseListener = listener;
			
			setWebViewClient(new WebClient());
			
			WebSettings settings = getSettings();
			settings.setJavaScriptEnabled(true);
			settings.setSaveFormData(false);
			settings.setSavePassword(false);
			settings.setBuiltInZoomControls(false);
			settings.setSupportZoom(false);
			settings.setLoadWithOverviewMode(false);
			settings.setUseWideViewPort(false);
			
			setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
			setBackgroundColor(getResources().getColor(com.pocket.ui.R.color.pkt_bg));
			
			isInflated = true;
			
			// Focus Hack needed to make the softkeyboard show properly in dialog fragments. Yay Android bugs...
			requestFocus(View.FOCUS_DOWN);
			setOnTouchListener((v, event) -> {
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_UP:
						if (!v.hasFocus()) {
							v.requestFocus();
						}
						break;
				}
				return false;
			});
		}
		
		purchaseListener.onWebPurchaseLoadingStateChanged(true);
		loadUrl(URL);
	}
	
	private class WebClient extends WebViewClient {
		@Override
		public void onPageFinished(WebView view, String url) {
			listenForPurchaseConfirmation(url);
			purchaseListener.onWebPurchaseLoadingStateChanged(false);
		}
		
		@Override
		public void onLoadResource(WebView view, String url) {
			listenForPurchaseConfirmation(url);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			listenForPurchaseConfirmation(url);
			return false;
		}
	}
	
	/**
	 * Some versions of Android don't detect redirects reliably so we listen for the change from multiple places.
	 * @param url Pass the url to check
	 */
	private void listenForPurchaseConfirmation(String url) {
		if (!isPurchaseConfirmed && DeepLinks.Parser.isPocketWebUrl(url)) {
			// Attempt to get the user's premium status to see if it has changed
			Pocket pocket = App.from(getContext()).pocket();
			pocket.syncRemote(AccountUtil.getuser(pocket.spec()))
					.onSuccess(result -> {
						if (result.user.premium_status) {
							onPurchaseConfirmed();
						}
					});
		}
	}

	public void onPurchaseConfirmed() {
		isPurchaseConfirmed = true;
		purchaseListener.onWebPurchaseComplete();
	}
	
	public interface WebViewPurchaseCallbacks {
		void onWebPurchaseLoadingStateChanged(boolean isLoading);
		void onWebPurchaseComplete();
	}
}
