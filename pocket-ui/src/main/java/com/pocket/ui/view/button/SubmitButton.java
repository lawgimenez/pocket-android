package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;

import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.text.TextViewUtil;
import com.pocket.ui.view.themed.ThemedTextView;

public class SubmitButton extends ThemedTextView {
	
	public SubmitButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public SubmitButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public SubmitButton(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		setGravity(Gravity.CENTER);
		setTypeface(Fonts.get(getContext(), Fonts.Font.GRAPHIK_LCG_MEDIUM));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.pkt_medium_text));
		setBackgroundDrawable(null);
		TextViewUtil.setVisualTextPadding(this, R.dimen.pkt_space_md, R.dimen.pkt_space_md);
		setTextColor(getResources().getColorStateList(R.color.white));
		setBackgroundDrawable(new ButtonBoxDrawable(getContext(), R.color.pkt_button_box_fill, 0, 0));
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
