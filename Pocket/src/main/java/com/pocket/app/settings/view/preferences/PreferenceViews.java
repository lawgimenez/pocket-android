package com.pocket.app.settings.view.preferences;

import android.content.DialogInterface;
import android.util.SparseArray;
import android.view.View;

import com.pocket.app.App;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.ActionPreference.OnClickAction;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.util.java.Logs;
import com.pocket.util.prefs.BooleanPreference;
import com.pocket.util.prefs.IntPreference;

/**
 * Helper class for easily creating preference views for use in {@link AbsPrefsFragment}.
 */
public abstract class PreferenceViews {

	/**
	 * Creates a new Preference Header. {@link Preference}
	 * 
	 * @param frag
	 * @param title
	 * @return
	 */
	public static HeaderPreference newHeader(AbsPrefsFragment frag, int title) {
		return newHeader(frag, getString(title), true);
	}
	
	/**
	 * Creates a new Preference Header. {@link Preference}
	 * 
	 * @param frag
	 * @param title
	 * @return
	 */
	public static HeaderPreference newHeader(AbsPrefsFragment frag, String title) {
		return newHeader(frag, title, true);
	}

	/**
	 * Creates a new Preference Header. {@link Preference}
	 *
	 * @param frag
	 * @param title
	 * @return
	 */
	public static HeaderPreference newHeader(AbsPrefsFragment frag, int title, boolean topDivider) {
		return newHeader(frag, getString(title), topDivider);
	}

	/**
	 * Creates a new Preference Header. {@link Preference}
	 *
	 * @param frag
	 * @param title
	 * @return
	 */
	public static HeaderPreference newHeader(AbsPrefsFragment frag, String title, boolean topDivider) {
		return new HeaderPreference(frag, title, topDivider);
	}
	
	/**
	 * Creates a new {@link ActionBuilder} to create a {@link ActionPreference}.
	 * 
	 * @param frag
	 * @param title
	 * @return
	 */
	public static ActionBuilder newActionBuilder(AbsPrefsFragment frag, int title) {
		return newActionBuilder(frag, getString(title));
	}
	
	/**
	 * Creates a new {@link ActionBuilder} to create a {@link ActionPreference}.
	 *  
	 * @param frag
	 * @param title
	 * @return
	 */
	public static ActionBuilder newActionBuilder(AbsPrefsFragment frag, String title) {
		return new ActionBuilder(frag, title);
	}

	/**
	 * Creates a new {@link ImportantBuilder} to create a {@link ImportantPreference}.
	 *
	 * @param frag
	 * @param title
	 * @return
	 */
	public static ImportantBuilder newImportantBuilder(AbsPrefsFragment frag, int title) {
		return newImportantBuilder(frag, getString(title));
	}

	/**
	 * Creates a new {@link ImportantBuilder} to create a {@link ImportantPreference}.
	 *
	 * @param frag
	 * @param title
	 * @return
	 */
	public static ImportantBuilder newImportantBuilder(AbsPrefsFragment frag, String title) {
		return new ImportantBuilder(frag, title);
	}
	
	/** Creates a new {@link ToggleSwitchBuilder} to create a {@link ToggleSwitchPreference}. */
	public static ToggleSwitchBuilder newToggleSwitchBuilder(AbsPrefsFragment frag, BooleanPreference pref, int title) {
		return newToggleSwitchBuilder(frag, pref, getString(title));
	}

	/** Creates a new {@link ToggleSwitchBuilder} to create a {@link ToggleSwitchPreference}. */
	public static ToggleSwitchBuilder newToggleSwitchBuilder(AbsPrefsFragment frag, ToggleSwitchPreference.PrefHandler pref, int title) {
		return newToggleSwitchBuilder(frag, pref, getString(title));
	}

	/** Creates a new {@link ToggleSwitchBuilder} to create a {@link ToggleSwitchPreference}. */
	public static ToggleSwitchBuilder newToggleSwitchBuilder(AbsPrefsFragment frag, BooleanPreference pref, String title) {
		return newToggleSwitchBuilder(frag,
				new ToggleSwitchPreference.PrefHandler() {
					@Override public boolean get() {
						return pref.get();
					}
					@Override public void set(boolean value) {
						pref.set(value);
					}
				},
				title);
	}

	/** Creates a new {@link ToggleSwitchBuilder} to create a {@link ToggleSwitchPreference}. */
	public static ToggleSwitchBuilder newToggleSwitchBuilder(AbsPrefsFragment frag, ToggleSwitchPreference.PrefHandler pref, String title) {
		return new ToggleSwitchBuilder(frag, title, pref);
	}

