package com.pocket.app.tags.editor;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.tags.ItemsTaggingFragment;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.endpoint.ApiException;
import com.pocket.sdk.api.generated.enums.PremiumFeature;
import com.pocket.sdk.api.generated.thing.SuggestedTag;
import com.pocket.sdk.api.source.V3Source;
import com.pocket.sync.source.result.SyncException;
import com.pocket.ui.view.badge.SuggestedTagView;
import com.pocket.ui.view.progress.RainbowProgressCircleView;
import com.pocket.util.android.view.chip.ChipLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import kotlin.Unit;


/**
 * Shows a set of tag suggestions. A user can tap on one to quickly add it to their selected tags.
 * <p>
 * <b>For Premium users only. Also only for {@link ItemsTaggingFragment.ItemMode#SINGLE_ITEM}.</b>
 * 
 * @see ItemsTaggingFragment This is the entry point for all related code. See the documentation on this class for the big picture.
 * @see TagModule
 * @see TagModuleManager
 */
public class SuggestedTagsModule extends TagModule implements ChipLayout.OnItemClickListener {
	
	/** 
	 * When the user taps retry/reload, if an error occurs, it should wait at least
	 * this amount of time before hiding the spinner and showing the error message.
	 * This ensures the users actually sees the loading spinner and therefore gets some 
	 * confirmation that their retry happened. Otherwise, if it goes too fast it appears
	 * to have not even attempted to retry.
	 */
	private static final long MIN_RETRY_LOADING_TIME = 500;
	private static final int X_ERROR_SUGGESTED_TAGS_NO_TAGS = 5202;
	
	private final String mItemUrl;
	private final View mRootView;
	private final ChipLayout mChipLayout;
	private final RainbowProgressCircleView mProgress;
	private final TextView mError;
	private final List<String> mSuggestedTags = new ArrayList<>();
	private final Subject<Unit> mItemClicks = PublishSubject.create();

	private boolean mIsAutocompleting;
	private long mRetryStartedAt;

	/** This is just held onto for error reporting purposes internally. Not intended for other use. */
	private JsonNode mTagResponseJson;
	private int mSuggestTagsInitialCount;
	
