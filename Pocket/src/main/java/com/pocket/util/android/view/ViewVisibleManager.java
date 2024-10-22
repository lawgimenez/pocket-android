package com.pocket.util.android.view;

import android.view.View;

import java.util.ArrayList;

/**
 * Controls whether or not a view is visible by checking a list of registered {@link ViewVisibilityCondition}'s.
 * To be enabled, all registered enablers must return true.
 * <p>
 * Anytime an enablers status changes, it should invoke {@link #invalidate()} to update the view's state..
 */
public class ViewVisibleManager {
	
	private final ArrayList<ViewVisibilityCondition> mEnablers = new ArrayList<ViewVisibilityCondition>();
	private final View mView;
	private final int mHiddenVisibilityFlag;
	
	/**
	 * @param view
	 * @param hiddenVisibility The visibility flag that should be used while not visible. Either {@link View#GONE} or {@link View#INVISIBLE}.
	 */
	public ViewVisibleManager(View view, int hiddenVisibility) {
		mView = view;
		mHiddenVisibilityFlag = hiddenVisibility;
	}
	
	public void addCondition(ViewVisibilityCondition enabler) {
		mEnablers.add(enabler);
		invalidate();
	}
	
	public boolean invalidate() {
        boolean currentState = mView.getVisibility() == View.VISIBLE;
        boolean newState = isVisible();
		if (newState) {
			mView.setVisibility(View.VISIBLE);
		} else {
			mView.setVisibility(mHiddenVisibilityFlag);
		}
        return currentState != newState;
	}
	
	private boolean isVisible() {
		for (ViewVisibilityCondition enabler : mEnablers) {
			if (!enabler.isVisible()) {
				return false;
			}
		}
		return true;
	}

	public interface ViewVisibilityCondition {
		public boolean isVisible();
	}
	
}
