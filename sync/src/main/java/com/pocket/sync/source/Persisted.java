package com.pocket.sync.source;

import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;

/**
 * Something, likely a {@link Source}, that offers methods for controlling what {@link Thing}s are kept in {@link Space}.
 * @see AsyncPersisted
 */
public interface Persisted {
	/**
	 * See {@link Space#remember(Holder, Thing...)}
	 */
	void remember(Holder holder, Thing... identities);

	/**
	 * If this thing doesn't exist in the local {@link Space}, imprint it.
	 * If it is already present, this has no effect.
	 * This can be considered as built in local action, in that this will be performed internally like a sync where this effect is applied.
	 * Note: just like other sync calls, this can trigger subscription changes.
	 */
	void initialize(Thing thing);
	
	/**
	 * See {@link Space#forget(Holder, Thing...)}
	 */
	void forget(Holder holder, Thing... identities);
	
	/**
	 * See {@link Space#contains(Thing...)}
	 */
	boolean[] contains(Thing... things);
	
	/**
	 * See {@link Space#contains(String...)}
	 */
	boolean[] contains(String... idkeys);
}
