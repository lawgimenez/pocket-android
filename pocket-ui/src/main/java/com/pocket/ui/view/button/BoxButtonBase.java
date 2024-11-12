package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.view.checkable.CheckableTextView;

class BoxButtonBase extends CheckableTextView {
	
	public BoxButtonBase(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public BoxButtonBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public BoxButtonBase(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		setClickable(true);
		setGravity(Gravity.CENTER);
		setTypeface(Fonts.get(getContext(), Fonts.Font.GRAPHIK_LCG_MEDIUM));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.pkt_medium_text));
		setBackgroundDrawable(null);
		TextViewUtil.setVisualTextPadding(this,R.dimen.pkt_space_sm, R.dimen.pkt_space_md);
		/// TODO ripple?
	}
	
	@Override
	public int visualAscent() {
		return 0;
	}
	
	@Override
	public int visualDescent() {
		return 0;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		PktViewsKt.updateEnabledAlpha(this);
	}
}
