package com.pocket.ui.view.progress;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.ui.util.IntrinsicSizeHelper;
import com.pocket.ui.util.NestedColorStateList;
import com.pocket.util.android.animation.Interpolators;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Random;

public class RainbowProgressCircleView extends View implements ValueAnimator.AnimatorUpdateListener {
	
	private static final long SWEEP_SPEED = 1250;
	private static final long ROTATION_SPEED = 1750;
	/** The percentage of the view's height that gives you the stroke width */
	private static final float STROKE_RATIO = 0.08f;
	
	private final IntrinsicSizeHelper mIntrinsicSizeHelper = new IntrinsicSizeHelper(DimenUtil.dpToPxInt(getContext(), 55));
	private final RectF mBounds = new RectF();
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Random mRandom = new Random();
	
	private ColorStateList[] mColorStateLists = new ColorStateList[] {
			NestedColorStateList.get(getContext(), R.color.pkt_themed_teal_4),
			NestedColorStateList.get(getContext(), R.color.pkt_themed_teal_3),
			NestedColorStateList.get(getContext(), R.color.pkt_themed_coral_2),
			NestedColorStateList.get(getContext(), R.color.pkt_themed_amber_1)
	};
	
	private boolean mIsStarting;
	private ValueAnimator mRotationAnimator;
	private ValueAnimator mSweepAngleAnimator;
	private ValueAnimator mProgressAnimator;
	private int mCurrentPrimaryColorIndex;
	private float mLastSweepAngle;
	
	private boolean mIsIndeterminate = true;
	private boolean mStartAsArc = true;
	private boolean mIsAttached;
	private float mProgress;
	
