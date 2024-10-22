package com.pocket.sync.source.subscribe;

/**
 * Helper for proxying/wrapping a subscription. That subscription doesn't have to be available right away, you can set it later with {@link #setSubscription(Subscription)}
 */
public class WrappedSubscription implements Subscription {
	
	private boolean isActive = true;
	private Subscription wrapped;
	
	public synchronized void setSubscription(Subscription subscription) {
		this.wrapped = subscription;
		if (wrapped != null && !isActive && wrapped.isActive()) {
			wrapped.stop();
			wrapped = null;
		}
	}
	
	@Override
	public synchronized boolean isActive() {
		if (wrapped != null && !wrapped.isActive()) {
			isActive = false;
			wrapped = null;
		}
		return isActive;
	}
	
	@Override
	public synchronized void stop() {
		isActive = false;
		if (wrapped != null && wrapped.isActive()) {
			wrapped.stop();
			wrapped = null;
		}
	}
}
