package com.pocket.sync.source.threads;

import com.pocket.util.android.AndroidBgThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Creates thread pools whose threads run with Android's recommend background thread priority.
 */
public class AndroidThreadPools implements ThreadPools {
	
	@Override
	public Pool newPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut) {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(), new AndroidBgThreadFactory());
		pool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		return new Pool() {
			@Override
			public void submit(Runnable task) {
				pool.execute(task);
			}
			@Override
			public void stop(long timeout, TimeUnit unit) {
				pool.shutdown();
				try {
					pool.awaitTermination(timeout, unit);
				} catch (InterruptedException ignore) {}
				pool.shutdownNow();
			}
		};
	}
	
	@Override
	public PrioritizedPool newPrioritizedPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut) {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, unit,
				new PriorityBlockingQueue<>(1, (o1, o2) -> Integer.compare(((PrioritizedRunnable) o1).priority(), ((PrioritizedRunnable) o2).priority())),
				new AndroidBgThreadFactory(name));
		pool.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
		return (task, priority) -> pool.execute(new PrioritizedRunnable() {
			@Override
			public int priority() {
				return priority;
			}
			@Override
			public void run() {
				task.run();
			}
		});
	}
	
}
