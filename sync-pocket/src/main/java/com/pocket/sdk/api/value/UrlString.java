package com.pocket.sdk.api.value;

import org.apache.commons.lang3.StringUtils;

/**
 * A http or https url string.
 */
public class UrlString {

	/** A non-null http or https url string. */
	public final String url;

	public UrlString(String url) {
		if (StringUtils.isBlank(url)) throw new IllegalArgumentException("blank or null urls are not allowed");
		this.url = url;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UrlString urlString = (UrlString) o;

		return url.equals(urlString.url);
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}
	
	@Override
	public String toString() {
		return url;
	}
	
	public static String asString(UrlString url) {
		return url != null ? url.url : null;
	}
}
