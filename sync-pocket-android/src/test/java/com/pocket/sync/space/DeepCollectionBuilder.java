package com.pocket.sync.space;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Quick and dirty helper class for generating an example instance of DeepCollectionTest
 * See {@link SpaceTest#collectionsNest()}
 */
public class DeepCollectionBuilder {
	
	private static final int ELEMENTS_PER_COLLECTION = 2;
	
	public static ObjectNode create() {
		ObjectNode json = createDepth0();
//		DeepCollectionsTest test = DeepCollectionsTest.from(json);
//		JsonUtil.printDiff(json, test.toJson());
//		Assert.assertTrue(JsonUtil.equals(json, test.toJson(), JsonUtil.EqualsFlag.ANY_TYPE));
		return json;
	}
	
	private static ObjectNode createDepth0() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		String postfix = "0";
		obj.put("id", String.valueOf(val++));
		setupDepthVal(obj, postfix);
		setupDepthObj(obj, postfix);
		setupDepthRefList(obj, postfix);
		setupDepthRefMap(obj, postfix);
		setupDepthValList(obj, postfix);
		setupDepthValMap(obj, postfix);
		setupDepthObjList(obj, postfix, () -> createDepth1());
		setupDepthObjMap(obj, postfix, () -> createDepth1());
		return obj;
	}
	
	private static ObjectNode createDepth1() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		String postfix = "1";
		setupDepthVal(obj, postfix);
		setupDepthObj(obj, postfix);
		setupDepthRefList(obj, postfix);
		setupDepthRefMap(obj, postfix);
		setupDepthValList(obj, postfix);
		setupDepthValMap(obj, postfix);
		setupDepthObjList(obj, postfix, () -> createDepth2());
		setupDepthObjMap(obj, postfix, () -> createDepth2());
		return obj;
	}
	
	private static ObjectNode createDepth2() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		String postfix = "2";
		setupDepthVal(obj, postfix);
		setupDepthObj(obj, postfix);
		setupDepthRefList(obj, postfix);
		setupDepthRefMap(obj, postfix);
		setupDepthValList(obj, postfix);
		setupDepthValMap(obj, postfix);
		setupDepthObjList(obj, postfix, () -> createDepth3());
		setupDepthObjMap(obj, postfix, () -> createDepth3());
		return obj;
	}
	
	private static ObjectNode createDepth3() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		String postfix = "3";
		setupDepthVal(obj, postfix);
		setupDepthObj(obj, postfix);
		setupDepthRefList(obj, postfix);
		setupDepthRefMap(obj, postfix);
		setupDepthValList(obj, postfix);
		setupDepthValMap(obj, postfix);
		setupDepthObjList(obj, postfix, () -> createNonIdThing());
		setupDepthObjMap(obj, postfix, () -> createNonIdThing());
		return obj;
	}
	
	interface NodeCreator  {
		JsonNode create();
	}
	
	private static void setupDepthObjList(ObjectNode obj, String postfix, NodeCreator creator) {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode list = mapper.createArrayNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			list.add(creator.create());
		}
		obj.set("obj_list"+postfix, list);
	}
	
	private static void setupDepthObjMap(ObjectNode obj, String postfix, NodeCreator creator) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode map = mapper.createObjectNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			map.put(String.valueOf(i), creator.create());
		}
		obj.set("obj_map"+postfix, map);
	}
	
	private static void setupDepthValList(ObjectNode obj, String postfix) {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode list = mapper.createArrayNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			list.add(val++);
		}
		obj.set("val_list"+postfix, list);
	}
	
	private static void setupDepthValMap(ObjectNode obj, String postfix) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode map = mapper.createObjectNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			map.put(String.valueOf(i), val++);
		}
		obj.set("val_map"+postfix, map);
	}
	
	private static void setupDepthRefList(ObjectNode obj, String postfix) {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode list = mapper.createArrayNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			list.add(createRefThing());
		}
		obj.set("ref_list"+postfix, list);
	}
	
	private static void setupDepthRefMap(ObjectNode obj, String postfix) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode map = mapper.createObjectNode();
		for (int i = 0; i < ELEMENTS_PER_COLLECTION; i++) {
			map.put(String.valueOf(i), createRefThing());
		}
		obj.set("ref_map"+postfix, map);
	}
	
	private static ObjectNode createRefThing() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		obj.put("id", String.valueOf(val++));
		return obj;
	}
	
	private static int val = 0;
	
	private static void setupDepthVal(ObjectNode obj, String postfix) {
		obj.put("val"+postfix, val++);
	}
	
	private static void setupDepthObj(ObjectNode obj, String postfix) {
		obj.put("obj"+postfix, createNonIdThing());
	}
	
	private static ObjectNode createNonIdThing() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		obj.put("value", String.valueOf(val++));
		obj.put("obj", createNonIdThing2());
		return obj;
	}
	
	private static ObjectNode createNonIdThing2() {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode obj = mapper.createObjectNode();
		obj.put("value", String.valueOf(val++));
		return obj;
	}
	
}
