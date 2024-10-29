package com.pocket.sdk.util.view.tooltip.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.ideashower.readitlater.R;
import com.pocket.sdk.util.view.tooltip.Tooltip;
import com.pocket.ui.view.bottom.BottomDrawer;
import com.pocket.util.android.ViewUtilKt;
import com.pocket.util.android.animation.AnimatorEndListener;

/**
 * Tooltip view that shows a bottom sheet with a message and two buttons.
 */
public final class BottomSheetTooltip extends BottomDrawer implements TooltipView {

	private TextView title;
	private TextView message;
	private TextView buttonPositive;
	private TextView buttonNeutral;
	
	private AnimatorEndListener animatorEndListener;
	
	public BottomSheetTooltip(@NonNull Context context) {
		super(context);
		inflate(BottomSheetBehavior.STATE_HIDDEN);
	}
	
	@Override
	protected void onLazyInflated() {
		super.onLazyInflated();
		setLayout(R.layout.view_tooltip_bottom);

		title = findViewById(R.id.title);
		message = findViewById(R.id.message);
		buttonPositive = findViewById(R.id.button_positive);
		buttonNeutral = findViewById(R.id.button_neutral);
		
		getBehavior().setHideable(true);
		
		addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
			@Override public void onStateChanged(@NonNull View view, int newState) {
				// Lock dragging.
				if (newState == BottomSheetBehavior.STATE_DRAGGING) {
					getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
				}
				
				// Simulate animation end listener.
				if (newState == BottomSheetBehavior.STATE_HIDDEN && animatorEndListener != null) {
					animatorEndListener.onAnimationEnd(null);
				}
			}
			
			@Override public void onSlide(@NonNull View view, float v) {}
		});
	}
	
	public BottomSheetTooltip withText(CharSequence title, int message, int buttonPositive, int buttonNeutral) {
		return withText(title, message, buttonPositive, null, buttonNeutral, null);
	}

	public BottomSheetTooltip withText(
			CharSequence title,
			int message,
			int buttonPositive,
			OnClickListener onPositiveButtonClick,
			int buttonNeutral, OnClickListener onNeutralButtonClick) {
		ViewUtilKt.setTextOrHide(this.title, title);
		this.message.setText(message);
		this.buttonPositive.setText(buttonPositive);
		this.buttonPositive.setOnClickListener(onPositiveButtonClick);
		this.buttonNeutral.setText(buttonNeutral);
		this.buttonNeutral.setOnClickListener(onNeutralButtonClick);
		return this;
	}
	
	@Override public void bind(Tooltip.TooltipController controller) {
		buttonPositive.setOnClickListener(v -> controller.clickAnchor(Tooltip.DismissReason.BUTTON_CLICKED));
		buttonNeutral.setOnClickListener(v -> controller.dismiss());
	}
	
	@Override public View getView() {
		return this;
	}
	
	@Override public boolean applyAnchor(int[] xy, Rect anchorBounds, Rect windowBounds) {
		// If the anchor is too low we're just going to cover it.
		return anchorBounds.bottom < windowBounds.bottom - contentParent().getMeasuredHeight();
	}
	
	@Override public void animateIn() {
		expand();
	}
	
	@Override public void animateOut(AnimatorEndListener callback) {
		animatorEndListener = callback;
		hide();
	}
	
	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(fixMeasureSpec(widthMeasureSpec), fixMeasureSpec(heightMeasureSpec));
	}
	
	/**
	 * Make sure this tooltip expands to fill available space when laid out with wrap_content.
	 */
	private static int fixMeasureSpec(int measureSpec) {
		return MeasureSpec.getMode(measureSpec) == MeasureSpec.AT_MOST
				? MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(measureSpec), MeasureSpec.EXACTLY) : measureSpec;
	}
}
