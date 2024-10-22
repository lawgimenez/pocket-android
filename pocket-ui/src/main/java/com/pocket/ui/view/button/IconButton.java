package com.pocket.ui.view.button;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.pocket.ui.R;
import com.pocket.ui.view.checkable.CheckableImageView;

import org.apache.commons.lang3.ArrayUtils;

import androidx.annotation.DimenRes;
import androidx.appcompat.widget.TooltipCompat;

public class IconButton extends CheckableImageView {

	public IconButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}

	public IconButton(Context context, AttributeSet attrs) {
		super(context, attrs, R.attr.iconButtonStyle);
		init(attrs);
	}
	
	public IconButton(Context context) {
		super(context);
		init(null);
	}

	private void init(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.IconButton);
			ColorStateList checkedOverride = a.getColorStateList(R.styleable.IconButton_checkedDrawableColor);
			if (checkedOverride != null) {
				setDrawableColorOverride((state, color) -> {
					if (isEnabled() &&  ArrayUtils.contains(state, android.R.attr.state_checked)) {
						// Only override checked color when view is enabled
						return checkedOverride.getColorForState(state, color);
					} else {
						return color;
					}
				});
			}
			a.recycle();
		}
	}

	@Override
	public void setContentDescription(CharSequence contentDescription) {
		super.setContentDescription(contentDescription);
		TooltipCompat.setTooltipText(this, contentDescription);
	}

	/**
	 * This is intended for icons which are aligned to the left/start of the screen.
	 *
	 * Icon buttons are 50dp, but their icons are a variable width, creating a visual margin/padding that depends on the size of the icon.
	 * This sets the margin to account for the icon size in order to visually align an icon to the left/start with the app's standard side margin,
	 * pkt_side_grid.
	 */
	public void setSideMarginStart() {
		setIconSideMargin(R.dimen.pkt_side_grid, true);
	}

	/**
	 * This is intended for icons which are aligned to the right/end of the screen.
	 *
	 * Icon buttons are 50dp, but their icons are a variable width, creating a visual margin/padding that depends on the size of the icon.
	 * This sets the margin to account for the icon size in order to visually align an icon to the right/end with the app's standard side margin,
	 * pkt_side_grid.
	 */
	public void setSideMarginEnd() {
		setIconSideMargin(R.dimen.pkt_side_grid, false);
	}

	public void setVisualMarginStart(@DimenRes int margin) {
		setIconSideMargin(margin, true);
	}

	public void setVisualMarginEnd(@DimenRes int margin) {
		setIconSideMargin(margin, false);
	}

	private void setIconSideMargin(@DimenRes int margin, boolean start) {
		final Context context = getContext();
		int iconWidth = context.getResources().getDimensionPixelSize(R.dimen.pkt_icon_button_width);																		// full width of the icon (eg 50dp)
		int drawableWidth = getDrawable().getIntrinsicWidth();										// width of the image inside it (variable width)
		int visualMargin = (iconWidth - drawableWidth) / 2;											// intrinsic visual margin
		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
		if (start) {																				// set margin to pkt_side_grid minus what's already visually there
			params.leftMargin = context.getResources().getDimensionPixelSize(margin) - visualMargin;
		} else {
			params.rightMargin = context.getResources().getDimensionPixelSize(margin) - visualMargin;
		}
		setLayoutParams(params);
	}

}
