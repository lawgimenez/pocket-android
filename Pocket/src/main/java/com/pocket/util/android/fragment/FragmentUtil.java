package com.pocket.util.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.util.android.FormFactor;

public abstract class FragmentUtil {

	public enum FragmentLaunchMode {
		/**
		 * Show as DialogFragment within current activity. Can use {@link FragmentUtil#addFragmentAsDialog(DialogFragment, FragmentActivity, String)}.
		 */
		DIALOG,
		/**
		 * Start a new Activity with this fragment as the content view. This typically means there is a Activity of the same name as the fragment available.
		 */
		ACTIVITY,
		/**
		 * This is for special cases, where no matter what, it should be launched as a new activity. Once in the activity, if {@link FormFactor#showSecondaryScreensInDialogs(Context)} then have this fragment appear as a DialogFragment with a rainbow background in the activity.
		 * If false then have it display as a normal content fragment.
		 * <p>
		 * <b>Two important suggestions for this mode:</b>
		 * <ol>
		 * <li>Use {@link AbsPocketActivity#setContentFragment(Fragment, String, FragmentLaunchMode)} to easily handle the standard UI/UX for these types of fragments.</li>
		 * <li>In your fragment's {@link Fragment#onActivityCreated(android.os.Bundle)}, invoke {@link FragmentUtil#cancelOutsideTouch(DialogFragment)} to prevent the activity from finishing if they touch outside of the fragment</li>
		 * </ol>
		 */
		ACTIVITY_DIALOG
	}
	
	/**
	 * if the fragment is not attached to an activity, or if the activity is finishing, or if this
	 * fragment is finishing itself.
	 * @return
	 */
	public static boolean isDetachedOrFinishing(Fragment frag) {
		if (frag == null) {
			return false;
		}
		Activity activity = frag.getActivity();
		return activity == null || activity.isFinishing() || frag.isDetached() || frag.isRemoving();
	}
	
	/**
	 * If this fragment is attached but the activity it is attached to is finishing.
	 * <b>IMPORTANT</b> if the fragment is not attached, this will return false because it doesn't know for sure. Do not use this
	 * as a null check for a fragment's activity. See {@link #isDetachedOrFinishing(Fragment)}.
	 * 
	 * @return
	 */
	public static boolean isFinishing(Fragment frag) {
		return frag.getActivity() != null && frag.getActivity().isFinishing();
	}
	
	/**
	 * Adds a fragment to an Activity. Immediately executes pending transactions afterwards.
	 * 
	 * @param frag
	 * @param activity
	 * @param contentView
	 * @param tag
	 * @param addToBackStack
	 */
	public static void addFragment(Fragment frag, FragmentActivity activity, int contentView, String tag, boolean addToBackStack) {
		addFragment(frag, activity, contentView, tag, addToBackStack, true);
	}
	
	public static void addFragment(Fragment frag, FragmentActivity activity, int contentView, String tag, boolean addToBackStack, boolean executeImmediately) {
		FragmentManager manager = activity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		
		transaction.add(contentView, frag, tag);
		if (addToBackStack) {
			transaction.addToBackStack(null); // We could provide a param for name here if anyone wants to use it.
		}
		transaction.commit();
		
		if (executeImmediately) {
			activity.getSupportFragmentManager().executePendingTransactions();
		}
	}
	
	/**
	 * Same as {@link #addFragmentAsDialog(DialogFragment, FragmentActivity, String)} with a null tag.
	 * @param frag
	 * @param activity
	 */
	public static void addFragmentAsDialog(DialogFragment frag, FragmentActivity activity) {
		addFragmentAsDialog(frag, activity, null);
	}
	
	public static void addFragmentAsDialog(DialogFragment frag, FragmentActivity activity, String tag) {
		addFragmentAsDialog(frag, activity, tag, true, true);
	}
	
	public static void addFragmentAsDialog(DialogFragment frag, FragmentActivity activity, String tag, boolean addToBackStack, boolean executeImmediately) {
		FragmentManager manager = activity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		
		if (addToBackStack) {
			transaction.addToBackStack(null); // We could provide a param for name here if anyone wants to use it.
		}
		frag.show(transaction, tag);
		
		if (executeImmediately) {
			activity.getSupportFragmentManager().executePendingTransactions();
		}
	}
	
	public static void removeFragment(Fragment frag, FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		FragmentTransaction transaction = manager.beginTransaction();
		transaction.remove(frag);
		transaction.commit();
	}
	
	public static View getRootView(Fragment fragment) {
		if (fragment instanceof AbsPocketFragment) {
			return ((AbsPocketFragment) fragment).getViewRoot();
		} else {
			return fragment.getView();
		}
	}
	
}
