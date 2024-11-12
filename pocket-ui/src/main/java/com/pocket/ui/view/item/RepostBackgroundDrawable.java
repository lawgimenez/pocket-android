package com.pocket.ui.view.item;

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
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * TODO we have a few rounded rect custom drawables, might be able to share code
 */
class RepostBackgroundDrawable extends Drawable {
	
	private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ColorStateList strokeColors;
	private final Shape stroke;
	
	private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ColorStateList fillColors;
	private final Shape fill;
	
	RepostBackgroundDrawable(Context context) {
		float radius = DimenUtil.dpToPx(context, 4);
		float strokeWidth = DimenUtil.dpToPx(context, 1);
		
		float radii[] = new float[]{radius, radius, radius, radius, radius, radius, radius, radius};
		stroke = new RoundRectShape(radii, new RectF(strokeWidth, strokeWidth, strokeWidth, strokeWidth), radii); // Using a Shape, since canvas.drawRoundRect with a stroke has terrible quality, Shape produces a cleaner stroked rounded rect
		fill = new RoundRectShape(radii, null, null);
		
		strokeColors = NestedColorStateList.get(context, R.color.pkt_themed_grey_5);
		fillColors = NestedColorStateList.get(context, R.color.pkt_touchable_area);
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		boolean r = super.onStateChange(state);
		int newFillColor = fillColors.getColorForState(state, Color.TRANSPARENT);
		int newStrokeColor = strokeColors.getColorForState(state, Color.TRANSPARENT);
		
		if (newFillColor != fillPaint.getColor() || newStrokeColor != strokePaint.getColor())  {
			invalidateSelf();
			fillPaint.setColor(newFillColor);
			strokePaint.setColor(newStrokeColor);
			return true;
		} else {
			return r;
		}
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		stroke.resize(bounds.width(), bounds.height());
		fill.resize(bounds.width(), bounds.height());
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		if (fillPaint.getColor() != Color.TRANSPARENT) {
			fill.draw(canvas, fillPaint);
		}
		stroke.draw(canvas, strokePaint);
	}
	
	@Override
	public void setAlpha(int alpha) {
		fillPaint.setAlpha(alpha);
		strokePaint.setAlpha(alpha);
		invalidateSelf();
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		fillPaint.setColorFilter(colorFilter);
		strokePaint.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
}
