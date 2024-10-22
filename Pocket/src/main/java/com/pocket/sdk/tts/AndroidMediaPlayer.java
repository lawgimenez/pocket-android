package com.pocket.sdk.tts;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import androidx.annotation.Nullable;
import androidx.media.AudioAttributesCompat;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.AppThreads;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.util.java.Logs;
import com.pocket.util.prefs.FloatPreference;

import org.threeten.bp.Duration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

final class AndroidMediaPlayer implements StreamingPlayer {
	private final Subject<Player> currents = BehaviorSubject.create();
	private final Subject<Boolean> playingSubject = PublishSubject.create();
	private final Observable<Object> completions = currents.switchMap(Player::getCompletions).share();
	
	private final AudioAttributesCompat audioAttributes;
	
	private Player current;
	private Player next;
	
	AndroidMediaPlayer(Context context, AppThreads threads, ListenMediaSession android, float initialSpeed, FloatPreference lowestReportedFailingSpeed) {
		this.audioAttributes = android.getAudioAttributes();
		
		final AssetFileDescriptor silence = context.getResources().openRawResourceFd(R.raw.silence04s);
		setCurrent(new Player(silence, initialSpeed, lowestReportedFailingSpeed, threads));
		next = new Player(silence, initialSpeed, lowestReportedFailingSpeed, threads);
	}
	
	void load(UrlString url, ListenPlayer.OnLoaded loaded) {
		if (current.hasLoaded(url)) {
			loaded.onArticleLoaded();
			return;
		}
		
		if (next.hasLoaded(url)) {
			final Player previous = current;
			setCurrent(next);
			
			next = previous;
			next.reset();
			
			loaded.onArticleLoaded();
			return;
		}
		
		current.load(url, loaded, audioAttributes);
	}
	
	void preloadNext(UrlString url) {
		if (url == null) {
			next.reset();
			
		} else if (!next.hasLoaded(url)) {
			next.load(url, audioAttributes);
		}
	}
	
	@Override public void play() {
		current.start();
		playingSubject.onNext(true);
	}
	
	@Override public void pause() {
		current.pause();
		playingSubject.onNext(false);
	}
	
	@Override public void seekTo(Duration position) {
		current.seekTo((int) position.toMillis());
	}
	
	void reset() {
		playingSubject.onNext(false);
		current.reset();
	}
	
	void release() {
		playingSubject.onNext(false);
		current.release();
		current = null;
		
		next.release();
		next = null;
	}
	
	@Override public boolean isLoaded() {
		return current.isPrepared();
	}
	
	@Override public boolean isPlaying() {
		return current.isPlaying();
	}
	
	@Override public Duration getDuration() {
		return Duration.ofMillis(current.getDuration());
	}
	
	@Override public Duration getElapsed() {
		return Duration.ofMillis(current.getElapsed());
	}
	
	@Override public void setSpeed(float speed) {
		current.setSpeed(speed);
		next.setSpeed(speed);
	}
	
	@Override public void setPitch(float pitch) {
		current.setPitch(pitch);
		next.setPitch(pitch);
	}
	
	@Override public Observable<?> getProgressUpdates() {
		return getPlayingUpdates()
				.switchMap(isPlaying -> {
					if (isPlaying) {
						return Observable.interval(1, TimeUnit.SECONDS);
					} else {
						return Observable.empty();
					}
				});
	}
	
	@Override public Observable<Float> getBufferingUpdates() {
		return currents.switchMap(player -> player.getBufferingUpdates().startWith(player.getBufferingProgress()));
	}
	
	@Override public Observable<?> getCompletions() {
		return completions;
	}
	
	public Observable<Error> getErrors() {
		return currents.switchMap(player -> player.getErrors());
	}
	
	private void setCurrent(Player player) {
		current = player;
		currents.onNext(player);
	}
	
	private Observable<Boolean> getPlayingUpdates() {
		return Observable.merge(playingSubject,
				completions.map(__ -> false));
	}
	
	enum Error {
		IO(null, MediaPlayer.MEDIA_ERROR_IO),
		MALFORMED(null, MediaPlayer.MEDIA_ERROR_MALFORMED),
		UNSUPPORTED(null, MediaPlayer.MEDIA_ERROR_UNSUPPORTED),
		TIMED_OUT(null, MediaPlayer.MEDIA_ERROR_TIMED_OUT),
		SYSTEM(null, -2147483648), // MediaPlayer.MEDIA_ERROR_SYSTEM is hidden
		SERVER_DIED(MediaPlayer.MEDIA_ERROR_SERVER_DIED, null),
		UNKNOWN(MediaPlayer.MEDIA_ERROR_UNKNOWN, null);
		
