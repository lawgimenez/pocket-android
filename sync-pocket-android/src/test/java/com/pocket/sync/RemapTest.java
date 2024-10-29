package com.pocket.sync;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.test.generated.Modeller;
import com.pocket.sync.test.generated.thing.Remapped;
import com.pocket.util.java.JsonUtil;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

public class RemapTest {

	/** For testing purposes, no special Json parsing configuration rules are used here */
	private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

	private final JsonFactory jsonFactory = JsonUtil.getObjectMapper().getFactory();
	
	@Test
	public void remapsObjectsAsList() throws Exception {
		ObjectNode json = Modeller.toObjectNode(IOUtils.toString(remapperObjStream(), UTF_8));
		Remapped remapped = Remapped.from(json, JSON_CONFIG);
		assertMappedCorrectly(remapped);
	}
	
	@Test
	public void remapsArrayAsList() throws Exception {
		ObjectNode json = Modeller.toObjectNode(IOUtils.toString(remapperArrayStream(), UTF_8));
		Remapped remapped = Remapped.from(json, JSON_CONFIG);
		assertMappedCorrectly(remapped);
	}
	
	@Test
	public void remapsObjectsAsListFromParser() throws Exception {
		final JsonParser json = jsonFactory.createParser(remapperObjStream());
		Remapped remapped = Remapped.from(json, JSON_CONFIG);
		assertMappedCorrectly(remapped);
	}
	
	@Test
	public void remapsArrayAsListFromParser() throws Exception {
		final JsonParser json = jsonFactory.createParser(remapperArrayStream());
		Remapped remapped = Remapped.from(json, JSON_CONFIG);
		assertMappedCorrectly(remapped);
	}
	
	private InputStream remapperObjStream() {
		return getClass().getResourceAsStream("/mock/remapper-obj.json");
	}
	
	private InputStream remapperArrayStream() {
		return this.getClass().getResourceAsStream("/mock/remapper-array.json");
	}
	
	private void assertMappedCorrectly(Remapped remapped) {
		Assert.assertEquals(asList(4, 3, 2, 1), remapped.sort_ids);
		Assert.assertEquals("first", remapped.info.get(0).value);
		Assert.assertNull(remapped.info.get(1));
		Assert.assertEquals("third", remapped.info.get(2).value);
		Assert.assertEquals("fourth", remapped.info.get(3).value);
	}
	
}
