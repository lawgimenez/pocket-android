package com.pocket.util.android.drawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A {@link Paint} that changes its color value based on state. Invoke {@link #setState(int[])} whenever
 * the state changes and it will update its color as needed.
 */
public class StatefulPaint extends Paint {
	
	private ColorStateList mColors;

	public StatefulPaint(Context context, int colorResourceId) {
		this(context.getResources(), colorResourceId);
	}

	public StatefulPaint(Resources res, int colorResourceId) {
		this();
		mColors = res.getColorStateList(colorResourceId);
	}
	
	public StatefulPaint() {
		super();
		setAntiAlias(true);
	}
	
	public void setStatefulColor(ColorStateList color, int[] state) {
		mColors = color;
		setState(state);
	}
	
	/**
	 * Set the drawable state. The paint will automatically adjust to the color matching that state.
	 * 
	 * @param state
	 * @return true if changed, false if the color is the same as before.
	 */
	public boolean setState(int[] state) {
		int newColor;
		if (mColors != null) {
			newColor = mColors.getColorForState(state, Color.TRANSPARENT);
		} else {
			newColor = Color.TRANSPARENT;
		}
		
		if (getColor() == newColor) {
			return false;
		}
		
		setColor(newColor);
		return true;
	}

	public boolean hasColor() {
		return mColors != null;
	}

}
