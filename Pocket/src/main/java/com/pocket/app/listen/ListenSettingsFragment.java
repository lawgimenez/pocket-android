package com.pocket.app.listen;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.ActionPreference;
import com.pocket.app.settings.view.preferences.Preference;
import com.pocket.app.settings.view.preferences.PreferenceViews;
import com.pocket.app.settings.view.preferences.TtsEnginePreference;
import com.pocket.sdk.api.generated.enums.CxtEvent;
import com.pocket.sdk.api.generated.enums.CxtSection;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.tts.Controls;
import com.pocket.sdk.tts.Listen;
import com.pocket.sdk.tts.ListenState;
import com.pocket.sdk.tts.NamedVoice;
import com.pocket.sdk.tts.VoiceCompat;
import com.pocket.sdk2.analytics.context.Interaction;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.fragment.FragmentUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.reactivex.disposables.CompositeDisposable;

import static com.pocket.util.android.fragment.FragmentUtil.FragmentLaunchMode;

public final class ListenSettingsFragment extends AbsPrefsFragment {
	
	private final CompositeDisposable disposables = new CompositeDisposable();
	
	private Controls controls;
	private ActionPreference languagePreference;
	private ActionPreference voicePreference;
	private VoiceOptions languageOptions;
	private VoiceOptions voiceOptions;
	
	public static FragmentLaunchMode getLaunchMode(Activity activity) {
		if (FormFactor.showSecondaryScreensInDialogs(activity)) {
			return FragmentLaunchMode.DIALOG;
		} else {
			return FragmentLaunchMode.ACTIVITY;
		}
	}
	
	public static ListenSettingsFragment newInstance() {
		return new ListenSettingsFragment();
	}
	
	public static void show(FragmentActivity activity) {
		if (getLaunchMode(activity) == FragmentLaunchMode.DIALOG) {
			FragmentUtil.addFragmentAsDialog(newInstance(), activity);
		} else {
			ListenSettingsActivity.startActivity(activity);
		}
	}
	
	@Override protected int getTitle() {
		return R.string.mu_settings;
	}
	
	@Override protected void createPrefs(ArrayList<Preference> prefs) {
		controls = app().listen().trackedControls(getViewRoot(), null);
		var streamingEngineEnabled = app().listen().isStreamingEngineEnabled();
		createOptions();
		
		if (streamingEngineEnabled) {
			prefs.add(PreferenceViews.newToggleSwitchBuilder(
					this, app().prefs().LISTEN_USE_STREAMING_VOICE, R.string.listen_settings_use_listen_endpoint)
					.setSummaryDefaultUnchecked(R.string.listen_settings_network_voice_warning)
					.build());
		}
		
		prefs.add(new TtsEnginePreference(
				this,
				getString(R.string.setting_tts_label),
				() -> !streamingEngineEnabled || !app().prefs().LISTEN_USE_STREAMING_VOICE.get(), 
				null
		));
		
		languagePreference = buildPicker(R.string.lb_tts_language, languageOptions);
		prefs.add(languagePreference);
		
		voicePreference = buildPicker(R.string.lb_tts_voice, voiceOptions);
		prefs.add(voicePreference);
		
		prefs.add(PreferenceViews.newToggleSwitchBuilder(
				this, app().prefs().ARTICLE_TTS_AUTO_PLAY, R.string.listen_settings_auto_play)
				.setOnChangeListener(nowEnabled -> track(CxtSection.AUTOPLAY_TOGGLE, nowEnabled))
				.build());
		
		prefs.add(PreferenceViews.newToggleSwitchBuilder(
				this, app().prefs().LISTEN_AUTO_ARCHIVE, R.string.listen_settings_auto_archive)
				.setOnChangeListener(nowEnabled -> track(CxtSection.AUTOARCHIVE_TOGGLE, nowEnabled))
				.build());
	}
	
	private void createOptions() {
		languageOptions = new VoiceOptions<>((VoiceOptions.OnSelect<Locale>) selected -> {
			final Set<VoiceCompat.Voice> filtered = filterVoices(app().listen().state().voices, selected);
			controls.setVoice(VoiceCompat.findBestMatch(selected, null, filtered));
		});
		
		voiceOptions = new VoiceOptions<>((VoiceOptions.OnSelect<VoiceCompat.Voice>) selected -> {
			if (selected.isNetworkConnectionRequired()) {
				warnNetworkVoice();
			}
			controls.setVoice(selected);
		});
	}
	
