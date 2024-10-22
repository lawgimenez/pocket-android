package com.pocket.app.reader.internal.article.image;

import android.graphics.Matrix;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MatrixAnimator {
	
	private static final float SNAP_DURATION = 180; // millis
	
	private final OnAnimationCompleteListener mOnAnimationCompleteListener;
	
	// Start
	final private ImageMatrix mStartMatrix;
	final private long mStartTime;
	final private float mCenterX;
	final private float mCenterY;
	
	// Distance
	final private float mDistanceX;
	final private float mDistanceY;
	final private float mDistanceScale;
	final private boolean mMovingLeft; // Flags for negative numbers
	final private boolean mMovingDown;
	final private boolean mShrinking;
	
	final private AccelerateDecelerateInterpolator mInterpolator;
	
	// Current
	private float mX;
	private float mY;
	private float mScale;
	//private float curTime;
	private float mPercentTime;
	private float mInterpolation;
	private boolean mFinishInstantly = false;
	    	
	
	public MatrixAnimator(ImageMatrix currentMatrix,
			OnAnimationCompleteListener listener,
			float startCenterX, float startCenterY,
			float distX, boolean left,
			float distY, boolean down,
			float distScale, boolean shrink) {
		
		mStartMatrix = new ImageMatrix();
		mStartMatrix.set(currentMatrix);
		mStartTime = SystemClock.uptimeMillis();
		
		mOnAnimationCompleteListener = listener;
		
		mCenterX = startCenterX;
		mCenterY = startCenterY;
		
		mDistanceX = distX;
		mMovingLeft = left;
		
		mDistanceY = distY;
		mMovingDown = down;
		
		mDistanceScale = distScale;
		mShrinking = shrink;
		
		mInterpolator = new AccelerateDecelerateInterpolator(); // OPT reuse or static?
		
		
		
		/*
		Dev.d("ReadItLater", "==========");
		Dev.d("ReadItLater", "START RETURNING");
		Dev.d("ReadItLater", "distanceX: "+mDistanceX);
		Dev.d("ReadItLater", "movingLeft: "+mMovingLeft);
		Dev.d("ReadItLater", "distanceY: "+mDistanceY);
		Dev.d("ReadItLater", "movingDown: "+mMovingDown);
		Dev.d("ReadItLater", "distanceScale: "+mDistanceScale);
		Dev.d("ReadItLater", "shrinking: "+mShrinking);
		Dev.d("ReadItLater", "==========");
		*/
	}
	
	public void postAnimation(Matrix matrix){
		matrix.set(mStartMatrix);
		mPercentTime = (SystemClock.uptimeMillis() - mStartTime) / SNAP_DURATION;
		mInterpolation = mInterpolator.getInterpolation(mPercentTime);
		
		//Dev.d("ReadItLater", "curPercentTime: "+mPercentTime);
		//Dev.d("ReadItLater", "curInterpolation: "+mInterpolation);
		
		if(mPercentTime >= 1 || mFinishInstantly){
        	// Finish, set to destination
			mX = mDistanceX;
        	mY = mDistanceY;
        	mScale = mDistanceScale;
			if(mOnAnimationCompleteListener != null)
				mOnAnimationCompleteListener.onAnimatorCompleted(this);
        	
			
		} else {
			// Get current distance traveled towards destination
    		mX = mInterpolation * mDistanceX;
    		mY = mInterpolation * mDistanceY;
    		mScale = mInterpolation * mDistanceScale;
    		
    		//Dev.d("ReadItLater", "DURING INTERPOL: "+mX + ", " + mY + ", " + mScale);
    			    		
		}
		
		// Convert to negative numbers if needed
		mX = mMovingLeft ? 0 - mX : mX;
		mY = mMovingDown ? 0 - mY : mY;
		mScale = mShrinking ? 1 - mScale : mScale + 1;
		
		//Dev.d("ReadItLater", "AFTER NEGATIVE: "+mX + ", " + mY + ", " + mScale);
		
		matrix.postTranslate(mX, mY);
		matrix.postScale(mScale, mScale, mCenterX+mX, mCenterY+mY);
		
        //Dev.d("ReadItLater", "POSTED: "+mX + ", " + mY + ", " + mScale);
       
	}
	
	public interface OnAnimationCompleteListener {
		public void onAnimatorCompleted(MatrixAnimator animator); 
	}
	
	public static MatrixAnimator getIfOutOfBounds(ImageMatrix matrix,
			float minZoomScale, float maxZoomScale,
			float viewportWidth, float viewportHeight,
			OnAnimationCompleteListener listener) {
		
		ImageMatrix.Values values = matrix.getValues();
		
		float[] rangeX;
		float[] rangeY;
		
		float returnFromX = values.x; // OPT "move" instead of "return", now that this is a shared function class
		float returnFromY = values.y;
		float returnFromScale = values.scale;
					
		float returnToX = returnFromX;
		float returnToY = returnFromY;
		float returnToScale = returnFromScale;
		
		float distanceToReturnX = 0;
		float distanceToReturnY = 0;
		float distanceToReturnScale = 0;
		
		float startingCenterX = values.x + (values.scaledWidth/2);
		float startingCenterY = values.y + (values.scaledHeight/2);
		
		// Check if X, Y, or Scale is out of Bounds //
		// If so, get the values they will snap back to //
		
		// Scale
		if(values.scale > maxZoomScale){
			returnToScale = maxZoomScale;
			
		} else if (values.scale < minZoomScale){
			returnToScale = minZoomScale;
			
		} else {
			returnToScale = values.scale;
		}
		
		// X, Y
		if(returnToScale != values.scale){
			// Scale is going to change, so X,Y should be based on post-scaled values
			
			/*
			 * OPT
			 * Improve this so the center of the scaling is the visual center of the
			 * image.  So that after it scales, they are still looking at the same area of the image
			 * 
			 */
			
			float futureWidth = values.fullWidth * returnToScale;
			float futureHeight = values.fullHeight * returnToScale;
			
			returnFromX = startingCenterX - (futureWidth/2); 
			returnFromY = startingCenterY - (futureHeight/2);
			
			// OPT clean up
			
			//returnFromX = mid.x - (futureWidth/2);
			//returnFromY = mid.y - (futureHeight/2);
			
			rangeX = getRange(futureWidth, viewportWidth);
			rangeY = getRange(futureHeight, viewportHeight);
											
		} else {
			// Scale is not changing, so X,Y is ok to use current location
			rangeX = getRange(values.scaledWidth, viewportWidth);
			rangeY = getRange(values.scaledHeight, viewportHeight);
			
		}
		
		returnToX = getReturnToValue(returnFromX, rangeX);
		returnToY = getReturnToValue(returnFromY, rangeY);
					
		if(returnToScale != returnFromScale ||
				returnToX != returnFromX ||
				returnToY != returnFromY) {
		
			// Calculate distances they must move //
			
			distanceToReturnX = Math.abs(returnToX - returnFromX);
    		boolean movingLeft = returnToX - returnFromX < 0;
    		
    		distanceToReturnY = Math.abs(returnToY - returnFromY);
    		boolean movingDown = returnToY - returnFromY < 0;
    		
    		distanceToReturnScale = returnToScale / returnFromScale;
    		boolean shrinking = distanceToReturnScale < 1;
    		if(shrinking){
    			distanceToReturnScale = 1 - distanceToReturnScale;
    		} else {
    			distanceToReturnScale = distanceToReturnScale - 1;
    		}
		
    		return new MatrixAnimator(matrix,
    				listener,
    				startingCenterX, startingCenterY,
    				distanceToReturnX, movingLeft,
    				distanceToReturnY, movingDown,
    				distanceToReturnScale, shrinking);
		
		} else {
			// Nothing needs to move //
			return null;
			
		}
	}
	
	private static float[] getRange(float imageWidth, float viewportWidth){
    	float[] range = new float[]{0,0};
    	float sizeDifference = imageWidth - viewportWidth;
		boolean imageBigger = sizeDifference > 0;
		if(imageBigger){
			range[0] = 0 - sizeDifference;
			range[1] = 0;
			
		} else {
			range[0] = Math.abs(sizeDifference) / 2;
			range[1] = range[0];
			
		}
		return range;
    }

	private static float getReturnToValue(float current, float[] range){
		if(current > range[1])
			return range[1];
		
		if(current < range[0])
			return range[0];
		
		return current;					            
	}

	public void finishInstantly() {
		mFinishInstantly = true;
	}

	
	
}
