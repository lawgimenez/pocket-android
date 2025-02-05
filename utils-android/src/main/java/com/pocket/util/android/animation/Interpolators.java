package com.pocket.util.android.animation;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import nl.codesoup.cubicbezier.CubicBezierInterpolator;

/**
 * Constants for interpolators that can be used instead of making new ones all the time.
 */
public class Interpolators {
	
	public static final DecelerateInterpolator DECEL = new DecelerateInterpolator();
	public static final AccelerateInterpolator ACCEL = new AccelerateInterpolator();
	public static final LinearInterpolator LINEAR = new LinearInterpolator();
	public static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator();
	public static final CubicBezierInterpolator CB_FAST_OUT_LINEAR_IN = new CubicBezierInterpolator(0.4f, 0, 1, 1);
}
