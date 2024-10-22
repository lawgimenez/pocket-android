package com.pocket.sdk.api.value;

/**
 * An id token that gives access to something and should be protected.
 * <p>
 * Special care should be taken to avoid exposing this in logs, crash reports, etc.
 * <p>
 * Only store on disk if you can protect it from outside access and/or encrypt it.
 */
public class AccessToken {
	
	public static final String REDACTED = "";
	public final String value;
	
	public AccessToken(String value) {
		this.value = value;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		AccessToken that = (AccessToken) o;
		
		if (value != null ? !value.equals(that.value) : that.value != null) return false;
		
		return true;
	}
	
	@Override public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return REDACTED; // Do not allow to appear in logs.
	}
	
}
