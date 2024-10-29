package com.pocket.ui.view.button;

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

import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ButtonBoxDrawable extends Drawable {
	
	private final RectF mRectFill = new RectF();
	private final RectF mRectStroke = new RectF();
	private final Paint mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mPaintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final float mOutlineStroke;
	
	private ColorStateList mColorFill;
	private ColorStateList mColorStroke;
	private boolean mHasOutline;
	private float mCornerRadius;
	private CornerStyle mCornerStyle;
	private int mAlpha = 255;

	public enum CornerStyle {
		/** Round the top corners */
		TOP,
		/** Round the bottom corners */
		BOTTOM,
		/** Round all corners */
		ALL
	}
	
	public ButtonBoxDrawable(Context context, int fillColors, int strokeColors) {
		this(context, fillColors,  strokeColors, DimenUtil.dpToPx(context, 4), DimenUtil.dpToPx(context, 1), CornerStyle.ALL);
	}

	public ButtonBoxDrawable(Context context, int fillColors, int strokeColors, float cornerRadius) {
		this(context, fillColors,  strokeColors, cornerRadius, DimenUtil.dpToPx(context, 1), CornerStyle.ALL);
	}

	public ButtonBoxDrawable(Context context, int fillColors, CornerStyle cornerStyle) {
		this(context, fillColors,  0, DimenUtil.dpToPx(context, 4), 0, cornerStyle);
	}

	public ButtonBoxDrawable(Context context, int fillColors, int strokeColors, float cornerRadius, float outlineStroke, CornerStyle cornerStyle) {
		mPaintFill.setAntiAlias(true);
		mPaintFill.setDither(true);
		mPaintFill.setStyle(Paint.Style.FILL);
		
		mPaintStroke.setStyle(Paint.Style.FILL);
		
		mCornerRadius = cornerRadius;
		mCornerStyle = cornerStyle;
		mOutlineStroke = outlineStroke;
		
		mColorFill =  fillColors != 0 ? NestedColorStateList.get(context, fillColors) : null;
		mColorStroke = strokeColors != 0 ? NestedColorStateList.get(context, strokeColors) : null;
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
		final int currentStroke = mPaintStroke.getColor();
		
		int[] state = getState();
		int fill = mColorFill != null ? mColorFill.getColorForState(state, Color.TRANSPARENT) : Color.TRANSPARENT;
		int stroke = mColorStroke != null ? mColorStroke.getColorForState(state, Color.TRANSPARENT) : Color.TRANSPARENT;
		
		mPaintFill.setColor(fill);
		mPaintStroke.setColor(stroke);
		mPaintFill.setAlpha(mAlpha);
		mPaintStroke.setAlpha(mAlpha);
		mHasOutline = stroke != Color.TRANSPARENT;
		
		if (currentFill != fill || currentStroke != stroke) {
			/*
			 * Workaround for a quirk in the TextView. If the text color selector doesn't change
			 * color, then it doesn't invalidate on state change. So if we detect our custom paints
			 * have changed color, we force the invalidate to ensure it redraws.
			 */
			invalidateSelf();
		}
		
		float strokeWidth = mHasOutline ? mOutlineStroke : 0;
		mRectStroke.set(getBounds());
		mRectFill.set(mRectStroke);
		mRectFill.inset(strokeWidth, strokeWidth);
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		/*
		 * Note:
		 * Borders are drawn as rectangles instead of strokes because on some devices, strokes end up being different for different corners
		 * and producing inconsistent results. For example on a first gen N7, every single corner would be a different curve and radius.
		 * This method looks better.
		 */
		if (mHasOutline) {
			canvas.drawRoundRect(mRectStroke, mCornerRadius, mCornerRadius, mPaintStroke);
		}
		canvas.drawRoundRect(mRectFill, mCornerRadius, mCornerRadius, mPaintFill);

		switch (mCornerStyle) {
			case TOP: // fill the bottom corners
				canvas.drawRect(mRectFill.left, mRectFill.bottom - mCornerRadius, mRectFill.right, mRectFill.bottom, mPaintFill);
				break;
			case BOTTOM: // fill the top corners
				canvas.drawRect(mRectFill.left, mRectFill.top, mRectFill.right, mRectFill.top + mCornerRadius, mPaintFill);
				break;
			case ALL: // nothing to do here
				break;
		}
	}
	
	@Override
	public void setAlpha(int alpha) {
		mAlpha = alpha;
		invalidateSelf();
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		mPaintFill.setColorFilter(colorFilter);
		mPaintStroke.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
}
