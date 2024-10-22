package com.pocket.app.tags.editor;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.squareup.phrase.Phrase;



/**
 * A module that controls the <code>add new tag "xyz"</code> option that appears at the top
 * of an autocomplete list.
 * 
 * @see ItemsTaggingFragment This is the entry point for all related code. See the documentation on this class for the big picture.
 * @see TagModule
 * @see TagModuleManager
 */
public class AddNewTagModule extends TagModule implements View.OnClickListener {
	
	private final View mRootView;
	private final TextView mTextView;
	
	private CharSequence mPendingTag;
	
	public AddNewTagModule(TagModuleManager manager, VisibilityListener visListener, Context context) {
		super(manager, visListener, context);
		
		mRootView = LayoutInflater.from(getContext())
					.inflate(R.layout.view_simple_title_divider_row, null, false);
		mRootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		mTextView = mRootView.findViewById(R.id.text);
		mTextView.setTextAppearance(context, com.pocket.ui.R.style.Pkt_Text_Link);
		mRootView.setOnClickListener(this);
	}
	
	@Override
	public View getView() {
		return mRootView;
	}

	@Override
	public void load(TagModuleLoadedListener callback) {
		onTagInputTextChanged(mPendingTag); // Trigger visibility default
		callback.onTagModuleLoaded();
	}

	@Override
	public void onTagInputTextChanged(CharSequence text) {
		mPendingTag = text;
		
		if (TextUtils.isEmpty(text) || getManager().containsTag(text)) {
			// Don't show
			mTextView.setText("");
			setPreferredVisibility(false);
			
		} else {
			// Show
			CharSequence label = Phrase.from(getContext().getResources(), R.string.ac_add_new_tag)
					.put("name_of_tag", text.toString()) // toString() removes the charsequence's underline, which is carried over from the EditText
					.format();
			
			mTextView.setText(label);
			setPreferredVisibility(true);
		}
	}

	@Override
	public void onTagAdded(String tag) {}
	
	@Override
	public void onTagRemoved(String tag) {}
	
	@Override
	public void onClick(View v) {
		String tag = mPendingTag.toString();
		getManager().addTag(this, tag);
	}
	
}
