package com.pocket.util.android;

import android.os.Handler;
import android.os.Looper;

/**
 * Simple helper class for a common pattern of having a runnable that is posted after a delay and can be canceled,
 * or restarted.
 * <p>
 * A delay of 0 will act like {@link android.os.Handler#post(Runnable)} without a delay.
 */
public class Timeout {

    private final Runnable mRunnable;
    private final Handler mHandler;

    private long mDelay;

    /**
     * Uses the default App handler with no delay. Be sure to use {@link #setDelay(long)} or {@link #start(long)}
     * @param runOnTimeout
     */
    public Timeout(TimeoutListener runOnTimeout) {
        this(runOnTimeout, 0);
    }

    /**
     * Uses the default App handler.
     *
     * @param runOnTimeout
     * @param timeoutMs
     */
    public Timeout(TimeoutListener runOnTimeout, long timeoutMs) {
        this(runOnTimeout, timeoutMs, new Handler(Looper.getMainLooper()));
    }

    public Timeout(final TimeoutListener runOnTimeout, long timeoutMs, Handler handler) {
        mHandler = handler;
        mRunnable = new Runnable() {
            @Override
            public void run() {
                runOnTimeout.onTimeout(Timeout.this);
            }
        };
        setDelay(timeoutMs);
    }

    /**
     * Sets the delay that future calls to {@link #start()} will use as a timeout delay.
     * @param delay In milliseconds
     */
    public Timeout setDelay(long delay) {
        mDelay = delay;
        return this;
    }

    /**
     * Starts the timeout. The runnable will fire at the end of the specified delay.
     * If the timeout is already running, it will cancel the pending one and will restart.
     * If your constructor didn't set a delay, be sure to set one with either {@link #setDelay(long)} or {@link #start(long)}.
     */
    public Timeout start(long delay) {
        cancel();
        setDelay(delay);
        if (mDelay > 0) {
            mHandler.postDelayed(mRunnable, mDelay);
        } else {
            mHandler.post(mRunnable);
        }
        return this;
    }

    /**
     * Starts the timeout. The runnable will fire at the end of the specified delay.
     * If the timeout is already running, it will cancel the pending one and will restart.
     * If your constructor didn't set a delay, be sure to set one with either {@link #setDelay(long)} or {@link #start(long)}.
     */
    public Timeout start() {
        start(mDelay);
        return this;
    }

    /**
     * Stops any pending timeouts. If none are pending nothing will happen. Don't worry about it, it's cool man.
     */
    public void cancel() {
        mHandler.removeCallbacks(mRunnable);
    }

    public interface TimeoutListener {
        public void onTimeout(Timeout timeout);
    }

}
