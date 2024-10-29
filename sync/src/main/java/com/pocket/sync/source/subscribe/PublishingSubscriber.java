package com.pocket.sync.source.subscribe;

import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.thing.Thing;

/**
 * A {@link Subscriber} that will proxy its {@link #onUpdate(Thing)} to another thread or some custom calling logic via a {@link Publisher}.
 * To ensure {@link #onUpdate(Thing)} won't be called after {@link Subscription#stop()}, avoiding race conditions, set the subscription with {@link #setSubscription(Subscription)}.
 */
public class PublishingSubscriber<T extends Thing> implements Subscriber<T> {
	
	private final Subscriber<T> target;
	public final Publisher publisher;
	private Subscription subscription;
	private boolean updated;
	
	/**
	 * @param target What to invoke onUpdate
	 * @param publisher Where to invoke it
	 */
	public PublishingSubscriber(Subscriber<T> target, Publisher publisher) {
		this.target = target;
		this.publisher = publisher;
	}
	
	/**
	 * If your {@link Publisher} is async, you can set its {@link Subscription} here and before invoking the target's onUpdate
	 * call, it will double check {@link Subscription#isActive()}. This can help avoid race conditions between stop and update being called.
	 */
	public PublishingSubscriber<T> setSubscription(Subscription subscription) {
		this.subscription = subscription;
		return this;
	}
	
	/**
	 * @return true if {@link #onUpdate(Thing)} has been invoked at least once.
	 */
	public boolean hasBeenInvoked() {
		return updated;
	}
	
	@Override
	public void onUpdate(T thing) {
		updated = true;
		publisher.publish(() -> {
			if (subscription == null || subscription.isActive()) {
				target.onUpdate(thing);
			}
		});
	}
	
}
