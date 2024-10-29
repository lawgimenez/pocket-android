package com.pocket.sdk.tts;

import android.speech.tts.TextToSpeech;

import com.pocket.util.java.LocaleUtils;
import com.pocket.util.java.Logs;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Provides the {@link android.speech.tts.Voice} related methods from {@link TextToSpeech} on all API levels.
 * On supported levels, it is backed by {@link android.speech.tts.Voice}s, on older levels it uses available
 * languages as "Voices". It doesn't give you the new voices on older levels, just allows you to work with
 * a consistent API on all levels and is intended to replace the following methods:
 * <ul>
 * <li>{@link TextToSpeech#getVoices()} -> {@link #getVoices(TextToSpeech)}
 * <li>{@link TextToSpeech#setVoice(android.speech.tts.Voice)} -> {@link #setVoice(TextToSpeech, Voice)}
 * <li>{@link TextToSpeech#getAvailableLanguages()} -> {@link #getVoices(TextToSpeech)} and filter out any with feature {@link android.speech.tts.TextToSpeech.Engine#KEY_FEATURE_NOT_INSTALLED}
 * <li>{@link TextToSpeech#setLanguage(Locale)} -> {@link #setVoice(TextToSpeech, Voice)}
 * <li>{@link TextToSpeech#isLanguageAvailable(Locale)} -> {@link #getVoices(TextToSpeech)} only returns available voices.
 * </ul>
 */
public class VoiceCompat {
	
	private static final int UNKNOWN = -1;
	private static final Impl IMPL = new Api21();
	
	/** @see android.speech.tts.Voice */
	public abstract static class Voice implements ListenState.Voice {
		
		public static final Type TYPE = new Type("tts");
		
		/** May return {@link #UNKNOWN}. */
		public abstract int getQuality();
		public abstract String getName();
		public abstract Set<String> getFeatures();
		public abstract Gender getGender();
		
		@Override public Type getType() {
			return TYPE;
		}
	}
	
	public enum Gender {
		UNKNOWN, MALE, FEMALE
	}
	
	
	interface Impl {
		Set<Voice> getVoices(TextToSpeech tts);
	}
	
	public static Set<Voice> getVoices(TextToSpeech tts) {
		return IMPL.getVoices(tts);
	}
	
	/**
	 * Set this voice on the provided TextToSpeech instance.
	 * @return {@link TextToSpeech#ERROR} or {@link TextToSpeech#SUCCESS}
	 */
	public static int setVoice(TextToSpeech tts, Voice voice) {
		if (voice instanceof Api21.Api21Voice) {
			return tts.setVoice(((Api21.Api21Voice)voice).voice);
		} else if (voice instanceof Api1.Api1Voice) {
			return tts.setLanguage(((Api1.Api1Voice)voice).locale);
		} else {
			return TextToSpeech.ERROR;
		}
	}
	
	private static class Api21 implements Impl {
		@Override
		public Set<Voice> getVoices(TextToSpeech tts) {
			try {
				Set<Voice>result = new HashSet<>();
				Set<android.speech.tts.Voice> voices = tts.getVoices();
				for (android.speech.tts.Voice v : voices) {
					result.add(new Api21Voice(v));
				}
				return result;
				
			} catch (Throwable t) {
				// Some devices / engines have internal errors on this method.
				// In this case, fallback to the older locale based approach.
				Logs.printStackTrace(t);
				return new Api1().getVoices(tts);
			}
		}
		
		/** Thin wrapper on {@link android.speech.tts.Voice} to implement {@link Voice}. */
		private static class Api21Voice extends Voice {
			private final android.speech.tts.Voice voice;
			private Api21Voice(android.speech.tts.Voice voice) { this.voice = voice; }
			@Override public Locale getLocale() { return voice.getLocale(); }
			@Override public int getQuality() { return voice.getQuality(); }
			@Override public boolean isNetworkConnectionRequired() { return voice.isNetworkConnectionRequired(); }
			@Override public String getName() { return voice.getName(); }
			@Override public Set<String> getFeatures() { return voice.getFeatures();}
			@Override
			public Gender getGender() {
				if (StringUtils.containsIgnoreCase(getName(), "female")) {
					return Gender.FEMALE;
				} else if (StringUtils.containsIgnoreCase(getName(), "male")) {
					return Gender.MALE;
				} else {
					return Gender.UNKNOWN;
				}
			}
			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Api21Voice that = (Api21Voice) o;
				return voice.equals(that.voice);
			}
			@Override
			public int hashCode() {
				return voice.hashCode();
			}
		}
	}
	
	private static class Api1 implements Impl {
		
		@Override
		public Set<Voice> getVoices(TextToSpeech tts) {
			HashMap<String, Object> locales = new HashMap<>(); // REVIEW can we use the Locale object as a key or do we have to do a string?
			
			Object foundBetterMatch = new Object();
			
			String key;
			Object value;
			
			Locale[] all = Locale.getAvailableLocales();
			for (Locale locale : all) {
				switch (isLanguageAvailableSafeCheck(tts, locale)) {
					case TextToSpeech.LANG_AVAILABLE:
						// Only language matched, add it to the index only by language
						key = locale.getLanguage();
						value = locales.get(key);
						if (value == null) {
							locales.put(key, new Locale(locale.getLanguage()));
						}
						break;
					
					case TextToSpeech.LANG_COUNTRY_AVAILABLE:
						// Only language and country matched, add it to the index only by language and country
						key = locale.getLanguage() + "_" + locale.getCountry();
						value = locales.get(key);
						if (value == null) {
							locales.put(key, new Locale(locale.getLanguage(), locale.getCountry()));
						}
						
						// Check if a less exact (language only) match was added to the index, if so flag it as overridden.
						key = locale.getLanguage();
						value = locales.get(key);
						if (value instanceof Locale) {
							locales.put(key, foundBetterMatch);
						}
						break;
					
					case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
						// The was an exact match for this locale (lang, country, and variant), add it to the index by the full definition
						key = locale.toString();
						value = locales.get(key);
						if (value == null) {
							locales.put(key, locale);
						}
						
						// Check if a less exact (language only) match was added to the index, if so flag it as overridden.
						key = locale.getLanguage();
						value = locales.get(key);
						if (value instanceof Locale) {
							locales.put(key, foundBetterMatch);
						}
						
						// Check if a less exact (language/country only) match was added to the index, if so flag it as overridden.
						key = locale.getLanguage() + "_" + locale.getCountry();
						value = locales.get(key);
						if (value instanceof Locale) {
							locales.put(key, foundBetterMatch);
						}
						break;
					
					case TextToSpeech.LANG_MISSING_DATA:
						locales.put(locale.toString(), new MissingLocale(locale));
						// REVIEW do they return this for partial matches or only full?
						break;
					
					default:
						// Not available
				}
			}
			
			Set<Voice> available = new HashSet<>();
			Set<Map.Entry<String, Object>> set = locales.entrySet();
			for (Map.Entry<String, Object> pair : set) {
				if (pair.getValue() instanceof Locale) {
					available.add(new Api1Voice((Locale) pair.getValue()));
					
				} else if (pair.getValue() instanceof MissingLocale) {
					available.add(new Api1Voice(((MissingLocale) pair.getValue()).locale, "notInstalled")); // TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED
				}
			}
			return available;
		}
		
		private static class MissingLocale {
			private final Locale locale;
			MissingLocale(Locale locale) { this.locale = locale;}
		}
		
		/**
		 * Samsung devices (and likely others?) have a bug that can occur when calling {@link TextToSpeech#isLanguageAvailable(Locale)}.
		 * If you call it with a locale of en_US_POSIX it will throw an {@link IllegalArgumentException}.
		 *
		 * This safely checks and catches any errors and returns {@link TextToSpeech#LANG_NOT_SUPPORTED} if it crashed during the check.
		 *
		 * @return A value like {@link TextToSpeech#LANG_AVAILABLE}, see {@link TextToSpeech#isLanguageAvailable(Locale)}
		 */
		private static int isLanguageAvailableSafeCheck(TextToSpeech tts, Locale locale) {
			try {
				return tts.isLanguageAvailable(locale);
			} catch (Throwable ignore) {
				//Dev.printStackTrace(ignore);
				return TextToSpeech.LANG_NOT_SUPPORTED;
			}
		}
		
		/** Thing wrapper on {@link Locale} to implement {@link Voice}. */
		private static class Api1Voice extends Voice {
			private final Locale locale;
			private final Set<String> features = new HashSet<>();
			private Api1Voice(Locale locale, String... features) {
				this.locale = locale;
				Collections.addAll(this.features, features);
			}
			@Override public Locale getLocale() { return locale; }
			@Override public int getQuality() { return UNKNOWN; }
			@Override public boolean isNetworkConnectionRequired() { return false; }
			@Override public String getName() { return locale.toString(); }
			@Override public Set<String> getFeatures() { return features; }
			@Override public Gender getGender() { return Gender.UNKNOWN; }
			@Override
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Api1Voice api1Voice = (Api1Voice) o;
				return locale.equals(api1Voice.locale);
			}
			@Override
			public int hashCode() {
				return locale.hashCode();
			}
		}
	}
	
	/** Find best voice for default locale. */
	public static Voice findBestMatch(TextToSpeech tts) {
		Locale l = Locale.getDefault();
		if (LocaleUtils.isEnglish(l) && LocaleUtils.isUS(l)) {
			l = new Locale("us", "GB"); // The gb voices sound much nicer than the us ones.
		}
		return findBestMatch(l, null, tts);
	}
	
	public static Voice findBestMatch(Locale locale, String name, TextToSpeech tts) {
		return findBestMatch(locale, name, getVoices(tts));
	}
	
	/**
	 * Finds a voice best matching the provided name, locale or both, with
	 * the best possible quality, avoiding network only voices if possible.
	 * Prefers voices that are installed.
	 * Returns null if no voices, or if none match the locale in any form.
	 */
	public static Voice findBestMatch(Locale locale, String name, Set<Voice> available) {
		if (available.isEmpty()) {
			return null;
		}
		
		// First filter by name, with the expectation that names are unique
		if (name != null) {
			for (Voice v : available) {
				if (v.getName().equals(name)) {
					return v;
				}
			}
		}
		
		Map<Voice, Integer> localeScores = new HashMap<>();
		for (Voice v : available) {
			if (locale != null) {
				if (LocaleUtils.localeEquals(locale, v.getLocale(), true, true, true)) {
					// Exact match (language, country and variant)
					localeScores.put(v, 4);
				} else if (LocaleUtils.localeEquals(locale, v.getLocale(), true, true, false)) {
					// Non variant match (language, country)
					localeScores.put(v, 3);
				} else if (LocaleUtils.localeEquals(locale, v.getLocale(), true, false, false)) {
					// Language match (language)
					localeScores.put(v, 2);
				} else if (LocaleUtils.localeEquals(locale, v.getLocale(), false, true, false)) {
					// Country match (country)
					localeScores.put(v, 1);
				} else {
					// Not a match
					localeScores.put(v, 0);
				}
			} else {
				localeScores.put(v, 0);
			}
		}
		
		List<Voice> ranked = new ArrayList<>(available);
		Collections.sort(ranked, (v1, v2) -> {
			// Prioritize Locale Matching
			int c = Integer.compare(localeScores.get(v2), localeScores.get(v1));
			if (c != 0) {
				return c;
			}
			// Then being installed
			c = Boolean.compare(v1.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED), v2.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED));
			if (c != 0) {
				return c;
			}
			// Then not being dependant on a network connection
			c = Boolean.compare(v1.isNetworkConnectionRequired(), v2.isNetworkConnectionRequired());
			if (c != 0) {
				return c;
			}
			// Finally by quality
			return Integer.compare(v2.getQuality(), v1.getQuality());
		});
		
		return ranked.get(0);
	}
	
}
