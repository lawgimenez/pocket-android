package com.pocket.ui.view.badge;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.pocket.ui.R;
import com.pocket.ui.util.EmptiableView;
import com.pocket.ui.util.EmptiableViewHelper;
import com.pocket.ui.view.themed.ThemedLinearLayout;

import java.util.List;

import androidx.annotation.Nullable;

/**
 * Displays a single line of badges, in a pre-determined ordering, handling truncating as needed.
 * <p>
 * Since this view is expected to be rebound in a recycled  view, it lazily creates the internal views only
 * when needed, but once they are created, it will keep them around for reuse as needed.
 */
public class BadgesView extends ThemedLinearLayout implements EmptiableView {
	
	private final EmptiableViewHelper emptyHelper = new EmptiableViewHelper(this, EmptiableView.GONE_WHEN_EMPTY);
	private TextBadgeView groupView;
	private TagBadgesView tagsView;
	private int spacing;
	
	public BadgesView(Context context) {
		super(context);
		init();
	}
	
	public BadgesView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public BadgesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		setOrientation(HORIZONTAL);
		spacing = getResources().getDimensionPixelSize(R.dimen.pkt_space_sm);
		bindClear();
	}
	
	// TODO change to the binder pattern
	
	public BadgesView bindClear() {
		bindGroup(null, null, null, null, null);
		bindTags(null, null);
		return this;
	}
	
	public BadgesView bindGroup(String name, String englishName, ColorStateList titleColor, ColorStateList badgeColor, OnClickListener listener) {
		if (name != null) {
			if (groupView == null) {
				groupView = new TextBadgeView(getContext());
				groupView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}
			groupView.setTextAndUpdateEnUsLabel(name, englishName);
			groupView.setUiEntityIdentifier("badge_" + englishName);
			groupView.setTextColor(titleColor);
			groupView.setBadgeColor(badgeColor);
			groupView.setEnabled(isEnabled());
			groupView.setOnClickListener(listener);
			groupView.setClickable(listener != null);
			addView(groupView);
			sortViews();
			
		} else if (groupView != null) {
			groupView.setText(null);
			removeView(groupView);
		}
		checkForEmpty();
		return this;
	}
	
	public BadgesView bindTags(List<String> tags, OnClickListener listener) {
		return bindTags(tags, null, listener);
	}
	
	public BadgesView bindTags(List<String> tags, List<String> highlightedTags,  OnClickListener listener) {
		if (tagsView != null) {
			tagsView.bind().clear();
		}
		
		if (tags != null && !tags.isEmpty()) {
			if (tagsView == null) {
				tagsView = new TagBadgesView(getContext());
				tagsView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}
			tagsView.bind()
				.tags(tags)
				.highlightedTags(highlightedTags)
				.badgeClick(listener);
			tagsView.setEnabled(isEnabled());
			addView(tagsView);
			sortViews();
			
		} else if (tagsView != null) {
			removeView(tagsView);
		}
		checkForEmpty();
		return this;
	}
	
	private void checkForEmpty() {
		emptyHelper.setEmpty(getChildCount() == 0);
	}
	
	private void sortViews() {
		View group = getIfPresent(groupView);
		View tags = getIfPresent(tagsView);
		removeAllViews();
		// Add them back in, in order.
		addIfPresent(group);
		addIfPresent(tags);
		// Setup correct spacing
		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			lp.rightMargin = i < getChildCount() - 1 ? spacing : 0;
			child.setLayoutParams(lp);
		}
	}
	
	private void addIfPresent(View view) {
		if (view != null) {
			addView(view);
		}
	}
	
	private View getIfPresent(View view) {
		 return view != null && indexOfChild(view) >= 0 ? view : null;
	}
	
	@Override
	public void setOnEmptyChangedListener(OnEmptyChangedListener listener) {
		emptyHelper.setOnEmptyChangedListener(listener);
	}
}
