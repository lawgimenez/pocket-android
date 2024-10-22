package com.pocket.ui.view.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.IntrinsicSizeHelper;
import com.pocket.ui.view.checkable.CheckableConstraintLayout;
import com.pocket.ui.view.themed.ThemedTextView;

public class OptionRowView extends CheckableConstraintLayout {
	
	private final IntrinsicSizeHelper sizeHelper = new IntrinsicSizeHelper(-1, DimenUtil.dpToPxInt(getContext(), 54));
	private ImageView icon;
	private ThemedTextView label;
	
	public OptionRowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public OptionRowView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public OptionRowView(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_option_row_view, this, true);
		icon = findViewById(R.id.icon);
		label = findViewById(R.id.label);
		setBackgroundResource(R.drawable.cl_pkt_touchable_area);
		setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
		engageable.setUiEntityType(Type.BUTTON);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
		heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public void setLabel(int stringResId) {
		label.setTextAndUpdateEnUsLabel(stringResId);
	}
	
	public void setIcon(Drawable value) {
		icon.setImageDrawable(value);
		icon.setVisibility(icon.getDrawable() != null ? VISIBLE : GONE);
	}
	
	public void setIcon(int drawableResId) {
		icon.setImageResource(drawableResId);
		icon.setVisibility(icon.getDrawable() != null ? VISIBLE : GONE);
	}

}
