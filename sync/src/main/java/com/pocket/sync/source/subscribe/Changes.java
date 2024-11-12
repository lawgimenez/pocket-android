package com.pocket.sync.source.subscribe;

import com.pocket.sync.thing.Thing;
import com.pocket.util.java.Safe;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Describes a type of change that can occur to one or many {@link Thing}s.
 * Use one of the static methods to create an instance and then if further refinement is needed, one of the methods on the instance.
 * Instances are immutable.
 */
public class Changes<T extends Thing> {
	
	/**
	 * All changes that occur on this specific {@link Thing}.
	 */
	public static <T extends Thing> Changes<T> of(T thing) {
		return new Changes<>(thing, (Class<T>) thing.getClass(), null, null);
	}
	
	/**
	 * All changes that occur to any things of this type.
	 */
	public static <T extends Thing> Changes<T> of(Class<T> type) {
		return new Changes<>(null, type, null, null);
	}
	
	/**
	 * Any changes that occur to any things of this type and match some criteria.
	 */
	public static <T extends Thing> Changes<T> of(Class<T> type, ThingMatch<T> matching) {
		return new Changes<>(null, type, matching, null);
	}
	
	/** If related to a specific {@link Thing}, its identity or null. */
	public final T identity;
	/** The {@link Thing}'s class. Never null. */
	public final Class<T> type;
	/** If not null, only match things that match this. */
	public final ThingMatch<T> match;
	/** If not null, only match changes that match this. */
	public final ChangeMatch<T> change;
	
	private Changes(T identity, Class<T> type, ThingMatch<T> match, ChangeMatch<T> change) {
		if (type == null) throw new IllegalArgumentException("must specify type");
		this.identity = identity;
		this.type = type;
		this.match = match;
		this.change = change;
	}
	
	public interface ChangeMatch<T extends Thing> {
		/**
		 * Decide if this change matches.
		 * @param before The thing before the change, may be null
		 * @param after The thing after the change, never null
		 * @return true if this change matches what you are looking for
		 */
		boolean matches(T before, T after);
	}
	
	public interface ThingMatch<T extends Thing> {
		/**
		 * @param t The latest state of this thing
		 * @return true to include this thing
		 */
		boolean matches(T t);
	}
	
	public interface Value<T extends Thing, V> {
		/** Return the field/value from the thing to be compared between before and after. */
		V value(T of);
	}
	
	/**
	 * Returns a new immutable instance with the same settings as before but also sets some criteria that a specific value must have changed.
	 * When comparing values it will use {@link Safe#get(Safe.Get)} to obtain the field values, which sees the value as null if accessing it throws errors like NPEs or array out of bounds.
	 * This is for convenience so you don't have to use null checks in your accessing of the value.
	 * If the value changed between the two states (before and after) it will consider it a match.
	 * @param value Describes how to access this value on an instance of the Thing.
	 */
	public <V> Changes<T> value(Value<T, V> value) {
		return new Changes<>(identity, type, match, (before, after) -> {
			V p = Safe.get(() -> value.value(before));
			V l = Safe.get(() -> value.value(after));
			return ObjectUtils.notEqual(p, l);
		});
	}
	
	/**
	 * @return A new immutable instance with the same settings as before but also sets some criteria based on what specific state changed.
	 */
	public Changes<T> when(ChangeMatch<T> change) {
		return new Changes<>(identity, type, match, change);
	}
	
	@NotNull
	@Override
	public String toString() {
		return (identity != null ? identity.toString() : type.getName())
			+ (match != null ? "(with match)" : "")
			+ (change != null ? "(with change)" : "");
	}
}
