package com.pocket.ui.view.badge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.IntrinsicSizeHelper;
import com.pocket.ui.view.button.PktViewsKt;
import com.pocket.ui.view.themed.ThemedTextView;

/**
 * Base class for badge views whose label is text.
 * Be sure to call {@link #setBadgeColor(ColorStateList)} and {@link #setTextColor(ColorStateList)}.
 * For subclasses, a good place to do this from is overriding {@link #init()}.
 */
public class TextBadgeView extends ThemedTextView {
	
	private IntrinsicSizeHelper sizeHelper;

	public TextBadgeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public TextBadgeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public TextBadgeView(Context context) {
		super(context);
		init();
	}
	
	protected void init() {
		sizeHelper = new IntrinsicSizeHelper(-1, BadgeUtil.getBadgeSize(getContext()));
		setTextAppearance(getContext(), R.style.Pkt_Text_Small_LightTitle);
		setMaxLines(1);
		setEllipsize(TextUtils.TruncateAt.END);
		
		//  Set the padding so the text will be vertically center, not, including its ascent. This gives it a more visually centered look.
		int sidePadding = getResources().getDimensionPixelSize(R.dimen.pkt_space_sm);
		Paint.FontMetrics fm = getPaint().getFontMetrics();
		int height = BadgeUtil.getBadgeSize(getContext());
		int topPadding = (int) (((height - -fm.ascent) / 2f) + (fm.top - fm.ascent)) - DimenUtil.dpToPxInt(getContext(), 1);
		setPadding(sidePadding, topPadding, sidePadding, 0);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
		heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public TextBadgeView setBadgeColor(ColorStateList colors) {
		setBackgroundDrawable(new BadgeDrawable(getContext(), colors));
		return this;
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