		@Nullable final Integer what;
		@Nullable final Integer extra;
		
		static Error from(int what, int extra) {
			for (Error error : Error.values()) {
				if (error.extra != null && error.extra == extra ||
						error.what != null && error.what == what) {
					return error;
				}
			}
			return UNKNOWN;
		}
		
		Error(@Nullable Integer what, @Nullable Integer extra) {
			this.what = what;
			this.extra = extra;
		}
	}
	
	private static class Player {
		private final AssetFileDescriptor silenceFile;
		
		private final MediaPlayer player = new MediaPlayer();
		private final MediaPlayer silence = new MediaPlayer();
		/** Allows us to just use the app's thread pool rather than managing our own here. */
		private final AppThreads threads;
		private final CompositeDisposable subscriptions = new CompositeDisposable();
		private final BehaviorSubject<Float> bufferingUpdates = BehaviorSubject.create();
		private final Subject<Error> errors = PublishSubject.create();
		private final FloatPreference lowestReportedFailingSpeed;
		
		private volatile boolean prepared;
		private volatile UrlString loaded;
		/** Used to see if an async task should still run. */
		private AtomicInteger cancelId = new AtomicInteger();
		
		private float speed;
		
		private Player(AssetFileDescriptor silenceFile, float initialSpeed, FloatPreference lowestReportedFailingSpeed, AppThreads threads) {
			this.threads = threads;
			this.silenceFile = silenceFile;
			this.lowestReportedFailingSpeed = lowestReportedFailingSpeed;
			speed = initialSpeed;
			
			subscriptions.add(
					MediaPlayerObservable.fromBufferingUpdateListener(player)
							.map(percent -> ((float) percent) / 100)
							.subscribe(bufferingUpdates::onNext));
			
			MediaPlayerObservable.fromErrorListener(player).subscribe(errors);
			subscriptions.add(errors.subscribe(error -> reset()));
			
			player.setOnCompletionListener(__ -> silence.start());
		}
		
		void load(UrlString url, AudioAttributesCompat audioAttributes) {
			load(url, null, audioAttributes);
		}
		
		void load(UrlString url, @Nullable ListenPlayer.OnLoaded onLoaded, AudioAttributesCompat audioAttributes) {
			int id = cancelId.addAndGet(1);
			// TODO Can we safely interrupt the (often long) player.prepare() call if the pool is blocked on it?
			threads.async(() -> {
				if (cancelId.get() != id) return;
				
				reset();
				
				silence.setDataSource(silenceFile.getFileDescriptor(),
						silenceFile.getStartOffset(),
						silenceFile.getLength());
				silence.prepare();
				
				setAudioAttributes(player, audioAttributes);
				player.setDataSource(url.url);
				
				if (cancelId.get() != id) return;
				player.prepare();
				
				prepared = true;
				loaded = url;
				
				if (onLoaded != null && cancelId.get() == id) {
					onLoaded.onArticleLoaded();
				}
			}, e-> errors.onNext(Error.IO));
		}
		
		void start() {
			if (getElapsed() <= player.getDuration()) {
				updateSpeed(speed);
				if (!player.isPlaying()) {
					player.start();
				}
				
			} else {
				silence.start();
			}
		}
		
		void pause() {
			if (player.isPlaying()) {
				player.pause();
				
			} else if (silence.isPlaying()) {
				silence.pause();
			}
		}
		
		void seekTo(int position) {
			if (!isPrepared()) return;
			
			final boolean wasPlaying = isPlaying();
			
			if (position <= player.getDuration()) {
				if (silence.isPlaying()) silence.pause();
				silence.seekTo(0);
				
				player.seekTo(position);
				if (wasPlaying && !player.isPlaying()) player.start();
				
			} else {
				if (player.isPlaying()) player.pause();
				player.seekTo(player.getDuration());
				
				silence.seekTo(Math.min(position - player.getDuration(), silence.getDuration()));
				if (wasPlaying && !silence.isPlaying()) silence.start();
			}
		}
		
