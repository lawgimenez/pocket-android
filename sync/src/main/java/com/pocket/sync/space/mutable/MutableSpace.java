package com.pocket.sync.space.mutable;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Change;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.persist.DumbStorage;
import com.pocket.sync.space.persist.SpaceRestoreException;
import com.pocket.sync.spec.Reactions;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingUtil;
import com.pocket.sync.value.Include;
import com.pocket.util.java.Logs;
import com.pocket.util.java.KeyLatch;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A {@link Space} implementation that uses the idea of mutable things as its implementation.
 * It aims to be as fast as possible so it can be fast enough for UI responsiveness.
 * <p>
 * By default it only persists in memory, but has an optional constructor for also dumping its data to some {@link DumbStorage}.
 * <p>
 * <h2>Design</h2>
 * All {@link Thing} instances are immutable. This is great for a lot of aspects of the app, but internally, within a {@link Space},
 * this can cause a lot of extra work and overhead when trying to imprint changes. Especially when making a change to a thing
 * that is referenced by another thing. If Thing A contains thing B, and a change happens to Thing B, the space must take the immutable
 * thing A, find where it references Thing B, swap it out with the new B instance and rebuild its immutable instance.
 * <p>
 * This can be complex to manage and make sure you are updating the states correctly and adds a lot of work and overhead.
 * <p>
 * This design focuses on converting all things to a {@link MutableThing} which references other things also as {@link MutableThing}s.
 * Then when a change is made to Thing B, Thing A is already directly referencing the mutable B and instantly and inherently already has
 * the change, no traversing of the tree or swapping out values is needed.
 * <p>
 * This relies on the code generated {@link MutableThing} methods combined with the {@link Mutables} interface to accomplish this.
 * <p>
 * See those classes for some additional design information.
 */
public class MutableSpace implements Space {
	
	/** Holders to the idkeys they are remembering. */
	private final Multimap<Holder, String> holdersToIdkeys = MultimapBuilder.hashKeys().hashSetValues().build();

	/**
	 * Holders to the matches they are remembering.
	 * @Deprecated No longer needed since we removed rememberWhere... except we need them for the 55% of beta users that used this.  We can remove this and all of its related code after we see there are no more active beta users on pre 7.21.0.5 versions. https://app.mode.com/getpocket/reports/9b9882efafde/runs/4cc074c9803f See the `remove-rememberWhere-fully` branch for suggestions on clean up
	 */
	private final Multimap<Holder, Thing> holdersToMatches = MultimapBuilder.hashKeys().arrayListValues().build();
	
	/** A list of {@link Thing#idkey()} that has been invalidated. Used to track for {@link #addInvalid(Thing)} related methods. */
	private final Set<String> invalid = new HashSet<>();
	
	/** Used to track for {@link #addAction(Action, RemotePriority)} related methods. */
	private final Map<Action, RemotePriority> actions = new HashMap<>();
	
	/** Used to implement {@link #startDiff()} and {@link #endDiff()} */
	private Diff openDiff;
	
	private Spec spec;
	
	
	
	/** The things currently in this space. The key is the identity of a thing, the value is its corresponding {@link MutableThing}. */
	private final Map<Thing, MutableThing> things = new HashMap<>();
	
	/** An index to lookup values in {@link #things} their type. The things in here should be the latest from {@link #things}. */
	private final Multimap<String, Thing> typeIndex = MultimapBuilder.hashKeys().hashSetValues().build();
	
	/** Key is {@link Thing#idkey()}, value is {@link Thing#identity()}. Use the returned identity to do a lookup. */
	private final Map<String, Thing> idkeys = new HashMap<>();
	
	/** An index of what things reference each other. */
	private final References<MutableThing> references = new References<>();
	
	/** Things that have flat changes since the last {@link #endDiff()} call. */
	private final Set<MutableThing> changed = new HashSet<>();
	
	/** Things that reference (at some depth) something in {@link #changed} */
	private final Set<MutableThing> changedRefs = new HashSet<>();
	
	/** Things that should be checked if they need to be cleaned. */
	private final Set<MutableThing> pendingClean = new HashSet<>();
	
