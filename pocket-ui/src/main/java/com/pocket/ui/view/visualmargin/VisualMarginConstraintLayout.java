package com.pocket.ui.view.visualmargin;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.pocket.ui.R;
import com.pocket.ui.view.themed.ThemedConstraintLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * A ConstraintLayout that has tools for getting visually pixel perfect spacing between elements.
 * <p>
 * This mostly aids when trying to get an exact vertical spacing between a TextView and another element.
 * Android's TextView's bounding box includes space for the ascent and descent of text, but often
 * designers want the vertical space between elements to be measured from the baseline or ascent/top
 * of text. This is challenging to accomplish in xml without having to manually tweak each margin to
 * get it just right.
 * <p>
 * With this view, you can specify the visual margin you want between elements and this will calculate
 * it and adjust the position so the margin accounts for ascent or descent of text. For example:
 *
 * <TextView
 * 		android:id="@+id/above"
 * 		android:layout_width="wrap_content"
 * 		android:layout_height"wrap_content" />
 *
 * <TextView
 * 		android:id="@+id/below"
 * 		android:layout_width="wrap_content"
 * 		android:layout_height"wrap_content"
 * 		app:layout_constraintTop_toBottomOf="@+id/above"
 * 		app:visualMargin_top="@dimen/pkt_space_md" />
 *
 * With a normal layout_marginTop, visually this would end up with more space between the two views as desired, it
 * would have pkt_space_md plus the descent of the above view plus the ascent of the below view.
 * <p>
 * Using "visualMargin_top", this view will take into account the ascent and descent and layout
 * the two views so they visually appear exactly pkt_space_md apart.
 * <p>
 * <h2>Implementation Notes/Limitations</h2>
 * Currently, this only supports layout_constraintTop_toBottomOf.
 * <p>
 * You must also use views that implement {@link VisualMargin}. {@link com.pocket.ui.view.themed.ThemedTextView} supports
 * it, so if you are  already using that for text views, it is ready to use.
 * <p>
 * Also see docs on {@link VisualMargin} and its methods for some additional details.
 * <h2>Gone and Chains</h2>
 * If it is anchored to a view that is gone, it will calculate its margin onto the next non-gone anchor
 * in the chain, following layout_constraintTop_toBottomOf. You can also set a visualMargin_goneTop value
 * for a visual margin to use when all anchors above it in a chain are gone.
 */
public class VisualMarginConstraintLayout extends ThemedConstraintLayout implements VisualMargin {

	public VisualMarginConstraintLayout(Context context) {
		super(context);
	}

	public VisualMarginConstraintLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public VisualMarginConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Prepare anchors.
		// This is done before the first measure so any changes here are accounted for.
		// TODO  optimize? since this duplicates a bit of process that happens in the final step
		for (int i = 0, count = getChildCount(); i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == GONE)  {
				continue;
			}
			if (((LayoutParams) child.getLayoutParams()).visualMarginTop != 0) {
				View anchor = resolveTopAnchorOf(child);
				prepareAscent(child);
				prepareDescent(anchor);
			}
		}
		
		// Do a measure pass, so child views are measured and given position data
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// Then adjust margins to be visual margins
		boolean changed = false;
		for (int i = 0, count = getChildCount(); i < count; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == GONE)  {
				continue;
			}
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			if (((LayoutParams) child.getLayoutParams()).visualMarginTop != 0) {
				int included;
				View anchor = resolveTopAnchorOf(child);
				changed = prepareAscent(child) || changed;
				changed = prepareDescent(anchor) || changed;
				included = calculateAscentOf(child);
				included += calculateDescentOf(anchor);
				int targetMargin = lp.visualMarginGoneTop != null && (anchor == null || anchor == this) ? lp.visualMarginGoneTop : lp.visualMarginTop;
				int visualMargin = Math.max(0, targetMargin - included);  // ConstraintLayout does not support negative margins.
				if (visualMargin != lp.topMargin) {
					lp.topMargin = visualMargin;
					child.setLayoutParams(lp);
					changed = true;
				}
			}
		}
		
		if (changed) {
			// Measure again with new margins
			requestLayout();
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}
	
	private boolean prepareAscent(View view) {
		return view instanceof VisualMargin && ((VisualMargin) view).prepareVisualAscent();
	}
	
	private boolean prepareDescent(View view) {
		return view instanceof VisualMargin && ((VisualMargin) view).prepareVisualDescent();
	}
	
	private View resolveTopAnchorOf(View view) {
		LayoutParams lp = (LayoutParams) view.getLayoutParams();
		View anchor = findViewById(lp.topToBottom);
		if (anchor == null) {
			return null;
		} else if (anchor.getVisibility() == VISIBLE || anchor.getVisibility() == INVISIBLE) {
			return anchor;
		} else {
			return resolveTopAnchorOf(anchor); // Check if there is one up in a chain.
		}
	}
	
	/**
	 * @return visual space between this view's getTop() and its visual/content top
	 */
	private int calculateAscentOf(View view) {
		return view instanceof VisualMargin && view.getVisibility() == VISIBLE ? ((VisualMargin) view).visualAscent() : 0;
	}

	/**
	 * @return visual space between this view's getBottom() and its visual/content bottom
	 */
	private int calculateDescentOf(View view) {
		return view instanceof VisualMargin && view.getVisibility() == VISIBLE ? ((VisualMargin) view).visualDescent() : 0;
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return generateDefaultLayoutParams();
	}
	
	@Override
	public boolean prepareVisualAscent() {
		return false;
	}
	
	@Override
	public boolean prepareVisualDescent() {
		return false;
	}
	
	@Override
	public int visualAscent() {
		return 0;
	}
	
	@Override
	public int visualDescent() {
		return 0;
	}
	
	public static class LayoutParams extends ConstraintLayout.LayoutParams {

		public int visualMarginTop;
		public Integer visualMarginGoneTop;

		public LayoutParams(VisualMarginConstraintLayout.LayoutParams source) {
			super(source);
			this.visualMarginTop = source.visualMarginTop;
			this.visualMarginGoneTop = source.visualMarginGoneTop;
		}
		
		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}
		
		public LayoutParams(int width, int height) {
			super(width, height);
		}
		
		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			
			if (attrs != null) {
				TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VisualMarginConstraintLayout_Layout);
				visualMarginTop = a.getDimensionPixelSize(R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_top, 0);
				if (a.hasValue(R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_goneTop)) {
					visualMarginGoneTop = a.getDimensionPixelSize(R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_goneTop, 0);
				}
				a.recycle();
			}
			
			if (visualMarginTop > 0)  {
				topMargin = visualMarginTop; // Start with the visual margin as a top margin, we'll likely need to reduce it, but this gets the layout close in the first pass.
			}
		}
		
	}
	
}
