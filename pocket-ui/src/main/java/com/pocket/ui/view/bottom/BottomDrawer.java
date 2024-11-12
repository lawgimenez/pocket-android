package com.pocket.ui.view.bottom;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.pocket.analytics.api.UiEntityable;
import com.pocket.analytics.api.UiEntityableHelper;
import com.pocket.ui.R;
import com.pocket.ui.util.PocketUIViewUtil;
import com.pocket.util.android.AccessibilityUtils;
import com.pocket.util.java.RangeF;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * A bottom sheet styled for Pocket.
 * <p>
 * Add your content view with {@link #setLayout(int)} or declare it in xml with app:sheetLayout.
 * Your layout will be merged into a vertical LinearLayout.
 * <p>
 * A scrim for blacking out the content behind is available.
 * To enable it call {@link #setScrimAlpha(float, float, float)}.
 * To access it to modify or customize, use and {@link #getScrim()}.
 * <p>
 * Customize the bottom sheet behaviour with {@link #getBehavior()}
 * <p>
 * Note: The internals of this view will lazily be inflated after the first call to {@link #expand()} or {@link #collapse()}.
 * If you need it inflated before then, use {@link #inflate(int)}. Subclasses can override and use {@link #onLazyInflated()}
 * to do their setup.
 */
public class BottomDrawer extends CoordinatorLayout implements UiEntityable {
	
	private final Set<BottomSheetBehavior.BottomSheetCallback> callbacks = new HashSet<>();
	
	private int layoutId;
	private boolean isInflated;
	
	private View scrim;
	private View nav;
	private View back;
	private TextView title;
	private ViewGroup content;
	private PktBottomSheetBehavior<ViewGroup> bottomSheetBehavior;
	private boolean hideOnOutsideTouch;
	private float scrimAlphaWhenHidden = 0;
	private float scrimAlphaWhenCollapsed = 0;
	private float scrimAlphaWhenExpanded = 0;

	protected final UiEntityableHelper uiEntityable = new UiEntityableHelper();

	public BottomDrawer(Context context) {
		super(context);
		init(null);
	}
	
	public BottomDrawer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public BottomDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(AttributeSet attrs) {
		if (attrs != null) {
			uiEntityable.obtainStyledAttributes(getContext(), attrs);

			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BottomDrawer);
			int layout = a.getResourceId(R.styleable.BottomDrawer_sheetLayout, 0);
			if (layout != 0) {
				setLayout(layout);
			}
			a.recycle();
			// TODO support behavior_hideable, behavior_peekHeight and other bottom sheet attrs directly on this view
			// TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout);
		}
	}

	/**
	 * Ensure the view is inflated and fully ready to use.
	 *
	 * @param startState the initial {@link BottomSheetBehavior} state to set to.
	 */
	public void inflate(int startState) {
		if (isInflated) return;
		
		isInflated = true;
		LayoutInflater.from(getContext()).inflate(R.layout.view_bottom_sheet, this, true);
		scrim = findViewById(R.id.bottom_sheet_scrim);
		nav = findViewById(R.id.bottom_sheet_nav);
		back = findViewById(R.id.bottom_sheet_back);
		title = findViewById(R.id.bottom_sheet_title);
		content = findViewById(R.id.bottom_sheet_content);
		bottomSheetBehavior = (PktBottomSheetBehavior<ViewGroup>) BottomSheetBehavior.from(content);
		bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
		
		content.setClickable(true); // Prevent touches from passing through to content underneath
		scrim.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN && hideOnOutsideTouch) {
				hide();
				return true;
			}
			return false;
		});
		
		content.setBackground(new BottomSheetBackgroundDrawable(getContext()));
		
		if (layoutId != 0) {
			LayoutInflater.from(getContext()).inflate(layoutId, content, true);
		}
		
		onLazyInflated();

		// Start hidden and animate into start state after layout.
		getBehavior().setState(BottomSheetBehavior.STATE_HIDDEN);
		PocketUIViewUtil.runAfterNextLayoutOf(this, () -> getBehavior().setState(startState));
	}
	
	protected boolean isInflated() {
		return isInflated;
	}
	
	/**
	 * The view has been inflated, subclasses should initialize their views.
	 */
	protected void onLazyInflated() {}
	
	protected View getNav() {
		return nav;
	}

	protected TextView getTitle() {
		return title;
	}

	protected View getBack() {
		return back;
	}
	
	@SuppressWarnings("unused") protected View getScrim() {
		return scrim;
	}

	public ViewGroup contentParent() {
		return content;
	}

	/**
	 * Change the content container to match_parent height.
	 * This is useful if your content view wants to be as large as possible
	 * or if it contains a RecyclerView.
	 */
	protected void matchParentHeight() {
		ViewGroup.LayoutParams lp = content.getLayoutParams();
		lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
		content.setLayoutParams(lp);
	}
	
	/**
	 * Note: Use {@link #addBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback)} instead of setting a callback on this object.
	 */
	protected PktBottomSheetBehavior<ViewGroup> getBehavior() {
		return bottomSheetBehavior;
	}
	
	/**
	 * Set the layout res to be inflated, if and when it is lazily inflated. If already inflated, it will be set immediately.
	 */
	protected void setLayout(int layout) {
		layoutId = layout;
		if (isInflated()) {
			LayoutInflater.from(getContext()).inflate(layout, content, true);
		}
	}

	/**
	 * Set a view to use as the content view. Must be invoked after {@link #onLazyInflated()}.
	 */
	protected void setLayout(View layout) {
		content.addView(layout);
	}
	
	public void addBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback callback) {
		callbacks.add(callback);
	}
	
	@SuppressWarnings("unused")
	public void removeBottomSheetCallback(BottomSheetBehavior.BottomSheetCallback callback) {
		callbacks.remove(callback);
	}
	
	public void setHideOnOutsideTouch(boolean value) {
		hideOnOutsideTouch = value;
	}
	
	public void setScrimAlpha(float whenHidden, float whenCollapsed, float whenExpanded) {
		scrimAlphaWhenHidden = whenHidden;
		scrimAlphaWhenCollapsed = whenCollapsed;
		scrimAlphaWhenExpanded = whenExpanded;
		invalidate();
	}
	
	public void collapse() {
		if (!isInflated()) {
			inflate(BottomSheetBehavior.STATE_COLLAPSED);
		} else {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
	}
	
	public void hide() {
		if (!isInflated()) return;
		bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
	}
	
	public void expand() {
		if (!isInflated()) {
			inflate(BottomSheetBehavior.STATE_EXPANDED);
		} else {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		}
	}
	
	private void applyBottomSheetOffset(float slideOffset) {
		float alpha;
		if (slideOffset <= 0) {
			// Between hidden and collapsed
			alpha = RangeF.valueOf(scrimAlphaWhenHidden, scrimAlphaWhenCollapsed, slideOffset+1, RangeF.Constrain.BOTH);
		} else {
			// Between collapsed and expanded
			alpha = RangeF.valueOf(scrimAlphaWhenCollapsed, scrimAlphaWhenExpanded, slideOffset, RangeF.Constrain.BOTH);
		}
		scrim.setVisibility(alpha > 0 ? VISIBLE : GONE);
		scrim.getBackground().setAlpha((int) (alpha * 255));
	}

	public boolean isExpanded() {
		return isInflated && bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED;
	}

	public boolean isOpen() {
		return isInflated && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
	}

	/**
	 * Whether or not to hide the parent view tree from accessibility tools, such as Talkback,
	 * while in the collapsed or expanded states.
	 * @return true to hide the parent view tree from accessibility tools when collapsed or expanded.
	 */
	protected boolean hideParentFromAccessibility() {
		return true;
	}

	@Nullable @Override public String getUiEntityIdentifier() {
		return uiEntityable.getUiEntityIdentifier();
	}
	
	@Override public void setUiEntityIdentifier(@Nullable String uiEntityIdentifier) {
		uiEntityable.setUiEntityIdentifier(uiEntityIdentifier);
	}
	
	@Nullable @Override public Type getUiEntityType() {
		return uiEntityable.getUiEntityType();
	}
	
	@Nullable @Override public String getUiEntityComponentDetail() {
		return uiEntityable.getUiEntityComponentDetail();
	}
	
	@Override public void setUiEntityComponentDetail(@Nullable String value) {
		uiEntityable.setUiEntityComponentDetail(value);
	}
	
	@Nullable @Override public String getUiEntityLabel() {
		return uiEntityable.getUiEntityLabel();
	}

	private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {

		private AccessibilityUtils.BottomSheetHelper bottomSheetAccessibilityHelper = hideParentFromAccessibility() ? new AccessibilityUtils.BottomSheetHelper() : null;

		@Override
		public void onStateChanged(@NonNull View bottomSheet, int newState) {
			switch (newState) {
				case BottomSheetBehavior.STATE_COLLAPSED:
					applyBottomSheetOffset(0);
					break;
				case BottomSheetBehavior.STATE_HIDDEN:
					applyBottomSheetOffset(-1);
					break;
				case BottomSheetBehavior.STATE_EXPANDED:
					applyBottomSheetOffset(1);
					break;
				case BottomSheetBehavior.STATE_DRAGGING:
				case BottomSheetBehavior.STATE_HALF_EXPANDED:
				case BottomSheetBehavior.STATE_SETTLING:
					// onSlide should handle these.
					break;
			}

			if (bottomSheetAccessibilityHelper != null) bottomSheetAccessibilityHelper.updateAccessibilityState(BottomDrawer.this, newState, true);

			for (BottomSheetBehavior.BottomSheetCallback callback : callbacks) {
				callback.onStateChanged(bottomSheet, newState);
			}
		}
		@Override
		public void onSlide(@NonNull View bottomSheet, float slideOffset) {
			if (Float.isNaN(slideOffset)) {
				// Seems like maybe a support lib bug when near expanded state...?  Just ignore for now. The onStateChange dispatch should give us a final value.
				return;
			}
			
			applyBottomSheetOffset(slideOffset);
			
			for (BottomSheetBehavior.BottomSheetCallback callback : callbacks) {
				callback.onSlide(bottomSheet, slideOffset);
			}
		}
	}

	public void showAsDialog() {
		final Dialog dialog = new Dialog(getContext(), R.style.Pkt_BottomDrawerDialog);
		addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override
			public void onStateChanged(@NonNull View view, int state) {
				if (state == BottomSheetBehavior.STATE_HIDDEN) {
					dialog.dismiss();
				}
			}
			@Override
			public void onSlide(@NonNull View view, float v) {}
		});
		dialog.setOnKeyListener((dialog1, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				hide();
				return true;
			}
			return false;
		});
		dialog.setContentView(this);
		dialog.show();

		inflate(BottomSheetBehavior.STATE_EXPANDED);
	}
}
