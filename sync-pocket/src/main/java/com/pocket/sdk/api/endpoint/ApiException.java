package com.pocket.sdk.api.endpoint;

import com.pocket.util.java.UserFacingErrorMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * There was an error when connecting to the Pocket API.
 */
public class ApiException extends Exception implements UserFacingErrorMessage {
	
	public enum Type {
		/** Likely a networking or i/o type error and likely an exception contains more info. See {@link #getCause()} */
		CONNECTION,
		/** Pocket received the request but returned an error. See {@link #xError}, {@link #xErrorCode} {@link #xErrorData} */
		POCKET,
		/** Pocket responded that the access token has been revoked by the user and the app should be logged out immediately. */
		POCKET_ACCESS_TOKEN_REVOKED
	}

	public static final int STATUS_INVALID_400 = 400;
	public static final int STATUS_USERPASS_401 = 401;
	public static final int STATUS_RATE_LIMIT_403 = 403;
	public static final int STATUS_DOWN_MAINTENANCE_503 = 503;
	
	public final Type type;
	public final Throwable exception;
	public final String xErrorCode;
	public final String xErrorData;
	public final String xError;
	public final int httpStatusCode;

	public ApiException(Type type, Throwable t, int httpStatusCode, String xError, String xErrorCode, String xErrorData) {
		super(t);
		this.type = type;
		this.httpStatusCode = httpStatusCode;
		this.xError = xError;
		this.xErrorCode = xErrorCode;
		this.xErrorData = xErrorData;
		this.exception = t;
	}

	/** @return A numerical {@link #xErrorCode}, otherwise 0. */
	public int getXErrorCodeInt() {
		return NumberUtils.toInt(xErrorCode);
	}
	
	@Override
	public String getUserFacingMessage() {
		return StringUtils.defaultString(xError);
	}

	@Override
	public String getMessage() {
		// NOTE: Be careful not to expose user info here that might end up in logs or crash reports.
		final StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(" ").append(httpStatusCode);
		if (xErrorCode != null) sb.append(", ").append(xErrorCode);
		if (xError != null) sb.append(", ").append(xError);
		if (getCause() != null) sb.append(", ").append(getCause());
		return sb.toString();
	}

	/** Returns an ApiException if it is an ApiException or has one as a cause, or null. */
	public static ApiException unwrap(Throwable t) {
		int i = ExceptionUtils.indexOfType(t, ApiException.class);
		return i >= 0 ? (ApiException) ExceptionUtils.getThrowables(t)[i] : null;
	}
	
	/** Returns the http status code of any ApiException found, or 0. See {@link #unwrap(Throwable)} */
	public static int unwrapStatusCode(Throwable t) {
		ApiException e = unwrap(t);
		return e != null ? e.httpStatusCode : 0;
	}
	
	/** Returns the X-ErrorCode of any ApiException found, or 0. See {@link #unwrap(Throwable)} */
	public static int unwrapXErrorCode(Throwable t) {
		ApiException e = unwrap(t);
		return e != null ? e.getXErrorCodeInt() : 0;
	}

	/** Returns the type of any ApiException found, or 0. See {@link #unwrap(Throwable)} */
	public static Type unwrapType(Throwable t) {
		ApiException e = unwrap(t);
		return e != null ? e.type : null;
	}
	
}
