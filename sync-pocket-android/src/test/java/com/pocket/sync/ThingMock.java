package com.pocket.sync;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocket.sdk.api.generated.thing.Feed;
import com.pocket.sdk.api.generated.thing.GetNotifications;
import com.pocket.sdk.api.generated.thing.GetProfileFeed;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.test.generated.thing.DeepCollectionsTest;
import com.pocket.sync.test.generated.thing.OpenDangerousUsages;
import com.pocket.sync.test.generated.thing.OpenUsages;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Allow;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helpers for creating example {@link Thing}s with lots of state.
 */

public class ThingMock {

	/** For testing purposes, no special Json parsing configuration rules are used here */
	private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

	public static Things thing() {
		return new Things();
	}
	
	public static Strings string() {
		return new Strings();
	}
	
	private static Streams streams() {
		return new Streams();
	}
	
	public static class Things {
		
		public Feed feed() throws IOException {
			return Feed.from(parse(streams().feed()), JSON_CONFIG);
		}
		
		public GetNotifications getNotifications() throws IOException {
			return GetNotifications.from(parse(streams().getNotifications()), JSON_CONFIG);
		}
		
		public GetProfileFeed getProfileFeed() throws IOException {
			return GetProfileFeed.from(parse(streams().getProfileFeed()), JSON_CONFIG);
		}
		
		public DeepCollectionsTest deepCollections() throws IOException {
			return DeepCollectionsTest.from(parse(streams().deepCollections()), JSON_CONFIG);
		}

		public OpenUsages openUsages() throws IOException {
			return OpenUsages.from(parse(streams().openUsages()), JSON_CONFIG, Allow.UNKNOWN);
		}

		public OpenDangerousUsages openDangerousUsages() throws IOException {
			return OpenDangerousUsages.from(parse(streams().openDangerousUsages()), JSON_CONFIG, Allow.UNKNOWN);
		}

		private JsonParser parse(InputStream stream) throws IOException {
			return new ObjectMapper().getFactory().createParser(stream);
		}

	}
	
	public static class Strings {
		
		public String feed() throws IOException {
			return IOUtils.toString(streams().feed(), StandardCharsets.UTF_8);
		}
		
		public String getNotifications() throws IOException {
			return IOUtils.toString(streams().getNotifications(), StandardCharsets.UTF_8);
		}
		
		public String getProfileFeed() throws IOException {
			return IOUtils.toString(streams().getProfileFeed(), StandardCharsets.UTF_8);
		}
		
		public String deepCollections() throws IOException {
			return IOUtils.toString(streams().deepCollections(), StandardCharsets.UTF_8);
		}

		public String openUsages() throws IOException {
			return IOUtils.toString(streams().openUsages(), StandardCharsets.UTF_8);
		}

		public String openDangerousUsages() throws IOException {
			return IOUtils.toString(streams().openDangerousUsages(), StandardCharsets.UTF_8);
		}
	}
	
	public static class Streams {
		public InputStream feed() throws IOException {
			return file("feed");
		}
		
		public InputStream getNotifications() throws IOException {
			return file("getNotifications");
		}
		
		public InputStream getProfileFeed() throws IOException {
			return file("getProfileFeed");
		}
		
		public InputStream deepCollections() throws IOException {
			return file("deepcollections");
		}

		public InputStream openUsages() throws IOException {
			return file("OpenUsages");
		}

		public InputStream openDangerousUsages() throws IOException {
			return file("OpenDangerousUsages");
		}

		private InputStream file(String name) {
			return this.getClass().getResourceAsStream("/mock/" + name + ".json");
		}
	}
}
