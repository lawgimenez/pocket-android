package com.pocket.sdk.tts;

import java.util.List;

/**
 * TODO Documentation
 */

public interface Playlist {
	
	Track get(int i);
	List<Track> get();
	int size();
	/** Do any setup required to function and invoke the callback when done. Invokes immediately if already setup. */
	void load(OnLoad onLoadedCallback);
	boolean isLoaded();
	void clear();
	int indexOf(String url);
	int indexOf(Track current);
	void insert(int index, Track e);
	void remove(Track track);
	Track before(Track current);
	Track after(Track current);
	void setListener(Listener listener);
	
	default boolean contains(Track track) {
		return indexOf(track) != -1;
	}
	
	default boolean isLast(Track track) {
		return contains(track) && after(track) == null;
	}
	
	interface Listener {
		void onPlayListChanged(Playlist playlist);
	}
	interface OnLoad {
		void onPlaylistLoaded(Playlist playlist);
	}
}
