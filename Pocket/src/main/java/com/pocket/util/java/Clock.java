package com.pocket.util.java;

import android.os.SystemClock;

/**
 * An injectable wrapper around System.currentTimeMillis().
 */
public interface Clock {
	long now();

	Clock SYSTEM = System::currentTimeMillis;
	Clock ELAPSED_REALTIME = SystemClock::elapsedRealtime;
}
