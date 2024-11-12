package com.pocket.sdk.tts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.v4.media.MediaBrowserCompat;

import androidx.media.AudioAttributesCompat;
import io.reactivex.disposables.Disposable;

/**
 * All of the misc Android APIs needed to make media controls and playback play nice on Android
 * so we get media buttons, bluetooth, audio focus, ducking, volume controls, notification,
 * lock screen controls, etc.
 * <p>
 * Android LOVES changing these APIs constantly so trying to keep them contained in
 * one place and very little impact on how Listen works. Listen just needs to call
 * these public methods in the places that make the most sense.
 */
class ListenMediaSession {
	private static final AudioAttributesCompat AUDIO_ATTRIBUTES = new AudioAttributesCompat.Builder()
			.setUsage(AudioAttributesCompat.USAGE_MEDIA)
			.setContentType(AudioAttributesCompat.CONTENT_TYPE_SPEECH)
			.build();
	
	private final Context context;
	private final AudioFocus focus;
	private final Listen listen;
	private final AudioFocus.Control control;
	private final BecomingNoisyReceiver noisyReceiver = new BecomingNoisyReceiver();
	private MediaBrowserCompat browser;
	private Disposable subscription;
	private boolean isNoisyReceiverRegistered;
	
	ListenMediaSession(Context context,
			Listen listen,
			AudioFocus.Control control) {
		this.context = context;
		this.listen = listen;
		this.control = control;
		this.focus = AudioFocus.compat(context, AUDIO_ATTRIBUTES, control);
	}
	
	/**
	 * Invoke when Listen is active and should be visible and accessible to the user in all normal media control locations.
	 */
	public void startSession() {
		if (browser == null) {
			subscription = listen.states().subscribe(s -> setNoisyReceiverActive(s.playstate == PlayState.PLAYING));
			setNoisyReceiverActive(listen.state().playstate == PlayState.PLAYING);
			browser = new MediaBrowserCompat(context,
					new ComponentName(context, ListenMediaService.class),
					new MediaBrowserCompat.ConnectionCallback() {
						@Override
						public void onConnected() {}
						
						@Override
						public void onConnectionSuspended() {
							browser = null;
						}
						
						@Override
						public void onConnectionFailed() {
							browser = null;
						}
					},
					null);
			browser.connect();
		}
	}
	
	/**
	 * Invoke before attempting to make noise to see if you are allowed to.
	 * @return true if you can play media, false means remain paused.
	 */
	public boolean requestAudioFocus() {
		return focus.request();
	}
	
	/**
	 * Invoke when you are paused or stopped.
	 */
	public void abandonFocus() {
		focus.abandon();
	}
	
	/**
	 * Invoke when Listen is totally "off" and media controls don't need to be available any more.
	 */
	public void stopSession() {
		if (browser != null) {
			subscription.dispose();
			browser.disconnect();
			browser = null;
		}
		setNoisyReceiverActive(false);
	}
	
	public AudioAttributesCompat getAudioAttributes() {
		return AUDIO_ATTRIBUTES;
	}
	
	private void setNoisyReceiverActive(boolean active) {
		if (isNoisyReceiverRegistered != active) {
			if (active) {
				context.registerReceiver(noisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
				isNoisyReceiverRegistered = true;
			} else {
				context.unregisterReceiver(noisyReceiver);
				isNoisyReceiverRegistered = false;
			}
		}
	}
	
	private class BecomingNoisyReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
				control.pause();
			}
		}
	}
}
