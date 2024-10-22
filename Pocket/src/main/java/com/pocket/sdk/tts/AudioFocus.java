package com.pocket.sdk.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.media.AudioAttributesCompat;

/**
 * Helpers for managing Audio Focus. Use {@link #compat(Context, AudioAttributesCompat, Control)} to get an instance
 * for the current API level.
 */
class AudioFocus implements AudioManager.OnAudioFocusChangeListener {
	
	public static AudioFocus compat(Context context,
			AudioAttributesCompat audioAttributes,
			Control control) {
		if (Build.VERSION.SDK_INT >= 26) {
			//noinspection ConstantConditions
			return new AudioFocus26(context, audioAttributes, control);
		} else {
			return new AudioFocus(context, audioAttributes.getLegacyStreamType(), control);
		}
	}
	
	
	private final Object lock = new Object();
	
	private final AudioManager manager;
	private final int streamType;
	private final Control control;
	private boolean resumeOnFocusGain;
	private boolean isPlaybackDelayed;
	
	public AudioFocus(Context context, int streamType, Control control) {
		this.manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		this.streamType = streamType;
		this.control = control;
	}
	
	public boolean request() {
		int res = requestFocus();
		synchronized (lock) {
			isPlaybackDelayed = false;
			if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
				return false;
			} else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				return true;
			} else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
				isPlaybackDelayed = true;
				return false;
			} else {
				return false;
			}
		}
	}
	
	/** Internal use only. Use {@link #request()} */
	protected int requestFocus() {
		return manager.requestAudioFocus(this, streamType, AudioManager.AUDIOFOCUS_GAIN);
	}
	
	public void abandon() {
		manager.abandonAudioFocus(this);
	}
	
	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
			case AudioManager.AUDIOFOCUS_GAIN:
				if (isPlaybackDelayed || resumeOnFocusGain) {
					synchronized (lock) {
						isPlaybackDelayed = false;
						resumeOnFocusGain = false;
					}
					control.play();
				}
				break;
			case AudioManager.AUDIOFOCUS_LOSS:
				synchronized (lock) {
					resumeOnFocusGain = false;
					isPlaybackDelayed = false;
				}
				control.pause();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				synchronized (lock) {
					resumeOnFocusGain = true;
					isPlaybackDelayed = false;
				}
				control.pauseTransiently();
				break;
		}
	}
	
	/** Simple controls to allow ducking to start and stop playback and also for delayed focus to be granted and playback to start later. */
	interface Control {
		void play();
		void pause();
		void pauseTransiently();
	}
	
	/**
	 * For Android 26 and above.
	 * NOTE: This has specific settings to Listen. If using for something else, you will want to modify the attributes.
	 */
	@RequiresApi(api = 26)
	private static class AudioFocus26 extends AudioFocus {
		
		private final AudioFocusRequest request;
		private final AudioManager manager;
		
		public AudioFocus26(Context context, AudioAttributesCompat compatAttributes, Control control) {
			super(context, compatAttributes.getLegacyStreamType(), control);
			
			final AudioAttributes audioAttributes = (AudioAttributes) compatAttributes.unwrap();
			if (audioAttributes == null) {
				throw new AssertionError("On API26+ we should be able to unwrap AudioAttributesCompat");
			}
			
			this.manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
					.setAudioAttributes(audioAttributes)
					.setAcceptsDelayedFocusGain(true)
					.setOnAudioFocusChangeListener(this, new Handler(Looper.getMainLooper()))
					.setWillPauseWhenDucked(true)
					.build();
		}
		
		@Override
		protected int requestFocus() {
			return manager.requestAudioFocus(request);
		}
		
		@Override
		public void abandon() {
			manager.abandonAudioFocusRequest(request);
		}
		
	}
}
