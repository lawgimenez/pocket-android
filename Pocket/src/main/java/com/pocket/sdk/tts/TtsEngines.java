package com.pocket.sdk.tts;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.tts.TtsEngineCompat.EngineInfoCompat;
import com.pocket.util.java.JsonUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for getting information about which TTS engines are installed and what the user prefers to use.
 * 
 * @author max
 *
 */
public class TtsEngines {
	
	private final List<EngineInfoCompat> mAvailableEngines;
	private final EngineInfoCompat mPreferredEngine;
	private final boolean mIsPreferenceExpired;
	
	public TtsEngines() {
		List<EngineInfoCompat> engines = TtsEngineCompat.getEngines(App.getContext());
		List<String> lastKnownEngines = getLastKnownEngines();
		boolean isExpired = false;
		
		// Check if installed engines have changed since we last checked.
		if (lastKnownEngines.size() != engines.size()) {
			isExpired = true;
		} else {
			for (String packagename : lastKnownEngines) {
				boolean match = false;
				for (EngineInfoCompat engine : engines) {
					if (engine.name.equals(packagename)) {
						match = true;
						break;
					}
				}
				if (!match) {
					isExpired = true;
					break;
				}
			}
		}
		
		// Check if preferred engine is still installed.
		String preferred = App.getApp().prefs().TTS_ENGINE.get();
		EngineInfoCompat preferredEngine = null;
		if (preferred != null) {
			for (EngineInfoCompat engine : engines) {
				if (engine.name.equals(preferred)) {
					preferredEngine = engine;
					break;
				}
			}
		}
		if (preferredEngine == null && !engines.isEmpty()) {
			// Use highest priority engine as default
			preferredEngine = engines.get(0);
		}
		
		mAvailableEngines = engines;
		mPreferredEngine = preferredEngine;
		mIsPreferenceExpired = isExpired;
	}
	
	/**
	 * @return Returns the list of engine package names, the last time the user made a choice about which to use. 
	 */
	private List<String> getLastKnownEngines() {
		ArrayNode array = JsonUtil.stringToArrayNode(App.getApp().prefs().TTS_ENGINES_LAST_KNOWN.get());
		ArrayList<String> engines = new ArrayList<String>(array != null ? array.size() : 0);
		if (array != null) {
			for (JsonNode node : array) {
				engines.add(node.asText());
			}
		}
		return engines;
	}
	
	/**
	 * @param engines
	 * @see #getLastKnownEngines()
	 */
	private void saveLastKnownEngines(List<EngineInfoCompat> engines) {
		ArrayNode array = JsonUtil.newArrayNode();
		for (EngineInfoCompat engine : engines) {
			array.add(engine.name);
		}
		App.getApp().prefs().TTS_ENGINES_LAST_KNOWN.set(array.toString());
	}

	/**
	 * Is the user's choice about which engine to use still valid?
	 * <p>
	 * If the user hasn't made a choice, or if an engine has been installed or uninstalled
	 * since they made a choice, their preference is expired and they should be asked again.
	 * @return
	 */
	public boolean isPreferenceExpired() {
		return mIsPreferenceExpired && mAvailableEngines.size() > 1;
	}
	
	/**
	 * @return The preferred engine or null if there are no Tts engines installed.
	 */
	public EngineInfoCompat getPreferredTtsEngine() {
		return mPreferredEngine;
	}
	
	/**
	 * @return The TTS engines installed on this device
	 */
	public List<EngineInfoCompat> getEngines() {
		return mAvailableEngines;
	}
	
	/**
	 * Set the user's preferred TTS engine
	 * @param engine
	 */
	private void setPreferred(EngineInfoCompat engine) {
		App.getApp().prefs().TTS_ENGINE.set(engine.name);
		saveLastKnownEngines(mAvailableEngines);
	}
	
	/**
	 * Show the user a dialog which will let them choose which engine they prefer.
	 * @param context
	 * @param listener
	 */
	public void showPicker(Context context, final OnTtsEngineSelectedListener listener) {
		final List<EngineInfoCompat> enginesInfo = mAvailableEngines;
		 
		 if (enginesInfo.isEmpty()) {
			 TTSUtils.showInstallRequiredDialog(context);
			 
		 } else {
			 int size = enginesInfo.size();
			 final CharSequence[] labels = new CharSequence[size];
			 for (int i = 0; i < size; i++) {
				 labels[i] = enginesInfo.get(i).label;
			 }
			 
			 AlertDialog dialog = new AlertDialog.Builder(context)
				.setTitle(R.string.tts_dg_choose_engine_t)
			 	.setItems(labels, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						EngineInfoCompat engine = enginesInfo.get(which);
						setPreferred(engine);
						listener.onSelected(engine);
					}
				})
				.create();
		
			dialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					listener.onCanceled();
				}
			});
			
			dialog.show();
		 }
	}
	
	public interface OnTtsEngineSelectedListener {
		public void onCanceled();
		public void onSelected(EngineInfoCompat engine);
	}

}
