package com.pocket.ui.util;

import android.view.View;
import android.view.ViewGroup;

public class PocketUIViewUtil {

    /**
     * Removes a view, and replaces it with a new view in the same position. Also copies layout params.
     */
    public static void replaceView(View replace, View replacement) {
        ViewGroup parent = (ViewGroup) replace.getParent();
        int index = parent.indexOfChild(replace);
        parent.removeView(replace);

        replacement.setLayoutParams(replace.getLayoutParams());
        parent.addView(replacement, index);
    }

    public static void runAfterNextLayoutOf(View view, Runnable block) {
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                block.run();
                view.removeOnLayoutChangeListener(this);
            }
        });
    }
}
