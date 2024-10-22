package com.pocket.ui.view.badge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;

import org.apache.commons.lang3.ArrayUtils;

/**
 * The background bounding box for use in various Badge views.
 * Maybe could be replaced with just an xml shape drawable.
 */
class BadgeDrawable extends Drawable {
	
	private final RectF mRectFill = new RectF();
	private final Paint mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ColorStateList mDisabledColorFill;
	private final ColorStateList mColorFill;
	private float mCornerRadius;
	
	BadgeDrawable(Context context, ColorStateList color) {
		mPaintFill.setAntiAlias(true);
		mPaintFill.setDither(true);
		mPaintFill.setStyle(Paint.Style.FILL);
		mCornerRadius = DimenUtil.dpToPx(context, 4);
		mColorFill = color;
		mDisabledColorFill = ContextCompat.getColorStateList(context, R.color.pkt_badge_disabled);
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		updateDrawComponents();
		return true;
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		updateDrawComponents();
	}
	
	private void updateDrawComponents() {
		if  (mPaintFill == null) {
			return; // not initialized yet
		}
		final int currentFill = mPaintFill.getColor();
		
		int[] state = getState();
		int fill = mColorFill.getColorForState(state, Color.TRANSPARENT);
		if (!ArrayUtils.contains(state, android.R.attr.state_enabled)) {
			fill = mDisabledColorFill.getColorForState(state, fill);
		}
		
		mPaintFill.setColor(fill);
		
		if (currentFill != fill) {
			/*
			 * Workaround for a quirk in the TextView. If the text color selector doesn't change
			 * color, then it doesn't invalidate on state change. So if we detect our custom paints
			 * have changed color, we force the invalidate to ensure it redraws.
			 */
			invalidateSelf();
		}
		mRectFill.set(getBounds());
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		canvas.drawRoundRect(mRectFill, mCornerRadius, mCornerRadius, mPaintFill);
	}
	
	@Override
	public void setAlpha(int alpha) {
		mPaintFill.setAlpha(alpha);
		invalidateSelf();
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		mPaintFill.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
}
