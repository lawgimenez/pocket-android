package com.pocket.sdk.notification.push;

import com.pocket.sdk.api.generated.enums.CxtUi;

public interface Push {

    interface RegistrationListener {
        void onResult(boolean success, String message);
    }

    /**
     * @return boolean whether push notifications are supported on this device
     */
    boolean isAvailable();

    /**
     * Registers this device with the backend to receive push notifications
     */
    void register(CxtUi cxt_ui, RegistrationListener registrationListener);

    /**
     * Deregisters this device from receiving push notifications
     */
    void deregister(CxtUi cxt_ui);

    /**
     * Invalidates our current registration and re-registers with the backend for push notifications
     */
    void invalidate();

    /**
     * Returns the user's current push notification token (FCM).  This is stored locally only in internal builds
     * and will return null in production. This is useful for debugging or testing targeted notifications to a
     * test device.
     *
     * @return the user's current FCM token (dev internal) or null (prod).
     */
    String getToken();
}
