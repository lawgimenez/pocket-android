package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.PublishingSubscriber;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscribers;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.source.subscribe.WrappedSubscription;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.source.threads.ThreadPools;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Resolver;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Logs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Source} built for a user facing app that maintains its own local state and syncs with a remote source.
 * It has several optimizations to prioritize doing local work as fast as possible, speed up remote work and never block local work with remote work.
 * <p>
 * <h2>Features and behaviour
 * <ul>
 * <li>Thread safe. Can be used as a app wide singleton, invoked from many features and threads at the same time.
 * <li>The calling threads will never be significantly blocked and all methods are async and safe to invoke from a ui thread.
 * <li>Local work will never be blocked by remote work, so local {@link Action}s will be applied relatively quickly and {@link Thing}s that are available locally will be retrieved quickly.
 * <li>{@link Thing}s that have an active {@link Subscription} will receive updates very quickly when {@link Action}s are applied locally. So UI that is bound or subscribed may get updates quickly enough that UI can feel responsive when actions are triggered.
 * <li>It can process a backlog of many remote requests quickly.
 * </ul>
 * To accomplish this, there is an important note to make:
 * <b>Work is guaranteed to be sequential — in call order — locally, but does not guarantee this if remote work is needed.</b>
 * All work is first done locally, in the order received. If any remote work is needed, such as asking for a thing that doesn't
 * exist locally yet, applying a remote only action, or when using {@link #syncRemote(Thing, Action...)}, the remote work will be done in separate threads,
 * allowing local work to continue in the meantime. When that remote work completes it may be as if it occurred
 * later, at the time it was received by the remote.
 * If you must have pure in-order calls, you can still achieve this by having each call be blocked before doing the next call, using {@link PendingResult#get()}.
 *
 * <h2>Implementation Details</h2>
 * This can be thought to have 3 different realms or levels that it is concerned with:
 * 1. The active level, {@link Thing}s that the user is actively seeing, interacting with. Represented by things with an active {@link Subscription}.
 * 2. The local persistence level, {@link Thing}s that have active {@link Holder}s and are persisted across the app closing and opening.
 * 3. The remote level, keeping the remote source up to date with changes happening in this app and syncing with changes made elsewhere.
 * <p>
 * Similarly, there are also 3 thread spaces involved here:
 * 1. The calling thread, which is the thread that invokes one if its methods like sync, subscribe, etc.
 * 2. The local thread, an internal thread it uses to process all syncing on the local state.
 * 3. The remote threads, one or many internal threads used to make calls to the remote source.
 *
 * <h3>Active/Calling</h3>
 * All methods, unless otherwise noted are async and non-blocking. They will return immediately and are safe to invoke from a ui thread.
 * This also includes invoking {@link Subscription#stop()}.
 *
 * <h3>Local</h3>
 * There is a single threaded pool that handles all local work, in the order it was invoked. This handles applying actions,
 * getting {@link Thing}s, and other work on the local persisted {@link Space} that was provided in the constructor.
 * All work is done locally first and then if anything needs to be done remotely it adds a task to the remote queue and releases
 * the local queue back to work on the next local task in the meantime. Any actions that are applied locally are persisted
 * to be sent to the remote whenever the next remote sync occurs.
 * <p>
 * Since this uses a {@link com.pocket.sync.space.mutable.MutableSpace}, most local work should be extremely fast and changes
 * should be pushed out to subscribers fast enough to be usable for ui responsiveness.
 *
 * <h3>Remote</h3>
 * This implementation tries to avoid working with the remote unless it needs to or is directed to. See each method's doc
 * for details on how and when it might go to the remote.
 * <p>
 * There is a remote thread pool that handles all connections to the remote source. Each time it contacts the remote,
 * it will handle grabbing any pending actions and sending them along with the request. Once it receives the response
 * from the remote, it passes it back to the local thread to imprint. This frees up the remote thread to make the next
 * remote request if there is one. This means that the local thread can be writing/imprinting the previous remote calls
 * response while the next remote call is happening. During this work in parallel can vastly improve the speed of making many
 * back to back remote calls.
 * <p>
 * However this also means that while the remote request is being made, there could have been changes made locally.
 * In sync, we can only trust a source's response if all of the actions we know about were sent to the source.
 * So we detect the case where actions happened locally while waiting for a remote, and if so, we discard the remote
 * response and try it again after sending all known actions.
 * <p>
 * To help speed up remote work even more, this implementation allows several remote threads to be spun up if there is a
 * backlog of remote work waiting to be done. However, if there are actions waiting to go out, it will only allow one
 * remote thread at a time. Once all actions have been sent to the remote, then it allows multiple threads to grab things
 * from the remote. If there is no remote work, those threads will be spun back down.
 *
 * <h3>Callbacks</h3>
 * To control what thread callbacks, such as {@link PendingResult.SuccessCallback}, {@link PendingResult.ErrorCallback}, {@link Subscriber}, and {@link BindingErrorCallback}
 * are invoked on, supply that logic via the `publisher` parameter on the constructor. There are a few important characteristics to note:
 * <p>
 * If your {@link Publisher} is asynchronous, meaning it jumps to another thread to run the callback, that will mean that callbacks
 * will run independently of the result of a sync or other method. Such that when blocking on {@link PendingResult#get()},
 * there will be no guarantee that your callbacks will run before or after {@link PendingResult#get()} releases. This behaviour is fine
 * but just worth noting its impact.
 * <p>
 * If your {@link Publisher} is synchronous and runs immediately on the invoking thread, such as {@link Publisher#CALLING_THREAD},
 * then your callback will run on some internal thread of this source. It does mean that all of your callbacks will be guarenteed to be
 * completed by the time {@link PendingResult#get()} releases.  However, the callbacks will not be allowed to invoke {@link PendingResult#get()}
 * from within the callback itself. This would cause a thread lock, and so is not allowed and an exception will be thrown. If your publisher
 * is synchronous and you have a callback that needs to invoke {@link PendingResult#get()}, it should jump to another thread before doing so.
 * <p>
 * If you have a case where for one off callbacks you want to have it use a different one than the default {@link Publisher} you can do so by:
 * <ul>
 *     <li>For {@link PendingResult}, use {@link PendingResult#publisher(Publisher)}</li>
 *     <li>For {@link Subscriber}s such as when using subscribe() or bind(), use a {@link PublishingSubscriber} and it will leave it up to that implementation to handle.</li>
 *     <li>For {@link BindingErrorCallback}s no such override is provided. If we find a use case for this we can add support here, but for now this callback is always on the publisher.</li>
 * </ul>
 * This ensures the thread safety contract of {@link Subscription#stop()} by using protections built into {@link PublishingSubscriber}.
 */
public class AppSource implements AsyncClientSource {
	private static final String APP_SOURCE_THREAD_PREFIX = "as_";

	private final Spec spec;
	private final Resolver resolver;
	private final Publisher publisher;
	
	private final Space space;
	private final Subscribers subscribers;
	/** The local task queue. See {@link #locally(Integer, Task, TaskError)} for submitting work to this queue. */
	private final ThreadPools.PrioritizedPool localThread;
	/** An incrementing id used to give each local task a unique id, in the order it was invoked. These ids may be used when the same body of work is jumping between local and remote to keep priority. */
	private final AtomicInteger nextTransactionId = new AtomicInteger(1);
	/** A cache of all actions that need to be sent to the remote. Should mirror {@link Space#getActions()} with the addition of any {@link RemotePriority#REMOTE} actions pending as well. */
	private final PendingActions pendingActions = new PendingActions();
	/** Things that are currently in progress of being resynced via {@link #syncInvalidated()}. Avoids trying to duplicate the work to resync these. See {@link #syncInvalidated()} for more. */
	private final Set<Thing> revalidating = new HashSet<>();
	/** Tracks what work is currently active. */
	private final WorkTracker workTracker = new WorkTracker();
	
	/** The remote task queue. Managed by {@link #syncRemote(int, Thing, RemoteCallback)}. */
	private final ThreadPools.PrioritizedPool remoteThreadPool;
	/** The remote source. Should only be accessed from the remote threads. Only {@link com.pocket.sync.source.SynchronousSource} or {@link FullResultSource} are supported. */
	private final Source remote;
	
	/** @see #autoSendPriorityActions(boolean) */
	private boolean autoSendActions = true;
	/** @see #autoSyncInvalidatedThings(boolean) */
	private boolean autoSyncInvalidated = true;
	/** @see #errorMonitor(ErrorMonitor) */
	private ErrorMonitor errorMonitor;
	
	/** Creates a new thread pool that will perform {@link TaskWrapper}s that have a lower id before those with a higher one. */
	private static ThreadPools.PrioritizedPool queue(int numberOfThreads, String namePrefix, ThreadPools pools) {
		return pools.newPrioritizedPool(
				namePrefix, numberOfThreads, numberOfThreads,
				10, TimeUnit.SECONDS,
				numberOfThreads > 1);
	}
	
	/**
	 * @param spec The spec to use for this source locally and remotely.
	 * @param space The local, persisted space implementation.
	 * @param remote The remote source. Only {@link com.pocket.sync.source.SynchronousSource} or {@link FullResultSource} are supported. If {@link LimitedSource} is implemented, it may use its interfaces to avoid some remote work as well.
	 * @param resolver If needed, a {@link Resolver}
	 * @param publisher Where to publish callbacks. See the "Callbacks" section of the main {@link AppSource} docs for more details.
	 */
	public AppSource(Spec spec, Space space, Source remote, Resolver resolver, Publisher publisher, ThreadPools pools) {
		if (spec == null) throw new NullPointerException("spec may not be null");
		if (space == null) throw new NullPointerException("space may not be null");
		if (remote == null) throw new NullPointerException("remote may not be null");
		if (!(remote instanceof SynchronousSource) && !(remote instanceof FullResultSource)) throw new NullPointerException("this type of source is not supported as a remote yet."); // If you hit this error, you can add new sources down in syncRemote()
		this.localThread = queue(1, APP_SOURCE_THREAD_PREFIX + "local", pools);
		this.remoteThreadPool = queue(4, APP_SOURCE_THREAD_PREFIX + "remote", pools);
		this.spec = spec;
		this.resolver = resolver != null ? resolver : Resolver.BASIC;
		this.space = space;
		this.remote = remote;
		this.publisher = runnable -> {
			// Don't ever let an outside callback crash our internals
			try {
				publisher.publish(runnable);
			} catch (Throwable t) {
				Logs.printStackTrace(t);
			}
		};
		this.subscribers = new Subscribers(space, "_subs", forget -> locally(i -> forget.run(), ((id, e) -> {})));
	}
	
	@Override
	public Spec spec() {
		return spec;
	}
	
	/**
	 * Automatically attempt to send {@link RemotePriority#SOON} actions shortly after they are applied locally.
	 * This will only be a best attempt and will quietly fail if anything goes wrong, but they will remain in the queue for the next remote sync.
	 * See {@link #syncActions(RemotePriority)} if you need to verify.
	 * @param autoSend true to enable, false to disable. Enabled by default.
	 */
	public void autoSendPriorityActions(boolean autoSend) {
		this.autoSendActions = autoSend;
	}
	
	/**
	 * Automatically attempt to remotely sync any things that are flagged as invalidated. See {@link Space#addInvalid(Thing)} for more details on what invalidated means.
	 * If any of those requests fail, they will fail quietly and those things will remain invalidated in the {@link Space} until they are forgotten or they are remotely synced in the future.
	 *
	 * @param autoSync true to enable, false to disable. Enabled by default.
	 */
	public void autoSyncInvalidatedThings(boolean autoSync) {
		this.autoSyncInvalidated = autoSync;
	}
	
	/**
	 * Receive a callback any time a method results in an error that would invoke a {@link PendingResult.ErrorCallback}.
	 * Can use to monitor what kinds of errors this source runs into.
	 * @param monitor The monitor or null to turn off. Off by default.
	 */
	public void errorMonitor(ErrorMonitor monitor) {
		this.errorMonitor = monitor;
	}
	public interface ErrorMonitor {
		/** An exception occurred during one of the operations. This is invoked within the provided {@link Publisher}. */
		void onError(Throwable e);
	}

	/**
	 * Invokes {@link Subscription#stop()} on all subscribers created by this source.
	 * In order to fulfill the {@link Subscription#stop()} thread safety contract,
	 * this will jump over to the {@link Publisher} and invoke the stop calls there.
	 * This will block until these calls have been made.
	 */
	public void stopAllSubscriptions() {
		CountDownLatch l = new CountDownLatch(1);
		publisher.publish(() -> {
			subscribers.stopAll();
			l.countDown();
		});
		try {
			l.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Perform an operation on the local space. This task will be submitted to the local queue to run.
	 * This is only intended for a parent source or wrapper to use.
	 */
	public void transaction(Transaction transaction) {
		locally(id -> transaction.transact(space), (id, e) -> Logs.printStackTrace(e));
	}
	public interface Transaction {
		void transact(Space space);
	}
	
	
	/**
	 * Add a task to the local queue.
	 * @param id null to add it to the end/tail of the queue, or an existing id if this is a continuation of previous work that should be done before new work.
	 * @param r The task to do on the local thread.
	 * @param e What to do if any exceptions are thrown while doing the work. All tasks must handle this so all {@link PendingResult}s receive a callback no matter what.
	 */
	private void locally(Integer id, Task r, TaskError e) {
		int useId = id != null ? id : nextTransactionId.getAndAdd(1);
		localThread.submit(new TaskWrapper(useId, i -> {
			pendingActions.init(space);
			r.run(i);
		}, e), useId);
	}
	
	/** Same as {@link #locally(Integer, Task, TaskError)} but always adds to the tail/end of the queue (by passing a null id). */
	private void locally(Task r, TaskError e) {
		locally(null, r, e);
	}
	
	/** A task in either the local or remote queue. */
	private interface Task {
		/** @param id A unique id for this task, if you need to reinsert back into the local queue, use this id. When adding work to the remote queue, use this id. */
		void run(int id) throws Exception;
	}
	/** Handler for if a {@link Task} throws an exception. */
	private interface TaskError {
		void onError(int id, Throwable e);
	}
	
	/** A helper for the thread pool to sort/prioritize by id and to catch exceptions. See {@link #queue(int, String, ThreadPools)} for how it sorts tasks in the queue. */
	private class TaskWrapper implements Runnable {
		
		final int id;
		final Task task;
		final TaskError error;
		
		TaskWrapper(int id, Task task, TaskError e) {
			this.id = id;
			this.task = task;
			this.error = e;
			if (e == null) throw new IllegalArgumentException("errors must be considered"); // Make sure you aren't forgetting to handle a callback in a certain case
			workTracker.added(this);
		}
		
		@Override
		public void run() {
			try {
				task.run(id);
			} catch (Throwable t) {
				error.onError(id, t);
			}
			workTracker.finished(this);
		}
	}
	
	
	
	
	/**
	 * Strictly local only sync. Will not go to the remote in order to perform this request.
	 * (Note that depending on {@link #autoSendActions} and {@link #autoSyncInvalidated} settings and the actions that are applied, this may asynchronously queue up remote work)
	 * <p>
	 * Applies actions locally, persisting them to be sent to the remote next time.
	 * If any actions are remote only, local actions will be applied first and then an error will be returned.
	 * Gets or derives the requested thing locally, returns null if unavailable locally.
	 * If abandoned, still applies actions, but will skip getting/deriving the requested thing.
	 */
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncLocal(T thing, Action... actions) {
		Pending<T,SyncException> pending = new Pending<>(publisher);
		SyncResult.Builder<T> results = new SyncResult.Builder<>(thing, actions);
		locally(id -> {
			pending.queueId = id;
			
			// Create a holder for the requested thing for the length of this task
			Holder holder = Holder.session("transaction" + id);
			if (thing != null) space.remember(holder, thing);
			
			// Apply Actions, if any are remote, mark them as failed
			boolean containsSoonActions = false;
			space.startDiff();
			if (actions.length > 0) {
				for (Action action : actions) {
					try {
						if (action.priority() == RemotePriority.REMOTE || action.priority() == RemotePriority.REMOTE_RETRYABLE) throw new RuntimeException("syncLocal does not support remote actions");
						if (action.time() == null) throw new RuntimeException("action is missing time");
						
						spec.apply(action, space);
						if (action.priority() != RemotePriority.LOCAL) {
							space.addAction(action, action.priority());
							pendingActions.add(id, action, null);
							containsSoonActions = containsSoonActions || action.priority() == RemotePriority.SOON;
						}
						results.action(action, Status.SUCCESS, null, "");
					} catch (Throwable t) {
						Status s = SyncException.statusOf(t, action);
						results.action(action, s != null ? s : Status.FAILED, t, null);
					}
				}
			}
			Diff diff = space.endDiff();
			subscribers.publish(diff);
			if (autoSendActions && containsSoonActions) syncActions(RemotePriority.SOON);
			if (results.hasFailures()) {
				throw new SyncException(results.build());
			}
			
			// Get
			T retrieved = null;
			if (thing != null) {
				if (pending.abandoned) throw new RuntimeException("abandoned");
				retrieved = space.get(thing);
				if (retrieved == null) {
					retrieved = space.derive(thing);
					if (retrieved != null) {
						space.startDiff();
						space.imprint(retrieved);
						subscribers.publish(space.endDiff());
					}
				}
				results.thing(retrieved);
			}
			
			// Return
			if (thing != null) space.forget(holder);
			if (autoSyncInvalidated && !diff.invalidated.isEmpty()) syncInvalidated();
			pending.success(retrieved);
			
		}, (id, e) -> {
			try {
				if (thing != null) space.forget(Holder.session("transaction" + id), thing);
			} catch (Throwable ignore){}
			SyncException se = SyncException.unwrap(e);
			pending.fail(se != null ? se : new SyncException(results.build(), e));
		});
		return pending;
	}
	
	/**
	 * Syncs locally and then always starts a remote sync.
	 * <p>
	 * Applies actions locally, persisting them to be sent to the remote.
	 * If there is a requested thing, this will not attempt to get or derive it locally, this will always attempt to sync the latest state from the remote.
	 * <p>
	 * Always begins a remote sync, unless there is no work to do, (no requested thing and no actions in this request or pending).
	 * If any of the actions in this request fail remotely or the requested thing fails, this results in an error.
	 * If any of the pending actions fail, they won't themselves cause this to fail unless it prevented a requested thing from being obtained.
	 *
	 * @return If requested, the latest state of the thing after syncing with the remote
	 */
	@Override
	public <T extends Thing> PendingResult<T, SyncException> syncRemote(T thing, Action... actions) {
		return syncRemote(null, thing, actions);
	}
	
	/**
	 * {@link #syncRemote(Thing, Action...)}'s implementation as a separate method so sync() can also invoke it from with an operation.
	 * @param queueId null to add this to the end of the local queue, or an existing id to continue the work of another task. See {@link #locally(Integer, Task, TaskError)}'s id parameter.
	 */
	private <T extends Thing> PendingResult<T, SyncException> syncRemote(Integer queueId, T thing, Action... actions) {
		Pending<T,SyncException> pending = new Pending<>(publisher);
		SyncResult.Builder<T> results = new SyncResult.Builder<>(thing, actions);
		AtomicReference<Holder> holder = new AtomicReference<>();
		locally(queueId, localId -> {
			// Create a holder for the requested thing for the length of this task
			holder.set(Holder.session("transaction" + localId + "remote"));
			if (thing != null) space.remember(holder.get(), thing);
			
			// Apply Actions Locally
			space.startDiff();
			if (actions.length > 0) {
				for (Action action : actions) {
					try {
						if (action.time() == null) throw new RuntimeException("action is missing time");
						spec.apply(action, space);
						if (action.priority() != RemotePriority.LOCAL) {
							pendingActions.add(localId, action, results);
							if (action.priority() != RemotePriority.REMOTE) {
								space.addAction(action, action.priority());
							}
						}
						results.action(action, Status.SUCCESS, null, "");
					} catch (Throwable t) {
						Status s = SyncException.statusOf(t, action);
						results.action(action, s != null ? s : Status.FAILED, t, null);
					}
				}
			}
			Diff diff = space.endDiff();
			subscribers.publish(diff);
			if (results.hasFailures()) {
				throw new SyncException(results.build());
			}
			
			// Create a processor that will deal with the result of the remote sync,
			// this is an instance so we can resubmit it if we need to retry it.
			RemoteCallback processor = new RemoteCallback() {
				
				int retry;
				
				@Override
				public void onRemoteResult(SyncResult<?> result, PendingActions.Payload waypoint) {
					// This callback returns the result of a remote sync, we are still on the remote thread at this point.
					// Jump back to the local thread to process the result, using the original local id so it jumps ahead of the queue.
					RemoteCallback self = this;
					locally(localId, i -> {
						// Process action results
						pendingActions.returnPending(waypoint, result.result_a, space);
						
						// At this point `results` will now contain a result for any action that was part of this syncRemote call.
						// Also set the `thing` result.
						if (thing != null) {
							results.thing(result.result_t.status, result.result_t.cause, result.result_t.message);
						}
						
						// Regardless of errors, imprint any resolved ids.
						space.startDiff();
						for (Thing t : result.resolved) {
							space.imprint(resolver.reduce(t));
						}
						subscribers.publish(space.endDiff());
						
						if (results.hasFailures() || (result.t != null && result.result_t != null && result.result_t.status == Status.NOT_ATTEMPTED)) {
							// TODO Also merge in any unrelated actions that might have caused the thing to fail?
							if (thing != null) space.forget(holder.get());
							pending.fail(new SyncException(result));
							
						} else if (result.returned_t != null) {
							// We can only imprint/trust the result from the remote if we verify no further actions occurred locally
							// since we began the remote request. Or another way of saying this, that we can guarantee the remote result
							// contains the effect of all actions that we know of so far.
							if (pendingActions.hasChangesSince(waypoint)) {
								// Cannot be trusted, must retry before imprinting.
								if (retry < 3) { // Fail safe to avoid a loop that continually hits a remote.
									retry++;
									syncRemote(localId, thing, self);
								} else {
									// TODO Saw this can actually happen during offline downloading for article views,
									// since there are so many downloaders and so many update_offline_status actions.
									// Perhaps we can be smarter here and track diffs since the request was started,
									// and then look at what we'd imprint from this remote call, and if there is overlap, retry, but otherwise consider it safe to imprint.
									if (thing != null) space.forget(holder.get());
									pending.fail(new SyncException(result, "too many retries"));
								}
								
							} else {
								// We can trust this result and can imprint it locally
								Thing imprint = Resolver.resolveAll(result.returned_t, space, resolver);
								space.startDiff();
								space.imprint(imprint);
								space.clearInvalid(imprint);
								subscribers.publish(space.endDiff());
								
								// Make sure we get the instance from the space, so it contains local and remote knowledge together
								T retrieved = space.get(thing);
								space.forget(holder.get());
								if (retrieved != null) {
									pending.success(retrieved);
								} else {
									// This can happen if the space is reset/cleared while we had been waiting for the remote. For example, when Pocket does a logout action and it clears the space's holders.
									// This can also happen if the endpoint thing doesn't have identity.
									if (thing == null) {
										pending.fail(new SyncException(result, "thing is null, but result.returned_t is " + result.returned_t));
									} else {
										pending.fail(new SyncException(result, "Failed retrieving " + thing.type() + " after imprint." +
												" isIdentifiable=" + thing.isIdentifiable() +
												" space.count(null)=" + space.count(null)));
									}
								}
							}
						} else {
							// Nothing to retrieve
							space.forget(holder.get());
							pending.success(null);
						}
					},
					(id, e) -> {
						try {
							if (thing != null) space.forget(holder.get(), thing);
						} catch (Throwable ignore){}
						SyncException se = SyncException.unwrap(e);
						pending.fail(se != null ? se : new SyncException(results.build(), e));
					});
				}
			};
			syncRemote(localId, thing, processor);
			
			if (autoSyncInvalidated && !diff.invalidated.isEmpty()) syncInvalidated();
			
			// This ends the work on the local thread for now, when the remote thread completes its work, it will invoke the processor above,
			// which will handle performing the result callbacks.
		},
		(localId, e) -> {
			try {
				if (thing != null && holder.get() != null) space.forget(holder.get(), thing);
			} catch (Throwable ignore){}
			SyncException se = SyncException.unwrap(e);
			pending.fail(se != null ? se : new SyncException(results.build(), e));
		});
		return pending;
	}
	
	/**
	 * Only intended for {@link #syncRemote(Integer, Thing, Action...)}'s use.
	 * Performs a remote sync on a remote thread and returns the result to the callback
	 * @param parentId The id of the local task that trigger this
	 * @param thing The requested thing
	 * @param callback Where to return the result
	 */
	private void syncRemote(int parentId, Thing thing, RemoteCallback callback) {
		remoteThreadPool.submit(new TaskWrapper(parentId, id -> {
			// Grab any actions that need to be sent to the remote.
			// This will include local pending ones and any remote-only ones waiting to go out.
			PendingActions.Payload pending = null;
			try {
				// Only one remote thread is allowed to send actions, so if there are actions waiting to go out
				// this will block this thread until this is either the thread that will perform the actions,
				// or there are no further actions to send.
				// For consistency we only allow multiple remote threads when there are no actions, and its just strictly a retrieval.
				Action[] sendArray;
				pending = pendingActions.getPending();
				if (!pending.actions.isEmpty()) {
					List<Action> send = new ArrayList<>(pending.actions);
					// Sort actions by time, remove duplicates, and convert to an array.
					Collections.sort(send, (o1, o2) -> Long.compare(o1.time().value, o2.time().value));
					sendArray = new LinkedHashSet<>(send).toArray(new Action[0]);
				} else {
					sendArray = new Action[0];
				}
				
				SyncResult<?> result;
				if (thing == null && sendArray.length == 0) {
					// Nothing to do
					result = new SyncResult.Builder<>(thing, sendArray).build();
					
				} else if (remote instanceof FullResultSource) {
					result = ((FullResultSource) remote).syncFull(thing, sendArray);
					
				} else if (remote instanceof SynchronousSource) {
					try {
						Thing t = ((SynchronousSource) remote).sync(thing, sendArray);
						SyncResult.Builder<Thing> sr = new SyncResult.Builder<>(thing, sendArray);
						if (thing != null) sr.thing(t);
						for (Action a : sendArray) {
							sr.action(a, Status.SUCCESS, null, null);
						}
						result = sr.build();
					} catch (SyncException e) {
						result = e.result;
					}
				} else {
					// Should happen, since we catch this case in the constructor, but throw regardless.
					throw new RuntimeException("unsupported source type");
				}
				callback.onRemoteResult(result, pending);
				
			} catch (Throwable t) {
				// Need to provide our own catch-all here because we absolutely must guarantee that the pending object is released.
				callback.onRemoteResult(new SyncResult.Builder<>(thing, null)
						.thing(Status.FAILED, t, null)
						.build(),
						pending);
			}
		},
		(id, e) -> callback.onRemoteResult(new SyncResult.Builder<>(thing, null).build(), null)), // This shouldn't happen since we have our own catch-all
		parentId);
	}
	
	interface RemoteCallback {
		void onRemoteResult(SyncResult<?> result, PendingActions.Payload pending);
	}
	
	/**
	 * A helper for keeping a list of all actions that need to be sent to the remote
	 * and keeping that synchronized between local and remote threads.
	 */
	private static class PendingActions {
		/** Actions waiting to be sent and optionally a result builder to place the result when obtained. */
		private final HashMap<Action, SyncResult.Builder> actions = new HashMap<>();
		/** true if this has initialized and extracted persisted actions from space. */
		private boolean isInitialized;
		/** The latest local task id that had changes. Used to know if we can trust a remote result or not. */
		private int lastChangeTransaction;
		/** A latch used to help ensure only one remote thread and send actions at a time. */
		private CountDownLatch latch;
		
		synchronized void init(Space space) {
			if (isInitialized) return;
			isInitialized = true;
			for (Action a : space.getActions().keySet()) {
				add(0, a, null);
			}
		}
		
		/** Add an action to be sent to the whenever the next remote sync occurs. */
		synchronized void add(int transactionId, Action action, SyncResult.Builder handler) {
			lastChangeTransaction = transactionId;
			actions.put(action, handler);
		}
		
		/**
		 * Get any actions that need to be sent.
		 * Only one remote thread is allowed to send actions at a time, so if there are actions to send,
		 * the first thread that invokes this will receive the payload to send and any other threads that
		 * invoke this method in the meantime will be blocked on this method until that thread returns
		 * the payload back in {@link #returnPending(Payload, Map, Space)}. The thread that receives the payload
		 * must absolutely invoke that method later, otherwise all remote threads will be locked forever.
		 * <p>
		 * The payload will also include an id to be used to determine if changes have been made during a remote request.
		 *
		 * @return the pending action payload.
		 */
		Payload getPending() throws InterruptedException {
			// Check if there is already a claim on the actions, and if not grab any pending actions and claim/lock for this thread.
			// This is done as a loop in order to avoid race conditions between releasing and grabbing the lock when multiple threads
			// are waiting to grab it.
			CountDownLatch wait;
			do {
				synchronized (this) {
					wait = this.latch;
					if (wait == null) {
						List<Action> pending = new ArrayList<>(actions.keySet());
						if (!pending.isEmpty()) {
							latch = new CountDownLatch(1);
						}
						return new Payload(lastChangeTransaction, pending);
					}
				}
				if (wait != null) { // Note: Intellji says this is always true, but its not... since this is multi threaded.
					wait.await();
				}
			} while (wait != null);
			throw new RuntimeException(); // This shouldn't be reachable.
		}
		
		public synchronized boolean hasChangesSince(Payload waypoint) {
			return lastChangeTransaction > waypoint.sinceId;
		}
		
		/**
		 * Return the results of pending actions. This will release the lock on actions obtained from {@link #getPending()}
		 * and set the results of actions, clearing from space if those actions are completed and don't need to be retried.
		 * This should be run from within the local thread.
		 *
		 * @param payload The payload obtained via {@link #getPending()}
		 * @param results The results of actions from the remote sync
		 * @param space The local space instance to clear actions from
		 */
		public synchronized void returnPending(Payload payload, Map<Action, com.pocket.sync.source.result.Result> results, Space space) {
			List<Action> actionsToClear = new ArrayList<>();
			for (Map.Entry<Action, com.pocket.sync.source.result.Result> ar : results.entrySet()) {
				Action action = ar.getKey();
				com.pocket.sync.source.result.Result result = ar.getValue();
				
				SyncResult.Builder handler = this.actions.get(action);
				if (handler != null) handler.action(action, result);
				
				switch (ar.getValue().status) {
					case IGNORED:
					case SUCCESS:
					case FAILED_DISCARD:
						actionsToClear.add(action);
						break;
					default:
						if (action.priority() == RemotePriority.REMOTE) {
							actionsToClear.add(action); // Never retry a remote only action.
						}
						break;
				}
			}
			if (!actionsToClear.isEmpty()) {
				space.clearActions(actionsToClear.toArray(new Action[0]));
				for (Action a : actionsToClear) {
					this.actions.remove(a);
				}
			}
			if (latch != null) {
				latch.countDown();
				latch = null;
			}
		}
		
		class Payload {
			public final int sinceId;
			public final List<Action> actions;
			Payload(int sinceId, List<Action> actions) {
				this.sinceId = sinceId;
				this.actions = actions;
			}
		}
		
	}
	
	/**
	 * Attempts to work locally but may sync with the remote as needed.
	 * @param thing Attempts to get or derive this locally, otherwise it will obtain it from the remote. Fails if it can't be obtained in either case.
	 * @param actions Applies locally if possible, and if any actions are remote only, this will act like {@link #syncRemote(Thing, Action...)} for the actions, failing if any fail on the remote.
	 */
	@Override
	public <T extends Thing> PendingResult<T, SyncException> sync(T thing, Action... actions) {
		boolean remoteActions = false;
		for (Action a : actions) {
			if (a.priority() == RemotePriority.REMOTE || a.priority() == RemotePriority.REMOTE_RETRYABLE) {
				remoteActions = true;
				break;
			}
		}
		if (remoteActions) {
			return syncRemote(thing, actions);
			
		} else {
			// Attempt a syncLocal first
			// If it fails return an error
			// If it returns null and a thing was requested, do a syncRemote for the thing
			AtomicBoolean abandon = new AtomicBoolean();
			AtomicReference<Pending<T, SyncException>> local = new AtomicReference<>(null);
			AtomicReference<Pending<T, SyncException>> remote = new AtomicReference<>(null);
			Pending<T,SyncException> pending = new Pending<T,SyncException>(publisher) {
				@Override
				public synchronized void abandon() {
					super.abandon();
					abandon.set(true);
					if (local.get() != null) local.get().abandon();
					if (remote.get() != null) remote.get().abandon();
				}
			};
			
			local.set((Pending<T, SyncException>) syncLocal(thing, actions));
			local.get().setAsProxy()
				.onSuccess(r -> {
					if (thing == null || r != null || !isSupportedByRemote(thing)) {
						// Handled locally, no need to go remote
						pending.success(r);

					} else {
						remote.set((Pending<T, SyncException>) syncRemote(local.get().queueId, thing));
						remote.get().setAsProxy()
								.onSuccess(pending::success)
								.onFailure(pending::fail);
						if (abandon.get()) remote.get().abandon();
					}
				})
				.onFailure(pending::fail);
			
			return pending;
		}
	}

	private boolean isSupportedByRemote(Thing thing) {
		if (remote instanceof LimitedSource) return ((LimitedSource) remote).isSupported(thing);
		return true;
	}

	/**
	 * If there are any actions not yet synced with the remote with a type matching the provided value,
	 * this will attempt to sync with the remote, sending all unsent actions (regardless of type).
	 * Success means this either had nothing matching that type or it was successfully sent.
	 * @param type the type to match or null to match any actions
	 */
	@Override
	public PendingResult<Void, SyncException> syncActions(RemotePriority type) {
		Pending<Void, SyncException> pending = new Pending<>(publisher);
		locally(id -> {
			if ((type == null && !space.getActions().isEmpty()) || space.getActions().containsValue(type)) {
				((Pending<Thing, SyncException>) syncRemote(null))
						.setAsProxy()
						.onFailure(pending::fail)
						.onSuccess(v -> pending.success(null));
			} else {
				pending.success(null);
			}
		},
		(id, e) -> {
			SyncException se = SyncException.unwrap(e);
			pending.fail(se != null ? se : new SyncException(new SyncResult.Builder<>(null,null).build(), e));
		});
		return pending;
	}
	
	/**
	 * Invokes a {@link #syncRemote(Thing, Action...)} for any Things that are currently returned in {@link Space#getInvalid()}.
	 * This does not provide any success or error callbacks for these requests, this is just a best attempt with no feedback on results. (if need this could be implemented, but to keep it simple, this isn't available yet)
	 */
	private void syncInvalidated() {
		locally(id ->
				{
					Set<Thing> invalid = space.getInvalid();
					synchronized (revalidating) {
						invalid.removeAll(revalidating);
						revalidating.addAll(invalid);
					}
					for (Thing t : invalid) {
						syncRemote(t).onComplete(() -> {
							synchronized (revalidating) {
								revalidating.remove(t);
							}
						});
					}
				},
				(id, e) -> {});
	}
	
	
	@Override
	public PendingResult<Void, Throwable> remember(Holder holder, Thing... identities) {
		final Pending<Void,Throwable> pending = new Pending<>(publisher);
		locally(id -> {
			space.remember(holder, identities);
			pending.success(null);
		},
		(id, e) -> pending.fail(e));
		return pending;
	}
	
	@Override
	public PendingResult<Void, Throwable> forget(Holder holder, Thing... identities) {
		final Pending<Void,Throwable> pending = new Pending<>(publisher);
		locally(id -> {
			space.forget(holder, identities);
			pending.success(null);
		},
		(id, e) -> pending.fail(e));
		return pending;
	}
	
	@Override
	public PendingResult<Void, Throwable> initialize(Thing thing) {
		final Pending<Void,Throwable> pending = new Pending<>(publisher);
		locally(id -> {
			space.startDiff();
			space.initialize(thing);
			subscribers.publish(space.endDiff());
			pending.success(null);
		},
		(id, e) -> pending.fail(e));
		return pending;
	}
	
	/**
	 * Wraps the subscriber in a PublishingSubscriber that will handle publishing on the intended thread,
	 * or if already a {@link PublishingSubscriber}, leaves it as is.
	 * See the "Callbacks" section of the main java doc for some more context.
	 */
	private <T extends Thing> PublishingSubscriber<T> applyPublisher(Subscriber<T> sub) {
		if (sub instanceof PublishingSubscriber) {
			// This is how implementations can opt-out or override the publisher. See the "Callbacks" section of the main class doc for context.
			return (PublishingSubscriber<T>) sub;
		} else {
			return new PublishingSubscriber<>(sub, publisher);
		}
	}
	
	/**
	 * See the "Callbacks" section for details on what thread the subscriber will be invoked on.
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Thing> Subscription subscribe(Changes<T> change, Subscriber<T> sub) {
		WrappedSubscription wrapped = new WrappedSubscription();
		locally(id -> wrapped.setSubscription(subscribers.add(change, applyPublisher(sub).setSubscription(wrapped))), (id, ignore) -> {});
		return wrapped;
	}
	
	/**
	 * See the "Callbacks" section for details on what thread the subscriber will be invoked on.
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Thing> Subscription bind(T request, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return bindImpl(true, false, request, subscriber, onFailure);
	}

	public <T extends Thing> Subscription bind(boolean forceRemote, T request, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return bindImpl(true, forceRemote, request, subscriber, onFailure);
	}
	
	/**
	 * See the "Callbacks" section for details on what thread the subscriber will be invoked on.
	 * {@inheritDoc}
	 */
	@Override
	public <T extends Thing> Subscription bindLocal(T request, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		return bindImpl(false, false, request, subscriber, onFailure);
	}
	
	private <T extends Thing> Subscription bindImpl(boolean allowRemote, boolean forceRemote, T request, Subscriber<T> subscriber, BindingErrorCallback onFailure) {
		// The subscription must be started before fetching the initial value to avoid missing updates between getting the initial value and starting the subscription
		// However, this introduces a chance that the subscriber is updated before the initial value is fetched and we duplicate a call to onUpdate() when nothing really changed.
		// So we'll need to observe PublishingSubscriber.hasBeenInvoked() before sending that initial value.
		
		// Start the subscription
		PublishingSubscriber<T> trackedSubscriber = applyPublisher(subscriber);
		Subscription originalSub = subscribe(Changes.of(request), trackedSubscriber);
		
		// Kick off the request for its current state
		Pending<T,SyncException> pending = (Pending<T, SyncException>) (forceRemote ? syncRemote(request) : allowRemote ? sync(request) : syncLocal(request));
		
		// Set up the subscription so it automatically abandons the request when stopped, return this subscription in all cases
		WrappedSubscription abandoningSub = new WrappedSubscription() {
			@Override
			public void stop() {
				super.stop();
				locally(i -> pending.abandon(), (id, e) -> {}); // Since Subscription.stop() can be invoked from any thread, make sure we safely submit the abandon as a transaction to maintain ordering and thread safety.
			}
		};
		abandoningSub.setSubscription(originalSub);
		trackedSubscriber.setSubscription(abandoningSub);
		
		// Listen for the request to complete and invoke the subscriber, unless its already been invoked.
		pending
			.setAsProxy()
			.onSuccess(result -> {
				if (trackedSubscriber.hasBeenInvoked()) return;
				if (result == null) {
					if (onFailure != null) publisher.publish(() -> onFailure.onBindingError(null, abandoningSub));
				} else {
					trackedSubscriber.onUpdate(result);
				}
			})
			.onFailure(e -> {
				if (onFailure != null) publisher.publish(() -> onFailure.onBindingError(e, abandoningSub));
			});
		return abandoningSub;
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(String... idkeys) {
		final Pending<boolean[],Throwable> pending = new Pending<>(publisher);
		locally(id -> pending.success(space.contains(idkeys)), (id, e) -> pending.fail(e));
		return pending;
	}
	
	@Override
	public PendingResult<boolean[], Throwable> contains(Thing... things) {
		final Pending<boolean[],Throwable> pending = new Pending<>(publisher);
		locally(id -> pending.success(space.contains(things)), (id, e) -> pending.fail(e));
		return pending;
	}
	
	@Override
	public PendingResult<Void, Throwable> await() {
		return workTracker.await();
	}
	
	private class Pending<T, E extends Throwable> implements PendingResult<T,E> {
		/** A latch that will be used for implementing {@link #get}*/
		private final CountDownLatch latch = new CountDownLatch(1);
		/** Where to invoke callbacks. */
		private Publisher publisher;
		/** Where to invoke {@link ErrorMonitor }callbacks. */
		private Publisher errorMonitorPublisher;
		/** The {@link TaskWrapper#id} that this task was assigned in the local queue. */
		private int queueId;
		/** The success callback, if it has been set. */
		private PendingResult.SuccessCallback<T> onSuccess;
		/** The error callback, if it has been set. */
		private PendingResult.ErrorCallback<E> onFail;
		/** The complete callback, if it has been set. */
		private PendingResult.CompleteCallback onComplete;
		/** The result if it completed successfully */
		private T result;
		/** The error if it completed with a failure. */
		private E error;
		/** If it is completed */
		private boolean isComplete;
		/** If it was abandoned via {@link PendingResult#abandon()} */
		private boolean abandoned;
		
		private Pending(Publisher publisher) {
			this.publisher = publisher;
			this.errorMonitorPublisher = publisher;
		}
		
		private synchronized void success(T result) {
			this.result = result;
			publish();
		}
		
		private synchronized void fail(E error) {
			this.error = error;
			publish();
		}
		
		/**
		 * Flag that this result is going to be used as an internal proxy.
		 * This will set it to use a publisher that calls immediately on the invoking thread.
		 * This is a bit faster since it skips some thread jumping and it also reduces
		 * chances of thread locks if something invokes .get() and the publisher is a one-at-time queue and
		 * it never gets a chance to hear from the proxies callbacks.
		 */
		private synchronized PendingResult<T,E> setAsProxy() {
			return publisher(null);
		}
		
		public synchronized PendingResult<T,E> publisher(Publisher publisher) {
			this.publisher = publisher != null ? publisher : Publisher.CALLING_THREAD;
			return this;
		}
		
		private synchronized void publish() {
			if (error != null && errorMonitor != null) {
				errorMonitorPublisher.publish(() -> {
					if (errorMonitor != null) errorMonitor.onError(error);
				});
			}
			if (abandoned) return;
			isComplete = true;
			latch.countDown();
			if (error == null) {
				if (onSuccess != null) {
					publisher.publish(() -> {
						if (abandoned || onSuccess == null) return;
						onSuccess.onSuccess(result);
					});
				}
			} else {
				if (onFail != null) {
					publisher.publish(() -> {
						if (abandoned || onFail == null) return;
						onFail.onError(error);
					});
				}
			}
			if (onComplete != null) {
				publisher.publish(() -> {
					if (abandoned || onComplete == null) return;
					onComplete.onComplete();
				});
			}
		}
		
		@Override
		public T get() throws E {
			if (Thread.currentThread().getName().startsWith(APP_SOURCE_THREAD_PREFIX)){
				throw new RuntimeException("cannot block from the local or remote thread");
				// This can lead to lock ups. If this is being invoked during a callback, make sure
				// the publisher passed to the constructor is asynchronous and runs on a different
				// thread.
			}
			try {
				latch.await();
				synchronized (this) {
					if (abandoned) return null;
					if (error != null) throw error;
					return result;
				}
				
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public synchronized void abandon() {
			abandoned = true;
			onComplete = null;
			onSuccess = null;
			onFail = null;
		}
		
		@Override
		public synchronized PendingResult<T,E> onSuccess(SuccessCallback<T> successCallback) {
			if (abandoned) return this;
			onSuccess = successCallback;
			
			if (isComplete
			&& error == null
			&& onSuccess != null) {
				publisher.publish(() -> onSuccess.onSuccess(result));
			}
			return this;
		}
		
		@Override
		public synchronized PendingResult<T,E> onFailure(ErrorCallback<E> failureCallback) {
			if (abandoned) return this;
			onFail = failureCallback;
			
			if (isComplete
			&& error != null
			&& onFail != null) {
				publisher.publish(() -> onFail.onError(error));
			}
			return this;
		}
		
		@Override
		public synchronized PendingResult<T,E> onComplete(CompleteCallback callback) {
			if (abandoned) return this;
			onComplete = callback;
			
			if (isComplete) {
				publisher.publish(() -> onComplete.onComplete());
			}
			return this;
		}
	}

	/**
	 * A helper for implementing {@link #await()}
	 * Counts up and down as tasks are added and completed.
	 * Alerts any pending awaits when the count is 0.
	 */
	private class WorkTracker {
		
		private final List<Pending<Void, Throwable>> waiting = new ArrayList<>();
		private int active;
		
		public synchronized void added(TaskWrapper wrapper) {
			active++;
		}
		
		public void finished(TaskWrapper wrapper) {
			boolean check;
			synchronized (this) {
				check = --active == 0 && !waiting.isEmpty();
			}
			if (check) check();
		}
		
		private void check() {
			List<Pending<Void, Throwable>> finish = null;
			synchronized (this) {
				if (active == 0) {
					finish = new ArrayList<>(waiting);
					waiting.clear();
				}
			}
			if (finish != null) {
				for (Pending<Void, Throwable> pending : finish) {
					pending.success(null);
				}
			}
		}
		
		public synchronized PendingResult<Void, Throwable> await() {
			Pending<Void, Throwable> pending = new Pending<>(publisher);
			waiting.add(pending);
			check();
			return pending;
		}
		
	}


}