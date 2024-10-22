package com.pocket.sync.space.persist;

import com.google.common.collect.Multimap;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Holder;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Map;

/**
 * Stores the data that a {@link com.pocket.sync.space.Space} needs to operate.
 * Provides no fancy logic, just read/write of data.
 * Some methods are async, some are blocking. See docs for more details.
 */
public interface DumbStorage {
	
	/**
	 * Reads in all data, invoking the various callbacks as data becomes available.
	 * This is a blocking operation until completed or an error is thrown.
	 * However note that callbacks could be called back on any thread if the implementation uses multiple threads to restore data.
	 */
	void restore(Spec spec, ThingCallback things, HolderCallback holders, ActionCallback actions, InvalidCallback invalids);
	
	/**
	 * Asynchronously write this data as part of a single transaction.
	 *
	 * @param addThings Things to add or update if already present. If null or empty, nothing to change.
	 * @param removeThings Things to remove if already present. If null or empty, nothing to change.
	 * @param addHolders Holders to add. If null or empty, nothing to change.
	 * @param removeHolders Holders to remove. If null or empty, nothing to change.
	 * @param addActions Actions to add. If null or empty, nothing to change.
	 * @param removeActions Actions to remove. If null or empty, nothing to change.
	 * @param addInvalids Invalids to add. If null or empty, nothing to change.
	 * @param removeInvalids Invalids to add. If null or empty, nothing to change.
	 * @param onSuccess Callback to invoke when successfully completed all writes. Can be made on on any thread.
	 * @param onFailure Callback to invoke if an error occurred and these changes were not applied. Can be made on on any thread.
	 */
	void store(
			Collection<Thing> addThings, Collection<Thing> removeThings,
			Collection<Pair<Holder, Object>> addHolders, Collection<Pair<Holder, Object>> removeHolders,
			Map<Action, RemotePriority> addActions, Collection<Action> removeActions,
			Collection<String> addInvalids, Collection<String> removeInvalids,
			WriteSuccess onSuccess, WriteFailure onFailure);
	
	
	/**
	 * Asynchronously clear all data.
	 *
	 * @param onSuccess Callback to invoke when successfully completed. Can be made on on any thread.
	 * @param onFailure Callback to invoke if an error occurred and these changes were not applied. Can be made on on any thread.
	 */
	void clear(WriteSuccess onSuccess, WriteFailure onFailure);
	
	/**
	 * See {@link Space#release()}
	 */
	void release();
	
	interface ThingCallback {
		void restored(Thing value);
	}
	interface HolderCallback {
		void restored(Multimap<Holder, Object> holders);
	}
	interface ActionCallback {
		void restored(Map<Action, RemotePriority> actions);
	}
	interface InvalidCallback {
		void restored(Collection<String> idkeys);
	}
	
	interface WriteSuccess {
		void onSuccess();
	}
	interface WriteFailure {
		void onFailure(Throwable error);
	}
	
}
