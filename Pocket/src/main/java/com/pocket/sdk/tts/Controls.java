package com.pocket.sdk.tts;


import com.pocket.sdk.api.generated.thing.Item;

import org.threeten.bp.Duration;

import androidx.annotation.NonNull;

/**
 * TODO Documentation
 */
public interface Controls {
	
	int START_FROM_SAVED_POSITION = -1;
	
	/**
	 * Initialise Listen, the player, acquire needed resources, load the playlist.
	 * This likely will show related UI.
	 */
	void on();
	
	/**
	 * If player state is {@link PlayState#STOPPED} then start playing from beginning of the list.
	 * Otherwise, just resume playback from the current item and position.
	 * <p>
	 * Note: If starting from {@link PlayState#STOPPED}, it will cycle through {@link PlayState#STARTING}
	 * to load the player and list. It may also throw errors in the process.
 	 */
	void play();
	
	/**
	 * Performs a {@link #play()} if paused or stopped. If playing, it performs a {@link #pause()}.
 	 */
	void playToggle();
	
	/** Proxy for {@link #play(Track, int)} with {@link #START_FROM_SAVED_POSITION}. */
	void play(@NonNull Track track);
	
	/**
	 * Play just this single item. Will create (or replace an existing one with) a playlist with a single item.
	 * @param track The track to play
	 * @param nodeIndex The node index to start at, or {@link #START_FROM_SAVED_POSITION} to begin where the user last left off.
	 */
	void play(@NonNull Track track, int nodeIndex);
	
	/**
	 * Pause playback. If not started at all, this has no effect.
	 * If completely done, use {@link #off()}.
	 */
	void pause();
	
	/**
	 * Completely stop the player, releasing all resources and clearing the current playlist.
	 * This likely will hide all related UI.
	 */
	void off();
	
	/**
	 * Remove a track from playlist and if it's the current track skip to the next item in the playlist.
	 * Keeps the current playback state. For example, if currently playing, immediately start playing on the new item. If paused, stays paused, just at the new item.
	 * If there are no new items in the playlist, reset to the first item and pause. // REVIEW is this the right behaviour?
	 * @param track to remove
	 */
	void remove(Track track);
	
	/**
	 * Skip to the next item in the playlist.
	 * Keeps the current playback state. For example, if currently playing, immediately start playing on the new item. If paused, stays paused, just at the new item.
	 * If there are no new items in the playlist, reset to the first item and pause. // REVIEW is this the right behaviour?
	 */
	void next();
	
	/**
	 * If playback time is greater than 5 seconds, return the playback position within the current item to the beginning.
	 * Otherwise, skip to the previous item in the playlist.
	 * Keeps the current playback state. For example, if currently playing, immediately start playing on the new item. If paused, stays paused, just at the new item.
	 * If there are no new items in the playlist, reset to the first item and pause. // REVIEW is this the right behaviour?
	 */
	void previous();
	
	/**
	 * Change the playback position within the current item to the specified position.
	 * @param position The time (between 0 and item duration) to seek to
	 */
	void seekTo(Duration position);
	
	/**
	 * Set the voice
	 * If playing, the current utterance will be restarted.
	 *
	 * @param voice
	 */
	void setVoice(ListenState.Voice voice);
	
	/**
	 * Set the speech/playback speed.
	 * If playing, the current utterance might be restarted.
	 *
	 * @param speed
	 */
	void setSpeed(float speed);
	
	/**
	 * Set the speech/playback pitch.
	 * If playing, the current utterance might be restarted.
	 *
	 * @param pitch
	 */
	void setPitch(float pitch);
	
	/**
	 * Similar to {@link #next} and {@link #previous()} but lets you select the exact track to jump to.
	 * @param track
	 */
	void moveTo(int track);
	
	/**
	 * Whether or not items should be automatically archived when playback of that item fully completes.
	 * @param autoArchive
	 */
	void setAutoArchive(boolean autoArchive);
	
	/**
	 * Whether or not, when playback reaches the end of an article, the next item should be played automatically.
	 * @param autoPlay
	 */
	void setAutoPlay(boolean autoPlay);
	
	/**
	 * Let Listen know it's now in foreground. For now only used to send additional tracking action.
	 */
	default void foreground() {}
}
