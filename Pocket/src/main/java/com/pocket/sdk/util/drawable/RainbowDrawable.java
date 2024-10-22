package com.pocket.sdk.util.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import com.pocket.app.App;
import com.pocket.app.settings.Theme;
import com.pocket.ui.util.DimenUtil;
import com.pocket.util.android.FormFactor;
import com.pocket.util.android.animation.AnimationUtil;
import com.pocket.util.android.animation.AnimationUtil.AnimationValues;

import java.util.Arrays;

/**
 * 
 */
public class RainbowDrawable extends Drawable {
	
	public static final int GREEN;
	public static final int BLUE;
	public static final int RED;
	public static final int GOLD;
	
	public static int DARK_MODE_ALPHA = (int) (255 * 0.7f);
	
	private static final Paint PAINT_MINT;
	private static final Paint PAINT_TURQUOISE;
	private static final Paint PAINT_CORAL;
	private static final Paint PAINT_GOLD;
	private static final Paint PAINT_WHITE;
	private static final Paint PAINT_BLACK;

	private static final int ANIMATION_SEGMENT_COUNT = 4;
	/** The time milliseconds in which one segment would move completely across the screen on a normal phone in portrait */
	private static final long ANIMATION_DURATION_AT_320DP = 1800;
	private static final Interpolator ANIMATION_INTERPOLATOR = new AccelerateInterpolator(1.6f);
	private static final Paint[] ANIMATION_COLORS;
	
	private enum AnimateState {
		/** Not animating */
		IDLE,
		/** Moving the static rainbow bar out of the way */
		STARTING,
		/** Going at normal consistent speed */
		ACTIVE,
		/** Moving the static rainbow bar out of the way */
		STOPPING_BEFORE_ACTIVE,
		/** The static rainbow bar is returning */
		STOPPING
	}
	
	static {
		Resources res = App.getContext().getResources();
		BLUE = res.getColor(com.pocket.ui.R.color.pkt_teal_4);
		GREEN = res.getColor(com.pocket.ui.R.color.pkt_teal_3);
		RED = res.getColor(com.pocket.ui.R.color.pkt_coral_2);
		GOLD = res.getColor(com.pocket.ui.R.color.amber_30);
		
		PAINT_MINT = newColorPaint(GREEN);
		PAINT_TURQUOISE = newColorPaint(BLUE);
		PAINT_CORAL = newColorPaint(RED);
		PAINT_GOLD = newColorPaint(GOLD);
		PAINT_WHITE = newColorPaint(Color.WHITE);
		PAINT_BLACK = newColorPaint(Color.BLACK);
		PAINT_WHITE.setStrokeWidth(0);
		
		ANIMATION_COLORS = new Paint[]{PAINT_TURQUOISE, PAINT_CORAL, PAINT_GOLD, PAINT_MINT};
	}
	
	private final float[] mSegments;
	
	private boolean mIsBorderVisible = true;
	private boolean mIsDark;
	private AnimateState mAnimationState = AnimateState.IDLE;
	private long mAnimationStart;
	private long mAnimationDuration;
	private final AnimationValues mAnimationValues = new AnimationValues();
	private int mAnimationLastRound;
	
	/**
	 * If you are going to want to animate this rainbow, with {@link #startProgressAnimation()}, make sure you supply
	 * a {@link Callback}.
	 * <p>
	 * Be sure to also override {@link View#verifyDrawable(Drawable)} in that case too.
	 * @param callback
	 */
	public RainbowDrawable(Callback callback) {
		setCallback(callback);
		mSegments = new float[ANIMATION_SEGMENT_COUNT];
	}

	@Override
	public void setAlpha(int alpha) {}

	@Override
	public void setColorFilter(ColorFilter cf) {}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
	
	@Override
	public boolean isStateful() {
		return true;
	}
	
