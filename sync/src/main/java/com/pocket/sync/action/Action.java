package com.pocket.sync.action;

import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Syncable;

/**
 * The inputs and values of an Action. The effect is typically defined in {@link com.pocket.sync.spec.Spec#apply(Action, Space)}.
 * Part of the Sync framework, see {@link com.pocket.sync} for more details.
 */
public interface Action extends Syncable {

	/** What name of the action. */
	String action();
	
	/** The time of the action. */
	Time time();
	
	/** The recommended priority for sending to a remote. */
	RemotePriority priority();
	
	/** @return A new builder of this Actions's type with the declared values of this Action already set. Note: Each action instance is immutable so changes to the builder won't have any impact on this instance. Instead the builder will create a new immutable instance when you invoke {@link ActionBuilder#build()}. */
	ActionBuilder builder();

	/** Parsers for handling return/responses about this action from a remote. Can be null if there is no resolve/return type. */
	ActionResolved resolved();
	
}
