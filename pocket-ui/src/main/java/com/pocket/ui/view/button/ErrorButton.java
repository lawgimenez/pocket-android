package com.pocket.ui.view.button;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;

public class ErrorButton extends BoxButtonBase {
	
	public ErrorButton(Context context) {
		super(context);
		init(context);
	}
	
	public ErrorButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public ErrorButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		setTextColor(getResources().getColorStateList(R.color.pkt_button_text));
		setBackgroundDrawable(new ButtonBoxDrawable(context, R.color.pkt_button_box_error_fill, 0));
	}
	
}
