package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.SyncException;

/**
 * The same methods of {@link SynchronousRemoteBackedSource} but provided as an asynchronous, callback based API.
 * It also provides a {@link #bindLocal(Thing, Subscriber, Bindable.BindingErrorCallback)} method.
 */
public interface AsyncRemoteBackedSource extends AsyncSource, RemoteBackedSource {
	
	/**
	 * Asynchronously attempts to do a local only sync but will go out to the remote if needed.
	 * <p>
	 * Applies any actions provided and then if requested, returns the latest known state of the thing.
	 * Actions will also be persisted and sent to the remote next time a remote sync occurs.
	 * If any of the actions are remote-only this will sync with the remote as part of this call.
	 * If the requested thing is not known locally, it will sync with the remote to fetch it as part of this call.
	 *
	 * @param thing The thing to return, or null to not return anything.
	 * @param actions Actions to apply. Or nothing to not apply any actions.
	 * @return A {@link PendingResult} of this work.
	 */
	@Override
	<T extends Thing> PendingResult<T, SyncException> sync(T thing, Action... actions);
	
	/**
	 * Asynchronously syncs locally and then always starts a remote sync.
	 * <p>
	 * Applies actions locally, persisting them to be sent to the remote next time this syncs with the remote.
	 * If there is a requested thing, this will not attempt to get or derive it locally, this will always attempt to sync the latest state from the remote.
	 * <p>
	 * Always begins a remote sync, unless there is no work to do, (no requested thing and no actions in this request or pending).
	 * If any of the actions in this request fail remotely or the requested thing fails, this results in an error.
	 * If any of the pending actions fail, they won't themselves cause this to fail unless it prevented a requested thing from being obtained.
	 *
	 * @param thing The thing to return, or null to not return anything.
	 * @param actions Actions to apply. Or nothing to not apply any actions.
	 * @return A {@link PendingResult} of this work.
	 */
	<T extends Thing> PendingResult<T, SyncException> syncRemote(T thing, Action... actions);
	
	/**
	 * Asynchronously and strictly local only sync. Will not go to the remote.
	 * <p>
	 * Applies actions locally, persisting them to be sent to the remote next time.
	 * If any actions are remote only, local actions will be applied first and then an error will be returned.
	 * Gets or derives the requested thing locally, returns null if unavailable locally.
	 *
	 * @param thing The thing to return, or null to not return anything.
	 * @param actions Actions to apply. Or nothing to not apply any actions.
	 * @return A {@link PendingResult} of this work.
	 */
	<T extends Thing> PendingResult<T, SyncException> syncLocal(T thing, Action... actions);
	
	/**
	 * Asynchronously checks if there are any actions not yet synced with the remote with a type matching the provided value,
	 * this will attempt to sync with the remote, sending all unsent actions (regardless of type).
	 * Success means this either had nothing matching that type or it was successfully sent.
	 * @param type the type to match or null to match any actions
	 * @return A {@link PendingResult} of this work.
	 */
	PendingResult<Void, SyncException> syncActions(RemotePriority type);
	
	
	
	<T extends Thing> Subscription bindLocal(T thing, Subscriber<T> subscriber, Bindable.BindingErrorCallback onFailure);
}
