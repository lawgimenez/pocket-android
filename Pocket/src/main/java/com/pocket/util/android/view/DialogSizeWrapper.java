package com.pocket.util.android.view;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.WindowManager;

import com.ideashower.readitlater.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.view.button.ButtonBoxDrawable;
import com.pocket.ui.view.themed.AppThemeUtil;
import com.pocket.ui.view.themed.ThemedFrameLayout;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.WindowManagerUtil;

/**
 * Handles the logic of creating a dialog size that fits the screen well
 */
public class DialogSizeWrapper extends ThemedFrameLayout {
	
	private float mMaxWidthPx;
	private float mMaxHeight;
	
	private boolean mCapHeight = true;
	private int mPad = DimenUtil.dpToPxInt(getContext(), 1);

	public DialogSizeWrapper(Context context) {
        super(context);
        setMaxWidth(getResources().getDimension(R.dimen.dialog_max_width));
        setMaxHeight(getResources().getDimension(R.dimen.dialog_max_height));
        setBackgroundDrawable(new ButtonBoxDrawable(context, com.pocket.ui.R.color.pkt_bg, R.color.add_overlay_free_stroke));
    }

    public DialogSizeWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DialogSizeWrapper);
		setMaxWidth(a.getDimension(R.styleable.DialogSizeWrapper_max_width, getResources().getDimension(R.dimen.dialog_max_width)));
		setMaxHeight(getResources().getDimension(R.dimen.dialog_max_height));
		a.recycle();
        setBackgroundDrawable(new ButtonBoxDrawable(context, com.pocket.ui.R.color.pkt_bg, R.color.add_overlay_free_stroke));
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int state = AppThemeUtil.getState(this)[0];
        if (state == com.pocket.ui.R.attr.state_light) {
            setPadding(0, 0, 0, 0);
        } else if (state == com.pocket.ui.R.attr.state_dark) {
            setPadding(mPad, mPad, mPad, mPad);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		WindowManager windowManager = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE));
		int displayWidth = WindowManagerUtil.getScreenWidth(windowManager);
        int displayHeight = WindowManagerUtil.getScreenHeight(windowManager);
        
        Resources res = getResources();
        int minPadding = (int) res.getDimension(R.dimen.dialog_min_padding);
        int maxWidth = (int) mMaxWidthPx;
        int maxHeight = (int) mMaxHeight;
        
        maxWidth = Math.min(displayWidth - minPadding, maxWidth);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int measuredWidth = measure(widthMode, widthSpecSize, maxWidth, true); // OPT use the min values
        int adjustedWidthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, widthMode);

		maxHeight = Math.min(displayHeight - minPadding, maxWidth);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        int measuredHeight = measure(heightMode, heightSpecSize, maxHeight, mCapHeight);
        int adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, heightMode);
        
        super.onMeasure(adjustedWidthMeasureSpec, adjustedHeightMeasureSpec);
    }

    private int measure(int specMode, int specSize, int max, boolean capToMax) {
        int result = specSize;
        
        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
            
        } else {
        	if (capToMax) {
        		result = Math.min(max, result); // Must be <= than max
        	}
        	
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
    }
    
    public void setMaxHeight(float height) {
    	mMaxHeight = height;
    	requestLayout();
    	invalidate();
    }
    
    public void setMaxWidth(float dp) {
    	mMaxWidthPx  = FormFactor.dpToPx(dp);
    	requestLayout();
    	invalidate();
    }

}
