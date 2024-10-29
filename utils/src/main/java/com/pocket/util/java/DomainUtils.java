package com.pocket.util.java;

import org.apache.commons.lang3.StringUtils;

public class DomainUtils {
	
	/**
	 * Will take a url such as http://www.stackoverflow.com and return www.stackoverflow.com
	 */
	public static String getHost(String url){
		if(url == null || url.length() == 0)
			return "";
		
		int doubleslash = url.indexOf("//");
		if(doubleslash == -1)
			doubleslash = 0;
		else
			doubleslash += 2;
		
		int slashEnd = url.indexOf('/', doubleslash);
		slashEnd = slashEnd >= 0 ? slashEnd : url.length();

		int queryEnd = url.indexOf('?', doubleslash);
		queryEnd = queryEnd >= 0 ? queryEnd : url.length();
		
		return url.substring(doubleslash, Math.min(slashEnd, queryEnd));
	}
	

	/**  Based on : http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.3_r1/android/webkit/CookieManager.java#CookieManager.getBaseDomain%28java.lang.String%29
     * Get the base domain for a given host or url. E.g. mail.google.com will return google.com
     */
    public static String getBaseDomain(String url) {
    	String host = getHost(url);
    	
        int startIndex = 0;
        int nextIndex = host.indexOf('.');
        int lastIndex = host.lastIndexOf('.');
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1;
            nextIndex = host.indexOf('.', startIndex);
        }
        if (startIndex > 0) {
            return host.substring(startIndex);
        } else {
            return host;
        }
    }
	
	/**
	 * Reduces {@code url} down to the host part, removing slashes, and `www.` subdomain.
	 * Other subdomains are fine to keep.
	 */
	public static String cleanHostFromUrl(String url) {
		String host = getHost(url);
		return StringUtils.replaceOnce(host, "www.", "");
	}
}
