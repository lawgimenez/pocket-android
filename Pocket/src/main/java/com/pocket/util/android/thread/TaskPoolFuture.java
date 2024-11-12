package com.pocket.util.android.thread;

import java.util.concurrent.FutureTask;


/**
 * A {@link FutureTask} wrapper on a {@link TaskRunnable} to provide some status info for {@link TaskPool}
 * and to invoke the cancel method if needed.
 */
public class TaskPoolFuture extends FutureTask<Object> {
	
	private final Object mLock = new Object();
	TaskRunnable mRunnable;
	private boolean mWasBulkCancelled = false;
	
	TaskPoolFuture(TaskRunnable runnable) {
		super(runnable, null);
		mRunnable = runnable;
	}

	@Override
	protected void done() {
		synchronized (mLock) {
			mRunnable = null;
		}
		super.done();
	}
	
	/**
	 * Same as {@link #cancel(boolean)} but also flags that this was part of a bulk cancel
	 * so that {@link #wasBulkCanceled()} will return true.
	 */
	public void bulkCancel() {
		synchronized (mLock) {
			if (mRunnable == null) {
				return;
			}
			mWasBulkCancelled = true;
		}
		
		mRunnable.cancel();
	}
	
	/**
     * Check if the TaskRunnable was cancelled because the pool did a bulk cancel.
     * @return true if it was part of a bulk pool cancel
     */
	public boolean wasBulkCanceled() {
		synchronized (mLock) {
			return mWasBulkCancelled;
		}
	}
	
}

