package com.pocket.ui.view.menu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Drawable for the selection indicator for {@link ThemeToggle}.
 */
public class ThemeToggleSelectionDrawable extends Drawable {
	
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ColorStateList color;
	private final float radius;
	
	public ThemeToggleSelectionDrawable(Context context) {
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(DimenUtil.dpToPx(context, 2));
		radius = DimenUtil.dpToPx(context, 23.5f);
		color = NestedColorStateList.get(context, R.color.pkt_themed_teal_2);
	}
	
	@Override
	public int getIntrinsicWidth() {
		return (int) Math.ceil(radius*2);
	}
	
	@Override
	public int getIntrinsicHeight() {
		return getIntrinsicWidth();
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		super.onStateChange(state);
		return true;
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		paint.setColor(color.getColorForState(getState(), Color.TRANSPARENT));
		Rect bounds = getBounds();
		canvas.drawCircle(bounds.centerX(), bounds.centerY(), radius, paint);
	}
	
	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
		invalidateSelf();
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		paint.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}
