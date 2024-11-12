package com.pocket.sdk2.api.legacy;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.pocket.app.App;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.thing.Item;

/**
 * Tools for capturing a bunch of info when an {@link OutOfMemoryError} occurs.
 * Likely can remove once we decide on what max number of item limits we will add to {@link com.pocket.sdk.Pocket}
 */
public class MemoryLimitLogging {
	
	public static void rethrowWithInfo(Throwable t, int thingCount, int itemCount) {
		throw new CaughtOomeError(gatherInfo(thingCount, itemCount), t);
	}
	
	public static String gatherInfo() {
		int thingCount = -1;
		int itemCount = -1;
		try {
			Pocket pocket = App.getApp().pocket();
			thingCount = pocket.count(null);
			itemCount = pocket.count(Item.THING_TYPE);
		} catch (Throwable ignore) {}
		return gatherInfo(thingCount, itemCount);
	}
	
	@SuppressLint("NewApi")
	public static String gatherInfo(int thingCount, int itemCount) {
		String out = "";
		
		// Strings are grouped together so if they throw an error they won't modify out
		
		// Capture thing counts
		try {
			out += thingCount + "," + itemCount + ",";
		} catch (Throwable ignore) {
			out += "-1,-1,";
		}
		
		// Memory state
		try {
			out += (
				((ActivityManager) App.getContext().getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() + "," +
				Runtime.getRuntime().freeMemory() + "," +
				Runtime.getRuntime().maxMemory() + "," +
				Runtime.getRuntime().totalMemory() + ","
			);
		} catch (Throwable ignore) {
			out += "-1,-1,-1,-1";
		}
		try {
			Debug.MemoryInfo meminfo = new Debug.MemoryInfo();
			Debug.getMemoryInfo(meminfo);
			out += (
				meminfo.getMemoryStat("summary.code") + "," +
				meminfo.getMemoryStat("summary.stack") + "," +
				meminfo.getMemoryStat("summary.graphics") + "," +
				meminfo.getMemoryStat("summary.java-heap") + "," +
				meminfo.getMemoryStat("summary.native-heap") + "," +
				meminfo.getMemoryStat("summary.system") + "," +
				meminfo.getMemoryStat("summary.total-pss") + "," +
				meminfo.getMemoryStat("summary.private-other") + "," +
				meminfo.getMemoryStat("summary.total-swap") + ","
			);
		} catch (Throwable ignore) {
			out += "-1,-1,-1,-1,-1,-1,-1,-1,-1,";
		}
		
		return out;
	}
	
	static class CaughtOomeError extends RuntimeException {
		
		public CaughtOomeError(String detailMessage, Throwable throwable) {
			super(detailMessage, throwable);
		}
		
	}
	
}
