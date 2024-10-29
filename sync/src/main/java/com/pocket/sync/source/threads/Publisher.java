package com.pocket.sync.source.threads;

import com.pocket.sync.source.AsyncSource;
import com.pocket.sync.source.subscribe.Subscriber;

/**
 * Controls where/what thread a {@link AsyncSource} will publish its callbacks.
 * May also be used by sources to control where {@link Subscriber} updates are invoked.
 */
public interface Publisher {
	
	/**
	 * Run this runnable on the thread that you want the callback to be invoked on.
	 * If this is called on the thread(s) belonging to this publisher, it should invoke it immediately now on this calling thread.
	 * That latter convention is important to avoid deadlocks in some cases.
	 */
	void publish(Runnable runnable);
	
	/** Just invokes immediately on the thread it is already on. */
	Publisher CALLING_THREAD = Runnable::run;
	
}
