package com.pocket.sdk.tts;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.AppThreads;
import com.pocket.app.FeatureStats;
import com.pocket.app.build.Versioning;
import com.pocket.app.session.AppSession;
import com.pocket.app.session.ItemSessions;
import com.pocket.app.session.Session;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.api.ServerFeatureFlags;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.ItemSessionTriggerEvent;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.generated.thing.ListenSettings;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.space.Holder;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Safe;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.FloatPreference;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.StringPreference;

import org.threeten.bp.Duration;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.PublishSubject;

/**
 * Allows the user to listen to their saved items.
 * <p>
 * Control playback with {@link #controls()}.
 * <p>
 * Track or read player state with {@link #state()} and {@link #states()}.
 *
 *
 * Notes:
 * At this point we might be at several different states.
 Idle / Stopped. Nothing is setup at all
 Starting. In the process of setting up
 Player Setup. but the play list isn't yet
 Setting up the play list
 Play list is setup, but no item selected yet.
 An item is selected, and either playing or paused
 *
 *
 *
 */
@Singleton
public class Listen implements AppLifecycle {
	private static final boolean DEBUG = BuildConfig.DEBUG && false;
	private static final int REQUEST_CODE = 555;
	private static final Session.Segment SESSION_SEGMENT = new Session.Segment() {};
	private static final String POLLY_REMOVAL_FLAG = "temp.android.app.listen.polly.removal";

	private final ListenControls controls = new ListenControls();
	private final Analytics analytics = new Analytics();
	private final PublishSubject<ListenState> subject = PublishSubject.create();
	private final CompositeDisposable subscriptions = new CompositeDisposable();
	private final AppThreads threads;
	private final FeatureStats featureStats;
	private final Pocket pocket;
	private final ItemSessions sessions;
	private final AppSession appSession;
	private final BooleanPreference prefAutoArchive;
	private final FloatPreference prefSpeed;
	private final BooleanPreference prefAutoPlay;
	private final BooleanPreference prefStreaming;
	private final StringPreference prefEngine;
	private final FloatPreference prefPitch;
	private final IntPreference maxWordCount;
	private final IntPreference minWordCount;
	private final FloatPreference streamingLowestFailedSpeed;
	private final ServerFeatureFlags flags;

	private ListenMediaSession android;
	private Context context;
	private ListenEngine engine;
	private ListenPlayer player;
	private ListenState state = new ListenState();
	private Playlist playlist = new NotLoaded();
	private ListenPlayer.OnLoaded onLoaded = new PauseWhenLoaded();
	private boolean pausedTransiently;
	private boolean streamingEngineEnabled = true;

