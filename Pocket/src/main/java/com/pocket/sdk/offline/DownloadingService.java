package com.pocket.sdk.offline;

import android.annotation.SuppressLint;
import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.WorkManager;

import com.ideashower.readitlater.BuildConfig;
import com.ideashower.readitlater.R;
import com.pocket.app.AppThreads;
import com.pocket.sdk.api.AppSync;
import com.pocket.sdk.notification.SystemNotifications;
import com.pocket.util.android.PendingIntentUtils;
import com.pocket.util.java.Logs;
import com.squareup.phrase.Phrase;

/**
 * A service that runs while {@link OfflineDownloading} is active and publishes a progress notification to the user.
 * To setup, call {@link #initialize(Context, AppThreads, OfflineDownloading, SystemNotifications)}
 * <p>
 * The design of this tries to do the notification updates as efficiently as possible.
 * It will only start the service once per downloading session, rather than every time the notification updates.
 * It passes count updates directly to the service instance via a method, rather than having to go through intents/extras etc.
 * It caches a notification builder and the Phrase object to try to avoid resource allocation and loading on each update.
 * It uses startForeground when first showing the notification, but then switches to the notification manager for future updates.
 * <p>
 * It also tries to battle the dreaded "android.app.RemoteServiceException: Context.startForegroundService() did not then call Service.startForeground()"
 * which seems to happen on some OSs. Some resources related to this: https://issuetracker.google.com/issues/76112072  https://proandroiddev.com/pitfalls-of-a-foreground-service-lifecycle-59f014c6a125
 * <p>
 * <h2>Refactor notes from Marcin</h2>
 * Personally I find the way this works a little confusing and frankly.. backwards. This service
 * isn't doing or starting any work. The work is done by {@link OfflineDownloading} and is started
 * either internally by observing changes in the sync engine or externally by components like
 * {@link AppSync}.
 * <p>
 * For me it would make sense to call this service whenever we want to start the work, to make sure
 * the service upgrades process priority in the OS for the whole lifecycle of the background work.
 * In absence of that it would make sense if the component doing the work directly called and managed
 * the service to protects the work.
 * <p>
 * Instead there's an added layer of indirection. {@link OfflineDownloading} only exposes a listener
 * and this service observes this listener to know when to start, stop or update the notification.
 * I think this might be due to historical reasons. Historically I think we had a class called
 * {@code DownloadingNotification} and it used the same listener pattern to publish the state of
 * offline downloading as a notification. When one of the Android versions started requiring running
 * a foreground service, we upgraded our existing notification to a foreground service notification.
 * <p>
 * For some time now—and especially with new restrictions on background work (including foreground
 * services) in Android 12—the recommended way to run background work is with {@link WorkManager}.
 * Work Manager can use a foreground service to run the work if we configure it to do so. But
 * I think the proper way to do this is to migrate {@link OfflineDownloading} to Work Manager,
 * possibly completely replacing its custom thread pools with Work Manager workers.
 * <p>
 * The above seems like quite a lot of work across implementation and testing, just to maintain
 * a feature that works (although possibly less reliably on Android 12 and up), but it's unclear
 * how important it is to users or how widely used it is. Especially since the restrictions only
 * kick in if you save from another device and we kick off the sync in the background. When you
 * save on your Android device or when you trigger a sync from My List, we can still start our
 * foreground service, displaying the downloading progress to the user and upgrading process
 * priority.
 */
public final class DownloadingService extends Service {
	
	private static final int ID = 42;
	private static final String ACTION_CANCEL = "com.pocket.action.CANCEL_DOWNLOADING";
	private static final boolean DEBUG = false && BuildConfig.DEBUG;
	
	@SuppressLint("StaticFieldLeak") // Given how we are using this as basically an app component it shouldn't cause a memory leak to hold the app context.
	private static Listener LISTENER;
	
	/** The running status of the service. */
	enum Status {
		OFF,
		STARTING,
		RUNNING
	}
	
	static class Listener implements OfflineDownloading.OnDownloadStateChangedListener {

		int remaining;
		int completed;

		private final Context context;
		private final AppThreads threads;
		private final OfflineDownloading offline;
		private final NotificationCompat.Builder builder;
		private final Phrase text;

		private Status status = Status.OFF;
		private DownloadingService instance;
		private long started;

		Listener(Context context,
				AppThreads threads,
				OfflineDownloading offline,
				SystemNotifications notifications) {
			this.context = context;
			this.threads = threads;
			this.offline = offline;
			offline.addSessionListener(this);

			builder = notifications.newDefaultBuilder()
					.setTicker(context.getString(R.string.nt_downloading))
					.setSmallIcon(R.drawable.ic_stat_notify)
					.setOngoing(true)
					.setOnlyAlertOnce(true)
					.setColor(ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_coral_2))
					.setContentText(context.getString(R.string.nt_cancel))
					.setContentIntent(
							PendingIntent.getService(
									context,
									0,
									new Intent(context, DownloadingService.class).setAction(ACTION_CANCEL),
									PendingIntentUtils.addMutableFlag(PendingIntent.FLAG_CANCEL_CURRENT)
							)
					)
					.setAutoCancel(true);
			text = Phrase.from(context, R.string.lb_downloading_items_notification);
		}
		
