package com.pocket.app.settings.cache;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.SeekBar;

import com.pocket.app.settings.view.preferences.CacheLimitPreferenceView;
import com.pocket.sdk.offline.cache.Assets;
import com.pocket.ui.view.settings.PocketSeekBar;
import com.pocket.util.java.BytesUtil;

/**
 * <i>Part of {@link CacheLimitPreferenceView}.</i>
 * <p>
 * A {@link SeekBar} that seeks between {@link Assets#CACHE_LIMIT_MIN} and {@link Assets#CACHE_LIMIT_MAX} in clean
 * increments of megabytes.
 * <p>
 * At the far end of the seekbar, it switches to {@link CacheLimitPreferenceView#UNLIMITED} to represent the user does not want to
 * limit the cache size.
 * <p>
 * Get a display value in megabytes in the nearest clean increment with {@link #getProgressInSnappedMb()}. Get the bytes
 * of that value instead with {@link #getProgressInBytes()}.
 * <p>
 * Set the seekbar to a certain byte count with {@link #setProgressInBytes(long)}.
 * <p>
 * Register a listener for when the seekbar progress changes in increments with {@link #setOnIncrementedMbProgressChangedListener(OnIncrementedMbProgressChangedListener)}. 
 */
public class CacheLimitSeekbar extends PocketSeekBar {

	private static final int INCREMENT_LOW_MB = 50;
	private static final int INCREMENT_HIGH_MB = 100;
	private static final int INCREMENT_HIGH_START_MB = 500;
	private static final int MAX_MB = (int) (Assets.CACHE_LIMIT_MAX / BytesUtil.MB);
	private static final int MIN_MB = (int) (Assets.CACHE_LIMIT_MIN / BytesUtil.MB);
	private static final float SCROLLABLE_AREA = 0.95f;
	private static final SeekInterpolator ACC_INTERPOLATOR = new SeekInterpolator(0.7f);
	private static final int PROGRESS_MAX = 1000;
	
	private OnIncrementedMbProgressChangedListener mListener;
	private int mLastSnappedMb = -1;
	protected boolean mIsTracking;
	
	public CacheLimitSeekbar(Context context) {
		super(context);
		init();
	}

	public CacheLimitSeekbar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public CacheLimitSeekbar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		setMax(PROGRESS_MAX);
		
		setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mIsTracking = false;
				
				int snappedMb = getProgressInSnappedMb();
				if (snappedMb <= CacheLimitPreferenceView.UNLIMITED
				&& getProgress() < getMax()) {
					// Snap Unlimited to far right
					setProgress(getMax());
				} else {
					if (mListener != null) {
						mListener.onIncrementedMbProgressChanged(snappedMb, getProgressInBytes(), mIsTracking);
					}
				}
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mIsTracking = true;
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int snappedMb = convertProgressToSnappedMb(progress);
				
