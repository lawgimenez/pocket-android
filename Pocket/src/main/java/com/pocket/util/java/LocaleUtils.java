package com.pocket.util.java;

import android.content.Context;

import com.pocket.app.App;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.MissingResourceException;

public class LocaleUtils {
	
	/**
	 * Returns true if the current locale is one that we have localized/translated Pocket for.
	 * @param context The context to get the current locale from. If null, it will use the application context to get current locale.
	 * @return
	 */
	public static boolean isLocalizedForPocket(Context context) {
		if (context == null) {
			context = App.getContext();
		}
		Locale currentLocale = context.getResources().getConfiguration().locale;
		String lang = currentLocale.getLanguage();
		
		return StringUtils.equalsIgnoreCase(lang, "en")
			|| StringUtils.equalsIgnoreCase(lang, "de")	
			|| StringUtils.equalsIgnoreCase(lang, "es")
			|| StringUtils.equalsIgnoreCase(lang, "fr")
			|| StringUtils.equalsIgnoreCase(lang, "it")
			|| StringUtils.equalsIgnoreCase(lang, "ja") 
			|| StringUtils.equalsIgnoreCase(lang, "ru")
			|| StringUtils.equalsIgnoreCase(lang, "pl")
			|| StringUtils.equalsIgnoreCase(lang, "pt")
			|| StringUtils.equalsIgnoreCase(lang, "nl")
			|| StringUtils.equalsIgnoreCase(lang, "zh")
			|| StringUtils.equalsIgnoreCase(lang, "ko");
	}
	
	public static boolean isEnglish(Context context) {
		return isEnglish(context.getResources().getConfiguration().locale);
	}
	
	public static boolean isEnglish(Locale locale) {
		return StringUtils.equalsIgnoreCase(locale.getLanguage(), "en");
	}

	public static boolean isUS(Context context) {
		return isUS(context.getResources().getConfiguration().locale);
	}
	
	public static boolean isUS(Locale locale) {
		return StringUtils.equalsIgnoreCase(locale.getCountry(), "us");
	}
	
	public static boolean isGerman(Context context) {
		return isGerman(context.getResources().getConfiguration().locale);
	}
	
	public static boolean isGerman(Locale locale) {
		return StringUtils.equalsIgnoreCase(locale.getLanguage(), "de");
	}
	
	/**
	 * Compares two locales regardless if the locale uses a two or three letter code. 
	 * Locale.equals() returns false when comparing "eng_USA" and "en_US". This method
	 * would return true in that case.
	 * 
	 * Use the boolean flags to set how strong the match must be. If false is provided for
	 * all flags an error will be thrown.
	 * 
	 * @param locale1
	 * @param locale2
	 * @param language if the langauge must match set true
	 * @param country if the country must match set true
	 * @param variant if the variant must match set true
	 * @return
	 */
	public static boolean localeEquals(Locale locale1, Locale locale2, boolean language, boolean country, boolean variant) {
		if (!language && !country && !variant)
			throw new RuntimeException("no flags set");

        /*
            According to http://docs.oracle.com/javase/7/docs/api/java/util/Locale.html
            Language and Country are case insensitive and variant is case sensitive.
            So using equalsIgnoreCase() for Country and Lang and equals() for variant

            Seems like TTSPlayer can sometimes provide locales with upper or lowercase country codes,
            so need to be case insensitive.
        */

		if (language) {
			if (!getLanguageSafely(locale1).equalsIgnoreCase(getLanguageSafely(locale2))) {
				return false;
			}
		}
		
		if (country) {
			if (!getCountrySafely(locale1).equalsIgnoreCase(getCountrySafely(locale2))) {
				return false;
			}
		}
		
		if (variant) {
			if (!locale1.getVariant().equals(locale2.getVariant())) {
				return false;
			}
		}
		
		return true;
	}
		
	/**
	 * Try to get the country name with {@link Locale.getISO3Country()} and
	 * default to {@link Locale.getCountry()} if getISO3Country() throw
	 * a {@link MissingResourceException} exception
	 * 
	 * @param locale
	 * @return country name
	 */
	private static String getCountrySafely(Locale locale) {
		try {
			return locale.getISO3Country();
		} catch (Exception e) {
			Logs.printStackTrace(e);
			return locale.getCountry();
		}
	}
	
	
	/**
	 * Try to get the country name with {@link Locale.getISO3Language()} and
	 * default to {@link Locale.getLanguage()} if getISO3Language() throw
	 * a {@link MissingResourceException} exception
	 * 
	 * @param locale
	 * @return language name
	 */
	private static String getLanguageSafely(Locale locale){
		try {
			return locale.getISO3Language();
		} catch (Exception e) {
			Logs.printStackTrace(e);
			return locale.getLanguage();
		}
	}

}
