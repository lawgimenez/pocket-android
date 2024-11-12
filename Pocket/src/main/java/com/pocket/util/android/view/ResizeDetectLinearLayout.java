package com.pocket.util.android.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.ideashower.readitlater.R;
import com.pocket.app.App;
import com.pocket.util.android.drawable.StatefulPaint;

public class ResizeDetectLinearLayout extends LinearLayout implements ResizeDetectView, ForegroundDrawableHelper.Setter, MaxWidthHelper.MaxWidthView {

    private final ForegroundDrawableHelper mForegroundDrawable = new ForegroundDrawableHelper(this);
    private final StatefulPaint mDividerPaint = new StatefulPaint();
	private final MaxWidthHelper mMaxWidth;
	
	private OnResizeListener mListener;
    private int mDividerInset;
    private boolean mDrawDivider;

	public ResizeDetectLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(attrs);
		mMaxWidth = new MaxWidthHelper(context, attrs);
	}

	public ResizeDetectLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(attrs);
		mMaxWidth = new MaxWidthHelper(context, attrs, defStyleAttr);
	}

	public ResizeDetectLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init(attrs);
		mMaxWidth = new MaxWidthHelper(context, attrs, defStyleAttr, defStyleRes);
	}

	public ResizeDetectLinearLayout(Context context) {
		super(context);
		init(null);
		mMaxWidth = new MaxWidthHelper();
	}

	private void init(AttributeSet attrs) {
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ResizeDetectLinearLayout);

			ColorStateList dividerColor = a.getColorStateList(R.styleable.ResizeDetectLinearLayout_dividerColor);
			int stroke = a.getDimensionPixelSize(R.styleable.ResizeDetectLinearLayout_dividerStroke, 0);
			int inset = a.getDimensionPixelSize(R.styleable.ResizeDetectLinearLayout_dividerInset, 0);
			if (dividerColor != null) {
				setDividerStroke(dividerColor, stroke, inset);
			}
			
			setForegroundDrawable(a.getDrawable(R.styleable.ResizeDetectLinearLayout_android_foreground));

			a.recycle();
		}
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

	@Override
    protected int[] onCreateDrawableState(int extraSpace) {
		final int[] state = super.onCreateDrawableState(extraSpace + 1);
		mergeDrawableStates(state, App.from(getContext()).theme().getState(this));
		return state;
	}
	
	public void setDividerStroke(ColorStateList color, int stroke, int inset) {
		mDrawDivider = true;
		mDividerPaint.setStatefulColor(color, getDrawableState());
		mDividerPaint.setStyle(Style.STROKE);
		mDividerPaint.setStrokeWidth(stroke);
		mDividerInset = inset;
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		
		if (mDrawDivider) {
			mDividerPaint.setState(getDrawableState());
			int size = getChildCount();
			
			for (int i = 1; i < size; i++) {
				View child = getChildAt(i);
				View leftChild = getChildAt(i-1);
				if (getOrientation() == HORIZONTAL) {
					if (leftChild!=null && leftChild.getVisibility() == VISIBLE) {
						float x = child.getLeft();
						canvas.drawLine(x, mDividerInset, x, getHeight()-mDividerInset, mDividerPaint);
					}					
				} else {
					float y = child.getTop();
					canvas.drawLine(mDividerInset, y, getWidth()-mDividerInset, y, mDividerPaint);
				}
			}
		}

        mForegroundDrawable.onParentDispatchDraw(canvas);
	}

	@Override
    public void setForegroundDrawable(Drawable drawable) {
        mForegroundDrawable.setForegroundDrawable(drawable);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
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
	
    @Override
	public int getMaxWidth() {
		return mMaxWidth.getMaxWidth();
	}
	
	@Override
	public void setMaxWidth(int maxWidth) {
		mMaxWidth.setMaxWidth(maxWidth);
	}
	
	@Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = mMaxWidth.onMeasure(widthMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
}
