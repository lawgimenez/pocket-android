package com.pocket.ui.util;

import android.view.View;
import android.view.ViewGroup;

public class EnabledUtil {
	
	public static void setChildrenEnabled(ViewGroup parent, boolean enabled, boolean deep) {
		for (int i = 0, count = parent.getChildCount(); i < count; i++) {
			View child = parent.getChildAt(i);
			child.setEnabled(enabled);
			if (deep && child instanceof ViewGroup) {
				setChildrenEnabled((ViewGroup) child, enabled, true);
			}
		}
	}
	
}