	/**
	 * Creates a new {@link MultipleChoiceBuilder} to create a {@link MultipleChoicePreference}.
	 * 
	 * @param frag
	 * @param pref The preference that will be controlled by this picker
	 * @param title
	 * @return
	 */
	public static MultipleChoiceBuilder newMultipleChoiceBuilder(AbsPrefsFragment frag, IntPreference pref, int title) {
		return newMultipleChoiceBuilder(frag, pref, getString(title));
	}
	
	/**
	 * Creates a new {@link MultipleChoiceBuilder} to create a {@link MultipleChoicePreference}.
	 * 
	 * @param frag
	 * @param pref The preference that will be controlled by this picker
	 * @param title
	 * @return
	 */
	public static MultipleChoiceBuilder newMultipleChoiceBuilder(AbsPrefsFragment frag, IntPreference pref, String title) {
		return new MultipleChoiceBuilder(frag, title, pref);
	}
	
	public static MultipleChoiceBuilder newMultipleChoiceBuilder(AbsPrefsFragment frag, int title, MultipleChoicePreference.PrefHandler pref) {
		return newMultipleChoiceBuilder(frag, getString(title), pref);
	}
	
	public static MultipleChoiceBuilder newMultipleChoiceBuilder(AbsPrefsFragment frag, String title, MultipleChoicePreference.PrefHandler pref) {
		return new MultipleChoiceBuilder(frag, title, pref);
	}
	
	public abstract static class SettingBuilder {

		protected final AbsPrefsFragment frag;
		protected final String title;
		protected final SparseArray<CharSequence> summary = new SparseArray<>();
		protected EnabledCondition enabledWhen;
		protected UiEntityIdentifier identifier;

		private SettingBuilder(AbsPrefsFragment frag, String title) {
			this.frag = frag;
			this.title = title;
		}
		
		/**
		 * Set the description text below the label/title for the default and/or unchecked state. 
		 * @param value
		 * @return
		 */
		public SettingBuilder setSummaryDefaultUnchecked(int value) {
			return setSummaryDefaultUnchecked(getString(value));
		}
		
		/**
		 * Set the description text below the label/title for the default and/or unchecked state. 
		 * @param value
		 * @return
		 */
		public SettingBuilder setSummaryDefaultUnchecked(String value) {
			summary.put(ActionPreference.SUMMARY_DEFAULT_OR_UNCHECKED, value);
			return this;
		}
		
		/**
		 * Set the description text to be used when the preference is disabled. 
		 * @param value
		 * @return
		 */
		public SettingBuilder setSummaryDisabled(int value) {
			summary.put(ActionPreference.SUMMARY_UNAVAILABLE, getString(value));
			return this;
		}
		
		/**
		 * If invoked, the preference will appear disabled unless the referenced preference equals the value.
		 * @param pref The preference to check
		 * @param value The value the referenced preference must be for this preference to be shown as enabled 
		 * @return
		 */
		public SettingBuilder setEnabledWhen(final BooleanPreference pref, final boolean value) {
			enabledWhen = () -> pref.get() == value;
			return this;
		}
		
		/**
		 * If invoked, the preference will appear disabled unless the condition evaluates to true. 
		 */
		public SettingBuilder setEnabledWhen(final EnabledCondition enabledWhen) {
			this.enabledWhen = enabledWhen;
			return this;
		}

		public SettingBuilder setIdentifier(UiEntityIdentifier identifier) {
			this.identifier = identifier;
			return this;
		}
		
		public abstract Preference build();
		
	}

	public static class ImportantBuilder extends ActionBuilder {

		private ImportantBuilder(AbsPrefsFragment frag, String title) {
			super(frag, title);
		}

		@Override
		public ImportantPreference build() {
			return new ImportantPreference(frag, title, onClickListener, onLongClickListener, enabledWhen, identifier);
		}

	}
	
	public static class ActionBuilder extends SettingBuilder {

		protected OnClickAction onClickListener;
		protected OnClickAction onLongClickListener;

		private ActionBuilder(AbsPrefsFragment frag, String title) {
			super(frag, title);
		}
		
		@Override
		public ActionBuilder setSummaryDefaultUnchecked(int value) {
			return (ActionBuilder) super.setSummaryDefaultUnchecked(value);
		}
		
		@Override
		public ActionBuilder setSummaryDefaultUnchecked(String value) {
			return (ActionBuilder) super.setSummaryDefaultUnchecked(value);
		}
		
