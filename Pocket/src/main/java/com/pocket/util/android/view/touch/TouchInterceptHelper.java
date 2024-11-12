package com.pocket.util.android.view.touch;

import android.content.Context;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects when a touch event has gone beyond the touch slop and is no longer a possible tap.
 */
public class TouchInterceptHelper {

    private final int mTouchSlop;
    private final PointF mDown = new PointF();
    private final List<MotionEvent> mRecord = new ArrayList<>();

    private boolean mIsRecordingEnabled;

    public TouchInterceptHelper(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setRecordingEnabled(boolean record) {
        mIsRecordingEnabled = record;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clearRecord();
                mDown.set(ev.getX(), ev.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                float xMove = Math.abs(mDown.x - ev.getX());
                float yMove = Math.abs(mDown.y - ev.getY());
                if (xMove > mTouchSlop || yMove > mTouchSlop) {
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
        }

        if (mIsRecordingEnabled) {
            mRecord.add(MotionEvent.obtain(ev));
        }

        return false;
    }

    public List<MotionEvent> getRecord() {
        return mRecord;
    }

    private void clearRecord() {
        for (MotionEvent ev : mRecord) {
            ev.recycle();
        }
        mRecord.clear();
    }

}
