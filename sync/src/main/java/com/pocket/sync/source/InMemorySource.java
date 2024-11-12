package com.pocket.sync.source;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscriber;
import com.pocket.sync.source.subscribe.Subscribers;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.source.threads.Publisher;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;

/**
 * A simple in-memory only source.
 */
public class InMemorySource<S extends Spec> implements Subscribeable, Specd, Persisted, SynchronousSource {
	
	private final Space space = new MutableSpace();
	private final Subscribers subscribers;
	private final S spec;
	private int transaction;
	
	public InMemorySource(S spec) {
		this.subscribers = new Subscribers(space, "link", Publisher.CALLING_THREAD);
		this.spec = spec;
		space.setSpec(spec);
	}
	
	@Override
	public synchronized <T extends Thing> T sync(T requested, Action... actions) throws SyncException {
		SyncResult.Builder<T> sr = new SyncResult.Builder<>(requested, actions);
		Holder holder = Holder.session("transaction" + transaction++);
		T result = null;
	
		space.startDiff();
		if (requested != null) space.remember(holder, requested);
		
		if (actions.length > 0) {
			for (Action a : actions) {
				try {
					spec.apply(a, space);
				} catch (Throwable t) {
					Status s = SyncException.statusOf(t, a);
					sr.action(a, s != null ? s : Status.FAILED, t, null);
				}
			}
			
			subscribers.publish(space.endDiff());
			
			if (sr.hasFailures()) {
				space.forget(holder);
				throw new SyncException(sr.build());
			}
		}
		if (requested != null) {
			result = space.get(requested);
			if (result == null) {
				result = space.derive(requested);
				if (result != null) space.imprint(result);
			}
		}
		
		space.forget(holder);
		return result;
	}
	
	@Override
	public synchronized <T extends Thing> Subscription subscribe(Changes<T> target, final Subscriber<T> sub) {
		return subscribers.add(target, sub);
	}
	
	@Override
	public synchronized void remember(Holder holder, Thing... identities) {
		space.remember(holder, identities);
	}
	
	@Override
	public void initialize(Thing thing) {
		space.startDiff();
		space.initialize(thing);
		subscribers.publish(space.endDiff());
	}
	
	@Override
	public synchronized void forget(Holder holder, Thing... identities) {
		space.forget(holder, identities);
	}
	
	@Override
	public boolean[] contains(Thing... things) {
		return space.contains(things);
	}
	
	@Override
	public boolean[] contains(String... idkeys) {
		return space.contains(idkeys);
	}
	
	@Override
	public S spec() {
		return spec;
	}
	
}
