package com.pocket.ui.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A drawable that lazily and asynchronously loads a bitmap from an external source.
 * It fills whatever the bounds of this drawable are, so it means your view must declare
 * explict bounds, it can not depend on the intrinsic bounds of this drawable.
 * See {@link LazyInstrinicBitmapDrawable} for that use case.
 * <p>
 * Will reload any time the bounds change.
 * <p>
 * If your view implements {@link SupportsPlaceholder} you will receive a callback
 * when a placeholder view should be drawn, such as when it is loading.
 */
public class LazyBitmapDrawable extends Drawable {
	
	private final LazyBitmap lazy;
	private final LazyBitmap.Loaded onLoaded;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	private Bitmap bitmap;
	private LazyBitmap.Canceller canceller;
	
	public LazyBitmapDrawable(LazyBitmap lazy) {
		this.lazy = lazy;
		this.onLoaded = this::setBitmap;
		paint.setFilterBitmap(false);
	}
	
	public interface SupportsPlaceholder {
		void drawPlaceholder(Canvas canvas, Rect bounds, int[] state);
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		bitmap = null;
		canceller = LazyBitmap.Canceller.cancelAndRenew(canceller);
		lazy.fill(bounds.width(), bounds.height(), onLoaded, canceller);
	}
	
	private void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		invalidateSelf();
	}
	
	@Override
	public int getIntrinsicWidth() {
		return -1;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return -1;
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		if (bitmap != null) {
			canvas.drawBitmap(bitmap, null, getBounds(), paint);
		} else if (getCallback() instanceof SupportsPlaceholder) {
			((SupportsPlaceholder) getCallback()).drawPlaceholder(canvas, getBounds(), getState());
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
}
