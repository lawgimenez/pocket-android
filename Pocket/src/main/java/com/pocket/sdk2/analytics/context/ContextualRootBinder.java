package com.pocket.sdk2.analytics.context;

import android.view.View;

import com.pocket.sdk.api.generated.thing.ActionContext;

import java.util.WeakHashMap;

/**
 * A helper for {@link ContextualRoot} implementations.
 */
public class ContextualRootBinder {

	private final WeakHashMap<View, Contextual> views = new WeakHashMap<>();

	public void bindViewContext(View view, Contextual context) {
		views.put(view, context);
	}

	public ActionContext getActionContextFor(View view) {
		Contextual contextual = views.get(view);
		if (contextual != null) {
			return contextual.getActionContext();
		} else {
			return null;
		}
	}

}
