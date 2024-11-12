package com.pocket.sdk.tts;

import android.app.Activity;

/**
 * What that can go wrong when interacting with {@link Listen}.
 */
public enum ListenError {
	
	/**
	 * TTS functionality is missing from this device.
	 * Potential fix is to link them to the device TTS settings.
	 * @see TTSUtils#openTTSSettings(Activity)
	 */
	NO_TTS_INSTALLED,
	
	/**
	 * TTS could not be initialized for some unknown reason.
	 * Potential fix is to link them to the device TTS settings.
	 * @see TTSUtils#openTTSSettings(Activity)
	 * </p>
	 */
	INIT_FAILED,
	
	/**
	 * There are no installed voices for TTS.
	 * Fix is for the user to install a voice.
	 */
	NO_VOICES,
	
	/**
	 * Listen isn't allowed while logged out.
	 */
	LOGGED_OUT,
	
	/**
	 * Article content not available. This typically means that the content
	 * is not downloaded for offline viewing and it had to try to download it,
	 * and the download failed.
	 * Fix is to go online and retry, or for the user to select a different article.
	 */
	ARTICLE_NOT_DOWNLOADED,
	
	/**
	 * For some unknown reason, the article failed to parse into a readable
	 * format.
	 * There is likely no retry, the user must pick a new article.
	 */
	ARTICLE_PARSING_FAILED,
	
	/**
	 * An utterance failed to render. This is likely due to using a network
	 * based voice and having network issues. Could also just be a general
	 * error with text to speech or something in the utterance that
	 * breaks things.
	 * Potential fixes include skipping the utterance, prompting the user
	 * to fix network issues, or selecting a different, non-network based voice.
	 */
	SPEECH_ERROR,
	
	/** A network call failed. */
	NETWORK_ERROR,
	
	/** A network call succeeded, but the server returned an error. */
	SERVER_ERROR,
	
	/**
	 * Network connectivity dropped before we were able to buffer the whole file we were streaming.
	 * So this is a kind of network error but breaking it out gives as a chance to show a more specific message.
	 */
	TIMED_OUT,
	
	/**
	 * Nothing is available to play.
	 */
	EMPTY_LIST,
	
	/**
	 * {@link android.media.MediaPlayer} failed without indicating anything helpful.
	 */
	MEDIA_PLAYER
}
