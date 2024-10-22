package com.pocket.app.session;

import com.pocket.util.java.Logs;
import com.pocket.util.java.Clock;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.prefs.LongPreference;

import org.threeten.bp.Duration;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

public final class Session {
	
	private static final boolean DEBUG = false;
	
	/**
	 * In case the last segment ends with a soft-close, if a new segment is opened within this time then the same
	 * session id is used. After this time expires a new session id is created.
	 * <p>
	 * In case the last segment ends with a hard-close, there is no expiration, session ends immediately.
	 */
	private final long sessionExpiration;
	
	private final LongPreference sessionId;
	private final LongPreference whenLastSegmentEnded;
	private final Clock clock;
	
	private final Set<Segment> activeSegments = new HashSet<>();
	
	private State state = State.INACTIVE;
	
	/** Total time spent {@link State#PAUSED} in milliseconds. */
	private LongPreference pauseDuration = LongPreference.NO_OP;
	private Duration timeSpent = Duration.ZERO;
	
	Session(long sessionExpiration,
			LongPreference sessionId,
			LongPreference whenLastSegmentEnded,
			Clock clock) {
		this.sessionExpiration = sessionExpiration;
		this.sessionId = sessionId;
		this.whenLastSegmentEnded = whenLastSegmentEnded;
		this.clock = clock;
		
		if (sessionId.get() != 0) {
			state = State.PAUSED;
		}
	}
	
	void enableTimeTracking(LongPreference pauseDuration) {
		this.pauseDuration = pauseDuration;
	}
	
	/**
	 * @return The sid of the current session.
	 */
	synchronized long getSid() {
		if (DEBUG) log("get");
		
		final long sid;
		final State state = getState();
		if (state == State.INACTIVE || state == State.EXPIRED) {
			if (DEBUG) log("get ~ GENERATE NEW");
			// Generate New
			sid = clock.now();
			sessionId.set(sid);
			
		} else {
			sid = sessionId.get();
		}
		return Milliseconds.toSeconds(sid); // Return as seconds, which the server uses instead of millis.
	}
	
	synchronized State getState() {
		if (hasExpired()) {
			state = State.EXPIRED;
			pauseDuration.set(0);
		}
		return state;
	}
	
	private boolean hasExpired() {
		if (state != State.PAUSED) {
			// Can only expire from paused state.
			return false;
		}
		
		long lastPause = whenLastSegmentEnded.get();
		if (lastPause <= 0) {
			if (DEBUG) log("isExpired = false ~ first session");
			return false; // This must be the very first session and it hasn't ended yet.
		}
		
		long sid = sessionId.get();
		if (lastPause < sid) {
			// A new sid was generated since the previous pause, and this new sid is still active. This can happen if 
			// getSid() generates a new sid before a session segment is started.
			if (DEBUG) log("isExpired = false ~ is new");
			return false;
		}
		
		long sinceLastPause = clock.now() - lastPause;
		if (sinceLastPause < 0) {
			if (DEBUG) log("isExpired = false ~ invalid last pause");
			return true; // Not expected, but reset the session if this happens.
		}
		
		if (sinceLastPause >= sessionExpiration) {
			if (DEBUG) log("isExpired = true");
			return true;
			
		} else {
			if (DEBUG) log("isExpired = false");
			return false;
		}
	}
	
	synchronized void startSegment(@NonNull Segment segment) {
		if (DEBUG) log("start segment " + segment);
		
		if (activeSegments.isEmpty()) {
			getSid();// Start of session, generate new one if needed.
			
			long lastPause = whenLastSegmentEnded.get();
			if (lastPause > 0 && lastPause > sessionId.get()) {
				pauseDuration.set(pauseDuration.get() + clock.now() - lastPause);
			}
		}
		activeSegments.add(segment);
		state = State.ACTIVE;
	}
	
	void softCloseSegment(Segment segment) {
		closeSegment(segment, false);
	}
	
	void hardCloseSegment(Segment segment) {
		closeSegment(segment, true);
	}
	
	private synchronized void closeSegment(Segment segment, boolean hard) {
		if (DEBUG) log("close segment " + segment + ", hard = " + hard);
		
		activeSegments.remove(segment);
		if (activeSegments.isEmpty()) {
			whenLastSegmentEnded.set(clock.now());
			timeSpent = Duration.ofMillis(whenLastSegmentEnded.get() - sessionId.get() - pauseDuration.get());
			if (hard) {
				sessionId.set(0);
				pauseDuration.set(0);
				state = State.INACTIVE;
			} else {
				state = State.PAUSED;
			}
			if (DEBUG) log("close segment NOW EMPTY");
		}
	}
	
	/**
	 * Returns for how long this session was active (excluding any time it was paused).
	 * It isn't updated while the session is active, so to get an accurate value first make sure there are no
	 * active/open segments (the session is paused or (hard-)closed).
	 */
	synchronized Duration getTimeSpent() {
		return timeSpent;
	}
	
	enum State {
		INACTIVE, ACTIVE, PAUSED, EXPIRED
	}
	
	/**
	 * An empty interface to represent a particular piece of the app the user perceives to be using. This might be an
	 * activity or a feature like audio playback in the background.
	 * Provided as an interface so your class can implement it. If you are looking for a simple object to use instead,
	 * create a new one like so:
	 * <pre>
	 *     Session.Segment segment = new Session.Segment() {};
	 * </pre>
	 */
	public interface Segment {}
	
	private synchronized void log(String log) {
		if (DEBUG) Logs.v("PktSession", log + " active:" + activeSegments.size());
	}
}
