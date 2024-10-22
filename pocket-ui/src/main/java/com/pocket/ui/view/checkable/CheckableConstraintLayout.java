package com.pocket.ui.view.checkable;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;

import com.pocket.ui.util.CheckableHelper;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

/**
 */
public class CheckableConstraintLayout extends VisualMarginConstraintLayout implements CheckableHelper.Checkable {
	
	private final CheckableHelper mCheckable = new CheckableHelper(this, super::setContentDescription);
	
	public CheckableConstraintLayout(Context context) {
		super(context);
		init(null);
	}
	
	public CheckableConstraintLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
	}
	
	public CheckableConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
	}
	
	private void init(AttributeSet attrs) {
		mCheckable.initAttributes(getContext(), attrs);
	}
	
	@Override
	public void setChecked(boolean checked) {
		mCheckable.setChecked(checked);
	}
	
	@Override
	public void setCheckable(boolean value) {
		mCheckable.setCheckable(value);
	}
	
	@Override
	public boolean isChecked() {
		if (mCheckable != null) {
			return mCheckable.isChecked();
		} else {
			return false;
		}
	}
	
	@Override
	public boolean isCheckable() {
		if (mCheckable != null) {
			return mCheckable.isCheckable();
		} else {
			return false;
		}
	}
	
	@Override
	public void toggle() {
		mCheckable.toggle();
	}
	
	@Override
	public boolean performClick() {
		toggle(); // Important to toggle before invoking the click listener.
		boolean handled = super.performClick();
		if (!handled) {
			// View only makes a sound effect if the onClickListener was
			// called, so we'll need to make one here instead.
			playSoundEffect(SoundEffectConstants.CLICK);
		}
		return handled;
	}
	
	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CheckableHelper.CHECKED_STATE_SET);
		}
		if (isCheckable()) {
			mergeDrawableStates(drawableState, CheckableHelper.CHECKABLE_STATE_SET);
		}
		return drawableState;
	}
	
	@Override
	public void setOnCheckedChangeListener(CheckableHelper.OnCheckedChangeListener listener) {
		mCheckable.setOnCheckedChangeListener(listener);
	}
	
	@Override
	public void setContentDescription(CharSequence contentDescription) {
		if (mCheckable != null) {
			mCheckable.setContentDescriptions(contentDescription, null);
		} else {
			super.setContentDescription(contentDescription);
		}
	}

	public void shouldPropagateChecks(Boolean propagateChecks) {
		mCheckable.shouldPropagateChecks(propagateChecks);
	}
	
}