	@Inject
	public Listen(AppThreads threads,
			FeatureStats featureStats,
			Pocket pocket,
			AppSession appSession,
			ItemSessions itemSessions,
			@ApplicationContext Context context,
			Versioning versioning,
			AppSync appsync,
			AppPrefs prefs,
			ServerFeatureFlags flags,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.prefAutoArchive = prefs.LISTEN_AUTO_ARCHIVE;
		this.prefSpeed = prefs.ARTICLE_TTS_SPEED;
		this.prefAutoPlay = prefs.ARTICLE_TTS_AUTO_PLAY;
		this.prefStreaming = prefs.LISTEN_USE_STREAMING_VOICE;
		this.prefEngine = prefs.TTS_ENGINE;
		this.prefPitch = prefs.ARTICLE_TTS_PITCH;
		this.maxWordCount = prefs.LISTEN_MAX_WORD_COUNT;
		this.minWordCount = prefs.LISTEN_MIN_WORD_COUNT;
		this.streamingLowestFailedSpeed = prefs.LISTEN_LOWEST_REPORTED_FAILING_SPEED;
		if (versioning.upgraded(7,0,0,0)) {
			prefAutoArchive.set(prefs.deprecateUserBoolean("articleTTSAutoArchive", true));  // Migrate to new pref, which has a new default value
		}
		if (versioning.upgraded(7,0,0,5)) {
			prefSpeed.set(1); // Reset all voice speed values to 1x since we're changing everyone's voice to the new one.
		}
		if (versioning.upgraded(7,0,1,4)) {
			// There's an old preference that Continuous Listen used to track the showing of a tooltip
			// the first time Listen was used. This means anyone still waiting to see the tooltip hasn't ever
			// used Continuous Listen.
			// We'd like to treat them as a new user and flip the auto-archive setting to the new default.
			// This will unfortunately also change the setting for anyone who manually updated it after
			// updating to 7.0.
			if (prefs.deprecateUserBoolean("introlisten_voices", true)) {
				prefAutoArchive.set(false);
			}
		}
		this.threads = threads;
		this.featureStats = featureStats;
		this.pocket = pocket;
		this.appSession = appSession;
		this.sessions = itemSessions;
		this.context = context;
		this.flags = flags;
		this.android = new ListenMediaSession(context, this, new AudioFocus.Control() {
			@Override public void play() {
				controls.play();
				
				analytics.startItemSession(state.current, Interaction.on(analytics, context).context);
			}
			
			@Override public void pause() {
				controls.pause();
				
				analytics.softCloseItemSession(state.current);
			}
			
			@Override public void pauseTransiently() {
				controls.pauseTransiently();
			}
		});
		updateEngine();
		setState(calculateCurrentState());
		
		appsync.addInitialFlags(g -> g.forcesettings(1));
		appsync.addFlags(g -> g.listen(true));
		pocket.setup(() -> {
			ListenSettings settings = pocket.spec().things().listenSettings().build();
			pocket.remember(Holder.persistent("listen"), settings);
			pocket.initialize(settings);
			pocket.subscribe(Changes.of(settings), u -> {
				if (u.declared.item_max_word_count && Safe.value(u.item_max_word_count) > 0) maxWordCount.set(u.item_max_word_count);
				if (u.declared.item_min_word_count && Safe.value(u.item_min_word_count) >= 0) minWordCount.set(Safe.value(u.item_min_word_count));
			});
		});
	}
	
	/**
	 * Controls for changing playback state. Remember this is a singleton controlled and will impact playback for the entire app.
	 * @see #trackedControls(View, CxtUi)
	 */
	public Controls controls() {
		return controls;
	}
	
	/**
	 * Same as {@link #controls()} except on this controller, all control calls will automatically send analytics.
	 * Use this for User triggered controls like UI, media buttons, etc.
	 * @param view If available, a view that represents the controls. Used for obtaining action context.
	 * @param cxt_ui If no view is available, you can just manually supply the cxt_ui.
	 */
	public Controls trackedControls(View view, CxtUi cxt_ui) {
		return new TrackedControls(App.from(context), context, controls, getStateChecker(), view, cxt_ui);
	}
	
	/**
	 * Same as {@link #controls()} except on this controller, all control calls will automatically send analytics.
	 * Use this for User triggered controls like UI, media buttons, etc.
	 * @param contextual Used for obtaining action context.
	 */
	public Controls trackedControls(Contextual contextual) {
		return new TrackedControls(App.from(context), context, controls, getStateChecker(), contextual);
	}
	
	private TrackedControls.StateChecker getStateChecker() {
		return new TrackedControls.StateChecker() {
			@Override public ListenState get() {
				return Listen.this.state();
			}
			
			@Override public Observable<ListenState> changes() {
				return Listen.this.states();
			}
		};
	}
	
	/** The current playback state. Immutable. Use {@link #states()} to track changes. */
	public ListenState state() {
		return state;
	}
	
	/** Subscribe to state changes. Safe for multiple subscribers. */
	public Observable<ListenState> states() {
		return subject;
	}
	
	public boolean isStreamingEngineEnabled() {
		return streamingEngineEnabled;
	}

