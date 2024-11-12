package com.pocket.ui.view.menu;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;
import com.pocket.ui.view.themed.ThemedConstraintLayout;
import com.squareup.phrase.Phrase;

import androidx.annotation.StringRes;

/**
 * Pocket's Display/Text Settings menu.
 * Has built in side and bottom paddings.
 */
public class DisplaySettingsView extends ThemedConstraintLayout {
	
	private final Binder binder = new Binder();
	private ThemeToggle themeToggle;
	private SeekBar brightness;
	private View brightnessUp;
	private View brightnessDown;
	private SettingIncrementor textSizeNonPremium;
	private View textSizeDividerNonPremium;
	private TextView changeFont;
	private View fontsChevron;

	private View premiumSettings;
	private SettingIncrementor textSizePremium;
	private SettingIncrementor lineHeight;
	private SettingIncrementor margin;

	public ThemedConstraintLayout premiumUpgrade;

	private SeekBar.OnSeekBarChangeListener brightnessListener;
	
	public DisplaySettingsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	public DisplaySettingsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public DisplaySettingsView(Context context) {
		super(context);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_display_settings, this, true);
		themeToggle = findViewById(R.id.settings_theme);
		brightnessDown = findViewById(R.id.settings_brightness_down);
		brightness = findViewById(R.id.settings_brightness_slider);
		brightnessUp = findViewById(R.id.settings_brightness_up);
		textSizeNonPremium = findViewById(R.id.settings_size_non_premium);
		textSizeDividerNonPremium = findViewById(R.id.settings_text_size_divider_non_premium);
		changeFont = findViewById(R.id.settings_change_font);
		fontsChevron = findViewById(R.id.fonts_chevron);
		premiumSettings = findViewById(R.id.premium_settings);
		textSizePremium = findViewById(R.id.settings_size);
		lineHeight = findViewById(R.id.settings_line_height);
		margin = findViewById(R.id.settings_margin);
		premiumUpgrade = findViewById(R.id.premium_upgrade);
		premiumUpgrade.setUiEntityType(Type.BUTTON);
		setPadding(0,0,0, getResources().getDimensionPixelSize(R.dimen.pkt_space_md));
		
