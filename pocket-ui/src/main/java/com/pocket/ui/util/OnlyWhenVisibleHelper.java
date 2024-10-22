package com.pocket.ui.util;

import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.view.ViewCompat;

/**
 * Helper for releasing resources or stopping animations/listeners etc for a {@link View} when no longer visible or off screen.
 * This isn't always clear how to accomplish within a {@link View} or requires a bunch of boilerplate. This helps you set it up
 * for any view with one call: {@link #install(View, Runnable, Runnable)}
 */
public class OnlyWhenVisibleHelper implements View.OnAttachStateChangeListener, ViewTreeObserver.OnGlobalLayoutListener {
	
	/**
	 * Runs `onVisible` if already visible, or 'onHidden' if already hidden and then any time it
	 * changes visibility state runs the appropriate method.
	 * <p>
	 * Visible means attached to a window, has a non-zero size and it and its parents are {@link View#VISIBLE}.
	 */
	public static OnlyWhenVisibleHelper install(View view, Runnable onVisible, Runnable onHidden) {
		return new OnlyWhenVisibleHelper(view, onVisible, onHidden);
	}
	
	private final View view;
	private final Runnable onVisible;
	private final Runnable onHidden;
	/** Null means it hasn't been initialized yet. */
	private Boolean isVisible;
	
	private OnlyWhenVisibleHelper(View view, Runnable onVisible, Runnable onHidden) {
		this.view = view;
		this.onVisible = onVisible != null ? onVisible : () -> {};
		this.onHidden = onHidden != null ? onHidden : () -> {};
		
		if (ViewCompat.isAttachedToWindow(view)) {
			onViewAttachedToWindow(view);
		}
		view.addOnAttachStateChangeListener(this);
	}
	
	@Override
	public void onViewAttachedToWindow(View v) {
		if (view.getViewTreeObserver() != null && view.getViewTreeObserver().isAlive()) {
			view.getViewTreeObserver().addOnGlobalLayoutListener(this);
		}
		update();
	}
	
	@Override
	public void onViewDetachedFromWindow(View v) {
		update();
		if (view.getViewTreeObserver() != null && view.getViewTreeObserver().isAlive()) {
			view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		}
	}
	
	@Override
	public void onGlobalLayout() {
		update();
	}
	
	private void update() {
		boolean first = isVisible == null;
		boolean was = isVisible != null && isVisible;
		boolean now = ViewCompat.isAttachedToWindow(view) && view.isShown() && view.getWidth() > 0 && view.getHeight() > 0;
		isVisible = now;
		if (first || was != now) {
			if (isVisible) {
				onVisible.run();
			} else {
				onHidden.run();
			}
		}
	}
	
}
