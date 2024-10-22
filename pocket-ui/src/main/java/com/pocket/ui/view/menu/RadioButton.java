package com.pocket.ui.view.menu;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.view.button.IconButton;

/**
 * TODO need to confirm what style design wants here
 */
public class RadioButton extends IconButton {

	public RadioButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public RadioButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public RadioButton(Context context) {
		super(context);
		init();
	}

	private void init() {
		setScaleType(ScaleType.CENTER);
		setDrawableColor(R.color.pkt_themed_teal_2);
		setImageResource(R.drawable.btn_radio_mtrl);
	}

}
