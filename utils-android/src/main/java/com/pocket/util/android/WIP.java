package com.pocket.util.android;


import com.pocket.util.java.Logs;

/**
 * A set of Dev methods that should never ever ship. These methods are only for debugging in process and should be removed after use.
 * @author max
 *
 */
public class WIP { // DEV make sure none of these are used.
	
	/**
	 * A quick logging method. Meant to be used in places where logging has no value outside of debugging an immediate issue and will be removed after the issue is finished.
	 */
	public static void l(String log) {
		Logs.i("WIP", log);
	}
	
	/* If you want to reproduce out of memory errors call OOMETest.
	 * If you want it to happen faster you could probably change the delay value.
	 * Also make sure you set the path to an actual image file.

	public static void OOMETest() {
		OOMETest(new ArrayList<Bitmap>());
	}
	
	private static void OOMETest(final ArrayList<Bitmap> refHolder) {
		final String path = "/mnt/sdcard/Android/data/com.ideashower.readitlater.dev/files/RIL_offline/RIL_assets/public0.ordienetworks.com/assets/spinner-042016a130976cacff054f182bdb3323_80-58.jpg";
		App.getHandler().postDelayed(new Runnable(){
			
			@Override
			public void run() {
				Dev.i("ImageCache", "oome test running");
				
				Bitmap bitmap = null;
				try {
					bitmap = MemoryAwareBitmapFactory.decode(path, null);
					
				} catch (OutOfMemoryError oome) {
					// End of test
				}
				
				if (bitmap != null) {
					refHolder.add(bitmap);
					OOMETest(refHolder);
				}
			}
			
		}, 50);
	}
	
	*/

	/**
	 * This log will run even on production and release builds. DEV Make sure it is not used in real releases!
	 * 
	 * @param tag
	 */
	/*
	public static void releaseLog(String tag, String log) {
		Log.i(tag, log);
	}
	*/
	
//	public static void logTwitterRateLimits() {
//		if (!App.IS_DEV_MODE) {
//			if (AppMode.isForInternalCompanyOnly()) {
//				throw new RuntimeException();
//			}
//			return;
//		}
//
//		new Task() {
//
//			@Override
//			protected void backgroundOperation() throws Exception {
//				Twitter twitter = TwitterManager.getTwitter(App.getContext());
//				boolean filterToOnlyUsedEndpoints = true;
//				Time time = new Time();
//
//				try {
//					ObjectNode json = JsonUtil.newObjectNode();
//					Map<String, RateLimitStatus> status = twitter.getRateLimitStatus();
//					Set<Entry<String, RateLimitStatus>> set = status.entrySet();
//					for (Entry<String, RateLimitStatus> entry : set) {
//						if (filterToOnlyUsedEndpoints && entry.getValue().getRemaining() == entry.getValue().getLimit()) {
//							continue;
//						}
//
//						time.set(entry.getValue().getResetTimeInSeconds() * 1000L);
//
//						ObjectNode values = JsonUtil.newObjectNode();
//						values.put("remaining", entry.getValue().getRemaining());
//						values.put("limit", entry.getValue().getLimit());
//						values.put("secondsUntilReset", entry.getValue().getSecondsUntilReset());
//						values.put("resets at", (time.hour > 12 ? time.hour - 12 : time.hour) + ":" + time.minute + ":" + time.second);
//						json.put(entry.getKey(), values);
//					}
//
//					l(json.toString());
//
//				} catch (TwitterException e) {
//					Dev.printStackTrace(e);
//				}
//			}
//
//		}.execute();
//	}
	
	
}
