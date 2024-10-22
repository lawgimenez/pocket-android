package com.pocket.sync.source.result;

import com.pocket.sync.action.Action;
import com.pocket.sync.source.Source;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.UserFacingErrorMessage;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Map;

/**
 * Something went wrong when a {@link Source} was performing a Sync.
 * Provides detailed information about the attempted request, what was processed successfully and what failed.
 */
public class SyncException extends Exception implements UserFacingErrorMessage {
	
	public final SyncResult<?> result;
	
	public SyncException(SyncResult rb) {
		this(rb, null, null);
	}
	
	public SyncException(SyncResult r, Throwable cause) {
		this(r, cause, null);
	}
	
	public SyncException(SyncResult r, String message) {
		this(r, null, message);
	}
	
	public SyncException(SyncResult r, Throwable cause, String message) {
		super(message, cause);
		this.result = r;
	}
	
	private Phase whereDidThingsBreak() {
		boolean attempted = false;
		if (result != null && result.a.length > 0) {
			for (Result r : result.result_a.values()) {
				switch (r.status) {
					case NOT_ATTEMPTED:
						break;
					case FAILED:
					case FAILED_DISCARD:
						return Phase.ACTIONS;
					case IGNORED:
					case SUCCESS:
						attempted = true;
						break;
				}
			}
		}
		return attempted ? Phase.THING : Phase.SETUP;
	}
	
	@Override
	public synchronized Throwable getCause() {
		Throwable t = super.getCause();
		if (t != null) return t;
		// See if there is an underlying cause we can extract
		if (result != null) {
			for (Result ar : result.result_a.values()) {
				if (ar.cause != null) {
					return ar.cause;
				}
			}
			if (result.result_t.cause != null) return result.result_t.cause;
		}
		return null;
	}
	
	@Override
	public String getUserFacingMessage() {
		Throwable c = getCause();
		int i = ExceptionUtils.indexOfType(c, UserFacingErrorMessage.class);
		return i >= 0 ? ((UserFacingErrorMessage) ExceptionUtils.getThrowables(c)[i]).getUserFacingMessage() : "";
	}
	
	/**
	 * At what point did the sync fail?
	 */
	public enum Phase {
		/** There was an error initializing, setting up or connecting and nothing was even attempted yet. */
		SETUP,
		/** One or many actions had a problem. See {@link #result} for more details. When actions fail, the {@link Thing} will not have been attempted. */
		ACTIONS,
		/** Actions were successfully processed, but there was a problem retrieving the requested {@link Thing}. See {@link #result} for more details.  */
		THING
	}
	
	public static SyncException unwrap(Throwable t) {
		int i = ExceptionUtils.indexOfType(t, SyncException.class);
		return i >= 0 ? (SyncException) ExceptionUtils.getThrowables(t)[i] : null;
	}
	
	public static Status statusOf(Throwable t, Action a) {
		SyncException e = unwrap(t);
		return e != null ? e.result.statusOf(a) : null;
	}

	@Override
	public String getMessage() {
		// NOTE: Be VERY careful not to expose user info here that might end up in logs or crash reports.
		StringBuilder s = new StringBuilder();
		s.append(super.getMessage());
		s.append(" ");
		if (result != null) {
			s.append("(");
			s.append(result.t != null ? result.t.type() : "null");
			s.append(", [");
			if (result.a != null) {
				for (int i = 0; i < result.a.length; i++) {
					s.append(result.a[i] == null ? "null" : result.a[i].action());
					if (i < result.a.length-1) s.append(", ");
				}
			}
			s.append("]");
			s.append(")");
		}
		switch (whereDidThingsBreak()) {
			case SETUP:
				s.append("setup failed");
				break;
			case ACTIONS:
				s.append("actions failed [");
				int i = result.result_a.size();
				for (Map.Entry<Action, Result> a : result.result_a.entrySet()) {
					Result r = a.getValue();
					s.append(a.getKey().action()).append(" ");
					s.append(r.status);
					if (r.cause != null) {
						s.append(" ").append(r.cause);
					}
					if (--i > 0) s.append(",");
				}
				s.append("]");
				break;
			case THING:
				s.append("thing failed ");
				s.append(result.result_t.status);
				if (result.result_t.cause != null) {
					s.append(" ").append(result.result_t.cause);
				}
				break;
		}
		return s.toString();
	}
}
