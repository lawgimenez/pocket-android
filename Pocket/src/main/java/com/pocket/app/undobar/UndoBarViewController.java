package com.pocket.app.undobar;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.util.android.FormFactor;
import com.pocket.util.java.Logs;
import com.pocket.util.android.animation.Interpolators;

import java.util.HashMap;
import java.util.Map;

/**
 * UndoBar class is used to display, manage and hide of the UndoBar.
 * TODO consider changing to a snack bar
 */
public class UndoBarViewController implements View.OnClickListener, View.OnTouchListener {

    private static final int DISPLAY_DURATION = 5000;
    private static final int CONFIRMATION_DISPLAY_DURATION = 2000;
    private static final int ANIMATION_DURATION = 333;

    private final String mMessage;
    private final String mUndoneMessage;
    private final Map<Activity, ActivityInstance> mInstances = new HashMap<>();
    private final Handler mHideHandler = new Handler();
    private final Runnable mDetachRunnable = this::finish;
    private final Runnable mFadeoutRunnable = () -> {
        for (ActivityInstance instance : mInstances.values()) {
            instance.view.animate()
                    .alpha(0)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(Interpolators.ACCEL);
        }
    };

    private boolean mHasClicked;
    private boolean mIsFinished;
    private long mStartTime;
    private OnFinished mOnFinished;
    private Undo mUndo;

    protected UndoBarViewController(String message, String undoneMessage) {
        mMessage = message;
        mUndoneMessage = undoneMessage;
        setListeners(null, null);
    }

    public void startTimeout() {
        if (mIsFinished) {
            return;
        }
        mStartTime = System.currentTimeMillis();
        extendHide(DISPLAY_DURATION);
    }
    
    public void setListeners(OnFinished onFinished, Undo undo) {
        mOnFinished = onFinished != null ? onFinished : bar -> {};
        mUndo = undo != null ? undo : bar -> {};
    }

    private View inflate(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.undobar_layout, null);
        Button button = (Button) view.findViewById(R.id.undobar_button);
        button.setOnClickListener(this);
        button.setOnTouchListener(this);

        TextView messageView = (TextView) view.findViewById(R.id.undobar_message);
        messageView.setText(mMessage);

        TextView undoneMessageView = (TextView) view.findViewById(R.id.confimation_textView);
        undoneMessageView.setText(mUndoneMessage);

		if (FormFactor.isKindleFire(true)) {
			button.setTextSize(FormFactor.dpToPx(14));
			messageView.setTextSize(FormFactor.dpToPx(14));
			undoneMessageView.setTextSize(FormFactor.dpToPx(14));
			view.findViewById(R.id.container_layout).setPadding(0, 0, 0, FormFactor.dpToPx(40));
		}

