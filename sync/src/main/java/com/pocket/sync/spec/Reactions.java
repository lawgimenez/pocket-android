package com.pocket.sync.spec;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.pocket.sync.space.Diff;
import com.pocket.sync.thing.Thing;

import java.util.Collection;

/**
 * Helper for collecting reactions in {@link Thing#reactions(Thing, Thing, Diff, Reactions)}.
 * Tracks things and their derived fields that should be rederived because something they "listen" to changed.
 */
public class Reactions {
	
	/** Thing "type" mapped to a set of fields within that type that should be rederived. */
	private final SetMultimap<String, String> typesAndFields = MultimapBuilder.hashKeys().hashSetValues().build();
	/** Thing mapped to a set of fields within that type that should be rederived. */
	private final SetMultimap<Thing, String> thingsAndFields = MultimapBuilder.hashKeys().hashSetValues().build();
	
	/** Notify that this type of thing should rederive the specified field because something it reacts to changed. */
	public void type(String type, String field) {
		typesAndFields.put(type, field);
	}
	
	/** Notify that this thing should rederive the specified field because something it reacts to changed. */
	public void thing(Thing thing, String field) {
		thingsAndFields.put(thing, field);
	}
	
	/** Notify that these things should rederive the specified fields because something it reacts to changed. */
	public void things(Collection<Thing> things, Collection<String> fields) {
		if (things == null || things.isEmpty()) return;
		for (Thing t : things) {
			thingsAndFields.putAll(t, fields);
		}
	}
	
	public SetMultimap<String, String> typesAndFields() {
		return typesAndFields;
	}
	
	public SetMultimap<Thing, String> thingsAndFields() {
		return thingsAndFields;
	}
	
	
	
}
