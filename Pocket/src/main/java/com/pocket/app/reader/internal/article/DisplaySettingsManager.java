package com.pocket.app.reader.internal.article;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.View;
import android.webkit.WebView;

import androidx.annotation.StringRes;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.build.Versioning;
import com.pocket.app.premium.PremiumFonts;
import com.pocket.app.premium.PremiumReader;
import com.pocket.app.settings.Brightness;
import com.pocket.app.settings.SystemDarkTheme;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.action.Pv;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.ui.text.Fonts;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.ScreenWidth;
import com.pocket.util.java.Logs;
import com.pocket.util.java.Range;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.IntPreference;
import com.pocket.util.prefs.Preferences;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * A static singleton manager for setting, getting and listening to changes made to Display Settings. Controls things like font, font size,
 * theme, brightness etc.
 */
@Singleton
public class DisplaySettingsManager {
	
	private final PremiumReader premiumReader;
	private final Theme theme;
	private final SystemDarkTheme systemDarkTheme;
	private final ThemeAnalytics themeAnalytics;
	private final IntPreference fontSize;
	private final IntPreference lineHeight;
	private final IntPreference margin;
	private final BooleanPreference justify;
	private final IntPreference fontChoice;
	private final Context context;
	/** This is only used for tracking this here in DisplaySettingsManager. Use appTheme to get the current theme value. */
	private final BooleanPreference themeIsDark;
	
	/** Maximum margin in web px, meaning the px that javascript and css use, <b>not</b> screen/native px considering zoom/scaling. */
	private int MAX_MARGIN;

	/** Minimum margin in web px, meaning the px that javascript and css use, <b>not</b> screen/native px considering zoom/scaling. */
	private int MIN_MARGIN;

	private final int DEFAULT_MARGIN;

	private int[] FONT_SIZES;
	private Range FONT_SIZE_RANGE;

	private int[] LINE_HEIGHTS;
	private Range LINE_HEIGHT_RANGE;

	private int[] MARGINS;
	private Range MARGIN_RANGE;

	private ArrayList<OnDisplaySettingsChangedListener> sListeners = new ArrayList<>();
	
	@Inject
	public DisplaySettingsManager(
			Pocket pocket,
			PremiumReader premiumReader,
			Theme theme,
			SystemDarkTheme systemDarkTheme,
			@ApplicationContext Context context,
			Versioning versioning,
			Preferences prefs,
			AppPrefs appPrefs
	) {
		final Resources resources = context.getResources();
		this.context = context;
		this.fontSize = prefs.forUser("articleFontSize2", resources.getInteger(R.integer.article_default_font_size));
		this.lineHeight = prefs.forUser("articleLineHeight", resources.getInteger(R.integer.article_default_line_height));
		this.margin = prefs.forUser("articleMargin", resources.getInteger(R.integer.article_default_margin));
		this.fontChoice = prefs.forUser("articleFontChoice", FontOption.BLANCO.id);
		this.justify = prefs.forUser("articleJustify", false);

		this.themeIsDark = prefs.forUser("appThemeDark", false);
		this.premiumReader = premiumReader;
		this.theme = theme;
		this.systemDarkTheme = systemDarkTheme;
		themeAnalytics = new ThemeAnalytics(pocket);

		DEFAULT_MARGIN = context.getResources().getInteger(R.integer.article_default_margin);
		MAX_MARGIN = context.getResources().getInteger(R.integer.article_max_margin);
		MIN_MARGIN = context.getResources().getInteger(R.integer.article_min_margin);
		FONT_SIZES = context.getResources().getIntArray(R.array.article_font_sizes);
		FONT_SIZE_RANGE = new Range(FONT_SIZES[0], FONT_SIZES[FONT_SIZES.length-1]);
		LINE_HEIGHTS = context.getResources().getIntArray(R.array.article_line_heights);
		LINE_HEIGHT_RANGE = new Range(LINE_HEIGHTS[0], LINE_HEIGHTS[LINE_HEIGHTS.length-1]);
		MARGINS = context.getResources().getIntArray(R.array.article_margins);
		MARGIN_RANGE = new Range(MARGINS[0], MARGINS[MARGINS.length-1]);
		
		if (versioning.upgraded(7,3,0,0)) {
			// Previous to the Premium Reader features we had a serif / non serif font setting.
			// Here we default to our free non serif font (Graphik) if the user had that switched on
			if (!appPrefs.deprecateUserBoolean("articleSerif", true)) {
				fontChoice.set(DisplaySettingsManager.FontOption.GRAPHIK.id);
			}
		}
    }

