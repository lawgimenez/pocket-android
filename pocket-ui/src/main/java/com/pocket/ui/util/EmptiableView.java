package com.pocket.ui.util;

import android.view.View;

/**
 * A view that depending on what data is bound to it, can either have content or be empty.
 * Provides an interface for parent views to decide how to handle in their layouts.
 * This is mostly so when chained views don't have content, they are set to GONE so that their margins are also hidden.
 *
 * See {@link EmptiableViewHelper} for easy implementation.
 */
public interface EmptiableView {
	OnEmptyChangedListener GONE_WHEN_EMPTY = (view, isEmpty) -> view.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
	
	void setOnEmptyChangedListener(OnEmptyChangedListener listener);
	interface OnEmptyChangedListener {
		void onEmptyChanged(View view, boolean isEmpty);
	}
}
