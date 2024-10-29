package com.pocket.util.android.thread;

import com.pocket.util.android.AndroidBgThreadFactory;
import com.pocket.util.java.Logs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * A {@link ThreadPoolExecutor} that lets you pause work in its queue. See {@link #pause()}
 * Also provides a {@link #setExecutionListener(ExecutionListener)}
 */
public class PausableThreadPoolExecutor extends ThreadPoolExecutor {
	
	private final AtomicBoolean isPaused = new AtomicBoolean(false);
	private final ReentrantLock pauseLock = new ReentrantLock();
	private final Condition unpaused = pauseLock.newCondition();
    private final String mName;
    private ExecutionListener mExecutionListener;
	
	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new AndroidBgThreadFactory(poolName));
        mName = poolName;
	}
	
	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory factory, String poolName) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, AndroidBgThreadFactory.wrap(factory, poolName));
		mName = poolName;
	}
	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if(mExecutionListener != null)
			mExecutionListener.beforeExecution(r);
		
		super.beforeExecute(t, r);
		
		pauseLock.lock();
		try {
			while (isPaused.get()){
				unpaused.await();
			}
		} catch(InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}
		
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		if(mExecutionListener != null)
			mExecutionListener.afterExecution(r);
		
		super.afterExecute(r, t);
		
		if (t == null && r instanceof Future<?>) {
			try {
				((Future<?>) r).get();
			} catch (CancellationException ce) {
				t = ce;
			} catch (ExecutionException ee) {
				t = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt(); // ignore/reset
			}
		}
		if (t != null){
			Logs.printStackTrace(t);
		}
	}
	
	/**
	 * Pauses work in this thread pool.
	 *
	 * Tasks that have already begun will be completed but
	 * nothing will be taken from the queue to run until after {@link #resume()} is invoked.
	 *
	 * While paused you may continue to add tasks to the queue.
	 */
	public void pause() {
		if(!isPaused.get()){
			pauseLock.lock();
			try {
				isPaused.set(true);
			} finally {
				pauseLock.unlock();
			}
		}
	}
	
	/**
	 * Resume running tasks in the queue.
	 */
	public void resume() {
		if (isPaused.get()) {
			pauseLock.lock();
			try {
				isPaused.set(false);
				unpaused.signalAll();
			} finally {
				pauseLock.unlock();
			}
		}
	}
	
	public boolean isPaused() {
		return isPaused.get();
	}
	
	/**
	 * Shutdown will also {@link #resume()} if paused, so the remaining tasks can be completed.
	 */
	@Override
	public void shutdown() {
		resume();
		super.shutdown();
	}

    public String getName() {
        return mName;
    }

	protected void setExecutionListener(ExecutionListener listener) {
		mExecutionListener = listener;
	}
	
	protected interface ExecutionListener {
		void beforeExecution(Runnable r);
		void afterExecution(Runnable r);
	}

}
