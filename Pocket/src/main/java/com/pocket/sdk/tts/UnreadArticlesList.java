package com.pocket.sdk.tts;

import com.pocket.app.App;
import com.pocket.app.AppThreads;
import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.enums.ItemContentType;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.source.subscribe.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Playlist} of the user's saved articles.
 * <p>
 * Notes about behaviour:
 * <ul>
 * 	<li>When first loading, no archived items will be included.</li>
 * 	<li>Only {@link #COUNT} items will be loaded. TODO allow larger lists, auto paging, etc if needed.</li>
 * 	<li>
 * 	    To keep things simple at first, once the list is loaded, the list will not change which items are in it,
 * 	    archives, deletes, adds, will not add or remove items from this list.
 * 	    The sort order of the items will not change.
 * 	    The user must restart listen for this to have an effect.
 * 	</li>
 * 	<li>The {@link Item} objects however, will be updated with the latest and correct state.</li>
 * </ul>
 *
 */

public class UnreadArticlesList implements Playlist {
	
	private static final int COUNT = 50;
	
	private final List<Subscription> subscriptions = new ArrayList<>();
	
	private final Pocket pocket;
	private final AppThreads threads;
	private final int minWordCount;
	private final int maxWordCount;
	
	private List<Track> list;
	private Listener listener;
	
	UnreadArticlesList(Pocket pocket, AppThreads threads, int minWordCount, int maxWordCount) {
		this.pocket = pocket;
		this.threads = threads;
		this.minWordCount = minWordCount;
		this.maxWordCount = maxWordCount;
	}
	
	@Override
	public boolean isLoaded() {
		return list != null;
	}
	
	@Override
	public void load(OnLoad onLoaded) {
		if (isLoaded()) {
			onLoaded.onPlaylistLoaded(this);
			return;
		}
		
		pocket.sync(pocket.spec().things().saves()
				.state(ItemStatusKey.UNREAD)
				.contentType(ItemContentType.ARTICLE)
				.sort(App.getApp().listManager().getSortFilterState().getValue().getSort())
				.minWordCount(minWordCount)
				.maxWordCount(maxWordCount)
				.count(COUNT)
				.build())
					.onSuccess(saves -> threads.runOrPostOnUiThread(() -> {
						for (Item item : saves.list) {
							insert(TrackKt.toTrack(item));
						}
						// Since this is a local operation, we don't need to handle errors, as they are not expected.
						// If we add other types of item or content loading, we will need to make the callback more sophisticated
						onLoaded.onPlaylistLoaded(UnreadArticlesList.this);
					}));
	}
	
	@Override
	public Track get(int i) {
		if (i < 0 || i >= list.size()) {
			return null;
		}
		return list.get(i);
	}
	
	@Override
	public int size() {
		return list != null ? list.size() : 0;
	}
	
	@Override
	public void clear() {
		list = null;
		
		// Async clear subscriptions. Because there are many, and since internally they invoke space.forget, this can be slow
		// TODO make this non-async / simpler when we have the faster space implementation.
		ArrayList<Subscription> subs = new ArrayList<>(subscriptions);
		subscriptions.clear();
		threads.async(() -> {
			for (Subscription s : subs) {
				s.stop();
			}
		});
	}
	
	@Override
	public int indexOf(String url) {
		return indexOf(TrackKt.toTrack(ItemUtil.create(url, pocket.spec())));
	}
	
	@Override
	public int indexOf(Track e) {
		return list != null ? list.indexOf(e) : -1;
	}
	
	@Override
	public void insert(int index, Track e) {
		if (list == null) {
			list = new ArrayList<>();
		}

		if (index >= size()) {
			list.add(e);
		} else {
			list.add(index, e);
		}
		
		// Subscribe to future state changes
		Subscription sub = pocket.subscribe(Changes.of(e.syncItem), updated -> {
			int i = indexOf(e.idUrl);
			if (i >= 0) {
				list.set(i, TrackKt.toTrack(updated));
				if (listener != null) {
					listener.onPlayListChanged(UnreadArticlesList.this);
				}
			}
		});
		subscriptions.add(sub);
	}
	
	private void insert(Track t) {
		insert(size(), t);
	}
	
	@Override public void remove(Track track) {
		list.remove(track);
	}
	
	@Override
	public Track before(Track current) {
		int index = indexOf(current);
		if (index <= 0 || size() == 0) {
			return null;
		} else {
			return get(index - 1);
		}
	}
	
	@Override
	public Track after(Track current) {
		int index = indexOf(current);
		if (index >= size() - 1) {
			return null;
		} else {
			return get(index + 1);
		}
	}
	
	@Override
	public List<Track> get() {
		return list != null ? new ArrayList<>(list) : new ArrayList<>();
	}
	
	@Override
	public void setListener(Listener listener) {
		this.listener = listener;
	}
}
