package com.pocket.sdk.tts;

import com.pocket.util.java.Logs;

import java.util.Collections;
import java.util.List;

/** An initial empty playlist before we load a real one. */
class NotLoaded implements Playlist {
	
	@Override public Track get(int i) {
		return null;
	}
	
	@Override public List<Track> get() {
		return Collections.emptyList();
	}
	
	@Override public int size() {
		return 0;
	}
	
	@Override public void load(OnLoad onLoaded) {
		onLoaded.onPlaylistLoaded(this);
	}
	
	@Override public boolean isLoaded() {
		return false;
	}
	
	@Override public void clear() {}
	
	@Override public int indexOf(String url) {
		return -1;
	}
	
	@Override public int indexOf(Track track) {
		return -1;
	}
	
	@Override public void insert(int index, Track e) {
		Logs.throwIfNotProduction(getClass().getSimpleName() + " does not support insert");
	}
	
	@Override public void remove(Track track) {}
	
	@Override public Track before(Track current) {
		return null;
	}
	
	@Override public Track after(Track current) {
		return null;
	}
	
	@Override public void setListener(Listener listener) {
		// This playlist never changes, so we never notify the listener.
	}
}
