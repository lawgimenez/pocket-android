package com.pocket.app.tags.editor;

import android.content.Context;
import android.view.View;

import com.pocket.app.App;
import com.pocket.app.PocketApp;
import com.pocket.app.tags.ItemsTaggingFragment;

import java.util.ArrayList;

import androidx.annotation.NonNull;


/**
 * Represents a piece of UI that displays tags or allows the user to modify the current selection of tags.
 * <p>
 * Used in conjuction with {@link TagModuleManager} and {@link ItemsTaggingFragment}.
 */
public abstract class TagModule {
	
	private final TagModuleManager mManager;
	private final VisibilityListener mVisibilityListener;
	private final Context mContext;
	private boolean mPreferredVisibility;
	
	public TagModule(TagModuleManager manager, VisibilityListener visiblityListener, Context context) {
		mManager = manager;
		mVisibilityListener = visiblityListener;
		mContext = context;
	}
	
	protected TagModuleManager getManager() {
		return mManager;
	}
	
	protected Context getContext() {
		return mContext;
	}
	
	protected PocketApp app() {
		return App.from(mContext);
	}
	
	/**
	 * Set whether or not this module should be visible. There is no guarantee as it is up to the parent
	 * classes to decide when to show this module. Leave that logic to the parents and just declare what you think
	 * it should be based on your inner state. 
	 *  
	 * @param visible
	 */
	protected void setPreferredVisibility(boolean visible) {
		mPreferredVisibility = visible;
		mVisibilityListener.onTagModuleVisibilityChanged(this, visible);
	}
	
	/** @see #setPreferredVisibility(boolean) */
	public boolean isPreferredVisible() {
		return mPreferredVisibility;
	}
	
	/**
	 * Return the View that represents this module.
	 */
	public abstract @NonNull View getView();
	
	/**
	 * Perform any loading needed for the module and invoke the callback when ready. <b>You
	 * must invoke the callback even if you don't need to load anything.</b>
	 * <p>
	 * If your module needs the list of all tags, you may invoke the call back during {@link #onTagsLoaded(ArrayList)} instead.
	 * @param callback
	 */
	public abstract void load(TagModuleLoadedListener callback);
	
	/**
	 * A user has changed the uncompleted tag.
	 * @param text
	 */
	public abstract void onTagInputTextChanged(CharSequence text);
	
	/**
	 * A tag has been selected.
	 * @param tag
	 */
	public abstract void onTagAdded(String tag);
	
	/**
	 * A tag has been removed.
	 * @param tag
	 */
	public abstract void onTagRemoved(String tag);
	
	/** 
	 * A callback to {@link TagModule#load(TagModuleLoadedListener)}.  
	 */
	public interface TagModuleLoadedListener {
		void onTagModuleLoaded();
	}
	
	public interface VisibilityListener {
		/**
		 * The preferred visibility of this module has changed.
		 * @param module
		 * @param visible
		 */
		void onTagModuleVisibilityChanged(TagModule module, boolean visible);
	}

	/**
	 * Invoked by {@link TagModuleManager} during the loading process when it has loaded the list of all available tags.
	 * <p>
	 * <b>DO NOT</b> modify the provided list. It is for read only use.
	 * @param allTags
	 */
	protected void onTagsLoaded(ArrayList<String> allTags) {
		
	}
	
	protected void onPremiumStatusChanged() {}
	
}