	private ActionPreference buildPicker(int titleRes, VoiceOptions options) {
		return PreferenceViews.newActionBuilder(this, titleRes)
				.setSummaryDisabled(R.string.setting_auto_dark_theme_threshold_automatic)
				.setEnabledWhen(() -> {
					final ListenState state = app().listen().state();
					return state.supportedFeatures.contains(ListenState.Feature.MULTIPLE_VOICES)
							&& state.voice != null;
				})
				.setOnClickListener(() -> new AlertDialog.Builder(getContext())
						.setTitle(titleRes)
						.setSingleChoiceItems(options.getLabels(), options.getSelected(), (dialog, which) -> {
							options.onSelected(which);
							dialog.dismiss();
						})
						.show())
				.build();
	}
	
	@Override public void onStart() {
		super.onStart();
		
		final CharSequence summaryAuto = getResources().getText(R.string.listen_settings_picker_summary_unavailable);
		final CharSequence summaryLoading = getResources().getText(R.string.dg_loading);
		final Listen listen = app().listen();
		
		disposables.add(listen.states().startWith(listen.state())
				.subscribe(state -> {
					if (state.voice == null) {
						// Switching to the TTS player, but haven't loaded the selected voice yet.
						voicePreference.updateSummary(ActionPreference.SUMMARY_UNAVAILABLE, summaryLoading);
						languagePreference.updateSummary(ActionPreference.SUMMARY_UNAVAILABLE, summaryLoading);
						
					} else if (!state.supportedFeatures.contains(ListenState.Feature.MULTIPLE_VOICES)) {
						// Using the streaming player, which doesn't let you choose voices.
						voicePreference.updateSummary(ActionPreference.SUMMARY_UNAVAILABLE, summaryAuto);
						languagePreference.updateSummary(ActionPreference.SUMMARY_UNAVAILABLE, summaryAuto);
						
					} else {
						// Voices should be loaded now.
						bindLanguages(state.voice, state.voices);
						bindVoices(state.voice, state.voices);
					}
					
					prefAdapter.notifyDataSetChanged();
				}));
		
		final Interaction it = Interaction.on(getContext());
		app().pocket().sync(null, app().pocket().spec().actions().pv()
				.section(CxtSection.OPEN)
				.view(getActionViewName())
				.context(it.context)
				.time(it.time)
				.build());
	}
	
	@Override public void onStop() {
		super.onStop();
		disposables.clear();
	}
	
	@Override protected View getBannerView() {
		return null;
	}
	
	@Override public CxtView getActionViewName() {
		return CxtView.LISTEN_SETTINGS;
	}
	
	@Nullable @Override public UiEntityIdentifier getScreenIdentifier() {
		return UiEntityIdentifier.LISTEN_SETTINGS;
	}
	
	private void bindLanguages(@NonNull ListenState.Voice voice, Set<VoiceCompat.Voice> voices) {
		languagePreference.updateSummary(
				ActionPreference.SUMMARY_DEFAULT_OR_UNCHECKED, voice.getLocale().getDisplayName());
		
		Set<Locale> locales = new HashSet<>();
		for (VoiceCompat.Voice v : voices) {
			locales.add(v.getLocale());
		}
		List<Option<Locale>> langOptions = new ArrayList<>();
		for (Locale locale : locales) {
			langOptions.add(new Option<>(locale.getDisplayName(), locale, 0));
		}
		Collections.sort(langOptions, (o1, o2) -> o1.label.toString().compareTo(o2.label.toString()));
		languageOptions.setOptions(langOptions);
		if (voice instanceof VoiceCompat.Voice) {
			int i = 0;
			for (Option<Locale> o : langOptions) {
				if (voice.getLocale().equals(o.value)) {
					languageOptions.setSelected(i);
					return;
				}
				i++;
			}
		}
		// If not found:
		languageOptions.setSelected(0);
	}
	
