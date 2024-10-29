package com.pocket.ui.view.visualmargin;

import android.view.View;
import android.view.ViewGroup;

/**
 * A view that supports visual margins in {@link VisualMarginConstraintLayout}.
 */
public interface VisualMargin {
	/**
	 * @return The number of pixels the visual top of your view is from the bounding box top.
	 */
	int visualAscent();
	
	/**
	 * @return The number of pixels the visual bottom of your view is from the bounding box bottom.
	 */
	int visualDescent();
	
	/**
	 * This will be used as the bottom view of a visual margin calculation,
	 * and will later have {@link #visualAscent()} invoked.
	 * Most views don't need to do anything to prepare.
	 * This is provided for views that by default have some padding or margin along their
	 * top, but when used as a visual margin anchor are happy to discard it in favor of
	 * the visual margin that the parent view wants to use between this view and the one above it.
	 * @return true if layout changes were made, false if not
	 */
	boolean prepareVisualAscent();
	
	/**
	 * This will be used as the top view of a visual margin calculation,
	 * and will later have {@link #visualDescent()} invoked.
	 * Most views don't need to do anything to prepare.
	 * This is provided for views that by default have some padding or margin along their
	 * bottom, but when used as a visual margin anchor are happy to discard it in favor of
	 * the visual margin that the parent view wants to use between this view and the one below it.
	 * @return true if layout changes were made, false if not
	 */
	boolean prepareVisualDescent();
	
	/**
	 * Helper for setting a top margin to 0.
	 * @return true if changes were made
	 */
	static boolean removeTopMargin(View view) {
		ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
		if (lp.topMargin != 0) {
			lp.topMargin = 0;
			if (lp instanceof VisualMarginConstraintLayout.LayoutParams) {
				((VisualMarginConstraintLayout.LayoutParams) lp).visualMarginTop = 0;
			}
			view.setLayoutParams(lp);
			return true;
		}
		return false;
	}
}
