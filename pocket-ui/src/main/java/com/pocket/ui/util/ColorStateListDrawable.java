package com.pocket.ui.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * A drawable that fills its bounds with a single color, from a {@link ColorStateList}, for the current drawable state.
 * <p>
 * Similar to {@link ColorDrawable} but supports {@link ColorStateList}.
 * 
 */
public class ColorStateListDrawable extends Drawable {
	
	private final Paint mPaint;
	private final ColorStateList mColors;
	
	private int mAlpha = 255;

	private RectF mRoundedCornerRect;
	private float mCornerRadius = 0;

	public ColorStateListDrawable(Context context, int colorStateListRes, float cornerRadius) {
		this(ContextCompat.getColorStateList(context, colorStateListRes));
		this.mCornerRadius = cornerRadius;
		this.mRoundedCornerRect = cornerRadius > 0 ? new RectF() : null;
	}

	public ColorStateListDrawable(Context context, int colorStateListRes) {
		this(ContextCompat.getColorStateList(context, colorStateListRes));
	}
	
	public ColorStateListDrawable(ColorStateList color) {
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mColors = color;
	}

	@Override
	protected boolean onStateChange(int[] state) {
		boolean rt = super.onStateChange(state);
		int newColor = mColors.getColorForState(state, Color.TRANSPARENT);
		if (mPaint.getColor() != newColor) {
			mPaint.setColor(newColor);
			mPaint.setAlpha(mAlpha);
			return true;
		} else {
			return rt;
		}
	}

	@Override
	protected void onBoundsChange (Rect bounds) {
		super.onBoundsChange(bounds);
		if (mRoundedCornerRect != null) {
			mRoundedCornerRect.set(bounds);
		}
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		if (mRoundedCornerRect != null) {
			canvas.drawRoundRect(mRoundedCornerRect, mCornerRadius, mCornerRadius, mPaint);
		} else {
			canvas.drawRect(getBounds(), mPaint);
		}
	}

	@Override
	public void setAlpha(int alpha) {
		if (alpha == mAlpha) {
			return;
		}
		mPaint.setAlpha(alpha);
		mAlpha = alpha;
		invalidateSelf();
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		if (mPaint.getColorFilter() == cf) {
			return;
		}
		mPaint.setColorFilter(cf);
		invalidateSelf();
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT; // Dependent on the actual color being drawn, just assume alpha.
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
}
