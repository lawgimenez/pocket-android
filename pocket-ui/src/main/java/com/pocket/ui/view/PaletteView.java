package com.pocket.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;

import java.util.ArrayList;
import java.util.List;

public class PaletteView extends View {
	
	private final List<int[]> rows = new ArrayList<>();
	private final RectF rect = new RectF();
	private int swatchPx;
	private int spacePx;
	private Paint paint;
	private float cornerRadius;
	
	public PaletteView(Context context) {
		super(context);
		init();
	}
	
	public PaletteView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public PaletteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public PaletteView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}
	
	private void init() {
		swatchPx = getResources().getDimensionPixelSize(R.dimen.pkt_space_md);
		spacePx = getResources().getDimensionPixelSize(R.dimen.pkt_space_sm);
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStyle(Paint.Style.FILL);
		cornerRadius = DimenUtil.dpToPx(getContext(), 3);
	}
	
	public void addRow(int... colorResIds) {
		int[] colors = new  int[colorResIds.length];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = getResources().getColor(colorResIds[i]);
		}
		rows.add(colors);
		invalidate();
		requestLayout();
	}
	
	public void clearRows() {
		rows.clear();
		invalidate();
		requestLayout();
	}
	
	@Override
	public int getSuggestedMinimumWidth() {
		int longestRow = 0;
		for (int[] row : rows) {
			longestRow = Math.max(longestRow, row.length);
		}
		return longestRow * (swatchPx + spacePx) - spacePx;
	}
	
	@Override
	protected int getSuggestedMinimumHeight() {
		return rows.size() * (swatchPx + spacePx) - spacePx;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		float x = 0;
		float y = 0;
		for (int[] row :  rows) {
			for (int color : row) {
				paint.setColor(color);
				rect.set(x, y, x+swatchPx, y+swatchPx);
				canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
				x += swatchPx;
				x += spacePx;
			}
			x = 0;
			y += swatchPx;
			y += spacePx;
		}
	}
}
