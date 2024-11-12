package com.pocket.sync.source;

import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;

/**
 * Something, likely a {@link Source}, that offers a way to subscribe to changes to {@link Thing}s.
 */
public interface Subscribeable {
	
	/**
	 * Listen to future state changes.
	 * <p>
	 * Note: If you use {@link Changes#of(Thing)}, this will as a convenience for you also remember this thing with a {@link com.pocket.sync.space.Holder.Hold#SESSION} for you as long as this subscription is still active.
	 * Changes that are only based on type, such as {@link Changes#of(Class)} will not create any holds automatically on any state.
	 *
	 * @param change The type of change to watch for. Use static methods in {@link Changes} to describe. This may describe one or many things.
	 * @param sub A callback to be invoked anytime a change occurs that matches the criteria. This will be invoked for every new change until {@link Subscription#stop()}.
	 *            If your criteria matches multiple things this same callback will be invoked for each one.
	 * @return A {@link Subscription} that can be used to later unsubscribe.
	 */
	<T extends Thing> Subscription subscribe(Changes<T> change, Subscriber<T> sub);
	
}