	/** An implementation of the interface we pass into {@link MutableThing}. */
	private final Helper helper = new Helper();
	
	/** An implementation of the interface we pass around as needed. */
	private final Selector selector = new Selector(this);
	
	/** An optional persistence, if non-null, will keep updated as changes occur. */
	private final DumbStorage persistence;
	private final Object persistenceLock = new Object();
	/** Tracks what changed during a transaction. Only used if {@link #persistence} is non-null. */
	private final TransactionDiff transactionDiff = new TransactionDiff();
	/** A latch that can be used to await writes to {@link #persistence}. Only used if {@link #persistence} is non-null. */
	private KeyLatch persistenceLatch;
	/** If this space has been restored from {@link #persistence}. */
	private boolean isWarmedUp;
	
	public MutableSpace(DumbStorage persistence) {
		this.persistence = persistence;
		if (persistence == null) isWarmedUp = true;
	}
	
	public MutableSpace() {
		this(null);
	}
	
	interface Transaction<R> {
		R run();
	}
	
	/** A variant of {@link #transaction(Transaction)} where you don't need a return value. */
	private void transaction(Runnable task) {
		transaction(() -> {
			task.run();
			return null;
		});
	}
	
	/**
	 * Represents a single batch of work, most likely representing a single method in the Space API.
	 * Pretty much any public facing method should use this as it will ensure proper setup and tear down of all changes.
	 * During the provided task, modify or retrieve state as needed. If you make any changes, update the various {@link #transactionDiff} fields that were affected.
	 *
	 * REVIEW right now to avoid nested transactions, we use a bunch of ___Internal() like methods.
	 * Perhaps there is a cleaner way to section off access to the data so it is only accessible inside of a transaction
	 */
	private <R> R transaction(Transaction<R> task) {
		// First make sure we loaded in any persisted data. This only needs to happen once at start
		if (!isWarmedUp && persistence != null) {
			try {
				persistence.restore(spec,
						thing -> {
							synchronized (things) {
								MutableThing<Thing> mt = imprint(thing, null);
								Thing id = mt.identity();
								idkeys.put(id.idkey(), id);
							}
						},
						holders -> {
							for (Map.Entry<Holder, Collection<Object>> e : holders.asMap().entrySet()) {
								for (Object held : e.getValue()) {
									if (held instanceof Thing) {
										holdersToMatches.put(e.getKey(), (Thing) held);
									} else {
										holdersToIdkeys.put(e.getKey(), (String) held);
									}
								}
							}
						},
						this.actions::putAll,
						this.invalid::addAll);
				
				// We can assume no reactions or cleaning are needs, so just load up indexes.
				for (MutableThing t : changed) {
					Thing value = t.build();
					t.previous(); // Release a previous instance if it was created
					typeIndex.put(value.type(), value);
				}
				changed.clear();
				changedRefs.clear();
				pendingClean.clear();
				forgetSessionInternal(); // Remove session holders and their things from previous session
			} catch (Throwable t) {
				throw new SpaceRestoreException(t);
			}
			isWarmedUp = true;
		}
		
		// Run the provided transaction
		transactionDiff.reset(); // Ensure, just in case the previous transaction broke
		R result = task.run();
		
		// Async persist changes if needed
		if (persistence != null) {
			if (transactionDiff.hasChanges()) {
				synchronized (persistenceLock) {
					if (persistenceLatch == null) persistenceLatch = new KeyLatch();
					persistenceLatch.hold(task);
				}
				DumbStorage.WriteSuccess success = () -> {
					synchronized (persistenceLock) {
						persistenceLatch.release(task);
						if (persistenceLatch.isOpen()) persistenceLatch = null;
					}
				};
				DumbStorage.WriteFailure failure = e -> {
					Logs.printStackTrace(e);
					synchronized (persistenceLock) {
						persistenceLatch.release(task);
						if (persistenceLatch.isOpen()) persistenceLatch = null;
					}
				};
				persistence.store(
						transactionDiff.addThings.isEmpty()      ? null : new ArrayList<>(transactionDiff.addThings),
						transactionDiff.removeThings.isEmpty()   ? null : new ArrayList<>(transactionDiff.removeThings),
						transactionDiff.addHolders.isEmpty()     ? null : new ArrayList<>(transactionDiff.addHolders),
						transactionDiff.removeHolders.isEmpty()  ? null : new ArrayList<>(transactionDiff.removeHolders),
						transactionDiff.addActions.isEmpty()     ? null : new HashMap<>(transactionDiff.addActions),
						transactionDiff.removeActions.isEmpty()  ? null : new ArrayList<>(transactionDiff.removeActions),
						transactionDiff.addInvalids.isEmpty()    ? null : new ArrayList<>(transactionDiff.addInvalids),
						transactionDiff.removeInvalids.isEmpty() ? null : new ArrayList<>(transactionDiff.removeInvalids),
						success, failure);
			}
		}
		transactionDiff.reset();
		return result;
	}
	
