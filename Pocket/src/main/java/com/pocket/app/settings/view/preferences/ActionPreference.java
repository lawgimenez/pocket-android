package com.pocket.app.settings.view.preferences;

import android.util.SparseArray;
import android.view.View;

import com.pocket.analytics.Tracker;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.PreferenceViews.EnabledCondition;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.ui.view.settings.SettingsSwitchView;

/**
 * This is a basic {@link Preference} that can register a simple click listener.
 */
public class ActionPreference extends Preference implements View.OnLongClickListener {
	
	public static int SUMMARY_UNAVAILABLE = -1;
	public static int SUMMARY_DEFAULT_OR_UNCHECKED = 0;
	public static int SUMMARY_CHECKED = 1;
	
	protected final String label;
	protected final SparseArray<CharSequence> summary;

	private final EnabledCondition enabledCondition;
	private final OnClickAction action;
    private final OnClickAction longPressAction;

	protected final UiEntityIdentifier uiEntityIdentifier;
	protected final Tracker tracker;

	/** Use {@link PreferenceViews} instead. */
	@Deprecated
	public ActionPreference (AbsPrefsFragment settings, String label, SparseArray<CharSequence> summary, OnClickAction action, OnClickAction longPressAction, EnabledCondition condition, UiEntityIdentifier identifier) {
		super(settings);

		if (label == null) {
			throw new NullPointerException("label cannot be null");
		}
		this.label = label;
		this.summary = summary;
		enabledCondition = condition;
		this.action = action;
		this.longPressAction = longPressAction;
		uiEntityIdentifier = identifier;
		tracker = settings.getAbsPocketActivity().app().tracker();
	}

	public CharSequence getSummary() {
		if (summary == null || summary.size() == 0) {
			return null;
			
		} else {
			if (isEnabled()) {
				return summary.get(SUMMARY_DEFAULT_OR_UNCHECKED);
			} else {
				CharSequence sum = summary.get(SUMMARY_UNAVAILABLE);
				if (sum == null) {
					return summary.get(SUMMARY_DEFAULT_OR_UNCHECKED);
				} else {
					return sum;
				}
			}
		}
	}
	
	public ActionPreference updateSummary(int key, CharSequence summary) {
		this.summary.put(key, summary);
		return this;
	}

	@Override
	public void onClick(View view) {
		if (action != null) {
			action.onClick();
		}
	}

    @Override
    public boolean onLongClick(View v) {
        if (longPressAction != null) {
            longPressAction.onClick();
            return true;
        } else {
            return false;
        }
    }

    @Override
	public PrefViewType getType() {
		return PrefViewType.ACTION;
	}
	
	public interface OnClickAction {
		void onClick();
	}

	@Override
	public void applyToView(View layout) {
		SettingsSwitchView view = (SettingsSwitchView) layout;
		view.bind().isToggle(false).title(label).subtitle(getSummary());
		if (uiEntityIdentifier != null) {
			view.setUiEntityIdentifier(uiEntityIdentifier.value);
		}
	}

	@Override
	public boolean isEnabled() {
		if (!isClickable()) {
			return false;
			
		} else if (enabledCondition != null) {
			return enabledCondition.isTrue();
			
		} else {
			return true;
		}
	}
	
	@Override
	public boolean isClickable() {
		return action != null;
	}

	@Override
	public boolean update() {
		return false;
	}

}
