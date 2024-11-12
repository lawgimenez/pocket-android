package com.pocket.sync.source.result;

import com.pocket.sync.action.Action;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.BaseModeller;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of a sync. Sources can create an instance of {@link Builder} and then invoke its methods as it performs the sync.
 */
public class SyncResult<T extends Thing> {
	
	/** The T that was passed into the request. */
	public final T t;
	/** The Actions that were supplied. */
	public final Action[] a;
	
	public final Map<Action, Result> result_a;
	public final Result result_t;
	public final T returned_t;
	public final List<Thing> resolved;
	
	public SyncResult(Builder<T> builder) {
		this.t = builder.thing;
		this.a = ArrayUtils.clone(builder.actions);
		this.result_a = Collections.unmodifiableMap(builder.actionResults);
		this.result_t = builder.thingResult;
		this.returned_t = builder.thingReturned;
		this.resolved = builder.resolved != null ? BaseModeller.immutable(builder.resolved) : Collections.emptyList();
	}
	
	/**
	 * A helper for tracking the state of a sync for reporting in a {@link SyncException}.
	 * To use, create an instance at the start of a sync and invoke the various failed/success messages along the way.
	 * If you need to throw a SyncException you can pass this in to provide more detailed information.
	 */
	public static class Builder<T extends Thing> {
		
		/** The T that was passed into the request. (Not the result) */
		public final T thing;
		/** The Actions that were supplied. */
		public final Action[] actions;
		
		final Map<Action, Result> actionResults = new HashMap<>();
		List<Thing> resolved;
		Result thingResult;
		T thingReturned;
		
		/**
		 * @param t The thing passed into the sync request
		 * @param a The actions passed into the sync request
		 */
		public Builder(T t, Action[] a) {
			this.thing = t;
			this.actions = a != null ? a : new Action[0];
		}
		
		public void thing(T result) {
			thing(Status.SUCCESS, null, null);
			thingReturned = result;
		}
		
		public Builder<T> thing(Status status, Throwable cause, String details) {
			thingResult = new Result(status, cause, details);
			return this;
		}
		
		public void action(Action a, Status status, Throwable cause, String details) {
			action(a, new Result(status, cause, details));
		}
		
		public void action(Action a, Result result) {
			actionResults.put(a, result);
		}
		
		public boolean hasFailures() {
			if (isFailure(thingResult)) return true;
			for (Result r : actionResults.values()) {
				if (isFailure(r)) return true;
			}
			return false;
		}

		public boolean hasNonDiscardedFailures() {
			if (isNonDiscardedFailure(thingResult)) return true;
			for (Result r : actionResults.values()) {
				if (isNonDiscardedFailure(r)) return true;
			}
			return false;
		}

		/** returns true if at least one action or thing request has a successful status. */
		public boolean hasSuccesses() {
			for (Result r : actionResults.values()) {
				if (r != null && r.status == Status.SUCCESS) return true;
			}
			return thingResult != null && thingResult.status == Status.SUCCESS;
		}

		public SyncResult<T> build() {
			return build(Status.NOT_ATTEMPTED);
		}

		/** @param actionsRemaining A status to apply to any actions not yet given a status. */
		public SyncResult<T> build(Status actionsRemaining) {
			if (actions.length > 0) {
				for (Action a : actions) {
					Result r = actionResults.get(a);
					if (r == null) action(a, actionsRemaining, null, null);
				}
			}
			// Mark as not attempted if not specified.
			if (thingResult == null) thing(Status.NOT_ATTEMPTED, null, null);
			return new SyncResult<>(this);
		}
		
		public void resolved(Thing t) {
			if (resolved == null) resolved = new ArrayList<>();
			resolved.add(t);
		}
	}
	
	public boolean hasFailures() {
		if (isFailure(result_t)) return true;
		for (Result r : result_a.values()) {
			if (isFailure(r)) return true;
		}
		return false;
	}
	
	public Status statusOf(Action a) {
		Result r = result_a.get(a);
		return r != null ? r.status : Status.NOT_ATTEMPTED;
	}
	
	public Result resultOf(Action a) {
		return result_a.get(a);
	}
	
	private static boolean isFailure(Result r) {
		return r != null && (r.status == Status.FAILED || r.status == Status.FAILED_DISCARD);
	}

	private static boolean isNonDiscardedFailure(Result r) {
		return r != null && r.status == Status.FAILED;
	}
	
}
