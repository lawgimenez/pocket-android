package com.pocket.util.android.view;

import java.util.ArrayList;

import android.view.View;

/**
 * Controls whether or not a view is enabled by checking a list of registered {@link ViewEnabledCondition}'s.
 * To be enabled, all registered enablers must return true.
 * <p>
 * Anytime an enablers status changes, it should invoke {@link #invalidate()} to update the view's state..
 */
public class ViewEnabledManager {
	
	private final ArrayList<ViewEnabledCondition> mEnablers = new ArrayList<ViewEnabledCondition>();
	private final View mView;
			
	public ViewEnabledManager(View view) {
		mView = view;
	}
	
	public void addCondition(ViewEnabledCondition enabler) {
		mEnablers.add(enabler);
		invalidate();
	}
	
	public void invalidate() {
		mView.setEnabled(isEnabled());
	}
	
	private boolean isEnabled() {
		for (ViewEnabledCondition enabler : mEnablers) {
			if (!enabler.isEnabled()) {
				return false;
			}
		}
		return true;
	}

	public interface ViewEnabledCondition {
		public boolean isEnabled();
	}
	
}
