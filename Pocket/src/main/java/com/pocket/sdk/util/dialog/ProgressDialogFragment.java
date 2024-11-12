package com.pocket.sdk.util.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

public class ProgressDialogFragment extends ExtendedDialogFragment {
	
	public static final int TYPE_CLEARING_CACHE = 1;
	public static final int TYPE_CHANGING_DATA_LOCATION = 2;
	
	protected static final String ARG_CANCELABLE = "cancelable";

	private static boolean mIsShowingProgressDialogFragment = false;
	
	private boolean mCancellable;
	
	public static ProgressDialogFragment getNew(int message, boolean cancelable) {
		return getNew(message, null, cancelable);
	}
	
	public static ProgressDialogFragment getNew(int message, String tag, boolean cancelable) {
		ProgressDialogFragment frag = new ProgressDialogFragment();
		frag.createArgs(null, message);
		return frag;
	}
	
	public static ProgressDialogFragment getNew(String message, String tag, boolean cancelable) {
		ProgressDialogFragment frag = new ProgressDialogFragment();
		frag.createArgs(null, message);
		return frag;
	}
	
	protected void setCancellable(boolean value){
		mCancellable = value;
	}
	
	@Override
	protected Bundle onCreateArgs(Bundle args) {
		args.putBoolean(ARG_CANCELABLE, mCancellable);
		return super.onCreateArgs(args);
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Basic setup
		String message = getArguments().getString(KEY_MESSAGE);
		boolean cancellable = getArguments().getBoolean(ARG_CANCELABLE);
		
		ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(message);
		dialog.setIndeterminate(true);
		
		// Some final setup
		dialogCancelableSetup(this, dialog, cancellable);
		return dialog;
	}
	
	@Override
	protected boolean isShowingInstance() {
		return mIsShowingProgressDialogFragment;
	}

	@Override
	protected void setShowingInstance(boolean isShowing) {
		mIsShowingProgressDialogFragment = isShowing;
	}
	

}
