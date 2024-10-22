package com.pocket.app.settings;

import android.content.Context;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.Preferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

@Singleton
public class Theme {
	
	public static final int LIGHT = 0;
	public static final int DARK = 1;

	// OPT this would be a good place to use bitwise ops instead
	public static final int FLAG_ALLOW_ALL = 0;
	public static final int FLAG_ONLY_DARK = 1;
	public static final int FLAG_ONLY_LIGHT = 2;

	public static final int[] STATE_LIGHT = {com.pocket.ui.R.attr.state_light};
	public static final int[] STATE_DARK = {com.pocket.ui.R.attr.state_dark};
	
	private final Subject<Object> changes = PublishSubject.create();
	private final Object change = new Object();
	private final IntPreference pref;

	@Inject
	public Theme(Preferences prefs) {
		pref = prefs.forUser("appTheme", Theme.LIGHT);
	}
	
	// OPT there are some drawing and layout operations that hit this, is that ok?
	
	/**
	 * Get the int key of the current app theme. This will not check if a theme is allowed in this context. When asking
	 * on behalf of a View or Activity, use get(Context) instead.
	 * 
	 * @return
	 */
	public int get() {
		return get((Context)null);
	}
	
	/**
	 * Get the int key of the current app theme.
	 * 
	 * @param context The context the themed view will display in. This allows for checks to see if the view allows certain Themes.
	 * @return
	 */
	public int get(Context context) {
		// Get the current setting
		int theme = pref.get();
		
		// If a context is available, use it to determine if any themes are not allowed in this context.
		int allowedFlag = FLAG_ALLOW_ALL;
		AbsPocketActivity activity = AbsPocketActivity.from(context);
		if (activity != null) {
			allowedFlag = activity.getThemeFlag();
		}
		
		return applyFlagsToTheme(theme, allowedFlag);
	}
	
	public int get(View view) {
		return get(view, null);
	}
	
	/**
	 * Get the int key of the current app theme.
	 *
	 * @param view This will search for the PageFragment that this view belongs to and check what themes are allowed for it.
	 * @param frag If the PageFragment is already known pass it here.
	 * @return int key of the current app theme
	 */
	public int get(View view, Fragment frag) {
		if (view.isInEditMode()) return LIGHT;

		// Get the current setting
		int theme = pref.get();
		
		// If a context is available, use it to determine if any themes are not allowed in this context.
		int allowedFlag = FLAG_ALLOW_ALL;
		AbsPocketActivity activity = AbsPocketActivity.from(view.getContext());
		if (activity != null) {
			if (frag == null) {
				frag = activity.getFragmentParentOfView(view);
			}
			if (frag instanceof AbsPocketFragment) {
				allowedFlag = ((AbsPocketFragment) frag).getThemeFlag();
				
			} else {
				// This could happen because the view has not been added yet, if the view is created and then added, but then doesn't have its drawable state refreshed, or if the fragment isn't a AbsPocketFragment.
				// Just use the default for the context
				return get(view.getContext());
			}
		}
		
		return applyFlagsToTheme(theme, allowedFlag);
	}
	
	public Observable<Integer> observeFor(Context context) {
		return changes.map(__ -> get(context)).distinctUntilChanged();
	}
	
	private static int applyFlagsToTheme(int theme, int flag) {
		switch (flag) {
		case FLAG_ONLY_DARK:
			return DARK;
		case FLAG_ONLY_LIGHT:
			return LIGHT;

		default:
			return theme;
		}
	}
	
	/**
	 * Is the current theme set to a dark variant?
	 * 
	 * @param context
	 * @return
	 */
	public boolean isDark(Context context) {
		return isDark(get(context));
	}
	
	/**
	 * Is the supplied int value a dark theme variant?
	 * 
	 * @param theme
	 * @return
	 */
	public static boolean isDark(int theme) {
		return theme == DARK;
	}
	
	/**
	 * Set the current theme setting.
	 */
	public void set(int theme) {
		pref.set(theme);
		changes.onNext(change);
	}
	
	/**
	 * Get the current theme represented by a view state int[]
	 *
	 * @param forView
	 * @return
	 */
	public int[] getState(View forView) {
		return getState(get(forView, null));
	}
	
	/**
	 * Get the current theme represented by a view state int[]
	 *
	 * @param forView
	 * @params frag If the parent PageFragment is known, pass it here. TODO this whole thing is a bloody hack, need to find a easier way to handle this.
	 * @return
	 */
	public int[] getState(View forView, AbsPocketFragment frag) {
		return getState(get(forView, frag));
	}
	
	/**
	 * Get the theme for the supplied int key, represented by a view state int[]
	 * 
	 * @param theme
	 * @return
	 */
	public static int[] getState(int theme) {
		switch(theme) {
		case DARK:
			return STATE_DARK;
		case LIGHT:
		default:
			return STATE_LIGHT;
		}
	}
	
	/**
	 * Get the bg color for the current theme.
	 * 
	 * @param context
	 * @return
	 */
	public int getThemeBGColor(Context context) {
		int theme = get(context);
		int res;
		switch(theme) {
		case DARK:
			res = com.pocket.ui.R.color.pkt_dm_base_bg;
			break;
		case LIGHT:
		default:
			res = com.pocket.ui.R.color.pkt_base_bg;
			break;
		}
		
		return context.getResources().getColor(res);
	}
	
	public static boolean isDark(int[] drawableState) {
		int len = drawableState.length;
		int state;
		for (int i = 0; i < len; i++) {
			state = drawableState[i];
			if (state == STATE_DARK[0]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the status bar color to use for the provided theme.
	 */
	public static int getStatusBarColor(int theme, Context context) {
		switch (theme) {
			case DARK:
				return ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_dm_base_bg);
			case LIGHT:
			default:
				return ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_base_bg);
		}
	}
	
	public static int getNavigationBarDividerColor(int theme, Context context) {
		switch (theme) {
			case DARK:
				return ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_dm_grey_6);
			case LIGHT:
			default:
				return ContextCompat.getColor(context, com.pocket.ui.R.color.pkt_grey_6);
		}
	}
	
	/** Exposed only for use in a preference screen. Use APIs on this class to modify and query. */
	public IntPreference pref() {
		return pref;
	}
	
}
