package com.pocket.sync.source.result;

import com.pocket.sync.action.Action;
import com.pocket.sync.space.Space;
import com.pocket.sync.source.SynchronousRemoteBackedSource;
import com.pocket.sync.source.Source;
import com.pocket.sync.spec.Spec;

/**
 * Used by {@link Spec#apply(Action, Space, Source)} to indicate to a {@link SynchronousRemoteBackedSource} the preference of how quickly
 * an action that was applied locally should be sent to the linked/remote source.
 */
public enum RemotePriority {
	/** No specific hurry, can just sent whenever the next natural call to the remote is. */
	WHENEVER("w"),
	/** Ideally this should be sent as soon as possible. */
	SOON("s"),
	/**
	 * This action has to be performed by the remote, it cannot be perform locally at all.
	 * Actions of this type will never be persisted/retried via {@link Space#addAction(Action, RemotePriority)},
	 * they will go directly to the remote and if they fail they are not retried.
	 * @see #REMOTE_RETRYABLE
	 */
	REMOTE("r"),
	/**
	 * This action has to be performed by the remote, it cannot be perform locally at all.
	 * Unlike {@link #REMOTE}, if this action fails due to a {@link Status#retryable} error,
	 * it is persisted and retried later. Note: Actions that contain sensitive or protected fields
	 * like passwords are not allowed to have this status.
	 */
	REMOTE_RETRYABLE("rr"),
	/** No need to send to the remote. */
	LOCAL("l");
	
	/** This key will be used by persistence to store and restore this enum, so it must not change*/
	public final String key;
	
	RemotePriority(String key) {
		this.key = key;
	}
	
	public static RemotePriority fromKey(String key) {
		for (RemotePriority a : values()) {
			if (a.key.equals(key)) return a;
		}
		return null;
	}
}
