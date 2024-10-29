package com.pocket.app.settings.rotation.interf;

/**
 * A manager class which handles "fine" orientation changes when listening for the orientation via the device sensor.
 */
public interface FineOrientationManager {

    interface OnNewRotationListener {
        void onNewRotation();
    }

    /**
     * Marks the current orientation as the last known orientation.
     */
    void markCurrentOrientation();

    /**
     * @param listener a listener for changes to the orientation.
     */
    void setOnNewRotationListener(OnNewRotationListener listener);

    /**
     * @param enabled whether to listen for "fine" orientation changes.
     */
    void setEnabled(boolean enabled);

}
