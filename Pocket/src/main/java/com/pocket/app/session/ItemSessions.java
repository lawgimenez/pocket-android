package com.pocket.app.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppMode;
import com.pocket.app.Feature;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.ItemSessionContinue;
import com.pocket.sdk.api.generated.action.ItemSessionEnd;
import com.pocket.sdk.api.generated.action.ItemSessionPause;
import com.pocket.sdk.api.generated.action.ItemSessionStart;
import com.pocket.sdk.api.generated.enums.ItemSessionTriggerEvent;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.util.java.Clock;
import com.pocket.util.java.Milliseconds;
import com.pocket.util.prefs.LongPreference;
import com.pocket.util.prefs.Preferences;
import com.pocket.util.prefs.StringPreference;

import org.apache.commons.lang3.StringUtils;
import org.threeten.bp.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This is the top level component for tracking item sessions (a.k.a. reading metrics).
 * <p/>
 * See <a href="https://docs.google.com/document/d/15zI7OkV6Y-pOotBXPx-D00V5IhwAM_tugNdQmsQSKrY/edit?disco=AAAABwll-4I&ts=5ad4d659">
 * the spec document</a> for a high level overview.
 */
@Singleton
public final class ItemSessions extends Feature implements AppLifecycle {
	
	public static final Session.Segment READING_SEGMENT = new Session.Segment() {};
	public static final Session.Segment LISTENING_SEGMENT = new Session.Segment() {};
	
	private static final long SESSION_EXPIRATION = Milliseconds.MINUTE * 3;
	
	private @Nullable Session session;
	
	private Analytics analytics;
	private Clock clock;
	private StringPreference currentUrl;
	private LongPreference sessionId;
	private LongPreference whenLastSegmentEnded;
	private LongPreference pauseDuration;

	@Inject
	public ItemSessions(
			AppMode mode,
			Pocket pocket,
			Preferences prefs,
			AppLifecycleEventDispatcher dispatcher
	) {
		this(mode, new Analytics(pocket, Clock.SYSTEM),
				Clock.SYSTEM,
				prefs);
		dispatcher.registerAppLifecycleObserver(this);
	}
	
	public ItemSessions(AppMode mode,
						Analytics analytics,
						Clock clock,
						Preferences prefs) {
		super(mode);
		this.analytics = analytics;
		this.clock = clock;
		this.currentUrl = prefs.forUser("itsess_url", "");
		this.sessionId = prefs.forUser("itsess_id", 0L);
		this.whenLastSegmentEnded = prefs.forUser("itsess_wlse", 0L);
		this.pauseDuration = prefs.forUser("itsess_tp", 0L);
	}
	
	@Override protected boolean isEnabled(Audience audience) {
		return true;
	}
	
	@Override public void onUserGone(Context context) {
		if (!isOn()) return;
		
		// User left the app, if there is an active reading session then pause it.
		if (session != null && session.getState() == Session.State.ACTIVE) {
			softCloseSegment(READING_SEGMENT, currentUrl.get(), null, ItemSessionTriggerEvent.CLOSED_APP, Interaction.on(context).context);
		}
	}
	
	public void startSegment(@NonNull Session.Segment segment,
			@NonNull String givenUrl,
			String itemId,
			ItemSessionTriggerEvent startEvent,
			@NonNull ActionContext uiContext) {
		if (!isOn()) return;
		
		if (session == null || !StringUtils.equals(givenUrl, currentUrl.get())) {
			// We need to create a new session.
			
			if (session != null && session.getState() == Session.State.ACTIVE) {
				// There is an existing active session.
				if (segment == READING_SEGMENT) {
					// If the new one is a reading session then close the existing one.
					
					analytics.fireEndAction(session.getSid(),
							currentUrl.get(),
							null,
							startEvent, // Use the same end trigger as the passed in start trigger.
							session.getTimeSpent(),
							uiContext);
					
				} else {
					// If Listen continues to the next item, while the user is active in a reading session
					// then ignore the Listen session, because Reader is "in the foreground" and takes precedence.
					return;
				}
			}
			
			if (session != null) {
				// If there was a previous session, clean up the prefs from old values.
				sessionId.set(0);
				whenLastSegmentEnded.set(0);
				pauseDuration.set(0);
			}
			
			session = new Session(SESSION_EXPIRATION, sessionId, whenLastSegmentEnded, clock);
			session.enableTimeTracking(pauseDuration);
			currentUrl.set(givenUrl);
		}
		
		final Session.State previousState = session.getState();
		session.startSegment(segment);
		
		switch (previousState) {
			case INACTIVE:
			case EXPIRED:
				analytics.fireStartAction(session.getSid(), givenUrl, itemId, startEvent, uiContext);
				break;
			case ACTIVE:
				// Already active, nothing to fire.
				break;
			case PAUSED:
				analytics.fireContinueAction(session.getSid(), givenUrl, itemId, startEvent, uiContext);
				break;
		}
	}
	
