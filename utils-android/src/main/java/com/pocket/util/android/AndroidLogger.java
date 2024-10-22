package com.pocket.util.android;

import android.util.Log;

import com.pocket.util.java.Logs;

public class AndroidLogger implements Logs.Logger {
	
	@Override
	public void v(String tag, String log) {
		Log.v(tag, log);
	}
	
	@Override
	public void w(String tag, String log) {
		Log.w(tag, log);
	}
	
	@Override
	public void e(String tag, String log) {
		Log.e(tag, log);
	}
	
	@Override
	public void i(String tag, String log) {
		Log.i(tag, log);
	}
	
	@Override
	public void d(String tag, String log) {
		Log.d(tag, log);
	}
	
	@Override
	public void printStackTrace(Throwable t) {
		t.printStackTrace();
	}
}
