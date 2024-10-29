package com.pocket.util.java;

public interface StringValidator {
	/** Return null if valid. If invalid, return an error message to display to user */
	public String validate(String value);
}
