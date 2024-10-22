package com.pocket.app.settings.view.preferences;

import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.tts.TtsEngineCompat.EngineInfoCompat;
import com.pocket.sdk.tts.TtsEngines;
import com.pocket.sdk.tts.TtsEngines.OnTtsEngineSelectedListener;

import org.apache.commons.lang3.StringUtils;

/**
 * A very specific preference for managing {@link TtsEngines}.
 */
public class TtsEnginePreference extends ActionPreference {
	
	private TtsEngines mEngines;
	private String mSelected;
	
	public TtsEnginePreference(AbsPrefsFragment settings,
							   String label,
							   PreferenceViews.EnabledCondition enabledCondition, UiEntityIdentifier identifier) {
		super(settings, label, null, null, null, enabledCondition, identifier);
		mEngines = new TtsEngines();
		EngineInfoCompat engine = mEngines.getPreferredTtsEngine();
		mSelected = engine != null ? engine.label : null;
	}
	
	@Override
	public PrefViewType getType() {
		return PrefViewType.ACTION;
	}
	
	@Override
	public void onClick(View view) {
		mEngines.showPicker(mSettings.getActivity(), new OnTtsEngineSelectedListener() {
			
			@Override
			public void onSelected(EngineInfoCompat engine) {
				update();
			}
			
			@Override
			public void onCanceled() {}
		});
	}
	
	@Override
	public boolean update() {
		mEngines = new TtsEngines(); // New instance to reflect changes to new installed engines if they left the app and returned.
		EngineInfoCompat engine = mEngines.getPreferredTtsEngine();
		String newSelection = engine != null ? engine.label : null;
		
		if (!StringUtils.equals(newSelection, mSelected)) {
			mSelected = newSelection;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public String getSummary() {
		return isEnabled() 
				? mSelected != null ? mSelected : App.getStringResource(R.string.setting_tts_sum_fallback)
				: App.getStringResource(R.string.listen_settings_picker_summary_unavailable);
	}
	
	@Override
	public boolean isClickable() {
		return true;
	}
	
}
