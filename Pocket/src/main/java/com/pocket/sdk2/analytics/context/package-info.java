/**
 * Tools for recording Action Context, which is additional information attached to all Actionss
 * and describe the when why and where (the context) of an action.
 * <p>
 * <h2>How to generate context</h2>
 * Context is automatically generated from the point of origin or interaction.
 * When an Action is triggered, invoke one of {@link com.pocket.sdk2.analytics.context.Interaction}'s on() methods passing
 * in the point of origin or cause of the action. For example, if a user pressed a button that archives an item,
 * call {@link com.pocket.sdk2.analytics.context.Interaction#on(android.view.View)} to
 * generate the context from that button View.
 * <p>
 * The generated {@link com.pocket.sdk2.analytics.context.Interaction#context} can be passed to the creation of the Action.
 * <p>
 * <h2>How to define context</h2>
 * <p>
 * Context is defined at the view level. A view may defined what context it wants to provide. To define context for a
 * view you can have your View subclass implement {@link com.pocket.sdk2.analytics.context.Contextual}. This is recommended since
 * it will be reusable for all instances of that view anywhere in the app. If it isn't a custom view class, you can
 * use {@link com.pocket.sdk2.analytics.context.ContextualRoot#bindViewContext(android.view.View, com.pocket.sdk2.analytics.context.Contextual)}
 * to tell the system that this view should have a specific context. {@link com.pocket.sdk.util.AbsPocketActivity} and {@link com.pocket.sdk.util.AbsPocketFragment}
 * are both implementations of this.
 * <p>
 * Once context is defined on a view, any interactions directly on that view or in one of its child views will automatically
 * be collected during an {@link com.pocket.sdk2.analytics.context.Interaction}.
 * <p>
 * See {@link com.pocket.sdk2.analytics} for info on Analytics in general.
 */
package com.pocket.sdk2.analytics.context;