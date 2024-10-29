package com.pocket.sdk.util.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.Fragment.SavedState;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener;
import androidx.fragment.app.FragmentTransaction;

import com.pocket.app.App;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sdk.util.AbsPocketFragment;
import com.pocket.util.android.fragment.FragmentUtil;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PocketFragmentManager extends FragmentManager implements OnBackStackChangedListener {
	
	private static final String STATE = "PocketFragmentManagerState";
	private static final String STATE_BACK_STACK_ENTRY_COUNT = "backStackEntryCount";
	private static final String STATE_BACK_STACK_ENTRY_ADDED_INDEXES = "backStackEntryAdds";
	private static final String STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES = "backStackEntryVisibles";
	
	/** The wrapped manager */
	private final FragmentManager mFragmentManager;
	private final AbsPocketActivity mActivity;
	
	/**
	 * All of the back stack entries and their fragments.
	 */
	private final ArrayList<BackStackEntryFragments> mBackStackEntryFragments = new ArrayList<>();
	
	/**
	 * The back stack entry count after the last back stack entry change.
	 */
	private int mLastKnownBackStackCount = 0;
	
	public PocketFragmentManager(FragmentManager fragmentManager, AbsPocketActivity activity) {
		mActivity = activity;
		mFragmentManager = fragmentManager;
		mBackStackEntryFragments.add(new BackStackEntryFragments());
		fragmentManager.addOnBackStackChangedListener(this);
	}
	
	public void onSaveInstanceState(Bundle outState) {
		Bundle state = new Bundle();
		
		// back stack entries
		int size = mBackStackEntryFragments.size();
		state.putInt(STATE_BACK_STACK_ENTRY_COUNT, mBackStackEntryFragments.size());
		List<Fragment> source = mFragmentManager.getFragments();
		for (int i = 0; i < size; i++) {
			BackStackEntryFragments entry = mBackStackEntryFragments.get(i);
			state.putIntArray(STATE_BACK_STACK_ENTRY_ADDED_INDEXES + i, getListIndex(source, entry.added));
			state.putIntArray(STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES + i, getListIndex(source, entry.visible));
		}
		
		outState.putBundle(STATE, state);
	}
	
	public void onRestoreInstanceState(Bundle inState) {
		Bundle state = inState.getBundle(STATE);
		int size = state.getInt(STATE_BACK_STACK_ENTRY_COUNT);
		
		if (size <= 0) {
			return; // Nothing to restore;
		}
		
		// All active fragments already restored by the default fragment manager
		List<Fragment> source = mFragmentManager.getFragments();
		
		for (int i = 0; i < size; i++) {
			if (i > 0) { // There is already one at 0 index
				mBackStackEntryFragments.add(new BackStackEntryFragments());
			}
			
			BackStackEntryFragments entry = mBackStackEntryFragments.get(i);
			
			addAllFromIndex(source, entry.added, state.getIntArray(STATE_BACK_STACK_ENTRY_ADDED_INDEXES + i));
			addAllFromIndex(source, entry.visible, state.getIntArray(STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES + i));
		}
		
		mLastKnownBackStackCount = size;
	}
	
	/**
	 * Maps each object of matchList to its index within sourceList. 
	 * Assumes all of the fragments of matchList can be found in sourceList.
	 * <p>
	 * For example, if sourceList is ["cat", "dog", "rabbit"] and matchList is ["rabbit", "cat"], this will return [2,0]
	 * <p>
	 * You can then rebuild the contents of matchList later by extracting them out of sourceList with {@link #addAllFromIndex(List, List, int[])} 
	 */
	private static int[] getListIndex(List<Fragment> sourceList, List<Fragment> matchList) {
		int[] indexes = new int[matchList.size()];
		int len = indexes.length;
		for (int i = 0; i < len; i++) {
			indexes[i] = sourceList.indexOf(matchList.get(i));
		}
		return indexes;
	}
	
	/**
	 * Used in conjunction with {@link #getListIndex(List, List)} to restore references to fragments when restoring state. 
	 */
	private static void addAllFromIndex(List<Fragment> sourceList, List<Fragment> destList, int[] indexes) {
		for (int i : indexes) {
			if (i < 0) {
				// In certain cases where an activity is restored, such as editing an avatar with "Don't Keep Activities" on,
				// this can happen. It isn't very clear why. However, we've been doing the above logging for years,
				// and failing silently on Production. No user has reported issues here so problem not worth the effort to
				// figure this out. We'll just skip restoring the reference here.
				continue;
			}
			destList.add(sourceList.get(i));
		}
	}
	
	@Override
	public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
		mFragmentManager.addOnBackStackChangedListener(listener);
	}

	@SuppressLint("CommitTransaction")
	@Override
	public FragmentTransaction beginTransaction() {
		return new PocketFragmentTransaction(mFragmentManager.beginTransaction(), this);
	}

	@Override
	public void dump(String prefix, FileDescriptor fd, PrintWriter writer,
			String[] args) {
		mFragmentManager.dump(prefix, fd, writer, args);
	}

	@Override
	public boolean executePendingTransactions() {
		return mFragmentManager.executePendingTransactions();
	}

	@Override
	public Fragment findFragmentById(int id) {
		return mFragmentManager.findFragmentById(id);
	}

	@Override
	public Fragment findFragmentByTag(String tag) {
		return mFragmentManager.findFragmentByTag(tag);
	}

	@Override
	public BackStackEntry getBackStackEntryAt(int index) {
		return mFragmentManager.getBackStackEntryAt(index);
	}

	@Override
	public int getBackStackEntryCount() {
		return mFragmentManager.getBackStackEntryCount();
	}

	@Override
	public Fragment getFragment(Bundle bundle, String key) {
		return mFragmentManager.getFragment(bundle, key);
	}

	@Override
	public List<Fragment> getFragments() {
		List<Fragment> fragments = mFragmentManager.getFragments();
		return fragments != null ? fragments : Collections.emptyList();
	}

	@Override
	public void popBackStack() {
		mFragmentManager.popBackStack();
	}

	@Override
	public void popBackStack(String name, int flags) {
		mFragmentManager.popBackStack(name, flags);
	}

	@Override
	public void popBackStack(int id, int flags) {
		mFragmentManager.popBackStack(id, flags);
	}

	@Override
	public boolean popBackStackImmediate() {
		return mFragmentManager.popBackStackImmediate();
	}

	@Override
	public boolean popBackStackImmediate(String name, int flags) {
		return mFragmentManager.popBackStackImmediate(name, flags);
	}

	@Override
	public boolean popBackStackImmediate(int id, int flags) {
		return mFragmentManager.popBackStackImmediate(id, flags);
	}

	@Override
	public void putFragment(Bundle bundle, String key, Fragment fragment) {
		mFragmentManager.putFragment(bundle, key, fragment);
	}

	@Override
	public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
		mFragmentManager.removeOnBackStackChangedListener(listener);
	}

	@Override
	public SavedState saveFragmentInstanceState(Fragment fragment) {
		return mFragmentManager.saveFragmentInstanceState(fragment);
	}

    @Override
    public boolean isDestroyed() {
        return mFragmentManager.isDestroyed();
    }
	
	@Override
	public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
		mFragmentManager.registerFragmentLifecycleCallbacks(cb, recursive);
	}
	
	@Override public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
		mFragmentManager.unregisterFragmentLifecycleCallbacks(cb);
	}
	
	@Override
	public boolean isStateSaved() {
		return mFragmentManager.isStateSaved();
	}
	
	@Override
	public Fragment getPrimaryNavigationFragment() {
		return mFragmentManager.getPrimaryNavigationFragment();
	}
	
	/**
	 * Returns the currently visible/active fragments. This may contain fragments added during multiple back stack entries.
	 * <p>
	 * Note: Do not modify this list.
	 */
	public ArrayList<Fragment> getVisibleFragments() {
		return getCurrentBackStackEntry().visible;
	}
	
	void onCommit(ArrayList<Fragment> added, ArrayList<Fragment> removed, boolean addedToBackStack) {
		List<Fragment> visible = new ArrayList<>(getVisibleFragments()); // Copy so we can modify
		visible.removeAll(removed);
		visible.addAll(added);
		
		// Keep track of back stack state
		if (addedToBackStack) {
			// Dispatch loss of focus to current fragments
			BackStackEntryFragments currentFocus = getCurrentBackStackEntry();
			
			if (currentFocus.visible.size() == 0) {
				mActivity.onLostFocus();
			}
			
			for (Fragment fragment : currentFocus.added) {
				if (fragment instanceof AbsPocketFragment) {
					((AbsPocketFragment) fragment).onLostFocus();
				}
			}
			
			// Create a new back stack entry
			BackStackEntryFragments entry = new BackStackEntryFragments();
			entry.added.addAll(added);
			entry.visible.clear();
			entry.visible.addAll(visible);
			mBackStackEntryFragments.add(entry);
			
		} else {
			// Add fragments to current back stack entry
			BackStackEntryFragments entry = getCurrentBackStackEntry();
			entry.added.addAll(added);
			entry.added.removeAll(removed);
			
			entry.visible.clear();
			entry.visible.addAll(visible);
		}
	}
	
	@Override
	public void onBackStackChanged() {
		int newCount = mFragmentManager.getBackStackEntryCount();
		
		if (newCount < mLastKnownBackStackCount) {
			// Need to update our references to remove these entries
			int entriesToRemove = mLastKnownBackStackCount - newCount;
			for (int i = 0; i < entriesToRemove; i++) {
				// Pop off the last entry
				mBackStackEntryFragments.remove(mBackStackEntryFragments.size()-1);
			}
			
			if (mBackStackEntryFragments.isEmpty()) {
				// Ensure at least one entry
				mBackStackEntryFragments.add(new BackStackEntryFragments());
				
				// We are seeing this case in production crash logs. It is probably fine and just
				// an Activity finishing, but just to make sure we understand why and where this is 
				// happening, report the error with the name of the activity so we can get more information.
				// If it turns out it is fine to do this way, then we can remove this reporting code.
				try {
					String activity = App.getActivityContext() != null ? App.getActivityContext().toString() : "";
					throw new RuntimeException("empty back stack at " + activity);
				} catch (RuntimeException e) {
					if (App.getApp().mode().isDevBuild()) { // REVIEW is there a problem here?
						throw e;
					} else {
						App.getApp().errorReporter().reportError(e);
					}
				}
			}
			
			BackStackEntryFragments currentFocus = getCurrentBackStackEntry();
			
			if (currentFocus.visible.size() == 0) {
				mActivity.onRegainedFocus();
			}
			
			for (Fragment fragment : currentFocus.added) {
				if (fragment instanceof AbsPocketFragment) {
					((AbsPocketFragment)fragment).onRegainedFocus();
				}
			}
		}
		
		mLastKnownBackStackCount = newCount;
	}
	
	private BackStackEntryFragments getCurrentBackStackEntry() {
		return mBackStackEntryFragments.get(mBackStackEntryFragments.size() - 1); // Will never be empty because of the one added during the constructor and the way the last stack is removed.
	}
	
	/**
	 * Is this fragment one of the root fragments?
	 */
	private boolean isRootFragment(Fragment fragment) {
		return mBackStackEntryFragments.get(0).added.contains(fragment);
	}
	
	/**
	 * Invoked during {@link Activity#onBackPressed()}
	 * @return true if handled by a fragment, false otherwise
	 */
	public boolean onBackPressed() {
		ArrayList<Fragment> fragments = getCurrentBackStackEntry().added;
		
		int size = fragments.size();
		for (int i = size - 1; i >= 0; i--) {
			 Fragment fragment = fragments.get(i);
			 if (fragment instanceof AbsPocketFragment){
				 boolean handled = ((AbsPocketFragment) fragment).onBackPressed();
				 if (handled) {
					return true;
				}
			 }
		}
		
		return false;
	}
	
	/**
	 * Invoked during {@link Activity#onRestart()}
	 */
	public void onActivityRestart() {
		ArrayList<Fragment> fragments = getCurrentBackStackEntry().added;
		for (Fragment fragment : fragments) {
			if (fragment instanceof AbsPocketFragment) {
				((AbsPocketFragment) fragment).onRestart();
			}
		}
	}
	
	/**
	 * Invoked during {@link AbsPocketActivity#onThemeChanged(int)}
	 */
	public void onThemeChanged(int newTheme) {
		ArrayList<Fragment> fragments = getCurrentBackStackEntry().added;
		for (Fragment fragment : fragments) {
			if (fragment instanceof AbsPocketFragment) {
				((AbsPocketFragment) fragment).onThemeChanged(newTheme);
			}
		}
	}

	/**
	 * Dismiss/hide this fragment. If it is a root fragment it will finish the Activity.
	 */
	public void finishFragment(Fragment fragment, FragmentActivity parentActivity) {
		if (fragment instanceof DialogFragment && ((DialogFragment) fragment).getShowsDialog()) {
			((DialogFragment) fragment).getDialog().dismiss();
			
		} else if (isRootFragment(fragment)) {
			parentActivity.finish();
			
		} else {
			// REVIEW this won't pop the back stack... is that a problem?
			FragmentUtil.removeFragment(fragment, parentActivity);
		}
	}

	/**
	 * Removes all fragments from the Activity.
	 */
	public void removeAllFragments() {
		List<Fragment> frags = mFragmentManager.getFragments();
		
		FragmentTransaction ft = beginTransaction();
		for (Fragment frag : frags) {
			if (frag.isVisible()) {
				ft.remove(frag);
			}
		}
		ft.commit();
		
		executePendingTransactions();
	}
	
	private class BackStackEntryFragments {
		/**
		 * The fragments added during this transaction/back stack entry.
		 */
		public ArrayList<Fragment> added = new ArrayList<>();
		/**
		 * A copy of what {@link PocketFragmentManager#getVisibleFragments()} is while this entry is the current state.
		 */
		public ArrayList<Fragment> visible = new ArrayList<>();
	}
	
}
