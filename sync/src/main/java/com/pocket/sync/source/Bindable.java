package com.pocket.sync.source;

import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.result.SyncException;

/**
 * A {@link Subscribeable} that also offers a bind method, which combines retrieving immediately and subscribing for future changes.
 */
public interface Bindable extends Subscribeable {
	
	/**
	 * Subscribes to future updates to this Thing and also asynchronously syncs returning the result to the callback.
	 * A convenience for syncing something, then subscribing to it, all to the same callback.
	 *
	 * @param thing    What to retrieve and listen to
	 * @param subscriber Where the initial and future updates will go
	 * @param onFailure  Optional, get notified if the initial sync attempt fails or if the initial request returns null. Failure does not cancel the subscription, so it may get a future update.
	 */
	<T extends Thing> Subscription bind(T thing, Subscriber<T> subscriber, BindingErrorCallback onFailure);
	
	interface BindingErrorCallback {
		/**
		 * Invoked if the initial sync threw an exception or returned null.
		 *
		 * @param error An exception if thrown, or null if the sync completed but returned null.
		 * @param subscription The same subscription that was returned to the original {@link #bind(Thing, Subscriber, BindingErrorCallback)} call.
		 */
		void onBindingError(SyncException error, Subscription subscription);
	}
}
