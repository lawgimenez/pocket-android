package com.pocket.sdk.util.dialog;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.pocket.app.App;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.util.android.fragment.FragmentUtil;

/**
 * More helper methods on top of RilDialogFragment.
 * 
 * Extend this class for easier DialogFragment creation. In order to extend this class:
 * 
 * Create a static method that creates a new instance of your class. Such as getNew(). In this method init your class such as:
 * 
 * MyDialogFragment frag = new MyDialogFragment();
 * 
 * With no params, just an empty constructor.
 * 
 * Then in getNew() call a variant of frag.createArgs() with your title and message.
 * 
 * If you want your dialog to only be able to have one visible instance at once, look at extending  onlyAllowOneInstanceAtATime(), isShowingInstance() and setShowingInstance().
 * 
 * 
 * If your dialog needs more than just a title and message, create a setter for the value and override onCreateArgs. During onCreateArgs,
 * add the set value to the supplied arguments.
 * 
 * A completed example:
 * 
 * 	public static MyDialogFragment getNew() {
 * 		MyDialogFragment frag = new MyDialogFragment();
 * 		frag.setSomeValue("A value");
 * 		frag.createArgs("title", "message");	
 * 	}
 * 
 * 	private String mSomeValue;
 * 
 * 	public void setSomeValue(String value) {
 * 		mSomeValue = value;
 * 	}
 * 
 * 	@ Override
 * 	protected Bundle onCreateArgs(Bundle args) {
 * 		args.putString(SOME_VALUES_KEY, mSomeValue);
 * 	}
 * 
 * 	Then, to build your dialog, override onCreateDialog(Bundle savedInstanceState).
 * 
 *  For more built in functionality look at AlertMessaging
 * 
 * @author max
 *
 */
public abstract class ExtendedDialogFragment extends RilDialogFragment {
	
	protected static final String KEY_TITLE = "title";
	protected static final String KEY_MESSAGE = "message";
	
	/**
	 * Show the dialog on whatever RilAppActivity is currently on screen. If there is no activity on screen it will not show.
	 * OPT allow it to show on the next activity launch if it occurs within the next second.
	 * OPT this shouldn't be used very often. We should know what activity it should appear in, in most cases.
	 */
	public void showOnCurrentActivity() {
		show(null);
	}
	
	/**
	 * Will show the dialog fragment on the activity, unless the activity is null or finishing, then it will try to show it on the current active RilAppActivity if any.
	 * @param activity
	 */
	public void show(FragmentActivity activity) {
		if (onlyAllowOneInstanceAtATime() && isShowingInstance())
			return;
		
		if (activity != null && !activity.isFinishing()) {
			setShowingInstance(true);
			FragmentUtil.addFragmentAsDialog(this, activity, getTag(), false, false);
			
		} else {
			final AbsPocketActivity currentActivity = App.getActivityContext();
			
			if (currentActivity == null) 
				return;
			
			App.getApp().threads().postOnUiThread(() -> {
				setShowingInstance(true);
				FragmentUtil.addFragmentAsDialog(this, currentActivity, getTag(), false, false);
			});
		}
	}
	
	/**
	 * Whether or not this class of dialog should be able to have multiples shown to the user at the same time.
	 * Use this when the dialog might be opened from multiple places at once such as database or file errors.
	 * This will prevent a flood of dialogs from being opened.
	 * 
	 * @return false to allow multiples, true to ignore show attempts while one is already open.
	 */
	protected boolean onlyAllowOneInstanceAtATime() {
		return false;
	}
	
	/**
	 * Whether or not an instance of this dialog is currently showing. Subclasses should maintain a static flag that is toggled by setShowingInstance().
	 * @return the dialogs static flag, set by setShowingInstance()
	 */
	protected abstract boolean isShowingInstance();
	
	/**
	 * Subclasses should maintain a private static boolean that is set by this method and returned in isShowingInstance
	 * @param isShowing
	 */
	protected abstract void setShowingInstance(boolean isShowing);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setShowingInstance(true);
		mWasRestored = savedInstanceState != null;
		if (mWasRestored) {
			mShouldPersist = savedInstanceState.getBoolean(STATE_SHOULD_PERSIST);
			if (!mShouldPersist) {
				setShowsDialog(false);
				dismiss();
			}
		}
		
	};
	
	protected void createArgs(int title, int message) {
		createArgs(App.getStringResource(title), App.getStringResource(message));
	}
	
	protected void createArgs(int title, String message) {
		createArgs(App.getStringResource(title), message);
	}
	
	public void createArgs(String title, int message) {
		createArgs(title, App.getStringResource(message));
	}
	
	public void createArgs(String title, String message) {
		Bundle args = new Bundle();
		
		if (title != null)
			args.putString(KEY_TITLE, title);
		
		if (message != null)
			args.putString(KEY_MESSAGE, message);
		
		args = onCreateArgs(args);
		setArguments(args);
	}
	
	// Hook for adding additional args
	protected Bundle onCreateArgs(Bundle args) {
		return args;
	}
	
	protected boolean allowCancel() {
		return true;
	}
	
	@Override
	protected void onClose(boolean isCancel) {
		super.onClose(isCancel);
		setShowingInstance(false);
	};
	
}
