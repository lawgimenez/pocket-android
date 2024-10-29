package com.pocket.sdk.api.value;

/**
 * A string that should be considered dangerous
 */
public class Dangerous {
	
	public static final String REDACTED = "";
	public final String value;
	
	public Dangerous(String value) {
		this.value = value;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		Dangerous that = (Dangerous) o;
		
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
