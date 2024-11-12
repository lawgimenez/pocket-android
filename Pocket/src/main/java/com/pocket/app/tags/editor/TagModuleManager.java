package com.pocket.app.tags.editor;

import android.os.Bundle;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.pocket.app.tags.editor.TagModule.TagModuleLoadedListener;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.ReservedTag;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.util.android.view.ViewEnabledManager.ViewEnabledCondition;
import com.pocket.util.java.Safe;
import com.pocket.util.java.StringValidator;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Used by the {@link ItemsTaggingFragment}. Manages the selection of tags and communication between the various {@link TagModule}'s.
 * For example, if a user selects a tag from a {@link TagListModule}, it informs this manager that a tag should be added. Then this 
 * new tag is propagated to all of the other modules so they can update their state. This keeps all the modules in sync. See the module
 * class for the types of information that is synced.
 * <p>
 * Anything that allows the user to see or select tags should be represented by a {@link TagModule}.
 * <p>
 * Add all of the {@link TagModule}'s with {@link #addTagModule(TagModule)}. Then start the loading process with {@link #load(List)}
 * <p>
 * Once all modules have loaded, the {@link TagModuleManagerListener} passed to the constructor will receive a {@link TagModuleManagerListener#onTagModulesLoadingStateChanged(boolean)} callback.
 * <p>
 * To get a list of the currently "selected" or choosen tags, use {@link #getListForReadOnly()}.
 */
public class TagModuleManager implements TagModuleLoadedListener, ViewEnabledCondition {
	
	private static final int MAX_TAG_LENGTH = 25;
	
	private static final String STATE_IS_MODIFIED = "isModified";
	public static final String STATE_CURR_TAG_LIST = "tagList";
	
	private final ArrayList<String> mAllTags = new ArrayList<String>();
	private final ArrayList<String> mSelectedTags = new ArrayList<String>();
	private final ArrayList<TagModule> mTagModules = new ArrayList<TagModule>();
	private final Pocket mPocket;
	private final TagModuleManagerListener mListener;
	private Subscription premiumSubscription;
	private StringValidator mTagValidator;
	
	private int mLoadingCount;
	private boolean mIsModified;
	private boolean mIsLoaded;
	
	// action context
	private boolean mCanStartCounting = false; // we need to wait for the EditText tags to be initialized 
	private int mContextOpenTagsCount = 0;
	private int mContextEnterTagsCount = 0;
	private int mContextSuggestedTagsAddCount = 0;
	private int mContextTapTagsCount = 0;
	private int mContextRemovedTagsCount = 0;

	public TagModuleManager(Pocket pocket,
			TagModuleManagerListener listener,
			Bundle savedInstanceState) {
		mPocket = pocket;
		mListener = listener;
		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(STATE_IS_MODIFIED)) {
				onModified();
			}
		}
		
		mTagValidator = value -> {
			if (value.equalsIgnoreCase(ReservedTag._UNTAGGED_.value)) {
				return App.getContext().getResources().getString(R.string.dg_invalid_tag_m, ReservedTag._UNTAGGED_.value);
			} else if (value.length() > MAX_TAG_LENGTH) {
				return App.getContext().getResources().getString(R.string.dg_tag_too_long_m);
			} else {
				return null;
			}
		};
		
		premiumSubscription = pocket.subscribe(Changes.of(pocket.spec().things().loginInfo().build()).value(i -> i.account.premium_status),
						a -> {
							if (mIsLoaded) {
								for (TagModule module : mTagModules) {
									module.onPremiumStatusChanged();
								}
							}
						});
	}
	
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_IS_MODIFIED, mIsModified);
		outState.putStringArrayList(STATE_CURR_TAG_LIST, getListForReadOnly());
	}

	public void onDestroy() {
		premiumSubscription = Subscription.stop(premiumSubscription);
	}
	
	public void addTagModule(TagModule tagModule) {
		mTagModules.add(tagModule);
	}
	
	/**
	 * Invokes {@link TagModule#load(TagModuleLoadedListener)} on all modules and then loads all known tags
	 * and sets them as {@link #mAllTags}. {@link TagModuleManagerListener#onTagModulesLoadingStateChanged(boolean)} will receive the callback when complete.
	 */
	public void load(List<String> selected) {
		mListener.onTagModulesLoadingStateChanged(true);
		mLoadingCount = mTagModules.size() + 1; // +1 to account for ourselves loading the available tags.
		
		for (TagModule module : mTagModules) {
			module.load(this);
		}

		mPocket.sync(mPocket.spec().things().tags().build())
				.onSuccess(tags -> {
					mAllTags.clear();
					mAllTags.addAll(Safe.nonNullCopy(tags.tags));
					
					for (String tag : selected) {
						addTag(null, tag);
					}
					for (TagModule module : mTagModules) {
						module.onTagsLoaded(mAllTags);
					}
					onTagModuleLoaded();
				})
				.onFailure(e -> {
					throw new RuntimeException(e);
				});
	}
	
	/**
	 * Invoked by a {@link TagModule} if it encounters an error to show to the user.
	 * @param error
	 */
	protected void setError(String error) {
		if (error != null) {
			mListener.onError(error);
		} else {
			mListener.onCancelError();
		}
	}
	
	/**
	 * Invoked by a {@link TagModule} when the user has selected a tag.
	 * @param from The module that invoked this method to prevent it from getting a callback to itself. Or null if you still want the callback.
	 * @param tag The new tag to add to the list of selected tags.
	 * @return true if allowed, false if there was an error with the tag.
	 */
	protected boolean addTag(TagModule from, String tag) {
		if (mCanStartCounting) {
			if (from instanceof SuggestedTagsModule) {
				mContextSuggestedTagsAddCount = mContextSuggestedTagsAddCount + 1;
			} else if (from instanceof AddNewTagModule || from instanceof TagEditTextModule){
				mContextEnterTagsCount = mContextEnterTagsCount + 1;
			} else if (from	instanceof TagListModule){
				if (((TagListModule)from).mIsFiltered){
					mContextEnterTagsCount = mContextEnterTagsCount + 1;
				} else {
					mContextTapTagsCount = mContextTapTagsCount + 1;
				}
			}
		}
		
		
		String error = mTagValidator.validate(tag);
		if (error != null) {
			mListener.onError(error);
			return false;
			
		} else {
			mListener.onCancelError();
			
			mSelectedTags.add(tag);
			for (TagModule module : mTagModules) {
				if (module == from) {
					continue; // Don't send listener back to itself (the one who invoked this method) 
				}
				module.onTagAdded(tag);
			}
			onModified();
			return true;
		}
	}
	
	/**
	 * Invoked by a {@link TagModule} when the user has unselected a tag.
	 * @param from The module that invoked this method to prevent it from getting a callback to itself. Or null if you still want the callback.
	 * @param tag The tag to remove from the list of selected tags.
	 */
	protected void removeTag(TagModule from, String tag) {
		if (from instanceof TagEditTextModule) {
			mContextRemovedTagsCount = mContextRemovedTagsCount + 1;
		}
		
		mSelectedTags.remove(tag);
		for (TagModule module : mTagModules) {
			if (module == from) {
				continue; // Don't send listener back to itself (the one who invoked this method) 
			}
			module.onTagRemoved(tag);
		}
		onModified();
	}
	
	/**
	 * Invoked by a {@link TagModule} when the user has changed the input in the {@link TagEditTextModule}. This fires as they type.
	 * @param from The module that invoked this method to prevent it from getting a callback to itself. Or null if you still want the callback.
	 * @param text The current text in the {@link TagEditTextModule}.
	 */
	protected void onTagInputTextChanged(TagModule from, CharSequence text) {
		for (TagModule module : mTagModules) {
			if (module == from) {
				continue; // Don't send listener back to itself (the one who invoked this method) 
			}
			module.onTagInputTextChanged(text);
		}
		onModified();
	}

	@Override
	public void onTagModuleLoaded() {
		mLoadingCount--;
		if (mLoadingCount == 0) {
			onAllModulesLoaded();
		}
	}

	/**
	 * Invoked when all of the modules have finished loading.
	 */
	private void onAllModulesLoaded() {
		mIsLoaded = true;
		mCanStartCounting = true;
		mContextOpenTagsCount = mSelectedTags.size();
		
		mListener.onTagModulesLoadingStateChanged(false);
	}
	
	private void onModified() {
		if (mLoadingCount > 0) {
			return; // Ignore while loading
		}
		mIsModified = true;
		mListener.onModified();
	}

	@Override
	public boolean isEnabled() {
		return mIsModified;
	}

	/** DO NOT MODIFY THIS LIST. READ ONLY ACCESS */
	public ArrayList<String> getListForReadOnly() {
		return mSelectedTags;
	}

	public boolean isModified() {
		return mIsModified;
	}
	
	/**
	 * Whether or not a tag is within the list of all tags (ignoring case).
	 * @param tag
	 * @return
	 */
	public boolean containsTag(CharSequence tag) {
		for (String v : mAllTags) {
			if (StringUtils.equalsIgnoreCase(v, tag)) {
				return true;
			}
		}
		return false;
	}
	
	public interface TagModuleManagerListener {
		void onTagModulesLoadingStateChanged(boolean isLoading);
		void onCancelError();
		void onModified();
		void onError(String string);
	}

	protected StringValidator getTagValidator() {
		return mTagValidator;
	}
	
	
	public int getContextOpenTagsCount() {
		return mContextOpenTagsCount;
	}
	
	public int getContextEnterTagsCount() {
		return mContextEnterTagsCount;
	}
	
	public int getContextSuggestedAddTagsCount() {
		return mContextSuggestedTagsAddCount;
	}
	
	public int getContextTapTagsCount() {
		return mContextTapTagsCount;
	}
	
	public int getContextRemovedTagsCount() {
		return mContextRemovedTagsCount;
	}
	
}