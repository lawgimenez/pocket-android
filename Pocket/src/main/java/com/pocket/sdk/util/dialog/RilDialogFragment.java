package com.pocket.sdk.util.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

import com.pocket.app.App;

import androidx.fragment.app.DialogFragment;

/**
 * An abstract base class for creating dialog fragments.
 * 
 * Adds extra functionality and helper methods for the lifecycle of the dialog.
 * 
 * @author max
 *
 */
public abstract class RilDialogFragment extends DialogFragment {
	
	protected static final String STATE_SHOULD_PERSIST = "stateShouldPersist";
	
	private OnCloseListener mOnCloseListener;
	private boolean mCalledDimissListener = false;
	protected boolean mWasRestored = false;
	protected boolean mShouldPersist = true;
	
	public void setShouldPersist(boolean value) {
		mShouldPersist = value;
	}
	
	protected static Dialog dialogCancelableSetup(DialogFragment fragment, Dialog dialog, boolean cancelable){
		// Ensures proper setup, including workaround fixes, for whatever cancelable mode the dialog is given
		
		if(!cancelable){
			fragment.setCancelable(false);
			dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
	
			    @Override
			    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
			        if (keyCode == KeyEvent.KEYCODE_SEARCH ){ 
			        	// Workaround, don't let the search button close a non-cancelable dialog
			            return true;
			        }
			        return false;
			    }
			});
		} else {
			dialog.setCanceledOnTouchOutside(true);
		}
		
		return dialog;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_SHOULD_PERSIST, mShouldPersist);
	};
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		onClose(false);
	};
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		onClose(true);
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		onClose(false);
	};
	
	protected void onClose(boolean isCancel) {
		if (mOnCloseListener != null) {
			if (isCancel) {
				mOnCloseListener.onCancel(this);
				
			} else if (!mCalledDimissListener) {
				mCalledDimissListener = true;
				mOnCloseListener.onDismiss(this);
			}
		}
	}
		
	/**
	 * This will not be restored
	 */
	public void setOnCloseListener(OnCloseListener listener) {
		mOnCloseListener = listener;
	}
	
	public interface OnCloseListener {
		public void onCancel(RilDialogFragment frag);
		public void onDismiss(RilDialogFragment frag);
	}
	
	/**
	 * API HACK
	 * since the support package does not yet have the dismissAllowingStateLoss() method available,
	 * this is a workaround.
	 * 
	 * Looking at the source code, the support package's onDismiss has the following contents:
	 * if (!mRemoved) {
            // Note: we need to use allowStateLoss, because the dialog
            // dispatches this asynchronously so we can receive the call
            // after the activity is paused.  Worst case, when the user comes
            // back to the activity they see the dialog again.
            dismissInternal(true);
        }
     * So by calling the private method dismissInternal(true), this accomplishes the same thing. The interface is ignored,
     * so it is safe to send a null value provided that overrides of this method don't need it.
	 */
	public void dismissAllowingStateLoss() {
		if (getFragmentManager() == null || isRemoving())
			return; // already removed.
		
		onDismiss(null);
	}
	
	/**  // OPT there is a copy of this method in OptionalDialogFragment.  sharing is caring.
	 * When fragments detach, they do not have access to resources and this can crash.  So instead of 
	 * using getString(), you can use this method which will get the string from the App context instead.
	 * @param res
	 * @return
	 */
	public String getStringSafely(int res){
		return App.getStringResource(res);
	}
	
}
