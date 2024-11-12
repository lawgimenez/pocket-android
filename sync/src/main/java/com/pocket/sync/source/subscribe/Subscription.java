package com.pocket.sync.source.subscribe;

import com.pocket.sync.source.Source;
import com.pocket.sync.source.Subscribeable;
import com.pocket.sync.thing.Thing;

/**
 * Controls for a {@link Subscriber} with a {@link Source}, provided by {@link Subscribeable#subscribe(Changes, Subscriber)}.
 */
public interface Subscription {
	
	/**
	 * @return true if this subscription is still active, or false if no further updates will be sent.
	 */
	boolean isActive();
	
	/**
	 * Stop receiving updates. After this, the subscriber is finished and a new one must be created to restart listening for state changes.
	 * Unless otherwise specified by the {@link Source} this should be a non-blocking call.
	 * <p>
	 * After this call, {@link Subscriber#onUpdate(Thing)} will no longer be invoked.
	 * <p>
	 * A note on thread safety: {@link Source}s should guarantee this contract when {@link #stop()} and {@link Subscriber#onUpdate(Thing)}
	 * are on the same thread. However, if {@link #stop()} is called on a different thread than you receive {@link Subscriber#onUpdate(Thing)} calls on,
	 * {@link Source}s make a best attempt but there can be a chance of race conditions. If in this case and it matters for you, check {@link #isActive()} during {@link Subscriber#onUpdate(Thing)}.
	 */
	void stop();
	
	
	/**
	 * A helper method for a common case of stopping a subscription if not null and setting it to null.
	 * So you can do something like `subscription = Subscription.stop(subscription)` to stop and nullify the field.
	 * @param subscription A subscription to stop. Null is allowed.
	 * @return Always returns null.
	 */
	static Subscription stop(Subscription subscription) {
		if (subscription != null) subscription.stop();
		return null;
	}
	
}