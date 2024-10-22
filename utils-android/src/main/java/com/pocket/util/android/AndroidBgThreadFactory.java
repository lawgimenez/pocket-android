package com.pocket.util.android;

import android.os.Process;

import java.util.concurrent.ThreadFactory;

/**
 * A thread factory that ensures it runs with the recommended background thread priority in Android
 */
public class AndroidBgThreadFactory implements ThreadFactory {
	
	public static ThreadFactory wrap(ThreadFactory factory, String name) {
		return r -> {
			Thread t = factory.newThread(() -> {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				r.run();
			});
			if (name != null) t.setName(name);
			return t;
		};
	}
	
	private final String name;
	private int count;
	
	public AndroidBgThreadFactory() {
		this(null);
	}
	
	public AndroidBgThreadFactory(String name) {
		this.name = name;
	}
	
	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
				r.run();
			}
		};
		if (name != null) thread.setName(name + "-" + count++);
		return thread;
	}
	
}
