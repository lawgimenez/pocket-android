package com.pocket.sdk.util.wakelock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pocket.app.App;

/**
 * A receiver that holds a wake lock during its onReceive method.
 */
public abstract class WakeLockBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLockHolder holder = WakeLockHolder.withTimeout(getClass().getSimpleName(), 0, 1, null);
        App.from(context).wakelocks().acquire(holder);
        doOnReceive(context, intent);
		App.from(context).wakelocks().release(holder);
    }

    public abstract void doOnReceive(Context context, Intent intent);

}
