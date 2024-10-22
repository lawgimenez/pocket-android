package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class ToggleButton extends BoxButtonBase {
	
	public ToggleButton(Context context) {
		super(context);
		init(context, null);
	}
	
	public ToggleButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public ToggleButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}
	
	private void init(Context context, AttributeSet attrs) {
		setCheckable(true);
		setTextColor(getResources().getColorStateList(R.color.pkt_button_toggle_text));
		setBackgroundDrawable(new ButtonBoxDrawable(context, R.color.pkt_button_toggle_fill, R.color.pkt_button_toggle_stroke));
	}
	
}
