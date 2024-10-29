package com.pocket.ui.view.highlight;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.ui.view.themed.ThemedTextView;

/**
 * A TextView that displays a visually highlighted quote.
 */
public class HighlightTextView extends ThemedTextView {
	
	private ColorStateList color;
	
	public HighlightTextView(Context context) {
		super(context);
		init(context, null);
	}
	
	public HighlightTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public HighlightTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	
	private void init(Context context, AttributeSet attrs) {
		setTextAppearance(context, R.style.Pkt_Text_Small_Medium);
		color = NestedColorStateList.get(getContext(), R.color.pkt_themed_amber_4);
		
		// Ensure there is a HighlightSpan on all text set on this view,
		addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (TextUtils.isEmpty(s)) {
					return;
				}
				
				// Ensure there is a single HighlightSpan and HighlightRegion covering the entire text
				// Ignore if it is already set up (to avoid infinite loops)
				HighlightSpan[] fulls = s.getSpans(0, s.length(), HighlightSpan.class);
				HighlightSpan.HighlightedRegion[] regions = s.getSpans(0, s.length(), HighlightSpan.HighlightedRegion.class);
				if (fulls.length == 0 || regions.length == 0 || fulls.length > 1 || regions.length > 1
					|| s.getSpanEnd(fulls[0]) != s.length()-1 || s.getSpanStart(fulls[0]) != 0
					|| s.getSpanEnd(regions[0]) != s.length()-1 || s.getSpanStart(regions[0]) != 0) {
					
					// Not setup, or setup incorrectly, so setup again.
					HighlightSpan.removeAll(s);
					new HighlightSpan(HighlightTextView.this, color)
							.attach(s, 0, s.length());
				}
			}
		});
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(true); // We currently don't support a disabled state for this view
	}
}
