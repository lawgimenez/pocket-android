package com.pocket.util.java;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * A Throwable that can explicitly provide a human friendly, user facing message.
 * {@link Throwable#getMessage()} may be overly technical or not clear whether or not it is intended or safe for users to view.
 */
public interface UserFacingErrorMessage {
	String getUserFacingMessage();
	
	static String find(Throwable t) {
		int i = ExceptionUtils.indexOfType(t, UserFacingErrorMessage.class);
		return i >= 0 ? ((UserFacingErrorMessage) ExceptionUtils.getThrowables(t)[i]).getUserFacingMessage() : null;
	}
}
