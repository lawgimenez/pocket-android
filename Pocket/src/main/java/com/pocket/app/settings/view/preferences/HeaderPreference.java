package com.pocket.app.settings.view.preferences;

import android.view.View;

import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.ui.view.menu.SectionHeaderView;

/**
 * A non-{@link AppPrefs} Preference that shows a {@link HeaderPreferenceView}. Used to display
 * dividing headers within Settings.
 */
public class HeaderPreference extends Preference {
	
	private final String mLabel;
	private final boolean topDivider;

	public HeaderPreference(AbsPrefsFragment settings, String label, boolean topDivider) {
		super(settings);
		this.mLabel = label;
		this.topDivider = topDivider;
	}

	@Override
	public PrefViewType getType() {
		return PrefViewType.HEADER;
	}

	@Override
	public void applyToView(View layout) {
		SectionHeaderView view = (SectionHeaderView) layout;
		view.bind().label(mLabel).showTopDivider(topDivider).showBottomDivider(false).textAllCaps(true);
	}

	@Override
	public boolean isEnabled() {
		return false;
	}
	
	/**
	 * Recheck the setting
	 * @return true if the setting changed, false if remains the same as before.
	 */
	@Override
	public boolean update() {
		return false;
	}

	@Override
	public void onClick(View v) {}

    @Override
    public boolean onLongClick(View v) {
        return false;
    }

    @Override
	public boolean isClickable() {
		return false;
	}

}
