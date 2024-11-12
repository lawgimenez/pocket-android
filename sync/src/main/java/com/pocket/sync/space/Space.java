package com.pocket.sync.space;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Remembers {@link Thing}s.
 * <p>
 * Anytime you see something, pass it to {@link #imprint(Thing)} and if it has
 * been set to {@link #remember(Holder, Thing...)} will be available from {@link #get(Thing)}
 * until it {@link #forget(Holder, Thing...)}s.
 */
public interface Space {
	
	/**
	 * Set the {@link Spec} that the space will use to work with and create things and actions.
	 */
	Space setSpec(Spec spec);
	
	/**
	 * Keep these types of {@link Thing}s in this space until {@link #forget(Holder, Thing...)}
	 * on behalf of this holder. It doesn't matter if the {@link Thing} is in this space yet or not.
	 * If it is not, then anytime in the future that it is imprinted, it will be held.
	 * <p>
	 * <b>This is not a setter</b> only the {@link Thing#identity()} info is used
	 * to track which types of {@link Thing}s to remember, this does not modify
	 * what is remembered about them. Use {@link #imprint(Thing)} to set a new value into this space.
	 * <p>
	 * Many holders can hold the same Thing, they have no impact on each other's holds.
	 *
	 * @param holder A unique name indicating who/reason for these to be remembered. To be referenced in {@link #forget(Holder, Thing...)} later.
	 * @param identities One or many types of things to remember.
	 * @see #initialize(Thing)
	 */
	void remember(Holder holder, Thing... identities);
	
	/**
	 * If this thing does not exist in this space, {@link #imprint(Thing)} the provided value.
	 * If it does already, this has no effect.
	 */
	void initialize(Thing thing);
	
	/**
	 * Remove this {@link Holder}'s hold on these {@link Thing}s.
	 * <p>
	 * There may be other {@link Holder}s holding these Things so this does not guarantee it will
	 * actually be removed, this just specifies that this {@link Holder} no longer needs these.
	 * <p>
	 * If after releasing this hold, if there are no other holds on these {@link Thing}s, they will be removed from memory during this call.
	 *
	 * @param holder A unique name that was used in {@link #remember(Holder, Thing...)}
	 * @param identities The specific {@link Thing}s to release holds on, or if empty, all holds by this {@link Holder}.
	 */
	void forget(Holder holder, Thing... identities);
	
	/**
	 * Retrieves the space's known version of this {@link Thing} or null if not known.
	 * @param thing
	 * @param <T>
	 * @return
	 */
	<T extends Thing> T get(T thing);
	
	/**
	 * Same as {@link #get(Thing)} but looks by matching its {@link Thing#idkey()}.
	 */
	Thing get(String idkey);
	
	/**
	 * Attempt to derive this thing from other things in space.
	 * Uses {@link Spec.Derive#derive(Thing, Selector)} from the {@link Spec} provided in {@link #setSpec(Spec)}.
	 *
	 * @param thing The thing to derive (only its identity is considered)
	 * @return The derived thing or null if not able to derive
	 */
	<T extends Thing> T derive(T thing);
	
	/**
	 * Retrieves {@link Thing}s of a certain type.
	 * @param type The {@link Thing#type()}
	 * @param clazz The class type to cast the returned things to.
	 */
	<T extends Thing> Collection<T> getAll(String type, Class<T> clazz);
	
	/**
	 * Change or record this and any of its nested {@link Thing}s in space.
	 * Only things that have been previously remembered ({@link #remember(Holder, Thing...)})
	 * will be kept. If a {@link Thing} is already known, it will maintain
	 * any state not explicitly declared in the provided value.
	 * If {@link Thing#isIdentifiable()} is false, it will only imprint identifiable things found within it.
	 *
	 * @param thing
	 * @return
	 */
	void imprint(Thing thing);
	
	/**
	 * {@link #imprint(Thing)} many at once.
	 */
	void imprint(Collection<? extends Thing> thing);
	
	<T extends Thing> void imprintManyWhere(String type, Class<T> clazz, Condition<T> condition, Edit<T> edit);
	
	/**
	 * Begin recording all changes made from imprints made after this call.
	 * Implementations will be basically equal to:
	 * Diff diff = new Diff()
	 * and then for each imprint:
	 * diff.addAll( [the result of the imprint] )
	 */
	void startDiff();
	
	/**
	 * Returns the diff since {@link #startDiff()} and stops recording.
	 * Returns empty diff if no recording was started.
	 */
	Diff endDiff();

	/**
	 * Returns the number of things in this space.
	 * @param type The type to count, or null to count all things
	 */
    int count(String type);

    interface Condition<T extends Thing> {
		boolean match(T thing);
	}
	interface Edit<T extends Thing> {
		/** @return a new instance with the new state, or the same instance or null to make no change. */
		T edit(T thing);
	}
	
	/**
	 * TODO docs
	 */
	Thing where(String thingType, String field, String value);
	
	/**
	 * Check if these things currently exist in the space.
	 * @return An array of true or false for each thing by the same index they were passed into the method.
	 */
	boolean[] contains(Thing... things);
	
	/**
	 * Check if these things currently exist in the space, but their {@link Thing#idkey()}
	 * @return An array of true or false for each thing by the same index they were passed into the method.
	 */
	boolean[] contains(String... idkeys);
	
	/**
	 * Erase all data, returning back to default state.
	 */
	void clear();
	
	/**
	 * Releases all system resources, threads, etc. that it might be using.
	 * Don't delete stored data, this just means the runtime is done using this for now
	 * and it will create a new instance if it needs to work with this database in the future.
	 * Accessing this space instance after invoking this is an error.
	 * Block until stopped
	 */
	void release();
	
	/**
	 * Forgets all {@link Holder}s that are {@link Holder.Hold#SESSION}.
	 * Keeps others.
	 */
	void forgetSession();
	
	/**
	 * Mark that this thing may be partially out of date and needs to be synced with a remote.
	 * If this thing doesn't exist in this space, or if the thing is later forgotten, this entry will also be removed.
	 * If there is an active diff, via {@link #startDiff()}, these will be added to the diff's {@link Diff#invalidated}.
	 * @param thing
	 */
	void addInvalid(Thing thing);
	
	/**
	 * @return All {@link Thing}s that have been marked as invalid via {@link #addInvalid(Thing)} that exist in this space. Returned as a new Set, which you may use and modify as you wish, it will have no changes on this space.
	 */
	Set<Thing> getInvalid();
	
	/**
	 * @param things Thing to remove from the invalid list. If they don't exist in this space or aren't invalid, this has no effect.
	 *               If there is an active diff, via {@link #startDiff()}, these will be removed from the diff's {@link Diff#invalidated}.
	 */
	void clearInvalid(Thing... things);
	
	/**
	 * Persist this action so it is returned by {@link #getActions()}.
	 * If this exact same action already exists in memory, it will update its priority to this new provided value.
	 */
	void addAction(Action action, RemotePriority priority);
	Map<Action, RemotePriority> getActions();
	void clearActions(Action[] actions);
	
	
	/** An indication that this space implementation is lost when it is killed, does not persist across processes or reboots. */
	interface Volatile {}
	/** An indication that this space implementation is persisted across reboots. */
	interface Persisted {}
	
	/**
	 * A reduced {@link Space} API that is safe to use within a "transaction" or as part of a larger method implementation.
	 * Read only with the exception of being able to flag things as invalid with {@link #addInvalid(Thing)}.
	 * Implementations can choose to add other methods of querying data.
	 */
	interface Selector {
		/** Same as {@link Space#get(Thing)} */
		<T extends Thing> T get(T thing);
		
		/** Same as {@link Space#contains(Thing...)} */
		boolean[] contains(Thing... things);
		
		/** Same as {@link Space#contains(String...)} */
		boolean[] contains(String... idkeys);
		
		/** Same as {@link Space#addInvalid(Thing)} REVIEW it is a little weird to have a modification API in a class called Selector, so perhaps there is something to rename here. Selector is more meant to be a limited set of APIs that are safe to expose in derive() rather than exposing the entire Space API. */
		void addInvalid(Thing thing);
	}
	
}