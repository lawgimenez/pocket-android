package com.pocket.app.settings.view.preferences;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.SparseArray;
import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.analytics.EngagementType;
import com.pocket.app.settings.AbsPrefsFragment;
import com.pocket.app.settings.view.preferences.PreferenceViews.EnabledCondition;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;

/**
 * Manages a multiple choice {@link com.pocket.util.prefs.IntPreference}. Displays an {@link ActionPreference}
 * with the currently selected option as the summary/description.
 * <p>
 * When tapped, it opens a dialog picker.
 */
public class MultipleChoicePreference extends ActionPreference {
	
	protected final PrefHandler mPref;
	private final OnSelectedItemChangedListener mListener;
	private int mSelected;
	private final CharSequence[] mChoices;

	public interface PrefHandler {
		int getSelected();
		void setSelected(int index);
	}
	
	/** Use {@link PreferenceViews} instead. */
	@Deprecated 
	public MultipleChoicePreference(AbsPrefsFragment settings, PrefHandler pref, String label, SparseArray<CharSequence> summary, OnSelectedItemChangedListener listener, EnabledCondition condition, UiEntityIdentifier identifier) {
		super(settings, label, summary, null, null, condition, identifier);
		
		if (summary == null || summary.size() == 0) {
			throw new NullPointerException("summary may not be empty");
		}
		
		if (pref == null) {
			throw new NullPointerException("pref may not be null");
		}
		
		int size = summary.size();
		mChoices = new String[size];
		for (int i = 0; i < size; i++) {
			mChoices[i] = summary.valueAt(i);
		}
		
		mPref = pref;
		
		mListener = listener;
		
		mSelected = pref.getSelected();
	}
	
	@Override
	public PrefViewType getType() {
		return PrefViewType.ACTION;
	}
	
	@Override
	public void onClick(View view) {
		new AlertDialog.Builder(mSettings.getActivity())
			.setTitle(label)
			.setSingleChoiceItems(mChoices, mSelected, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (onItemSelected(view, which, dialog)) {
						dialog.dismiss();
					} else {
						// TODO need a way to reset the selection
					}
				}
				
			})
			.setNegativeButton(R.string.ac_cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			})
			.show();
	}

	public boolean onItemSelected(View view, int newValue, DialogInterface dialog) {
		boolean allowed = mListener == null || mListener.onItemSelected(view, newValue, dialog);
		
		if (allowed && newValue != mSelected) {
			mSelected = newValue;
			mPref.setSelected(newValue);
			mSettings.onPreferenceChange(true);
			if (mListener != null) {
				mListener.onItemSelectionChanged(newValue);
			}
		}

		if (uiEntityIdentifier != null) {
			tracker.bindUiEntityValue(view, Integer.toString(newValue));
			tracker.trackEngagement(view, EngagementType.GENERAL, null, null, null);
		}
		
		return allowed;
	}
	
	public interface OnSelectedItemChangedListener {
		/**
		 * Called when the preference is requested to change. <b>It has not changed yet. If you query the preference it will have the old value.</b> You must return true to allow it to change.
		 * <p>
		 * If you want to know when the value has actually changed, see {@link #onItemSelectionChanged(int)}.
		 *
		 * @param view the view that triggered the selection
		 * @param newValue
		 * @return true if allowed to change, false if not
		 */
		boolean onItemSelected(View view, int newValue, DialogInterface dialog);
		/**
		 * The preference's value has changed.
		 * @param newValue
		 * @see #onItemSelected(View, int, DialogInterface)
		 */
		void onItemSelectionChanged(int newValue);
	}

	@Override
	public boolean update() {
		int newVal = mPref.getSelected();
		if (newVal != mSelected) {
			mSelected = newVal;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public CharSequence getSummary() {
		if (summary == null) {
			return null;
			
		} else {
			return summary.get(mSelected);
		}
	}
	
	@Override
	public boolean isClickable() {
		return true;
	}
	
}
