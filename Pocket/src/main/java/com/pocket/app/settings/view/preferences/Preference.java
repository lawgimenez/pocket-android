package com.pocket.app.settings.view.preferences;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.AbsPrefsFragment.PrefAdapter;
import com.pocket.sdk.preferences.AppPrefs;
import com.pocket.sdk.util.AbsPocketFragment;

/**
 * An item and View in {@link PrefAdapter}. Typically the view is a representation or
 * control of a {@link AppPrefs}.
 */
public abstract class Preference implements OnClickListener, View.OnLongClickListener {

	/** Used as a dummy preference for the banner view */
	public static class SimplePreference extends Preference {

		public SimplePreference(AbsPrefsFragment settings) {
			super(settings);
		}

		@Override
		public PrefViewType getType() {
			return null;
		}

		@Override
		public void applyToView(View view) {

		}

		@Override
		public boolean isEnabled() {
			return false;
		}

		@Override
		public boolean isClickable() {
			return false;
		}

		@Override
		public boolean update() {
			return false;
		}

		@Override
		public void onClick(View v) {

		}

		@Override
		public boolean onLongClick(View v) {
			return false;
		}
	}

	public enum PrefViewType {
		BANNER, HEADER, ACTION, TOGGLE, CACHE_LIMIT, IMPORTANT
	}
	
	protected final AbsPrefsFragment mSettings;
	
	public Preference (AbsPrefsFragment settings) {
		if (settings == null) {
			throw new NullPointerException("settings cannot be null");
		}
		mSettings = settings;
	}

	protected AbsPrefsFragment getFragment() {
		return mSettings;
	}

	/**
	 * @return The view type indentifier within {@link PrefAdapter}. Used for recycling views via {@link BaseAdapter#getItemViewType(int)}.
	 */
	public abstract PrefViewType getType();

	/**
	 * Setup the view for controlling or showing this {@link Preference}.
	 * View will be of type that {@link PrefAdapter#onCreateViewHolder(ViewGroup, int)} creates based on {@link #getType()}
	 */
	public abstract void applyToView(View view);

	/**
	 * @return Whether or not the {@link Preference} and view is enabled.
	 */
	public abstract boolean isEnabled();
	
	/**
	 * @return Whether or not a pressed state or {@link OnClickListener} will be allowed on the view that represents this.
	 */
	public abstract boolean isClickable();
	
	/**
	 * Something may have changed related to the status of the {@link Preference}. If needed,
	 * recheck its status.
	 * 
	 * @return true if the setting did change, false if remains the same as before.
	 */
	public abstract boolean update();

	/**
	 * The view you setup via {@link #applyToView(View)} was clicked on.
	 */
	@Override
	public abstract void onClick(View v);

	@Override
	public abstract boolean onLongClick(View v);
}
