package com.pocket.app.listen;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ideashower.readitlater.R;
import com.pocket.analytics.ImpressionComponent;
import com.pocket.analytics.Tracker;
import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.tts.Controls;
import com.pocket.sdk.tts.ListenState;
import com.pocket.sdk.tts.Track;
import com.pocket.sdk2.view.LazyAssetBitmap;
import com.pocket.sdk2.view.ModelBindingHelper;
import com.pocket.ui.view.item.ItemRowView;
import com.pocket.util.DisplayUtil;

/**
 * Adapter for the {@link ListenView} playlist.
 */
final class ListenItemAdapter extends RecyclerView.Adapter<ListenItemAdapter.ViewHolder> {
	
	private static final int HEADER_COUNT = 1;
	
	private final ListenPlayerView playerView;
	private final Drawable selectionIndicator;
	private final OnClick clicks;
	
	private ListenState state;
	private Controls controls;
	private boolean shouldShowDegradedView;
	private final Tracker tracker;
	
	ListenItemAdapter(Context context, ListenPlayerView playerView, OnClick clicks) {
		this.clicks = clicks;
		this.tracker = App.from(context).tracker();
		this.playerView = playerView;
		
		final Drawable bars = AppCompatResources.getDrawable(context, com.pocket.ui.R.drawable.ic_pkt_audio_bars_mini);
		if (bars != null) {
			selectionIndicator = DrawableCompat.wrap(bars);
			DrawableCompat.setTintList(selectionIndicator,
					ContextCompat.getColorStateList(context, com.pocket.ui.R.color.pkt_themed_teal_2));
		} else {
			selectionIndicator = null;
		}
	}
	
	/**
	 * Bind the data to present
	 */
	public void bind(ListenState state, Controls controls, boolean shouldShowDegradedView) {
		final boolean listChanged;
		final int lastIndex;
		if (this.state == null) {
			listChanged = true;
			lastIndex = RecyclerView.NO_POSITION;
		} else {
			listChanged = !this.state.list.equals(state.list);
			lastIndex = this.state.index;
		}
		
		this.state = state;
		this.controls = controls;
		this.shouldShowDegradedView = shouldShowDegradedView;
		
		if (listChanged) {
			notifyDataSetChanged();
			
		} else {
			notifyItemChanged(0, state); // Update the player header.
			
			if (state.index != lastIndex) {
				notifyItemChanged(lastIndex + HEADER_COUNT);
				notifyItemChanged(state.index + HEADER_COUNT);
			}
		}
	}
	
	@Override
	public int getItemCount() {
		return HEADER_COUNT + state.list.size();
	}
	
	@Override public int getItemViewType(int position) {
		if (position == 0) {
			return R.layout.view_listen_player;
		} else {
			return com.pocket.ui.R.layout.view_item_row;
		}
	}
	
	@NonNull
	@Override
	public ListenItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
		if (viewType == R.layout.view_listen_player) {
			final ViewGroup.LayoutParams layoutParams = viewGroup.getLayoutParams();
			layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
			playerView.setLayoutParams(layoutParams);
			return new ListenControlsHolder(playerView);

		} else if (viewType == com.pocket.ui.R.layout.view_item_row) {
			final ItemRowHolder holder =
					new ItemRowHolder(new ItemRowView(viewGroup.getContext()), selectionIndicator);
			holder.itemRow.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
			holder.itemRow.setOnClickListener(view -> {
				if (clicks != null) {
					final int index = holder.getAdapterPosition() - HEADER_COUNT;
					clicks.onClick(view, index);
				}
			});
			tracker.bindUiEntityIdentifier(holder.itemView, UiEntityIdentifier.ITEM.value);
			return holder;

		} else {
			throw new AssertionError("Unknown viewType in " + getClass().getSimpleName());
		}
	}
	
	@Override
	public void onBindViewHolder(@NonNull ListenItemAdapter.ViewHolder viewHolder, int position) {
		viewHolder.bind(state, shouldShowDegradedView, position);
	}
	
	void applyBottomSheetOffset(float slideOffset) {
		playerView.applyBottomSheetOffset(slideOffset);
	}
	
	public interface OnClick {
		void onClick(View view, int position);
	}
	
	static abstract class ViewHolder extends RecyclerView.ViewHolder {
		public ViewHolder(View itemView) {
			super(itemView);
		}
		
		abstract void bind(ListenState state, boolean shouldShowDegradedView, int position);
	}
	
	private class ItemRowHolder extends ViewHolder {
		private final ItemRowView itemRow;
		private final Drawable selectionIndicator;
		private final ModelBindingHelper modelBindingHelper;
		
		ItemRowHolder(ItemRowView itemView, Drawable selectionIndicator) {
			super(itemView);
			itemRow = itemView;
			this.selectionIndicator = selectionIndicator;
			modelBindingHelper = new ModelBindingHelper(itemView.getContext());
		}
		
		@Override void bind(ListenState state, boolean shouldShowDegradedView, int position) {
			final int playlistIndex = position - HEADER_COUNT;
			final Track track = state.list.get(playlistIndex);
			final boolean selected = state.index == playlistIndex;
			
			// TODO switch to a general item binding pattern (once we have one).
			final String thumbnail = track.displayThumbnailUrl;
			itemRow.bind()
					.clear()
					.thumbnail(thumbnail == null ? null
							: new LazyAssetBitmap(thumbnail,
									AssetUser.forItem(track.timeAdded, track.idKey)
							), track.isVideo())
					.meta()
						.title(track.displayTitle)
						.domain(DisplayUtil.displayHost(track.displayUrl))
						.timeEstimate(modelBindingHelper.listenDurationEstimate(track))
						.indicator(selected ? selectionIndicator : null);
			itemRow.setActivated(selected);
			tracker.enableImpressionTracking(itemRow, ImpressionComponent.CONTENT, track);
		}
	}
	
	private static class ListenControlsHolder extends ViewHolder {
		private final ListenPlayerView itemView;
		
		ListenControlsHolder(ListenPlayerView itemView) {
			super(itemView);
			this.itemView = itemView;
		}
		
		@Override void bind(ListenState state, boolean shouldShowDegradedView, int position) {
			itemView.bind(state, shouldShowDegradedView);
		}
	}
}
