package com.pocket.sdk.util.view.tooltip.theme;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.pocket.sdk.util.view.tooltip.Tooltip;

/**
 * Defines convenience methods for creating tooltips in a specific style. This allows us to quickly
 * swap out the default style used through out the entire app with different implementations.
 */
public interface TooltipTheme {

    public Tooltip.TooltipController showButton(final View button, int text, Tooltip.TooltipListener listener);

    public Tooltip.TooltipController showButton(final View button, ViewGroup displayLocation, int text, Tooltip.TooltipListener listener);

    public Tooltip.TooltipController showAdapterItem(final Object item, final AdapterView adapterView, int text, Tooltip.TooltipListener listener);
}