	private void bindVoices(@NonNull ListenState.Voice voice, Set<VoiceCompat.Voice> voices) {
		Set<VoiceCompat.Voice> filtered = filterVoices(voices, voice.getLocale());
		List<NamedVoice> named = NamedVoice.name(filtered, getContext());
		Collections.sort(named, (v1, v2) -> Integer.compare(v1.sortOrder, v2.sortOrder));
		List<Option<VoiceCompat.Voice>> voiceOptions = new ArrayList<>();
		int selected = 0;
		for (NamedVoice v : named) {
			CharSequence label = v.name;
			label = addVoiceIcons(label, v);
			voiceOptions.add(new Option<>(label, v.voice, 0));
			if (v.voice.equals(voice)) {
				selected = named.indexOf(v);
			}
		}
		this.voiceOptions.setOptions(voiceOptions);
		this.voiceOptions.setSelected(selected);
		voicePreference.updateSummary(
				ActionPreference.SUMMARY_DEFAULT_OR_UNCHECKED, this.voiceOptions.getLabels()[selected]);
	}
	
	private CharSequence addVoiceIcons(CharSequence label, NamedVoice voice) {
		if (voice.voice.isNetworkConnectionRequired()) {
			SpannableStringBuilder spannable = new SpannableStringBuilder(label);

			spannable.append("   ");
			int spanStart = spannable.length();
			spannable.append(getResources().getString(R.string.tts_network));

			Drawable icon = getContext().getResources().getDrawable(R.drawable.ic_cloud_black_24dp);
			icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());

			spannable.setSpan(new ImageSpan(icon), spanStart, spannable.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

			label = spannable;
		}
		return label;
	}
	
	private void warnNetworkVoice() {
		if (!app().prefs().ARTICLE_TTS_WARNED_NETWORK_VOICES.get()) {
			app().prefs().ARTICLE_TTS_WARNED_NETWORK_VOICES.set(true);
			new AlertDialog.Builder(getContext())
					.setTitle(R.string.tts_network_voice_t)
					.setIcon(R.drawable.ic_cloud_black_24dp)
					.setMessage(R.string.tts_network_voice)
					.setPositiveButton(R.string.ac_ok, null)
					.show();
		}
	}
	
	private void track(CxtSection section, boolean enabled) {
		track(section, enabled ? CxtEvent.ENABLED : CxtEvent.DISABLED);
	}
	
	private void track(CxtSection section, CxtEvent event) {
		final Interaction it = Interaction.on(getContext());
		app().pocket().sync(null, app().pocket().spec().actions().pv()
				.view(getActionViewName())
				.section(section)
				.event(event)
				.context(it.context)
				.time(it.time)
				.build());
	}
	
	private static Set<VoiceCompat.Voice> filterVoices(Set<VoiceCompat.Voice> voices, Locale include) {
		Set<VoiceCompat.Voice> filtered = new HashSet<>(voices);
		Iterator<VoiceCompat.Voice> it = filtered.iterator();
		while (it.hasNext()) {
			if (!include.equals(it.next().getLocale())) {
				it.remove();
			}
		}
		return filtered;
	}
	
	private static class VoiceOptions<T> {
		
		private final OnSelect<T> onSelect;
		private final List<Option<T>> options = new ArrayList<>();
		private int selected;
		
		VoiceOptions(OnSelect<T> onSelect) {
			this.onSelect = onSelect;
		}
		
		void setOptions(List<Option<T>> options) {
			this.options.clear();
			this.options.addAll(options);
		}
		
		void setSelected(int index) {
			selected = index;
		}
		
		int getSelected() {
			return selected;
		}
		
		void onSelected(int which) {
			setSelected(which);
			onSelect.onSelected(options.get(which).value);
		}
		
		CharSequence[] getLabels() {
			CharSequence[] o = new CharSequence[options.size()];
			int i = 0;
			for (Option option : options) {
				o[i++] = option.label;
			}
			return o;
		}
		
		interface OnSelect<T> {
			void onSelected(T selected);
		}
	}
	
	private static class Option<T> {
		public final CharSequence label;
		public final T value;
		public final int icon;
		
		Option(CharSequence label, T value, int icon) {
			this.label = label;
			this.value = value;
			this.icon = icon;
		}
	}
}
