package com.pocket.sync.space;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.thing.Thing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes changes made to {@link Thing}s.
 * Immutable instances.
 * Can use constructors or a {@link Builder} to help create an instance.
 * Use public fields or helper methods to query for what changed.
 */
public class Diff {
	
	public static class Builder {
		
		private final Map<Thing, Change<?>> changes = new HashMap<>();
		
		public void added(Thing thing) {
			changes.put(thing.identity(), new Change<>(null, thing));
		}
		
		public void changed(Thing previous, Thing latest) {
			changes.put(latest.identity(), new Change<>(previous, latest));
		}
		
		public Diff build() {
			return new Diff(changes, new HashSet<>());
		}
		
	}
	
	public final Map<Thing, Change<?>> changes;
	public final ListMultimap<Class<? extends Thing>, Change<?>> byType;
	public final Set<Thing> invalidated;
	
	public Diff() {
		this(new HashMap<>(), new HashSet<>());
	}
	
	public Diff(Map<Thing, Change<?>> changes, Set<Thing> invalidated) {
		this.invalidated = Collections.unmodifiableSet(invalidated);
		this.changes = Collections.unmodifiableMap(changes);
		ListMultimap<Class<? extends Thing>, Change<?>> byType = MultimapBuilder.hashKeys().arrayListValues().build();
		for (Map.Entry<Thing, Change<?>> t : this.changes.entrySet()) {
			byType.put(t.getKey().getClass(), t.getValue());
		}
		this.byType = ImmutableListMultimap.copyOf(byType);
	}
	
	/**
	 * Creates a new instance with these additional changes included.
	 * Where there are existing changes, it will keep the existing {@link Change#previous} and the new {@link Change#latest}.
	 * @param diff The changes to add
	 * @return A new immutable instance with the additional changes included.
	 */
	public Diff add(Diff diff) {
		Map<Thing, Change<?>> updatedChanges = new HashMap<>(this.changes);
		for (Map.Entry<Thing, Change<?>> add : diff.changes.entrySet()) {
			Change<?> existing = updatedChanges.get(add.getKey());
			if (existing != null) {
				Change<?> merged = new Change<>(existing.previous, add.getValue().latest);
				updatedChanges.put(add.getKey(), merged);
			} else {
				updatedChanges.put(add.getKey(), add.getValue());
			}
		}
		
		Set<Thing> updatedInvalidated = new HashSet<>(this.invalidated);
		updatedInvalidated.addAll(diff.invalidated);
		
		return new Diff(updatedChanges, updatedInvalidated);
	}
	
	/**
	 * Creates a new instance with the change to the invalidation state of this thing.
	 * @return A new immutable instance with this change included.
	 */
	public Diff setInvalidated(Thing thing, boolean invalid) {
		Set<Thing> updatedInvalidated = new HashSet<>(this.invalidated);
		if (invalid) {
			updatedInvalidated.add(thing);
		} else {
			updatedInvalidated.remove(thing);
		}
		return new Diff(changes, updatedInvalidated);
	}
	
	public boolean isEmpty() {
		return changes.isEmpty();
	}
	
	/**
	 * Looks for changes that match the provided criteria
	 * @return A non null (but maybe empty) set of changes that matched
	 */
	public <T extends Thing> Set<Change<T>> find(Changes<T> changes) {
		Set<Change<T>> matches = new HashSet<>();
		if (changes.identity != null) {
			Change<T> c = (Change<T>) this.changes.get(changes.identity);
			if (c != null) {
				if (changes.match == null || changes.match.matches(c.latest)) {
					if (changes.change == null || changes.change.matches(c.previous, c.latest)) {
						matches.add(c);
					}
				}
			}
		} else {
			List<Change<?>> t = byType.get(changes.type);
			for (Change<?> c : t) {
				Change<T> cast = (Change<T>) c;
				if (changes.match == null || changes.match.matches(cast.latest)) {
					if (changes.change == null || changes.change.matches(cast.previous, cast.latest)) {
						matches.add((Change<T>) c);
					}
				}
			}
		}
		return matches;
	}
	
	/**
	 * @return If part of this diff, a change to this thing, or null.
	 */
	public <T extends Thing> Change<T> find(T t) {
		return (Change<T>) changes.get(t);
	}
	
	/**
	 * @return The {@link Change#latest} values for all things that match the provided criteria, or an empty set if none found.
	 */
	public <T extends Thing> Set<T> currentValues(Changes<T> of) {
		Set<T> matches = new HashSet<>();
		for (Change<T> c : find(of)) {
			matches.add(c.latest);
		}
		return matches;
	}
	
	/**
	 * @return The {@link Change#latest} of all things that changed
	 */
	public Collection<Thing> all() {
		List<Thing> all = new ArrayList<>(changes.size());
		for (Change c : changes.values()) {
			all.add(c.latest);
		}
		return all;
	}
	
}
