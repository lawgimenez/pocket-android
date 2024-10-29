package com.pocket.sdk2.analytics.context;

import android.view.View;

import com.pocket.sdk.api.generated.thing.ActionContext;


/**
 * A root of views. If you have views that don't implement {@link Contextual} but you want to declare some
 * {@link ActionContext} for them, you can bind them with {@link #bindViewContext(View, Contextual)}.
 * Then any {@link Interaction}s that occur within this {@link ContextualRoot}s views, will properly collect
 * this context.
 * <p>
 * See {@link com.pocket.sdk2.analytics.context} for usage.
 */
public interface ContextualRoot extends Contextual {
	
	/** Set this view to have this context. Alternative to having the view implement {@link Contextual} */
	void bindViewContext(View view, Contextual context);
	/** Get the action context for this view. */
	ActionContext getActionContextFor(View view);

}
