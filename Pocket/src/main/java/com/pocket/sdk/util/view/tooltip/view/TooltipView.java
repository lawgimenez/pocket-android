package com.pocket.sdk.util.view.tooltip.view;

import android.graphics.Rect;
import android.view.View;

import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.util.android.animation.AnimatorEndListener;

/**
 * The visual representation a {@link Tooltip}.
 */
public interface TooltipView {

    /**
     * Invoked when a tooltip is shown to provide method for dismissing if needed.
     * @param controller
     */
    void bind(Tooltip.TooltipController controller);

    /**
     * @return The View for the tooltip. This must always be the same view.
     */
    View getView();

    /**
     * Update your view to target the new location of the anchor.
     *
     * @param xy Set the new x and y coordinates within the window that your view should be positioned at. x in[0] and y in [1]
     * @param anchorBounds The bounds of the anchor view in absolute screen coordinates.
     * @param windowBounds The bounds of the thing holding your view, in absolute screen coordinates. Your tooltip should fit completely within this.
     * @return true if your tooltip was able to be shown, false if it couldn't fit or won't properly be able to display based on the anchor position and window size.
     *          Your tooltip should be flexible enough, or this style only used in certain cases so that this shouldn't really happen.
     */
    boolean applyAnchor(int[] xy, Rect anchorBounds, Rect windowBounds);

    void animateIn();

    void animateOut(AnimatorEndListener callback);

}
