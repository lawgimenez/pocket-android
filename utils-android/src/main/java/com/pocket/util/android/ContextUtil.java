package com.pocket.util.android;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

/**s
 * Helper methods related to {@link Context}
 */
public abstract class ContextUtil {

    /**
     * Gets an Activity instance from a view's context using {@link #getActivity(android.content.Context)}.
     * @param view
     * @return
     */
    public static Activity getActivity(View view) {
        return getActivity(view.getContext());
    }

    /**
     * Tries to cast a Context to an Activity. Can find even within ContextThemeWrapper. If context is null or the context is not an activity, returns null.
     * @param context
     * @return
     */
    public static Activity getActivity(Context context) {
        return findContext(context, Activity.class);
    }
    
    public static <T> T findContext(View view, Class<T> clazz) {
        return findContext(view.getContext(), clazz);
    }

	/**
     * Search this context and wrapped contexts for one that matches this class type
     */
    public static <T> T findContext(Context context, Class<T> clazz) {
        if (context == null) {
            return null;
        } else if (clazz.isAssignableFrom(context.getClass())) {
            return (T) context;
        } else if (context instanceof ContextWrapper) {
            return findContext(((ContextWrapper) context).getBaseContext(), clazz);
        } else {
            return null;
        }
    }
}
