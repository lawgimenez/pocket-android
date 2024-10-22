package com.pocket.sync.source.result;

/**
 * The result of a Thing or Action in a {@link SyncException}
 */
public class Result {
	public final Status status;
	public final Throwable cause;
	public final String message;
	
	public Result(Status status, Throwable cause, String message) {
		this.status = status;
		this.cause = cause;
		this.message = message;
	}
}