	@Override
	public synchronized MutableSpace setSpec(Spec spec) {
		this.spec = spec;
		return this;
	}
	
	@Override
	public synchronized void initialize(Thing thing) {
		transaction(() -> {
			if (getInternal(thing) == null) {
				imprintInternal(Arrays.asList(thing));
			}
		});
	}
	
	@Override
	public synchronized void remember(Holder holder, Thing... identities) {
		transaction(() -> {
			for (Thing id : identities) {
				holdersToIdkeys.put(holder, id.idkey());
				transactionDiff.add(holder, id.idkey());
			}
		});
	}
	
	@Override
	public synchronized void forget(Holder holder, Thing... identities) {
		transaction(() -> forgetInternal(holder, identities));
	}
	
	/** An internal version of {@link #forgetInternal(Holder, Thing...)}. Meant only to be used within an existing transaction. */
	private void forgetInternal(Holder holder, Thing... identities) {
		if (holder == null) return;
		Collection<String> held = holdersToIdkeys.get(holder);
		Set<MutableThing> clean;
		if (identities == null || identities.length == 0) {
			clean = new HashSet<>(held.size());
			for (String idkey : held) {
				MutableThing mt = getMutable(getInternal(idkey));
				transactionDiff.remove(holder, idkey);
				if (mt != null) clean.add(mt);
			}
			held.clear();
		} else {
			clean = new HashSet<>(identities.length);
			for (Thing id : identities) {
				if (id == null) continue;
				held.remove(id.idkey());
				transactionDiff.remove(holder, id.idkey());
				MutableThing mt = getMutable(getInternal(id.idkey()));
				if (mt != null) clean.add(mt);
			}
		}

		// This code can be cleaned up when holdersToMatches is
		// State declared holds via rememberWhere()
		if (!holdersToMatches.isEmpty()) {
			Collection<Thing> heldMatches = holdersToMatches.get(holder);
			List<Thing> matchesToRelease = new ArrayList<>();
			if (identities == null || identities.length == 0) {
				matchesToRelease.addAll(heldMatches);
				heldMatches.clear();
			} else {
				for (Thing m : identities) {
					Iterator<Thing> it = heldMatches.iterator();
					while (it.hasNext()) {
						if (m.equals(Thing.Equality.STATE_DECLARED, it.next())) {
							matchesToRelease.add(m);
							it.remove();
							break;
						}
					}
				}
			}
			for (Thing m : matchesToRelease) {
				transactionDiff.remove(holder, m);
				for (Thing t : typeIndex.get(m.type())) {
					if (m.equals(Thing.Equality.STATE_DECLARED, t)) {
						clean.add(getMutable(t));
					}
				}
			}
		}

		clean(clean);
	}
	
	private boolean heldByMatch(Thing thing) {
		for (Thing m : holdersToMatches.values()) {
			if (m.equals(Thing.Equality.STATE_DECLARED, thing)) return true;
		}
		return false;
	}
	
