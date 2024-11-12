package com.pocket.sdk.tts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.image.Image;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.DownloadAuthorization;
import com.pocket.ui.util.PlaceHolderBuilder;
import com.pocket.util.DisplayUtil;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.android.FormFactor;
import com.pocket.util.java.Logs;

import org.threeten.bp.Duration;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Handles all of the Android Media Session and Media Buttons on behalf of {@link Listen}.
 * Also the {@link ListenNotification}.
 * Start and stop is controlled by {@link ListenMediaSession}.
 */
public class ListenMediaService extends MediaBrowserServiceCompat {
	private static final Duration SEEK_DURATION = Duration.ofSeconds(15);
	private static final int ALBUM_ART_SIZE = FormFactor.dpToPx(300);
	private static final long BASE_CAPABILITIES = PlaybackStateCompat.ACTION_PLAY_PAUSE
			| PlaybackStateCompat.ACTION_PAUSE
			| PlaybackStateCompat.ACTION_PLAY
			| PlaybackStateCompat.ACTION_FAST_FORWARD
			| PlaybackStateCompat.ACTION_REWIND
			| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
			| PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
			| PlaybackStateCompat.ACTION_STOP;

	private final PlaybackStateCompat.Builder playstate = new PlaybackStateCompat.Builder();
	private final MediaMetadataCompat.Builder metadata = new MediaMetadataCompat.Builder();

	private MediaSessionCompat session;
	private ListenNotification notification;
	private Disposable subscription;

	private Track track;
	private String thumbnail;

	@Override
	public void onCreate() {
		super.onCreate();
		// WARNING: This is an exported service. Extras could come from outside apps and may not
		// be trust worthy.
		App app = App.from(this);
		final Controls controls = app.listen().trackedControls(() -> {
			final ListenState state = app.listen().state();
			return new ActionContext.Builder()
					.cxt_view(CxtView.LISTEN)
					.cxt_ui(CxtUi.BACKGROUND)
					.cxt_index(state.index + 1)
					.cxt_progress(state.getProgressPercent())
					.build();
		});

		session = new MediaSessionCompat(this, getString(R.string.nm_app));
		session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
				MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
		session.setPlaybackState(playstate.build());
		session.setCallback(new MediaSessionCompat.Callback() {
			@Override
			public void onPlay() {
				if (session.isActive()) controls.play();
			}

			@Override
			public void onPause() {
				controls.pause();
			}

			@Override
			public void onSkipToNext() {
				controls.next();
			}

			@Override
			public void onSkipToPrevious() {
				controls.previous();
			}

			@Override
			public void onFastForward() {
				controls.seekTo(app.listen().state().elapsed.plus(SEEK_DURATION));
			}

			@Override
			public void onRewind() {
				controls.seekTo(app.listen().state().elapsed.minus(SEEK_DURATION));
			}

			@Override
			public void onSeekTo(long pos) {
				controls.seekTo(Duration.ofMillis(pos));
			}

			@Override
			public void onStop() {
				controls.off();
			}
		});
		final MediaSessionCompat.Token sessionToken = session.getSessionToken();
		notification = new ListenNotification(
				this,
				sessionToken,
				app.listen(),
				app.threads(),
				app.notifications()
		);
		setSessionToken(sessionToken);

		subscription =
				app.listen().states().startWith(app.listen().state()).subscribe(this::publishState);
	}

	private void setSessionActive(boolean active) {
		if (session.isActive() == active) {
			// No change
		} else if (active) {
			session.setActive(true);
			if (ApiLevel.isOreoOrGreater()) {
				fixAndroidOreo();
			}
		} else {
			session.setActive(false);
		}
	}

	/**
	 * Horrible ugly hack needed to make Media Buttons work on Android O.
	 * Essentially we need to play a sound locally to make them think the session is alive.
	 * TTS is not enough.
	 * Hopefully they will fix and this can be removed.
	 */
	private void fixAndroidOreo() {
		try {
			final MediaPlayer mediaPlayer;
			mediaPlayer = MediaPlayer.create(this, R.raw.silence);
			mediaPlayer.setOnCompletionListener(MediaPlayer::release);
			mediaPlayer.start();
		} catch (Throwable throwable) {
			// Not expecting, but let's not blow up the app. Media buttons just might not work.
			Logs.printStackTrace(throwable);
		}
	}

