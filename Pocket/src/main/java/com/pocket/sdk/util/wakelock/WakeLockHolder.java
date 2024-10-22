package com.pocket.sdk.util.wakelock;

/**
 * A unique part of the app that uses a wakelock and logic for
 * how long it expects to use the lock so {@link WakeLockManager} can
 * catch case where this lock is held accidentally too long.
 * <p>
 * The point of all of this extra logic is to allow us to use wake locks
 * <p>
 * Use one of the static methods to create one.
 * The simplest and most common one is {@link #withTimeout(String, int, int, OnTimeout)}
 */
public class WakeLockHolder {
    
    /**
     * Uses a timeout for safety.
     * <p>
     * Note: These timeouts are not meant for functional usage. You should make sure you always
     * release your locks. This is just safety fallback to avoid accidental holding indefinite or long locks.
     * <p>
     * Note: These timeouts only apply to time spent in the background.
     * Each time the user leaves the app, timeouts restart from 0.
     *
     * @param name A name (should be unique app wide) that indicates who/what component needs the lock.
     *             Be aware users may see this name device settings or in reports they generate.
     *             This name is used to determine equality, so instances with the same name are considered
     *             the same holder.
     *
     * @param warnTimeout Optional. (If you don't need this, pass 0)
     *             If the lock is held longer than this number of minutes,
     *             it will quietly log but allow the lock to continue to be held.
     *             Use this for "I don't think it should  be held this long, but if it is I'd like to know about it, but not release the lock."
     *
     * @param stopTimeout Required.
     *                If the lock is held longer than this number of minutes,
     *                it will quietly log and automatically release it.
     *                Google Play perceives a held wake lock of 1 hour as bad behaviour,
     *                so it is encouraged to use a value less than 60.
     *                If you need a long or indefinite lock (like for audio playback)
     *                use {@link #withLivinessCheck(String, int, LivelinessCheck, OnTimeout)} instead.
     *
     * @param onTimeout Optional, if you want to provide some additional debug info for logs if it timed out.
     */
    public static WakeLockHolder withTimeout(String name, int warnTimeout, int stopTimeout, OnTimeout onTimeout) {
        return new WakeLockHolder(name, warnTimeout, stopTimeout, onTimeout);
    }
    
    /**
     * Checks some condition on an interval to make sure it isn't being held to long.
     * Useful for indefinite or long locks like audio playback.
     * <p>
     * Note: This check is not meant to be functional. You should make sure you always
     * release your locks. This is just safety fallback to avoid accidental holding indefinite or long locks.
     * <p>
     * In the example of audio playback, the LivelinessCheck can keep track of the state
     * of playback, if it notices that it hasn't changed since the last interval, it can
     * assume the lock has been accidentally held to long and can be released.
     *
     * @param name A name (should be unique app wide) that indicates who/what component needs the lock.
     *             Be aware users may see this name device settings or in reports they generate.
     *             This name is used to determine equality, so instances with the same name are considered
     *             the same holder.
     * @param checkInterval In minutes, how often to run the {@link LivelinessCheck}.
     * @param check The logic to run on each interval to check if the lock has accidentally been held too long.
     * @param onTimeout Optional, if you want to provide some additional debug info for logs if it timed out.
     */
    public static WakeLockHolder withLivelinessCheck(String name, int checkInterval, LivelinessCheck check, OnTimeout onTimeout) {
        return new WakeLockHolder(name, checkInterval, check, onTimeout);
    }

    public final String name;
    public final int warnTimeout;
    public final int stopTimeout;
    public final OnTimeout onTimeout;
    public final int checkInterval;
    public final LivelinessCheck check;
    
    private WakeLockHolder(String name, int warnTimeout, int stopTimeout, OnTimeout onTimeout) {
        if (name == null  || name.length() == 0) throw new IllegalArgumentException("Name must not be empty and must be unique.");
        if (stopTimeout < 1) throw new IllegalArgumentException("All wakelocks must have a stopTimeout > 0");
        this.name = name;
        this.warnTimeout = warnTimeout;
        this.stopTimeout = stopTimeout;
        this.onTimeout = onTimeout;
        this.checkInterval = 0;
        this.check = null;
    }
    
    private WakeLockHolder(String name, int checkInterval, LivelinessCheck check, OnTimeout onTimeout) {
        if (name == null  || name.length() == 0) throw new IllegalArgumentException("Name must not be empty and must be unique.");
        if (checkInterval < 1) throw new IllegalArgumentException("must supply a check interval");
        if (check == null) throw new IllegalArgumentException("must supply check logic");
        this.name = name;
        this.warnTimeout = 0;
        this.stopTimeout = 0;
        this.onTimeout = onTimeout;
        this.checkInterval = checkInterval;
        this.check = check;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WakeLockHolder that = (WakeLockHolder) o;
        return name.equals(that.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    public interface OnTimeout {
        /**
         * Your wakelock was held longer than you expected.
         * Return a string with any additional data that will be useful to debug this.
         * Do not include user or personal data.
         * @param
         */
        String onTimedOut();
    }
    
    public interface LivelinessCheck {
        /**
         * Fired each interval.
         * @return true if the lock has been held longer than expected and should time out, false to keep it going.
         */
        boolean keepAlive();
    }
    
}