package com.pocket.sdk.api.value;

/**
 * An email address
 */
public class EmailString {

	public final String value;

	public EmailString(String email) {
		this.value = email;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		EmailString that = (EmailString) o;
		
		if (value != null ? !value.equals(that.value) : that.value != null) return false;
		
		return true;
	}
	
	@Override public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}
}
