package com.pocket.ui.view.badge;

import android.content.Context;
import android.util.AttributeSet;

import com.pocket.ui.R;
import com.pocket.ui.util.NestedColorStateList;

/**
 * A badge that shows a tag name. Use normal text view setText methods to set the tag.
 */
public class TagBadgeView extends TextBadgeView {
	
	public TagBadgeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public TagBadgeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TagBadgeView(Context context) {
		super(context);
	}
	
	@Override
	protected void init() {
		super.init();
		setBadgeColor(NestedColorStateList.get(getContext(), R.color.pkt_badge_tag));
		setTextColor(NestedColorStateList.get(getContext(), R.color.pkt_badge_tag_text));
		setUiEntityIdentifier("badge_tag");
	}
	
}
