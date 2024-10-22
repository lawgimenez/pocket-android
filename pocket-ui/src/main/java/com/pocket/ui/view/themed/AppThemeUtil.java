package com.pocket.ui.view.themed;

import android.content.Context;
import android.view.View;
import android.view.ViewParent;

import com.pocket.util.android.ContextUtil;

/**
 * Helper for providing custom themes like a light and dark theme to an app, screen and/or views.
 * These themes will be available at the drawable state level, so you can define state lists
 * that will automatically update as your theme does, without requiring recreating activities or views,
 * just like pressed states.
 *
 * <h2>Setup</h2>
 * <h4>Define available themes as attributes</h4>
 *
 * Add attributes to values/attrs.xml like:
 *
 * <pre> {@code
 * 	<declare-styleable name="appTheme">
 *		<attr name="state_light" format="boolean" />
 *		<attr name="state_dark" format="boolean" />
 *	</declare-styleable>
 * }</pre>
 *
 * <h4>Create Themed Views</h4>
 *
 * For any views that you will use custom theming, create subclasses that override this method:
 * (Note: there are already ThemedViews created for most View types)
 *
 * <pre> {@code
 *	protected int[] onCreateDrawableState(int extraSpace) {
 *		final int[] state = super.onCreateDrawableState(extraSpace + 1);
 *		mergeDrawableStates(state, AppThemeUtil.getState(this));
 *		return state;
 *	}
 * }</pre>
 *
 * <h4>Set/Apply Themes</h4>
 *
 * To define an app wide theme that applies to all activities and views in the entire app,
 * have your {@link android.app.Application} implement {@link Themed}.
 * <p>
 * To define the theme at the activity level, have your {@link android.app.Activity} implement {@link Themed}.
 * <p>
 * To have the theme applied to only specific views, have your view or one of its parent views implement {@link Themed}.
 *
 * <h2>Usage</h2>
 *
 * After that initial setup, then you can make custom state lists using your themes, for example:
 *
 * <pre> {@code
 * <selector xmlns:android="http://schemas.android.com/apk/res/android"
 *	xmlns:pocket="http://schemas.android.com/apk/res-auto">
 *		<item
 *		  pocket:state_dark="true"
 *		  android:color="@color/dark_gray" />
 *		<item
 *		  android:color="@color/light_gray"/>
 * </selector>
 * }</pre>
 *
 * In this example, when the app is in dark mode, it will use dark_gray, and when in light mode, it will use light_gray.
 * <p>
 * You can then use these state list color or drawables normally in xml or at runtime.
 *
 *
 */
public abstract class AppThemeUtil {
	public static final int[] EMPTY = new int[0];
	
	public static int[] getState(View view) {
		Themed themed = findThemed(view);
		if (themed != null) {
			return themed.getThemeState(view);
		} else {
			return EMPTY;
		}
	}
	
	private static Themed findThemed(View view) {
		// Check the view itself
		if (view instanceof Themed) {
			return (Themed) view;
		}
		// Check its parent views
		ViewParent parent = view.getParent();
		while (parent instanceof View) {
			if (parent instanceof Themed) {
				return (Themed) parent;
			}
			parent = parent.getParent();
		}
		// Check its context (such as activities and application)
		return findThemed(view.getContext());
	}
	
	public static Themed findThemed(Context context) {
		Themed themed = ContextUtil.findContext(context, Themed.class);
		if (themed != null) {
			return themed;
		}
		return ContextUtil.findContext(context.getApplicationContext(), Themed.class);
	}
}
