package com.pocket.app.settings.view.preferences;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.app.settings.cache.CacheLimitSeekbar;
import com.pocket.app.settings.cache.CacheSettingsFragment;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.sdk.offline.cache.Assets.OnCacheSizeChangedListener;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;
import com.pocket.util.java.BytesUtil;
import com.squareup.phrase.Phrase;

import org.apache.commons.lang3.StringUtils;

/**
 * Used by {@link CacheSettingsFragment} to let the user adjust the cache limit.
 */
public class CacheLimitPreferenceView extends VisualMarginConstraintLayout {
	
	public static final int UNLIMITED = 0;
	
	private final CharSequence mMB = App.getStringResource(R.string.setting_cache_mb);
	private final CharSequence mGB = App.getStringResource(R.string.setting_cache_gb);
	
	private final TextView mCurrentCacheSize;
	private final TextView mItemCachedTextView;
	private final TextView mLimitToValue;
	private final CacheLimitSeekbar mSeekbar;
	private final Assets assets;
	
	private OnCacheLimitChangedListener mOnCacheLimitChangedListener;
	private String mItemOrder;
	private final OnCacheSizeChangedListener mAssetsCacheSizeListener;
	
	public CacheLimitPreferenceView(Context context) {
		super(context);

		LayoutInflater.from(context)
			.inflate(R.layout.view_pref_cache_limit, this);

		mSeekbar = findViewById(R.id.seekbar);
		mCurrentCacheSize = findViewById(R.id.current_cache_limit_textview);
		mItemCachedTextView = findViewById(R.id.current_items_cached_textview);
		mLimitToValue = findViewById(R.id.value_limit);
		
		mSeekbar.setOnIncrementedMbProgressChangedListener((snappedMb, bytes, isDragging) -> {
            // Update dependent text views
            updateLimitToTextView(snappedMb);
            updateCacheDescriptionTextView(bytes);

            // Only push up the change while not dragging the progress bar thumb.
            if (!isDragging) {
                if (mOnCacheLimitChangedListener != null) {
                    mOnCacheLimitChangedListener.onCacheLimitChanged(bytes);
                }
            }
        });


        int horizontalPadding = (int)getContext().getResources().getDimension(R.dimen.pref_inner_cell_padding_horizontal);
        mSeekbar.setPadding(horizontalPadding, mSeekbar.getPaddingTop(), horizontalPadding, mSeekbar.getPaddingBottom());

        assets = App.from(getContext()).assets();
		mAssetsCacheSizeListener = new CacheSizeListener();
		assets.addOnCacheSizeChangedListener(mAssetsCacheSizeListener);
		mAssetsCacheSizeListener.onCacheSizeChanged(assets.getCacheSize());

		engageable.setUiEntityType(Type.BUTTON); // TODO type for slider?
	}
	
	/**
	 * Set the what to show the currently selected limit as.
	 * @param bytes The number of bytes or {@link #UNLIMITED}
	 */
	public void setLimit(long bytes) {
		mSeekbar.setProgressInBytes(bytes); 
	}
	
	/**
	 * @return The user's set limit in bytes or {@link #UNLIMITED}
	 */
	public long getLimit() {
		return mSeekbar.getProgressInBytes();
	}
	
	/**
	 * @param value The value to display in the approx item count sentence to describe the download priority.
	 */
	public void setItemOrder(String value) {
		mItemOrder = value;
		updateCacheDescriptionTextView(mSeekbar.getProgressInBytes());
	}
	
	public void setOnCacheLimitChangedListener(OnCacheLimitChangedListener listener) {
		mOnCacheLimitChangedListener = listener;
	}
	
	public void releaseResources() {
		assets.removeOnCacheSizeChangedListener(mAssetsCacheSizeListener);
	}
	
	private void updateCacheDescriptionTextView(double cacheSize) {
		if (cacheSize <= CacheLimitPreferenceView.UNLIMITED) {
			mItemCachedTextView.setVisibility(View.INVISIBLE);
			
		} else {
			mItemCachedTextView.setVisibility(View.VISIBLE);
			CharSequence itemCountCharSequence  = Phrase.from(getContext(), R.string.setting_cache_approx_item_count)
					.put("item_count", String.valueOf(convertCacheSizeToItems(cacheSize)))
					.put("newest_or_oldest", StringUtils.defaultIfBlank(mItemOrder, "")) // OK to return blank if mItemOrder hasn't been set yet. It will be soon.
					.format();
			mItemCachedTextView.setText(itemCountCharSequence);			
		}
	}
	
	private void updateLimitToTextView(int snappedMb) {
		if (snappedMb <= UNLIMITED) {
			mLimitToValue.setText(R.string.setting_cache_unlimited);
		} else {
			String value;
			if (snappedMb < 1000) {
				value = snappedMb + " " + mMB;
			} else {
				value = String.format("%.1f", (snappedMb / BytesUtil.KB)) + " " + mGB;
			}
			mLimitToValue.setText(value);
		}
	}
	
	private int convertCacheSizeToItems(double cacheSize) {
		cacheSize -= Assets.CACHE_BUFFER;
		
		long avergeSize = BytesUtil.getAverageBytesPerItem();
		int itemNumber = (int) (cacheSize/avergeSize);
		int roundToIncrementsOf = 5;
		
		return (int) (Math.floor(itemNumber / roundToIncrementsOf) * roundToIncrementsOf);
	}

    private class CacheSizeListener implements Assets.OnCacheSizeChangedListener {
		
		@Override
		public void onCacheSizeChanged(final long size) {
			App.getApp().threads().runOrPostOnUiThread(() -> {
				if (assets.isCacheFull()) {
					// Cache needs to be trimmed or is trimming. Show an in progress message.
					mCurrentCacheSize.setVisibility(View.VISIBLE);
					mCurrentCacheSize.setText(R.string.setting_cache_currently_adjusting);
					
				} else if (assets.isCacheLimitSet()) {
					// If we are within the limit, no need to provide any further details about the usage. With the buffer it will not be exactly their limit so we don't want to be overly specific here to avoid confusion.
					mCurrentCacheSize.setVisibility(View.INVISIBLE);
					
				} else {
					// No limit set. Show the current cache size
					mCurrentCacheSize.setVisibility(View.VISIBLE);
					
					setDisplayValue(size + BytesUtil.mbToBytes(18 + 6 + 5), // Add on the rough app size, premium fonts size and some extra space for other databases and misc files.
							R.string.setting_cache_currently_using);
				}
			});
		}
		
		private void setDisplayValue(long bytes, int res) {
			float displayValue;
			CharSequence unit;
			if (bytes >= BytesUtil.gbToBytes(1)) {
				displayValue = (float) Math.round(BytesUtil.bytesToGb(bytes) * 100) / 100; // Round GB to 2 decimal places
				unit = mGB;
				
			} else {
				double mb = BytesUtil.bytesToMb(bytes);
				displayValue = Math.round(mb); // Round MB
				unit = mMB;
			}
			
			String value;
			if (unit.equals(mMB)) {
				value = String.valueOf((int) displayValue);
			} else {
				value = String.format("%.2f", displayValue);
			}
			value = value + " " + unit;
			
			CharSequence usingDiskCharSequence = Phrase.from(getContext(), res)
					.put("disk_space_size", value)
					.format();
			
			mCurrentCacheSize.setText(usingDiskCharSequence);
		}
		
	}

	public interface OnCacheLimitChangedListener {
		/**
		 * The user has changed the limit control.
		 * @param bytes The user's set limit in bytes or {@link #UNLIMITED}
		 */
		void onCacheLimitChanged(long bytes);
	}
	
}
