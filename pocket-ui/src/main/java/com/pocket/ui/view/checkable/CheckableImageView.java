package com.pocket.ui.view.checkable;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;

import com.pocket.ui.util.CheckableHelper;
import com.pocket.ui.view.themed.ThemedImageView;

public class CheckableImageView extends ThemedImageView implements CheckableHelper.Checkable {

	private final CheckableHelper mCheckable = new CheckableHelper(this, super::setContentDescription);

	public CheckableImageView(Context context) {
		this(context, null);
		mCheckable.initAttributes(context, null);
	}

	public CheckableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCheckable.initAttributes(context, attrs);
	}
	
	public CheckableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mCheckable.initAttributes(context, attrs);
	}

	@Override
	public void toggle() {
		mCheckable.toggle();
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
	public boolean performClick() {
		toggle();
		boolean handled = super.performClick();
		if (!handled) {
			// View only makes a sound effect if the onClickListener was
			// called, so we'll need to make one here instead.
			playSoundEffect(SoundEffectConstants.CLICK);
		}
		return handled;
	}

	@Override
	public void setChecked(boolean checked) {
		mCheckable.setChecked(checked);
	}
	
	@Override
	public void setCheckable(boolean value) {
		mCheckable.setCheckable(value);
	}

	/**
	 * Register a callback to be invoked when the checked state of this button changes.
	 * 
	 * @param listener
	 *            the callback to call on checked state change
	 */
	public void setOnCheckedChangeListener(CheckableHelper.OnCheckedChangeListener listener) {
		mCheckable.setOnCheckedChangeListener(listener);
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
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		invalidate();
	}

	@Override
	public Parcelable onSaveInstanceState() {
		return mCheckable.onSaveInstanceState(super.onSaveInstanceState());
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		state = mCheckable.onRestoreInstanceState(state);
		super.onRestoreInstanceState(state);
	}
	
	@Override
	public void setContentDescription(CharSequence contentDescription) {
		if (mCheckable != null) {
			mCheckable.setContentDescriptions(contentDescription, null);
		} else {
			super.setContentDescription(contentDescription);
		}
	}
}
