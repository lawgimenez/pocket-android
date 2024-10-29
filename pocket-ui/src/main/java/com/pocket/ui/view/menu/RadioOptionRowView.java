package com.pocket.ui.view.menu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.IntrinsicSizeHelper;
import com.pocket.ui.view.checkable.CheckableConstraintLayout;

public class RadioOptionRowView extends CheckableConstraintLayout {
	
	private final IntrinsicSizeHelper sizeHelper = new IntrinsicSizeHelper(-1, DimenUtil.dpToPxInt(getContext(), 54));
	private TextView label;
	
	public RadioOptionRowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public RadioOptionRowView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	public RadioOptionRowView(Context context) {
		super(context);
		init(null);
	}
	
	private void init(AttributeSet attrs) {
		LayoutInflater.from(getContext()).inflate(R.layout.view_radio_option_row_view, this, true);
		label = findViewById(R.id.label);
		setBackgroundResource(R.drawable.cl_pkt_touchable_area);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
		heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public void setLabel(int stringResId) {
		label.setText(stringResId);
	}
	
	public void setLabel(CharSequence value) {
		label.setText(value);
	}

}
