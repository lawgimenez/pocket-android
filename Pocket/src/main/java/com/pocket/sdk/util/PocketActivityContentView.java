package com.pocket.sdk.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * The content view of {@link PocketActivityRootView}.
 * Even if this implementation stays empty, its main function is to
 * serve as a strong type for what ViewGroup the content view is.
 */
public class PocketActivityContentView extends FrameLayout {
	
	public PocketActivityContentView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	public PocketActivityContentView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public PocketActivityContentView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public PocketActivityContentView(Context context) {
		super(context);
	}
	
}
