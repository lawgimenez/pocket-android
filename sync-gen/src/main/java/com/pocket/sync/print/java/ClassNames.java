package com.pocket.sync.print.java;

import com.squareup.javapoet.ClassName;

import java.util.Set;

/**
 * A single place to reference all of the classes that the code generation needs to use.
 */
public class ClassNames {
	
	public static final ClassName BASE_MODELLER = 		ClassName.get("com.pocket.sync.value", "BaseModeller");
	public static final ClassName SYNCABLE_CREATOR = 	ClassName.get("com.pocket.sync.value", "SyncableParser");
	public static final ClassName STREAMING_THING_CREATOR = ClassName.get("com.pocket.sync.value", "StreamingThingParser");
	public static final ClassName TYPE_CREATOR = 		ClassName.get("com.pocket.sync.value", "TypeParser");
	public static final ClassName STREAMING_TYPE_CREATOR = ClassName.get("com.pocket.sync.value", "StreamingTypeParser");
	public static final ClassName BYTE_CREATOR = 		ClassName.get("com.pocket.sync.value", "ByteTypeParser");
	public static final ClassName OPEN_CREATOR = 		ClassName.get("com.pocket.sync.value", "OpenParser");
	public static final ClassName STRING_ENUM = 		ClassName.get("com.pocket.sync.value", "StringEnum");
	public static final ClassName INTEGER_ENUM = 		ClassName.get("com.pocket.sync.value", "IntegerEnum");
	public static final ClassName INCLUDE = 			ClassName.get("com.pocket.sync.value", "Include");
	public static final ClassName ALLOW = 				ClassName.get("com.pocket.sync.value", "Allow");
	public static final ClassName VARIETY = 			ClassName.get("com.pocket.sync.value", "Variety");
	public static final ClassName ENCRYPTER = 			ClassName.get("com.pocket.sync.value.protect", "StringEncrypter");
	public static final ClassName BYTE_WRITER = 		ClassName.get("com.pocket.sync.value.binary", "ByteWriter");
	public static final ClassName BYTE_READER = 		ClassName.get("com.pocket.sync.value.binary", "ByteReader");

	public static final ClassName THING = 				ClassName.get("com.pocket.sync.thing", "Thing");
	public static final ClassName FLAT_UTILS = 			ClassName.get("com.pocket.sync.thing", "FlatUtils");
	public static final ClassName FLATTENER = 			GenUtil.innerclass(FLAT_UTILS, "Output");
	public static final ClassName EQUALITY = 			GenUtil.createInnerClassName(THING, "Equality");
	public static final ClassName THING_UTIL = 			ClassName.get("com.pocket.sync.thing", "ThingUtil");
	public static final ClassName THING_BUILDER = 		ClassName.get("com.pocket.sync.thing", "ThingBuilder");
	
	public static final ClassName ACTION = 				ClassName.get("com.pocket.sync.action", "Action");
	public static final ClassName ACTION_BUILDER = 		ClassName.get("com.pocket.sync.action", "ActionBuilder");
	public static final ClassName TIME = 				ClassName.get("com.pocket.sync.action", "Time");
	public static final ClassName ACTION_RESOLVED =		ClassName.get("com.pocket.sync.action", "ActionResolved");

	public static final ClassName SPACE = 				ClassName.get("com.pocket.sync.space", "Space");
	public static final ClassName SPACE_SELECTOR = 		GenUtil.createInnerClassName(SPACE, "Selector");
	public static final ClassName CHANGE = 				ClassName.get("com.pocket.sync.space", "Change");
	public static final ClassName RICH_DIFF = 			ClassName.get("com.pocket.sync.space", "Diff");
	public static final ClassName MUTABLE =		 		ClassName.get("com.pocket.sync.space.mutable", "MutableThing");
	public static final ClassName MUTABLES =		 	ClassName.get("com.pocket.sync.space.mutable", "Mutables");
	