	/**
	 * Checks if any of the provided things can be released and removed from the cache.
	 * @param things A set of things to check. This set will be modified and cleared during this process.
	 */
	private void clean(Set<MutableThing> things) {
		// First filter out any that still have a Holder on them
		List<Thing> removed = new ArrayList<>();
		Collection<String> held = holdersToIdkeys.values();
		Iterator<MutableThing> it = things.iterator();
		while (it.hasNext()) {
			if (held.contains(it.next().identity().idkey())) it.remove(); // Still held by something
		}
		
		// Then check if anything references it.
		// We loop here because removing something might release further references and free something else up.
		do {
			for (MutableThing thing : things) {
				if (!references.referencesThis(thing).isEmpty()) continue; // Still referenced
				
				Thing built = thing.build();
				
				if (heldByMatch(built)) continue;
				
				// No holders, no references, good to remove
				this.references.removeReferencesFrom(thing);
				this.things.remove(built);
				this.typeIndex.get(built.type()).remove(built);
				this.idkeys.remove(built.idkey());
				removed.add(built);
			}
			things.clear();
			things.addAll(references.dirty());
			
		} while (!things.isEmpty());
		
		transactionDiff.remove(removed);
	}
	
	@Override
	public synchronized <T extends Thing> T get(T thing) {
		return transaction(() -> getInternal(thing));
	}
	
	@Override
	public synchronized Thing get(String idkey) {
		return transaction(() -> getInternal(idkey));
	}
	
	/** A variant of {@link #get(Thing)} that can be used internally, only within an existing transaction. */
	private <T extends Thing> T getInternal(T thing) {
		MutableThing<T> t = getMutable(thing);
		return t != null ? t.build() : null; // TODO do we need to ensure build() is not invoked during imprints?  make a transaction framework here for protections or make Mutables.imprint(collection) ?
	}
	
	/** A variant of {@link #get(String)} that can be used internally, only within an existing transaction. */
	private Thing getInternal(String idkey) {
		return getInternal(idkeys.get(idkey));
	}
	
	private <T extends Thing> MutableThing<T> getMutable(T thing) {
		return things.get(thing);
	}
	
	@Override
	public synchronized <T extends Thing> T derive(T thing) {
		return transaction(() -> spec.derive().derive(thing, selector));
	}
	
	@Override
	public synchronized <T extends Thing> Collection<T> getAll(String type, Class<T> clazz) {
		return transaction(() -> ThingUtil.castSet(new HashSet<>(typeIndex.get(type)), clazz));
	}
	
	@Override
	public synchronized void imprint(Thing thing) {
		imprint(Collections.singletonList(thing));
	}
	
	@Override
	public synchronized void imprint(Collection<? extends Thing> things) {
		transaction(() -> imprintInternal(things));
	}
	
	/** Internal implementation of {@link #imprint(Collection)}, only meant to be used within an existing transaction. */
	private void imprintInternal(Collection<? extends Thing> things) {
		List<Thing> imprints = new ArrayList<>(things);
		Diff fullDiff = new Diff();

		// Also double check our trackers are cleared
		changed.clear();
		changedRefs.clear();
		pendingClean.clear();
		
		while (!imprints.isEmpty()) {
			// Apply changes
			// changed and changedRefs will be updated during this traversal
			for (Thing t : imprints) {
				imprint(t, null);
			}
			
			// Find anything that needs to change because it references something that changed
			for (MutableThing c : changed) {
				uprefs(c);
			}
			
			// Now everything that is changed is flagged and invalidated
			// Calculate diff and rebuild values
			Diff.Builder db = new Diff.Builder();
			
			changedRefs.removeAll(changed); // Prioritize it as a change vs a changed ref if in both places
			
			for (MutableThing t : changed) {
				Thing previous = t.previous();
				Thing latest = t.build();
				if (previous != null) {
					db.changed(previous, latest);
					// If an existing thing changed, it will have already flagged for cleaning if its references changed,
					// but we need to manually see if it no longer matches any holder matches
					if (heldByMatch(previous) && !heldByMatch(latest)) {
						pendingClean.add(t);
					}
				} else {
					db.added(latest);
					idkeys.put(latest.idkey(), t.identity());
					pendingClean.add(t);  // Flag anything that was added as possible clean up, since nothing may be holding it
				}
				typeIndex.remove(latest.type(), latest);
				typeIndex.put(latest.type(), latest);
			}
			for (MutableThing t : changedRefs) {
				Thing previous = t.previous();
				Thing latest = t.build();
				db.changed(previous, latest);
				typeIndex.remove(latest.type(), latest);
				typeIndex.put(latest.type(), latest);
			}
			
			changed.clear();
			changedRefs.clear();
			pendingClean.addAll(references.dirty()); // Flag anything that had references released as possible cleanup
			
			Diff diff = db.build();
			fullDiff = fullDiff.add(diff);
			
			// Check for rederives
			Reactions reactions = new Reactions();
			for (Change c : diff.changes.values()) {
				c.latest.reactions(c.previous, c.latest, diff, reactions);
			}
			// Convert all types and fields to concrete things and fields
			for (Map.Entry<String, Collection<String>> e : reactions.typesAndFields().asMap().entrySet()) {
				reactions.things(new HashSet<>(typeIndex.get(e.getKey())), e.getValue());
			}
			
			// Iterate through all changes, track anything we need to imprint as a result
			imprints.clear();
			for (Map.Entry<Thing, Collection<String>> e : reactions.thingsAndFields().asMap().entrySet()) {
				Thing t = getInternal(e.getKey()); // TODO can we just use this value if it is from diff? rather than looking up
				Thing t2 = spec.derive().rederive(t, e.getValue(), diff, selector);
				if (!t.equals(Thing.Equality.FLAT, t2)) {
					// Imprint changes in the next loop
					imprints.add(t2);
				}
			}
		}
		
		if (openDiff != null) openDiff = openDiff.add(fullDiff);
		transactionDiff.add(fullDiff.all());
		clean(pendingClean);
	}
	
