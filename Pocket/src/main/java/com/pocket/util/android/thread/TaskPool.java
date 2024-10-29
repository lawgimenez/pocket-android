package com.pocket.util.android.thread;

import android.os.SystemClock;

import androidx.test.espresso.IdlingResource;

import com.pocket.util.java.Logs;
import com.pocket.util.android.thread.PausableThreadPoolExecutor.ExecutionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool executor that provides a bulk cancel function and can be paused, resumed and monitored.
 * <ul>
 * <li>Submit/queue tasks with {@link #submit(TaskRunnable)} instead of the default execute and submit methods.
 * <li>Cancel all tasks with {@link #cancelAll()}.
 * <li>Pause and Resume with {@link #pause()} and {@link #resume()}.
 * <li>Terminate the entire pool with {@link #terminate(int, TimeUnit)}.
 * </ul>
 * <p>
 * This class is thread safe.
 * <p>
 * Developer Note: When a task is bulk canceled, it remains in the queue and will
 * actually execute, but since it is flagged as cancelled, it will not perform its
 * function and will quickly be discarded.
 * <p>
 * Pocket Usage: For the Pocket app this serves as a replacement for AsyncTask and
 * most thread pools. It allows fine control over cancelling and stopping all async tasks
 * at logout or for specific cases like stopping offline downloading.
 */
public class TaskPool extends PausableThreadPoolExecutor implements ExecutionListener, IdlingResource {

	private final Object mLock = new Object();
	
	private Status mStatus = Status.ACTIVE;
	private enum Status {
		ACTIVE,
		TERMINATING,
		TERMINATED
	}
	
	/**
	 * The timestamp (elapsedRealtime) of when the last/latest task was submitted to this pool,
	 * or zero if never.
	 */
	private long mLastSubmit = 0;
	
	/**
	 * Tasks that are active, meaning queued or executing, and haven't been bulk cancelled.
	 * Tasks are added to these when submitted and are removed if bulk canceled or completed.
	 * This is not used as a queue, only to track status and to reference for bulk canceling.
	 */
	private final List<TaskPoolFuture> mActiveTasks = new ArrayList<>();

	private final TaskPoolIdlingResource mCounter = new TaskPoolIdlingResource(getName());
	
	private final ArrayList<ExecutionStateChangeListener> mExecutionStateChangeListeners = new ArrayList<>();
	private AfterExecutionListener mAfterExecutionListener;
	
	public TaskPool(int fixedSize, String poolName) {
		this(fixedSize, fixedSize, poolName);
	}
	
	public TaskPool(int corePoolSize, int maximumPoolSize, String poolName) {
		this(corePoolSize, maximumPoolSize, new LinkedBlockingQueue<>(), poolName);
	}
	
	protected TaskPool(int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue, String poolName) {
		this(corePoolSize, maximumPoolSize, 1, TimeUnit.SECONDS, workQueue, poolName);
	}
	
	protected TaskPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, poolName);
		setExecutionListener(this);
	}
	
	protected TaskPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory factory, String poolName) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, factory, poolName);
		setExecutionListener(this);
	}
	
	
	@Override
	public void execute(Runnable command) {
		// This is a catch all for anything that submits via the public ThreadPoolExecutor interface, so all calls run through submit(TaskRunnable) instead.
		submit(TaskRunnable.simple(command::run));
	}
	
	/**
	 * Submit a task to the queue to be executed.
	 * @param runnable A {@link TaskRunnable} containing the future work to complete.
	 * @return A future or null if this pool is terminated and no longer accepting tasks
	 */
	public FutureTask<Object> submit(TaskRunnable runnable) {
		synchronized (mLock) {
			if (mStatus != Status.ACTIVE) {
				return null;
			}
			
			mLastSubmit = SystemClock.elapsedRealtime();
			
			TaskPoolFuture futureTask = newFutureTask(runnable);
			mActiveTasks.add(futureTask);
			mCounter.increment();

			runnable.onTaskPoolSubmit(this, futureTask);
			super.execute(futureTask);
			invalidateExecutionState();
			return futureTask;
		}
	}
	
	/**
	 * Create a wrapper for this runnable.
	 * Provided for subclasses to override as needed
	 */
	protected TaskPoolFuture newFutureTask(TaskRunnable runnable) {
		return new TaskPoolFuture(runnable);
	}
	
	/**
	 * Cancel all tasks currently being executed and in the queue.
	 * For executing tasks, it only invokes {@link TaskRunnable#cancel()} and does not try to interrupt the thread.
	 * This does not shutdown the queue.
	 * Any new submissions after this call will be accepted.
	 */
	public void cancelAll() {
		cancelAll(false);
	}
	
	/**
	 * Like {@link #cancelAll()} but also blocks until the queue is empty.
	 * If new tasks are added during this wait it will also cancel those and will extend the wait time.
	 */
	public void cancelAllUntilEmpty() {
		cancelAll(true);
	}
	
	private void cancelAll(boolean await) {
		List<TaskPoolFuture> futures = new ArrayList<>();
		synchronized (mLock) {
			for (TaskPoolFuture r : mActiveTasks) {
				if (await) futures.add(r);
				try {
					r.bulkCancel();
				} catch (Throwable t) {
					// Don't let an outside implementation break our flow here.
					Logs.printStackTrace(t);
				}
			}
			mActiveTasks.clear();
			mCounter.clear();
			invalidateExecutionState();
		}
		if (await && !futures.isEmpty()) {
			for (TaskPoolFuture r : futures) {
				try {
					r.get();
				} catch (Throwable ignore) {}
			}
			cancelAll(true); // Loop until we didn't have to cancel/await anything. This catches tasks added during our wait time.
		}
	}
	
	@Override
	public void beforeExecution(Runnable r) {}
	
	@Override
	public void afterExecution(Runnable r) {
		TaskPoolFuture f = (TaskPoolFuture) r;
		boolean wasBulkCanceled;
		
		synchronized (mLock) {
			mActiveTasks.remove(f);
			mCounter.decrement();
			wasBulkCanceled = f.wasBulkCanceled();
			invalidateExecutionState();
		}
		
		if (mAfterExecutionListener != null) {
			try {
				mAfterExecutionListener.afterExecution(wasBulkCanceled);
			} catch (Throwable t) {
				// Don't let a bad callback break this classes functionality and flow
				Logs.printStackTrace(t);
			}
		}
	}
	
	/**
	 * Stop receiving new tasks and wait (blocking) for currently queue'd tasks to complete (up to the provided timeout)
	 * afterwards, it kills the underlying pool. This pool is no longer useable and should be discarded.
	 *
	 * @param timeout how long to wait to allow the already queue'd work to complete before discarding and terminating.
	 * @param unit The timeout's time unit.
	 */
	public void terminate(int timeout, TimeUnit unit) {
		synchronized (mLock) {
			mStatus = Status.TERMINATING;
			shutdown();
		}
		
		try {
			awaitTermination(timeout, unit);
		} catch (InterruptedException e) {
			Logs.printStackTrace(e);
		}
		
		synchronized (mLock) {
			cancelAll();
			mStatus = Status.TERMINATED;
			shutdownNow();
			invalidateExecutionState();
		}
	}
	
	public boolean isActive() {
		synchronized (mLock) {
			return mStatus == Status.ACTIVE;
		}
	}
	
	// TODO pausing should have a timeout to avoid accidently holding work indefinitely.
	
	public void pause() {
		synchronized (mLock) {
			super.pause();
		}
	}

	public void resume() {
		synchronized (mLock) {
			super.resume();
		}
	}
	
	public boolean isPaused() {
		synchronized (mLock) {
			return super.isPaused();
		}
	}

	/**
	 * true when this pool is actively working on a task and/or has pending  (non-cancelled) work in its queue.
	 */
	public boolean hasWork() {
		synchronized (mLock) {
			return mStatus != Status.TERMINATED && !mActiveTasks.isEmpty();
		}
	}

    public void addExecutionStateChangeListener(ExecutionStateChangeListener listener) {
		synchronized (mLock) {
			listener.isExecuting = hasWork();
			mExecutionStateChangeListeners.add(listener);
		}
    }

    @Override
    public String getName() {
        return super.getName(); // mCounter uses this TaskPool's name, so for IdlingResource's implementation just call PausableThreadPoolExecutor's implementation
    }

	@Override
	public boolean isIdleNow() {
		return mCounter.isIdleNow();
	}

	@Override
	public void registerIdleTransitionCallback(ResourceCallback callback) {
		mCounter.registerIdleTransitionCallback(callback);
	}

	private void invalidateExecutionState() {
		synchronized (mLock) {
			boolean isExecuting = hasWork();
			for (ExecutionStateChangeListener listener : mExecutionStateChangeListeners) {
				if (listener.isExecuting != isExecuting) {
					listener.isExecuting = isExecuting;
					try {
						listener.onTaskPoolExecutionStateChanged(isExecuting);
					} catch (Throwable t) {
						// Don't let an outside implementation break our flow
						Logs.printStackTrace(t);
					}
				}
			}
		}
    }
	
	public interface AfterExecutionListener {
		void afterExecution(boolean wasBulkCanceled);
	}

    public abstract static class ExecutionStateChangeListener {

        /** The last execution state sent to the listener, or its initial state when registered to listen. */
        private boolean isExecuting;

        public abstract void onTaskPoolExecutionStateChanged(boolean isExecuting);
    }
    
    public long millisSinceLastSubmit() {
		if (mLastSubmit == 0) {
			return 0;
		} else {
			return  SystemClock.elapsedRealtime() - mLastSubmit;
		}
	}
	
}
