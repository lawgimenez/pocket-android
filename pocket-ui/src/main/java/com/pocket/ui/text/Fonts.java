package com.pocket.ui.text;

import android.content.Context;
import android.graphics.Typeface;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Access to custom typefaces and fonts used by the pocket ui components.
 * <p>
 * At runtime, you can get these fonts with {@link #get(Context, Font)}.
 * <p>
 * In xml, use a {@link com.pocket.ui.view.themed.ThemedTextView} or {@link com.pocket.ui.view.themed.ThemedEditText} and the app:typeface attribute.
 * <p>
 * In a WebView, include the css file in the assets directory with the file name of {@link #GRAPHIK_CSS}
 * and then fonts can be referenced in css styles by the font family names declared in that file.
 * <p>
 * Once loaded, typefaces are cached in memory. If we ever find a use case for clearing the cache, we could
 * provide a method to.
 * <p>
 * This class is intended for use only on the UI-Thread. (If we find a reason to enforce this, we can add later)
 * <p>
 * Dev Note: Why not use the font tools in support library? Our main Pocket app needs the fonts available for use in css and html
 * as well. While there is file:///android_res/ available, in practice it appeared pretty unreliable and could break
 * based on changing package names, using modules, build configs and could be affected by different WebView implementations
 * and versions. For example: https://bugs.chromium.org/p/chromium/issues/detail?id=599869 So having them in assets allows
 * any apps that use this module to access these fonts in code, xml and in html/css without having to duplicate large font files.
 * We also experienced bugs with ResourcesCompat.getFont on some devices.
 */
public class Fonts {
	
	/** Filename of the graphik-lcg font family css file within the assets directory. */
	public static final String GRAPHIK_CSS = "graphik-lcg_mobile.css";
	
	public enum Font {
		GRAPHIK_LCG_BOLD(10, "graphik_lcg_bold_no_leading.otf"),
		GRAPHIK_LCG_MEDIUM(1, "graphik_lcg_medium_no_leading.otf"),
		GRAPHIK_LCG_MEDIUM_ITALIC(2, "graphik_lcg_medium_italic_no_leading.otf"),
		GRAPHIK_LCG_REGULAR(3, "graphik_lcg_regular_no_leading.otf"),
		GRAPHIK_LCG_REGULAR_ITALIC(4, "graphik_lcg_regular_italic_no_leading.otf"),

		BLANCO_REGULAR(5, "blanco_osf_regular.otf"),
		BLANCO_BOLD(6, "blanco_osf_bold.otf"),
		BLANCO_ITALIC(7, "blanco_osf_italic.otf"),
		BLANCO_BOLD_ITALIC(8, "blanco_osf_bold_italic.otf"),

		DOYLE_MEDIUM(9, "doyle_medium.otf"),

		/**
		 * Pocket icons as a typeface.  This allows certain images to be easily added to text Strings while still
		 * properly scaling with the user's text size settings.
		 */
		ICONS(10, "pocket_icons.ttf");
		
		private final int attrValue;
		final String filename;
		
		/**
		 * @param attrValue This font's "value" within the {@link com.pocket.ui.R.attr#typeface} enum attribute.
		 * @param filename The filename of the font in the assets folder.
		 */
		Font(int attrValue, String filename) {
			this.attrValue = attrValue;
			this.filename = filename;
		}
	}
	
	private static Map<Font, Typeface> cache = new HashMap<>();
	
	/**
	 * Get a typeface by attribute.
	 * @param attrValue The "value" of a {@link com.pocket.ui.R.attr#typeface} to load
	 * @return The typeface or null if none matched that value
	 */
	public static @Nullable Typeface get(Context context, int attrValue) {
		for (Font font : Font.values()) {
			if (font.attrValue == attrValue) {
				return get(context, font);
			}
		}
		return null;
	}
	
	/**
	 * Get a typeface by enum.
	 */
	public static Typeface get(Context context, Font font) {
		Typeface typeFace = cache.get(font);
		if (typeFace == null) {
			typeFace = Typeface.createFromAsset(context.getAssets(), font.filename);
			cache.put(font, typeFace);
		}
		return typeFace;
	}
}
