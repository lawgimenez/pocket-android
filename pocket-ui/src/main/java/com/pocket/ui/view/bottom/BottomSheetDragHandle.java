package com.pocket.ui.view.bottom;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.themed.ThemedView;

import androidx.core.content.ContextCompat;

import static android.view.View.MeasureSpec.EXACTLY;

/**
 * A standard drag handle to include at the top of bottom sheets in Pocket.
 * Use width and height set to wrap_content for standard sizing. Custom width and/or height (whether bigger or smaller)
 * will be respected for cases when it's needed.
 */
public final class BottomSheetDragHandle extends ThemedView {
	
	private final Paint paint = new Paint();
	private final RectF bounds = new RectF();
	
	private final ColorStateList handleColor;
	
	public BottomSheetDragHandle(Context context) {
		this(context, null);
	}
	
	public BottomSheetDragHandle(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		handleColor = ContextCompat.getColorStateList(context, R.color.pkt_themed_grey_5);
	}
	
	@Override protected int getSuggestedMinimumHeight() {
		return DimenUtil.dpToPxInt(getContext(), 6) + getPaddingTop() + getPaddingBottom();
	}
	
	@Override protected int getSuggestedMinimumWidth() {
		return DimenUtil.dpToPxInt(getContext(), 70) + getPaddingLeft() + getPaddingRight();
	}
	
	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(getSuggestedSize(getSuggestedMinimumWidth(), widthMeasureSpec),
				getSuggestedSize(getSuggestedMinimumHeight(), heightMeasureSpec));

		bounds.set(getPaddingLeft(),
				getPaddingTop(),
				getMeasuredWidth() - getPaddingRight(),
				getMeasuredHeight() - getPaddingBottom());
	}
	
	@Override protected void onDraw(Canvas canvas) {
		updatePaint(getDrawableState());
		final float radius = Math.min(bounds.bottom - bounds.top, bounds.right - bounds.left) / 2;
		canvas.drawRoundRect(bounds, radius, radius, paint);
	}
	
	private void updatePaint(int[] state) {
		paint.setColor(handleColor.getColorForState(state, Color.TRANSPARENT));
	}
	
	private static int getSuggestedSize(int size, int measureSpec) {
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		
		switch (specMode) {
			case EXACTLY:
				return specSize;
				
			case MeasureSpec.AT_MOST:
				return Math.min(size, specSize);
			
			case MeasureSpec.UNSPECIFIED:
			default:
				return size;
		}
	}
}