		/**
		 * Reset the player to initial state, you will have to
		 * call {@link #load(UrlString, ListenPlayer.OnLoaded, AudioAttributesCompat)} to use it again.
		 * <p>
		 * This is the method to call if you do want to use it again, just want to clean up after previous file has 
		 * been completed.
		 */
		void reset() {
			player.reset();
			silence.reset();
			prepared = false;
			loaded = null;
			bufferingUpdates.onNext(0f);
		}
		
		/**
		 * Release resources used by this player. The player becomes unusable afterwards and you will have to create
		 * a player if you want to do any more playback. Call it when you don't intend to play anything in the immediate
		 * future and what to clean up.
		 */
		void release() {
			prepared = false;
			loaded = null;
			silence.release();
			player.release();
			subscriptions.clear();
		}
		
		boolean isPrepared() {
			return prepared;
		}
		
		boolean hasLoaded(UrlString url) {
			return isPrepared() && url.equals(loaded);
		}
			
		boolean isPlaying() {
			return player.isPlaying() || silence.isPlaying();
		}
		
		long getDuration() {
			return player.getDuration() + silence.getDuration();
		}
		
		long getElapsed() {
			return player.getCurrentPosition() + silence.getCurrentPosition();
		}
		
		float getBufferingProgress() {
			final Float progress = bufferingUpdates.getValue();
			return progress != null ? progress : 0;
		}
		
		Observable<Float> getBufferingUpdates() {
			return bufferingUpdates.distinctUntilChanged();
		}
		
		Observable<Object> getCompletions() {
			return MediaPlayerObservable.fromCompletionListener(silence);
		}
		
		Observable<Error> getErrors() {
			return errors;
		}
		
		private static void setAudioAttributes(MediaPlayer player, AudioAttributesCompat audioAttributes) {
			player.setAudioAttributes((AudioAttributes) audioAttributes.unwrap());
		}
		
		void setSpeed(float speed) {
			if (player.isPlaying()) {
				// Only set it on the player if it's playing, otherwise it will start playing.
				updateSpeed(speed);
			}
			this.speed = speed;
		}
		
		/**
		 * Set speed on the {@link MediaPlayer}.
		 * <p>
		 * In most cases setting a non-0 speed will also start playback. Except on API 23 if 
		 * {@link MediaPlayer#start()} hasn't been called since {@link MediaPlayer#prepare()}. So be careful not to 
		 * start the player if you don't want it to, but also don't rely on it starting the player when you want it to.
		 */
		private void updateSpeed(float speed) {
			try {
				player.setPlaybackParams(player.getPlaybackParams().setSpeed(speed));
			} catch (IllegalArgumentException e) {
				// We're probably trying to set a speed value greater than the maximum this device supports.
				// We haven't found a good way to check what is the maximum supported speed.
				
				var app = App.getApp();
				if (app.mode().isForInternalCompanyOnly() && speed < lowestReportedFailingSpeed.get()) {
					app.errorReporter().reportError(new RuntimeException("Trying to set unsupported speed: " + speed, e));
					lowestReportedFailingSpeed.set(speed);
					
				} else {
					Logs.printStackTrace(e);
				}
			}
		}
		
		void setPitch(@SuppressWarnings("unused") float pitch) {
			throw new UnsupportedOperationException("Currently not needed, could be implemented similar to setSpeed.");
		}
	}
	
	private static class MediaPlayerObservable {
		private MediaPlayerObservable() {
			throw new AssertionError("No instances.");
		}
		
		static Observable<Integer> fromBufferingUpdateListener(MediaPlayer player) {
			return Observable.create(emitter -> {
				player.setOnBufferingUpdateListener((mp, percent) -> emitter.onNext(percent));
				
				emitter.setCancellable(() -> player.setOnBufferingUpdateListener(null));
			});
		}
		
		static Observable<Error> fromErrorListener(MediaPlayer player) {
			return Observable.create(emitter -> {
				player.setOnErrorListener((mp, what, extra) -> {
					emitter.onNext(Error.from(what, extra));
					return true;
				});
				
				emitter.setCancellable(() -> player.setOnErrorListener(null));
			});
		}
		
		static Observable<Object> fromCompletionListener(MediaPlayer player) {
			return Observable.create(emitter -> {
				player.setOnCompletionListener(emitter::onNext);
				
				emitter.setCancellable(() -> player.setOnCompletionListener(null));
			});
		}
	}
}
