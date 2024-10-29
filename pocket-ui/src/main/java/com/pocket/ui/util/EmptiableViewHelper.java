package com.pocket.ui.util;

import android.view.View;
import android.view.ViewGroup;

/**
 * Helper for views that are {@link EmptiableView}.
 * Create an instance, pass on calls of {@link #setOnEmptyChangedListener(OnEmptyChangedListener)} to here,
 * and invoke {@link #setEmpty(boolean)} as needed.
 */
public class EmptiableViewHelper implements EmptiableView {
	
	private final View view;
	private OnEmptyChangedListener listener;
	private boolean empty;
	
	public EmptiableViewHelper(View view, OnEmptyChangedListener defaultListener) {
		this.view = view;
		this.listener = defaultListener;
	}
	
	public void setEmpty(boolean empty) {
		if (this.empty != empty) {
			this.empty = empty;
			if (listener != null) {
				listener.onEmptyChanged(view, empty);
			}
		}
	}
	
	@Override
	public void setOnEmptyChangedListener(OnEmptyChangedListener listener) {
		this.listener = listener;
	}
	
	public static boolean hasVisibleChildren(ViewGroup view) {
		for (int i = 0, count = view.getChildCount(); i < count; i++) {
			if (view.getChildAt(i).getVisibility() == View.VISIBLE) {
				return true;
			}
		}
		return false;
	}
}
