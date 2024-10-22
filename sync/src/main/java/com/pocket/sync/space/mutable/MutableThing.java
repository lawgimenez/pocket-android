package com.pocket.sync.space.mutable;

import com.pocket.sync.thing.FlatUtils;
import com.pocket.sync.thing.Thing;

import java.util.Collection;

/**
 * A variant of a {@link Thing} that is mutable, for use in {@link MutableSpace}.
 * <p>
 * It holds all of its state in an internal {@link com.pocket.sync.thing.ThingBuilder}
 * and anywhere it references another identifiable thing, it stores that state as a {@link MutableThing}.
 * So if a change is made to a referenced thing, it inherently already has the change.
 * <p>
 * Fields that are non identifiable things are stored in the builder, unless the type of thing can contain
 * other identifiable things within it. In that case it is also stored as a {@link MutableThing}.
 * This includes open types that could be a mixture of identifiable and non-identifiable things.
 * Basically, if there is any chance a field could hold an identifiable thing (itself as its children) at runtime, it must be represented as a {@link MutableThing}.
 * <p>
 * This will be code generated.
 *
 * Note: Instances should have their equals/hashCode proxy to calling equals/hashCode on the value returned by {@link #identity()}.
 */
public interface MutableThing<T extends Thing> {
	
	/** @return The {@link Thing#identity()} of what this represents. */
	T identity();
	
	/**
	 * @return An immutable instance of its current state. This should be cached once built and only rebuilt when needed because of a change.
	 *
	 * 			However, If this thing is not identifiable and potentially contains identifiable things, this should NOT cache its results,
	 * 			and instead, should rebuild on each call of this method.
	 *
	 * 			Why? Consider a case where we have an identifiable thing (A) that has an non-identifiable thing (B), which has an identifiable thing (C).
	 * 			In the current implementation of {@link Mutables#link(MutableThing, MutableThing)}, it will only record a link between identifiable things (A -> C)
	 * 			If C later changes, {@link MutableSpace} will look for who is referencing C and it will find A and call {@link #invalidate()}.
	 * 			However, B will not be invalidated, and if this is cached, it will still have the old C rather than the new instance.
	 *
	 * 			Perhaps we could optimize for this with some kind of change to linking to also capture non-identifiable to identifiable links,
	 * 			but that has its own complexities (1. don't want it to influence holds, 2. non-identifiable things don't have their own identity, etc).
	 *
	 * 			This will only be rebuilt when its parent is, so this is more in line with non-identifiable things being apart of its {@link #root}.
	 */
	T build();
	
	/** If you have cached a built instance, move it to be returned on the next {@link #previous()} call and clear the stored build version so it is freshly made next {@link #build()} */
	void invalidate();
	
	/** @return the previous state, see {@link #invalidate()}. After this is invoked, set to null so its reference is released. This can help free up resources that might be held by holding a copy of its old version. */
	T previous();
	
	/**
	 * Do a {@link com.pocket.sync.space.Space#imprint(Thing)}, imprinting the provided state onto the existing state.
	 * Invoke {@link Mutables#flagChanged(MutableThing)} if a change is detected.
	 * @param value The value to imprint
	 * @param mutables A helper for the imprint
	 */
	void imprint(T value, Mutables mutables);
	
	/** Return a collection of other things this thing references. This should only include {@link MutableThing}s that are identifiable. Similar to what would be returned to {@link Thing#subthings(FlatUtils.Output)} */
	Collection<? extends MutableThing> references();
	
	/**
	 * If this {@link Thing#isIdentifiable()} this should return itself.
	 * Otherwise it should be the identifiable thing that contains this value.
	 * May be null if the thing it detached for some reason.
	 */
	MutableThing root();

	/**
	 * @return Same as {@link Thing#isIdentifiable()} for the underlying instance.
	 */
	default boolean isIdentifiable() {
		return identity().isIdentifiable();
	}

}
