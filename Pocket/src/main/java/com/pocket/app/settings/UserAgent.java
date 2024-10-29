package com.pocket.app.settings;

import android.content.Context;
import android.webkit.WebView;

import com.pocket.util.java.Logs;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages the user agent for downloading and the reader. It allows faking a desktop agent when required or requested by user.
 */
@Singleton
public class UserAgent {

	/**
	 * REVIEW USER AGENT This should be updated every now and then or we should find a permanent and automatic way. Can we just omit it?
	 */
	private static final String FAKE_DESKTOP_OS = "X11; Linux x86_64";
	private static final String FAIL_SAFE_MOBILE = "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36";
	private static final String FAIL_SAFE_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Safari/537.36";

	private BooleanPreference useMobile;
	private StringPreference mobile;
	private StringPreference desktop;

	@Inject
	public UserAgent(@ApplicationContext Context context, AppPrefs prefs) {
		mobile = prefs.USER_AGENT_MOBILE;
		desktop = prefs.USER_AGENT_DESKTOP;
		useMobile = prefs.USE_MOBILE_AGENT;
		if (mobile.get() == null || desktop.get() == null) {
			// Only reload if we don't already have something cached.
			// TODO add something that will refresh this every once in a while
			// We don't need to refresh it every app load, as it has some start up cost, but maybe each app update, or OS update?
			// Unfortunately can't make it an async task because it needs the ui thread for a WebView. Maybe a scheduled repeating task?
			reload(context);
		}
	}
	
	/** @return The default User Agent for this device. */
	public String mobile() {
		return mobile.get();
	}
	
	/** @return A fake desktop User Agent. */
	public String desktop() {
		return desktop.get();
	}
	
	/** @return One of the two agents, based on the user's {@link AppPrefs#USE_MOBILE_AGENT} preference. */
	public String preferred() {
		if (useMobile.get()) {
			return mobile();
		} else {
			return desktop();
		}
	}
	
	private void saveMobileAgent(String agent) {
		mobile.set(agent);
	}
	
	private void saveDesktopAgent(String agent) {
		desktop.set(agent);
	}
	
	/**
	 * Attempts to load the current user agent from a WebView
	 * and updates the known user agents.
	 * If you need to have the absolute latest, invoke this first.
	 * On a Nexus 5x this takes around 40/50ms
	 */
	public UserAgent reload(Context context) {
		// First attempt to get the real device user agent.
		try {
			WebView webView = new WebView(context);
			String m = webView.getSettings().getUserAgentString();
			m = StringUtils.trimToNull(m);
			if (m != null) {
				saveMobileAgent(m);
				// Success
				
			} else {
				saveMobileAgent(FAIL_SAFE_MOBILE);
				saveDesktopAgent(FAIL_SAFE_DESKTOP);
				return this;
			}
			
		} catch (Throwable t) {
			// Not really expected, but let's not blow up the app over this.
			Logs.printStackTrace(t);
			saveMobileAgent(FAIL_SAFE_MOBILE);
			saveDesktopAgent(FAIL_SAFE_DESKTOP);
			return this;
		}
		
		// If successfully obtained the mobile agent...
		// Second, tweak the user agent to fake a desktop agent, but using the correct version numbers.
		try {
			/*
			 * A default WebView agent looks something like this 			// Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36
			 * Chrome's "Request Desktop Agent" looks something like this:	// Mozilla/5.0 (X11; Linux x86_64) 								AppleWebKit/537.36 (KHTML, like Gecko) 			   Chrome/48.0.2564.95 	Safari/537.36
			 */
			
			// Remove "Mobile" from Safari
			String d = StringUtils.replace(mobile.get(), "Mobile Safari", "Safari");
			
			// Replace OS info
			int open = d.indexOf("(");
			int android = StringUtils.indexOfIgnoreCase(d, "Android");
			int close = d.indexOf(")");
			if (open > 0 && close > open && android > open && android < close) {
				d = StringUtils.substring(d, 0, open + 1) + FAKE_DESKTOP_OS + StringUtils.substring(d, close);
			}
			d = StringUtils.trimToNull(d);
			
			saveDesktopAgent(d != null ? d : FAIL_SAFE_DESKTOP);
			
		} catch (Throwable t) {
			// Not really expected, but let's not blow up the app over this.
			Logs.printStackTrace(t);
			saveDesktopAgent(FAIL_SAFE_DESKTOP);
		}
		return this;
	}

}
