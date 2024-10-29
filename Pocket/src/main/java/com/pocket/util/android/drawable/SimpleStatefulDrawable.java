package com.pocket.util.android.drawable;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;

/**
 * Contains some of the boilerplate for custom drawables we write over and over again.
 * Register paints with {@link #registerPaint(Paint)}.
 */
public abstract class SimpleStatefulDrawable extends Drawable {

	private final ArrayList<Paint> paints = new ArrayList<Paint>();
	
	protected void registerPaint(Paint paint) {
		paints.add(paint);
		paint.setAntiAlias(true);
	}
	
	@Override
	public void setAlpha(int alpha) {
		for (Paint paint : paints) {
			paint.setAlpha(alpha);
		}
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		for (Paint paint : paints) {
			paint.setColorFilter(cf);
		}
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		super.onStateChange(state);
		for (Paint paint : paints) {
			if (paint instanceof StatefulPaint) {
				((StatefulPaint) paint).setState(state);
			}
		}
		invalidateSelf();
		return true;
	}

}

