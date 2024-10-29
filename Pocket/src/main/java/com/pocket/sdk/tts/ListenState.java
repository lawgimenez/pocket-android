package com.pocket.sdk.tts;

import org.threeten.bp.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * TODO Documentation
 */
public class ListenState {
	
	public final Set<VoiceCompat.Voice> voices;
	public final PlayState playstate;
	public final float speed;
	public final float pitch;
	public final Voice voice;
	public final Utterance utterance;
	public final Duration duration;
	public final Duration elapsed;
	public final float bufferingProgress;
	public final Track current;
	public final int index;
	public final List<Track> list;
	public final boolean autoArchive;
	public final boolean autoPlay;
	public final ListenError error;
	public final List<Feature> supportedFeatures;
	
	protected ListenState() {
		voices = new HashSet<>();
		playstate = PlayState.STOPPED;
		speed = 1;
		pitch = 1;
		voice = null;
		utterance = null;
		duration = Duration.ZERO;
		elapsed = Duration.ZERO;
		bufferingProgress = -1;
		current = null;
		index = 0;
		list = new ArrayList<>();
		autoArchive = false;
		autoPlay = false;
		error = null;
		supportedFeatures = new ArrayList<>();
	}
	
	protected ListenState(Builder builder) {
		if (builder.playstate == null) { throw new IllegalArgumentException("playstate must not be null, use STOPPED instead"); }
		
		voice = builder.voice;
		playstate = builder.playstate;
		speed = builder.speed;
		pitch = builder.pitch;
		voices = builder.voices;
		utterance = builder.utterance;
		duration = builder.duration;
		elapsed = builder.elapsed;
		bufferingProgress = builder.bufferingProgress;
		current = builder.current;
		index = builder.index;
		list = new ArrayList<>(builder.list);
		autoArchive = builder.autoArchive;
		autoPlay = builder.autoPlay;
		error = builder.error;
		supportedFeatures = builder.supportedFeatures;
	}
	
	public int getProgressPercent() {
		return duration.isZero() ? 0 : Math.round(100f * elapsed.getSeconds() / duration.getSeconds());
	}
	
