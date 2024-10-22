package com.pocket.sync.source.threads;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Creates thread pools using java defaults.
 */
public class JavaThreadPools implements ThreadPools {
	
	@Override
	public Pool newPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut) {
		ThreadPoolExecutor pool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>());
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
				new PriorityBlockingQueue<>(1, (o1, o2) -> Integer.compare(((PrioritizedRunnable) o1).priority(), ((PrioritizedRunnable) o2).priority())));
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
