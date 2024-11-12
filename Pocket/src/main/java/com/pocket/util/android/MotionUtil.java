package com.pocket.util.android;

import android.view.MotionEvent;

/**
 * TODO Documentation
 */
public class MotionUtil {
	
	public static String motionEventToShortString(MotionEvent ev, boolean round) {
		if (ev == null) {
			return "{null}";
		}
		
		String s = "{";
		if (round) {
			s += ((int) ev.getX()) + "," + ((int) ev.getY());
		} else {
			s += ev.getX() + "," + ev.getY();
		}
		return s + ", " + motionEventActionToString(ev.getAction()) + "}";
	}
	
	/**
	 * Copied from hidden method MotionEvent#actionToString(int)
	 *
	 * @param action
	 * @return
	 */
	public static String motionEventActionToString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return "ACTION_DOWN";
            case MotionEvent.ACTION_UP:
                return "ACTION_UP";
            case MotionEvent.ACTION_CANCEL:
                return "ACTION_CANCEL";
            case MotionEvent.ACTION_OUTSIDE:
                return "ACTION_OUTSIDE";
            case MotionEvent.ACTION_MOVE:
                return "ACTION_MOVE";
            case MotionEvent.ACTION_HOVER_MOVE:
                return "ACTION_HOVER_MOVE";
            case MotionEvent.ACTION_SCROLL:
                return "ACTION_SCROLL";
            case MotionEvent.ACTION_HOVER_ENTER:
                return "ACTION_HOVER_ENTER";
            case MotionEvent.ACTION_HOVER_EXIT:
                return "ACTION_HOVER_EXIT";
        }
        int index = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                return "ACTION_POINTER_DOWN(" + index + ")";
            case MotionEvent.ACTION_POINTER_UP:
                return "ACTION_POINTER_UP(" + index + ")";
            default:
                return Integer.toString(action);
        }
    }
}