		brightness.setMax(100);
		brightnessDown.setOnClickListener(v -> incrementBrightness(-0.1f));
		brightnessUp.setOnClickListener(v -> incrementBrightness(0.1f));
		brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				float fprogress = progress / (float) seekBar.getMax();
				brightnessUp.setEnabled(fprogress < 1);
				brightnessDown.setEnabled(fprogress > 0);
				if (brightnessListener != null) brightnessListener.onProgressChanged(seekBar, progress, fromUser);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				if (brightnessListener != null) brightnessListener.onStartTrackingTouch(seekBar);
			}
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (brightnessListener != null) brightnessListener.onStopTrackingTouch(seekBar);
			}
		});

		bind().clear();
		setUiEntityIdentifier("text_settings_overflow");
		engageable.setUiEntityType(Type.MENU);
	}
	
	private void incrementBrightness(float amt) {
		amt += brightness.getProgress() / (float) brightness.getMax();
		int progress = (int) (amt * brightness.getMax());
		brightness.setProgress(Math.min(Math.max(progress, 0), brightness.getMax()));
	}
	
	public Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		public Binder clear() {
			theme().clear();
			brightnessListener(null);
			brightness(0);
			textSizePremium.bind().clear().icon(R.drawable.ic_pkt_text_size_solid).label(R.string.text_setting_text_size);
			textSizeNonPremium.bind().clear().icon(R.drawable.ic_pkt_text_size_solid).label(R.string.text_setting_text_size);
			lineHeight.bind().clear().icon(R.drawable.ic_pkt_letter_spacing_line).label(R.string.text_setting_line_height);
			margin.bind().clear().icon(R.drawable.ic_pkt_margins_line).label(R.string.text_setting_margin);
			fontChangeText(null);
			fontChangeClick(null);
			fontChangeTypeface(Fonts.get(getContext(), Fonts.Font.GRAPHIK_LCG_MEDIUM));
			premiumUpsellVisible(true);
			premiumSettingsVisible(false);
			fontsLoading(false);
			return this;
		}
		
		public ThemeToggle.Binder theme() {
			return themeToggle.bind();
		}
		
		/**
		 * @param value progress of seekbar 0-1
		 */
		public Binder brightness(float value) {
			brightness.setProgress((int) (value * brightness.getMax()));
			return this;
		}
		
		public Binder brightnessListener(SeekBar.OnSeekBarChangeListener listener) {
			brightnessListener = listener;
			return this;
		}
		
		public Binder fontSizeUpEnabled(boolean value) {
			textSizePremium.bind().upEnabled(value);
			textSizeNonPremium.bind().upEnabled(value);
			return this;
		}
		
		public Binder fontSizeDownEnabled(boolean value) {
			textSizePremium.bind().downEnabled(value);
			textSizeNonPremium.bind().downEnabled(value);
			return this;
		}
		
		public Binder fontSizeUpClick(OnClickListener listener) {
			textSizePremium.bind().upListener(listener);
			textSizeNonPremium.bind().upListener(listener);
			return this;
		}
		
		public Binder fontSizeDownClick(OnClickListener listener) {
			textSizePremium.bind().downListener(listener);
			textSizeNonPremium.bind().downListener(listener);
			return this;
		}

		public Binder lineHeightUpEnabled(boolean value) {
			lineHeight.bind().upEnabled(value);
			return this;
		}

		public Binder lineHeightDownEnabled(boolean value) {
			lineHeight.bind().downEnabled(value);
			return this;
		}

		public Binder lineHeightUpClick(OnClickListener listener) {
			lineHeight.bind().upListener(listener);
			return this;
		}

		public Binder lineHeightDownClick(OnClickListener listener) {
			lineHeight.bind().downListener(listener);
			return this;
		}

		public Binder marginUpEnabled(boolean value) {
			margin.bind().upEnabled(value);
			return this;
		}

		public Binder marginDownEnabled(boolean value) {
			margin.bind().downEnabled(value);
			return this;
		}

		public Binder marginUpClick(OnClickListener listener) {
			margin.bind().upListener(listener);
			return this;
		}

		public Binder marginDownClick(OnClickListener listener) {
			margin.bind().downListener(listener);
			return this;
		}

		public Binder premiumUpgradeClick(OnClickListener listener) {
			premiumUpgrade.setOnClickListener(listener);
			return this;
		}

		public Binder fontChangeText(CharSequence typeface) {
			changeFont.setText(typeface);
			if (typeface != null) {
				changeFont.setContentDescription(Phrase.from(getResources(), R.string.current_typeface)
						.put("typeface", typeface)
						.format());
			} else {
				changeFont.setContentDescription(null);
			}
			return this;
		}

		public Binder fontChangeText(@StringRes int text) {
			return fontChangeText(getResources().getText(text));
		}

		public Binder fontChangeTypeface(Typeface typeface) {
			if (typeface != null) {
				changeFont.setTypeface(typeface);
			}
			return this;
		}

		public Binder fontChangeClick(OnClickListener listener) {
			changeFont.setOnClickListener(listener);
			return this;
		}

		public Binder fontsLoading(boolean show) {
			changeFont.setEnabled(!show);
			fontsChevron.setEnabled(!show);
			return this;
		}

		public Binder premiumUpsellVisible(boolean visible) {
			premiumUpgrade.setVisibility(visible ? View.VISIBLE : View.GONE);
			return this;
		}

		public Binder premiumSettingsVisible(boolean visible) {
			premiumSettings.setVisibility(visible ? View.VISIBLE : View.GONE);
			textSizeNonPremium.setVisibility(visible ? View.GONE : View.VISIBLE);
			textSizeDividerNonPremium.setVisibility(visible ? View.GONE : View.VISIBLE);
			return this;
		}
	}
}