	private <T extends Thing> MutableThing<T> imprint(T value, MutableThing root) {
		if (value == null) return null;
		if (value.isIdentifiable()) {
			MutableThing<T> entry = getMutable(value);
			if (entry == null) {
				entry = value.mutable(helper, root);
				things.put(value.identity(), entry);
				helper.flagChanged(entry);
			} else {
				entry.imprint(value, helper);
			}
			return entry;
		} else {
			return value.mutable(helper, root);
		}
	}
	
	/**
	 * Recursively find things that reference this, invalidate and add them to {@link #changedRefs}
	 */
	private void uprefs(MutableThing thing) {
		for (MutableThing r : references.referencesThis(thing)) {
			if (changedRefs.add(r)) {
				r.invalidate();
				uprefs(r);
			}
		}
	}
	
	/**
	 * An implementation of {@link Mutables} that will be passed into {@link MutableThing} implementations.
	 */
	class Helper implements Mutables {
		
		@Override
		public <T extends Thing> MutableThing<T> imprint(T value, MutableThing root) {
			return MutableSpace.this.imprint(value, root);
		}
		
		@Override
		public <T extends Thing> List<MutableThing<T>> imprint(List<T> value, MutableThing root) {
			// TODO in code generation, if we already know this hasn't changed... then we can skip all this copying work?
			// is there some way to combine the iteration needed to copy and check? is it better to check, then copy or copy always?
			if (value == null) return null;
			if (value.isEmpty()) return Collections.emptyList();
			List<MutableThing<T>> list = new ArrayList<>(value.size());
			for (T t : value) {
				list.add(imprint(t, root));
			}
			return list;
		}
		
		@Override
		public <T extends Thing> Map<String, MutableThing<T>> imprint(Map<String, T> value, MutableThing root) {
			if (value == null) return null;
			if (value.isEmpty()) return Collections.emptyMap();
			HashMap<String, MutableThing<T>> map = new HashMap<>(value.size());
			for (Map.Entry<String, T> t : value.entrySet()) {
				map.put(t.getKey(), imprint(t.getValue(), root));
			}
			return map;
		}
		
		@Override
		public void flagChanged(MutableThing thing) {
			thing.invalidate();
			changed.add(thing);
		}
		
		@Override
		public void link(MutableThing parent, MutableThing child) {
			if (child == null) return;
			if (child.identity().isIdentifiable()) {
				if (parent.root() != null) references.link(parent.root(), child);
			} else {
				link(parent, child.references());
			}
		}
		
		@Override
		public void link(MutableThing parent, Collection<? extends MutableThing> children) {
			if (children == null || children.isEmpty()) return;
			for (MutableThing t : children) link(parent, t);
		}
		
