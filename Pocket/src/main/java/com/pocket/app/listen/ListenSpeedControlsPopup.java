package com.pocket.app.listen;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ideashower.readitlater.databinding.ViewListenSpeedControlsBinding;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.util.android.ViewUtil;

public final class ListenSpeedControlsPopup extends PopupWindow {
	private final ViewListenSpeedControlsBinding views;
	
	ListenSpeedControlsPopup(Context context, int width, int height) {
		super(width, height);
		views = ViewListenSpeedControlsBinding.inflate(LayoutInflater.from(context));
		setContentView(views.getRoot());
		views.getRoot().setBackground(new Background(context));
		setFocusable(true);
		ViewUtil.setCancelOnOutsideTouch(this);
	}
	
	void setSpeed(CharSequence speed) {
		views.listenSpeed.setText(speed);
	}
	
	void setOnPlusClickListener(View.OnClickListener listener) {
		views.listenSpeedInc.setOnClickListener(listener);
	}
	
	void setOnMinusClickListener(View.OnClickListener listener) {
		views.listenSpeedDec.setOnClickListener(listener);
	}
	
	private static class Background extends Drawable {
		
		private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		private final ColorStateList fillColors;
		private final ColorStateList strokeColors;
		private final float strokeWidth;
		private final Shape shape;
		
		public Background(Context context) {
			float radius = DimenUtil.dpToPx(context, 27);
			shape = new RoundRectShape(new float[]{radius, radius, radius, radius, radius, radius, radius, radius},
					null,
					null);
			fillColors = NestedColorStateList.get(context, com.pocket.ui.R.color.pkt_bg);
			strokeColors = NestedColorStateList.get(context, com.pocket.ui.R.color.pkt_themed_grey_4);
			strokePaint.setStyle(Paint.Style.STROKE);
			strokeWidth = DimenUtil.dpToPx(context, 1);
			strokePaint.setStrokeWidth(strokeWidth);
			strokePaint.setStrokeJoin(Paint.Join.ROUND);
		}
		
		@Override
		public boolean isStateful() {
			return true;
		}
		
		@Override
		protected boolean onStateChange(int[] state) {
			boolean r = super.onStateChange(state);
			
			int newFillColor = fillColors.getColorForState(state, Color.TRANSPARENT);
			if (newFillColor != fillPaint.getColor()) {
				invalidateSelf();
				fillPaint.setColor(newFillColor);
				r = true;
			}
			
			final int newStrokeColor = strokeColors.getColorForState(state, Color.TRANSPARENT);
			if (newStrokeColor != strokePaint.getColor()) {
				invalidateSelf();
				strokePaint.setColor(newStrokeColor);
				r = true;
			}
			
			return r;
		}
		
		@Override
		protected void onBoundsChange(Rect bounds) {
			super.onBoundsChange(bounds);
			shape.resize(bounds.width() - 2 * strokeWidth, bounds.height() - 2 * strokeWidth);
		}
		
		@Override
		public void draw(@NonNull Canvas canvas) {
			canvas.save();
			canvas.translate(strokeWidth, strokeWidth);
			shape.draw(canvas, fillPaint);
			shape.draw(canvas, strokePaint);
			canvas.restore();
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
}
