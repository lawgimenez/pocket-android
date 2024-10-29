package com.pocket.sync.spec;

import com.pocket.sync.space.Space;
import com.pocket.sync.thing.FlatUtils;
import com.pocket.sync.thing.Thing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the resolving part of a sync, during the imprint phase.
 * See "Resolving Ids" in the main Sync docs for more information.
 * <p>
 * Used by a client side {@link com.pocket.sync.source.Source} to take a {@link Thing} returned by a remote {@link com.pocket.sync.source.Source}
 * to ensure it has identity, in cases where the remote might not return the same identity format that the client expects, such as when it returns
 * a server based id only.  This helps the client source look up the thing by different criteria than normal and resolve its identity to one the client
 * will understand.
 */
public interface Resolver {
	
	/**
	 * Looks at the provided {@link Thing} and all things within it and for any that are missing identity
	 * and for each of those invokes {@link #resolve(Thing, Space)} to attempt to fix its missing identity.
	 * Returns either the same instance or a new instance with the missing identities added where possible.
	 * @param t The thing (and its subthings) to resolve
	 * @param space Where to resolve it from
	 * @param resolver The logic to resolve each individual thing
	 * @return The same instance if no changes were needed, or a modified instance if a change was made
	 */
	static <T extends Thing> T resolveAll(T t, Space space, Resolver resolver) {
		// Use a list, because sets drop multiple things of the same type with missing identity.
		class ShallowList implements FlatUtils.Output {
			private List<Thing> list = new ArrayList<>();
			
			@Override public void add(Thing child, boolean hasChildren) {
				list.add(child);
			}
			@Override
			public void addAll(Collection<? extends Thing> children, boolean hasChildren) {
				for (Thing value : children) {
					if (value != null) list.add(value);
				}
			}
			@Override
			public void addAll(Map<String, ? extends Thing> children, boolean hasChildren) {
				for (Thing value : children.values()) {
					if (value != null) list.add(value);
				}
			}
		}
		
		t = resolver.resolve(t, space);
		final ShallowList out = new ShallowList();
		t.subthings(out);
		for (Thing sub : out.list) {
			// Theoretically it's possible to get into infinite recursion here if the thing 
			// references itself. But we currently don't have a use case like this. And since our 
			// server uses JSON, this would also be impossible to represent in a response
			// (JSON representing it would have to be infinite too).
			// So we'll ignore it for now.
			Thing r = resolveAll(sub, space, resolver);
			if (r != sub) {
				T c = (T) t.with(thing -> thing == sub, r); // It is just comparing that the class instance is equal, rather than identity which might be missing
				t = c != null ? c : t;
			}
		}
		return t;
	}
	
	/**
	 * Check if missing identity and if so attempt to find it in {@link Space} by some other criteria such as server based id.
	 * Return a modified instance that adds the missing identity (identity should be the addition. all other state should be left as is).
	 * If it doesn't know how or can't find it, return the provided instance.
	 * @return A modified or the same instance
	 */
	<T extends Thing> T resolve(T t, Space space);
	
	/**
	 * Reduce the provided {@link Thing} to only declared fields that contain identifiable information, whether part of its
	 * {@link Thing#identity()} or some additional server based ids.  Should strip out all state and fields that are effected
	 * by actions or possible changes. Any values remaining should be ok to imprint out of sync order without breaking consistency.
	 * @return A new instance that's been reduced to only having ids and identity declared.
	 */
	<T extends Thing> T reduce(T t);
	
	/**
	 * Doesn't look anything up, just does the bare minimum implementation.
	 */
	Resolver BASIC = new Resolver() {
		
		@Override
		public <T extends Thing> T resolve(T t, Space space) {
			return t;
		}
		
		@Override
		public <T extends Thing> T reduce(T t) {
			return (T) t.identity();
		}
	};
	
}