	private void publishState(ListenState state) {
		// TODO notification state publishing can be tweaked later, see refactor notes on that
		//  class.
		setSessionActive(state.playstate != PlayState.STOPPED);

		final boolean showProgress =
				state.supportedFeatures.contains(ListenState.Feature.ACCURATE_DURATION_AND_ELAPSED);
		long capabilities = BASE_CAPABILITIES;
		if (showProgress) {
			capabilities |= PlaybackStateCompat.ACTION_SEEK_TO;
		}
		playstate.setActions(capabilities);

		int compatState = toCompatPlaystate(state.playstate);
		playstate.setState(compatState, state.elapsed.toMillis(), state.speed);
		session.setPlaybackState(playstate.build());

		if (state.current != null && state.playstate != PlayState.STOPPED) {
			final Track oldTrack = track;
			track = state.current;
			String album = DisplayUtil.displayHost(track.displayUrl);
			String artist = !track.authors.isEmpty() ? track.authors.get(0).name : null;
			String title = track.displayTitle;
			String url = track.displayUrl;
			final long duration = state.duration.toMillis();

			metadata
					.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
					.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
					.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
					.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, url);

			if (showProgress) {
				metadata.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);
			}

			String oldThumbnail = thumbnail;
			thumbnail = track.displayThumbnailUrl;

			if (thumbnail == null) {
				if (!track.equals(oldTrack)) {
					final Drawable drawable =
							PlaceHolderBuilder.getDrawable(this,
									track.idUrl,
									track.displayTitle.charAt(0));
					final Bitmap bitmap = Bitmap.createBitmap(ALBUM_ART_SIZE,
							ALBUM_ART_SIZE,
							Bitmap.Config.ARGB_8888
					);
					final Canvas canvas = new Canvas(bitmap);
					drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
					drawable.draw(canvas);
					metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap);
				}
			} else if (!thumbnail.equals(oldThumbnail)) {
				metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, null);
				String loading = thumbnail;
				Image.build(loading, AssetUser.forSession())
						.setDownloadAuthorization(DownloadAuthorization.ALWAYS)
						.setCanceller(info -> loading.equals(thumbnail))
						.fill(ALBUM_ART_SIZE, ALBUM_ART_SIZE)
						.getAsync((request, wrapper, result) -> {
									if (wrapper != null) {
										if (!wrapper.hasValidBitmap()) {
											wrapper.setBeingUsed(false);
											wrapper = null;
										}
									}
									if (wrapper != null) {
										// Copy the bitmap so we don't have to use the cached
										// version, since we can't control its lifecycle remotely
										Bitmap bitmap = wrapper.getBitmap()
												.copy(wrapper.getBitmap().getConfig(), false);
										wrapper.setBeingUsed(false);
										metadata.putBitmap(
												MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
												bitmap
										);
										session.setMetadata(metadata.build());
										notification.update(playstate.build().getState(),
												metadata.build());
									}
								}
						);
			}
		}

		session.setMetadata(metadata.build());
		notification.update(compatState, metadata.build());
	}

	private int toCompatPlaystate(PlayState playstate) {
		switch (playstate) {
			case ERROR: return PlaybackStateCompat.STATE_ERROR;
			case PAUSED: return PlaybackStateCompat.STATE_PAUSED;
			case STOPPED: return PlaybackStateCompat.STATE_STOPPED;

			case STARTING:
			case BUFFERING:
				return PlaybackStateCompat.STATE_BUFFERING;

			case PLAYING:
			case PAUSED_TRANSIENTLY:
				return PlaybackStateCompat.STATE_PLAYING;
		}
		throw new RuntimeException("unknown state " + playstate);
	}

	@Override
	public @Nullable BrowserRoot onGetRoot(
			@NonNull String clientPackageName,
			int clientUid,
			@Nullable Bundle bundle
	) {
		return bundle == null || !bundle.getBoolean(BrowserRoot.EXTRA_RECENT)
				? new BrowserRoot(getString(R.string.nm_app), null)
				: null;
	}

	@Override
	public void onLoadChildren(
			@NonNull String parentMediaId,
			@NonNull Result<List<MediaBrowserCompat.MediaItem>> result
	) {
		result.sendResult(new ArrayList<>()); // Despite the Android docs, examples and medium posts, it does appear sending null can cause crashes https://android.googlesource.com/platform/frameworks/base/+/3625bf7/media/java/android/service/media/MediaBrowserService.java#420
		// and the docs of onLoadChildren say to return an empty list.
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		subscription.dispose();
		session.release();
		notification.unregister();
	}
}
