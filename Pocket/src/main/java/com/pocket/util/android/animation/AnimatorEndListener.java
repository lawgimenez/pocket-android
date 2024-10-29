package com.pocket.util.android.animation;

import android.animation.Animator;

/**
 * An {@link android.animation.Animator.AnimatorListener} whos only abstract method is {@link #onAnimationEnd(Animator)} to keep
 * code cleaner when you only need that method. The other methods are no-ops.
 * 
 * @author max
 *
 */
public abstract class AnimatorEndListener implements Animator.AnimatorListener {
	
	private boolean wasCanceled = false;
	
	@Override
	public void onAnimationCancel(Animator animator) {
		wasCanceled = true;
	}

	@Override
	public void onAnimationRepeat(Animator animator) {
		
	}
	
	@Override
	public void onAnimationStart(Animator animator) {
		wasCanceled = false;
	}
	
	public boolean wasCanceled() {
		return wasCanceled;
	}
	
}
