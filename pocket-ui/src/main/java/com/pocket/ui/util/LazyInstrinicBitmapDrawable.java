package com.pocket.ui.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A variant of {@link LazyBitmapDrawable} when you need the drawable to have
 * intrinsic size that some view is basing its measuring on, for example when
 * being used as a image in an ImageView with wrap_content in one or both directions.
 * <p>
 * This will initially have no intrinsic size, but after it loads the image it will have one
 * and attempt to update the view for you.
 */
public class LazyInstrinicBitmapDrawable extends Drawable {
	
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Context context;
	private final ViewSizeUpdater updater;
	
	private Bitmap bitmap;
	private int width;
	private int height;
	
	public LazyInstrinicBitmapDrawable(Context context, LazyBitmap lazy, ViewSizeUpdater updater) {
		this.context = context;
		this.updater = updater;
		paint.setFilterBitmap(false);
		lazy.fill(0,0, this::setBitmap, null);
	}
	
	private void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		if (bitmap != null) {
			int dpi = context.getResources().getDisplayMetrics().densityDpi;
			width = bitmap.getScaledWidth(dpi);
			height = bitmap.getScaledHeight(dpi);
		}  else {
			width = 0;
			height = 0;
		}
		invalidateSelf();
		updater.onDrawableSizeChanged(this);
	}
	
	@Override
	public int getIntrinsicWidth() {
		return width > 0 ? width : -1;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return height > 0 ? height : -1;
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		if (bitmap != null) {
			canvas.drawBitmap(bitmap, null, getBounds(), paint);
		} else if (getCallback() instanceof LazyBitmapDrawable.SupportsPlaceholder) {
			((LazyBitmapDrawable.SupportsPlaceholder) getCallback()).drawPlaceholder(canvas, getBounds(), getState());
		}
	}
	
	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		paint.setColorFilter(colorFilter);
	}
	
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
		// Always return true, assuming the placeholder is stateful, could optimize to check with the placeholder
		return true;
	}
	
	public interface ViewSizeUpdater {
		
		/**
		 * A {@link ViewSizeUpdater} for when a {@link LazyInstrinicBitmapDrawable} is an ImageView's drawable.
		 */
		ViewSizeUpdater IMAGE_VIEW = drawable -> {
			ImageView view = (ImageView) drawable.getCallback();
			if  (view != null) {
				// Need to force it to update its internal mDrawableWidth and height, most reliable way is to just clear and reset the drawable.
				view.setImageDrawable(null);
				view.setImageDrawable(drawable);
			}
		};
		
		void onDrawableSizeChanged(LazyInstrinicBitmapDrawable drawable);
	}
}
