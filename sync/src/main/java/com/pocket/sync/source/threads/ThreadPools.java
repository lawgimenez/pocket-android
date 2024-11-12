package com.pocket.sync.source.threads;

import com.pocket.sync.source.Source;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Creates thread pools that internal implementations of a {@link Source} and or {@link com.pocket.sync.space.Space} can use.
 * This interface allows the big picture app/architecture that uses a source to have control/tracking over what threads it might need to create.
 * <p>
 * If you need help deciding about parameters can also take a look at the internals of {@link java.util.concurrent.Executors}'s static creators for examples.
 */
public interface ThreadPools {
	
	/**
	 * Creates a new thread pool.
	 * For parameter meanings, see {@link ThreadPoolExecutor)
	 */
	Pool newPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut);
	
	/**
	 * Creates a new thread pool.
	 * For parameter meanings, see {@link ThreadPoolExecutor)
	 */
	PrioritizedPool newPrioritizedPool(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, boolean allowCoreThreadTimeOut);
	
	interface Pool {
		void submit(Runnable task);
		/** Await tasks to complete and shut down pool. Block until shutdown*/
		void stop(long timeout, TimeUnit unit);
	}
	
	interface PrioritizedPool {
		/**
		 * Submit a task with the specified priority.
		 * @param priority Tasks with the lower number will be run first.
		 */
		void submit(Runnable task, int priority);
	}
	
	interface PrioritizedRunnable extends Runnable {
		/** @return the priority of this runnable. Tasks with the lower number will be run first. */
		int priority();
	}
	
}