		@Override
		public void link(MutableThing parent, Map<String, ? extends MutableThing> children) {
			if (children == null || children.isEmpty()) return;
			for (MutableThing t : children.values()) link(parent, t);
		}
		
		@Override
		public void unlink(MutableThing parent, MutableThing child) {
			if (child == null) return;
			if (child.identity().isIdentifiable()) {
				if (parent.root() != null) references.unlink(parent.root(), child);
			} else {
				unlink(parent, child.references());
			}
		}
		
		@Override
		public void unlink(MutableThing parent, Collection<? extends MutableThing> children) {
			if (children == null || children.isEmpty()) return;
			for (MutableThing t : children) unlink(parent, t);
		}
		
		@Override
		public void unlink(MutableThing parent, Map<String, ? extends MutableThing> children) {
			if (children == null || children.isEmpty()) return;
			for (MutableThing t : children.values()) unlink(parent, t);
		}
		
	}
	
	@Override
	public synchronized <T extends Thing> void imprintManyWhere(String type, Class<T> clazz, Condition<T> condition, Edit<T> edit) {
		transaction(() -> {
			Collection<Thing> found = typeIndex.get(type);
			if (found != null) {
				Set<Thing> imprints = new HashSet<>();
				for (Thing t : found) {
					if (condition.match((T) t)) {
						T e = edit.edit((T) t);
						if (!t.equals(Thing.Equality.STATE, e)) {
							imprints.add(e);
						}
					}
				}
				if (!imprints.isEmpty()) {
					imprintInternal(imprints);
				}
			}
		});
	}
	
	@Override
	public synchronized void startDiff() {
		transaction(() -> {
			openDiff = new Diff();
		});
	}
	
	@Override
	public synchronized Diff endDiff() {
		return transaction(() -> {
			Diff r = openDiff != null ? openDiff : new Diff();
			openDiff = null;
			return r;
		});
	}
	
	@Override
	public synchronized Thing where(String thingType, String field, String value) {
		return transaction(() -> {
			Set<Thing> ofType = (Set<Thing>) typeIndex.get(thingType);
			if (ofType != null) {
				for (Thing t : ofType) {
					Object v = t.toMap(Include.DANGEROUS).get(field);
					if (StringUtils.equals(v != null ? v.toString() : null, value)) {
						return t;
					}
				}
			}
			return null;
		});
	}
	
	@Override
	public synchronized boolean[] contains(Thing... things) {
		return transaction(() -> containsInternal(things));
	}
	
	/** Internal version of {@link #contains(Thing...)}, only meant for use within an existing transaction. */
	private boolean[] containsInternal(Thing... things) {
		boolean[] results = new boolean[things.length];
		for (int i = 0, len = things.length; i < len; i++) {
			results[i] = getInternal(things[i]) != null;
		}
		return results;
	}
	
	@Override
	public synchronized boolean[] contains(String... idkeys) {
		return transaction(() -> containsInternal(idkeys));
	}
	
	/** Internal version of {@link #contains(String...)}, only meant for use within an existing transaction. */
	private boolean[] containsInternal(String... idkeys) {
		boolean[] results = new boolean[idkeys.length];
		for (int i = 0, len = idkeys.length; i < len; i++) {
			results[i] = getInternal(idkeys[i]) != null;
		}
		return results;
	}
	
	@Override
	public synchronized void clear() {
		invalid.clear();
		actions.clear();
		things.clear();
		typeIndex.clear();
		idkeys.clear();
		references.clear();
		changedRefs.clear();
		changed.clear();
		holdersToIdkeys.clear();
		holdersToMatches.clear();
		if (persistence != null) persistence.clear(null, null);
	}

	@Override
	public synchronized int count(String type) {
		return transaction(() -> type == null ? things.size() : typeIndex.get(type).size());
	}

	@Override
	public synchronized void release() {
		if (persistence != null) persistence.release();
	}
	
	@Override
	public synchronized void forgetSession() {
		transaction(this::forgetSessionInternal);
	}
	
