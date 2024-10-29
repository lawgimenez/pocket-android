package com.pocket.sdk.tts;

import org.threeten.bp.Duration;

import io.reactivex.Observable;

interface StreamingPlayer {
	void play();
	void pause();
	void seekTo(Duration position);
	boolean isLoaded();
	boolean isPlaying();
	Duration getDuration();
	Duration getElapsed();
	void setSpeed(float speed);
	void setPitch(float pitch);
	Observable<?> getProgressUpdates();
	Observable<Float> getBufferingUpdates();
	Observable<?> getCompletions();
}
