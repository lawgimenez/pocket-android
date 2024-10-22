package com.pocket.util.android.animation;

import android.os.SystemClock;

public class AnimationUtil {
	
	public static class AnimationValues {
		public long elapsedTotal;
		public int repeatCount;
		public long currentElapsed;
		public float currentPercent;
		
		public AnimationValues(){}
	}
	
	public static void getAnimationValue(AnimationValues values, long start, long duration) {
		values.elapsedTotal = SystemClock.uptimeMillis() - start;
		values.repeatCount = (int) Math.floor(values.elapsedTotal / (double) duration);
		values.currentElapsed = values.elapsedTotal - (values.repeatCount * duration);
		values.currentPercent = values.currentElapsed / (float) duration;
	}

}