	public enum FontOption {

		BLANCO(0, R.string.blanco, null, false, 1.0f, "blanco"),
		GRAPHIK(1, R.string.graphik, null, false, 0.9f, "graphik"),
		IDEAL_SANS(2, R.string.idealsans, PremiumFonts.Font.IDEAL_SANS_BOOK, true, 0.85f, "ideal_sans"),
		INTER(3, R.string.inter, PremiumFonts.Font.INTER_REGULAR, true, 0.9f, "inter"),
		IBM_PLEX_SANS(4, R.string.plex_sans, PremiumFonts.Font.IBM_PLEX_SANS_REGULAR, true, 0.95f, "plex_sans"),
		SENTINEL(5, R.string.sentinel, PremiumFonts.Font.SENTINEL_BOOK, true, 0.95f, "sentinel"),
		TIEMPOS(6, R.string.tiempos, PremiumFonts.Font.TIEMPOS_REGULAR, true, 0.9f, "tiempos"),
		VOLLKORN(7, R.string.vollkorn, PremiumFonts.Font.VOLLKORN_REGULAR, true, 0.95f, "vollkorn"),
		WHITNEY(8, R.string.whitney, PremiumFonts.Font.WHITNEY_BOOK, true, 0.85f, "whitney"),
		ZILLA_SLAB(9, R.string.zillaslab, PremiumFonts.Font.ZILLA_SLAB_REGULAR, true, 0.95f, "zilla_slab");

		public final int id;
		public final int displayName;
		public final PremiumFonts.Font previewFont;
		public final boolean isPremium;
		public final float sizeModifier;
		public final String analyticsName;

		FontOption(int id, @StringRes int displayName, PremiumFonts.Font font, boolean isPremium, float sizeModifier, String analyticsName) {
			this.id = id;
			this.displayName = displayName;
			this.previewFont = font;
			this.isPremium = isPremium;
			this.sizeModifier = sizeModifier;
			this.analyticsName = analyticsName;
		}

