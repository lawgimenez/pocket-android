package com.pocket.sync.source;

import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;

/**
 * The same methods found in {@link Persisted} but provided as an asynchronous callback based API.
 */
public interface AsyncPersisted {
	/**
	 * An async version of {@link Persisted#remember(Holder, Thing...)} but asynchronous.
	 * @return {@link PendingResult} of this.
	 */
	PendingResult<Void, Throwable> remember(Holder holder, Thing... identities);
	
	/**
	 * An async version of {@link Persisted#forget(Holder, Thing...)} but asynchronous.
	 * @return {@link PendingResult} of this.
	 */
	PendingResult<Void, Throwable> forget(Holder holder, Thing... identities);
	
	/**
	 * If this thing doesn't exist in the local {@link Space}, imprint it.
	 * If it is already present, this has no effect.
	 * This can be considered as built in local action, in that this will be performed internally like a sync where this effect is applied.
	 * Note: just like other sync calls, this can trigger subscription changes.
	 * @return {@link PendingResult} of this.
	 */
	PendingResult<Void, Throwable> initialize(Thing thing);
	
	/**
	 * An async version of {@link Persisted#contains(String...)} but asynchronous.
	 * @return {@link PendingResult} of this.
	 */
	PendingResult<boolean[], Throwable> contains(String... idkeys);
	
	/**
	 * An async version of {@link Persisted#contains(Thing...)} but asynchronous.
	 * @return {@link PendingResult} of this.
	 */
	PendingResult<boolean[], Throwable> contains(Thing... things);
}