		@Override
		public ActionBuilder setSummaryDisabled(int value) {
			return (ActionBuilder) super.setSummaryDisabled(value);
		}
		
		@Override
		public ActionBuilder setEnabledWhen(final BooleanPreference pref, final boolean value) {
			return (ActionBuilder) super.setEnabledWhen(pref, value);
		}
		
		@Override
		public ActionBuilder setEnabledWhen(EnabledCondition enabledWhen) {
			return (ActionBuilder) super.setEnabledWhen(enabledWhen);
		}
		
		/**
		 * Set an action to be performed when clicked.
		 * @param listener
		 * @return
		 */
		public ActionBuilder setOnClickListener(OnClickAction listener) {
			this.onClickListener = listener;
			return this;
		}

        /**
         * Set an action to be performed when long pressed. This is not for normal use,
         * mostly for hidden features that the support team can tell people about as needed.
         *
         * @param listener
         * @return
         */
        public ActionBuilder setOnLongClickListener(OnClickAction listener) {
            this.onLongClickListener = listener;
            return this;
        }

		@SuppressWarnings("deprecation")
		@Override
		public ActionPreference build() {
			return new ActionPreference(frag, 
					title,
					summary.size() > 0 ? summary : null,
					onClickListener, onLongClickListener,
					enabledWhen, identifier);
		}
		
	}

	public static class ToggleSwitchBuilder extends SettingBuilder {

		private final ToggleSwitchPreference.PrefHandler pref;
		private ToggleSwitchPreference.OnChangeListener onChangeListener;

		private ToggleSwitchBuilder(AbsPrefsFragment frag, String title, ToggleSwitchPreference.PrefHandler pref) {
			super(frag, title);
			this.pref = pref;
		}

		@Override
		public ToggleSwitchBuilder setSummaryDefaultUnchecked(int value) {
			return (ToggleSwitchBuilder) super.setSummaryDefaultUnchecked(value);
		}

		@Override
		public ToggleSwitchBuilder setSummaryDefaultUnchecked(String value) {
			return (ToggleSwitchBuilder) super.setSummaryDefaultUnchecked(value);
		}

		@Override
		public ToggleSwitchBuilder setSummaryDisabled(int value) {
			return (ToggleSwitchBuilder) super.setSummaryDisabled(value);
		}

		@Override
		public ToggleSwitchBuilder setEnabledWhen(final BooleanPreference pref, final boolean value) {
			return (ToggleSwitchBuilder) super.setEnabledWhen(pref, value);
		}

		public ToggleSwitchBuilder setEnabledWhen(final EnabledCondition enabledWhen) {
			this.enabledWhen = enabledWhen;
			return this;
		}

		/**
		 * Set the description text to be used when the preference is checked.
		 * @param value
		 * @return
		 */
		public ToggleSwitchBuilder setSummaryChecked(int value) {
			return setSummaryChecked(getString(value));
		}

		/**
		 * Set the description text to be used when the preference is checked.
		 * @param value
		 * @return
		 */
		public ToggleSwitchBuilder setSummaryChecked(String value) {
			summary.put(ActionPreference.SUMMARY_CHECKED, value);
			return this;
		}

		/**
		 * Set a listener to be invoked before and after the user toggles the checkbox
		 * @param listener
		 * @return
		 */
		public ToggleSwitchBuilder setOnChangeListener(ToggleSwitchPreference.OnChangeListener listener) {
			this.onChangeListener = listener;
			return this;
		}
		
		/**
		 * A simpler, lambda supported variant if you want onChange to just return true.
		 */
		public ToggleSwitchBuilder setOnChangeListener(SimpleOnChangeListener listener) {
			return setOnChangeListener(new ToggleSwitchPreference.OnChangeListener() {
				@Override
				public boolean onChange(View view, boolean nowEnabled) {
					return true;
				}
				
				@Override
				public void afterChange(boolean nowEnabled) {
					listener.afterChange(nowEnabled);
				}
			});
		}
		
		public interface SimpleOnChangeListener {
			void afterChange(boolean nowEnabled);
		}
		
		@Override
		public ToggleSwitchPreference build() {
			return new ToggleSwitchPreference(frag,
					pref,
					title,
					summary.size() > 0 ? summary : null,
					onChangeListener,
					enabledWhen,
					identifier);
		}

	}
	
	public static class MultipleChoiceBuilder extends ActionBuilder {

