package com.pocket.util.android;

import android.util.Property;
import android.view.Window;

public abstract class WindowUtil {
	public static class StatusBarColorProperty extends Property<Window, Integer> {
		public StatusBarColorProperty() {
			super(Integer.class, "statusBarColor");
		}

		@Override public void set(Window object, Integer value) {
			object.setStatusBarColor(value);
		}

		@Override public Integer get(Window object) {
			return object.getStatusBarColor();
		}
	}

	public static class NavigationBarColorProperty extends Property<Window, Integer> {
		public NavigationBarColorProperty() {
			super(Integer.class, "navigationBarColor");
		}

		@Override public void set(Window object, Integer value) {
			object.setNavigationBarColor(value);
		}

		@Override public Integer get(Window object) {
			return object.getNavigationBarColor();
		}
	}
}
