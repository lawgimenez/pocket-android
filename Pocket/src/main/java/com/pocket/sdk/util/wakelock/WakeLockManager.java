package com.pocket.sdk.util.wakelock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.sdk.dev.ErrorHandler;
import com.pocket.util.java.Clock;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Milliseconds;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages acquiring and releasing wakelocks in a way that encourages good behaviour,
 * but can catch when a wake lock is accidentally held too long and release it before
 * it causes problems for the user, earns us bad reviews or "bad behaviour" on Google Play's vitals.
 * <p>
 * It also catches cases where locks are held too long and reports them to us proactively,
 * with a bunch of helpful debugging info included, rather than us having to wait for users to
 * report it or us having to figure out some way to debug it.
 * <p>
 * All wake locks in the Pocket app should use this class to benefit from this!
 * <p>
 * If this class is successful, our users will have happy batteries,
 * we'll catch wake lock bugs earlier and fix them easily and we'll have
 * low or no sessions with long held locks in Google Play vitals.
 * <p>
 * Use {@link #acquire(WakeLockHolder)} to obtain a wakelock and {@link #release(WakeLockHolder)} when you are done.
 * <p>
 * Implementation Notes:
 * <ul>
 * <li>
 * All locks here are {@link PowerManager#PARTIAL_WAKE_LOCK}.
 * </li>
 * <li>
 * Under the hood, we don't really create a real actual wakelock until the app goes into the background.
 * While the app is open, it is likely going to rapidly go through tasks that call acquire/release,
 * So only hitting the power manager when it matters (in the background) helps reduce unneeded work.
 * </li>
 * <li>
 * Internally, after you request a wakelock to be released, we'll actually hold it for a few seconds,
 * this gives tasks a very small window to launch the next one without losing power.
 * </li>
 * </ul>
 */
@Singleton
public class WakeLockManager implements AppLifecycle {
	
	static final boolean DEBUG = BuildConfig.DEBUG && false;
	static final boolean REPORT_WARNINGS = false;
	static final boolean REPORT_TIMEOUTS = false;
	
	/**
	 * To help smooth out locks that might quickly acquire and release rapidly (like task pools),
	 * Locks will wait this number of seconds, after being requested to release, before internally releasing
	 * the actual framework wake lock.
	 * This also helps reduce rapid starting and stopping of the {@link WakefulAppService}, WakeLocks
	 * and also gives tasks a short buffer of time to trigger another task that might need a lock.
	 */
	private static final int RELEASE_BUFFER = 5;
	
	/**
	 * Currently held locks.
	 */
	private final Map<WakeLockHolder, Lock> locks = new HashMap<>();
	
	/**
	 * Holds a lock while the app is in the foreground.
	 * Mostly a simple way to provide the app at least {@link #RELEASE_BUFFER} seconds
	 * to obtain locks after going into the background. This was functionality
	 * provided in older versions of this class. I think the idea is that somethings
	 * that trigger asynchronously during onPause might not acquire their lock during onPause
	 * so this gives them a small window to do so.
	 * REVIEW whether or not we really need this buffer. How quickly does Android shut us down after onPause and onStop anyways?
	 */
	private final WakeLockHolder foregroundHolder = WakeLockHolder.withTimeout("app", 0, 1, null);
	
	private final Clock clock = Clock.ELAPSED_REALTIME;
	private final ErrorHandler errorReporter;
	private final Handler handler;
	private final PowerManager power;
	private boolean isHoldingLocks;
	private boolean isInBackground = true;
	private Listener listener;

	@Inject
	public WakeLockManager(
			ErrorHandler errorReporter,
			@ApplicationContext Context context,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.errorReporter = errorReporter;
		this.power = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
		this.handler = new Handler(Looper.getMainLooper());
	}
	
	/**
	 * Acquires a wake lock and holds it until {@link  #release(WakeLockHolder)}
	 */
	public synchronized void acquire(WakeLockHolder holder) {
		Lock lock = locks.get(holder);
		if (lock != null) {
			// Already exists
			// Can keep existing state unless it is in a release buffer, for that, reactivate it.
			if (lock.state == LockState.RELEASE_BUFFER) {
				if (DEBUG) log("reactivate", holder);
				lock.cancelBuffer();
				lock.activate();
			}
			
		} else {
			// Create and activate
			if (DEBUG) log("create", holder);
			lock = new Lock(holder);
			locks.put(holder, lock);
			lock.activate();
			
			invalidateListener();
		}
	}
	
	/**
	 * Release a {@link WakeLockHolder}'s lock.
	 * It is safe to call this even if you already have released it.
	 * It will just ignore duplicate or redundant calls.
	 */
	public synchronized void release(WakeLockHolder holder) {
		Lock lock = locks.get(holder);
		if (lock != null) {
			if (DEBUG) log("release", holder);
			
			// Release it after the buffer time
			lock.bufferRelease(() -> {
				synchronized (WakeLockManager.this) {
					if (DEBUG) log("released after buffer", holder);
					lock.release();
					locks.remove(holder);
					invalidateListener();
				}
			});
		}
	}
	
	/**
	 * A listener for when there are active holds.
	 */
	public synchronized void setListener(Listener listener) {
		this.listener = listener;
	}
	
	public synchronized boolean hasLocks() {
		return isHoldingLocks;
	}
	
	public interface Listener {
		void onWakeLockStateChanged(boolean isLocked);
	}
	
	@Override
	public void onUserPresent() {
		synchronized (this) {
			if (DEBUG) log("foreground");
			
			this.isInBackground = false;
			for (Lock lock : locks.values()) {
				lock.foreground();
			}
			acquire(foregroundHolder);
		}
	}
	
	@Override
	public void onUserGone(Context context) {
		synchronized (this) {
			if (DEBUG) log("background");
			
			this.isInBackground = true;
			long now = clock.now();
			for (Lock lock : locks.values()) {
				lock.background(now);
			}
			release(foregroundHolder);
		}
	}
	
	private synchronized void invalidateListener() {
		boolean hasLocks = !locks.isEmpty();
		if (hasLocks != isHoldingLocks) {
			isHoldingLocks = hasLocks;
			if (DEBUG) log(isHoldingLocks ? "HAS LOCKS" : "NO LOCKS");
			if (listener != null) {
				// TODO cycles pretty quickly, maybe have a timeout on this  one?
				listener.onWakeLockStateChanged(isHoldingLocks);
			}
		}
	}
	
	private enum LockState {
		/**
		 * While the app is in the foreground, we note that a wakelock is requested,
		 * but don't actually hold on.
		 */
		FOREGROUND,
		/**
		 * While the app is in the background, we hold a wake lock.
		 */
		BACKGROUND,
		/**
		 * The lock was requested to be released, but we are waiting {@link #RELEASE_BUFFER} seconds
		 * to see if it reactivates before we unlock and release it.
		 */
		RELEASE_BUFFER,
		/**
		 * The lock is unlocked and fully released.
		 */
		RELEASED
	}
	
	private class Lock {
		final WakeLockHolder holder;
		final long created;
		
		LockState state;
		
		PowerManager.WakeLock lock;
		long timeBackgrounded;
		
		Runnable warning;
		Runnable timeout;
		Runnable releaseBuffer;
		Runnable liveliness;
		
		Lock(WakeLockHolder holder) {
			this.holder = holder;
			this.created = clock.now();
		}
		
		/**
		 * Set it into the correct state based on whether the app is currently foreground or background.
		 */
		void activate() {
			if (isInBackground) {
				background(timeBackgrounded > 0 ? timeBackgrounded : created);
			} else {
				foreground();
			}
		}
		
		/**
		 * Set lock into foreground mode.
		 */
		void foreground() {
			state = LockState.FOREGROUND;
			timeBackgrounded = 0;
			clearTimeouts();
			unlock();
		}
		
		/**
		 * Set lock into background mode.
		 */
		
		void background(long start) {
			state = LockState.BACKGROUND;
			timeBackgrounded = start;
			lock();
			
			// Setup fail safes
			if (holder.stopTimeout > 0) {
				// Timeout Based
				timeout = this::timeout;
				handler.postDelayed(timeout, Milliseconds.minutesToMillis(holder.stopTimeout));
				
				if (holder.warnTimeout > 0 && REPORT_WARNINGS) {
					warning = () -> {
						synchronized (WakeLockManager.this) {
							if (DEBUG) log("warn", holder);
							errorReporter.reportError(WakeLockException.newException("warning", holder, this, clock.now()));
						}
					};
					handler.postDelayed(warning, Milliseconds.minutesToMillis(holder.warnTimeout));
				}
				
			} else if (holder.check != null) {
				// Liveliness Based
				liveliness = () -> {
					synchronized (WakeLockManager.this) {
						if (DEBUG) log("liveliness", holder);
						if (holder.check.keepAlive()) {
							handler.postDelayed(liveliness, Milliseconds.minutesToMillis(holder.checkInterval));
						} else {
							timeout();
						}
					}
				};
				handler.postDelayed(liveliness, Milliseconds.minutesToMillis(holder.checkInterval));
			}
		}
		
		/**
		 * The lock timed out, quietly log an error and release it.
		 */
		private void timeout() {
			synchronized (WakeLockManager.this) {
				if (DEBUG) log("timeout", holder);
				if (REPORT_TIMEOUTS) {
					errorReporter.reportError(WakeLockException.newException("timeout", holder, this, clock.now()));
				}
				release();
				locks.remove(holder);
			}
		}
		
		/**
		 * Release after {@link #RELEASE_BUFFER} seconds.
		 */
		void bufferRelease(Runnable afterBuffer) {
			state = LockState.RELEASE_BUFFER;
			if (releaseBuffer == null) {
				releaseBuffer = afterBuffer;
				clearTimeouts();
				handler.postDelayed(releaseBuffer, Milliseconds.seconds(RELEASE_BUFFER));
			}
		}
		
		/**
		 * Cancel releasing started by {@link #bufferRelease(Runnable)}.
		 */
		void cancelBuffer() {
			if (releaseBuffer != null) {
				handler.removeCallbacks(releaseBuffer);
				releaseBuffer = null;
			}
		}
		
		/**
		 * Fully release/unlock the lock.
		 * Locks are not intended to be reused after this point.
		 */
		void release() {
			state = LockState.RELEASED;
			unlock();
		}
		
		/**
		 * Cancel all future timeouts or liveliness checks.
		 */
		void clearTimeouts() {
			if (warning != null) {
				handler.removeCallbacks(warning);
				warning = null;
			}
			if (timeout != null) {
				handler.removeCallbacks(timeout);
				timeout = null;
			}
			if (liveliness != null) {
				handler.removeCallbacks(liveliness);
				liveliness = null;
			}
		}
		
		/**
		 * Actually obtain the real wake lock.
		 */
		private void lock() {
			if (lock == null) {
				if (DEBUG) log("lock", holder);
				lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, holder.name);
				lock.acquire();
			}
		}
		
		/**
		 * Actually release the real wake lock.
		 */
		private void unlock() {
			if (lock != null) {
				if (DEBUG) log("unlock", holder);
				lock.release();
				lock = null;
			}
		}
		
	}
	
	static class WakeLockException extends RuntimeException {
		
		private static WakeLockException newException(String type, WakeLockHolder holder, Lock lock, long now) {
			String details = type;
			details += holder.name;
			details += " cr:" + Milliseconds.toSeconds(now - lock.created); // Seconds since first created
			details += " bg:" + Milliseconds.toSeconds(now - lock.timeBackgrounded); // Seconds in background
			details += holder.onTimeout != null ? StringUtils.defaultIfBlank(holder.onTimeout.onTimedOut(), "") : "";
			return new WakeLockException(details);
		}
		
		WakeLockException(String details) {
			super(details);
		}
		
	}
	
	static void log(String log) {
		log(log, null);
	}
	
	private static void log(String log, WakeLockHolder holder) {
		if (DEBUG) {
			Logs.v("WakeLockManager", log + " " + (holder != null ? "~" + holder.name : ""));
		}
	}
	
	
}