	Builder rebuild() {
		return new Builder(this);
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ListenState{");
		sb.append("playstate=").append(playstate);
		sb.append(", current=").append(current);
		sb.append(", index=").append(index);
		sb.append('}');
		return sb.toString();
	}
	
	/*
	 * Note: in the future it probably makes sense to make the current speech engine part of the ListenState.
	 * (Like are we using a TTS Engine with TTSPlayer and TTS voices provided by the device? Or are we using
	 * an engine that requests audio files via v3/getItemAudio?)
	 * When we do that then each engine would have it's set of available voices and know how to save and restore
	 * the voices. So in shared prefs instead of saving voice type, we might save the current engine instead
	 * and delegate to it restoring whatever it needs from prefs to restore a voice.
	 */
	public interface Voice {
		Type getType();
		String getName();
		Locale getLocale();
		boolean isNetworkConnectionRequired();
		
		class Type {
			private final String type;
			
			public Type(String type) {
				this.type = type;
			}
			
			@Override public String toString() {
				return type;
			}
		}
	}
	
	public enum Feature {
		MULTIPLE_VOICES,
		ACCURATE_DURATION_AND_ELAPSED,
		PRELOADING
	}
	
	static class Builder {
		
		private Set<VoiceCompat.Voice> voices;
		private PlayState playstate;
		private float speed;
		private float pitch;
		private Voice voice;
		private Utterance utterance;
		private Duration duration;
		private Duration elapsed;
		private float bufferingProgress;
		private Track current;
		private int index;
		private List<Track> list;
		private boolean autoArchive;
		private boolean autoPlay;
		private ListenError error;
		private List<Feature> supportedFeatures;
		
		private Builder(ListenState state) {
			voices = new HashSet<>(state.voices);
			playstate = state.playstate;
			speed = state.speed;
			pitch = state.pitch;
			voice  = state.voice;
			utterance = state.utterance;
			duration = state.duration;
			elapsed = state.elapsed;
			bufferingProgress = state.bufferingProgress;
			current = state.current;
			index = state.index;
			list = new ArrayList<>(state.list);
			autoArchive = state.autoArchive;
			autoPlay = state.autoPlay;
			error = state.error;
			supportedFeatures = new ArrayList<>(state.supportedFeatures);
		}
		
		public Builder voices(Set<VoiceCompat.Voice> voices) {
			this.voices = voices;
			return this;
		}
		
		public Builder playstate(PlayState playstate) {
			this.playstate = playstate;
			return this;
		}
		
		public Builder speed(float speed) {
			this.speed = speed;
			return this;
		}
		
		public Builder pitch(float pitch) {
			this.pitch = pitch;
			return this;
		}
		
		public Builder voice(Voice voice) {
			this.voice = voice;
			return this;
		}
		
		public Builder utterance(Utterance utterance) {
			this.utterance = utterance;
			return this;
		}
		
		public Builder duration(Duration duration) {
			this.duration = duration;
			return this;
		}
		
		public Builder elapsed(Duration elapsed) {
			this.elapsed = elapsed;
			return this;
		}
		
		public Builder bufferingProgress(float progress) {
			bufferingProgress = progress;
			return this;
		}
		
		public Builder current(Track current) {
			this.current = current;
			return this;
		}
		
		public Builder index(int index) {
			this.index = index;
			return this;
		}
		
		public Builder list(List<Track> list) {
			this.list.clear();
			this.list.addAll(list);
			return this;
		}
		
		public Builder autoArchive(boolean value) {
			this.autoArchive = value;
			return this;
		}
		
		public Builder autoPlay(boolean value) {
			this.autoPlay = value;
			return this;
		}
		
		public Builder error(ListenError value) {
			this.error = value;
			return this;
		}
		
		Builder supportedFeatures(Feature... features) {
			this.supportedFeatures.clear();
			this.supportedFeatures.addAll(Arrays.asList(features));
			return this;
		}
		
		void addSupportedFeature(Feature feature) {
			supportedFeatures.add(feature);
		}
		
		public ListenState build() {
			return new ListenState(this);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		ListenState that = (ListenState) o;
		
		if (Float.compare(that.speed, speed) != 0) return false;
		if (Float.compare(that.pitch, pitch) != 0) return false;
		if (Float.compare(that.bufferingProgress, bufferingProgress) != 0) return false;
		if (index != that.index) return false;
		if (autoArchive != that.autoArchive) return false;
		if (autoPlay != that.autoPlay) return false;
		if (voices != null ? !voices.equals(that.voices) : that.voices != null) return false;
		if (playstate != that.playstate) return false;
		if (voice != null ? !voice.equals(that.voice) : that.voice != null) return false;
		if (utterance != null ? !utterance.equals(that.utterance) : that.utterance != null) return false;
		if (current != null ? !current.equals(that.current) : that.current != null) return false;
		if (!list.equals(that.list)) return false;
		if (duration != null ? !duration.equals(that.duration) : that.duration != null) return false;
		if (elapsed != null ? !elapsed.equals(that.elapsed) : that.elapsed != null) return false;
		if (!supportedFeatures.equals(that.supportedFeatures)) return false;
		return error == that.error;
		
	}
	
	@Override
	public int hashCode() {
		int result = voices != null ? voices.hashCode() : 0;
		result = 31 * result + (playstate != null ? playstate.hashCode() : 0);
		result = 31 * result + (speed != +0.0f ? Float.floatToIntBits(speed) : 0);
		result = 31 * result + (pitch != +0.0f ? Float.floatToIntBits(pitch) : 0);
		result = 31 * result + (voice != null ? voice.hashCode() : 0);
		result = 31 * result + (utterance != null ? utterance.hashCode() : 0);
		result = 31 * result + (bufferingProgress != +0.0f ? Float.floatToIntBits(bufferingProgress) : 0);
		result = 31 * result + (current != null ? current.hashCode() : 0);
		result = 31 * result + index;
		result = 31 * result + list.hashCode();
		result = 31 * result + (autoArchive ? 1 : 0);
		result = 31 * result + (autoPlay ? 1 : 0);
		result = 31 * result + (error != null ? error.hashCode() : 0);
		result = 31 * result + (duration != null ? duration.hashCode() : 0);
		result = 31 * result + (elapsed != null ? elapsed.hashCode() : 0);
		result = 31 * result + supportedFeatures.hashCode();
		return result;
	}
}