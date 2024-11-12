package com.pocket.util.android.animation;

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
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
	public static final AccelerateDecelerateInterpolator ACCEL_DECEL = new AccelerateDecelerateInterpolator();
	public static final LinearInterpolator LINEAR = new LinearInterpolator();
	public static final OvershootInterpolator OVERSHOOT = new OvershootInterpolator();
	public static final AnticipateOvershootInterpolator ANTICIPATE_OVERSHOOT = new AnticipateOvershootInterpolator();
	public static final AnticipateInterpolator ANTICIPATE = new AnticipateInterpolator();
	public static final CubicBezierInterpolator CB_LINEAR_OUT_SLOW_IN = new CubicBezierInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    public static final CubicBezierInterpolator CB_FAST_OUT_LINEAR_IN = new CubicBezierInterpolator(0.4f, 0, 1, 1);
    public static final CubicBezierInterpolator CB_FAST_OUT_SLOW_IN = new CubicBezierInterpolator(0.4f, 0, 0.2f, 1);

}
