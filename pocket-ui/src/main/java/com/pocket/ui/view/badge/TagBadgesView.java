package com.pocket.ui.view.badge;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.pocket.ui.R;
import com.pocket.ui.util.EnabledUtil;
import com.pocket.ui.view.themed.ThemedTextView;
import com.pocket.ui.view.themed.ThemedViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Displays a single line of tags handling various forms of truncating as needed.
 * <p>
 * We'll try to display as many tags as we can fit, following these rules:
 * <p>
 * If there is enough room for all tags, they are laid out normally:
 * [tag] [tag]
 * <p>
 * If there are more tags that will fit, we display +3 or whatever number is not displayed:
 * [tag] [tag] +2
 * <p>
 * If we don't even have space for a single tag, we ellipsize the tag:
 * [tag...]
 * <p>
 * If we can't fit a single tag, and there are many, just display the +#:
 * +2 // TODO this is a rare case so probably not worth optimizing but would be nice to say "+2 tags"
 */
public class TagBadgesView extends ThemedViewGroup {
	
	private static final int MAX_RECYCLED_VIEWS = 3; // TODO consider recycling more or allowing a parent to pass in a recycler interface that would allow sharing between all views in a collection
	
	private final Binder binder = new Binder();
	private final List<String> tags = new ArrayList<>();
	private final List<String> highlightedTags = new ArrayList<>();
	private final List<View> staged = new ArrayList<>();
	private final List<TagBadgeView> recycler = new ArrayList<>();
	private int spacing;
	private ThemedTextView overflow;
	private OnClickListener clickListener;
	
	public TagBadgesView(Context context) {
		super(context);
		init();
	}
	
	public TagBadgesView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public TagBadgesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		spacing = getResources().getDimensionPixelSize(R.dimen.pkt_space_sm);
		overflow = new ThemedTextView(getContext());
		overflow.setLayoutParams(generateDefaultLayoutParams());
		overflow.setTextAppearance(getContext(), R.style.Pkt_Text_Small_LightTitle);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Disconnect (and recycle) views to avoid them triggering additional layout passes.
		for (int i = 0, count = getChildCount(); i < count; i++) {
			View view = getChildAt(i);
			if (view instanceof TagBadgeView && recycler.size() < MAX_RECYCLED_VIEWS) {
				recycler.add((TagBadgeView) view);
			}
		}
		removeAllViews();
		
		// Measure and create views that will fit the space
		int width = MeasureSpec.getSize(widthMeasureSpec) - this.getPaddingRight() - this.getPaddingLeft();
		if  (width > 0) {
			int x = 0;
			int childUnboundWidthSpec = getChildMeasureSpec(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), 0, LayoutParams.WRAP_CONTENT);
			int childHeightSpec = getChildMeasureSpec(heightMeasureSpec, 0, LayoutParams.WRAP_CONTENT);
			for (int i = 0, count = tags.size(); i < count; i++) {
				String tag = tags.get(i);
				if (i > 0) {
					x += spacing;
				}
				
				View badge = createOrGet(tag);
				if (count == 1) {
					badge.measure(MeasureSpec.makeMeasureSpec(width-x, MeasureSpec.AT_MOST), childHeightSpec);
				} else {
					badge.measure(childUnboundWidthSpec, childHeightSpec);
				}
				int measured = badge.getMeasuredWidth();
				if (measured > 0 && x + measured <= width) {
					// Fits
					stage(badge, x);
					x += badge.getMeasuredWidth();
					
				} else {
					// Does not fit, back track until the overflow fits
					do {
						int remaining = count-i;
						if (remaining > 0)  {
							String label = "+" + remaining;
							overflow.setText(label);
							overflow.measure(MeasureSpec.makeMeasureSpec(childUnboundWidthSpec, MeasureSpec.AT_MOST), childHeightSpec);
							if (x + overflow.getMeasuredWidth() <= width) {
								stage(overflow, x);
								break;
							}
						}
						if (i > 0) {
							x = ((LayoutParams) staged.remove(i-1).getLayoutParams()).x;
						}
						i--;
					} while (i >= 0);
					break;
				}
			}
		}
		
		int i = 0;
		int maxHeight = 0;
		int maxRight = 0;
		for (View child : staged) {
			maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
			maxRight = Math.max(maxRight, ((LayoutParams) child.getLayoutParams()).x + child.getMeasuredWidth());
			addViewInLayout(child, i++, child.getLayoutParams());
		}
		staged.clear();
		this.setMeasuredDimension(resolveSize(maxRight, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));
	}
	
	private TagBadgeView createOrGet(String tag) {
		TagBadgeView badge;
		if (recycler.isEmpty()) {
			badge = new TagBadgeView(getContext());
			badge.setLayoutParams(generateDefaultLayoutParams());
		} else {
			badge = recycler.remove(0);
		}
		badge.setTextAndUpdateEnUsLabel(tag, tag); // since tags are user created, they'll have the same label as what's displayed to the user
		badge.setSelected(highlightedTags.contains(tag));
		badge.setEnabled(isEnabled());
		badge.setOnClickListener(clickListener);
		badge.setClickable(clickListener != null);
		return badge;
	}
	
	private void stage(View view, int x) {
		LayoutParams lp = (LayoutParams) view.getLayoutParams();
		lp.x = x;
		view.setLayoutParams(lp);
		staged.add(view);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();
		int height = b-t;
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			int left = lp.x;
			int right = left + child.getMeasuredWidth();
			
			// center vertically
			int top = (int) ((height - child.getMeasuredHeight()) / 2f);
			int bottom = top + child.getMeasuredHeight();
			
			child.layout(left, top, right, bottom);
		}
	}
	
	public Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		public Binder clear() {
			tags(null);
			highlightedTags(null);
			badgeClick(null);
			return this;
		}
		
		public Binder tags(List<String> value) {
			tags.clear();
			if (value != null) {
				tags.addAll(value);
			}
			invalidate();
			requestLayout();
			return this;
		}
		
		public Binder highlightedTags(List<String> value) {
			highlightedTags.clear();
			if (value != null) {
				highlightedTags.addAll(value);
			}
			invalidate();
			requestLayout();
			return this;
		}

		public Binder badgeClick(OnClickListener listener) {
			clickListener = listener;
			for (int i = 0; i < getChildCount(); i++) {
				View v = getChildAt(i);
				if (v instanceof TagBadgeView) {
					v.setOnClickListener(listener);
					v.setClickable(listener != null);
				}
			}
			return this;
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		EnabledUtil.setChildrenEnabled(this, enabled, true);
	}
	
	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}
	
	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}
	
	@Override
	public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
		return new LayoutParams(getContext(), attributeSet);
	}
	
	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p);
	}
	
	public static class LayoutParams extends ViewGroup.LayoutParams {
		
		private int x;
		
		public LayoutParams(Context context, AttributeSet attributeSet) {
			super(context, attributeSet);
		}
		
		public LayoutParams(int width, int height) {
			super(width, height);
		}
		
		public LayoutParams(ViewGroup.LayoutParams layoutParams) {
			super(layoutParams);
		}
		
	}
	
	
}
