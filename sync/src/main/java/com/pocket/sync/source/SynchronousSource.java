package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.result.SyncException;

/**
 * A {@link Source} that has a blocking sync implementation.
 */
public interface SynchronousSource extends Source {
	
	/**
	 * Applies any actions provided and then if requested, returns the latest known state of the thing.
	 *
	 * @param thing The thing to return, or null to not return anything.
	 * @param actions Actions, in the order to apply them. Or nothing to not apply any actions.
	 * @param <T> The type of thing
	 * @return If `thing` is set and the source has it available, the latest known state of that thing, or null.
	 * @throws SyncException If there were any errors applying or retrieving
	 */
	<T extends Thing> T sync(T thing, Action... actions) throws SyncException;
	
}
