package com.pocket.sdk.tts;

import android.content.Context;

import com.ideashower.readitlater.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO Documentation
 */

public class NamedVoice {
	
	public static List<NamedVoice> name(Collection<VoiceCompat.Voice> voices, Context context) {
		List<NamedVoice> named = new ArrayList<>();
		
		int unknown = 0;
		int female = 0;
		int male = 0;
		
		for (VoiceCompat.Voice voice : voices) {
			switch (voice.getGender()) {
				case MALE:
					++male;
					named.add(new NamedVoice(male + 1000, context.getString(R.string.tts_voice_male, String.valueOf(male)), voice));
					break;
				case FEMALE:
					++female;
					named.add(new NamedVoice(female, context.getString(R.string.tts_voice_female, String.valueOf(female)), voice));
					break;
				case UNKNOWN:
				default:
					++unknown;
					named.add(new NamedVoice(unknown + 2000, context.getString(R.string.tts_voice, String.valueOf(unknown)), voice));
					break;
			}
		}
		return named;
	}

	public final int sortOrder;
	public final CharSequence name;
	public final VoiceCompat.Voice voice;
	public NamedVoice(int sortOrder, CharSequence name, VoiceCompat.Voice voice) {
		this.sortOrder = sortOrder;
		this.name = name;
		this.voice = voice;
	}
	
}
