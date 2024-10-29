package com.pocket.app.tags.editor;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.Filter;

import com.ideashower.readitlater.R;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.pocket.ui.view.menu.SectionHeaderView;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * A tagging module that controls the list of all unselected tags.
 * <p>
 * While there is a pending tag it filters by tags that start with the pending input.
 * 
 * @see ItemsTaggingFragment This is the entry point for all related code. See the documentation on this class for the big picture.
 * @see TagModule
 * @see TagModuleManager
 */
public class TagListModule extends TagModule implements View.OnClickListener {
	
	/** All tags, unfiltered */
	private final ArrayList<String> mSourceList = new ArrayList<String>();
	/** A filtered list, this is the one actually displayed in the adapter */
	private final ArrayList<String> mDisplayList = new ArrayList<String>();
	private final Filter mFilter = new TagFilter();

	private CharSequence mConstraint;
	private TagModuleLoadedListener mPendingCallback;
	private final SectionHeaderView[] mHeaders;
	protected boolean mIsFiltered;

	public TagListModule(TagModuleManager manager, VisibilityListener visListener, Context context, SectionHeaderView[] headers) {
		super(manager, visListener, context);
		mHeaders = headers;
	}
	
	@Override
	public View getView() {
		return null; // unused, this module provides a list of tags, rather than a view
	}
	
	@Override
	public void load(final TagModuleLoadedListener callback) {
		mPendingCallback = callback;
	}
	
	@Override
	protected void onTagsLoaded(ArrayList<String> allTags) {
		mSourceList.clear();
		mSourceList.addAll(allTags);
		
		mDisplayList.clear();
		mDisplayList.addAll(allTags);
		
		mFilter.filter(null);
		
		mPendingCallback.onTagModuleLoaded();
		mPendingCallback = null;
		
		updateVisibility();
	}

	@Override
	public void onTagInputTextChanged(CharSequence text) {
		if (TextUtils.isEmpty(text)) {
			setIsFiltered(false);
		} else {
			setIsFiltered(true);
		}
		
		mConstraint = text;
		mFilter.filter(text);
		updateVisibility();
	}

	private void updateVisibility() {
		setPreferredVisibility(mIsFiltered || !mSourceList.isEmpty());
	}
	
	@Override
	public void onTagAdded(String tag) {
		refilter();
	}

	@Override
	public void onTagRemoved(String tag) {
		refilter();
	}
	
	@Override
	public void onClick(View v) {
		String tag = (String) v.getTag();
		getManager().addTag(this, tag);
		onTagAdded(tag);
	}
	
	private void refilter() {
		mFilter.filter(mConstraint);
	}
	
	private void setIsFiltered(boolean isFiltered) {
		if (mIsFiltered == isFiltered) {
			return;
		}
		mIsFiltered = isFiltered;
		
		for (SectionHeaderView view : mHeaders) {
			view.bind().label(isFiltered ? R.string.lb_tags_autocomplete : R.string.lb_all_tags);
		}
	}
	
	private class TagFilter extends Filter  {
		
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			ArrayList<String> filtered = (ArrayList<String>) results.values;
			
			mDisplayList.clear();
			mDisplayList.addAll(filtered);
			
			setPreferredVisibility(true);
		}
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			ArrayList<String> filtered = new ArrayList<>();
			ArrayList<String> startsWith = new ArrayList<>();
			ArrayList<String> contains = new ArrayList<>();
			ArrayList<String> source = new ArrayList<>(mSourceList); // REVIEW if the source was updated for some reason, this could possibly cause a race condition or concurrent mod exeception?
			
			// Filter by tags that start with the constraint
			if (!TextUtils.isEmpty(constraint)) {
				boolean doContainsMatches = constraint.length() >= 2;
				for (String tag : source) {
					if (StringUtils.startsWithIgnoreCase(tag, constraint)) {
						startsWith.add(tag);
					} else if (doContainsMatches && StringUtils.containsIgnoreCase(tag, constraint)) {
						contains.add(tag);
					}
				}
				
				// Merge both lists, with startsWith matches on top. Should already be ordered the same way as the source because we traversed the list top to bottom.
				filtered.addAll(startsWith);
				filtered.addAll(contains);
				
			} else {
				filtered.addAll(mSourceList);
			}
			
			// Filter out any tags already selected
			filtered.removeAll(getManager().getListForReadOnly());
			
			FilterResults results = new FilterResults();
			results.values = filtered;
			results.count = filtered.size();
			
			return results;
		}
	}
	
	public List<String> getDisplayList() {
		return mDisplayList;
	}
	
}
