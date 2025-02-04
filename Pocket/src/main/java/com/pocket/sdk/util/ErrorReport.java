package com.pocket.sdk.util;

import android.app.Activity;

import com.pocket.app.App;
import com.pocket.app.help.Help;

/**
 * Details on an error for passing to {@link Help}
 */
public class ErrorReport {

	/** The screen they were in when it occurred if known, otherwise null. */
	public final String activity;
	/** The cause of the error, if known. */
	public final Throwable cause;
	/** The message that was displayed to the user, this helps support know what they saw in app. */
	public final String messageSeenByUser;
	
	public ErrorReport(Throwable cause, String messageSeenByUser) {
		this.messageSeenByUser = messageSeenByUser;
		this.cause = cause;
		Activity activity = App.getActivityContext();
		if(activity != null){
			this.activity = activity.getClass().getSimpleName().replace("Activity", "");
		} else {
			this.activity = null;
		}
	}
}
