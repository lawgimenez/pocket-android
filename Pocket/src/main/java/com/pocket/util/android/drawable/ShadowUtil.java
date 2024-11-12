package com.pocket.util.android.drawable;

import android.graphics.Paint;
import android.widget.TextView;

/**
 * Methods to account for a native crash that occurs if a shadow radius is over 25 px.
 *
 * See: https://code.google.com/p/android/issues/detail?id=73886
 */
public abstract class ShadowUtil {

    public static final float MAX_RADIUS = 25;

    public static float getSafeRadius(float radius) {
        return Math.min(MAX_RADIUS, radius);
    }

    public static void setShadowLayer(TextView textView, float radius, float dx, float dy, int color) {
        textView.setShadowLayer(getSafeRadius(radius), dx, dy, color);
    }

    public static void setShadowLayer(Paint paint, float radius, float dx, float dy, int color) {
        paint.setShadowLayer(getSafeRadius(radius), dx, dy, color);
    }
}
