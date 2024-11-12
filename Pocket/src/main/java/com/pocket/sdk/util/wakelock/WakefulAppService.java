package com.pocket.sdk.util.wakelock;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.pocket.app.ActivityMonitor;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.util.android.ContextUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * This is a service that runs while {@link com.pocket.sdk.util.wakelock.WakeLockManager} is locked. Its intention is to
 * give the Android system a Service as a sign we are still doing things so the App isn't killed if we are doing
 * work in the background when the activity is closed.
 *
 * REVIEW - do we want to have all major delegates in the app to manage their own services and wakelocks rather
 * than having a central one?
 *
 */
public class WakefulAppService extends Service {

	@Singleton
	public static class Component implements AppLifecycle {
		
		private final Context context;
		private final ActivityMonitor activities;
		private boolean isWakeLocked;
		private boolean isUserPresent;
		private boolean isRunning;

		@Inject
		public Component(
				ActivityMonitor activities,
				WakeLockManager wakelocks,
				@ApplicationContext Context context,
				AppLifecycleEventDispatcher dispatcher
		) {
			dispatcher.registerAppLifecycleObserver(this);
			this.context = context;
			this.activities = activities;
			setWakeLocked(wakelocks.hasLocks(), context); // By the time this is created, it might already have some locks.
			wakelocks.setListener(isLocked -> setWakeLocked(isLocked, context));
		}
		
		private void setWakeLocked(boolean value, Context context) {
			isWakeLocked = value;
			invalidateService(context);
		}
		
		@Override
		public void onUserGone(Context context) {
			isUserPresent = false;
			invalidateService(context);
		}
		
		@Override
		public void onUserPresent() {
			isUserPresent = true;
			invalidateService(context);
		}
		
		private void invalidateService(Context context) {
			boolean run;
			if (isRunning) {
				// Continue running if there is a wakelock
				run = isWakeLocked;
			} else {
				// Only start running if the user is present, otherwise Android Oreo will throw exceptions for background startService calls.
				// In theory, if we are running in the background, there should already be some basis for it like a service or job, so the service shouldn't be needed.
				// TODO eventually we should remove this class and have all individual components manage their own wakefulness.
				run = isWakeLocked && isUserPresent;
			}
			
			Intent intent = new Intent(context, WakefulAppService.class);
			
			if (run) {
				isRunning = true;
				// When starting, try to use an activity context if available to better work with Android's background restrictions
				Activity activity = ContextUtil.getActivity(context);
				activity = activity != null ? activity : activities.getVisible();
				context = activity != null ? activity : context;
				try {
					context.startService(intent);
				} catch (Throwable ignore) {
					/*
						There  is a bug in Android P where even though the app is resumed, this can crash with
						"Unable to resume activity ... Not allowed to start service Intent ... app is in background"
						https://rink.hockeyapp.net/manage/apps/207507/app_versions/213/crash_reasons/242641781
						https://issuetracker.google.com/issues/113122354
						
						In this case, we shouldn't crash. The app is in the foreground so we don't need the wakelock app service running quite yet
						anyways. When the user changes something with a wakelock or leaves the app we'll get another chance to start this service
						from setWakeLocked() or onUserGone().
					 */
				}
			} else {
				isRunning = false;
				context.stopService(intent);
			}
		}
		
	}

    @Override
    public void onCreate() {
        super.onCreate();
        if (WakeLockManager.DEBUG) WakeLockManager.log("service start");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (WakeLockManager.DEBUG) WakeLockManager.log ("service destroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
}
