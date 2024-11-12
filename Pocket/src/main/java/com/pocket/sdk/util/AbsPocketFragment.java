package com.pocket.sdk.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.pocket.analytics.ImpressionComponent;
import com.pocket.analytics.ImpressionRequirement;
import com.pocket.analytics.Tracker;
import com.pocket.analytics.UiEntityType;
import com.pocket.app.App;
import com.pocket.app.PocketApp;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.util.fragment.PocketFragmentManager;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.fragment.FragmentUtil;
import com.pocket.util.android.view.DialogSizeWrapper;
import com.pocket.util.java.Logs;

import java.util.ArrayList;

/**
 * A base Fragment class that handles showPage life cycles. Any fragments that
 * opened from a showPage() call or that want to showPages should subclass this.
 * 
 * Extends DialogFragment instead of Fragment so that subclasses can implement
 * themselves as dialogs or not.
 * 
 * @author max
 * 
 */
public abstract class AbsPocketFragment extends AppCompatDialogFragment {

	/**
	 * @deprecated get components through dagger/hilt dependency injection
	 */
	@Deprecated
	private PocketApp mApp;
	
	/**
	 * If this fragment is displayed as a dialog, this is the root view.
	 * @see AbsPocketFragment#getViewRoot()
	 * @see #mRootView
	 */
	private View mDialogRootView;
	/**
	 * The view returned by {@link #onCreateViewImpl(LayoutInflater, ViewGroup, Bundle)}
	 * @see AbsPocketFragment#getViewRoot()
	 * @see #mDialogRootView
	 */
	private View mRootView;
	
	private ArrayList<OnFragmentDestoryListener> mOnDestoryListeners; // REVIEW convert to use the new OnFragmentLifeCycleChangedListener
	
	/**
	 * @throws IllegalStateException if fragment hasn't been attached to a context
	 * @deprecated get components through dagger/hilt dependency injection
	 */
	@Deprecated
	protected PocketApp app() {
		if (mApp == null) {
			throw new IllegalStateException("Fragment " + this + " hasn't been attached to a context yet.");
		}
		
		return mApp;
	}
	
	/**
	 * @throws IllegalStateException if fragment hasn't been attached to a context
	 * @deprecated get components through dagger/hilt dependency injection
	 */
	@Deprecated
	public Pocket pocket() {
		return app().pocket();
	}
	
	/**
	 * Returning the name of the current view for action context.
	 */
	public CxtView getActionViewName() {
		return null;
	}
	
	/** Returns an identifier for the screen represented by this fragment. */
	public @Nullable UiEntityIdentifier getScreenIdentifier() {
		return null;
	}

	public @Nullable String getScreenIdentifierString() {
		return null;
	}
	
	/**
	 * Use instead of {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
	 * This method should pretty much be limited to inflating and creating your view.
	 * Do bindings, findViewById and other setup in {@link #onViewCreatedImpl(View, Bundle)}
	 */
	protected abstract View onCreateViewImpl(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);
	
	/**
	 * Use instead of {@link #onViewCreated(View, Bundle)}.
	 */
	protected void onViewCreatedImpl(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Tracker tracker = app().tracker();
		if (getScreenIdentifierString() != null) {
			tracker.bindUiEntityType(view, UiEntityType.SCREEN);
			tracker.bindUiEntityIdentifier(view, getScreenIdentifierString());
		} else if (getScreenIdentifier() != null) {
			tracker.bindUiEntityType(view, UiEntityType.SCREEN);
			tracker.bindUiEntityIdentifier(view, getScreenIdentifier().value);
		}
	}
	
	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		
		// Check if Activity is the correct type.
		if (!(activity instanceof AbsPocketActivity)) {
			Logs.throwIfNotProduction("AbsPocketFragment requires the parent Activity to be a AbsPocketActivity in order to use the additional functionality and APIs");
		}
		
