package com.pocket.sdk2.analytics.context;


import android.view.View;

import com.pocket.sdk.api.generated.thing.ActionContext;

/**
 * Something that can provide {@link ActionContext}.
 * Typically this will be a {@link View}. View's can declare context by implemeting the {@link Contextual} interface
 * or by using {@link ContextualRoot#bindViewContext(View, Contextual)}.
 * <p>
 * See {@link com.pocket.sdk2.analytics} for info on Action Context.
 */
public interface Contextual {
	/** @return The current {@link ActionContext} of this component. */
	ActionContext getActionContext();
}