		private final MultipleChoicePreference.PrefHandler pref;
		private MultipleChoicePreference.OnSelectedItemChangedListener onItemSelectedListener;

		private MultipleChoiceBuilder(AbsPrefsFragment frag, String title, IntPreference pref) {
			this(frag, title, new MultipleChoicePreference.PrefHandler() {
				@Override
				public int getSelected() {
					return pref.get();
				}
				@Override
				public void setSelected(int index) {
					pref.set(index);
				}
			});
		}
		
		private MultipleChoiceBuilder(AbsPrefsFragment frag, String title, MultipleChoicePreference.PrefHandler pref) {
			super(frag, title);
			this.pref = pref;
		}
		
		/** Not allowed for {@link MultipleChoiceBuilder}. Use {@link #addChoice(int)} */
		@Override
		public MultipleChoiceBuilder setSummaryDefaultUnchecked(int value) {
			Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead");
			return this;
		}
		
		/** Not allowed for {@link MultipleChoiceBuilder}. Use {@link #addChoice(int)} */
		@Override
		public MultipleChoiceBuilder setSummaryDefaultUnchecked(String value) {
			Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead");
			return this;
		}
		
		/** Not allowed for {@link MultipleChoiceBuilder}. Use {@link #addChoice(int)} */
		@Override
		public MultipleChoiceBuilder setSummaryDisabled(int value) {
			Logs.throwIfNotProduction("not allowed on this pref type, use addChoice instead");
			return this;
		}
		
		/** REVIEW i don't believe this pref type supports being disabled? */
		@Override
		public MultipleChoiceBuilder setEnabledWhen(final BooleanPreference pref, final boolean value) {
			return (MultipleChoiceBuilder) super.setEnabledWhen(pref, value);
		}
		
		/** Not allowed for {@link MultipleChoiceBuilder}. */
		@Override
		public final ActionBuilder setOnClickListener(OnClickAction listener) {
			Logs.throwIfNotProduction("setOnClickListener not allowed for checkboxes, use on changed listeners instead.");
			return super.setOnClickListener(listener);
		}

        /** Not allowed for {@link MultipleChoiceBuilder}. */
        @Override
        public final ActionBuilder setOnLongClickListener(OnClickAction listener) {
            Logs.throwIfNotProduction("setOnLongClickListener not allowed for checkboxes, use on changed listeners instead.");
            return super.setOnClickListener(listener);
        }
		
		/**
		 * Set a listener to be invoked when the user picks a new value
		 * @param listener
		 * @return
		 */
		public MultipleChoiceBuilder setOnItemSelectedListener(MultipleChoicePreference.OnSelectedItemChangedListener listener) {
			this.onItemSelectedListener = listener;
			return this;
		}
		
		/**
		 * A simpler, lambda supported variant if you want onItemSelected to just return true.
		 */
		public MultipleChoiceBuilder setOnItemSelectedListener(SimpleOnSelectedItemChangedListener listener) {
			return setOnItemSelectedListener(new MultipleChoicePreference.OnSelectedItemChangedListener() {
				@Override
				public boolean onItemSelected(View view, int newValue, DialogInterface dialog) {
					return true;
				}
				
				@Override
				public void onItemSelectionChanged(int newValue) {
					listener.onItemSelectionChanged(newValue);
				}
			});
		}
		
		public interface SimpleOnSelectedItemChangedListener {
			void onItemSelectionChanged(int newValue);
		}
		
		/**
		 * Invoke in order, for as many options as should be available in the picker dialog. These are the labels the user
		 * will pick from and will also be displayed under the label/title of the preference view to show the currently selected option.
		 * 
		 * @param label
		 * @return
		 */
		public MultipleChoiceBuilder addChoice(int label) {
			return addChoice(getString(label));
		}
		
		/**
		 * Invoke in order, for as many options as should be available in the picker dialog. These are the labels the user
		 * will pick from and will also be displayed under the label/title of the preference view to show the currently selected option.
		 * 
		 * @param label
		 * @return
		 */
		public MultipleChoiceBuilder addChoice(String label) {
			summary.put(summary.size(), label);
			return this;
		}

		@SuppressWarnings("deprecation")
		@Override
		public MultipleChoicePreference build() {
			return new MultipleChoicePreference(frag,
					pref,
					title,
					summary, 
					onItemSelectedListener, 
					enabledWhen,
					identifier);
		}
		
	}
	
	public interface EnabledCondition {
		boolean isTrue();
	}
	
	private static String getString(int res) {
		return App.getStringResource(res);
	}



}
