package com.pocket.util.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.sdk.util.AbsPocketFragment;

import androidx.annotation.NonNull;

public class ResizeDetectRelativeLayout extends RelativeLayout implements ResizeDetectView, ForegroundDrawableHelper.Setter, MaxWidthHelper.MaxWidthView {

	private final MaxWidthHelper mMaxWidth;

    private ForegroundDrawableHelper mForegroundDrawable = new ForegroundDrawableHelper(this);

    private OnResizeListener mListener;

	private AbsPocketFragment mFrag;

	private int mMaxHeight;

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public ResizeDetectRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		mMaxWidth = new MaxWidthHelper(context, attrs, defStyleAttr, defStyleRes);
	}

	public ResizeDetectRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mMaxWidth = new MaxWidthHelper(context, attrs, defStyle);
	}

	public ResizeDetectRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mMaxWidth = new MaxWidthHelper(context, attrs);

		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PocketTheme);
		mMaxHeight = a.getDimensionPixelSize(R.styleable.PocketTheme_maxHeight, 0);
		a.recycle();
	}

	public ResizeDetectRelativeLayout(Context context) {
		super(context);
		mMaxWidth = new MaxWidthHelper();
	}

	@Override
    public void setForegroundDrawable(Drawable drawable) {
        mForegroundDrawable.setForegroundDrawable(drawable);
    }

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		if (mListener != null) { 
			mListener.onViewSizeChanged(this, w, h, oldw, oldh);
		}

        mForegroundDrawable.onParentSizeChanged(w, h, oldw, oldh);
	}

	@Override
	public void setOnResizeListener(OnResizeListener listener) {
		mListener = listener;
	}
	
	public void setFrag(AbsPocketFragment frag) {
		mFrag = frag;
	}
	
	@Override
    protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, App.from(getContext()).theme().getState(this, mFrag));
		return state;
	}
	
	@Override
	public int getMaxWidth() {
		return mMaxWidth.getMaxWidth();
	}
	
	@Override
	public void setMaxWidth(int maxWidth) {
		mMaxWidth.setMaxWidth(maxWidth);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = mMaxWidth.onMeasure(widthMeasureSpec);
        
		int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode);
        }
		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        mForegroundDrawable.onParentDispatchDraw(canvas);
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || (mForegroundDrawable != null && mForegroundDrawable.onParentVerifyDrawable(who));
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        mForegroundDrawable.onParentJumpDrawablesToCurrentState();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mForegroundDrawable.onParentTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mForegroundDrawable.onParentDrawableStateChanged();
    }
}