		mApp = App.from(activity);
	}
	
	@Override
	public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (getShowsDialog()) {
			return null; // View will be added to the dialog in onCreateDialog instead.
		}
		
		mRootView = onCreateViewImpl(inflater, container, savedInstanceState);
		return mRootView;
	}
	
	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		onViewCreatedImpl(view, savedInstanceState);
	}
	
	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// If the fragment is shown as a DialogFragment, this is the replacement to onCreateView.
		
		setStyle(STYLE_NO_TITLE, 0);
		
		DialogSizeWrapper wrapper = null;
		if (FormFactor.showSecondaryScreensInDialogs(getActivity())) {
			wrapper = new DialogSizeWrapper(getActivity());
		}
		
		mRootView = onCreateViewImpl(LayoutInflater.from(getActivity()), wrapper, savedInstanceState);
		onViewCreatedImpl(mRootView, savedInstanceState);
		
		if (wrapper != null) {
			wrapper.addView(mRootView);
			mDialogRootView = wrapper;
		} else {
			mDialogRootView = mRootView;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(mDialogRootView);
		AlertDialog dialog = builder.create();
		dialog.setOnKeyListener(new OnKeyListener() {
			
			/**
			 * BUG it seems like in this case, onKey gets called twice on BACK.
			 * Might be a bug in android, needs more investigation, but for now here is a cheap
			 * workaround.
			 * 
			 * We will ignore the first back button call and only respond to the second.
			 */
			private boolean backPressed = false;
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					if (backPressed) {
						// Second one, react to this one.
						backPressed = false; // Reset
						return onBackPressed();
						
					} else {
						// First one, ignore this one.
						backPressed = true;
						return false;
					}
				}
				
				// Workaround for a hard search button (on older devices) closing open dialogs.
				if (keyCode == KeyEvent.KEYCODE_SEARCH ) { 
					return true; // Prevent search from closing the dialog. This is ok because we don't use Activity.onSearchRequested for anything.
				}
				
				return false;
			}
		});
		dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		
		setupDialogWithNoTopSpace(mDialogRootView);
		
		return dialog;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		// Set root view as clickable to avoid touch events passing down to views below this fragment
		if (getViewRoot() != null) {
			getViewRoot().setClickable(true);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		if (getShowsDialog()) {
			// HACK to make dialog fragments appear correctly
			ViewGroup parent = (ViewGroup) mDialogRootView.getParent();
			parent.setPadding(0, 0, 0, 0);
		}

		app().tracker().trackImpression(mRootView,
				ImpressionComponent.SCREEN,
				ImpressionRequirement.INSTANT,
				null);
	}

	/**
	 * Invoked during the parent Activity's Activity#onRestart() callback.
	 */
	public void onRestart() {}
	
	/**
	 * The opposite of {@link #onLostFocus()}. A callback to inform the fragment that it is likely users direct focus again.
	 * Invoked when a {@link FragmentTransaction} pops the back stack and this fragment is once again at the top of the stack.
	 * <p>
	 * A common example is if another {@link DialogFragment} is added above this fragment. This fragment is still partially
	 * visible but the DialogFragment is now the "focus". Then that dialog is dismissed. That is when this fragment will regain "focus".
	 * 
	 * @see #onRegainedFocus()
	 */
	public void onRegainedFocus() {}
	
	/**
	 * A callback to inform the fragment that it is likely no longer the users direct focus. Invoked when a {@link FragmentTransaction} adds something to the back stack.
	 * <p>
	 * A common example is if another {@link DialogFragment} is added above this fragment. This fragment is still partially
	 * visible but the DialogFragment is now the "focus".
	 * 
	 * @see #onLostFocus()
	 */
	public void onLostFocus() {}
	
	/**
	 * Dismiss/hide this fragment. If it is a root fragment it will finish the Activity.
	 */
	public void finish() {
		AbsPocketActivity activity = (AbsPocketActivity) getActivity();
		if (activity == null) {
			return;
		}
		
		((PocketFragmentManager) activity.getSupportFragmentManager())
			.finishFragment(this, getActivity());
	}

	/**
	 * Convenience method for calling {@link FragmentUtil#isDetachedOrFinishing(Fragment)} with itself.
	 */
	public boolean isDetachedOrFinishing() {
		return FragmentUtil.isDetachedOrFinishing(this);
	}
		
	/**
	 * Convenience method for calling {@link FragmentUtil#isFinishing(Fragment)} with itself.
	 */
	public boolean isFinishing() {
		return FragmentUtil.isFinishing(this);
	}
	
	/**
	 * The app's theme (dark/light mode) has changed.
	 */
	public void onThemeChanged(int newTheme) { 
		ViewUtil.refreshDrawableStateDeep(getViewRoot());
	}

	/**
	 * A convenience for returning the Fragment's activity casted to
	 * {@link AbsPocketActivity}
	 */
	public AbsPocketActivity getAbsPocketActivity() {
		return (AbsPocketActivity) getActivity();
	}
	
	/**
	 * Similar to {@link AbsPocketActivity#getDefaultThemeFlag()} in that it declares what themes are supported
	 * by this fragment. By default it will be whatever the activity supports. If your fragment is more limited,
	 * override this to declare your allowed themes.
	 */
	public int getThemeFlag() {
		AbsPocketActivity activity = getAbsPocketActivity();
		if (activity != null) {
			return activity.getThemeFlag();
		} else {
			return Theme.FLAG_ALLOW_ALL;
		}
	}

	/**
	 * Called when the user presses the device's back button.
	 * @return true if handled, false if not
	 */
	public boolean onBackPressed() {
       return false;
	}

	/**
	 * Only used for fragments that use {@link com.pocket.util.BackPressedUtil}, which is,
	 * only used in {@link com.pocket.app.MainActivity} at the moment
	 * @return true if this fragment needs to intercept onBackPressed from a child fragment
	 */
	public boolean onInterceptBackPressed() {
		return false;
	}

	/**
	 * Convenience method for finding a view within this fragment.
	 * @param res
	 * @return
	 */
	public <T extends View> T findViewById(int res) {
		return getViewRoot().findViewById(res);
	}
	
	/**
	 * Returns the actual view returned by {@link #onCreateViewImpl(LayoutInflater, ViewGroup, Bundle)}, or in the case of being displayed as a dialog, possibly the wrapper of that view.
	 * @return
	 */
	public View getViewRoot() {
		if (mDialogRootView != null) {
			return mDialogRootView;
		} else {
			return mRootView; // Seems like the compatibility library wraps the view returned by onCreateView and returns that parent in super.getView() instead of our actual view. So we just maintain a reference ourselves.
		}
	}
	
	/**
	 * When fragments detach, they do not have access to resources and this can crash.  So instead of 
	 * using getString(), you can use this method which will get the string from the App context instead.
	 * @param res
	 * @return
	 */
	public String getStringSafely(int res){
		return App.getStringResource(res);
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		if (getShowsDialog()) {
			finish();
		}
		super.onCancel(dialog);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mOnDestoryListeners != null) {
			for (OnFragmentDestoryListener listener : mOnDestoryListeners) {
				listener.onFragmentDestory(this);
			}
		}
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mRootView = null;
		mDialogRootView = null;
	}
	
	public void addOnFragmentDestoryListener(OnFragmentDestoryListener listener) {
		if (mOnDestoryListeners == null) {
			mOnDestoryListeners = new ArrayList<>();
		}
		mOnDestoryListeners.add(listener);
	}

	public interface OnFragmentDestoryListener {
		void onFragmentDestory(Fragment fragment);
	}
	
	
	
	/**
	 * Show a Dialog with the extra title/top padding collapsed.
	 * 
	 * @param customView The custom view that you added to the dialog
	 */
	public static void setupDialogWithNoTopSpace(final View customView) {
		// Now we setup a listener to detect as soon as the dialog has shown.
		customView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		    
			@SuppressWarnings("deprecation")
			@Override
		    public void onGlobalLayout() {
				// Check if your view has been laid out yet
		    	if (customView.getHeight() > 0) {
		    		// If it has been, we will search the view hierarchy for the view that is responsible for the extra space. 
		    		LinearLayout dialogLayout = findDialogLinearLayout(customView);
		    		if (dialogLayout == null) {
		    			// Could find it. Unexpected.
		    			// OPT report
		    			
		    		} else {
		    			// Found it, now remove the height of the title area
		    			View child = dialogLayout.getChildAt(0);
		    			if (child != customView) {
		    				// remove height
		    				LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
		    				lp.height = 0;
		    				child.setLayoutParams(lp);
		    				
		    			} else {
		    				// Could find it. Unexpected.
		    				// OPT report
		    			}
		    		}
		    		
		    		// Done with the listener
		    		customView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
		    	}
		     }
			
		});
	}
	
	/**
	 * Searches parents for a LinearLayout
	 * 
	 * @param view to search the search from
	 * @return the first parent view that is a LinearLayout or null if none was found
	 */
	public static LinearLayout findDialogLinearLayout(View view) {
		ViewParent parent = view.getParent();
		if (parent != null) {
			if (parent instanceof LinearLayout) {
				// Found it
				return (LinearLayout) parent;
				
			} else if (parent instanceof View) {
				// Keep looking
				return findDialogLinearLayout((View) parent);
				
			}
		}
		
		// Couldn't find it
		return null;
	}
}
