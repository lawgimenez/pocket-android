package com.pocket.sdk.util.view.tooltip;

import android.view.View;

/**
 * Something that can hold and display tooltip views.
 */
public interface ViewDisplayer {

    /**
     * The view to display.
     * @param view
     */
    public void setView(View view);

    /**
     * Visually remove/animate the views away. This is final, no need to prepare to reshow.
     */
    public void dismiss();


}
