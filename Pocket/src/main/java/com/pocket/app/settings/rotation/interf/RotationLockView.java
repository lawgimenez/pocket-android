package com.pocket.app.settings.rotation.interf;

/**
 * A View that displays a "rotation lock" toggle which allows the user
 * to lock the current display orientation within the Pocket App.
 */
public interface RotationLockView {

    interface OnClick {
        void onClick(boolean checked);
    }

    void setOnToggleClick(OnClick onclick);

    void show(boolean checked);

    void hide();

}