		/** Run all work through here to ensure always invoking on the ui thread. */
		private void run(Runnable r) {
			threads.runOrPostOnUiThread(r);
		}
		
		@Override
		public void onDownloadStateChange(OfflineDownloading offline) {
			run(() -> {
				this.remaining = offline.predownloadingCount();
				this.completed = offline.predownloadedCount();
				if (remaining > 0) {
					if (status == Status.OFF) {
						if (DEBUG) log("starting");
						started = System.currentTimeMillis();
						status = Status.STARTING;
						startService();
					} else if (instance != null) {
						if (DEBUG) log("updating");
						NotificationManagerCompat.from(context)
								.notify(ID, buildNotification(remaining, completed));
					}
				} else {
					if (DEBUG) log("stopping");
					stop();
				}
			});
		}

		private void startService() {
			Intent intent = new Intent(context, DownloadingService.class);

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
				ContextCompat.startForegroundService(context, intent);

			} else {
				try {
					ContextCompat.startForegroundService(context, intent);
				} catch (ForegroundServiceStartNotAllowedException ignored) {
					// Just post the notification without starting the service.
					status = Status.OFF;
					NotificationManagerCompat.from(context)
							.notify(ID, buildNotification(remaining, completed));
				}
			}
		}

		/** Invoke from the service once during its lifetime, as it starts the first time. */
		void started(DownloadingService service) {
			run(() -> {
				instance = service;
				status = Status.RUNNING;
			});
		}
		
		/** Stops the service. */
		void stop() {
			run(() -> {
				remaining = 0;
				completed = 0;
				started = 0;
				status = Status.OFF;
				if (instance != null) {
					DownloadingService i = instance;
					instance = null; // Null out so it doesn't loop when we call stop() within the service
					i.stop();
				}
				try {
					// Fail safe for making sure it is cancelled if the service doesn't cancel it.
					NotificationManagerCompat.from(context).cancel(ID);
				} catch (Throwable ignore) {}
			});
		}
		
		/** Cancels offline downloading and stops the service. */
		void cancelDownloading() {
			run(() -> {
				offline.cancelPredownloading();
				offline.suspendAutoDownload();
				stop();
			});
		}

		/**
		 * Publishes or updates the notification
		 * @param remaining items remaining
		 * @param completed items complete
		 */
		Notification buildNotification(int remaining, int completed) {
			int total = remaining + completed;
			return builder
					.setContentTitle(text.put("number_of_items", remaining).format())
					.setProgress(total, completed, completed == 0)
					.setWhen(started)
					.build();
		}
	}
	
	/**
	 * Creates a static singleton instance and installs itself in the {@link OfflineDownloading}.
	 */
	public static void initialize(Context context,
			AppThreads threads,
			OfflineDownloading offline,
			SystemNotifications notifications) {
		if (LISTENER == null) LISTENER = new Listener(context, threads, offline, notifications);
	}
	
	private static void log(String log) {
		Logs.e("DownloadingService", log);
	}

	private boolean hasStartedForeground;
	
	@Override
	public void onCreate() {
		super.onCreate();
		// Calling from onCreate is another suggestion to avoid the "did not then call Service.startForeground()" error on some devices
		// This ends up duplicating some work that will be done in onStartCommand, but hopefully this provides yet another bit of protection.
		if (LISTENER != null) update();
	}
	
	@Override
	public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
		if (DEBUG) log("onStartCommand " + this);
		if (LISTENER == null) {
			// Not expected in our usage, if something else starts this, just ignore and stop
			stop();
		} else if (ACTION_CANCEL.equals(intent != null ? intent.getAction() : null)) {
			LISTENER.cancelDownloading();
		} else {
			update();
		}
		return START_NOT_STICKY; // Don't let the system restart this service
	}
	
	/** Updates the notification to the latest counts or stops if the remaining count is <= 0 */
	void update() {
		int remaining = LISTENER.remaining;
		int completed = LISTENER.completed;
		if (remaining > 0) {
			if (DEBUG) log("update " + remaining + " " + completed);
			LISTENER.started(this);
			if (!hasStartedForeground) {
				hasStartedForeground = true;
				startForeground(ID, LISTENER.buildNotification(remaining, completed));
			}
		} else {
			if (DEBUG) log("update stopping");
			stop();
		}
	}
	
	/**
	 * Actually stops the service
	 */
	private void stop() {
		if (DEBUG) log("stop " + this);
		if (!hasStartedForeground && LISTENER != null) {
			// To avoid the "did not then call Service.startForeground()" error we need to call startForeground at least once on some OSs, even if we are stopping it before ever "going into the foreground"
			if (DEBUG) log("stopping before foreground");
			startForeground(42, LISTENER.buildNotification(1,1)); // Just pass in any number, since we'll cancel it right away.
		}
		stopForeground(true);
		stopSelf();
		if (LISTENER != null) LISTENER.stop();
	}
		
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (DEBUG) log("onDestroy " + this);
	}
	
	@Nullable @Override public IBinder onBind(Intent intent) {
		return null;
	}
}