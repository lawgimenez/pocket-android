package com.pocket.app;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Management of JobScheduler like Jobs. Actually now WorkManager Workers.
 *
 * How to use:
 * <ol>
 *     <li>Create a worker, usually by extending {@link Worker}, but can be another (even custom) subclass of
 *     {@link ListenableWorker}.</li>
 *     <li>During app init register a creator for your worker via
 *     {@link #registerCreator(Class, WorkCreator)}.</li>
 *     <li>Call one of the <code>schedule*()</code> methods as needed.</li>
 * </ol>
 * 
 * REFACTOR: rethink the API to make it impossible to call schedule* methods without calling
 * {@link #registerCreator(Class, WorkCreator)} first. Like maybe return something from 
 * registerCreator() that is required to call schedule*. Or at least make it a runtime exception
 * in dev builds.
 */
@Singleton
public class Jobs {
	
	private final Map<String, WorkCreator> creators = new HashMap<>();
	
	private WorkManager manager;

	@Inject
	public Jobs(AppMode mode, @ApplicationContext Context context) {
		WorkManager.initialize(context, new Configuration.Builder()
				.setMinimumLoggingLevel(mode.isDevBuild() ? Log.VERBOSE : Log.INFO)
				.setWorkerFactory(new CreatorWorkerFactory(context))
				.build());
		manager = WorkManager.getInstance();
	}
	
	public void cancel(Class<? extends ListenableWorker> worker) {
		manager.cancelUniqueWork(worker.getName());
	}
	
	public <T extends ListenableWorker> void registerCreator(Class<T> worker, WorkCreator<T> creator) {
		creators.put(worker.getName(), creator);
	}
	
	/**
	 * Schedule a one-shot worker.
	 * @param worker The work to schedule.
	 * @param startInMs Initial delay before attempting to run this work (optional, can be 0 if not required).
	 * @param networkType Specifies network requirements (can be {@link NetworkType#NOT_REQUIRED} if you don't need 
	 * network connection).
	 */
	public void scheduleOneOff(Class<? extends ListenableWorker> worker, long startInMs, NetworkType networkType) {
		manager.enqueueUniqueWork(worker.getName(),
				ExistingWorkPolicy.REPLACE,
				new OneTimeWorkRequest.Builder(worker)
						.setConstraints(new Constraints.Builder().setRequiredNetworkType(networkType).build())
						.setInitialDelay(startInMs, TimeUnit.MILLISECONDS)
						.build());
	}
	
	/**
	 * Schedule a periodic worker (by default it only runs when there is a network connection).
	 */
	public void schedulePeriodic(Class<? extends ListenableWorker> worker, long intervalMs) {
		manager.enqueueUniquePeriodicWork(worker.getName(),
				ExistingPeriodicWorkPolicy.REPLACE,
				new PeriodicWorkRequest.Builder(worker, intervalMs, TimeUnit.MILLISECONDS)
						.setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
						.build());
	}
	
	public void scheduleImmediate(Class<? extends ListenableWorker> worker) {
		manager.enqueue(new OneTimeWorkRequest.Builder(worker).build());
	}
	
	public interface WorkCreator<T extends ListenableWorker> {
		T create(Context context, WorkerParameters workerParams);
	}
	
	private class CreatorWorkerFactory extends WorkerFactory {
		private final Context context;
		
		CreatorWorkerFactory(Context context) {this.context = context;}
		
		@Nullable @Override
		public ListenableWorker createWorker(@NonNull Context appContext,
				@NonNull String workerClassName,
				@NonNull WorkerParameters workerParameters) {
			final WorkCreator creator = creators.get(workerClassName);
			if (creator != null) {
				return creator.create(context, workerParameters);
			} else {
				return null;
			}
		}
	}
}