	@Override
	protected boolean onStateChange(int[] state) {
		super.onStateChange(state);
		
		mIsDark = Theme.isDark(state);
		
		PAINT_MINT.setAlpha(mIsDark ? DARK_MODE_ALPHA : 255);
		PAINT_TURQUOISE.setAlpha(mIsDark ? DARK_MODE_ALPHA : 255);
		PAINT_GOLD.setAlpha(mIsDark ? DARK_MODE_ALPHA : 255);
		PAINT_CORAL.setAlpha(mIsDark ? DARK_MODE_ALPHA : 255);
		
		return true;
	}
	
	public void setBorderVisible(boolean visible) {
		mIsBorderVisible = visible;
	}
	
	public void startProgressAnimation() {
		mAnimationState = AnimateState.ACTIVE; // No longer using the start animation, so just go straight into the active state
		mAnimationStart = SystemClock.uptimeMillis();
		invalidateSelf();
	}
	
	public void stopProgressAnimation() {
		if (mAnimationState == AnimateState.IDLE || mAnimationState == AnimateState.STOPPING) {
			return;
		}
		
		if (mAnimationState == AnimateState.STARTING) {
			mAnimationState = AnimateState.STOPPING_BEFORE_ACTIVE;
		} else {
			mAnimationState = AnimateState.STOPPING;
		}
		
		AnimationUtil.getAnimationValue(mAnimationValues, mAnimationStart, mAnimationDuration);
		mAnimationLastRound = mAnimationValues.repeatCount + 1;
		
		invalidateSelf();
	}
	
	@Override
	public void draw(Canvas canvas) {
		Rect bounds = getBounds();
		float width = bounds.width();
		float height = bounds.height();
		
		if (mIsDark) {
			// draw black background behind rainbow
			canvas.drawRect(bounds, PAINT_BLACK);
		}
		
		AnimationValues values = null;
		if (mAnimationState != AnimateState.IDLE) {
			// Animating, get the latest values
			values = mAnimationValues;
			AnimationUtil.getAnimationValue(values, mAnimationStart, mAnimationDuration);
			
			// Check if the animation needs to change state
			if (mAnimationState == AnimateState.STARTING && values.repeatCount >= 1) {
				// The start animation has completed
				mAnimationState = AnimateState.ACTIVE;
			} else if (mAnimationState == AnimateState.STOPPING_BEFORE_ACTIVE && values.repeatCount == mAnimationLastRound) {
				// Completed the starting round, ready to start the stop animation
				mAnimationState = AnimateState.STOPPING;
			} else if (mAnimationState == AnimateState.STOPPING && values.repeatCount > mAnimationLastRound) {
				// The end animation has completed
				mAnimationState = AnimateState.IDLE;
			}
		}
		
		/*
		 * Note, this does not currently respect the bounds of the view. It assumes this will always go edge to edge.
		 * If you need it to respect the bounds you will need to update this as it will draw out of bounds as is.
		 */
		
		float y = bounds.top;
		float x = bounds.left;
		if (mAnimationState == AnimateState.IDLE) {
			// Static Rainbow Bar
			drawStaticRainbow(canvas, x, y, width, height);
			
		} else {
			// Animating, values should be not null as set above.
			// Calculate the position of the segments. Segments are the small dividers/spaces between bars.
			float[] positions = mSegments;
			int len = positions.length;
			float segmentSize = 1f / len;
			for (int i = 0; i < len; i++) {
				float p = values.currentPercent + segmentSize*i;
				if (p > 1) {
					p -= 1;
				}
				positions[i] = ANIMATION_INTERPOLATOR.getInterpolation(p) * width;
				positions[i] -= DimenUtil.dpToPx(App.getContext(), 2);
			}
			float center = positions[0];
			Arrays.sort(positions);
			
			// Determine which color to use for segments before the center point and after
			int colorIndex;
			if (values.repeatCount > ANIMATION_COLORS.length - 1) {
				colorIndex = values.repeatCount % ANIMATION_COLORS.length;
			} else { 
				colorIndex = values.repeatCount;
			}
			Paint rightPaint;
			if (mAnimationState == AnimateState.STARTING || mAnimationState == AnimateState.STOPPING_BEFORE_ACTIVE) {
				rightPaint = null;
				drawClippedRainbow(canvas, x, y, width, height, x + center, x + width);
				
			} else {
				rightPaint = ANIMATION_COLORS[colorIndex];
			}
			Paint leftPaint;
			if (mAnimationState == AnimateState.STOPPING && values.repeatCount >= mAnimationLastRound) {
				leftPaint = null;
				float speedUp = ANIMATION_INTERPOLATOR.getInterpolation(values.currentPercent) * (width * 1.75f);
				drawClippedRainbow(canvas, x, y, width, height, x, x + center + speedUp);
				x += speedUp; // offset the segments so they move faster at the end of the animation.
				
			} else {
				leftPaint = ANIMATION_COLORS[colorIndex < ANIMATION_COLORS.length-1 ? colorIndex + 1 : 0];
			}
			
			// Draw the segments
			for (int i = 0; i < len; i++) {
				float left;
				if (i == 0) {
					left = 0;
				} else {
					left = positions[i-1] + DimenUtil.dpToPx(App.getContext(), 2);
					if (left < 0) {
						left = 0;
					}
				}
				
				if (rightPaint == null && left >= center) {
					break;
				}
				
				float right = positions[i];
				if (right > left) {
					Paint paint = left >= center ? rightPaint : leftPaint;
					if (paint != null) {
						canvas.drawRect(x + left, y, x + right, y + height, paint);
					}
				}
			}
			float last = positions[len-1] + DimenUtil.dpToPx(App.getContext(), 2);
			if (last < width) {
				Paint paint = last >= center ? rightPaint : leftPaint;
				if (paint != null) {
					canvas.drawRect(x + last, y, width, y + height, paint);
				}
			}
		}
	
		if (mIsBorderVisible && !mIsDark) {
			// REVIEW is this still needed? canvas.drawLine(0, height+1, width, height+1, PAINT_WHITE);
		}
		
		if (mAnimationState != AnimateState.IDLE) {
			invalidateSelf();
		}
	}
	
