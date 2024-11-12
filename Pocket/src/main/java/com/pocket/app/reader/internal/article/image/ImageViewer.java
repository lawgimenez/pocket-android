package com.pocket.app.reader.internal.article.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.pocket.sdk.api.generated.thing.Image;

import java.util.ArrayList;

public class ImageViewer extends FrameLayout implements GalleryImageView.OnMoveListenser {

	public static final int MOVE_LEFT = -1;
	public static final int MOVE_RIGHT = 1;
	public static final int PADDING = 10;
	
	private final ArrayList<Image> mArticleImages = new ArrayList<>();
	private final ArrayList<CachedImage> mCache = new ArrayList<>();
	
	private int mCurrentImage;
	
	private GalleryImageView mCenterImage;
	private GalleryImageView mLeftImage;
	private GalleryImageView mRightImage;
	
	private OnImageChangeListener mListener;
	private OnClickListener mClickHandler;
	
	public ImageViewer(Context context) {
		super(context);
		init(context);
	}

	public ImageViewer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public ImageViewer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public void init(Context context){
		mClickHandler = new OnClick();
		
		setCenterImage(new GalleryImageView(context));
		setLeftImage(new GalleryImageView(context));
		setRightImage(new GalleryImageView(context));
		
		addView(mLeftImage);
		addView(mRightImage);
		addView(mCenterImage);
	}
	
	public void setImages(ArrayList<Image> images, int startingImageId) {
		mArticleImages.clear();
		mCache.clear();
		
		for (Image image : images) {
			try {
				mCache.add(new CachedImage(image.src));
				mArticleImages.add(image);
			} catch (IllegalArgumentException ignore) {}
		}
		
		final int startingImage = findImageById(startingImageId);
		mCurrentImage = startingImage;
		mCenterImage.setImage(getImage(mCurrentImage));
		loadFollowerImage(mLeftImage, startingImage - 1);
		loadFollowerImage(mRightImage, startingImage + 1);
	}
	
	private int findImageById(int imageId) {
		for (int i = 0; i < mArticleImages.size(); i++) {
			final Image image = mArticleImages.get(i);
			if (image.image_id != null && image.image_id == imageId) return i;
		}
		
		return -1;
	}
	
	public void setCenterImage(GalleryImageView view){
		bringChildToFront(view);
		view.setAsCenterImage(true, this); 
		mCenterImage = view;
		view.setOnClickListener(mClickHandler);
	}
	
	public void setLeftImage(GalleryImageView view){
		setSideImage(view);
		mLeftImage = view;
	}
	
	public void setRightImage(GalleryImageView view){
		setSideImage(view);
		mRightImage = view;
	}
	
	private void setSideImage(GalleryImageView view){
		view.setAsCenterImage(false, null);
		view.setOnClickListener(null);
	}
	
	private void loadFollowerImage(GalleryImageView imageView, int index) {
		Image image = getSafely(mArticleImages, index);
		if(image != null){
			imageView.setVisibility(View.VISIBLE);
			imageView.setImage(getImage(index));
			
		} else {
			imageView.setImage(null);
			imageView.setVisibility(View.GONE);
		}
	}
	
	private Bitmap getImage(int image){
		CachedImage cachedImage = getSafely(mCache, image);
		return cachedImage != null ? cachedImage.get() : null;
	}
	
	@Override
	public boolean shift(int direction, boolean animate) {
		// OPT this is being called before everything loads.  it shouldn't but here is a check for now
		if (mArticleImages.isEmpty()) return false;
		
		// Remember current state
		GalleryImageView left = mLeftImage;
		GalleryImageView center = mCenterImage;
		GalleryImageView right = mRightImage;
		int newCurrent = mCurrentImage;
		
		// OPT clean this up and share some code
		
		if(direction == MOVE_LEFT){
			newCurrent -= 1;
			
			Image newArticleImage = getSafely(mArticleImages, newCurrent);
			if (newArticleImage == null) return false;
						
			// Right ImageView will become the new left, and will load a new image
			loadFollowerImage(right, newCurrent-1);
			setLeftImage(right);
			
			// Center ImageView is now moving to the right, can keep image, but needs to reset after it goes off screen
			center.resetWhenOffScreen();
			setRightImage(center);
			
			// Left ImageView is now the center image
			setCenterImage(left);
			
						
		} else {
			newCurrent += 1;
			
			Image newArticleImage = getSafely(mArticleImages, newCurrent);
			if (newArticleImage == null) return false;
						
			// Left ImageView will become the new right, and will load a new image
			loadFollowerImage(left, newCurrent+1);
			setRightImage(left);
			
			// Center ImageView is now moving to the left, can keep image, but needs to reset after it goes off screen
			center.resetWhenOffScreen();
			setLeftImage(center);
			
			// Right ImageView is now the center image
			setCenterImage(right);
			
		}
		
		mCenterImage.snap(animate);
		mCurrentImage = newCurrent;
		if (mListener != null) mListener.onChange();
		return true;
	}
	
	private class OnClick implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			if(mListener != null)
				mListener.onClick();
		}
		
	}
	
	public void move(int increment){
		if(!canGo(increment))
			return;
		
		shift(increment, false);
	}

	@Override // OPT create an object for this interface so it can have reusable variables?
	public void onMove(ImageMatrix matrix) {
		ImageMatrix.Values values = matrix.getValues();
		float padding = PADDING + values.getFramePadding(getWidth());
		mLeftImage.updateOffset(values.x - padding, true);
		mRightImage.updateOffset(values.x + values.scaledWidth + padding, false);
	}

	public boolean canGo(int increment) {
		return getSafely(mArticleImages, mCurrentImage + increment) != null;
	}

	public Image getCurrentImage() {
		return getSafely(mArticleImages, mCurrentImage);
	}
	
	public int getCurrentImageIndex() {
		return mCurrentImage;
	}
	
	public void setOnImageChangeListener(OnImageChangeListener listener){
		mListener = listener;
	}

	public interface OnImageChangeListener {
		public void onChange();
		public void onClick();
	}

	@Override public void onSnapped() {}

	public void onDestroy() {
		if(mLeftImage == null)
			return;
		
		mLeftImage.setImage(null);
		mCenterImage.setImage(null);
		mRightImage.setImage(null);
	}
	
	private static <T> T getSafely(ArrayList<T> list, int index) {
		return index >= 0 && index < list.size() ? list.get(index) : null;
	}
	
}
