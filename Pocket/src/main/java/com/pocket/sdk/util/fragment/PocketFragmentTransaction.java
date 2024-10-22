package com.pocket.sdk.util.fragment;

import android.view.View;

import java.util.ArrayList;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class PocketFragmentTransaction extends FragmentTransaction {

	private final FragmentTransaction mTransaction;
	private final PocketFragmentManager mPocketFragmentManager;
	
	private final ArrayList<Fragment> mAdded = new ArrayList<>();
	private final ArrayList<Fragment> mRemoved = new ArrayList<>();
	
	private boolean mIsAddedToBackStack;
	
	public PocketFragmentTransaction(FragmentTransaction transaction, PocketFragmentManager pocketFragmentManager) {
		mTransaction = transaction;
		mPocketFragmentManager = pocketFragmentManager;
	}

	@Override
	public FragmentTransaction add(Fragment fragment, String tag) {
		mTransaction.add(fragment, tag);
		mAdded.add(fragment);
		
		return this;
	}

	@Override
	public FragmentTransaction add(int containerViewId, Fragment fragment) {
		return add(containerViewId, fragment, null);
	}

	@Override
	public FragmentTransaction add(int containerViewId, Fragment fragment, String tag) {
		mTransaction.add(containerViewId, fragment, tag);
		mAdded.add(fragment);
		
		return this;
	}
	
	@Override
	public FragmentTransaction replace(int containerViewId, Fragment fragment) {
		return replace(containerViewId, fragment, null);
	}

	@Override
	public FragmentTransaction replace(int containerViewId, Fragment fragment, String tag) {
		Fragment currentFragment = mPocketFragmentManager.findFragmentById(containerViewId);
		
		mTransaction.replace(containerViewId, fragment, tag);

		if (currentFragment != null) {
			mRemoved.add(currentFragment);
		}
		mAdded.add(fragment);
		return this;
	}
	
	@Override
	public FragmentTransaction show(Fragment fragment) {
		mTransaction.show(fragment);

		mAdded.add(fragment);
		return this;
	}

	@Override
	public FragmentTransaction addToBackStack(String name) {
		mTransaction.addToBackStack(name);
		
		mIsAddedToBackStack = true;
		return this;
	}

	@Override
	public FragmentTransaction attach(Fragment fragment) {
		mTransaction.attach(fragment);
		
		mAdded.add(fragment);
		return this;
	}
	
	@Override
	public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment) {
		return mTransaction.setPrimaryNavigationFragment(fragment);
	}
	
	@Override
	public FragmentTransaction detach(Fragment fragment) {
		mTransaction.detach(fragment);
		
		mRemoved.add(fragment);
		return this;
	}
	
	@Override
	public FragmentTransaction remove(Fragment fragment) {
		mTransaction.remove(fragment);
	
		mRemoved.add(fragment);
		return this;
	}

	@Override
	public int commit() {
		mPocketFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack);
		return mTransaction.commit();
	}

	@Override
	public int commitAllowingStateLoss() {
		mPocketFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack);
		return mTransaction.commitAllowingStateLoss();
	}

	@Override
	public void commitNowAllowingStateLoss() {
		mPocketFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack);
		mTransaction.commitNowAllowingStateLoss();
	}

	@Override
	public void commitNow() {
		mPocketFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack);
		mTransaction.commitNow();
	}

	@Override
	public FragmentTransaction disallowAddToBackStack() {
		mTransaction.disallowAddToBackStack();
	
		return this;
	}

	@Override
	public FragmentTransaction hide(Fragment fragment) {
		mTransaction.hide(fragment);
		
		mRemoved.add(fragment);
		return this;
	}

	@Override
	public boolean isAddToBackStackAllowed() {
		return mTransaction.isAddToBackStackAllowed();
	}

	@Override
	public boolean isEmpty() {
		return mTransaction.isEmpty();
	}

	@Override
	public FragmentTransaction setBreadCrumbShortTitle(int res) {
		mTransaction.setBreadCrumbShortTitle(res);
		
		return this;
	}

	@Override
	public FragmentTransaction setBreadCrumbShortTitle(CharSequence text) {
		mTransaction.setBreadCrumbShortTitle(text);

		return this;
	}
	
	@Override
	public FragmentTransaction setReorderingAllowed(boolean b) {
		return mTransaction.setReorderingAllowed(b);
	}
	
	@Override public FragmentTransaction setAllowOptimization(boolean allowOptimization) {
		mTransaction.setAllowOptimization(allowOptimization);
		
		return this;
	}
	
	@Override
	public FragmentTransaction setBreadCrumbTitle(int res) {
		mTransaction.setBreadCrumbTitle(res);

		return this;
	}

	@Override
	public FragmentTransaction setBreadCrumbTitle(CharSequence text) {
		mTransaction.setBreadCrumbTitle(text);

		return this;
	}

	@Override
	public FragmentTransaction setCustomAnimations(int enter, int exit) {
		mTransaction.setCustomAnimations(enter, exit);

		return this;
	}

	@Override
	public FragmentTransaction setCustomAnimations(int enter, int exit, int popEnter, int popExit) {
		mTransaction.setCustomAnimations(enter, exit, popEnter, popExit);
	
		return this;
	}

    @Override
    public FragmentTransaction addSharedElement(View view, String s) {
        mTransaction.addSharedElement(view, s);
        return this;
    }

    @Override
	public FragmentTransaction setTransition(int transit) {
		return mTransaction.setTransition(transit);
	}

	@Override
	public FragmentTransaction setTransitionStyle(int styleRes) {
		mTransaction.setTransitionStyle(styleRes);

		return this;
	}
	
	@Override
	public FragmentTransaction runOnCommit(Runnable runnable) {
		mTransaction.runOnCommit(runnable);
		return this;
	}
}
