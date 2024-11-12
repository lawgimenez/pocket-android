package com.pocket.util.android.view;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.pocket.util.java.Logs;
import com.pocket.util.android.FormFactor;

public class HorizontalGestureRecognizer {
	
	private static final boolean DEBUG = false;
	
	private boolean mIsMovingHorizontal = false;
	private final int mTouchSlop;
	private boolean mAbsorbTouches = false;
	private float mStartX;
	private float mStartY;
	private boolean mDetectSwipe;
	private boolean mDetectDirection;
	private boolean mIsHorizontalMode = false;
	private SwipeListener mListener;
	private boolean mHasMoved;
	private final int mSwipeLengthThresholdInHorizontal;
	private final int mSwipeLengthThresholdInVertical;
	
	public HorizontalGestureRecognizer(Context viewContext) {
		mTouchSlop = ViewConfiguration.get(viewContext).getScaledTouchSlop();
		mSwipeLengthThresholdInHorizontal = FormFactor.dpToPx(15); 
		mSwipeLengthThresholdInVertical = FormFactor.dpToPx(70); 
	}
	
	public boolean onTouchEvent(MotionEvent ev) {
		if (ev == null || mListener == null || !mListener.isPagingEnabled()) {
			return false;
		}
		
		boolean isFirstAbsorb = false;
		
		switch(ev.getAction()){
		case MotionEvent.ACTION_DOWN:
			if (DEBUG) Logs.d("Paging", "DOWN");
			mIsMovingHorizontal = false;
			mAbsorbTouches = false;
			if (mListener.onHorizontalGestureDown(ev.getX(), ev.getY())) {
				mDetectDirection = true;
				mDetectSwipe = true;
				mStartX = ev.getX();
				mStartY = ev.getY();
				mHasMoved = false;
			} else {
				return false; // Ignore swipes starting here.
			}
			break;
			
		case MotionEvent.ACTION_MOVE:
			if (!mHasMoved) {
				if (DEBUG) Logs.l("Swipe " + "MOVE");
			}
			mHasMoved  = true;
			// No break
			
		case MotionEvent.ACTION_UP:
			if (DEBUG) {
				if (ev.getAction() == MotionEvent.ACTION_UP) {
					Logs.d("Paging", "UP");
				}
			}
			float distanceX = ev.getX() - mStartX;
			if (Math.abs(distanceX) > mTouchSlop) {
				if (mDetectSwipe && mIsMovingHorizontal) {
					float swipeLength = mIsHorizontalMode ? mSwipeLengthThresholdInHorizontal : mSwipeLengthThresholdInVertical; 
					if (DEBUG) Logs.d("Paging", "swiping " + Math.abs(distanceX) + " " + swipeLength);
					if (Math.abs(distanceX) >= swipeLength) {
						mListener.swiped(distanceX < 0);
						mDetectSwipe = false;
					}
					
				}
				
				if (mDetectDirection) {
					// Determine Angle
					float angleInDegrees = (float) Math.abs(Math.atan2(ev.getY() - mStartY, distanceX) * 180 / Math.PI);
					
					// Determine the sensitivity. If in horizontal mode, they should do something VERY vertical in order to get out of it
					// The size of the threshold looks like this: >< basically the amount they can be move vertically before we decide it is not horizontal
					// The angle goes both ways on the y axis. So if the value is 10, then it's 10 up or 10 down
					int sensitivity = mIsHorizontalMode ? 73 : 17;
					
					if (angleInDegrees < sensitivity || angleInDegrees > 180 - sensitivity) {
						// Horizontal
						mIsMovingHorizontal = true;
						isFirstAbsorb = true;
						mAbsorbTouches = true;
						
					} else {
						// Vertical
						
					}
					
					if (DEBUG) Logs.d("Paging", "direction " + angleInDegrees + " " + sensitivity + " " + mAbsorbTouches);
					
					mDetectDirection = false;
				}
			}
			
			break;
			
		
			
			/*
			if(mHasMoved){
				float distance = Math.abs(ev.getX() - mLastX);
				float offPath = Math.abs(ev.getY() - mLastY);
				float velocity = distance / ((ev.getEventTime() - mLastTime) / 1000); // pixels per second
				if(distance > SWIPE_MIN_DISTANCE && velocity > SWIPE_MIN_VELOCITY && offPath < SWIPE_MAX_OFF_PATH){
					//Check if they are swiping at the edge of a zoomed in page, meaning their scroll didn't change
					boolean scrolled = false;
					if(Math.abs(view.getScrollX() - mLastScrollX) > SCROLL_THRESHOLD){
						scrolled = true;
					}
					
					if (mLastX > ev.getX()) {
						return new SwipeInfo(SwipeInfo.LEFT, scrolled);
					} else {
						return new SwipeInfo(SwipeInfo.RIGHT, scrolled);
					}
				}
			}
			*/
			
		}
		
		if (isFirstAbsorb) {
			// Let this one pass through but set it as a cancel
			ev.setAction(MotionEvent.ACTION_CANCEL);
			if (DEBUG) Logs.d("Paging", "touches absorbed");
			return false;
		}
		return mAbsorbTouches;
	}
	
	
	
	/**
	 * When set in horizontal mode, swipes are much easier to perform (more sensitive).
	 * 
	 * @param isHorizontalMode
	 */
	public void setHorizontalMode(boolean isHorizontalMode) {
		mIsHorizontalMode = isHorizontalMode;
	}
	
	public interface SwipeListener {
		/**
		 * A swipe has been performed.
		 * @param left true if touches moved from right to left, false if left to right.
		 */
		public void swiped(boolean left);
		/** 
		 * @return true if paging is allowed at this time.
		 */
		public boolean isPagingEnabled();
		/**
		 * @param x The touches down x
		 * @param y The touches down y
		 * @return true if swipes are allowed starting at this location, false if not.
		 */
		public boolean onHorizontalGestureDown(float x, float y);
	}

	public void setListener(SwipeListener listener) {
		mListener = listener;
	}

	public boolean isPaging() {
		return mIsHorizontalMode;
	}
	
}
