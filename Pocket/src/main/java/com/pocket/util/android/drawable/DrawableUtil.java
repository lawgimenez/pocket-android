package com.pocket.util.android.drawable;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public abstract class DrawableUtil {

    /**
     * Applies the motion events x and y to {@link Drawable#setHotspot(float, float)}.
     *
     * @param drawable ok as null, it just do nothing.
     */
    public static void setHotspot(Drawable drawable, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && drawable != null) {
            drawable.setHotspot(event.getX(), event.getY());
        }
    }
}
