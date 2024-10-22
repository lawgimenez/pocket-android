package com.pocket.app.reader.internal.article.image;

import android.view.MotionEvent;
// OPT since this app is only for 2.0 and above this class could be merged with the super class
public class EclairMotionEvent extends WrapMotionEvent {
	  protected EclairMotionEvent(MotionEvent event) {
	            super(event);
	    }

	    public float getX(int pointerIndex) {
	            return event.getX(pointerIndex);
	    }

	    public float getY(int pointerIndex) {
	            return event.getY(pointerIndex);
	    }

	    public int getPointerCount() {
	            return event.getPointerCount();
	    }

	    public int getPointerId(int pointerIndex) {
	            return event.getPointerId(pointerIndex);
	    }
	
}
