package com.pocket.sdk.tts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pocket.app.PocketApp;
import com.pocket.app.session.ItemSessions;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.ItemSessionTriggerEvent;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.Interaction;

import org.threeten.bp.Duration;

import io.reactivex.Observable;

/** @see Listen#trackedControls(View, CxtUi)  */
class TrackedControls implements Controls {
	
	private final PocketApp app;
	private final Pocket pocket;
	private final Context context;
	private final Controls controls;
	private final Contextual contextual;
	private final StateChecker state;
	private final View view;
	private final CxtUi cxt_ui;
	
	TrackedControls(PocketApp app,
					Context context,
					Controls controls,
					StateChecker state,
					@Nullable View view,
					@Nullable CxtUi cxt_ui) {
		this.app = app;
		this.pocket = app.pocket();
		this.context = context;
		this.view = view;
		this.cxt_ui = cxt_ui;
		this.controls = controls;
		this.state = state;
		contextual = null;
	}
	
	TrackedControls(PocketApp app,
					Context context,
					Controls controls,
					StateChecker state,
					Contextual contextual) {
		this.app = app;
		this.pocket = app.pocket();
		this.context = context;
		this.controls = controls;
		this.state = state;
		this.contextual = contextual;
		view = null;
		cxt_ui = null;
	}
	
	private Interaction interaction() {
		return interaction(null);
	}
	
	private Interaction interaction(Track current) {
		Interaction it;
		if (contextual != null) {
			it = Interaction.on(contextual, context);
		} else if (view != null) {
			it = Interaction.on(view);
		} else {
			it = Interaction.on(context);
		}
		it = it.merge(buildListenContext(current));
		if (cxt_ui != null) {
			it = it.merge(cxt -> cxt.cxt_ui(cxt_ui));
		}
		return it;
	}
	
	private ActionContext buildListenContext(Track current) {
		final ListenState listenState = state.get();
		final ActionContext.Builder listenContext = new ActionContext.Builder()
				.cxt_index(listenState.index + 1);
		
		if (current == null) current = listenState.current;
		if (current != null) {
			listenContext.cxt_item_id(current.itemId);
			
			final Long sessionId = app.itemSessions().getSessionId(current.idUrl);
			if (sessionId != null) {
				listenContext.item_session_id(String.valueOf(sessionId));
			}
		}
		
		return listenContext.build();
	}
	
	@Override public void on() {
		controls.on();
		trackOn();
	}
	
	private void trackOn() {
		Interaction it = interaction();
		pocket.sync(null, pocket.spec().actions().listen_opened()
				.time(it.time)
				.context(it.context)
				.build());
	}
	
	@Override
	public void play() {
		controls.play();
		trackPlay();
	}
	
	@Override
	public void play(@NonNull Track track) {
		controls.play(track);
		trackPlay(track);
	}
	
	@Override
	public void play(@NonNull Track track, int nodeIndex) {
		if (state.get().playstate == PlayState.STOPPED) {
			trackOn();
		}
		controls.play(track, nodeIndex);
		trackPlay(track);
	}
	
	private void trackPlay(Track current) {
		Interaction it = interaction(current);
		final boolean isResume = !state.get().elapsed.isZero();
		
		app.itemSessions().startSegment(ItemSessions.LISTENING_SEGMENT,
				current.idUrl,
				current.itemId,
				isResume ? ItemSessionTriggerEvent.RESUME_LISTEN : ItemSessionTriggerEvent.START_LISTEN,
				it.context);
		
		if (isResume) {
			pocket.sync(null, pocket.spec().actions().resume_listen()
					.time(it.time)
					.context(it.context)
					.url(new UrlString(current.idUrl))
					.build());
		} else {
			pocket.sync(null, pocket.spec().actions().start_listen()
					.time(it.time)
					.context(it.context)
					.url(new UrlString(current.idUrl))
					.build());
		}

		if (current.idUrl != null) {
			pocket.sync(null, pocket
					.spec()
					.actions()
					.markAsViewed()
					.time(Timestamp.now())
					.url(new UrlString(current.idUrl))
					.build());
		}
	}
	
	@SuppressWarnings("ResultOfMethodCallIgnored") @SuppressLint("CheckResult")
	private void trackPlay() {
		// Listen for the first state (including current) that has non-null current track and track it.
		state.changes()
				.startWith(state.get())
				.filter(state -> state.current != null)
				.map(state -> state.current)
				.firstElement()
				.subscribe(this::trackPlay);
	}
	
	@Override
	public void playToggle() {
		if (state.get().playstate != PlayState.PLAYING) {
			controls.playToggle();
			trackPlay();
		} else {
			trackPause();
			controls.playToggle();
		}
	}
	
	@Override
	public void pause() {
		controls.pause();
		trackPause();
	}
	
	private void trackPause() {
		final Track current = state.get().current;
		if (current == null) {
			return; // Media buttons can trigger these events anytime, even if we aren't actively playing something, if this happens, ignore it.
		}
		Interaction it = interaction();
		
		pocket.sync(null, pocket.spec().actions().pause_listen()
				.time(it.time)
				.context(it.context)
				.url(new UrlString(current.idUrl))
				.build());
		
		app.itemSessions().softCloseSegment(ItemSessions.LISTENING_SEGMENT,
				current.idUrl,
				current.itemId,
				ItemSessionTriggerEvent.PAUSE_LISTEN,
				it.context);
	}
	
	@Override
	public void off() {
		trackOff();
		controls.off();
	}
	