	public SuggestedTagsModule(String itemUrl, TagModuleManager manager, VisibilityListener visListener, Context context) {
		super(manager, visListener, context);
		
		mItemUrl = itemUrl;
		mRootView = LayoutInflater.from(getContext())
						.inflate(R.layout.view_suggested_tags, null, false);
		mRootView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		
		mError = mRootView.findViewById(R.id.error);
		mError.setOnClickListener(v -> {
			mRetryStartedAt = System.currentTimeMillis();
			fetchSuggestedTags(); // Retry load
		});
		
		mChipLayout = mRootView.findViewById(R.id.suggested_tags);
		mChipLayout.setOnItemClickListener(this);
		mChipLayout.setAdapter((text, parent) -> {
			SuggestedTagView chip = new SuggestedTagView(getContext());
			chip.setText(text);
			return chip;
		});
		
		mChipLayout.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
			
			@Override
			public void onChildViewRemoved(View parent, View child) {
				invalidateVisibility();
			}
			
			@Override
			public void onChildViewAdded(View parent, View child) {}
		});
		
		mProgress = mRootView.findViewById(R.id.progress);
		
		mChipLayout.setVisibility(View.GONE);
		invalidateVisibility();
	}
	
	@NonNull @Override
	public View getView() {
		return mRootView;
	}
	
	@Override
	public void onItemClick(ChipLayout parent, View view, int position) {
		String tag = ((TextView)view).getText().toString();
		getManager().addTag(this, tag);
		parent.removeChipAt(parent.indexOfChild(view));
		mItemClicks.onNext(Unit.INSTANCE);
	}

	public Observable<Unit> getItemClicks() {
		return mItemClicks;
	}

	@Override
	public void load(final TagModuleLoadedListener callback) {
		fetchSuggestedTags();
		callback.onTagModuleLoaded();
	}

	/**
	 * Return the list of tags suggested by the server.
	 * @return
	 */
	public List<String> getTags() {
		return mSuggestedTags;
	}
	
	public int getSuggestTagsInitialCount() {
		return mSuggestTagsInitialCount;
	}
	
	private void fetchSuggestedTags() {
		if (!App.from(getContext()).pktcache().hasFeature(PremiumFeature.SUGGESTED_TAGS)) {
			return;
		}
		
		mChipLayout.setVisibility(View.GONE);
		mError.setVisibility(View.GONE);
		mProgress.setVisibility(View.VISIBLE);
		((ViewGroup) mChipLayout.getParent()).setMinimumHeight(getContext().getResources().getDimensionPixelSize(R.dimen.suggested_tags_loading_min_height));
		invalidateVisibility();
		
		Pocket pocket = App.from(getContext()).pocket();
		pocket.sync(pocket.spec().things().suggestedTags().url(mItemUrl).build())
				.onSuccess(t -> {
					mSuggestedTags.clear();
					mTagResponseJson = null;
					
					for (SuggestedTag tag : t.suggested_tags) {
						mSuggestedTags.add(tag.tag);
					}
					
					mSuggestTagsInitialCount = mSuggestedTags.size();
					
					if (mSuggestedTags.isEmpty()) {
						showEmpty();
						
					} else {
						// Show tags
						mChipLayout.removeAllChips();
						for (String tag : mSuggestedTags) {
							mChipLayout.addChip(tag);
						}
						
						((ViewGroup) mChipLayout.getParent()).setMinimumHeight(0); // Remove the min height. The min height was only used to hold the space while loading to avoid the height of the area shifting after loading in the common case of having two rows worth of tags. If only one row is available, this will now be allowed to shrink.
						mChipLayout.setVisibility(View.VISIBLE);
						mProgress.setVisibility(View.GONE);
						mError.setVisibility(View.GONE);
						
						mTagResponseJson = t.toJson(V3Source.JSON_CONFIG);
					}
				})
				.onFailure(e -> {
					mSuggestedTags.clear();
					mTagResponseJson = null;
					
					long remainingMinRetryTime = (mRetryStartedAt + MIN_RETRY_LOADING_TIME) - System.currentTimeMillis();
					mRetryStartedAt = 0;
					if (remainingMinRetryTime > 0) {
						// Delay, see comment on MIN_RETRY_LOADING_TIME for more details.
						app().threads().getHandler().postDelayed(() -> showError(e), remainingMinRetryTime);
					} else {
						// Can handle now
						showError(e);
					}
				});
	}
	
	private void showError(SyncException e) {
		mError.setVisibility(View.VISIBLE);
		mProgress.setVisibility(View.GONE);
		mChipLayout.setVisibility(View.GONE);
		
		if (ApiException.unwrapXErrorCode(e) == X_ERROR_SUGGESTED_TAGS_NO_TAGS) {
			// This error code means it's actually an empty state.
			// We override the error message from API, because we can show something more specific.
			showEmpty();
			return;

		} else if (!App.from(mChipLayout.getContext()).http().status().isOnline()) {
			mError.setText(R.string.suggested_tags_no_connection);
		} else if (e.getUserFacingMessage() != null) {
			mError.setText(e.getUserFacingMessage());
		} else {
			mError.setText(R.string.suggested_tags_unknown_error);
		}
		
		invalidateVisibility();
	}

	private void showEmpty() {
		// We have different error messages depending on whether the user has tagged before or not.
		Pocket pocket = App.from(getContext()).pocket();
		pocket.syncLocal(pocket.spec().things().tags().build())
				.onSuccess(tags -> {
					if (tags.tags == null || tags.tags.isEmpty()) {
						mError.setText(R.string.suggested_tags_empty_no_tags);
					} else {
						mError.setText(R.string.suggested_tags_empty_none_found);
					}
					invalidateVisibility();
				})
				.onFailure(ex -> {
					mError.setText(R.string.suggested_tags_empty_none_found);
					invalidateVisibility();
				});
	}

	@Override
	public void onTagInputTextChanged(CharSequence text) {
		mIsAutocompleting = !TextUtils.isEmpty(text);
		invalidateVisibility();
	}
	
	public void invalidateVisibility() {
		final boolean visible;
		if (!App.from(getContext()).pktcache().hasFeature(PremiumFeature.SUGGESTED_TAGS)) {
			visible = false;
		} else if (mIsAutocompleting) {
			visible = false;
		} else if (mError.getVisibility() == View.VISIBLE
			   || mProgress.getVisibility() == View.VISIBLE
			   || (mChipLayout.getVisibility() == View.VISIBLE && mChipLayout.getChipCount() > 0)) {
			visible = true;
		} else {
			visible = false;
		}
		
		setPreferredVisibility(visible);
	}

	@Override
	public void onTagAdded(String tag) {
		mChipLayout.removeChip(tag);
	}

	@Override
	public void onTagRemoved(String tag) {
		// If they picked a suggested tag, and then removed it, we probably don't need to suggest it again.
	}
	
	@Override
	public void onPremiumStatusChanged() {
		boolean hasPremiumTags = App.from(getContext()).pktcache().hasFeature(PremiumFeature.SUGGESTED_TAGS);
		invalidateVisibility();
		if (hasPremiumTags) {
			fetchSuggestedTags();
		} else {
			mSuggestedTags.clear();
			mTagResponseJson = null;
		}
	}
	
	/** This just for internal error reporting purposes. Not intended for other use. May be null. */
	public JsonNode getJsonData() {
		return mTagResponseJson;
	}
	
}