	private void drawStaticRainbow(Canvas canvas, float left, float top, float width, float height) {
		float quarter = width / 4.0f;
		drawRainbowSegment(canvas, left + 0, top, quarter, height, PAINT_MINT);
		drawRainbowSegment(canvas, left + quarter, top, quarter, height, PAINT_TURQUOISE);
		drawRainbowSegment(canvas, left + quarter*2, top, quarter, height, PAINT_CORAL);
		drawRainbowSegment(canvas, left + quarter*3, top, quarter, height, PAINT_GOLD);
	}
	
	private void drawClippedRainbow(Canvas canvas, float left, float top, float width, float height, float clipLeft, float clipRight) {
		float quarter = width / 4.0f;
		drawRainbowSegment(canvas, left + 0, top, quarter, height, clipLeft, clipRight, PAINT_MINT);
		drawRainbowSegment(canvas, left + quarter, top, quarter, height, clipLeft, clipRight, PAINT_TURQUOISE);
		drawRainbowSegment(canvas, left + quarter*2, top, quarter, height, clipLeft, clipRight, PAINT_CORAL);
		drawRainbowSegment(canvas, left + quarter*3, top, quarter, height, clipLeft, clipRight, PAINT_GOLD);
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		
		// Calculate animation duration base
		float mod = bounds.width() / (float) FormFactor.dpToPx(320);
		mAnimationDuration = (long) (mod * ANIMATION_DURATION_AT_320DP);
		invalidateSelf();
	}
	
	private static void drawRainbowSegment(Canvas canvas, float left, float top, float width, float height, Paint paint) {
		canvas.drawRect(left, top, left + width, top + height, paint);
	}
	
	private static void drawRainbowSegment(Canvas canvas, float left, float top, float width, float height, float clipLeft, float clipRight, Paint paint) {
		if (left < clipLeft) {
			left = clipLeft;
		}
		float right = left + width;
		if (right > clipRight) {
			right = clipRight; 
		}
		if (left < right) {
			canvas.drawRect(left, top, right, top + height, paint);
		}
	}
	
	private static Paint newColorPaint(int color) {
		Paint paint = new Paint();
		paint.setColor(color);
		return paint;
	}

}