	private void trackOff() {
		Interaction it = interaction();
		
		pocket.sync(null, pocket.spec().actions().listen_closed()
				.time(it.time)
				.context(it.context)
				.build());
		
		final ListenState state = this.state.get();
		final Track current = state.current;
		if (state.playstate == PlayState.PLAYING && current != null) {
			app.itemSessions().hardCloseSegment(ItemSessions.LISTENING_SEGMENT,
					current.idUrl,
					current.itemId,
					ItemSessionTriggerEvent.CLOSED_LISTEN,
					it.context);
		}
	}
	
	@Override public void remove(Track track) {
		controls.remove(track);
	}
	
	@Override
	public void next() {
		final ListenState state = this.state.get();
		if (state.list.isEmpty()) {
			return;
		}
		Interaction it = interaction();
		final int nextIndex = state.index + 1;
		Track next = state.list.get(nextIndex < state.list.size() ? nextIndex : 0);
		pocket.sync(null, pocket.spec().actions().skip_next_listen()
				.time(it.time)
				.context(it.context)
				.url(new UrlString(next.idUrl))
				.build());
		trackSkipSession(it, ItemSessionTriggerEvent.SKIP_NEXT_LISTEN);
		controls.next();
	}
	
	@Override
	public void previous() {
		final ListenState state = this.state.get();
		if (state.list.isEmpty()) {
			return;
		}
		Interaction it = interaction();
		final int previousIndex = Math.max(0, state.index - 1);
		Track previous = state.list.get(previousIndex);
		pocket.sync(null, pocket.spec().actions().skip_back_listen()
				.time(it.time)
				.context(it.context)
				.url(new UrlString(previous.idUrl))
				.build());
		trackSkipSession(it, ItemSessionTriggerEvent.SKIP_BACK_LISTEN);
		controls.previous();
	}
	
	@Override
	public void moveTo(int track) {
		Interaction it = interaction();
		trackSkipSession(it, ItemSessionTriggerEvent.START_LISTEN);
		controls.moveTo(track);
	}
	
	private void trackSkipSession(Interaction it, ItemSessionTriggerEvent endEvent) {
		final Track current = state.get().current;
		
		// If we're not playing then there's no item session to stop
		if (state.get().playstate == PlayState.PLAYING) {
			app.itemSessions().hardCloseSegment(ItemSessions.LISTENING_SEGMENT,
					current.idUrl,
					current.itemId,
					endEvent,
					it.context);
		}
	}
	
	@Override
	public void seekTo(Duration position) {
		trackSeek(position);
		controls.seekTo(position);
	}
	
	private void trackSeek(Duration position) {
		final ListenState state = this.state.get();
		Track current = state.current;
		if (current == null) {
			return; // Media buttons can trigger these events anytime, even if we aren't actively playing something, if this happens, ignore it.
		}
		
		final long percent = state.duration.isZero() ? 0
				: 100 * Math.abs(position.minus(state.elapsed).getSeconds()) / state.duration.getSeconds();
		
		final Interaction it = interaction();
		
		if (position.compareTo(state.elapsed) > 0) {
			pocket.sync(null, pocket.spec().actions().fast_forward_listen()
					.time(it.time)
					.context(it.context)
					.url(new UrlString(current.idUrl))
					.cxt_scroll_amount((int) percent)
					.build());
		} else {
			pocket.sync(null, pocket.spec().actions().rewind_listen()
					.time(it.time)
					.context(it.context)
					.url(new UrlString(current.idUrl))
					.cxt_scroll_amount((int) percent)
					.build());
		}
	}
	
	@Override
	public void setVoice(ListenState.Voice voice) {
		controls.setVoice(voice);
		trackVoiceChange(voice.getName());
	}
	
	private void trackVoiceChange(String name) {
		final Interaction it = interaction();
		app.pocket().sync(null, app.pocket().spec().actions().pv()
				.context(it.context)
				.section(CxtSection.SET_VOICE)
				.event(CxtEvent.create(name))
				.view(CxtView.LISTEN_SETTINGS)
				.time(it.time)
				.build());
	}
	
	@Override
	public void setSpeed(float speed) {
		controls.setSpeed(speed);
		trackSpeed(speed);
	}
	
	private void trackSpeed(float speed) {
		final Interaction it = interaction();
		app.pocket().sync(null, app.pocket().spec().actions().pv()
				.view(CxtView.LISTEN_PLAYER)
				.section(CxtSection.SET_SPEED)
				.event(CxtEvent.create(String.valueOf(speed)))
				.context(it.context)
				.time(it.time)
				.build());
	}
	
	@Override
	public void setPitch(float pitch) {
		controls.setPitch(pitch);
	}
	
	@Override
	public void setAutoArchive(boolean autoArchive) {
		controls.setAutoArchive(autoArchive);
	}
	
	@Override
	public void setAutoPlay(boolean autoPlay) {
		controls.setAutoPlay(autoPlay);
	}
	
	@Override
	public void foreground() {
		final ListenState state = this.state.get();
		if (state == null || state.playstate != PlayState.PLAYING || state.current == null) return;
		
		final Track current = state.current;
		
		app.itemSessions().startSegment(ItemSessions.LISTENING_SEGMENT,
				current.idUrl,
				current.itemId,
				ItemSessionTriggerEvent.OPENED_LISTEN,
				interaction(current).context);
	}
	
	interface StateChecker {
		ListenState get();
		Observable<ListenState> changes();
	}
}
