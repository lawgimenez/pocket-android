package com.pocket.sdk.util.thread;

import com.pocket.sdk.util.wakelock.WakeLockHolder;
import com.pocket.sdk.util.wakelock.WakeLockManager;
import com.pocket.util.android.thread.TaskPool;
import com.pocket.util.java.Milliseconds;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool that maintains a wake lock while it is running or has pending tasks.
 *
 * @see com.pocket.sdk.util.wakelock.WakeLockManager
 */
public class WakefulTaskPool extends TaskPool {
	
	private WakeLockHolder mWakeLockHolder;

    public WakefulTaskPool(WakeLockManager wakelocks, int fixedSize, String poolName) {
        super(fixedSize, poolName);
        init(wakelocks);
    }

    public WakefulTaskPool(WakeLockManager wakelocks, int corePoolSize, int maximumPoolSize, String poolName) {
        super(corePoolSize, maximumPoolSize, poolName);
        init(wakelocks);
    }
	
	public WakefulTaskPool(WakeLockManager wakelocks, int corePoolSize, int maximumPoolSize, BlockingQueue<Runnable> workQueue, String poolName) {
        super(corePoolSize, maximumPoolSize, workQueue, poolName);
        init(wakelocks);
    }
	
	public WakefulTaskPool(WakeLockManager wakelocks, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String poolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, poolName);
        init(wakelocks);
    }
	
    private void init(WakeLockManager wakelocks) {
        mWakeLockHolder = WakeLockHolder.withTimeout(getName(), 30, 50, () -> new StringBuilder()
				.append(" pk:").append(getQueue().peek())
				.append(" qs:").append(getQueue().size())
				.append(" ac:").append(getActiveCount())
				.append(" cc:").append(getCompletedTaskCount())
				.append(" tc:").append(getTaskCount())
				.append(" ls:").append(Milliseconds.millisToMinutes(WakefulTaskPool.this.millisSinceLastSubmit()))
				.append(" hw:").append(WakefulTaskPool.this.hasWork())
				.append(" ia:").append(WakefulTaskPool.this.isActive())
				.append(" ip:").append(WakefulTaskPool.this.isPaused()).toString());
        
        addExecutionStateChangeListener(new ExecutionStateChangeListener() {
            @Override
            public void onTaskPoolExecutionStateChanged(boolean isExecuting) {
                if (isExecuting) {
					wakelocks.acquire(mWakeLockHolder);
                } else {
					wakelocks.release(mWakeLockHolder);
                }
            }
        });
    }
}