	public void softCloseSegment(@NonNull Session.Segment segment,
			@NonNull String givenUrl,
			String itemId,
			ItemSessionTriggerEvent endEvent,
			@NonNull ActionContext uiContext) {
		if (!isOn()) return;
		if (session == null || !StringUtils.equals(givenUrl, currentUrl.get())) {
			// Nothing to close.
			return;
		}
		
		final long sessionId = session.getSid();
		session.softCloseSegment(segment);
		
		if (session.getState() != Session.State.ACTIVE) {
			analytics.firePauseAction(sessionId, givenUrl, itemId, endEvent, session.getTimeSpent(), uiContext);
		}
	}
	
	public void hardCloseSegment(@NonNull Session.Segment segment,
			@NonNull String givenUrl,
			String itemId,
			ItemSessionTriggerEvent endEvent,
			@NonNull ActionContext uiContext) {
		if (!isOn()) return;
		if (session == null || !StringUtils.equals(givenUrl, currentUrl.get())) {
			// Nothing to close.
			return;
		}
		
		final long sessionId = session.getSid();
		session.hardCloseSegment(segment);
		
		if (session.getState() != Session.State.ACTIVE) {
			analytics.fireEndAction(sessionId, givenUrl, itemId, endEvent, session.getTimeSpent(), uiContext);
		}
	}

	/**
	 * @return Returns the session id only if there is an active session and it matches this item url
	 */
	public @Nullable Long getSessionId(String givenUrl) {
		if (session == null) return null;
		if (session.getState() != Session.State.ACTIVE) return null;
		if (!StringUtils.equals(givenUrl, currentUrl.get())) return null;
		
		return session.getSid();
	}

	/**
	 * @return If there is an active item session, returns its id
	 */
	public @Nullable Long getSessionId() {
		if (session == null) return null;
		if (session.getState() != Session.State.ACTIVE) return null;
		return session.getSid();
	}
	
	static class Analytics {
		private final Pocket pocket;
		private final Clock clock;
		
		Analytics(Pocket pocket, Clock clock) {
			this.pocket = pocket;
			this.clock = clock;
		}
		
		void fireStartAction(long sessionId,
				String url,
				String itemId,
				ItemSessionTriggerEvent startEvent,
				ActionContext uiContext) {
			final ItemSessionStart.Builder builder = pocket.spec().actions().item_session_start();
			if (itemId != null) {
				builder.item_id(itemId);
			}
			pocket.sync(null,
					builder.time(Timestamp.fromMillis(clock.now()))
							.context(uiContext)
							.item_session_id(String.valueOf(sessionId))
							.trigger_event(startEvent)
							.url(new UrlString(url))
							.build());
		}
		
		void firePauseAction(long sessionId,
				String url,
				String itemId,
				ItemSessionTriggerEvent endEvent,
				Duration timeSpent,
				ActionContext uiContext) {
			final ItemSessionPause.Builder builder = pocket.spec().actions().item_session_pause();
			if (itemId != null) {
				builder.item_id(itemId);
			}
			pocket.sync(null,
					builder.time(Timestamp.fromMillis(clock.now()))
							.context(uiContext)
							.item_session_id(String.valueOf(sessionId))
							.time_spent((int) timeSpent.getSeconds())
							.trigger_event(endEvent)
							.url(new UrlString(url))
							.build());
		}
		
		void fireContinueAction(long sessionId,
				String url,
				String itemId,
				ItemSessionTriggerEvent startEvent,
				ActionContext uiContext) {
			final ItemSessionContinue.Builder builder = pocket.spec().actions().item_session_continue();
			if (itemId != null) {
				builder.item_id(itemId);
			}
			pocket.sync(null,
					builder.time(Timestamp.fromMillis(clock.now()))
							.context(uiContext)
							.item_session_id(String.valueOf(sessionId))
							.trigger_event(startEvent)
							.url(new UrlString(url))
							.build());
		}
		
		void fireEndAction(long sessionId,
				String url,
				String itemId,
				ItemSessionTriggerEvent endEvent,
				Duration timeSpent,
				ActionContext uiContext) {
			final ItemSessionEnd.Builder builder = pocket.spec().actions().item_session_end();
			if (itemId != null) {
				builder.item_id(itemId);
			}
			pocket.sync(null,
					builder.time(Timestamp.now())
							.context(uiContext)
							.item_session_id(String.valueOf(sessionId))
							.time_spent((int) timeSpent.getSeconds())
							.trigger_event(endEvent)
							.url(new UrlString(url))
							.build());
		}
	}
}
