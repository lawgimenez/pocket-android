package com.pocket.sync.source.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A publisher that has its own thread and publishes on that thread, one at a time, in the order received.
 */
public class ThreadPublisher implements Publisher {
	
	private final ExecutorService pool = Executors.newSingleThreadExecutor(OwnedThread::new);
	
	@Override
	public void publish(Runnable runnable) {
		if (Thread.currentThread() instanceof OwnedThread) {
			runnable.run();
		} else {
			pool.execute(runnable);
		}
	}

	/** A seperate class we can detect that we own and is running in our executor (which can restart threads during lifetime) */
	private static class OwnedThread extends Thread {
		OwnedThread(Runnable target) { super(target);}
	}
	
}