	/** Internal implementation of {@link #forgetSession()}, intended only for use within an existing transaction. */
	private void forgetSessionInternal() {
		List<Holder> sessions = new ArrayList<>();
		for (Holder holder : holdersToIdkeys.keySet()) {
			if (holder.hold() == Holder.Hold.SESSION) sessions.add(holder);
		}
		for (Holder holder : sessions) { // Loops on a different collection to avoid concurrent mods.
			forgetInternal(holder);
		}
	}
	
	@Override
	public synchronized void addInvalid(Thing thing) {
		transaction(() -> addInvalidInternal(thing));
	}
	
	/** Internal version of {@link #addInvalid(Thing)}, meant to only be used within an existing transaction. */
	private void addInvalidInternal(Thing thing) {
		if (getInternal(thing) == null) return;
		Thing id = thing.identity();
		invalid.add(id.idkey());
		if (openDiff != null) openDiff = openDiff.setInvalidated(id, true);
		transactionDiff.add(id.idkey());
	}
	
	@Override
	public synchronized Set<Thing> getInvalid() {
		return transaction(() -> {
			Set<Thing> v = new HashSet<>(invalid.size());
			for (String idkey : invalid) {
				Thing t = getInternal(idkey);
				if (t != null) v.add(t);
			}
			return v;
		});
	}
	
	@Override
	public synchronized void clearInvalid(Thing... things) {
		transaction(() -> {
			for (Thing t : things) {
				invalid.remove(t.idkey());
				if (openDiff != null) openDiff = openDiff.setInvalidated(t, false);
				transactionDiff.remove(t.idkey());
			}
		});
	}
	
	@Override
	public synchronized void addAction(Action action, RemotePriority priority) {
		transaction(() -> {
			actions.put(action, priority);
			transactionDiff.add(action, priority);
		});
	}
	
	@Override
	public synchronized Map<Action, RemotePriority> getActions() {
		return transaction(() -> new HashMap<>(actions));
	}
	
	@Override
	public synchronized void clearActions(Action[] actions) {
		transaction(() -> {
			Collection<Action> asCollection = Arrays.asList(actions);
			this.actions.keySet().removeAll(asCollection);
			transactionDiff.removeActions(asCollection);
		});
	}
	
	
	public static class Selector implements Space.Selector {
		
		private final MutableSpace space;
		
		private Selector(MutableSpace space) {
			this.space = space;
		}
		
		@Override
		public <T extends Thing> T get(T thing) {
			return space.getInternal(thing);
		}
		
		public Collection<Thing> get(Collection<Thing> collection) {
			List<Thing> all = new ArrayList<>(collection.size());
			for (Thing t : collection) {
				Thing found = space.getInternal(t);
				if (found != null) all.add(found);
			}
			return all;
		}
		
		public <T extends Thing> List<T> getOfType(String type, Class<T> cast) {
			List<T> c = new ArrayList<>();
			Collection<Thing> ofType = space.typeIndex.get(type);
			if (ofType != null) {
				for (Thing t : ofType) {
					c.add((T)t);
				}
			}
			return c;
		}
		
		@Override
		public void addInvalid(Thing thing) {
			space.addInvalidInternal(thing);
		}
		
		@Override
		public boolean[] contains(Thing... things) {
			return space.containsInternal(things);
		}
		
		@Override
		public boolean[] contains(String... idkeys) {
			return space.containsInternal(idkeys);
		}
	}
	
	
	
	
	
	private static class References<T> {
		/** Values are the objects that the key references. */
		private final SetMultimap<T, T> references = MultimapBuilder.hashKeys().hashSetValues().build();
		/** Values are the objects that reference the key. */
		private final SetMultimap<T, T> referencedBy = MultimapBuilder.hashKeys().hashSetValues().build();
		/** A set of objects that were unlinked since the last call to {@link #dirty} */
		private final Set<T> dirty = new HashSet<>();
		
		/**
		 * Mark that this `object` references another object `reference`.
		 */
		public void link(T object, T reference) {
			this.references.put(object, reference);
			this.referencedBy.put(reference, object);
		}
		
		/** @return The objects that reference this object. Empty if no references. */
		public Set<T> referencesThis(T object) {
			return referencedBy.get(object);
		}
		
