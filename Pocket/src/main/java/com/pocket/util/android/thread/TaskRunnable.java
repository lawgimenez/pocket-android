package com.pocket.util.android.thread;

import com.pocket.app.App;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Cancelable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * A single background task to run in a {@link TaskPool}.
 * <p>
 * Subclass and implement the task's work in {@link #backgroundOperation()}.
 * Submit the task to a pool with {@link TaskPool#submit(TaskRunnable)}.
 * <p>
 * Provides some additional hooks that can be overridden:
 * <ul>
 * <li>{@link #backgroundOnSkipped()} Runs if the task's work skipped because it was cancelled
 * <li>{@link #backgroundOnComplete(boolean, Throwable)} Runs after the work, while still on a background thread
 * <li>{@link #requiresUIResponse()} Allows you to enable a response to be invoked on the ui thread
 * <li>{@link #uiOnComplete(boolean, Throwable)} If requiresUIResponse is true, this will be invoked on the ui thread after your work is complete
 * </ul>
 * Outside processes can also observe/listen to the result of this operation with {@link #setOperationListener(OperationListener, boolean)}
 */
public abstract class TaskRunnable implements Runnable, Cancelable {
	
	public static final int STATUS_UNEXECUTED = 1;
	public static final int STATUS_PENDING = 2;
	public static final int STATUS_EXECUTING = 3;
	public static final int STATUS_COMPLETE = 4;
	public static final int STATUS_CANCELED = -1;
	public static final int STATUS_CRASHED = -2;
	
	
	public static final int PRIORITY_LOW = 1;
	public static final int PRIORITY_NORMAL = 2;
	public static final int PRIORITY_HIGH = 3;
	public static final int PRIORITY_VERY_HIGH = 4;

	protected int mPriority;
	
	protected final AtomicInteger mStatus = new AtomicInteger(STATUS_UNEXECUTED);
	protected final AtomicBoolean mCanceled = new AtomicBoolean(false);
	protected TaskPoolFuture mFuture;
	protected final boolean mUIResponse;
	protected TaskPool mPool;
	private OperationListener mOperationListener;
	private boolean mOperationListenerReturnOnUIThread;
	
	public TaskRunnable() {
    	this(PRIORITY_NORMAL);
    }
    
    public TaskRunnable(int priority) {
    	mPriority = priority;
    	mUIResponse = requiresUIResponse();
    }
	
	/**
	 * Flag this task as cancelled. This does not interrupt threads.
	 * If the task has not yet run, when it is run, it will see this flag,
	 * and skip all of the work. See {@link #backgroundOnSkipped()}.
	 * <p>
	 * Very long running tasks can also check {@link #isCancelled()} periodically to see if it should stop its current work.
	 */
	@Override
	public void cancel() {
    	mCanceled.set(true);
    }
    
    @Override
    public boolean isCancelled() {
    	return mCanceled.get()
    	|| mStatus.get() == STATUS_CANCELED
    	|| (mFuture != null && mFuture.isCancelled());
    }
    
    /*
     * A TaskPool will call this right before it executes.
     * 
     */
    public void onTaskPoolSubmit(TaskPool pool, TaskPoolFuture futureTask) {
    	mStatus.compareAndSet(STATUS_UNEXECUTED, STATUS_PENDING);
    	mPool = pool;
    	mFuture = futureTask;
    }
    
    /**
     * Blocks on and awaits this task's future and then returns the status flag (One of the {@link #STATUS_COMPLETE} like constants.
	 * Before invoking this, the task must have either been submitted to a task pool or {@link #runNow()} or it will throw an exception.
     */
    public int get() {
    	if (mFuture == null)
    		throw new NullPointerException("task not yet submitted or run");
    	
    	try {
    		mFuture.get();
    		
    	} catch (Throwable t) {
    		return STATUS_CRASHED;
    	}
    	
    	return mStatus.get();
    }
    
    /*
     * Run now on the current Thread, blocking until complete, instead of submitting to a pool.
     */
    public void runNow() {
    	onTaskPoolSubmit(null, null);
    	run();
    }
    
    public int getPriority(){
		return mPriority;
	}
	
	public void setPriority(int priority){
		mPriority = priority;
	}
	
	protected void setAsFailed(){
		mStatus.set(STATUS_CRASHED);
	}
	
	public boolean hasFailed(){
		return mStatus.get() == STATUS_CRASHED;
	}
	
	/**
	 * Should only be called from a TaskPool's processes', if you want to run it directly, use {@link #runNow()}
	 */
	@Override
	public final void run() {
		boolean success = false;
		Throwable operationCrash = null;
		if (!isCancelled() && mStatus.compareAndSet(STATUS_PENDING, STATUS_EXECUTING)) {
	    	
	    	// Run the operation
	    	try {
	    		backgroundOperation();
	    		success = true;
	    			    	
	    	// If it crashes, call the on error method
	    	} catch (Throwable operationThrowable) {
	    		mStatus.set(STATUS_CRASHED);
	    		operationCrash = operationThrowable;
	    		Logs.printStackTrace(operationThrowable);
			}
	    	
    	} else {
    		// OPT should check if it is cancelled versus something else?
    		mStatus.set(STATUS_CANCELED);
    		backgroundOnSkipped();
    		return;
    	}
		
		// Whether it crashes or completes, always guarantee one of these callbacks
		// Even if cancelled
		final boolean fSuccess = success && mStatus.get() == STATUS_EXECUTING;
		final Throwable fOperationCrash = operationCrash;
		try {
			if (mUIResponse) {
				// Callback on ui thread
				// QUESTION should the operation wait for this callback to finish?
				App.getApp().threads().runOrPostOnUiThread(new Runnable() {
    				@Override
    				public void run() {
    					uiOnComplete(fSuccess, fOperationCrash);
    					if (mOperationListener != null) {
    						mOperationListener.onComplete(TaskRunnable.this, fSuccess);
    					}
    				}
    			});
				
			} else {
				// Callback on background thread
				backgroundOnComplete(fSuccess, fOperationCrash);
				if (mOperationListener != null) {
					if (mOperationListenerReturnOnUIThread) {
						App.getApp().threads().runOrPostOnUiThread(() -> mOperationListener.onComplete(TaskRunnable.this, fSuccess));
					} else {
						mOperationListener.onComplete(TaskRunnable.this, fSuccess);
					}
				}
			}
		} catch (Throwable unexpected) {
			mStatus.set(STATUS_CRASHED);
			App.getApp().errorReporter().reportError(unexpected);

		}
		
		if (mOperationListener != null) {
			mOperationListener.onFinal();
		}
		
		mStatus.compareAndSet(STATUS_EXECUTING, STATUS_COMPLETE);
	}
	
    /**
     * Called on the operation thread. Do your background work here.
     * @throws Exception
     */
	protected abstract void backgroundOperation() throws Exception;
    
	/**
	 * If the operation should call onComplete on the ui thread instead of the background thread, override this and return true.
	 * 
	 * @return
	 */
	protected boolean requiresUIResponse(){ return false; };   // OPT combine this with the uiOnComplete method some how
    
	/**
	 * If the operation is canceled, this will always be called to allow subclasses to clean up.
	 * Runs on the operation thread.
	 */
	protected void backgroundOnSkipped(){};
	
	/**
	 * Even if the backgroundOperation crashes, this will always be called. Use this as a chance to perform clean up.
	 * Runs on the operation thread.
	 * 
	 * @param success if backgroundOperation finished successfully.
	 * @param operationCrash if success is false, this will contain the Throwable that crashed it. It may be null if success is false because of the operation being canceled.
	 */
    protected void backgroundOnComplete(final boolean success, Throwable operationCrash){
    	
	}
	
    
    
	/**
	 * Even if the backgroundOperation crashes, this will always be called (if requiresUIResponse() returns true).
	 * Runs on the UI thread.
	 * 
	 * NOTE: By the time this runs, the operation may be complete and nothing in this should access the operation's variables.
	 * Also at this time, the operation does not wait until this has run. This should be considered in the future.
	 * 
	 * @param success if backgroundOperation finished successfully.
	 * @param operationCrash if success is false, this will contain the Throwable that crashed it. It may be null if success is false because of the operation being canceled.
	 */
    protected void uiOnComplete(boolean success, Throwable operationCrash){};
    
	
	public void setOperationListener (OperationListener listener, boolean returnOnUIThread) {
		mOperationListener = listener;
		mOperationListenerReturnOnUIThread = returnOnUIThread;
	}
	
	public interface OperationListener {
		/**
		 * Called after backgroundOperation() or backgroundOnSkipped(). If you want this to be called on the UI Thread, set returnOnUIThread of setOperationLister() to true.
		 * 
		 * @param success whether or not the backgroundOperation() completed
		 */
		public void onComplete(TaskRunnable operation, boolean success);
		
		/**
		 * Called on background thread as last chance to perform tasks on the background thread, last thing that occurs in the operation.
		 */
		public void onFinal();
	}
	
	public static TaskRunnable simple(ThrowingRunnable work, int priority) {
		return new TaskRunnable(priority) {
			@Override
			protected void backgroundOperation() throws Exception {
				work.run();
			}
		};
	}
	
	public static TaskRunnable simple(ThrowingRunnable work) {
		return simple(work, PRIORITY_NORMAL);
	}
	
	public interface ThrowingRunnable {
		void run() throws Exception;
	}
	
	
}
