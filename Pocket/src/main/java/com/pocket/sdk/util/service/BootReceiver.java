package com.pocket.sdk.util.service;

import android.content.Context;
import android.content.Intent;

import com.pocket.app.App;
import com.pocket.app.AppLifecycle;
import com.pocket.sdk.util.wakelock.WakeLockBroadcastReceiver;

public class BootReceiver extends WakeLockBroadcastReceiver {

    @Override
    public void doOnReceive(Context context, Intent intent) {
        // WARNING: This is an exported receiver. Extras could come from outside apps and may not be trust worthy.
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        App.from(context).dispatcher().dispatch(AppLifecycle::onDeviceBoot);
    }
}
