package com.pocket.sync.space.persist;

/**
 * A {@link com.pocket.sync.space.Space} failed to restore its state/database needed to function.
 * This could have been due to a temporary error that can be retried or it could be an indication
 * that the data source is missing or corrupted.
 */
public class SpaceRestoreException extends RuntimeException {
	
	public SpaceRestoreException() {
		super();
	}
	
	public SpaceRestoreException(String message) {
		super(message);
	}
	
	public SpaceRestoreException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public SpaceRestoreException(Throwable cause) {
		super(cause);
	}
}
