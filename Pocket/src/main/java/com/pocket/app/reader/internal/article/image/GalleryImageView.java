package com.pocket.app.reader.internal.article.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

public class GalleryImageView extends AppCompatImageView implements MatrixAnimator.OnAnimationCompleteListener {
	
	public String tag;
	
	public static final int MODE_LEADING = 1;
	public static final int MODE_FOLLOWING = 2;
	
	private static final int TOUCH_CANCEL = -1;
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_DRAG = 1;
	private static final int TOUCH_ZOOM = 2;
	private static final int TOUCH_UP_NOW_SNAPPING_BACK = 3;
	
	private static final float SHIFT_THRESHOLD_ZOOMED_OUT = 0.3f;
	private static final float SHIFT_THRESHOLD_ZOOMED_IN = 0.4f;
	private static final float MAX_ZOOM_SCALE = 3;
	
    private OnMoveListenser mOnMoveListenser;

    private int mTouchMode = TOUCH_NONE;
    private int mMode = MODE_FOLLOWING;
    
    private MatrixAnimator mAnimation;
    
    protected ImageMatrix mImageMatrix = new ImageMatrix();
	
	protected float mBitmapWidth;
	protected float mBitmapHeight;
	protected float mBitmapThumbScale;
	
	protected PointF mMid = new PointF();
    
    protected Context mContext;
    
	public GalleryImageView(Context context) {
        super(context);
        init(context);
    }
    
    public GalleryImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public GalleryImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	private void init(Context context){
		mContext = context;

        resetImageMatrix();
        setImageMatrix(mImageMatrix);
        setScaleType(ScaleType.MATRIX);
    }
	
	private void resetImageMatrix(){
		mImageMatrix = new ImageMatrix();
		mImageMatrix.setTranslate(1f, 1f);
		mMid = new PointF();
	}
	