	public static final ClassName SPEC = 				ClassName.get("com.pocket.sync.spec", "Spec");
	public static final ClassName REACTIONS = 			ClassName.get("com.pocket.sync.spec", "Reactions");
	public static final ClassName APPLIER = 			ClassName.get("com.pocket.sync.spec", "Applier");
	public static final ClassName SPEC_THINGS = 		GenUtil.createInnerClassName(SPEC, "Things");
	public static final ClassName SPEC_ACTIONS = 		GenUtil.createInnerClassName(SPEC, "Actions");
	public static final ClassName SPEC_DERIVE = 		GenUtil.createInnerClassName(SPEC, "Derive");
	
	public static final ClassName CHANGES = 			ClassName.get("com.pocket.sync.source.subscribe", "Changes");
	public static final ClassName THING_MATCH = 		GenUtil.createInnerClassName(CHANGES, "ThingMatch");
	public static final ClassName REMOTE = 				ClassName.get("com.pocket.sync.source", "Remote");
	public static final ClassName REMOTE_METHOD = 		ClassName.get("com.pocket.sync.source", "Remote.Method");
	public static final ClassName AUTH_TYPE = 			ClassName.get("com.pocket.sync.source", "AuthType");
	public static final ClassName STYLE = 				ClassName.get("com.pocket.sync.source", "RemoteStyle");
	public static final ClassName REMOTE_PRIORITY = 	ClassName.get("com.pocket.sync.source.result", "RemotePriority");
	public static final ClassName JSON_CONFIG = 		ClassName.get("com.pocket.sync.source", "JsonConfig");

	public static final ClassName JSON_NODE = 			ClassName.get("com.fasterxml.jackson.databind", "JsonNode");
	public static final ClassName OBJECT_NODE = 		ClassName.get("com.fasterxml.jackson.databind.node", "ObjectNode");
	public static final ClassName ARRAY_NODE = 			ClassName.get("com.fasterxml.jackson.databind.node", "ArrayNode");
	public static final ClassName NULL_NODE = 			ClassName.get("com.fasterxml.jackson.databind.node", "NullNode");
	public static final ClassName JSON_TOKEN = 			ClassName.get("com.fasterxml.jackson.core", "JsonToken");
	public static final ClassName JSON_PARSER = 		ClassName.get("com.fasterxml.jackson.core", "JsonParser");
	public static final ClassName JSON_UTIL = 			ClassName.get("com.pocket.util.java", "JsonUtil");
	
	public static final ClassName OBJECT_UTIL = 		ClassName.get("org.apache.commons.lang3", "ObjectUtils");
	public static final ClassName STRING_UTILS = 		ClassName.get("org.apache.commons.lang3", "StringUtils");
	public static final ClassName ARRAY_UTILS = 		ClassName.get("org.apache.commons.lang3", "ArrayUtils");
	
	public static final ClassName STRING = 				ClassName.get(String.class);
	public static final ClassName INTEGER = 			ClassName.get(Integer.class);
	public static final ClassName SET = 				ClassName.get(Set.class);
	public static final ClassName ILLEGAL_ARG_EXCEPTION = ClassName.get(IllegalArgumentException.class);
	public static final ClassName RUNTIME_EX = 			ClassName.get(RuntimeException.class);

	public static final ClassName GRAPHQL_SELECTION_SET = 	ClassName.get("com.pocket.sync.source.protocol.graphql", "GraphQlSelectionSet");
	public static final ClassName GRAPHQL_SYNCABLE = 	ClassName.get("com.pocket.sync.source.protocol.graphql", "GraphQlSyncable");
	public static final ClassName GRAPHQL_FRAGMENT = 	ClassName.get("com.pocket.sync.source.protocol.graphql", "GraphQlFragment");
	public static final ClassName GRAPHQL_SUPPORT = 	ClassName.get("com.pocket.sync.source.protocol.graphql", "GraphQlSupport");
	public static final ClassName GRAPHQL_BUILDER = 	ClassName.get("com.pocket.sync.source.protocol.graphql", "GraphQlSelectionSetBuilder");
}
