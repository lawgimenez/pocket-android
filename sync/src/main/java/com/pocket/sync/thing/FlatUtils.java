package com.pocket.sync.thing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tools for finding sub identifiable Things within parents.
 */
public class FlatUtils {
	
	/**
	 * Finds all identifiable things including the provided `values` themselves and any they contain has children, at any depth.
	 * <p>
	 * This adds all top level things, the ones directly provided in `values` first, then in the order of `values` traverses each one looking for child things, adding that are found.
	 * If a duplicate thing is found, it will keep the first one it encountered. So that means it will prioritize the thing entries found directly in `values` and then in order found.
	 * For example, if in the provided values you include thing A and also another thing B that contains/references thing A,
	 * it will keep the version of A that was directly in the provided `values` (not the one within B).
	 *
	 * @return all identifiable things, including the provided values themselves and any they contain has children, at any depth.
	 */
	public static Set<Thing> flatten(Collection<? extends Thing> values) {
		Deep out = new Deep();
		// Traverse the provided values first so they are prioritized
		for (Thing thing : values) {
			if (thing.isIdentifiable()) {
				out.things.add(thing);
			}
		}
		// Then the things they contain
		for (Thing thing : values) {
			thing.subthings(out);
		}
		return out.things;
	}
	
	/** Same as {@link #flatten(Collection)} with one thing. */
	public static Set<Thing> flatten(Thing thing) {
		return flatten(Collections.singletonList(thing));
	}
	
	/**
	 * @return Any identifiable things contained directly within the provided thing. Things within those are not collected.
	 */
	public static Set<Thing> references(Thing thing) {
		Shallow out = new Shallow();
		thing.subthings(out);
		return out.things;
	}
	
	/**
	 * Base search with common methods implemented.
	 */
	private abstract static class Base implements Output {
		
		public final Set<Thing> things = new HashSet<>();
		
		@Override
		public void addAll(Collection<? extends Thing> children, boolean hasChildren) {
			for (Thing c : children) {
				if (c != null) {
					add(c, hasChildren);
				}
			}
		}
		
		@Override
		public void addAll(Map<String, ? extends Thing> children, boolean hasChildren) {
			addAll(children.values(), hasChildren);
		}
	}
	
	/**
	 * Searches within sub identifiable things.
	 */
	private static class Deep extends Base {
		@Override
		public void add(Thing child, boolean hasChildren) {
			if (things.add(child)) {
				if (hasChildren) {
					child.subthings(this);
				}
			}
		}
	}
	
	/**
	 * Only extracts direct child identifiable things, doesn't search within those.
	 */
	private static class Shallow extends Base {
		@Override
		public void add(Thing child, boolean hasChildren) {
			things.add(child);
		}
	}
	
	public interface Output {
		/**
		 * Add this thing to the output.
		 * @param hasChildren Whether this thing contains identifiable things. It is up to the implementation whether to search within this thing, but this is a hint to whether or not it is needed.
		 */
		void add(Thing child, boolean hasChildren);
		
		/** Invokes {@link #add(Thing, boolean)} on all things in this collection. */
		void addAll(Collection<? extends Thing> children, boolean hasChildren);
		/** Invokes {@link #add(Thing, boolean)} on all things in this collection. */
		void addAll(Map<String, ? extends Thing> children, boolean hasChildren);
		
		/** Invokes {@link Thing#subthings(Output)} on all things in this collection. */
		default void searchAll(Collection<? extends Thing> children) {
			for (Thing c : children) {
				if (c != null) {
					c.subthings(this);
				}
			}
		}
		
		/** Invokes {@link Thing#subthings(Output)} on all things in this collection. */
		default void searchAll(Map<String, ? extends Thing> children) {
			for (Thing c : children.values()) {
				if (c != null) {
					c.subthings(this);
				}
			}
		}
	}
}
