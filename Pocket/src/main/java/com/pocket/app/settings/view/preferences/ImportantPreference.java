package com.pocket.app.settings.view.preferences;

import android.view.View;

import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.ui.view.settings.SettingsImportantButton;

public class ImportantPreference extends ActionPreference {

    public ImportantPreference(AbsPrefsFragment settings, String label, OnClickAction action, OnClickAction longPressAction, PreferenceViews.EnabledCondition condition, UiEntityIdentifier identifier) {
        super(settings, label, null, action, longPressAction, condition, identifier);
    }

    @Override
    public void applyToView(View layout) {
        SettingsImportantButton view = (SettingsImportantButton) layout;
        view.bind().text(label);
        if (uiEntityIdentifier != null) {
            view.setUiEntityIdentifier(uiEntityIdentifier.value);
        }
    }

    @Override
    public PrefViewType getType() {
        return PrefViewType.IMPORTANT;
    }

}