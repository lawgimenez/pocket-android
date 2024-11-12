package com.pocket.app;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.ideashower.readitlater.R;

/**
 * Less verbose methods for toasting a message and handling common use cases. All toasts made through this
 * class use the pocket toast theme and use the application context. Also all toasts default to the long duration.
 */
public class QuickToast {

    private static final int DEFAULT_DURATION = Toast.LENGTH_LONG;

    private static Toast sSingleton;

    /**
     * @param msg
     * @return An unshown standardized style of Toast for the app.
     */
    public static Toast make(int msg) {
        return Toast.makeText(getContext(), msg, DEFAULT_DURATION);
    }

    /**
     * @param msg
     * @return An unshown standardized style of Toast for the app.
     */
    public static Toast make(CharSequence msg) {
        return Toast.makeText(getContext(), msg, DEFAULT_DURATION);
    }

    /**
     * Show a standardized toast. Ok to invoke from off the ui thread, but in that case it will not return the toast.
     * If you need the toast instance, invoke this from the ui thread.
     *
     * @param msg
     * @param reuse Optional Toast to be reused instead of making a new one. This is helpful for updating the toast rather than waiting for it to hide and show again.
     * @return The Toast or null if this was invoked off the ui thread and it had to be created/shown async.
     */
    public static Toast show(final int msg, final Toast reuse) {
        if (App.getApp().threads().isOnUIThread()) {
            Toast toast;
            if (reuse != null) {
                reuse.setText(msg);
                reuse.setDuration(DEFAULT_DURATION);
                toast = reuse;
            } else {
                toast = make(msg);
            }
            toast.show();
            return toast;

        } else {
            App.getApp().threads().runOrPostOnUiThread(() -> show(msg, reuse));
            return null;
        }
    }

    /**
     * Show a standardized toast. Ok to invoke from off the ui thread, but in that case it will not return the toast.
     * If you need the toast instance, invoke this from the ui thread.
     *
     * @param msg
     * @param reuse Optional Toast to be reused instead of making a new one. This is helpful for updating the toast rather than waiting for it to hide and show again.
     */
    public static void show(final CharSequence msg, final Toast reuse) {
        App.getApp().threads().runOrPostOnUiThread(() -> {
            Toast toast;
            if (reuse != null) {
                reuse.setText(msg);
                reuse.setDuration(DEFAULT_DURATION);
                toast = reuse;
            } else {
                toast = make(msg);
            }
            toast.show();
        });
    }

    /**
     * Show a standardized toast. Ok to invoke from off the ui thread, but in that case it will not return the toast.
     * If you need the toast instance, invoke this from the ui thread.
     *
     * @param msg
     * @return The Toast or null if this was invoked off the ui thread and it had to be created/shown async.
     */
    public static Toast show(final int msg) {
        return show(msg, null);
    }

    /**
     * Show a standardized toast. Ok to invoke from off the ui thread, but in that case it will not return the toast.
     * If you need the toast instance, invoke this from the ui thread.
     *
     * @param msg
     * @return The Toast or null if this was invoked off the ui thread and it had to be created/shown async.
     */
    public static void show(final CharSequence msg) {
        show(msg, null);
    }

    /**
     * Same as {@link #show(int, android.widget.Toast)} where the reused toast is a static singleton
     * shared by any other users of this method. If the toast is already visible, it will be updated
     * with the new message.
     *
     * @param msg
     */
    public static void showInSingleton(int msg) {
        if (sSingleton == null) {
            sSingleton = make(msg);
        }
        sSingleton.setText(msg);
        sSingleton.setDuration(Toast.LENGTH_LONG);
        sSingleton.show();
    }

    /**
     * Same as {@link #show(CharSequence, android.widget.Toast)} where the reused toast is a static singleton
     * shared by any other users of this method. If the toast is already visible, it will be updated
     * with the new message.
     *
     * @param msg
     */
    public static void showInSingleton(CharSequence msg) {
        if (sSingleton == null) {
            sSingleton = make(msg);
        }
        sSingleton.setText(msg);
        sSingleton.setDuration(Toast.LENGTH_LONG);
        sSingleton.show();
    }

    public static Toast getSingleton() {
        if (sSingleton == null) {
            sSingleton = make("");
        }
        return sSingleton;
    }

    /**
     * @return The standardized context to use for all toast creation in this class.
     */
    private static Context getContext() {
        return new ContextThemeWrapper(App.getContext(), R.style.Theme_PocketDefault_Light);
    }


}
