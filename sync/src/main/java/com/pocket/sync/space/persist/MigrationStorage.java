package com.pocket.sync.space.persist;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Holder;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link DumbStorage} that on its first call to {@link #restore(Spec, ThingCallback, HolderCallback, ActionCallback, InvalidCallback)}
 * runs a migration step. See the various subclasses of {@link Type} to see the kinds of migrations that can be run.
 * Further calls to restore only load from the main storage.
 * All other method calls always are delegated to the main storage
 */
public class MigrationStorage implements DumbStorage {
	
	private final Type migration;
	private final DumbStorage to;
	private boolean migrated;
	
	/**
	 * @param migration The migration logic
	 * @param to The main storage location
	 */
	public MigrationStorage(Type migration, DumbStorage to) {
		this.migration = migration;
		this.to = to;
	}
	
	@Override
	public void restore(Spec spec, ThingCallback things, HolderCallback holders, ActionCallback actions, InvalidCallback invalids) {
		if (!migrated) {
			if (migration instanceof Source) {
				Source from = (Source) migration;
				List<Thing> outThings = new ArrayList<>();
				Collection<Pair<Holder, Object>> outHolders = new ArrayList<>();
				Map<Action, RemotePriority> outActions = new HashMap<>();
				Collection<String> outIdkeys = new HashSet<>();
				from.restore(spec,
						value -> {
							synchronized (outThings) {
								outThings.add(value);
							}
						},
						h -> {
							for (Map.Entry<Holder, Collection<Object>> e : h.asMap().entrySet()) {
								for (Object o : e.getValue()) {
									outHolders.add(Pair.of(e.getKey(), o));
								}
							}
						},
						outActions::putAll,
						outIdkeys::addAll);
				CountDownLatch latch = new CountDownLatch(1);
				AtomicReference<Throwable> error = new AtomicReference<>();
				to.store(outThings, null, outHolders, null, outActions, null, outIdkeys, null,
						latch::countDown,
						e -> {
							error.set(e);
							latch.countDown();
						});
				try {
					latch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (error.get() != null) throw new RuntimeException("failed to restore", error.get());

			} else if (migration instanceof Access) {
				((Access) migration).transform(spec, to);

			} else {
				throw new RuntimeException("unsupported migration type " + migration);
			}

			migrated = true;
		}
		to.restore(spec, things, holders, actions, invalids);
	}
	
	@Override
	public void store(Collection<Thing> addThings, Collection<Thing> removeThings, Collection<Pair<Holder, Object>> addHolders, Collection<Pair<Holder, Object>> removeHolders, Map<Action, RemotePriority> addActions, Collection<Action> removeActions, Collection<String> addInvalids, Collection<String> removeInvalids, WriteSuccess onSuccess, WriteFailure onFailure) {
		to.store(addThings, removeThings, addHolders, removeHolders, addActions, removeActions, addInvalids, removeInvalids, onSuccess, onFailure);
	}
	
	@Override
	public void clear(WriteSuccess onSuccess, WriteFailure onFailure) {
		to.clear(onSuccess, onFailure);
	}
	
	@Override
	public void release() {
		to.release();
	}

	/** A type of migration. Use one of the subclasses available. */
	public interface Type {}

	/** Reads data from some other source and writes it into the main storage. */
	public interface Source extends Type {
		/** Same as {@link DumbStorage#restore(Spec, ThingCallback, HolderCallback, ActionCallback, InvalidCallback)}. Will only be called once and implementations should clean up any resources afterwards. */
		void restore(Spec spec, ThingCallback things, HolderCallback holders, ActionCallback actions, InvalidCallback invalids);
	}

	/** A very open ended migration that just gives you access to the storage object for you to work with as needed. */
	public interface Access extends Type {
		/** Modify the existing storage as needed. Be sure to block thread until completed. */
		void transform(Spec spec, DumbStorage storage);
	}
	
}