		/** @return The objects that this object references. Empty if no references. */
		public Set<T> thisReferences(T node) {
			return references.get(node);
		}
		
		/**
		 * Removes all references that this object has on others
		 * @return The nodes it referenced previously
		 */
		public Set<T> removeReferencesFrom(T node) {
			Set<T> removed = references.removeAll(node);
			for (T n : removed) {
				unlink(node, n);
			}
			return removed;
		}
		
		/**
		 * Mark that this `object` no longer references another object `reference`.
		 * Opposite of {@link #link(T, T)}
		 */
		public void unlink(T object, T reference) {
			this.references.remove(object, reference);
			this.referencedBy.remove(reference, object);
			dirty.add(reference);
		}
		
		public void clear() {
			references.clear();
			referencedBy.clear();
			dirty.clear();
		}
		
		public Set<T> dirty() {
			HashSet<T> v = new HashSet<>(dirty);
			dirty.clear();
			return v;
		}
		
	}
	
	public void await(long time, TimeUnit unit) throws InterruptedException {
		if (persistence == null) return;
		KeyLatch latch;
		synchronized (persistenceLock) {
			latch = persistenceLatch;
		}
		if (latch != null) latch.await(time, unit);
	}
	
	private static class TransactionDiff {
		final Set<Thing> addThings = new HashSet<>();
		final Set<Thing> removeThings = new HashSet<>();
		final Set<Pair<Holder, Object>> addHolders = new HashSet<>();
		final Set<Pair<Holder, Object>> removeHolders = new HashSet<>();
		final Map<Action, RemotePriority> addActions = new HashMap<>();
		final Set<Action> removeActions = new HashSet<>();
		final Set<String> addInvalids = new HashSet<>();
		final Set<String> removeInvalids = new HashSet<>();
		
		public void add(Collection<Thing> things) {
			addThings.addAll(things);
			removeThings.removeAll(things);
		}
		
		public void remove(Collection<Thing> things) {
			removeThings.addAll(things);
			addThings.removeAll(things);
		}
		
		public void add(Holder holder, Object held) {
			Pair<Holder, Object> pair = Pair.of(holder, held);
			addHolders.add(pair);
			removeHolders.remove(pair);
			
		}
		
		public void remove(Holder holder, Object held) {
			Pair<Holder, Object> pair = Pair.of(holder, held);
			removeHolders.add(pair);
			addHolders.remove(pair);
		}
		
		public void add(Action action, RemotePriority priority) {
			removeActions.remove(action);
			addActions.put(action, priority);
		}
		
		public void removeActions(Collection<Action> actions) {
			removeActions.addAll(actions);
			for (Action action : actions) {
				addActions.remove(action);
			}
		}
		
		public void add(String invalidIdkey) {
			removeInvalids.remove(invalidIdkey);
			addInvalids.add(invalidIdkey);
		}
		
		public void remove(String invalidIdkey) {
			addInvalids.remove(invalidIdkey);
			removeInvalids.add(invalidIdkey);
		}
		
		boolean hasChanges() {
			return !addThings.isEmpty() || !removeThings.isEmpty()
					|| !addHolders.isEmpty() || !removeHolders.isEmpty()
					|| !addActions.isEmpty() || !removeActions.isEmpty()
					|| !addInvalids.isEmpty() || !removeInvalids.isEmpty();
		}
		
		void reset() {
			addThings.clear();
			removeThings.clear();
			addHolders.clear();
			removeHolders.clear();
			addActions.clear();
			removeActions.clear();
			addInvalids.clear();
			removeInvalids.clear();
		}
	}
	
	
	/**
	 * Returns the number things of this type.
	 * WIP This is only supplied for the LegacyMigration and memory logging during the sync beta, if we want this as an official api, set it up in {@link Space} instead
	 * Otherwise remove it once we have figured out the item limitations.
	 * @param type The type, or null to count all things regardless of type
	 * @return
	 */
	public synchronized int countOf(String type) {
		return transaction(() -> {
			if (type == null) return things.size();
			return typeIndex.get(type).size();
		});
	}
	
}
