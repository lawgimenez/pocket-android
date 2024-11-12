package com.pocket.app.settings.rotation.interf;

/**
 * An abstract representation of an OS level rotation lock, as seen in most device's "Quick Settings" menu dropdown.
 */
public interface OSRotationLock {

    /**
     * Called when you would like to start observing for OS rotation lock changes.
     * Typically you only want to be observing when the user is actually on the screen.
     */
    void startObserving();

    /**
     * Called when you would like to stop observing for OS rotation lock changes.
     * Typically done when the user is no longer viewing the screen.
     */
    void stopObserving();

    /**
     * @return boolean whether the OS rotation lock is currently on.
     */
    boolean isLocked();

}
