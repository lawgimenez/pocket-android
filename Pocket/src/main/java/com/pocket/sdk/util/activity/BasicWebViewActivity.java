package com.pocket.sdk.util.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ideashower.readitlater.R;
import com.pocket.app.help.Help;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.android.IntentUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BasicWebViewActivity extends AbsPocketActivity {
	
	public static final String PATH = "pathToLoad";
	public static final String EXTRA_POST_ACCOUNT = "postAccount";

	// Views
	private WebView mWebView = null;
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.BASICWEBVIEWACTIVITY; // REVIEW this is kinda weird...
	}
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
		mIsHelpActivity = true;
		
		super.onCreate(savedInstanceState);
		
		setContentView(getLayoutId());
		
		Intent intent = getIntent();
		String path = intent.getStringExtra(PATH);
		boolean postAccount = intent.getBooleanExtra(EXTRA_POST_ACCOUNT, false);
		
		if (path == null){
			startDefaultActivity(); // OPT throw
			return;
		}
		
		mWebView = (WebView) findViewById(getWebViewResId());
		
		mWebView.setWebViewClient(new HookClient());
		
		WebSettings settings = mWebView.getSettings();
		settings.setJavaScriptEnabled(true);
		settings.setBuiltInZoomControls(true);
		settings.setLoadWithOverviewMode(true);
		settings.setSaveFormData(false);
		settings.setSavePassword(false);
		settings.setUseWideViewPort(path.startsWith("file") ? false : true); // If it is a local file, the viewport will be setup in html
		
		mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		mWebView.setBackgroundColor(Color.TRANSPARENT);
		
		if (postAccount) {
		    /* REVIEW Do we still need this? If so, switch to use the oauth token
			String postData = "username="+User.getUsername()+"&password="+User.getPassword();
		    mWebView.postUrl(path, EncodingUtils.getBytes(postData, "BASE64"));
			*/
			
		} else {
			Map<String, String> headers = new HashMap<String, String>();
			String lang = Locale.getDefault().getLanguage();
			String country = Locale.getDefault().getCountry();
			headers.put("Accept-Language", lang + "-" + country); 

			mWebView.loadUrl(path,headers);
		}
		
	}
	
	protected int getLayoutId() {
		return R.layout.activity_basic_webview;
	}
		
	
	protected int getWebViewResId(){
		return R.id.basic_webview;
	}
	
	private class HookClient extends WebViewClient {

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			
			// Search for hooks
			
			if (url.startsWith("browser::")){
				startActivity(new Intent("android.intent.action.VIEW", Uri.parse(StringUtils.replaceOnce(url, "browser::", ""))));
				return true;
				
			} else if(url.startsWith("webview::")){
				Intent intent = new Intent(BasicWebViewActivity.this, BasicWebViewActivity.class);
				intent.putExtra(BasicWebViewActivity.PATH, StringUtils.replaceOnce(url, "webview::", ""));
				startActivity(intent);
				return true;
				
			} else if (url.startsWith("mailto:")){
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setType("plain/text");
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{ StringUtils.replaceOnce(url, "mailto:", "")});
				
				if(IntentUtils.isActivityIntentAvailable(BasicWebViewActivity.this, intent)){
					startActivity(intent);
				}
				return true;
				
			} else {
				return false;
				
			}
			
		}
	}
	
	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ANY;
	}
	
}
