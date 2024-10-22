package com.pocket.sdk.api.value;

import com.pocket.sync.space.Space;

/**
 * Passwords should never ever be stored client side, even encrypted. Just don't do it.
 * Passwords should only live in memory and only temporarily, just long enough to pass over to the api.
 * <p>
 * Special care should be taken to make sure this is never exposed in logs, crash reports, etc.
 * <p>
 * {@link Space} and {@link com.pocket.sync.source.Source} implementations that store Things on disk must redact this value before storing.
 * To redact, create a new Password(Password.REDACTED) instance and store that. It will indicate there
 * was a password value set, but it won't store it on disk. This does mean the data will be lost when
 * you try to pull it back out, but if you are properly passing it onto the api quickly, that shouldn't matter.
 * <p>
 * When thinking about this value in sync, imagine that you can put it into a source, but never get it back out.
 */
public class Password {
	
	public static final String REDACTED = "";
	
	public final String password;
	
	public Password(String value) {
		this.password = value;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return true; // With redaction, equality checks are meaningless, we assume all are equal.
	}
	
	@Override
	public int hashCode() {
		return 0;
	}
	
	@Override
	public String toString() {
		return REDACTED;
	}
	
	public static String toJsonValue(Password value) {
		return value == null ? null : Password.REDACTED;
	}
}