		public Typeface getTypeface(Context context) {
			if (id == 0) {
				return Fonts.get(context, Fonts.Font.BLANCO_REGULAR);
			} else if (id == 1) {
				return Fonts.get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM);
			} else {
				return App.from(context).premiumFonts().get(previewFont);
			}
		}

	}

	/* TODO
	 * Default sizes
	 * 
	 * matches itself / what we want it to be for that screen size
	 * S2: 18 / 18
	 * N5: 18 / 18
	 * Note3: 16 / 18 (maybe don't jump from 19 to 22)
	 * Kindle Fire: 21 / 22 (auto margin not working on kindle fire)
	 * CrossOver: 23ish / maybe 20/21  (need more ranges under 30)
	 * N7(gen1): 20 / 21ish
	 * N10: 20ish / 22
	 * 
	 * auto margin maybe wrong min margin on 7" devices
	 * 
	 * remember syncing theme change 
	 * 
	 */
	
	public int getFontSize() {
		return fontSize.get();
	}

	public int getLineHeight() {
		return lineHeight.get();
	}

	public int getMargin() {
		return margin.get();
	}
	
	public int getCurrentFontChoice() {
		return fontChoice.get();
	}

	public FontOption getCurrentFont() {
		int choice = getCurrentFontChoice();
		for (FontOption font : FontOption.values()) {
			if (choice == font.id) {
				return font;
			}
		}
		return FontOption.BLANCO;
	}

	public void revertFontPreview() {
		if (getCurrentFont().isPremium && !premiumReader.isEnabled()) {

			// set the font back to default
			setFont(FontOption.BLANCO.id);
		}
	}

	public BooleanPreference justify() {
		return justify;
	}

	public boolean isJustified() {
		return justify().get();
	}

	private static boolean isPrefAtMax(IntPreference pref, Range range) {
		return pref.get() >= range.max;
	}

	private static boolean isPrefAtMin(IntPreference pref, Range range) {
		return pref.get() <= range.min;
	}

	public boolean isFontSizeAtMax() {
		return isPrefAtMax(fontSize, FONT_SIZE_RANGE);
	}
	
	public boolean isFontSizeAtMin() {
		return isPrefAtMin(fontSize, FONT_SIZE_RANGE);
	}

	public boolean isLineHeightAtMax() {
		return isPrefAtMax(lineHeight, LINE_HEIGHT_RANGE);
	}

	public boolean isLineHeightAtMin() {
		return isPrefAtMin(lineHeight, LINE_HEIGHT_RANGE);
	}

	public boolean isMarginAtMax() {
		return isPrefAtMax(margin, MARGIN_RANGE);
	}

	public boolean isMarginAtMin() {
		return isPrefAtMin(margin, MARGIN_RANGE);
	}
	
	public int getTheme() {
		return theme.get();
	}

	private interface OnPrefIncrementedListener {
		void onPrefIncremented(int to);
	}

	private static boolean incrementPref(IntPreference pref, int[] values, Range range, OnPrefIncrementedListener listener) {
		// Find the next size up
		int from = pref.get();
		if (from == range.max) {
			return false; // No change, already at maximum
		}
		int to = from;
		for (int value : values) {
			if (value > from) {
				to = value;
				break;
			}
		}

		// Limit to range
		to = range.limit(to);

		if (to == from) {
			return false; // No change
		}

		listener.onPrefIncremented(to);
		return true;
	}

	private static boolean decrementPref(IntPreference pref, int[] values, Range range, OnPrefIncrementedListener listener) {
		// Find the next size down
		int from = pref.get();
		if (from == range.min) {
			return false; // No change, already at maximum
		}
		int to = from;
		int len = values.length;
		for (int i = len - 1; i >= 0; i--) {
			int value = values[i];
			if (value < from) {
				to = value;
				break;
			}
		}

		// Limit to range
		to = range.limit(to);

		if (to == from) {
			return false; // No change
		}


		listener.onPrefIncremented(to);
		return true;
	}

	/**
	 * Increases the users preferred font size up one increment in the list of {@link #FONT_SIZES}
	 */
	public void incrementFontSize() {
		incrementPref(fontSize, FONT_SIZES, FONT_SIZE_RANGE, to -> setFontSize(to));
	}
	
	/**
	 * Decreases the users preferred font size down one increment in the list of {@link #FONT_SIZES}
	 */
	public void decrementFontSize() {
		decrementPref(fontSize, FONT_SIZES, FONT_SIZE_RANGE, to -> setFontSize(to));
	}

	/**
	 * Increases the users preferred line height up one increment in the list of {@link #LINE_HEIGHTS}
	 */
	public void incrementLineHeight() {
		incrementPref(lineHeight, LINE_HEIGHTS, LINE_HEIGHT_RANGE, to -> setLineHeight(to));
	}

	/**
	 * Decreases the users preferred line height down one increment in the list of {@link #LINE_HEIGHTS}
	 */
	public void decrementLineHeight() {
		decrementPref(lineHeight, LINE_HEIGHTS, LINE_HEIGHT_RANGE, to -> setLineHeight(to));
	}

	/**
	 * Increases the users preferred margin setting up one increment in the list of {@link #MARGINS}
	 */
	public void incrementMargin() {
		incrementPref(margin, MARGINS, MARGIN_RANGE, to -> setMargin(to));
	}

	/**
	 * Decreases the users preferred margin setting down one increment in the list of {@link #MARGINS}
	 */
	public void decrementMargin() {
		decrementPref(margin, MARGINS, MARGIN_RANGE, to -> setMargin(to));
	}
	
	/** 
	 * @see #decrementFontSize()
	 * @see #incrementFontSize()
	 */
	private void setFontSize(int size) {
		if (size == getFontSize()) {
			return;
		}
		fontSize.set(size);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onFontSizeChanged(size, isFontSizeAtMin(), isFontSizeAtMax());
		}
	}

	/**
	 * @see #decrementLineHeight() ()
	 * @see #incrementLineHeight() ()
	 */
	private void setLineHeight(int value) {
		if (value == getLineHeight()) {
			return;
		}
		lineHeight.set(value);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onLineHeightChanged(value, isLineHeightAtMin(), isLineHeightAtMax());
		}
	}

	/**
	 * Internal use only
	 *
	 * @see #decrementMargin() ()
	 * @see #incrementMargin() ()
	 */
	private void setMargin(int value) {
		if (value == getMargin()) {
			return;
		}
		margin.set(value);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onMarginChanged(value, isMarginAtMin(), isMarginAtMax());
		}
	}

	public void setFont(int value) {
		if (value == getCurrentFontChoice()) {
			return;
		}
		fontChoice.set(value);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onFontChanged(value);
		}
	}
	
	/**
	 * Set the users preferred theme.
	 */
	public void setTheme(View analyticsCtx, int theme) {
		if (this.theme.get() == theme && !systemDarkTheme.isOn()) {
			return; // Already set to this value
		}
		
		this.theme.set(theme);
		themeIsDark.set(theme == Theme.DARK);
		if (analyticsCtx != null) {
			systemDarkTheme.turnOff(analyticsCtx);
		} else {
			systemDarkTheme.turnOff(this.context);
		}
		theme = this.theme.get(); // Let the theme handle the black/dark setting
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onThemeChanged(theme);
		}
		if (analyticsCtx != null) {
			themeAnalytics.track(analyticsCtx, theme);
		}
	}
	
	public void setSystemDarkThemeOn(View context) {
		if (context != null) {
			systemDarkTheme.turnOn(context);
		} else {
			systemDarkTheme.turnOn(this.context);
		}
		themeIsDark.set(theme.get() == Theme.DARK);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onThemeChanged(theme.get());
		}
	}
	
	public void setBrightness(float hardware) {
		if (Brightness.getBrightness() == hardware) {
			return;
		}
		
		Brightness.setBrightness(hardware);
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onBrightnessChanged(hardware);
		}
	}
	
	public void onJustificationSettingChanged() {
		for (OnDisplaySettingsChangedListener listener : sListeners) {
			listener.onTextAlignChanged(isJustified());
		}
	}
	
	/**
	 * Returns the value to use for left and right margins. <b>This value will be in web px, not in screen pixels</b>.
	 * <p>
	 * For non-premium users, the view's width is used in the calculation so if the width changes, this value will need to be recalculated.
	 * <p>
	 * In the case of premium users, the margin will be set to their {@link AppPrefs#ARTICLE_MARGIN} setting.
	 * @return 
	 */
	public int getHorizontalMargin(WebView webview) {

		// if the user is premium, their margin setting takes precedence
		if (premiumReader.isEnabled()) {
			return margin.get();
		}

		return DEFAULT_MARGIN;
	}
	
	/**
	 * @return The maximum/ideal width of images to load in web px.
	 */
	public int getImageWidth(Activity activity) {
		int longestWidth = ScreenWidth.get(activity).getLongest(false);

		if (FormFactor.isPhone()) {
			return longestWidth; // Full bleed in landscape is maximum
		}

		// On tablets, the maximum width is the largest the column will ever be in landscape.
		int margins = FormFactor.dpToPx(MIN_MARGIN * 2); // Convert web margins to what they are on screen
		return longestWidth - margins;
	}

	/**
	 * Listen to changes made to display settings. Be sure to avoid memory leaks and remove your listener when 
	 * it is no longer needed.
	 * <p>
	 * To easily recheck the settings at any time, see {@link #invokeListener(OnDisplaySettingsChangedListener, boolean, boolean, boolean, boolean, boolean, boolean)}.
	 * 
	 * @param listener
	 * @see #removeListener(OnDisplaySettingsChangedListener)
	 * @see #invokeListener(OnDisplaySettingsChangedListener, boolean, boolean, boolean, boolean, boolean, boolean)
	 */
	public void addListener(OnDisplaySettingsChangedListener listener) {
		if (!sListeners.contains(listener)) {
			sListeners.add(listener);
		} else {
			Logs.throwIfNotProduction("warning: duplicate listener added");
		}
	}
	
	public void removeListener(OnDisplaySettingsChangedListener listener) {
		sListeners.remove(listener);
	}
	
	/**
	 * A helper method for invoking the methods on the listener with the current settings. This can be 
	 * used to update or set the initial values for your UI based on the current display settings.
	 * <p>
	 * If the boolean flag for a setting is set, it will invoke the callback. For example, if font is set,
	 * {@link OnDisplaySettingsChangedListener#onFontChanged(int)} will be invoked with the current Font.
	 * value.
	 * 
	 * @param listener
	 * @param font
	 * @param size
	 * @param align
	 * @param theme
	 */
	public void invokeListener(OnDisplaySettingsChangedListener listener, boolean font, boolean size, boolean lineHeight, boolean margin, boolean align, boolean theme) {
		if (font) {
			listener.onFontChanged(getCurrentFontChoice());
		}
		if (size) {
			listener.onFontSizeChanged(getFontSize(), isFontSizeAtMin(), isFontSizeAtMax());
		}
		if (lineHeight) {
			listener.onLineHeightChanged(getLineHeight(), isFontSizeAtMin(), isFontSizeAtMax());
		}
		if (margin) {
			listener.onMarginChanged(getMargin(), isFontSizeAtMin(), isFontSizeAtMax());
		}
		if (align) {
			listener.onTextAlignChanged(isJustified());
		}
		if (theme) {
			listener.onThemeChanged(getTheme());
		}
	}
	
	public interface OnDisplaySettingsChangedListener {
		void onFontChanged(int fontChoice);
		void onFontSizeChanged(int size, boolean isMinimum, boolean isMaximum);
		void onLineHeightChanged(int value, boolean isMinimum, boolean isMaximum);
		void onMarginChanged(int value, boolean isMinimum, boolean isMaximum);
		void onTextAlignChanged(boolean justify);
		void onThemeChanged(int theme);
		void onBrightnessChanged(float brightness);
	}

	/**
	 * Theme analytics originally defined here: https://docs.google.com/spreadsheets/d/1kHwHEK1YVLZfzoMGw7Nlh1mp1N-ZZU1ugyMaraCLznA/edit#gid=0
	 */
	private class ThemeAnalytics {

		private final Pocket pocket;

		private ThemeAnalytics(Pocket pocket) {
			this.pocket = pocket;
		}

		public void track(View ctx, int theme) {

			Interaction it = Interaction.on(ctx);

			CxtEvent event = null;

			switch (theme) {
				case Theme.LIGHT:
					event = CxtEvent.LIGHT;
					break;
				case Theme.DARK:
					event = CxtEvent.DARK;
					break;
			}

			Pv.Builder pv = pocket.spec().actions().pv()
					.section(CxtSection.THEME)
					.event(event)
					.event_type(9)
					.time(it.time)
					.context(it.context);

			pocket.sync(null, pv.build());
		}
	}

}
