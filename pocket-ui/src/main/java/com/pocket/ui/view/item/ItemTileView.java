package com.pocket.ui.view.item;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.DimenRes;

import com.pocket.ui.R;
import com.pocket.ui.util.LazyBitmap;
import com.pocket.ui.util.LazyBitmapDrawable;
import com.pocket.ui.view.checkable.CheckableConstraintLayout;
import com.pocket.ui.view.visualmargin.VisualMargin;
import com.pocket.ui.view.visualmargin.VisualMarginConstraintLayout;

/**
 * Displays an Item as a tile, with an optional thumbnail and actions bar.
 * <p>
 * Includes a default {@link R.dimen#pkt_space_md} padding on sides and a visual top margin that will be consumed if you set a visual top margin on this.
 */
public class ItemTileView extends CheckableConstraintLayout implements VisualMargin {
	
	private final Binder binder = new Binder();
	private ItemMetaView meta;
	private ItemActionsBarView actions;
	private ItemThumbnailView thumbnail;
	
	public ItemTileView(Context context) {
		super(context);
		init();
	}
	
	public ItemTileView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public ItemTileView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.view_item_tile, this, true);
		meta = findViewById(R.id.item_meta);
		actions = findViewById(R.id.item_actions);
		thumbnail = findViewById(R.id.item_thumbnail);
		bind().clear();
		
		setMinimumWidth(getResources().getDimensionPixelSize(R.dimen.pkt_item_tile_width_min));
		setMinHeight(getResources().getDimensionPixelSize(R.dimen.pkt_item_tile_height_min));
		
		setBackgroundResource(R.drawable.cl_pkt_touchable_area);
		setClickable(true);
	}
	
	public Binder bind() {
		return binder;
	}
	
	public class Binder {
		
		private ItemThumbnailView.VideoIndicator videoIndicatorStyle;
		
		public Binder clear() {
			enabled(true, true);
			meta().clear().excerptMaxLines(4);
			actions().clear();
			thumbnailVideoIndicatorStyle(ItemThumbnailView.VideoIndicator.TILE);
			thumbnail((LazyBitmap) null, false);
			return this;
		}
		
		public Binder enabled(boolean itemDataEnabled, boolean actionsEnabled) {
			setEnabled(itemDataEnabled);
			meta.setEnabled(itemDataEnabled);
			thumbnail.setEnabled(itemDataEnabled);
			
			actions.setEnabled(actionsEnabled);
			return this;
		}
		
		public ItemMetaView.Binder meta() {
			return meta.bind();
		}
		
		public ItemActionsBarView.Binder actions() {
			return actions.bind();
		}
		
		public Binder thumbnail(LazyBitmap value, boolean isVideo) {
			thumbnail.setImageDrawable(value != null ? new LazyBitmapDrawable(value) : null);
			thumbnail.setVideoIndicatorStyle(isVideo ? videoIndicatorStyle : null);
			return this;
		}

		public Binder thumbnail(Drawable drawable, boolean isVideo) {
			thumbnail.setImageDrawable(drawable);
			thumbnail.setVideoIndicatorStyle(isVideo ? videoIndicatorStyle : null);
			return this;
		}
		
		public Binder thumbnailVideoIndicatorStyle(ItemThumbnailView.VideoIndicator style) {
			videoIndicatorStyle = style;
			return this;
		}

		public Binder marginStart(@DimenRes int margin) {
			final int marginPx = getResources().getDimensionPixelSize(margin);
			MarginLayoutParams params1 = (MarginLayoutParams) thumbnail.getLayoutParams();
			params1.leftMargin = marginPx;
			thumbnail.setLayoutParams(params1);

			MarginLayoutParams params2 = (MarginLayoutParams) meta.getLayoutParams();
			params2.leftMargin = marginPx;
			meta.setLayoutParams(params2);
			return this;
		}

		public Binder marginEnd(@DimenRes int margin) {
			final int marginPx = getResources().getDimensionPixelSize(margin);
			MarginLayoutParams params1 = (MarginLayoutParams) thumbnail.getLayoutParams();
			params1.rightMargin = marginPx;
			thumbnail.setLayoutParams(params1);

			MarginLayoutParams params2 = (MarginLayoutParams) meta.getLayoutParams();
			params2.rightMargin = marginPx;
			meta.setLayoutParams(params2);
			return this;
		}
	}
	
	@Override
	public boolean prepareVisualAscent() {
		if (thumbnail.getVisibility() == GONE) {
			return VisualMargin.removeTopMargin(meta);
		} else {
			boolean changed = VisualMargin.removeTopMargin(thumbnail);
			VisualMarginConstraintLayout.LayoutParams lp = (VisualMarginConstraintLayout.LayoutParams) meta.getLayoutParams();
			if (lp.visualMarginTop == 0) {
				lp.visualMarginTop = getResources().getDimensionPixelSize(R.dimen.pkt_space_md);
				meta.setLayoutParams(lp);
				changed = true;
			}
			return changed;
		}
	}
}
