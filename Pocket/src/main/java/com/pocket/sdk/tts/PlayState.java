package com.pocket.sdk.tts;

/**
 * TODO Documentation
 */
public enum PlayState {
	/** The player is off, idle, not doing anything. This is the default state. */
	STOPPED,
	/** In the start up / loading phase. */
	STARTING,
	
	PLAYING,
	PAUSED,
	
	/** Temporarily paused while another application transiently gained audio focus. */
	PAUSED_TRANSIENTLY,
	
	/** In the case of Listen, this means the play is ready, but it is currently loading in an article. */
	BUFFERING,
	
	/** The player encountered an error that must be resolved before continuing. See {@link ListenState#error} */
	ERROR
}
