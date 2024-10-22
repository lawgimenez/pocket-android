package com.pocket.util.android.thread;



public class PriorityFutureTask extends TaskPoolFuture {
	
	/**
	 * Used to keep track of the order it was submitted to the queue,
	 * so the Comparator can keep track of priority within this order.
	 */
	private final long mAddedOrder;
	private final int mInitialPriority;
	
	public PriorityFutureTask(TaskRunnable runnable, long addedOrder) {
		super(runnable);
		
		mAddedOrder = addedOrder;
		mInitialPriority = getPriority();
	}
	
	public int getPriority() {
		if (mRunnable == null) {
			// This task was cancelled or finished
			return mInitialPriority;
		} else {
			return mRunnable.getPriority();
		}
	}
	
	public long getAddedOrder() {
		return mAddedOrder;
	}
	
}

