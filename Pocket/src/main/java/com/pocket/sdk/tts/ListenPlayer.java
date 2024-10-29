package com.pocket.sdk.tts;

import org.threeten.bp.Duration;

import java.util.Set;

import io.reactivex.Observable;

/**
 * A player that can play articles. Can use for example on-device TTS or play audio files downloaded from a server.
 */
public interface ListenPlayer {
	/**
	 * Move to the specified playback position.
	 * @param position where to move to
	 */
	void seekTo(Duration position);
	
	/**
	 * Load an article.
	 *
	 * @param track article to load
	 * @param loaded callback after a successful load
	 */
	void load(Track track, OnLoaded loaded);
	/**
	 * Preload the next item in the background so it's ready to play sooner when the current item
	 * finishes and Listen auto-advances to the next.
	 * <p>
	 * For some players this might be a no-op if playing an item doesn't require any time-consuming
	 * loading.
	 */
	void preloadNext(String itemId);
	
	/**
	 * Is there an article loaded into the player?
	 */
	boolean isLoaded();
	
	void play();
	void pause();
	boolean isPlaying();
	Duration getDuration();
	Duration getElapsed();
	ListenState.Voice getVoice();
	Set<VoiceCompat.Voice> getVoices();
	void setVoice(ListenState.Voice voice);
	void setSpeed(float rate);
	void setPitch(float pitch);
	void release();
	
	/** A stream that emits when the player finishes initialisation and is ready */
	Observable<?> getReadies();
	/** A stream of utterances emitted as they start playing */
	Observable<Utterance> getStartedUtterances();
	/** A stream that emits any time the progress changes */
	Observable<?> getProgressUpdates();
	/** A stream that emits new buffering progress values */
	Observable<Float> getBufferingUpdates();
	/** A stream that emits when the player completes playing an item */
	Observable<?> getCompletions();
	/**
	 * A stream of all the errors.
	 * They can happen during initialisation, playback and any other phases.
	 */
	Observable<ListenError> getErrors();
	
	// TODO support resuming from specified position in StreamingPlayer and design API common with TTSPlayer
	void playFromNodeIndex(int nodeIndex);
	
	interface OnLoaded { void onArticleLoaded(); }
}
