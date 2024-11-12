package com.pocket.app.updated;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pocket.app.App;

public class UpdatedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// WARNING: This is an exported receiver. Extras could come from outside apps and may not be trust worthy.
		if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;
		App.from(context).versioning().onAppUpdateReceiver();
	}
}