        return view;
    }

    @Override
    public void onClick(View view) {
        if (mHasClicked || mIsFinished) {
            return;
        }
        mHasClicked = true;
        mUndo.onUndoClicked(this);

        // Change views to confirmation
        for (ActivityInstance instance : mInstances.values()) {
            instance.showConfirmation();
        }

        // Wait for confirmation to end before hiding
        extendHide(CONFIRMATION_DISPLAY_DURATION);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
		if (mHasClicked || mIsFinished) {
            return false; // If we are in the undone stage, we don't need this anymore
        }

		// If they touch it, extend the show duration
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                extendHide(DISPLAY_DURATION);
                break;
        }
		return false;
	}

    private void extendHide(long duration) {
        if (mIsFinished) {
            return;
        }
        mHideHandler.removeCallbacks(mDetachRunnable);
        mHideHandler.removeCallbacks(mFadeoutRunnable);
        mHideHandler.postDelayed(mDetachRunnable, duration);
        mHideHandler.postDelayed(mFadeoutRunnable, duration - ANIMATION_DURATION);
    }

    public void attach(Activity activity) {
        if (mIsFinished) {
            return;
        }
        final ActivityInstance instance = new ActivityInstance(activity, inflate(activity));
        if (activity.getWindow().isActive() && !activity.isFinishing()) {
            instance.attach();
        } else {
            // Try in next run loop, otherwise give up
            mHideHandler.post(() -> {
                if (activity.getWindow().isActive() && !activity.isFinishing()) {
                    instance.attach();
                } else {
                    // give up
                }
            });
        }
        mInstances.put(activity, instance);

        // If still within fade-in time, animate, otherwise show at full alpha immediately
        long sinceStart = System.currentTimeMillis() - mStartTime;
        if (sinceStart >= ANIMATION_DURATION) {
            instance.view.setAlpha(1);
        } else {
            float percent = sinceStart / (float) ANIMATION_DURATION;
            instance.view.setAlpha(percent);
            instance.view.animate()
                    .alpha(1)
                    .setDuration(ANIMATION_DURATION - (long) (ANIMATION_DURATION * percent))
                    .setInterpolator(Interpolators.DECEL);
        }
    }

    public void detach(Activity activity) {
        if (mIsFinished) {
            return;
        }
        ActivityInstance instance = mInstances.get(activity);
        if (instance != null) {
            instance.detach();
        }
    }

    public void finish() {
        if (mIsFinished) {
            return;
        }
        mIsFinished = true;

        mHideHandler.removeCallbacks(mDetachRunnable);
        mHideHandler.removeCallbacks(mFadeoutRunnable);

        for (ActivityInstance instance : mInstances.values()) {
            instance.detach();
        }

        mOnFinished.onUndoBarFinished(this);
    }

    /**
     * <p>
     * Interface handling the callbacks from undobar.
     * </p>
     */
    protected interface OnFinished {
    	/**
    	 * Called when the UndoBar is hidden, regardless of timeout or action.
    	 */
        void onUndoBarFinished(UndoBarViewController bar);

    }
    
    protected interface Undo {
        /**
         * Called when undo is clicked, perform the undo actions.
         */
        void onUndoClicked(UndoBarViewController bar);
        
    }

    private static class ActivityInstance {

        private final Activity activity;
        private final View view;
        private final WindowManager window;

        private boolean isAttached;

        private ActivityInstance(Activity activity, View view) {
            this.activity = activity;
            this.window = activity.getWindowManager();
            this.view = view;
        }

        private void attach() {
            if (isAttached) {
                return;
            }
            isAttached = true;

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );
            lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            lp.y = FormFactor.dpToPx(80);
            lp.windowAnimations = 0; // We will animate

            try {
                window.addView(view, lp);
            } catch (Throwable ignore) {
                isAttached = false; // I could have moved isAttached = true to after window.addView but in the interest of avoiding regressions caused by changing the order, this felt safest for now.
                // We've seen this throw the following error: "Unable to add window -- token null is not valid; is your activity running?"
                // which is a little confusing given the checks we've done ahead of this method to make sure it isn't finished, but perhaps
                // we also need to check if it is resumed. Looking at the crash logs, https://rink.hockeyapp.net/manage/apps/2885/app_versions/360/crash_reasons/242795185TODO it would be nice to fully  understand the problem and fix this higher up,
                // it seems this is caused when triggering an archive or delete and then quickly leaving the activity.
                // So ignoring the crash seems like the safest thing for now. TODO it would be nice to fully understand why this isn't working the way
                // we think it should and fix it higher up, but if it fails, it will try to reattach when the next activity is resumed.
            }
        }

        private void detach() {
            if (!isAttached) {
                return;
            }
            isAttached = false;

            try {
                window.removeView(view);
            } catch (Throwable ignore) {
                view.setVisibility(View.GONE);
                Logs.printStackTrace(ignore);
            }
        }

        public void showConfirmation() {
            view.findViewById(R.id.regular_layout).setVisibility(View.GONE);
            view.findViewById(R.id.confimation_textView).setVisibility(View.VISIBLE);
        }
    }

}
