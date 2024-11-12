package com.pocket.sdk.http;

import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.sdk.network.eclectic.EclecticHttpUtil;
import com.pocket.util.java.DomainUtils;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages Cookies for the app, mostly for the site logins feature. See {@link com.pocket.sdk.api.generated.thing.Loginlist} and related classes.
 */
@Singleton
public class CookieDelegate implements AppLifecycle {
	
	private final HttpClientDelegate http;
	private final AppThreads threads;
	private CookieSyncManagerCompat cookieSyncManager;

	@Inject
	public CookieDelegate(HttpClientDelegate http, AppThreads threads, AppLifecycleEventDispatcher dispatcher) {
		dispatcher.registerAppLifecycleObserver(this);
		this.http = http;
		this.threads = threads;
	}
	
	private void init() {
		if (cookieSyncManager == null) {
			cookieSyncManager = new CookieSyncManagerCompat();
		}
	}
	
    public String getCookiesString(String url) {
		init();
        final CookieManager cookieManager = CookieManager.getInstance();
		return cookieManager.getCookie(url);
    }
    
    public void extendCookies(String url) {
		init();
		// Hit the url so we get the latest cookies in the store.
		EclecticHttp client = http.getClient();
		EclecticHttpUtil.getString(
				http.getClient().buildRequest(url).setHeader("User-Agent", App.getApp().userAgent().desktop()),
				client);
		
		extendCookies(url, client.getCookieManager().getCookieStore().getCookies()); // REVIEW these get all cookies... don't we only want the ones for the url? but looking at FileDownloader's getCookiesFromStore method, this seems to be what it did returned.
    }
		
	public void extendCookies(String url, List<HttpCookie> cookies){
		init();
    	CookieManager cookieManager = CookieManager.getInstance();
		StringBuilder builder = new StringBuilder();
    	String value;
    	String domain;
    	String path;
    	boolean isSecure;
    	for (HttpCookie cookie : cookies) {
    		builder.setLength(0);
    		
    		value = cookie.getValue();
    		domain = cookie.getDomain();
    		path = cookie.getPath();
    		isSecure = cookie.getSecure();
    		
    		builder
			.append(cookie.getName())
			.append("=")
			.append(value != null ? value : "")
			.append(";")
			.append("expires=Fri, 01 Jan 2049 01:01:01 GMT;");
    		
    		if(domain != null && domain.length() > 0){
    			builder
    			.append("Domain=")
    			.append(domain)
    			.append(";");
    		}
    		
    		if(path != null && path.length() > 0){
    			builder
    			.append("Path=")
    			.append(path)
    			.append(";");
    		}
    		
    		if(isSecure){
    			builder
    			.append("Secure")
    			.append(";");
    		}
    		
    		cookieManager.setCookie(url, builder.toString());
        }
    }
	
	/**
	 * Performs a {@link CookieManager#flush()} asynchronously.
	 * We used to use {@link CookieSyncManager#sync()}, which before Lollipop flushed asynchronously,
	 * but since Lollipop, it is a blocking call. To keep it asynchronous, we handle wrapping it
	 * in an async call. If a blocking call is needed at some point another method could be created.
	 */
	public void sync() {
		threads.async(() -> {
			init();
			cookieSyncManager.sync();
		});
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override public void stopModifyingUserData() {}

			@Override
			public void deleteUserData() {
				if (cookieSyncManager != null) {
					cookieSyncManager.removeAllCookies();
				}
			}

			@Override public void restart() {}

			@Override public void onLoggedOut() {}
		};
	}
	
	private static class CookieSyncManagerCompat {
		public void sync() {
			CookieManager.getInstance().flush();
		}
		
		public void removeAllCookies() {
			CookieManager.getInstance().removeAllCookies(null);
			CookieManager.getInstance().flush();
		}
	}
	
	/**
	 * Add cookies to the headers
	 */
	public void addCookiesToRequest(EclecticHttpRequest request, EclecticHttp client) {
		String url = request.getUrl();
		String cookieString = getCookiesString(url);
		if (cookieString != null){
			
			CookieStore store = client.getCookieManager().getCookieStore();
			HttpCookie cookie;
			String[] cookieParts;
			
			String[] cookies = cookieString.split(";");
			int length = cookies.length;
			for (int i = 0; i < length; i++) {
				cookieParts = cookies[i].split("=");
				cookie = new HttpCookie(cookieParts[0], cookieParts.length > 1 ? cookieParts[1] : null);
				cookie.setDomain(DomainUtils.getBaseDomain(url));
				try {
					store.add(new URI(request.getUrl()), cookie);
				} catch (URISyntaxException ignore) {}
			}
		}
	}
}
