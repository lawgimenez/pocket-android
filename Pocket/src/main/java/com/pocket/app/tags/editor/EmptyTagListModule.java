package com.pocket.app.tags.editor;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ideashower.readitlater.R;
import com.pocket.app.tags.ItemsTaggingFragment;

import java.util.ArrayList;

/**
 * A module that shows an empty state for the tag list.
 * 
 * @see TagModule
 * @see TagModuleManager
 * @see ItemsTaggingFragment
 */
public class EmptyTagListModule extends TagModule {
	
	private final View mView;
	
	private boolean mIsTagsEmpty;
	private boolean mHasInput;
	
	public EmptyTagListModule(TagModuleManager manager, VisibilityListener visListener, Context context) {
		super(manager, visListener, context);
		
		mView = LayoutInflater.from(getContext())
					.inflate(R.layout.view_add_tags_empty, null, false);
		mView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
	}
	
	@Override
	public View getView() {
		return mView;
	}

	@Override
	public void load(TagModuleLoadedListener callback) {
		callback.onTagModuleLoaded();
	}
	
	@Override
	protected void onTagsLoaded(ArrayList<String> allTags) {
		mIsTagsEmpty = allTags.isEmpty();
		updateVisibility();
	}

	@Override
	public void onTagInputTextChanged(CharSequence text) {
		mHasInput = !TextUtils.isEmpty(text);
		updateVisibility();
	}
	
	private void updateVisibility() {
		setPreferredVisibility(mIsTagsEmpty && !mHasInput);
	}

	@Override
	public void onTagAdded(String tag) {}
	
	@Override
	public void onTagRemoved(String tag) {}

}
