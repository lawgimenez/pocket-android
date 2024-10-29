package com.pocket.util.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.pocket.util.java.Logs;
import com.pocket.util.android.MotionUtil;

/**
 * If this FrameLayout's top position is moved while the user is touching down or moving, this will offset the touch events
 * until a ACTION_UP or a ACTION_CANCEL occurs. This is to avoid a jump in scrolling when doing a relayout in the middle of a 
 * scroll.
 * 
 * @author max
 *
 */
public class TouchShiftFrameLayout extends FrameLayout {
	
	private boolean mIsTracking;
	private int mTopAtDown;

	public TouchShiftFrameLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public TouchShiftFrameLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public TouchShiftFrameLayout(Context context) {
		super(context);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		
		int topChange = getTop() - mTopAtDown;
		if (mIsTracking && topChange != 0) {
			if (ScrollTracker.DEBUG) Logs.i("Scroll", "OFFSET " + MotionUtil.motionEventToShortString(ev, true) + " BY " + topChange);
			ev.offsetLocation(0, topChange);
			if (ScrollTracker.DEBUG) Logs.i("Scroll", "OFFSET TO" + MotionUtil.motionEventToShortString(ev, true));
		}
		
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			mIsTracking = false;
			break;
			
		case MotionEvent.ACTION_DOWN:
			mTopAtDown = getTop();
			mIsTracking = true;
			break;
		}
		
		return super.onInterceptTouchEvent(ev);
	}

}
