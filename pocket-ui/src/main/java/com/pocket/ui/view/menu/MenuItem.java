package com.pocket.ui.view.menu;

import android.view.View;

import androidx.annotation.StringRes;

public class MenuItem {
	
	public final @StringRes int label;
	public final int icon;
	public final int id;
	public final int groupId;
	public final View.OnClickListener onClick;
	public final String uiEntityIdentifier;

    private boolean mIsVisible = true;
    private boolean mIsEnabled = true;

	public MenuItem(int label, int menuId, int icon, View.OnClickListener onClick) {
		this(label, menuId, icon, onClick, null);
	}

	public MenuItem(int label, int menuId, int icon, View.OnClickListener onClick, String uiEntityIdentifier) {
		this.id = menuId;
		this.groupId = 1;
		this.label = label;
		this.icon = icon;
		this.onClick = onClick;
		this.uiEntityIdentifier = uiEntityIdentifier;
	}

    public void setVisible(boolean visible) {
        mIsVisible = visible;
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

	public void onClick(View v) {
		if (onClick != null) {
			onClick.onClick(v);
		}
	}

	/**
	 * Invoked each time, right before the menu is displayed. Returns whether or
	 * not this option should be visible in the menu.
	 * 
	 * @return
	 */
	public boolean isVisible() {
		return mIsVisible;
	}

	/**
	 * Invoked each time, right before the menu is displayed. Returns whether or
	 * not this option should be enabled in the menu.
	 * 
	 * @return
	 */
	public boolean isEnabled() {
		return mIsEnabled;
	}

}