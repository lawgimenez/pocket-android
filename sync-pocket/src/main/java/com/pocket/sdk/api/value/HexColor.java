package com.pocket.sdk.api.value;

/**
 * String that represents a hex color, such as "#FFFFFF".   Alpha channel optionally supported: #FFFFFFFF
 */
public class HexColor {
	public final String value;
	
	/**
	 * Throws exceptions if empty or not a hex color.
	 * @param value
	 */
	public HexColor(String value) {
		this.value = value;
		validate(value);
	}
	
	private void validate(String value) {
		if (!value.startsWith("#") || (value.length() != 7 && value.length() != 9)) {
			throw new RuntimeException("Invalid hex color " + value);
		}
		Long.parseLong(value.substring(1, value.length()), 16); // If not hexdecimal, it will throw an exception
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		HexColor that = (HexColor) o;
		
		if (value != null ? !value.equals(that.value) : that.value != null) return false;
		
		return true;
	}
	
	@Override public int hashCode() {
		return value != null ? value.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return value;
	}
}
