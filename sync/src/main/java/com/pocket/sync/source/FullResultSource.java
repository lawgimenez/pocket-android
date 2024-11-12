package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.result.SyncResult;

/**
 * A synchronous {@link Source} that can return a full {@link SyncResult}.
 * Used if a source returns action by action results or resolved things.
 */
public interface FullResultSource extends Source {
	
	/**
	 * Applies the actions provided and retrives the requested thing if known.
	 * No exceptions are thrown, instead a {@link SyncResult} is returned that has the result for each action for
	 * any request thing.
	 *
	 * @param thing The thing to retrieve. See {@link SyncResult#returned_t} for the result.
	 * @param actions Actions, in the order to apply them. Or nothing to not apply any actions. See {@link SyncResult#result_a} for the result of each action. Also see {@link SyncResult#resolved} for any things that have resolved data related to one of these actions.
	 * @param <T> The type of thing
	 * @return The result of the sync.
	 */
	<T extends Thing> SyncResult<T> syncFull(T thing, Action... actions);
}
