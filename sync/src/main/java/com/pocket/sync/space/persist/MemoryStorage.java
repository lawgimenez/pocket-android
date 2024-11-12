package com.pocket.sync.space.persist;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Holder;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Just stores in memory. Not useful in normal cases since a Space will likely already do this, but might be useful for migration, testing or as a simple example implementation.
 */
public class MemoryStorage implements DumbStorage {
	
	private final Map<Thing, Thing> things = new HashMap<>(); // Using a Map instead of a Set, because it allows us to replace values. A Set would keep the older value.
	private final Set<Pair<Holder, Object>> holders = new HashSet<>();
	private final Map<Action, RemotePriority> actions = new HashMap<>();
	private final Set<String> invalids = new HashSet<>();
	
	@Override
	public void restore(Spec spec, ThingCallback things, HolderCallback holders, ActionCallback actions, InvalidCallback invalids) {
		for (Thing t : this.things.values()) {
			things.restored(t);
		}
		
		Multimap<Holder, Object> holdersMap = MultimapBuilder.hashKeys().arrayListValues().build();
		for (Pair<Holder, Object> h : this.holders) {
			holdersMap.put(h.getKey(), h.getValue());
		}
		holders.restored(holdersMap);
		
		actions.restored(new HashMap<>(this.actions));
		
		invalids.restored(new HashSet<>(this.invalids));
	}
	
	@Override
	public void store(Collection<Thing> addThings, Collection<Thing> removeThings, Collection<Pair<Holder, Object>> addHolders, Collection<Pair<Holder, Object>> removeHolders, Map<Action, RemotePriority> addActions, Collection<Action> removeActions, Collection<String> addInvalids, Collection<String> removeInvalids, WriteSuccess onSuccess, WriteFailure onFailure) {
		if (addThings != null) {
			for (Thing t : addThings) {
				things.put(t, t);
			}
		}
		if (removeThings != null) {
			for (Thing t : removeThings) {
				things.remove(t);
			}
		}
		
		if (addHolders != null) holders.addAll(addHolders);
		if (removeHolders != null) holders.removeAll(removeHolders);
		
		if (addActions != null) actions.putAll(addActions);
		if (removeActions != null) {
			for (Action a : removeActions) {
				actions.remove(a);
			}
		}
		
		if (addInvalids != null) invalids.addAll(addInvalids);
		if (removeInvalids != null) invalids.removeAll(removeInvalids);
	}
	
	@Override
	public void clear(WriteSuccess onSuccess, WriteFailure onFailure) {
		things.clear();
		holders.clear();
		actions.clear();
		invalids.clear();
	}
	
	@Override public void release() {}
}
