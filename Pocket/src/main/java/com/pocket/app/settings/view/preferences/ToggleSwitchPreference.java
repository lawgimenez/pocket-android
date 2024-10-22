package com.pocket.app.settings.view.preferences;

import android.util.SparseArray;
import android.view.View;

import androidx.annotation.Nullable;

import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.ui.view.settings.SettingsSwitchView;

import static com.pocket.app.settings.view.preferences.ActionPreference.SUMMARY_CHECKED;
import static com.pocket.app.settings.view.preferences.ActionPreference.SUMMARY_DEFAULT_OR_UNCHECKED;
import static com.pocket.app.settings.view.preferences.ActionPreference.SUMMARY_UNAVAILABLE;

public class ToggleSwitchPreference extends Preference {

    public interface OnChangeListener {
        /**
         * Called when the preference is requested to change.
         * @param view The view that triggered the change
         * @param nowEnabled
         * @return true if allowed to change, false if not
         */
        boolean onChange(View view, boolean nowEnabled);

        /**
         * After the value has changed.
         *
         * @param nowEnabled
         */
        void afterChange(boolean nowEnabled);
    }

    public interface PrefHandler {
        boolean get();
        void set(boolean value);
    }

    private final PrefHandler pref;
    private final OnChangeListener listener;
    private final String label;
    private final SparseArray<CharSequence> summary;
    private final PreferenceViews.EnabledCondition enabledCondition;

    private boolean isChecked;

    private @Nullable UiEntityIdentifier uiEntityIdentifier;

    public ToggleSwitchPreference(AbsPrefsFragment settings, PrefHandler pref, String label, SparseArray<CharSequence> summary, OnChangeListener listener, PreferenceViews.EnabledCondition condition, @Nullable UiEntityIdentifier identifier) {
        super(settings);
        this.pref = pref;
        this.listener = listener;
        this.isChecked = pref.get();
        this.label = label;
        this.summary = summary;
        this.enabledCondition = condition;
        this.uiEntityIdentifier = identifier;
    }

    private CharSequence getSummary() {
        if (summary == null || summary.size() == 0) {
            return null;
        } else {
            CharSequence sum = null;
            if (isEnabled()) {
                if (isChecked) {
                    sum = summary.get(SUMMARY_CHECKED);
                }
            } else {
                sum = summary.get(SUMMARY_UNAVAILABLE);
            }

            if (sum == null) {
                sum = summary.get(SUMMARY_DEFAULT_OR_UNCHECKED);
            }
            return sum;
        }
    }

    @Override
    public PrefViewType getType() {
        return PrefViewType.TOGGLE;
    }

    @Override
    public void applyToView(View layout) {
        SettingsSwitchView view = (SettingsSwitchView) layout;
        view.bind().isToggle(true).title(label).subtitle(getSummary()).checked(isEnabled() && isChecked);
        if (uiEntityIdentifier != null) {
            view.setUiEntityIdentifier(uiEntityIdentifier.value);
        }
    }

    @Override
    public boolean isEnabled() {
        if (enabledCondition != null) {
            return enabledCondition.isTrue();
        } else {
            return true;
        }
    }

    @Override
    public boolean isClickable() {
        return true;
    }

    @Override
    public boolean update() {
        boolean newVal = pref.get();
        if (newVal != isChecked) {
            isChecked = newVal;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        boolean newValue = !isChecked;
        boolean allowed = listener == null || listener.onChange(v, newValue);

        if (allowed) {
            isChecked = newValue;
            pref.set(newValue);

            if (listener != null) {
                listener.afterChange(newValue);
            }

            mSettings.onPreferenceChange(true);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

}
