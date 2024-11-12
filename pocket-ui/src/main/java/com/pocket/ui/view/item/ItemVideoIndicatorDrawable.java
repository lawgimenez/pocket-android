package com.pocket.ui.view.item;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.pocket.ui.R;
import com.pocket.ui.util.DimenUtil;
import com.pocket.util.android.drawable.ColorUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

/**
 * Overlaid on thumbnails to indicate the Item is a video.
 * Ideally we would have used a layer-list here, since its
 * pretty simple, but doesn't appear that vector compat
 * works for layer-list without having to enable
 * AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
 * which is heavy handed just to support that.
 *
 * Note: This drawable does not change colors or appearance based on
 * themes, it is always the same colors.
 */
public class ItemVideoIndicatorDrawable extends Drawable {
	
	
	public static Drawable forItemRow(Context context) {
		return new ItemVideoIndicatorDrawable(context, R.drawable.ic_pkt_play_mini, DimenUtil.dpToPxInt(context, 34/2));
	}
	
	public static Drawable forItemTile(Context context) {
		return new ItemVideoIndicatorDrawable(context, R.drawable.ic_pkt_play_mini, DimenUtil.dpToPxInt(context, 48/2));
	}
	
	public static Drawable forDiscoverTile(Context context) {
		return new ItemVideoIndicatorDrawable(context, R.drawable.ic_pkt_play_solid, DimenUtil.dpToPxInt(context, 72/2));
	}
	
	private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final int circleRadius;
	private final Drawable icon;
	private final int iconWidthRadius;
	private final int iconHeightRadius;
	
	private ItemVideoIndicatorDrawable(Context context, int icon, int circleRadius) {
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(ColorUtil.setAlpha(0.8f, context.getResources().getColor(R.color.pkt_grey_2)));
		
		this.circleRadius = circleRadius;
		this.icon = VectorDrawableCompat.create(context.getResources(), icon, null);
		DrawableCompat.setTint(this.icon, Color.WHITE);
		this.iconWidthRadius = this.icon.getIntrinsicWidth()/2;
		this.iconHeightRadius = this.icon.getIntrinsicHeight()/2;
	}
	
	@Override
	public int getIntrinsicHeight() {
		return circleRadius*2;
	}
	
	@Override
	public int getIntrinsicWidth() {
		return circleRadius*2;
	}
	
	@Override
	protected void onBoundsChange(Rect bounds) {
		super.onBoundsChange(bounds);
		int cx = getBounds().centerX();
		int cy = getBounds().centerY();
		icon.setBounds(cx-iconWidthRadius, cy-iconHeightRadius,
				cx+iconWidthRadius, cy+iconHeightRadius);
	}
	
	@Override
	public void draw(@NonNull Canvas canvas) {
		canvas.drawCircle(getBounds().centerX(), getBounds().centerY(), circleRadius, circlePaint);
		icon.draw(canvas);
	}
	
	@Override
	public void setAlpha(int alpha) {
		circlePaint.setAlpha(alpha);
		icon.setAlpha(alpha);
	}
	
	@Override
	public void setColorFilter(@Nullable ColorFilter colorFilter) {
		circlePaint.setColorFilter(colorFilter);
		icon.setColorFilter(colorFilter);
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}
}
