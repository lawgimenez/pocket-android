package com.pocket.util.java;

import com.pocket.app.AppMode;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility for logging and controlling logging in various builds.
 * Setup {@link #logger(Logger)} and {@link #mode(AppMode)} at the startup of your app,
 */
public class Logs {
	
	public interface Logger {
		void v(String tag, String log);
		void w(String tag, String log);
		void e(String tag, String log);
		void i(String tag, String log);
		void d(String tag, String log);
		void printStackTrace(Throwable t);
	}
	
	public static final Logger SOUT = new Logger() {
		
		@Override
		public void v(String tag, String log) {
			System.out.println(tag + ": " + log);
		}
		
		@Override
		public void w(String tag, String log) {
			System.out.println(tag + ": " + log);
		}
		
		@Override
		public void e(String tag, String log) {
			System.out.println(tag + ": " + log);
		}
		
		@Override
		public void i(String tag, String log) {
			System.out.println(tag + ": " + log);
		}
		
		@Override
		public void d(String tag, String log) {
			System.out.println(tag + ": " + log);
		}
		
		@Override
		public void printStackTrace(Throwable t) {
			t.printStackTrace();
		}
	};
	
	public static final Logger OFF = new Logger() {
		@Override public void v(String tag, String log) {}
		@Override public void w(String tag, String log) {}
		@Override public void e(String tag, String log) {}
		@Override public void i(String tag, String log) {}
		@Override public void d(String tag, String log) {}
		@Override public void printStackTrace(Throwable t) {}
	};
	
	private static Logger logger = OFF;
	private static AppMode mode;
	
	/** Where to output logs such as {@link com.pocket.util.android.AndroidLogger} or {@link #SOUT}. By default or if passing null, will use {@link #OFF}. */
	public static void logger(Logger value) {
		logger = value != null ? value : OFF;
	}
	
	/** What mode to use for methods like {@link #throwIfNotProduction(String)} */
	public static void mode(AppMode value) {
		mode = value;
	}
	
	public static void v(String tag, String log){
		logger.v(tag, log);
	}
	
	public static void d(String tag, String log){
		logger.d(tag, log);
	}
	
	public static void i(String tag, String log){
		logger.i(tag, log);
	}
	
	public static void w(String tag, String log){
		logger.w(tag, log);
	}
	
	public static void e(String tag, String log){
		logger.e(tag, log);
	}
	
	public static void l(String string) {
		v("ReadItLater", string);
	}

	public static void printStackTrace(Throwable t) {
		t.printStackTrace();
	}
	
	/**
	 * Convenience method for throwing a RuntimeException ONLY if the app is in beta or development mode.
	 * <p>
	 * If this unexpected state is safe to ignore in production, this can be used to inform developers of unexpected states without
	 * causing a crash for production users if for some reason it occurs.
	 * <p>
	 * If no mode has been set via {@link #mode(AppMode)}, this will act as if the mode is production.
	 */
	public static void throwIfNotProduction(String string) {
		if (mode != null && mode.isForInternalCompanyOnly()) {
			throw new RuntimeException(StringUtils.defaultString(string));
		}
	}
	
	/**
	 * Simple one line stack trace useful for debugging.
	 * @param depth How far into the stack to log. 1 just prints out the class, method and line number that invoked this method.
	 * @return A one line trace.
	 */
	public static String stack(int depth) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		String log = "";
		for (int i = 3; i < Math.min(3+depth, stack.length); i++) { // 3 starts at the line that invoked this method.
			log = log + " | " + stack[i].toString();
		}
		return log;
	}
	
//	/**
//	 * Starts logging memory usage, similar in break down to what you might see in Android Studio's profiler
//	 * These logs run on ALL builds, including release builds so this method is kept commented out when not used.
//	 * Likely should never be committed uncommented out.
//	 * <p>
//	 * Android Studio's profiler only runs on debuggable apps and also injects code that monitors network traffic
//	 * and other things that end up actually using memory! Also debug apps use a lot of code memory.
//	 * So running this on a release build and then charting it out can give a realistic picture of actual memory usage.
//	 * <p>
//	 * To create a chart, call this method once when you want to start logging, filter to MemoryLog, grab the logs,
//	 * convert to a csv and open in some spreadsheet software.
//	 */
//	@RequiresApi(api = Build.VERSION_CODES.M)
//	public static void logMemoryUsageInAllBuilds(Handler handler, Context context, long intervalMillis) {
//		final int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
//		long heapFree = Runtime.getRuntime().freeMemory();
//		long heapMax = Runtime.getRuntime().maxMemory();
//		long heapCurrent = Runtime.getRuntime().totalMemory();
//
//		Debug.MemoryInfo meminfo = new Debug.MemoryInfo();
//		Debug.getMemoryInfo(meminfo);
//
//		String line = "";
//		line += memClass + ",";
//		line += meminfo.getMemoryStat("summary.code") + ",";
//		line += meminfo.getMemoryStat("summary.stack") + ",";
//		line += meminfo.getMemoryStat("summary.graphics") + ",";
//		line += meminfo.getMemoryStat("summary.java-heap") + ",";
//		line += meminfo.getMemoryStat("summary.native-heap") + ",";
//		line += meminfo.getMemoryStat("summary.system") + ",";
//		line += meminfo.getMemoryStat("summary.total-pss") + ",";
//		line += meminfo.getMemoryStat("summary.private-other") + ",";
//		line += meminfo.getMemoryStat("summary.total-swap") + ",";
//		line += heapMax + ",";
//		line += heapCurrent + ",";
//		line += heapFree;
//
//		Log.e("MemoryLog", line);
//
//		handler.postDelayed(() -> logMemoryUsageInAllBuilds(handler, context, intervalMillis), intervalMillis);
//	}
	
	
}
