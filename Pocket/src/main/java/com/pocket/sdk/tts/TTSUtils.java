package com.pocket.sdk.tts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;

import com.ideashower.readitlater.R;
import com.pocket.util.android.IntentUtils;

/**
 * REVIEW move to {@link Listen} or keep here?
 */
public abstract class TTSUtils {
	
	/**
	 * 
	 * @param activity
	 * @param resultCode
	 * @param data
	 * return The available voices (if the install check was successful)
	 *
	 */
	public static ArrayList<Locale> onInstallResponse(final Activity activity, int resultCode, Intent data) {
		ArrayList<String> voices = data != null ? data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES) : null;
		
		if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL && voices != null && voices.size() > 0) {
			// Success
			ArrayList<Locale> locales = new ArrayList<Locale>(voices.size());
			for (String voice : voices) {
				String[] parts = voice.split("-");
				String lang = parts[0];
				String country = parts.length > 1 ? parts[1] : null;
				String variant = parts.length > 2 ? parts[2] : null;
				locales.add(new Locale(lang, StringUtils.defaultIfBlank(country, ""), StringUtils.defaultIfBlank(variant, "")));
			}
			
			Collections.sort(locales, new Comparator<Locale>() {
				
				@Override
				public int compare(Locale locale1, Locale locale2) {
					return locale1.getDisplayName().compareTo(locale2.getDisplayName());
				}
				
			});
			
			return locales;
			
		} else {
			showInstallRequiredDialog(activity);
			return null;
		}
	}
	
	public static void showInstallRequiredDialog(final Context context) {
		new AlertDialog.Builder(context)
			.setTitle(R.string.tts_dg_install_t)
			.setMessage(R.string.tts_dg_install_m)
			.setNegativeButton(R.string.ac_cancel, null)
			.setPositiveButton(R.string.ac_install, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Intent installIntent = new Intent();
		            installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
		            if (IntentUtils.isActivityIntentAvailable(context, installIntent)) {
		            	context.startActivity(installIntent);
		            	
		            } else {
		            	new AlertDialog.Builder(context)
							.setTitle(R.string.tts_dg_install_t)
							.setMessage(R.string.tts_dg_not_supported_m)
							.setNeutralButton(R.string.ac_ok, null)
							.show();
		            }
				}
			})
			.show();
	}
	
	public static void openTTSSettings(Activity activity) {
		Intent intent = new Intent("com.android.settings.TTS_SETTINGS");
		if (IntentUtils.isActivityIntentAvailable(activity, intent)) {
			activity.startActivity(intent);
			
		} else {
			new AlertDialog.Builder(activity)
				.setTitle(R.string.tts_settings)
				.setMessage(R.string.tts_dg_missing_settings_m)
				.setNeutralButton(R.string.ac_ok, null)
				.show();
		}
		
	}
	
}
