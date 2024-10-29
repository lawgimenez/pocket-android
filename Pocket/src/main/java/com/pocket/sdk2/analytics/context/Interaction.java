package com.pocket.sdk2.analytics.context;

import android.content.Context;
import android.view.View;

import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.thing.ThingUtil;
import com.pocket.util.android.ContextUtil;

import static com.pocket.util.android.ContextUtil.findContext;

/**
 * Methods for generating {@link ActionContext}. See {@link com.pocket.sdk2.analytics.context} for usage.
 */
public class Interaction {

	public static Interaction on(View view) {
		Timestamp time = Timestamp.now();
		ActionContext context = new ActionContext.Builder().build();

		ContextualRoot root = findContext(view.getContext(), ContextualRoot.class);

		context = addViewContext(context, view, root);
		context = addAppContext(context, view.getContext(), root);

		return new Interaction(context, time);
	}

	public static Interaction on(Context androidContext) {
		Timestamp time = Timestamp.now();
		ActionContext context = new ActionContext.Builder().build();

		ContextualRoot root = findContext(androidContext, ContextualRoot.class);

		context = addAppContext(context, androidContext, root);

		return new Interaction(context, time);
	}
	
	// TODO REVIEW, seems like this class could be cleaned up a little bit to be clearer and reuse code more. A lot of constructors now.
	public static Interaction on(Contextual view, Context androidContext) {
		Timestamp time = Timestamp.now();
		ActionContext context = view.getActionContext();
		if (context == null) {
			context = new ActionContext.Builder().build();
		}

		ContextualRoot root = findContext(androidContext, ContextualRoot.class);
		context = addAppContext(context, androidContext, root);
		
		return new Interaction(context, time);
	}

	public final ActionContext context;
	public final Timestamp time;

	protected Interaction(ActionContext context, Timestamp time) {
		this.context = context;
		this.time = time;
	}

	private static ActionContext addAppContext(ActionContext actionContext, Context androidContext, ContextualRoot root) {
		// ContextualRoot
		if (root != null) {
			actionContext = ThingUtil.merge(root.getActionContext(), actionContext);
		}

		// Android Context
		if (androidContext != null) {
			// Component, such as Activity, Service
			Contextual component = ContextUtil.findContext(androidContext, Contextual.class);
			if (component != null) {
				actionContext = ThingUtil.merge(component.getActionContext(), actionContext);
			}

			// Application
			Contextual app = ContextUtil.findContext(androidContext.getApplicationContext(), Contextual.class);
			if (app != null) {
				actionContext = ThingUtil.merge(app.getActionContext(), actionContext);
			}
		}

		return actionContext;
	}

	private static ActionContext addViewContext(ActionContext context, View view, ContextualRoot root) {
		if (view == null) {
			return context;
		}

		ActionContext viewContext = null;
		
		if (view instanceof Contextual) {
			viewContext = ((Contextual) view).getActionContext();
			
		} else if (root != null) {
			viewContext = root.getActionContextFor(view);
		}
		
		context = ThingUtil.merge(viewContext, context); // Prioritizing values set closer to the interaction origin.

		if (view.getParent() instanceof View) {
			return addViewContext(context, (View) view.getParent(), root);
		} else {
			return context;
		}
	}
	
	/**
	 * Modify the {@link ActionContext}, overriding the base values with custom ones.
	 */
	public Interaction merge(ActionContext cxt) {
		if (cxt == null) return this;
		return new Interaction(context.builder().set(cxt).build(), time);
	}
	
	/**
	 * Modify the {@link ActionContext}, overriding the base values with custom ones.
	 */
	public Interaction merge(Modifier mod) {
		ActionContext.Builder cxt = context.builder();
		mod.modify(cxt);
		return new Interaction(cxt.build(), time);
	}
	
	public interface Modifier {
		void modify(ActionContext.Builder cxt);
	}
}
