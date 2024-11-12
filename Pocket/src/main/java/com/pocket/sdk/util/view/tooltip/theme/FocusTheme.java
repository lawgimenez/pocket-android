package com.pocket.sdk.util.view.tooltip.theme;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.pocket.sdk.util.view.tooltip.OutsideTouchAction;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.sdk.util.view.tooltip.view.BottomSheetTooltip;
import com.pocket.sdk.util.view.tooltip.view.TooltipFocusBackground;

/**
 * Shows tooltips where it blacks out the surrounding area and focuses on the anchor.
 */
public class FocusTheme implements TooltipTheme {
    
    private final TooltipFocusBackground.Shape shape;
    private final CharSequence title;
    private final int buttonPositive;
    private final int buttonNeutral;
    
    public FocusTheme(TooltipFocusBackground.Shape shape, CharSequence title, int buttonPositive, int buttonNeutral) {
        this.shape = shape;
        this.title = title;
        this.buttonPositive = buttonPositive;
        this.buttonNeutral = buttonNeutral;
    }
    
    @Override
    public Tooltip.TooltipController showButton(final View button, int text, Tooltip.TooltipListener listener) {
        return showButton(button, null, text, listener);
    }

    @Override
    public Tooltip.TooltipController showButton(final View button, ViewGroup displayLocation, int text, Tooltip.TooltipListener listener) {
        Context context = button.getContext();

        return new Tooltip.Builder(context)
                .addView(new TooltipFocusBackground(context, shape))
                .addView(new BottomSheetTooltip(context).withText(title, text, buttonPositive, buttonNeutral))
                .setDisplayLocation(displayLocation)
                .setTooltipListener(listener)
                .setOutsideTouchAction(new OutsideTouchAction(OutsideTouchAction.Block.IF_NOT_ON_ANCHOR, false))
                .show(button, false);
    }

    @Override
    public Tooltip.TooltipController showAdapterItem(final Object item, final AdapterView adapterView, int text, Tooltip.TooltipListener listener) {
        Context context = adapterView.getContext();

        return new Tooltip.Builder(context)
                .addView(new TooltipFocusBackground(context, shape))
                .addView(new BottomSheetTooltip(context).withText(context.getText(text), 0, 0, 0))
                .setTooltipListener(listener)
                .setOutsideTouchAction(new OutsideTouchAction(OutsideTouchAction.Block.IF_NOT_ON_ANCHOR, false))
                .showAdapterItem(item, adapterView);
    }

}
