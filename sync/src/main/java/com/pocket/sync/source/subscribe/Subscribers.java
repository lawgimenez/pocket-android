package com.pocket.sync.source.subscribe;

import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Logs;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper for a source implementing {@link com.pocket.sync.source.Subscribeable#subscribe(Changes, Subscriber)}.
 * Create an instance for your {@link com.pocket.sync.source.Source} and then invoke {@link #add(Changes, Subscriber)} as needed.
 */
public class Subscribers {
	
	private final Set<Pair<Changes<?>, Subscriber>> subscribers = new HashSet<>();
	private final Map<Pair<Changes<?>, Subscriber>, Subscription> subscriptions = new HashMap<>();
	
	private final Space space;
	private final String holderPrefix;
	private final Publisher forget;
	
	private int count;
	
	/**
	 * @param forget A helper for thread safety when handling {@link Subscription#stop()} which may need
	 * to invoke {@link Space#forget(Holder, Thing...)}.  If your implementation should only
	 * interact with the {@link Space} in a synchronized block or specific thread, then
	 * switch over to that context before running the tasks submitted to it, otherwise can just use {@link Publisher#CALLING_THREAD}.
	 */
	public Subscribers(Space space, String holder, Publisher forget) {
		this.space = space;
		this.holderPrefix = holder + "_";
		this.forget = forget;
	}
	
	public synchronized <T extends Thing> Subscription add(Changes<T> changesOf, Subscriber<T> sub) {
		Pair<Changes<?>, Subscriber> pair = Pair.of(changesOf, sub);
		subscribers.add(pair);
		final Holder hold = Holder.session(holderPrefix + ++count);;
		if (changesOf.identity != null) {
			space.remember(hold, changesOf.identity);
		}
		
		Subscription subscription = new Subscription() {
			
			AtomicBoolean isActive = new AtomicBoolean(true);
			
			@Override
			public boolean isActive() {
				return isActive.get();
			}
			
			@Override
			public void stop() {
				// We only flip the active flag here to avoid blocking this calling thread.
				// The rest we'll pass back to the Source to handle in the thread/order they want.
				if (isActive.getAndSet(false)) {
					forget.publish(() -> {
						synchronized (Subscribers.this) {
							subscriptions.remove(pair);
							subscribers.remove(pair);
							if (changesOf.identity != null) {
								space.forget(hold, changesOf.identity);
							}
						}
					});
				}
			}
		};
		subscriptions.put(pair, subscription);
		return subscription;
	}
	
	/**
	 * Push the provided changes out to anyone subscribed to them.
	 * @param changes The diff of what has changed. The {@link com.pocket.sync.space.Change#latest} must be up to date.
	 *                Sources should invoke this method while still within their sync transaction to ensure these values can be relied on.
	 */
	public synchronized void publish(Diff changes) {
		if (changes == null || changes.isEmpty()) return;
		
		for (Pair<Changes<?>, Subscriber> pair : new HashSet<>(subscribers)) { // Loop on a different collection so subscribers can safely unsubscribe during their callback without a concurrent mod exception. REVIEW seems like we should have a thread lock on subscribers to be even safer. but this is at least an improvement
			Changes<?> change = pair.getLeft();
			Subscriber sub = pair.getRight();
			for (Thing changed : changes.currentValues(change)) {
				try {
					// One final check of isActive()
					// This can still have a race condition between this check and invoking onUpdate()
					// This is only a best attempt as described in Subscription.stop()
					if (subscriptions.get(pair).isActive()) sub.onUpdate(changed);
				} catch (Throwable t) {
					// If a subscriber implementation blows up, don't crash the whole Source.
					Logs.printStackTrace(t);
				}
			}
		}
	}
	
	/**
	 * Invokes {@link Subscription#stop()} on all active subscriptions
	 */
	public synchronized void stopAll() {
		for (Subscription s : new HashMap<>(subscriptions).values()) { // Iterate over a copy to avoid concurrent mod exceptions
			s.stop(); // Will handle clearing out the subscriptions values
		}
	}
}