	@Override
	public void onActivityResult(AbsPocketActivity activity, int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && !activity.isFinishing()) {
			TTSUtils.onInstallResponse(activity, resultCode, data);
		}
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		return new LogoutPolicy() {
			@Override public void stopModifyingUserData() {
				controls().off();
			}
			
			@Override public void deleteUserData() {}
			
			@Override public void restart() {}

			@Override public void onLoggedOut() {}
		};
	}
	
	public boolean isListenable(Track track) {
		return track != null && Safe.value(track.isArticle())
				&& Safe.value(track.wordCount) >= minWordCount.get()
				&& Safe.value(track.wordCount) <= maxWordCount.get();
	}
	
	private class ListenControls implements Controls {
		
		private boolean isActive() {
			return player != null && state.current != null;
		}
		
		@Override
		public void playToggle() {
			transact(() -> {
				resetErrors();
				if (isActive() && resumesWhenLoaded()) {
					pause();
				} else {
					play();
				}
			});
		}
		
		@Override
		public void seekTo(Duration position) {
			if (isActive()) {
				transact(() -> player.seekTo(position));
			}
		}
		
		@Override
		public void pause() {
			if (isActive()) transact(() -> {
				player.pause();
				onLoaded = new PauseWhenLoaded();
			});
		}
		
		/**
		 * Like {@link #pause()} but don't abandon focus because we want to regain it.
		 */
		void pauseTransiently() {
			if (isActive()) transact(() -> {
				pausedTransiently = true;
				player.pause();
				onLoaded = new PauseWhenLoaded();
				
			});
		}
		
		@Override
		public void setVoice(ListenState.Voice voice) {
			transact(() -> player.setVoice(voice));
		}
		
		@Override
		public void setSpeed(float speed) {
			transact(() -> {
				player.setSpeed(speed);
				prefSpeed.set(speed);
			});
		}
		
		@Override
		public void setPitch(float pitch) {
			transact(() -> {
				player.setPitch(pitch);
				prefPitch.set(pitch);
			});
		}
		
		@Override
		public void remove(Track track) {
			transact(() -> {
				if (isActive() && track.equals(state.current)) {
					// If we're currently playing the track we want to remove we need to skip to the next one.
					
					if (onLoaded instanceof ResumeWhenLoaded) {
						((ResumeWhenLoaded) onLoaded).setCxtUi(null);
					}
					skipTo(playlist.after(track));
				}
				
				playlist.remove(track);
			});
		}
		
		@Override
		public void next() {
			next(CxtUi.SKIP_NEXT);
		}
		
		/** Same as {@link #next()}, but when it happens automatically, not because of a user action. */
		void autoPlayNext() {
			next(CxtUi.PLAYED_NEXT);
		}
		
		private void next(CxtUi cxt_ui) {
			if (onLoaded instanceof ResumeWhenLoaded) {
				((ResumeWhenLoaded) onLoaded).setCxtUi(cxt_ui);
			}
			if (isActive()) transact(() -> skipTo(playlist.after(state.current)));
			
		}
		
		@Override
		public void previous() {
			if (onLoaded instanceof ResumeWhenLoaded) {
				((ResumeWhenLoaded) onLoaded).setCxtUi(CxtUi.SKIP_BACK);
			}
			if (isActive()) transact(() -> skipTo(playlist.before(state.current)));
		}
		
		@Override
		public void moveTo(int track) {
			if (onLoaded instanceof ResumeWhenLoaded) {
				((ResumeWhenLoaded) onLoaded).setCxtUi(null);
			}
			if (isActive()) transact(() -> skipTo(playlist.get(track)));
		}
		
		@Override
		public void off() {
			transact(() -> {
				disconnectPlayer();
				state = new ListenState();
				playlist.clear();
				playlist = new NotLoaded();
				onLoaded = new PauseWhenLoaded();
			});
		}
		
		@Override
		public void setAutoArchive(boolean autoArchive) {
			transact(() -> prefAutoArchive.set(autoArchive)); // The transaction will pick up the new value.
		}
		
		@Override
		public void setAutoPlay(boolean value) {
			transact(() -> prefAutoPlay.set(value)); // The transaction will pick up the new value.
		}
		
		@Override
		public void on() {
			load(null);
		}
		
		@Override
		public void play() {
			onLoaded = new ResumeWhenLoaded(START_FROM_SAVED_POSITION);
			load(null);
		}
		
		@Override
		public void play(@NonNull Track track) {
			play(track, START_FROM_SAVED_POSITION);
		}
		
		@Override
		public void play(@NonNull Track track, int nodeIndex) {
			onLoaded = new ResumeWhenLoaded(nodeIndex);
			load(track);
		}
		
		private void load(Track track) {
			initPlaylist();
			pausedTransiently = false;

			flags.get(POLLY_REMOVAL_FLAG, null).onSuccess(assignment -> {
				if (assignment == null) {
					streamingEngineEnabled = true;
				} else if (!assignment.assigned) {
					streamingEngineEnabled = true;
				} else if ("control".equals(assignment.variant)) {
					streamingEngineEnabled = true;
				} else {
					streamingEngineEnabled = false;
				}
				updateEngine();
				loadWhenSetup(track);
			});
		}

		private void loadWhenSetup(Track track) {
			runWhenSetup(() -> transact(() -> {
				
				final Track moveTo;
				if (track != null && track.equals(state.current)) {
					moveTo = state.current;
					
				} else if (track != null) {
					// Move to or insert element
					moveTo = track;
					int index = playlist.indexOf(track);
					if (index >= 0) {
						// Remove tracks from the top so that the target is top of the playlist.
						while (!playlist.get(0).equals(track)) {
							playlist.remove(playlist.get(0));
						}
					} else {
						// Insert new track, into current position
						index = Math.max(0, playlist.indexOf(state.current));
						playlist.insert(index, track);
					}
					
				} else if (state.current == null) {
					if (playlist.size() > 0) {
						// Pick the first one
						moveTo = playlist.get(0);
					} else {
						// Nothing to play
						onEmptyPlaylist();
						return;
					}
					
				} else {
					moveTo = state.current;
				}
				
				setTrack(moveTo);
			}));
		}
		
		private void initPlaylist() {
			if (playlist.size() == 0) {
				playlist = new UnreadArticlesList(pocket,
						threads,
						minWordCount.get(),
						maxWordCount.get());
				playlist.setListener(p -> pushState());
			}
		}
		
		/**
		 * Sets this as the new track. If null, returns to the beginning of the playlist and pauses.
		 * Player must be active. Expected as part of a transaction.
		 */
		private void skipTo(Track e) {
			if (e == null) {
				if (playlist.size() > 0) {
					onLoaded = new PauseWhenLoaded();
					setTrack(playlist.get(0));
				} else {
					onEmptyPlaylist();
				}
			} else {
				setTrack(e);
			}
		}
		
		/**
		 * Moves the current focus to this element and loads it as needed, firing the loaded callback when ready.
		 */
		private void setTrack(@NonNull Track track) {
			if (!track.equals(state.current) || !player.isLoaded()) { // TODO equality
				setState(state.rebuild()
						.current(track)
						.build());
				
				// Intentionally not passing onLoaded directly nor using a method reference in case onLoaded changes
				// between when .load() is called and when the callback actually fires.
				player.load(track, () -> onLoaded.onArticleLoaded());
				
			} else if (onLoaded != null) {
				onLoaded.onArticleLoaded();
			}
		}
		
		/**
		 * @param nodeIndex The starting node index, or {@link Controls#START_FROM_SAVED_POSITION}.
		 */
		private void resume(int nodeIndex, @Nullable CxtUi cxt_ui) {
			if (player.isPlaying()) {
				return;
			}
			
			resetErrors();
			
			if (android.requestAudioFocus()) {
				if (nodeIndex == START_FROM_SAVED_POSITION) {
					nodeIndex = Safe.getInt(() -> state.current.getArticlePosition().getNodeIndex());
				}
				if (nodeIndex > 0) {
					player.playFromNodeIndex(nodeIndex);
				} else {
					player.play();
				}
				
				analytics.trackNonUiPlay(cxt_ui);
				
			} else {
				pause();
			}
		}
		
		private void resetErrors() {
			setState(state.rebuild().error(null).build());
		}
		
		private void onEmptyPlaylist() {
			playlist.clear(); // Clear so it can be reloaded on next attempt
			pause();
			setState(state.rebuild().error(ListenError.EMPTY_LIST).build());
			// calculatePlayState will notice the error state.
		}
	}
	
	/**
	 * @return true if Listen is either currently playing or expected to resume playing
	 * once buffering/loading completes
	 * 
	 * TODO Revisit to see if we can pick a better name or clarify otherwise
	 */
	private boolean resumesWhenLoaded() {
		return onLoaded instanceof ResumeWhenLoaded;
	}
	
	private void disconnectPlayer() {
		if (player != null) {
			player.release();
			player = null;
		}
		subscriptions.clear();
	}
	
	private void pushState() {
		transact(() -> {});
	}
	
	private void transact(Runnable runnable) {
		String debugStack = DEBUG ? Logs.stack(8) : null;
		threads.runOrPostOnUiThread(() -> {
			if (state.playstate != PlayState.STOPPED) {
				android.startSession();
				appSession.startSegment(SESSION_SEGMENT);
			}
			
			ListenState before = state;
			runnable.run();
			setState(calculateCurrentState());
			if (state.error != null) {
				handleError();
			}
			if (before.playstate == PlayState.STOPPED && state.playstate != PlayState.STOPPED) {
				// Became active.
				featureStats.trackUse(FeatureStats.Feature.LISTEN);
			}
			if (!deepEquals(before, state)) {
				subject.onNext(state);
			}
			
			if (state.playstate != PlayState.PLAYING && state.playstate != PlayState.PAUSED_TRANSIENTLY) {
				android.abandonFocus();
			}
			if (state.playstate == PlayState.STOPPED) {
				android.stopSession();
				appSession.closeSegment(SESSION_SEGMENT);
			}
			
			if (DEBUG) {
				Logs.d("Listen", state.toString() + " " + debugStack);
			}
		});
	}
	
	private void handleError() {
		if (state.playstate == PlayState.PLAYING) {
			analytics.hardCloseItemSession(state.current, ItemSessionTriggerEvent.ERROR);
		}
		setState(state.rebuild().playstate(PlayState.ERROR).build());
		onLoaded = new PauseWhenLoaded();
		if (state.error == ListenError.INIT_FAILED ||
				state.error == ListenError.LOGGED_OUT ||
				state.error == ListenError.NO_VOICES ||
				state.error == ListenError.NO_TTS_INSTALLED) {
			disconnectPlayer();
		}
	}
	
	private boolean deepEquals(ListenState s1, ListenState s2) {
		if (!s1.equals(s2)) {
			return false;
		} else {
			// Track's default equality is just on the url, but here we want to see if the entire item state changed or not.
			int len = s1.list.size();
			for (int i = 0; i < len; i++) {
				if (!s1.list.get(i).equals(s2.list.get(i))) {
					return false;
				}
			}
		}
		return true;
	}
	
	private void runWhenSetup(Runnable runnable) {
		transact(() -> {
			if (player == null) listenForEngineChanges();
			
			Playlist.OnLoad onLoaded = (p) -> runnable.run();
			if (engine.isValid(player)) {
				threads.async(() -> playlist.load(onLoaded));
			} else {
				if (player != null) {
					disconnectPlayer();
				}
				
				player = engine.createPlayer(context,
						pocket,
						threads,
						android,
						prefSpeed.get(),
						streamingLowestFailedSpeed);
				final Observable<Float> bufferingUpdates = player.getBufferingUpdates().share();
				
				subscriptions.addAll(
						player.getReadies()
								.subscribe(ready -> threads.async(() -> playlist.load(onLoaded))),
						
						player.getStartedUtterances()
								.subscribe(utterance -> transact(() -> setState(state.rebuild()
										.utterance(utterance)
										.build()))),
						
						player.getProgressUpdates()
								.subscribe(progress -> transact(() -> { /* Pick up the progress update */ })),
						
						bufferingUpdates.subscribe(progress ->
								transact(() -> setState(state.rebuild()
										.bufferingProgress(progress)
										.build()))),
						
						player.getCompletions()
								.subscribe(completed -> onTrackCompleted()),
						
						player.getErrors().subscribe(error ->
								transact(() -> setState(state.rebuild().error(error).build())))
				);
				
				if (state.supportedFeatures.contains(ListenState.Feature.PRELOADING)) {
					final Observable<Boolean> bufferingComplete =
							bufferingUpdates.map(progress -> progress == 1).distinctUntilChanged();
					final Observable<Boolean> isAutoPlayOn =
							states().map(state -> state.autoPlay).distinctUntilChanged();
					subscriptions.add(Observable.combineLatest(
							bufferingComplete, isAutoPlayOn, (complete, autoPlay) -> complete && autoPlay)
							.subscribe(preloadNext -> {
								final Track track = playlist.after(state.current);
								if (preloadNext && track != null) {
									player.preloadNext(track.itemId);
								}
							}));
				}
			}
		});
	}
	
	private void listenForEngineChanges() {
		final Observable<?> streamingChanges = prefStreaming.changes();
		final Observable<?> ttsChanges = prefEngine.changes();
		final Observable<?> engineChanges = streamingChanges.cast(Object.class).mergeWith(ttsChanges);
		
		subscriptions.add(engineChanges.subscribe(change -> {
			if (player != null) {
				final Track current = state.current;
				transact(() -> {
					disconnectPlayer();
					updateEngine();
					setState(state.rebuild().current(null).build());
				});
				onLoaded = new PauseWhenLoaded();
				controls.load(current);
			}
		}));
	}
	
	private void updateEngine() {
		if (streamingEngineEnabled && prefStreaming.get()) {
			engine = ListenEngine.Streaming.INSTANCE;
		} else {
			engine = ListenEngine.Tts.INSTANCE;
		}
	}
	
	private void onTrackCompleted() {
		final Track track = state.current;
		
		analytics.trackReachEnd(track);
		
		final Interaction interaction = Interaction.on(analytics, context);
		if (state.autoArchive) {
			pocket.sync(null, pocket.spec().actions().archive()
					.item_id(track.itemId)
					.url(new UrlString(track.idUrl))
					.time(interaction.time)
					.context(interaction.context)
					.build());
		}
		
		if (state.autoPlay) {
			analytics.hardCloseItemSession(
					state.current,
					ItemSessionTriggerEvent.REACH_END_LISTEN,
					interaction.context);
			
			if (!playlist.isLast(state.current)) {
				analytics.startItemSession(playlist.after(state.current), interaction.merge(cxt -> cxt.cxt_ui(CxtUi.PLAYED_NEXT)).context);
			}
			
			controls.autoPlayNext();
		}
	}
	
	private ListenState calculateCurrentState() {
		ListenState.Builder builder = state.rebuild()
				.speed(prefSpeed.get())
				.pitch(prefPitch.get())
				.autoArchive(prefAutoArchive.get())
				.autoPlay(prefAutoPlay.get())
				.supportedFeatures(engine.supportedFeatures());
		
		if (player != null) {
			builder.voice(player.getVoice()).voices(player.getVoices());
		} else {
			builder.voice(null).voices(Collections.emptySet());
		}
		
		if (playlist.isLoaded()) {
			builder
				.list(playlist.get())
				.index(playlist.contains(state.current) ? playlist.indexOf(state.current) : 0);
			
			if (playlist.size() == 0) {
				return builder
						.duration(Duration.ZERO)
						.elapsed(Duration.ZERO)
						.bufferingProgress(0)
						.error(ListenError.EMPTY_LIST)
						.playstate(PlayState.ERROR)
						.build();
				
			} else if (player != null && player.isLoaded()) {
				return builder
						.duration(player.getDuration())
						.elapsed(player.getElapsed())
						.playstate(player.isPlaying() ? PlayState.PLAYING
								: pausedTransiently ? PlayState.PAUSED_TRANSIENTLY : PlayState.PAUSED)
						.build();
				
			} else {
				return builder
						.duration(Duration.ZERO)
						.elapsed(Duration.ZERO)
						.playstate(resumesWhenLoaded() ? PlayState.BUFFERING : PlayState.PAUSED)
						.build();
			}
		} else {
			return builder
					.utterance(null)
					.duration(Duration.ZERO)
					.elapsed(Duration.ZERO)
					.playstate(player != null ? PlayState.STARTING : PlayState.STOPPED)
					.build();
		}
	}
	
	private void setState(ListenState state) {
		this.state = state;
	}
	
	class Analytics implements Contextual {
		
		@Override
		public ActionContext getActionContext() {
			final ActionContext.Builder actionContext = new ActionContext.Builder()
					.cxt_view(CxtView.LISTEN)
					.cxt_index(state.index + 1)
					.cxt_progress(state.getProgressPercent());
			if (state.current != null) {
				final Long sessionId = sessions.getSessionId(state.current.idUrl);
				if (sessionId != null) {
					actionContext.item_session_id(String.valueOf(sessionId));
				}
			}
			return actionContext.build();
		}
		
		void startItemSession(Track current, ActionContext uiContext) {
			sessions.startSegment(ItemSessions.LISTENING_SEGMENT,
					current.idUrl,
					current.itemId,
					ItemSessionTriggerEvent.START_LISTEN,
					uiContext);
		}
		
		void softCloseItemSession(Track current) {
			if (current != null) {
				sessions.softCloseSegment(ItemSessions.LISTENING_SEGMENT,
						current.idUrl,
						current.itemId,
						ItemSessionTriggerEvent.PAUSE_LISTEN,
						Interaction.on(this, context).context);
			}
		}
		
		void hardCloseItemSession(Track current, ItemSessionTriggerEvent endEvent) {
			if (current != null) {
				hardCloseItemSession(current, endEvent, Interaction.on(this, context).context);
			}
		}
		
		void hardCloseItemSession(Track current, ItemSessionTriggerEvent endEvent, ActionContext uiContext) {
				sessions.hardCloseSegment(ItemSessions.LISTENING_SEGMENT,
						current.idUrl,
						current.itemId,
						endEvent,
						uiContext);
		}
		
		void trackNonUiPlay(@Nullable CxtUi cxt_ui) {
			if (cxt_ui != null) {
				final Interaction it = Interaction.on(this, context).merge(cxt -> cxt.cxt_ui(cxt_ui));
				sessions.startSegment(ItemSessions.LISTENING_SEGMENT,
						state.current.idUrl,
						state.current.itemId,
						ItemSessionTriggerEvent.START_LISTEN,
						it.context);
				pocket.sync(null, pocket.spec().actions().start_listen()
						.url(new UrlString(state.current.idUrl))
						.context(it.context)
						.time(Timestamp.now())
						.build());
			}
		}
		
		void trackReachEnd(Track track) {
			final Interaction interaction = Interaction.on(this, context);
			pocket.sync(null, pocket.spec().actions().reach_end_listen()
					.item_id(track.itemId)
					.time(interaction.time)
					.context(new ActionContext.Builder(interaction.context).cxt_progress(100).build())
					.build());
		}
	}
	
	class PauseWhenLoaded implements ListenPlayer.OnLoaded {
		@Override public void onArticleLoaded() {
			transact(controls::pause);
		}
	}
	
	class ResumeWhenLoaded implements ListenPlayer.OnLoaded {
		private final int nodeIndex;
		private CxtUi cxt_ui;
		
		ResumeWhenLoaded(int nodeIndex) {
			this.nodeIndex = nodeIndex;
		}
		
		@Override public void onArticleLoaded() {
			transact(() -> controls.resume(nodeIndex, cxt_ui));
		}
		
		void setCxtUi(CxtUi cxt_ui) {
			this.cxt_ui = cxt_ui;
		}
	}
}
