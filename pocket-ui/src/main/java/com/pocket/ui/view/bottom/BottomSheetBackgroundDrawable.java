package com.pocket.ui.view.bottom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The background of a bottom sheet. Required to be a Drawable since xml shapes don't support themed colors.
 * No intrinsic bounds, fills the set bounds.
 */
public class BottomSheetBackgroundDrawable extends Drawable {
	
	private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final ColorStateList fillColors;
	private final Shape fill;
	private final int shadowSize;

	public BottomSheetBackgroundDrawable(Context context) {
		float radius = DimenUtil.dpToPx(context, 16);
		fill = new RoundRectShape(new float[]{radius, radius, radius, radius, 0,0,0,0}, null, null); // Rounded tops, square bottoms
		fillColors = NestedColorStateList.get(context, R.color.pkt_bg);

		shadowSize = (int) context.getResources().getDimension(R.dimen.pkt_drawer_shadow_radius);
		// simulating box-shadow: 0px -3px 6px rgba(0, 0, 0, 0.06);
		fillPaint.setShadowLayer(shadowSize, 0.0f, 0.0f, 0x10000000);
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		boolean r = super.onStateChange(state);
		int newFillColor = fillColors.getColorForState(state, Color.TRANSPARENT);
		if (newFillColor != fillPaint.getColor())  {
			invalidateSelf();
			fillPaint.setColor(newFillColor);
			return true;
		} else {
			return r;
		}
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		fill.resize(bounds.width(), bounds.height());
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		canvas.translate(0, shadowSize);
		fill.draw(canvas, fillPaint);
	}
	
	@Override
	public void setAlpha(int alpha) {
		fillPaint.setAlpha(alpha);
		invalidateSelf();
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		fillPaint.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
}