				if (mLastSnappedMb != snappedMb) {
					mLastSnappedMb = snappedMb;
					if (mListener != null) {
						mListener.onIncrementedMbProgressChanged(snappedMb, getProgressInBytes(), mIsTracking);
					}
				}
			}
		});
	}
	
	/**
	 * Register a {@link OnIncrementedMbProgressChangedListener}. Automatically triggers the listener, if not null,
	 * with the current progress values.
	 *  
	 * @param listener
	 */
	public void setOnIncrementedMbProgressChangedListener(OnIncrementedMbProgressChangedListener listener) {
		mLastSnappedMb = -1; // Reset so next update always goes to listener
		mListener = listener;
		if (mListener != null) {
			mListener.onIncrementedMbProgressChanged(getProgressInSnappedMb(), getProgressInBytes(), mIsTracking);
		}
	}
	
	/**
	 * Set the progress of the seekbar by bytes. Opposite of {@link #getProgressInBytes()}.
	 * @param bytes
	 */
	public void setProgressInBytes(long bytes) {
		setProgress(convertBytesToProgress(bytes));
	}
	
	/**
	 * Returns the {@link #getProgressInSnappedMb()} in bytes instead of mb.
	 */
	public long getProgressInBytes() {
		return convertProgressToBytes(getProgress());
	}
	
	/**
	 * @return The closest clean increment in megabytes. For example, if the progress value is 123 MB, it may return 100 MB as a clean/rounded value.
	 */
	public int getProgressInSnappedMb() {
		return convertProgressToSnappedMb(getProgress());
	}
	
	/**
	 * Converts the seekbar's units [0-{@link #getMax()}] to the closest clean increment in mb or {@link CacheLimitPreferenceView#UNLIMITED}.
	 */
	private int convertProgressToSnappedMb(int progress) {
		float interpolatedPercent = ACC_INTERPOLATOR.getInterpolation((float)progress / getMax()); // [0-1]
		if (interpolatedPercent >= SCROLLABLE_AREA) {
			return CacheLimitPreferenceView.UNLIMITED;
			
		} else {
			interpolatedPercent = interpolatedPercent / SCROLLABLE_AREA; // [0-1] within the scrollable area. Excluding the threshold for unlimited at the end.
			float mbExact = ((MAX_MB - MIN_MB) * interpolatedPercent) + MIN_MB;
			
			int snapIncrement;
			if (mbExact >= INCREMENT_HIGH_START_MB) {
				snapIncrement = INCREMENT_HIGH_MB;
			} else {
				snapIncrement = INCREMENT_LOW_MB;
			}
			
			return Math.round(mbExact / snapIncrement) * snapIncrement;
		}
	}
	
	/**
	 * Same as {@link #convertProgressToSnappedMb(int)} but returned as bytes instead of mb.
	 */
	private long convertProgressToBytes(int progress) {
		return BytesUtil.mbToBytes(convertProgressToSnappedMb(progress));
	}
	
	/**
	 * Converts bytes to the seekbars progress units [0-{@link #getMax()}] needed to put the thumb into the correct location.
	 * @param bytes
	 * @return
	 */
	private int convertBytesToProgress(long bytes) {
		if (bytes <= CacheLimitPreferenceView.UNLIMITED) {
			return getMax();
			
		} else {
			int mb = (int) (bytes / BytesUtil.MB);
			if (mb < MIN_MB) {
				mb = MIN_MB;
			}
			
			float percent = (mb - MIN_MB) / (float) (MAX_MB - MIN_MB); // Percentage within range
			percent *= SCROLLABLE_AREA; // Adjust for the area at the end of the progress bar reserved for unlimited 
			percent = ACC_INTERPOLATOR.getReversedInterpolation(percent); // Apply reverse interpolation of ACC_INTERPOLATOR
			return (int) (percent * getMax()); // Finally convert to the units used by the seek bar
		}
	}
	
	public interface OnIncrementedMbProgressChangedListener {
		/**
		 * Invoked when {@link CacheLimitSeekbar}'s progress value changes and/or when a drag/touch event ends.
		 * 
		 * @param snappedMb See {@link CacheLimitSeekbar#getProgressInSnappedMb()}
		 * @param bytes See {@link CacheLimitSeekbar#getProgressInBytes()}
		 * @param isDragging true if the user is touching/dragging the thumb
		 */
		public void onIncrementedMbProgressChanged(int snappedMb, long bytes, boolean isDragging);
	}
	
	/**
	 * Basically a copy of {@link AccelerateInterpolator} so we can add a {@link #getReversedInterpolation(float)} method
	 * more clearly.
	 */
	private static class SeekInterpolator implements Interpolator {
		
	    private final float mFactor;
	    private final double mDoubleFactor;

	    /**
	     * Constructor
	     * 
	     * @param factor Degree to which the interpolation should be eased. Seting
	     *        factor to 1.0f produces a y=x^2 parabola. Increasing factor above
	     *        1.0f  exaggerates the ease-in effect (i.e., it starts even
	     *        slower and ends evens faster)
	     */
	    public SeekInterpolator(float factor) {
	        mFactor = factor;
	        mDoubleFactor = 2 * mFactor;
	    }
	   
	    @Override
		public float getInterpolation(float input) {
	        if (mFactor == 1.0f) {
	            return input * input;
	        } else {
	            return (float)Math.pow(input, mDoubleFactor);
	        }
	    }
	    
	    /**
	     * The opposite of {@link #getInterpolation(float)}
	     * @param value
	     * @return
	     */
	    public float getReversedInterpolation(float value) {
			if (mFactor == 1.0f) {
				return (float) Math.sqrt(value);
			} else {
				return (float) Math.pow(value, 1.0f / mDoubleFactor);
			}
		}
	    
	}
	
}
