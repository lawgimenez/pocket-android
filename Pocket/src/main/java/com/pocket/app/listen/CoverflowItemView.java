package com.pocket.app.listen;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.ideashower.readitlater.R;
import com.pocket.ui.view.item.ItemThumbnailView;
import com.pocket.ui.view.themed.ThemedConstraintLayout;

public final class CoverflowItemView extends ThemedConstraintLayout {
	private final ItemThumbnailView cover;
	
	public CoverflowItemView(Context context) {
		this(context, null);
	}
	
	public CoverflowItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.view_coverflow_item, this);
		
		cover = findViewById(R.id.coverflow_item);
		disableAutoHiding();

		engageable.setUiEntityType(Type.CARD);
		engageable.setUiEntityComponentDetail("item_cover");
	}
	
	private void disableAutoHiding() {
		cover.setOnEmptyChangedListener(null);
		cover.setVisibility(View.VISIBLE);
	}
	
	void setThumbnail(Drawable drawable) {
		cover.setImageDrawable(drawable);
	}
	
	@Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		updateOffCenterTransform();
	}
	
	@Override public void offsetLeftAndRight(int offset) {
		super.offsetLeftAndRight(offset);
		updateOffCenterTransform();
	}
	
	private void updateOffCenterTransform() {
		final float center = getX() + getWidth() / 2;
		final int parentCenter = ((View) getParent()).getWidth() / 2;
		final float offset = Math.min(1, Math.abs(center - parentCenter) / getWidth());
		
		// Goes from 100% alpha in the center to 70% on either side.
		cover.setAlpha((int) (255 * (1 - 0.3f * offset)));
		
		// Goes from 100% scale in the center to 90% on either side.
		final float scale = 1 - (0.1f * offset);
		cover.setScaleX(scale);
		cover.setScaleY(scale);
	}
}
