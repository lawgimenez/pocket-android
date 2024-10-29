package com.pocket.sdk.tts;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import com.ideashower.readitlater.R;
import com.pocket.app.AppThreads;
import com.pocket.sdk.notification.SystemNotifications;
import com.pocket.sdk.util.DeepLinks;
import com.pocket.util.android.PendingIntentUtils;

import org.apache.commons.lang3.StringUtils;

/**
 * Controls showing a notification that shows the current state and controls of {@link Listen} and the {@link TTSPlayer}.
 */
public class ListenNotification {
	
	private static final int NOTIFICATION_ID = 424242;
	
	private final ListenMediaService service;
	private final MediaSessionCompat.Token sessionToken;
	private final Listen listen;
	private final AppThreads threads;
	private final SystemNotifications notifications;
	
	private Notification notification;
	private PendingIntent pendingIntent;
	private long startTime;
	
	ListenNotification(ListenMediaService service,
			MediaSessionCompat.Token sessionToken,
			Listen listen,
			AppThreads threads,
			SystemNotifications notifications) {
		this.service = service;
		this.sessionToken = sessionToken;
		this.listen = listen;
		this.threads = threads;
		this.notifications = notifications;
	}
	
	void update(int playState, MediaMetadataCompat metadata) {
		threads.runOrPostOnUiThread(() -> {
			Track current = listen.state().current;
			if (current != null && playState != PlaybackStateCompat.STATE_STOPPED && metadata != null) {
				if (pendingIntent == null) {
					startTime = System.currentTimeMillis();
				}
				Intent intent = DeepLinks.newListenIntent(current.idUrl, service);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				pendingIntent = PendingIntent.getActivity(
						service,
						0,
						intent,
						PendingIntentUtils.addMutableFlag(PendingIntent.FLAG_UPDATE_CURRENT)
				);

				try {
					notification = newNotification(playState, metadata);
					
					if (isPlaying(playState)) {
						service.startForeground(NOTIFICATION_ID, notification);
						
					} else {
						NotificationManagerCompat.from(service).notify(NOTIFICATION_ID, notification);
						service.stopForeground(false);
					}
				} catch (Exception e) {
					// We've seen null pointer exceptions in framework notifications code on some devices.
					// https://appcenter.ms/orgs/pocket-app/apps/Android-Production-Google-Play-Amazon-App-Store/crashes/errors/1737503110u/overview
					
					// Let's make sure we fail gracefully.
					unregister();
				}
				
			} else if (notification != null) {
				unregister();
			}
		});
	}
	
	private Notification newNotification(int playState, MediaMetadataCompat metadata) {
		final String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
		String artistAlbum = StringUtils.defaultString(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST), "");
		if (!StringUtils.isEmpty(artistAlbum)) {
			artistAlbum = artistAlbum + " — ";
		}
		artistAlbum = artistAlbum + metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
		
		final PendingIntent previous = newMediaButtonIntent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);
		final PendingIntent playPause = newMediaButtonIntent(PlaybackStateCompat.ACTION_PLAY_PAUSE);
		final PendingIntent close = newMediaButtonIntent(PlaybackStateCompat.ACTION_STOP);
		final PendingIntent next = newMediaButtonIntent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
		
		final Bitmap largeIcon = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART);
		
		final int pauseIcon = com.pocket.ui.R.drawable.ic_pkt_pause_solid;
		final int playIcon = com.pocket.ui.R.drawable.ic_pkt_play_solid;
		final int prevIcon = com.pocket.ui.R.drawable.ic_pkt_prev_line;
		final int nextIcon = com.pocket.ui.R.drawable.ic_pkt_next_line;
		final int playPauseIcon = isPlaying(playState) ? pauseIcon : playIcon;
		final CharSequence playPauseText = service.getText(isPlaying(playState) ? com.pocket.ui.R.string.ic_pause : com.pocket.ui.R.string.ic_play);
		
		return notifications.newDefaultBuilder()
				.setOnlyAlertOnce(true)
				.setSmallIcon(R.drawable.ic_stat_notify)
				.setContentIntent(pendingIntent)
				.setDeleteIntent(close)
				.setWhen(startTime)
				.setContentTitle(title)
				.setContentText(artistAlbum)
				.setLargeIcon(largeIcon)
				.addAction(prevIcon, service.getText(com.pocket.ui.R.string.ac_previous), previous)
				.addAction(playPauseIcon, playPauseText, playPause)
				.addAction(nextIcon, service.getText(R.string.ac_next), next)
				.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
						.setMediaSession(sessionToken)
						.setShowActionsInCompactView(0, 1, 2)
						.setShowCancelButton(true)
						.setCancelButtonIntent(close))
				.setColor(NotificationCompat.COLOR_DEFAULT) // reset color set by SystemNotifications
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
				.build();
	}
	
	private boolean isPlaying(int playState) {
		return playState == PlaybackStateCompat.STATE_PLAYING || playState == PlaybackStateCompat.STATE_BUFFERING;
	}

	private PendingIntent newMediaButtonIntent(Long action) {
		try {
			return MediaButtonReceiver.buildMediaButtonPendingIntent(service, action);
		} catch (RuntimeException e) {
			// We've seen "java.lang.RuntimeException: Package manager has died" here.
			// Ignore it instead of crashing.
			return null;
		}
	}
	
	void unregister() {
		service.stopForeground(true);
		NotificationManagerCompat.from(service).cancel(NOTIFICATION_ID);
		notification = null;
		pendingIntent = null;
	}
}
