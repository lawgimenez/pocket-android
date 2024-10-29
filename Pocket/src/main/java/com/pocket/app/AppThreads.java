package com.pocket.app;

import android.os.Handler;
import android.os.Looper;

import androidx.test.espresso.IdlingResource;

import com.pocket.sdk.util.thread.PriorityTaskPool;
import com.pocket.sdk.util.thread.WakefulTaskPool;
import com.pocket.sdk.util.wakelock.WakeLockManager;
import com.pocket.util.android.thread.TaskPool;
import com.pocket.util.android.thread.TaskRunnable;
import com.pocket.util.java.Logs;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helpers and utilities for working with Threads and Handlers in the app
 * <p>
 * Run async tasks using one of the async() like methods.
 * <p>
 * These tasks are run in a general purpose task pool which will automatically handle stopping on during the {@link AppLifecycle.LogoutPolicy#stopModifyingUserData()}.
 * <p>
 * If you want to manage your own task pool, see some of the helper methods like {@link #newWakefulPool(String, int)} and {@link #newPriorityPool(String, int)} variants.
 * <p>
 * This also has {@link Handler} and Ui Thread related helper methods.
 */
@Singleton
public class AppThreads implements IdlingResource {
	
	private final Handler handler;
	private final Thread main;
	
	private final Object lock = new Object();
	private final WakeLockManager wakelocks;
	private TaskPool pool;

	@Inject
	public AppThreads(WakeLockManager wakelocks) {
		this.wakelocks = wakelocks;
		this.handler = new Handler(Looper.getMainLooper());
		this.main = Looper.getMainLooper().getThread();
	}
	
	/**
	 * Convenience for {@link Handler#post(Runnable)} with the built in {@link #getHandler()}.
	 * This always posts. Also see {@link #runOrPostOnUiThread(Runnable)} which will run it immediately if already on the ui thread.
	 */
	public void postOnUiThread(Runnable r) {
		handler.post(r);
	}
	
	/** If already on the UI thread, run the runnable immediately. Otherwise post it to the ui thread. */
	public void runOrPostOnUiThread(Runnable r) {
		if (isOnUIThread()) {
			r.run();
		} else {
			handler.post(r);
		}
	}
	
	/** If already NOT on the UI thread, run the runnable immediately. Otherwise submit it to a background pool to run. */
	public void runOffUiThread(Runnable r) {
		if (!isOnUIThread()) {
			r.run();
		} else {
			async(r);
		}
	}
	
	public boolean isOnUIThread() {
		return Thread.currentThread() == main;
	}
	
	public Handler getHandler() {
		return handler;
	}
	
	/**
	 * @return Creates if needed and returns a single task pool for general usage.
	 */
	private TaskPool pool() {
		synchronized (lock) {
			if (pool == null) {
				pool = new WakefulTaskPool(wakelocks, 5, 128, "task");
			}
			return pool;
		}
	}
	
	/**
	 * Queue up a task in the general purpose task pool, see main doc for details.
	 */
	public void submit(TaskRunnable task) {
		pool().submit(task);
	}
	
	/**
	 * Queues up a task off the ui thread in the general purpose task pool.
	 * <b>Note</b> It is up to you to handle exceptions this might throw.
	 * If any uncaught runtime exceptions are thrown, they will be logged via {@link Logs#printStackTrace(Throwable)} but otherwise ignored.
	 * @param task The background work to complete
	 * @return The already submitted task for listening, referencing, controlling, etc.
	 */
	public TaskRunnable async(Runnable task) {
		TaskRunnable run = TaskRunnable.simple(task::run);
		pool().submit(run);
		return run;
	}
	
	/**
	 * Same as {@link #async(Runnable)} but catches uncaught exceptions during the task
	 * and invokes onError with the the error. This error callback will be invoked from the task thread as well.
	 * The error will automatically be logged via {@link Logs#printStackTrace(Throwable)}.
	 * Passing a null onError will essentially ignore the error.
	 *
	 * This can be useful if your task must callback to something regardless of result and you want to make sure runtime exceptions
	 * and other errors still end up with a callback.
	 */
	public TaskRunnable async(SimpleTask task, OnError onError) {
		return async(() -> {
			try {
				task.backgroundOperation();
			} catch (Throwable t) {
				if (onError != null) onError.onError(t);
			}
		});
	}
	
	/**
	 * A way to run a simple, quick task with an interface that allows for lambda use.
	 * Submitted to the general purpose task pool, see main doc for details.
	 * @param task The background work to complete. Note, all exceptions here are caught. See the callback's crash parameter for the exception.
	 * @param uiOnComplete A callback on the ui thread.
	 * @return The already submitted task for listening, referencing, controlling, etc.
	 */
	public TaskRunnable asyncThen(SimpleTask task, UiThreadResponse uiOnComplete) {
		TaskRunnable run = new TaskRunnable() {
			@Override
			protected void backgroundOperation() throws Exception {
				task.backgroundOperation();
			}
			
			@Override
			protected boolean requiresUIResponse() {
				return true;
			}
			
			@Override
			protected void uiOnComplete(boolean success, Throwable operationCrash) {
				if (uiOnComplete != null) {
					uiOnComplete.uiOnComplete(success, operationCrash);
				}
			}
		};
		pool().submit(run);
		return run;
	}
	
	public interface SimpleTask {
		void backgroundOperation() throws Exception;
	}
	public interface OnError {
		void onError(Throwable error);
	}
	public interface ResultTask<T> {
		T backgroundOperation() throws Exception;
	}
	
	public interface UiThreadResponse {
		void uiOnComplete(boolean success, Throwable operationCrash);
	}
	public interface UiThreadResultResponse<T> {
		void uiOnComplete(boolean success, Throwable operationCrash, T result);
	}
	
	
	/**
	 * This does not use {@link AppLifecycle#onLogoutStarted()} because {@link com.pocket.app.UserManager}
	 * wants to run this policy at the very end, because logout processes might actually need
	 * to use this to run async tasks.
	 */
	public AppLifecycle.LogoutPolicy getLogoutPolicy() {
		return new AppLifecycle.LogoutPolicy() {
			
			@Override
			public void stopModifyingUserData() {
				synchronized (lock) {
					if (pool != null) {
						pool.terminate(20, TimeUnit.SECONDS);
					}
				}
			}
			
			@Override
			public void deleteUserData() {}
			
			@Override
			public void restart() {
				pool = null;
			}

			@Override
			public void onLoggedOut() {}

		};
	}
	
	/**
	 * Creates and returns a new task pool that can process tasks by priority.
	 * It also has the same rules as {@link #newWakefulPool(String, int)}.
	 */
	public PriorityTaskPool newPriorityPool(String name, int maxThreads) {
		PriorityTaskPool pool = new PriorityTaskPool(wakelocks, maxThreads, name);
		setup(pool);
		return pool;
	}
	
	/**
	 * Creates and returns a new task pool that has some standard settings:
	 * <ul>
	 *     <li>Holds a wake lock as long as it has pending or active tasks.</li>
	 *     <li>Can spin up to maxThreads when busy.</li>
	 *     <li>Will spin down and release down threads when idle.</li>
	 * </ul>
	 * Note: These pools are <b>not</b> automatically managed during logout,
	 * please consider whether or not you need a {@link com.pocket.app.AppLifecycle.LogoutPolicy}.
	 */
	public WakefulTaskPool newWakefulPool(String name, int maxThreads) {
		WakefulTaskPool pool = new WakefulTaskPool(wakelocks, maxThreads, name);
		setup(pool);
		return pool;
	}
	
	/** A new pool with custom settings. These pools are not automatically managed during logout, it is up to you to handle that as needed. */
	public WakefulTaskPool newWakefulPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		return new WakefulTaskPool(wakelocks, corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(), name);
	}
	
	/** A new pool with custom settings. These pools are not automatically managed during logout, it is up to you to handle that as needed. */
	public PriorityTaskPool newPriorityPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		return new PriorityTaskPool(wakelocks, corePoolSize, maximumPoolSize, keepAliveTime, unit, name);
	}
	
	private void setup(WakefulTaskPool pool) {
		pool.setKeepAliveTime(10, TimeUnit.SECONDS);
		pool.allowCoreThreadTimeOut(true);
	}

	@Override
	public String getName() {
		return pool().getName();
	}

	@Override
	public boolean isIdleNow() {
		return pool().isIdleNow();
	}

	@Override
	public void registerIdleTransitionCallback(ResourceCallback callback) {
		pool().registerIdleTransitionCallback(callback);
	}
}
