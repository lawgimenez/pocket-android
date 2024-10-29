package com.pocket.app.listen;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.pocket.sdk.api.generated.enums.CxtUi;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.tts.ListenState;
import com.pocket.sdk2.analytics.context.Contextual;
import com.pocket.util.android.ViewUtil;
import com.pocket.util.android.view.BetterPagerSnapHelper;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public final class CoverflowView extends RecyclerView implements Contextual {
	private final CoverflowAdapter adapter = new CoverflowAdapter(getContext());
	private final LinearLayoutManager layoutManager;
	private final int itemPadding;
	
	private int position;
	
	private OnSnappedPositionChangedListener snapListener;
	
	public CoverflowView(Context context) {
		this(context, null);
	}
	
	public CoverflowView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		
		itemPadding = getResources().getDimensionPixelSize(com.pocket.ui.R.dimen.pkt_space_sm);
		
		layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
		setLayoutManager(layoutManager);
		setAdapter(adapter);
		
		new BetterPagerSnapHelper(this).attach();
		
		addOnScrollListener(new OnScrollListener() {
			@Override public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				
				if (newState == RecyclerView.SCROLL_STATE_IDLE) {
					position = getChildAdapterPosition(findCenterChild());
					if (snapListener != null) {
						snapListener.onSnappedPositionChanged(position);
					}
				}
			}
		});
	}
	
	@Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
		super.onSizeChanged(w, h, oldW, oldH);
		ViewUtil.setPaddingHorizontal(this, (w - h) / 2 - itemPadding);
		scrollBy((oldW - w) / 2, 0);
	}
	
	@Override public boolean hasOverlappingRendering() {
		// Without this the whole view disappears as soon as I set alpha even slightly less than 100%.
		return false;
	}
	
	@Override public ActionContext getActionContext() {
		return new ActionContext.Builder().cxt_ui(CxtUi.COVER_FLOW).build();
	}
	
	@Override public void scrollToPosition(int position) {
		layoutManager.scrollToPositionWithOffset(position, 0);
	}
	
	void bind(ListenState state) {
		adapter.bind(state.list);
		
		if (state.index != position) {
			scrollToPosition(state.index);
			position = state.index;
		}
	}
	
	void setOnSnappedPositionChangedListener(OnSnappedPositionChangedListener listener) {
		snapListener = listener;
	}
	
	private View findCenterChild() {
		final LayoutManager layout = getLayoutManager();
		for (int i = 0; i < layout.getChildCount(); i++) {
			final View child = layout.getChildAt(i);
			if (getWidth() / 2 - child.getWidth() <= child.getX() && child.getX() <= getWidth() / 2) {
				return child;
			}
		}
		return null;
	}
	
	interface OnSnappedPositionChangedListener {
		void onSnappedPositionChanged(int position);
	}
}
