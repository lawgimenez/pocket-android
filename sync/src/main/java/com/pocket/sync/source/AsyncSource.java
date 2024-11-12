package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.result.SyncException;

/**
 * An asynchronous {@link Source}.
 */
public interface AsyncSource extends Source {
	
	/**
	 * Asynchronously applies any actions provided and then if requested, returns the latest known state of a thing.
	 *
	 * @param thing The thing to return, or null to not return anything.
	 * @param actions Actions, in the order to apply them. Or nothing to not apply any actions.
	 * @param <T> The type of thing
	 * @return If `thing` is set and the source has it available, the latest known state of that thing, or null.
	 * @throws SyncException If there were any errors applying or retrieving
	 */
	<T extends Thing> PendingResult<T, SyncException> sync(T thing, Action... actions);
	
	/**
	 * @return A {@link PendingResult} that will be complete when all currently active async work is completed. If more work is added after this call it may also wait for that.
	 * Success here means it awaited all work (or there was none to await). Failure here means for some reason it was interrupted or could not await, some work may still be running.
	 */
	PendingResult<Void, Throwable> await();
}
