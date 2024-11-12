package com.pocket.util.android.view;

import android.content.Context;

/**
 * For views that are unable to use state selectors for managing the Theme, they can implement this interface to help keep track of how to update manually.
 * @author max
 *
 */
public interface ManuallyUpdateTheme {
	/**
	 * Should be called during the init of a view.  It should call Theme.registerForManualUpdates(). This is also a good time to call updateThemeManually to setup the view.
	 */
	public void registerForThemeUpdates();
	
	/**
	 * Called when the Theme has changed. The view should update itself for the new Theme.
	 */
	public void updateThemeManually();

	public Context getContext(); // Should be applied to Views
}
