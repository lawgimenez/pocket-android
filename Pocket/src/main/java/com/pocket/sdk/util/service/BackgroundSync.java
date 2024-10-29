package com.pocket.sdk.util.service;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.app.AppLifecycleEventDispatcher;
import com.pocket.app.Jobs;
import com.pocket.app.build.Versioning;
import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.notification.push.Push;
import com.pocket.sdk.util.file.AndroidStorageUtil;
import com.pocket.sdk2.api.legacy.PocketCache;
import com.pocket.util.java.Logs;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages the background syncing feature that lets a user choose when/how-often
 * the app will sync their Pocket.
 */
@Singleton
public class BackgroundSync implements AppLifecycle {
	
	public static final int SYNC_INSTANT = 0;
	public static final int SYNC_HOURLY = 1;
	public static final int SYNC_TWICE_DAILY = 2;
	public static final int SYNC_DAILY = 3;
	public static final int SYNC_NEVER = 4;
	private static final int SYNC_TEST_ONLY_AS_SHORT_AS_POSSIBLE = 5;
	
	private final Jobs jobs;
	private final PocketCache pktcache;
	private final Push push;
	private final Context context;
	private final boolean isFirstRun;
	private final IntPreference pref;
	
	private boolean hasActivityResumed;

	@Inject
	public BackgroundSync(
			Jobs jobs,
			PocketCache pktcache,
			Push push,
			@ApplicationContext Context context,
			Versioning versioning,
			Preferences prefs,
			AppLifecycleEventDispatcher dispatcher
	) {
		dispatcher.registerAppLifecycleObserver(this);
		this.jobs = jobs;
		this.pktcache = pktcache;
		this.push = push;
		this.context = context;
		this.isFirstRun = versioning.isFirstRun();
		pref = prefs.forUser("backgroundSyncingValue", BackgroundSync.SYNC_TWICE_DAILY);
		jobs.registerCreator(SyncJob.class, SyncJob::new);
		
		if (versioning.upgraded(7,1,24,0) && pktcache.isLoggedIn()) {
			// Switched to WorkManager, rescheduling with the updated SyncJob.
			scheduleAlarmSync();
		}
	}
	
	@Override
	public void onActivityResumed(Activity activity) {
		if (hasActivityResumed) return;
		hasActivityResumed = true;
		
		// always reregister on instantiation
		push.invalidate();
	}
	
	@Override
	public void onDeviceBoot() {
		scheduleAlarmSync();
	}
	
	@Override
	public LogoutPolicy onLogoutStarted() {
		cancelAlarmSync();
		return null;
	}
	
	@Override
	public void onLoggedIn(boolean isNewUser) {
		// Set timer based sync as default, but try to setup instant sync and switch to that if possible.
		// This uses CxtUi.LOGOUT because this will handle unregistering any previously registered user guid
		// The deregistration occurs at login, instead of logout because the logout process involves killing pending
		// requests/actions so is unreliable.  Deregistering also requires the user to be logged in, so it cannot be
		// done for instance on the login page.
		setBackgroundSyncing(AndroidStorageUtil.isInstalledOnExternalStorage(context) ? BackgroundSync.SYNC_NEVER : BackgroundSync.SYNC_HOURLY, CxtUi.LOGOUT);

		scheduleAlarmSync();

		// now register the new user / guid and set up instant sync by default
		push.register(CxtUi.LOGIN, (success, message) -> {
			if (success) {
				setBackgroundSyncing(BackgroundSync.SYNC_INSTANT, null);
			} else {
				// Leave as is
			}
		});
	}
	
	private void cancelAlarmSync() {
		jobs.cancel(SyncJob.class);
	}

	public void scheduleAlarmSync() {
		if (!pktcache.isLoggedIn()) {
			cancelAlarmSync();
			return;
		}
		
		long interval;
		switch (pref.get()) {
		case SYNC_DAILY:
			interval = AlarmManager.INTERVAL_DAY;
			break;
		case SYNC_TWICE_DAILY:
			interval = AlarmManager.INTERVAL_HALF_DAY;
			break;
		case SYNC_HOURLY:
			interval = AlarmManager.INTERVAL_HOUR;
			break;
		case SYNC_TEST_ONLY_AS_SHORT_AS_POSSIBLE:
			interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
			break;
		case SYNC_NEVER:
		case SYNC_INSTANT:
		default:
			cancelAlarmSync();
			return;
		}
		
		jobs.schedulePeriodic(SyncJob.class, interval);
	}
	
	public boolean isInstantSync(){
		return pref.get() == SYNC_INSTANT;
	}

	/**
	 * @param value one of {@link BackgroundSync#SYNC_INSTANT}, {@link BackgroundSync#SYNC_HOURLY}, {@link BackgroundSync#SYNC_DAILY},
	 * 					{@link BackgroundSync#SYNC_TWICE_DAILY}, or {@link BackgroundSync#SYNC_NEVER}
	 *
	 * @param cxt_ui The {@link CxtUi} to use for a call to deregister for push notifications,
	 *                  if {@link BackgroundSync#SYNC_INSTANT} is not selected.
	 */
	public void setBackgroundSyncing(int value, CxtUi cxt_ui) {
		pref.set(value);

		if (value != SYNC_INSTANT) {
			// TODO deregistering here would kill Amazon pinpoint notifications. Is there ever a reason to deregister now?
			// push.deregister(cxt_ui);
		}

		if (value == SYNC_INSTANT || value == SYNC_NEVER){
			cancelAlarmSync();
		} else {
			scheduleAlarmSync();
		}
	}

	private boolean isTimerSync() {
		return isTimerSync(pref.get());
	}

	public boolean isTimerSync(int setting) {
		switch(setting) {
		case SYNC_HOURLY:
		case SYNC_TWICE_DAILY:
		case SYNC_DAILY:
			return true;
		default:
			return false;
		}
	}

	public int getSelectedSettingLabel() {
		switch (pref.get()) {
			case 0: return R.string.setting_background_sync_0;
			case 1: return R.string.setting_background_sync_1;
			case 2: return R.string.setting_background_sync_2;
			case 3: return R.string.setting_background_sync_3;
			case 4: return R.string.setting_background_sync_4;
			default: return 0;
		}
	}
	
	/** Schedules a sync triggered by a push notification. */
	public void scheduleSyncFromPush() {
		jobs.scheduleImmediate(SyncJob.class);
	}
	
	/** This is only intended for a settings screen to use. */
	public IntPreference pref() {
		return pref;
	}
	
	private static class SyncJob extends Worker {
		SyncJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
			super(context, workerParams);
		}
		
		public Result doWork() {
			try {
				App.getApp().appSync().sync().get();
			} catch (Exception e) {
				Logs.printStackTrace(e);
			}
			return Result.success(); // Either way mark the job as complete. It can retry on next scheduled attempt.
		}
	}
	
}
