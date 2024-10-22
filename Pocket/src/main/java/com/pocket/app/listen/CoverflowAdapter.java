package com.pocket.app.listen;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pocket.analytics.ImpressionComponent;
import com.pocket.analytics.ItemContent;
import com.pocket.analytics.Tracker;
import com.pocket.app.App;
import com.pocket.sdk.api.generated.enums.UiEntityIdentifier;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.tts.Track;
import com.pocket.sdk2.view.LazyAssetBitmap;
import com.pocket.ui.util.LazyBitmapDrawable;
import com.pocket.ui.util.PlaceHolderBuilder;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams;

final class CoverflowAdapter extends RecyclerView.Adapter<CoverflowAdapter.ViewHolder> {
	
	private final List<Track> tracks = new ArrayList<>();
	private final Tracker tracker;

	public CoverflowAdapter(Context context) {
		this.tracker = App.from(context).tracker();
	}
	
	@Override public int getItemCount() {
		return tracks.size();
	}
	
	@NonNull @Override public CoverflowAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		final CoverflowItemView itemView = new CoverflowItemView(parent.getContext());
		itemView.setLayoutParams(new RecyclerView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		tracker.bindUiEntityIdentifier(itemView, UiEntityIdentifier.ITEM.value);
		return new ViewHolder(itemView);
	}
	
	@Override public void onBindViewHolder(@NonNull CoverflowAdapter.ViewHolder holder, int position) {
		holder.bind(tracks.get(position));
	}
	
	void bind(List<Track> tracks) {
		if (this.tracks.equals(tracks)) return;
		
		this.tracks.clear();
		this.tracks.addAll(tracks);
		notifyDataSetChanged();
	}
	
	final class ViewHolder extends RecyclerView.ViewHolder {
		private final CoverflowItemView itemView;

		public ViewHolder(CoverflowItemView itemView) {
			super(itemView);
			this.itemView = itemView;
		}
		
		void bind(Track track) {
			final String thumbnail = track.displayThumbnailUrl;

			// TODO should we cache placeholders?
			itemView.setThumbnail(thumbnail == null ?
					PlaceHolderBuilder.getDrawable(
							itemView.getContext(),
							track.idUrl,
							track.displayTitle.charAt(0)
					) :
					new LazyBitmapDrawable(
							new LazyAssetBitmap(
									thumbnail,
									AssetUser.forItem(track.timeAdded, track.idKey)
							)));
			tracker.bindContent(itemView, new ItemContent(track.idUrl));
			tracker.enableImpressionTracking(itemView, ImpressionComponent.CONTENT, track);
		}
	}
}
