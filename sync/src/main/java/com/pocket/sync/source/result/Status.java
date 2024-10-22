package com.pocket.sync.source.result;

/**
 * An indication of what happened to a Thing or Action before a {@link SyncException}.
 */
public enum Status {
	/** Sync didn't get far enough to attempt this yet. */
	NOT_ATTEMPTED(true),
	/** Successfully Synced. For Actions this means it was applied. For Things, this means it retrieved */
	SUCCESS(false),
	/** Successfully process, but ignored. An example of this would be a local action that was sent to a remote source, it will ignore it and not do anything with it. */
	IGNORED(false),
	/** There was an error when trying to apply this Action or retrieve this Thing. */
	FAILED(true),
	/** (Only for Actions) The Source received the action but it could not apply it and won't be able to, so the action should be discarded and not retried. */
	FAILED_DISCARD(false);
	
	public final boolean retryable;
	
	Status(boolean retryable) {
		this.retryable = retryable;
	}
}
