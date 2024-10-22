package com.pocket.sync.source.threads;

import android.os.Handler;
import android.os.Looper;

/**
 * A {@link Publisher} that runs callbacks on the Android main/ui thread.
 */
public class AndroidUiThreadPublisher implements Publisher {
	
	private final Handler handler;
	private final Thread thread;
	
	public AndroidUiThreadPublisher() {
		Looper looper = Looper.getMainLooper();
		this.handler = new Handler(looper);
		this.thread = looper.getThread();
	}
	
	@Override
	public void publish(Runnable runnable) {
		if (Thread.currentThread().equals(thread)) {
			runnable.run();
		} else {
			handler.post(runnable);
		}
	}
	
}
