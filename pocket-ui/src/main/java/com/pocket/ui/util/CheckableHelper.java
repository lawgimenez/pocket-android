package com.pocket.ui.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.appcompat.widget.TooltipCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import com.pocket.ui.R;

import androidx.annotation.CheckResult;

/**
 * Helper for adding Checkableness to your view. A bare minimum implementation in your view subclass would look like this:
 *
 * <pre>
 *
 *   private final CheckableHelper mCheckable = new CheckableHelper(this);
 
     private void init(AttributeSet attrs) { // Invoke this from your constructors
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
 *
 * </pre>
 *
 * To support the `checkedContentDescription` xml attr that lets you specify content descriptions that change
 * when in a checked state, change your constructor to look like this and add this additional override:
 *
 * <pre>
     private final CheckableHelper mCheckable = new CheckableHelper(this, super::setContentDescription);
 
     @Override
     public void setContentDescription(CharSequence contentDescription) {
        if (mCheckable != null) {
          mCheckable.setContentDescriptions(contentDescription, null);
        } else {
          super.setContentDescription(contentDescription);
        }
     }
     
 * </pre>
 */
public class CheckableHelper implements Checkable {

    public static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
    public static final int[] CHECKABLE_STATE_SET = { android.R.attr.state_checkable };

    private final View mView;
    private final SuperSetContentDescription mContentDescriptionSetter;
    
    private boolean mIsChecked = false;
    private boolean mIsBroadcasting = false;
    private boolean mIsCheckable;
    private OnCheckedChangeListener mListener;
    private CharSequence mUncheckedContentDescription;
    private CharSequence mCheckedContentDescription;
    private boolean propagateChecks = true;
    
    public CheckableHelper(View view) {
        this(view, null);
    }
    
    /**
     * Use if your checked view wants to support the checkedContentDescription xml attr.
     * See main java doc on this class for details.
     */
    public CheckableHelper(View view, SuperSetContentDescription contentDescriptionSetter) {
        mView = view;
        mContentDescriptionSetter = contentDescriptionSetter;
    }
    
    public void initAttributes(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckableHelper);
            setCheckable(a.getBoolean(R.styleable.CheckableHelper_isCheckable, false));
            setContentDescriptions(mView.getContentDescription(), a.getText(R.styleable.CheckableHelper_checkedContentDescription));
            a.recycle();

            // TODO need to document and find a sane default for how this handles clickability. These views absorb touch events unless you explictly set clickable and longClickable to false and lead to confusing issues.
            // Need to set checkable views to clickable for them to toggle by default. But want to avoid overridding clickable:false if it was set in the xml.
            int[] set = {
                    android.R.attr.clickable
            };
            a = context.obtainStyledAttributes(attrs, set);
            mView.setClickable(a.getBoolean(0, true));
            a.recycle();
        } else {
            mView.setClickable(true);
        }
    }
    
    /**
     * Whether or not {@link #toggle()} will have any effect.
     *
     * @param value
     */
    public void setCheckable(boolean value) {
        if (mIsCheckable != value) {
            mIsCheckable = value;
            mView.refreshDrawableState();
            mView.invalidate();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (mIsChecked != checked) {
            mIsChecked = checked;

            // Avoid infinite recursions if setChecked() is called from a listener
            if (!mIsBroadcasting) {
                mIsBroadcasting = true;
                if (mListener != null) {
                    mListener.onCheckedChanged(mView, checked);
                }
                mIsBroadcasting = false;
            }
            reapplyContentDescription();
            
            mView.refreshDrawableState();
            mView.invalidate();

            if (propagateChecks) {
                setChildrenChecked(mView, checked);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void toggle() {
        if (isCheckable()) {
            setChecked(!mIsChecked);
        }
    }
    
    public boolean isCheckable() {
        return mIsCheckable;
    }

    public void shouldPropagateChecks(Boolean propagateChecks) {
        this.propagateChecks = propagateChecks;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mListener = listener;
    }
    
    public void setContentDescriptions(CharSequence regular, CharSequence checked) {
        mUncheckedContentDescription = regular;
        mCheckedContentDescription = checked;
        reapplyContentDescription();
    }
    
    private void reapplyContentDescription() {
        if (mContentDescriptionSetter != null) {
            CharSequence contentDescription = isChecked() && !TextUtils.isEmpty(mCheckedContentDescription) ? mCheckedContentDescription : mUncheckedContentDescription;
            mContentDescriptionSetter.setSuperContentDescription(contentDescription);
            TooltipCompat.setTooltipText(mView, contentDescription);
        } else {
            // Ignore, this feature is not in use on this helper.
        }
    }
    
    public Parcelable onSaveInstanceState(Parcelable superState) {
        SavedState ss = new SavedState(superState);
        ss.checked = isChecked();
        return ss;
    }

    @CheckResult public Parcelable onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            return state;
        }
        SavedState ss = (SavedState) state;
        setChecked(ss.checked);
        setCheckable(ss.checkable);
        return ss.getSuperState();
    }
    
    public interface Checkable extends android.widget.Checkable {
        void setOnCheckedChangeListener(OnCheckedChangeListener listener);
        void setCheckable(boolean isCheckable);
        boolean isCheckable();
    }
    
    public interface OnCheckedChangeListener {
        void onCheckedChanged(View view, boolean isChecked);
    }
    
    public interface SuperSetContentDescription {
        /**
         * Invoke your view's `super.setContentDescription()` method with the provided value.
         */
        void setSuperContentDescription(CharSequence value);
    }

    // REVIEW
    private static class SavedState extends View.BaseSavedState {
        boolean checked;
        boolean checkable;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            checked = (Boolean) in.readValue(null);
            checkable = (Boolean) in.readValue(null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeValue(checked);
            out.writeValue(checkable);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private static void setChildrenChecked(View parent, boolean checked) {
        if (parent instanceof ViewGroup) {
            for (int i = 0, count = ((ViewGroup)parent).getChildCount(); i < count; i++) {
                View child = ((ViewGroup)parent).getChildAt(i);
                if (child instanceof android.widget.Checkable) {
                    ((android.widget.Checkable) child).setChecked(checked);
                }
                setChildrenChecked(child, checked);
            }
        }
    }
    
}
