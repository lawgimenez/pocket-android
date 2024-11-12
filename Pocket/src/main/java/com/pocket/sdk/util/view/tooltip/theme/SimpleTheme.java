package com.pocket.sdk.util.view.tooltip.theme;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.sdk.util.view.tooltip.view.CaretTooltip;
import com.pocket.util.android.FormFactor;

/**
 * Shows tooltips that have a simple single color rounded box with content and a caret arrow extending out of them 
 * towards the anchor/target.
 */
public class SimpleTheme implements TooltipTheme {

    private static final float ANCHOR_DISTANCE = -5f;

    @Override
    public Tooltip.TooltipController showButton(final View button, int text, Tooltip.TooltipListener listener) {
        return showButton(button, null, text, listener);
    }

    @Override
    public Tooltip.TooltipController showButton(final View button, ViewGroup displayLocation, int text, Tooltip.TooltipListener listener) {
        Context context = button.getContext();

        return new Tooltip.Builder(context)
                .addView(new CaretTooltip.Builder(context)
                                .setText(text, 0)
                                .setDistance(FormFactor.dpToPx(ANCHOR_DISTANCE))
                                .build()
                )
                .setDisplayLocation(displayLocation)
                .setTooltipListener(listener)
                .show(button, false);
    }

    @Override
    public Tooltip.TooltipController showAdapterItem(final Object item, final AdapterView adapterView, int text, Tooltip.TooltipListener listener) {
        Context context = adapterView.getContext();

        return new Tooltip.Builder(context)
                .addView(new CaretTooltip.Builder(context)
                                .setText(text, 0)
                                .setDistance(FormFactor.dpToPx(ANCHOR_DISTANCE))
                                .build()
                )
                .setTooltipListener(listener)
                .showAdapterItem(item, adapterView);
    }

}
