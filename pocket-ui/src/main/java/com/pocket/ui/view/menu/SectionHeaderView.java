package com.pocket.ui.view.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.pocket.util.android.ViewUtilKt;

/**
 * The default style of a section header. Includes a bottom divider by default.
 * <p>
 * You can control dividers with xml attrs or methods:
 * <p>
 * `app:showDividerTop` or `bind().showTopDivider()` turns on and off a thick top divider
 * <p>
 * `app:showDividerBottom` or `bind().showBottomDivider()` turns on and off a thin bottom divider
 * <p>
 * Supports the standard android:text attribute for setting the header text via xml
 * TODO consider enforcing upper case
 */
public class SectionHeaderView extends ThemedConstraintLayout {
	
	private final Binder binder = new Binder();
	private TextView label;
	private TextView button;
	private View top;
	private View bottom;
	
	public SectionHeaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(attrs);
	}
	
	public SectionHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public SectionHeaderView(Context context) {
		super(context);
		init(null);
	}
	
	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.view_section_header, this, true);
		label = findViewById(R.id.label);
		button = findViewById(R.id.button);
		top = findViewById(R.id.top_divider);
		bottom = findViewById(R.id.bottom_divider);
		
		bind().clear();
		
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SectionHeaderView);
			bind().showTopDivider(a.getBoolean(R.styleable.SectionHeaderView_showDividerTop, false));
			bind().showBottomDivider(a.getBoolean(R.styleable.SectionHeaderView_showDividerBottom, true));
			bind().label(a.getText(R.styleable.SectionHeaderView_android_text));
			a.recycle();
		}
	}
	
	public Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		public Binder clear() {
			label(null);
			showTopDivider(false);
			showBottomDivider(true);
			button(null, null);
			textAllCaps(false);
			return this;
		}
		
		public Binder label(CharSequence value) {
			label.setText(value);
			return this;
		}
		
		public Binder label(int stringResId) {
			label.setText(stringResId);
			return this;
		}
		
		public Binder showTopDivider(boolean show) {
			top.setVisibility(show ? VISIBLE : GONE);
			return this;
		}
		
		public Binder showBottomDivider(boolean show) {
			bottom.setVisibility(show ? VISIBLE : GONE);
			return this;
		}
		
		public Binder button(int text, OnClickListener onClick) {
			return button(button.getResources().getText(text), onClick);
		}
		
		public Binder button(CharSequence text, OnClickListener onClick) {
			ViewUtilKt.setTextOrHide(button, text);
			button.setOnClickListener(onClick);
			return this;
		}

		public Binder textAllCaps(boolean caps) {
			label.setAllCaps(caps);
			return this;
		}
		
	}
	
}