	public RainbowProgressCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initAttrs(attrs, defStyleAttr);
	}

	public RainbowProgressCircleView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initAttrs(attrs, 0);
	}

	public RainbowProgressCircleView(Context context) {
		super(context);
		initAttrs(null, 0);
	}
	
	private void initAttrs(AttributeSet attrs, int defStyleAttr) {
		if (isInEditMode()) {
			return;
		}
		
		if (attrs != null) {
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.RainbowProgressCircleView, defStyleAttr, 0);
			if (a.getBoolean(R.styleable.RainbowProgressCircleView_progressColorsExcludeCoral, false)) {
				mColorStateLists = ArrayUtils.remove(mColorStateLists, 2);
			}
			mStartAsArc = a.getBoolean(R.styleable.RainbowProgressCircleView_progressStartAsArc, true);
			a.recycle();
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		widthMeasureSpec = mIntrinsicSizeHelper.applyWidth(widthMeasureSpec);
		heightMeasureSpec = mIntrinsicSizeHelper.applyHeight(heightMeasureSpec);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	public void setProgress(float progress) {
		setProgressIndeterminate(false);
		
		float start = mProgress;
		if (mProgressAnimator != null) {
			start = ((Float) mProgressAnimator.getAnimatedValue()).floatValue();
			mProgressAnimator.cancel();
		}
		
		mProgressAnimator = ValueAnimator.ofFloat(start, progress); // TODO can we just change the values instead of creating a new animator?
		mProgressAnimator.setInterpolator(Interpolators.DECEL);
		mProgressAnimator.setDuration(400);
		mProgressAnimator.start();
		mProgress = progress;
		
		invalidate();
	}
	 
	public void setProgressIndeterminate(boolean indeterminate) {
		mIsIndeterminate = indeterminate;
		invalidate();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mIsAttached = true;
		updateAnimationStatus();
	}
	
	private ValueAnimator newDegreesAnimator(long duration) {
		ValueAnimator animator = ValueAnimator.ofFloat(0, 360);
		animator.setDuration(duration);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.RESTART);
		animator.setInterpolator(new LinearInterpolator());
		return animator;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mIsAttached = false;
		updateAnimationStatus();
	}
	
	private void startAnimation() {
		if (mSweepAngleAnimator != null) {
			return; // Already running.
		}
		
		mPaint.setStyle(Style.STROKE);
		
		mIsStarting = mStartAsArc;
		mCurrentPrimaryColorIndex = mRandom.nextInt(mColorStateLists.length);
		
		mSweepAngleAnimator = newDegreesAnimator(SWEEP_SPEED);
		mRotationAnimator = newDegreesAnimator(ROTATION_SPEED);
		
		mSweepAngleAnimator.addUpdateListener(this);
		mSweepAngleAnimator.start();
		mRotationAnimator.start();
	}
	
	private void cancelAnimation() {
		if (mSweepAngleAnimator == null) {
			return; // Already canceled
		}
		mSweepAngleAnimator.removeAllUpdateListeners();
		mSweepAngleAnimator.cancel();
		mRotationAnimator.cancel();
		
		mSweepAngleAnimator = null;
		mRotationAnimator = null;
	}
	
	@Override
	public void setVisibility(int visibility) {
		super.setVisibility(visibility);
		updateAnimationStatus();
	}
	
	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		updateAnimationStatus();
	}
	
	@Override
	public void onAnimationUpdate(ValueAnimator animator) {
		invalidate();
	}
	
	private boolean updateAnimationStatus() {
		if (getVisibility() == View.VISIBLE && mIsAttached && isShown()) {
			startAnimation();
			return true;
		} else {
			cancelAnimation();
			return false;
		}
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		mPaint.setStrokeWidth(h * STROKE_RATIO);
		mBounds.set(0, 0, w, h);
		mBounds.inset(mPaint.getStrokeWidth()/2, mPaint.getStrokeWidth()/2);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mSweepAngleAnimator == null) {
			return;
		}
		
		float sweepAngle = ((Float) mSweepAngleAnimator.getAnimatedValue()).floatValue();
		float rotate = ((Float) mRotationAnimator.getAnimatedValue()).floatValue();
		
		if (!mIsIndeterminate) {
			float progress = ((Float) mProgressAnimator.getAnimatedValue()).floatValue();
			sweepAngle = Math.max(progress * 360, 5);
		}
		
		if (sweepAngle < mLastSweepAngle) {
			// It reset/repeated
			mIsStarting = false;
			mCurrentPrimaryColorIndex++;
			if (mCurrentPrimaryColorIndex >= mColorStateLists.length) {
				mCurrentPrimaryColorIndex = 0;
			}
			
			// Recheck if still visible
			boolean stillRunning = updateAnimationStatus();
			if (!stillRunning) {
				return;
			}
		}
		
		canvas.save();
		canvas.rotate(rotate, mBounds.centerX(), mBounds.centerY());
		
		mPaint.setColor(mColorStateLists[mCurrentPrimaryColorIndex].getColorForState(getDrawableState(), Color.TRANSPARENT));
		canvas.drawArc(mBounds, 0, sweepAngle, false, mPaint); // For progress mode (!mIsIndeterminate) this is the progress so far
		
		// Draw the rest of the circle
		boolean fillRestOfCircle = !mIsStarting || !mIsIndeterminate;
		if (fillRestOfCircle) {
			int color;
			int alpha;
			if (mIsIndeterminate) {
				// Get another color to draw along side the primary/growing one.
				int secondaryIndex = mCurrentPrimaryColorIndex - 1;
				if (secondaryIndex < 0) {
					secondaryIndex = mColorStateLists.length-1;
				}
				color = mColorStateLists[secondaryIndex].getColorForState(getDrawableState(), Color.TRANSPARENT);
				alpha = 255;
			} else {
				// Draw the primary color, but translucent
				color = mColorStateLists[mCurrentPrimaryColorIndex].getColorForState(getDrawableState(), Color.TRANSPARENT);
				alpha = 50;
			}
			mPaint.setColor(color);
			mPaint.setAlpha(alpha);
			canvas.drawArc(mBounds, sweepAngle, 360-sweepAngle+1, false, mPaint);
			
		} else {
			// During the start animation we only draw one arc.
		}
			
		canvas.restore();
		
		mLastSweepAngle = sweepAngle;
	}

	
}