	public void setImage(Bitmap bm) { 
		Drawable drawable = getDrawable();
		if(drawable != null && drawable instanceof BitmapDrawable){
			Bitmap oldBitmap = ((BitmapDrawable)getDrawable()).getBitmap();
			if(oldBitmap != null)
				oldBitmap.recycle();
		}
		
		resetImageMatrix();
		
		setImageBitmap(bm);
		if(bm != null){
			mBitmapWidth = bm.getWidth();
		    mBitmapHeight = bm.getHeight();
		    fitToScreen();
		}
        
    }
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		if(w > 0 && h > 0){
			fitToScreen();
			if(mMode == MODE_LEADING){
				mTouchMode = TOUCH_CANCEL;
				checkBounds();
			}
		}
	}

	private void fitToScreen(){
		int viewWidth = getWidth();
		int viewHeight = getHeight();
		
		if(viewWidth == 0 || viewHeight == 0)
			return;
		
		resetImageMatrix();
		
		mImageMatrix.setBitmapSize(mBitmapWidth, mBitmapHeight); // OPT move elsewhere?
		
		// Scale
        float xScale = viewWidth / mBitmapWidth;
        float yScale = viewHeight / mBitmapHeight;
        float scale = xScale <= yScale ? xScale : yScale;
        mBitmapThumbScale = scale;
        
        mImageMatrix.postScale(scale, scale, mMid.x, mMid.y);
        
        // Center the image
        float redundantXSpace = viewWidth - (scale * mBitmapWidth);
        float redundantYSpace = viewHeight - (scale * mBitmapHeight);
        
        redundantYSpace /= 2;
        redundantXSpace /= 2;
        
        if(mMode == MODE_FOLLOWING) // then hide off side
        	redundantXSpace += viewWidth;
        
        mImageMatrix.postTranslate(redundantXSpace, redundantYSpace);
        setImageMatrix(mImageMatrix);
        
        moved();
	}
	
	private void moved(){
		if(mMode == MODE_LEADING && mOnMoveListenser != null) // QUESTION it will be null if it is following? is this needed?
			mOnMoveListenser.onMove(mImageMatrix);
	}
	
	public void setAsCenterImage(boolean value, OnMoveListenser listener){
		setClickable(value);
		mOnMoveListenser = listener;
		
		if(value){
			setOnTouchListener(new ImageTouchListener());
			mMode = MODE_LEADING;
		} else {
			setOnTouchListener(null);
			mMode = MODE_FOLLOWING;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(mTouchMode == TOUCH_UP_NOW_SNAPPING_BACK && mAnimation != null){
			mAnimation.postAnimation(mImageMatrix);
			setImageMatrix(mImageMatrix);
			
			moved();
			
			invalidate(); // OPT not needed?
		}
		
	}
	
	private class ImageTouchListener implements OnTouchListener {
		
		private final PointF start = new PointF();
	    float oldDist = 1f;
		
		private final ImageMatrix onDownMatrix = new ImageMatrix();
		private ImageMatrix.Values onDownValues = onDownMatrix.getValues();
		private boolean onDownFittedToScreen = false;
		
		@Override
        public boolean onTouch(View v, MotionEvent rawEvent) {
            WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);

            if(mTouchMode == TOUCH_CANCEL && event.getAction() != MotionEvent.ACTION_DOWN)
            	return true;
            
            // Handle touch events here...
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
            	onDownMatrix.set(mImageMatrix);
            	onDownValues = onDownMatrix.getValues();
            	onDownFittedToScreen = onDownValues.scale == mBitmapThumbScale;
            	
                start.set(event.getX(), event.getY());
                mTouchMode = TOUCH_DRAG;
                break;
                
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                	onDownMatrix.set(mImageMatrix);
                    midPoint(mMid, event);
                    mTouchMode = TOUCH_ZOOM;
                }
                break;
                
            case MotionEvent.ACTION_UP:
                int xDiff = (int) Math.abs(event.getX() - start.x);
                int yDiff = (int) Math.abs(event.getY() - start.y);
                if (xDiff < 8 && yDiff < 8){
                    performClick();
                }
            case MotionEvent.ACTION_POINTER_UP:
                mTouchMode = TOUCH_NONE;
                checkBounds();
                break;
                
            case MotionEvent.ACTION_MOVE:
                if (mTouchMode == TOUCH_DRAG) {
                    // ...
                    mImageMatrix.set(onDownMatrix);
                    
                    mImageMatrix.postTranslate(event.getX() - start.x,
                    		onDownFittedToScreen ? 0 : event.getY() - start.y);
                     
                } else if (mTouchMode == TOUCH_ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        mImageMatrix.set(onDownMatrix);
                        float scale = newDist / oldDist;
                        mImageMatrix.postScale(scale, scale, mMid.x, mMid.y);
                    }
                }
                break;
            }

            moved();
            setImageMatrix(mImageMatrix);
            return true; // indicate event was handled
        }
				
		/** Determine the space between the first two fingers */
	    private float spacing(WrapMotionEvent event) {
	        // ...
	        float x = event.getX(0) - event.getX(1);
	        float y = event.getY(0) - event.getY(1);
	        return (float) Math.sqrt(x * x + y * y);
	    }

	    /** Calculate the mid point of the first two fingers */
	    private void midPoint(PointF point, WrapMotionEvent event) {
	        // ...
	        float x = event.getX(0) + event.getX(1);
	        float y = event.getY(0) + event.getY(1);
	        point.set(x / 2, y / 2);
	    }
		
	}

	private void checkBounds() {
		// Check for image switch
		ImageMatrix.Values values = mImageMatrix.getValues();
		boolean shifted = false;
		int width = getWidth();
		float threshold = width * (values.scale <= mBitmapThumbScale ? SHIFT_THRESHOLD_ZOOMED_OUT : SHIFT_THRESHOLD_ZOOMED_IN);
		if(values.x > threshold){
			// FINISH shift left
			if(mOnMoveListenser != null)
				shifted = mOnMoveListenser.shift(ImageViewer.MOVE_LEFT, true);
			
		} else if(values.x + values.scaledWidth < width - threshold){
			// FINISH shift right
			if(mOnMoveListenser != null)
				shifted = mOnMoveListenser.shift(ImageViewer.MOVE_RIGHT, true);
			
		}
		
		if(!shifted)
			snap(true);
        
	}
	
	public void snap(boolean animate) {
		// Check for boundary snapping
		mAnimation = MatrixAnimator.getIfOutOfBounds(mImageMatrix,
        		mBitmapThumbScale, MAX_ZOOM_SCALE,
        		getWidth(), getHeight(),
        		this);
		
		if(mAnimation != null){
			if(!animate)
				mAnimation.finishInstantly();
			
			mTouchMode = TOUCH_UP_NOW_SNAPPING_BACK;
			invalidate(); // OPT needed?
		}
	}
	
	public interface OnMoveListenser {
		public void onMove(ImageMatrix matrix);
		public boolean shift(int direction, boolean animate);
		public void onSnapped();
	}


	@Override
	public void onAnimatorCompleted(MatrixAnimator animator) {
		if(mTouchMode == TOUCH_UP_NOW_SNAPPING_BACK && animator == mAnimation){
			mTouchMode = TOUCH_NONE;
			if(mOnMoveListenser != null)
				mOnMoveListenser.onSnapped();
		}
	}

	public void resetWhenOffScreen() {
		// OPT
	}
	
	public void updateOffset(float edgeX, boolean rightEdgeProvided){
		if(mMode == MODE_LEADING)
			return;
		
		// OPT if is off screen, can save draw time?
		
		// OPT reset when off screen check
		
		ImageMatrix.Values values = mImageMatrix.getValues();
		float padding = values.getFramePadding(getWidth());
		float newX = rightEdgeProvided ? edgeX - values.scaledWidth - padding : edgeX + padding;
		
		mImageMatrix.postTranslate(newX - values.x, 0);
		setImageMatrix(mImageMatrix); // OPT does this need to happen everytime?
	}

	public void tag(String string) { // For debugging purposes
		tag = string;
	}
	
	
}

