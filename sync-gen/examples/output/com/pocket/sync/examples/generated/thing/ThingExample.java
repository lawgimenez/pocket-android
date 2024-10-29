package com.pocket.sync.examples.generated.thing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.examples.generated.ExamplesAuthType;
import com.pocket.sync.examples.generated.ExamplesRemoteStyle;
import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.Remote.Method;
import com.pocket.sync.source.protocol.graphql.GraphQlSupport;
import com.pocket.sync.source.protocol.graphql.GraphQlSyncable;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.space.Change;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.mutable.MutableThing;
import com.pocket.sync.space.mutable.Mutables;
import com.pocket.sync.spec.Reactions;
import com.pocket.sync.thing.FlatUtils;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingBuilder;
import com.pocket.sync.thing.ThingUtil;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.BaseModeller;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.StreamingThingParser;
import com.pocket.sync.value.SyncableParser;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.binary.ByteWriter;
import com.pocket.sync.value.protect.StringEncrypter;
import com.pocket.util.java.JsonUtil;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An example thing with all of the syntax features
 */
public final class ThingExample implements GraphQlSyncable, Thing {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<ThingExample> JSON_CREATOR = ThingExample::from;

    public static final StreamingThingParser<ThingExample> STREAMING_JSON_CREATOR = ThingExample::from;

    public static final String THING_TYPE = "ThingExample";

    public static final Remote REMOTE = new Remote("endpoint/{.val}", Remote.Method.GET, ExamplesRemoteStyle.REMOTE, "hash_target");

    public static final ByteTypeParser<ThingExample> BYTE_CREATOR = ThingExample::uncompress;

    @Nullable
    public final String id;

    @Nullable
    public final String id_derived;

    @Nullable
    public final String hash_target;

    @Nullable
    public final String val;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<String> val_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, String> val_map;

    @Nullable
    public final Boolean bool;

    @Nullable
    public final IdentifiableByValue id_thing;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<IdentifiableByValue> id_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, IdentifiableByValue> id_map;

    @Nullable
    public final NonIdentifiable non_id_thing;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<NonIdentifiable> non_id_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, NonIdentifiable> non_id_map;

    @Nullable
    public final String derived_first_available;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<IdentifiableByValue> remap_target;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<String> derived_remap;

    @Nullable
    public final String reactive_to_type;

    @Nullable
    public final String reactive_to_type_field;

    @Nullable
    public final String reactive_to_sibling_field;

    @Nullable
    public final String reactive_to_any_sibling_field;

    @Nullable
    public final String reactive_collection_field;

    @Nullable
    public final String dangerous;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<String> dangerous_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, String> dangerous_map;

    @Nullable
    public final HasDangerousValue has_dangerous;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<HasDangerousValue> has_dangerous_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, HasDangerousValue> has_dangerous_map;

    @Nullable
    public final VarietyExample variety;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<VarietyExample> variety_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, VarietyExample> variety_map;

    @Nullable
    public final InterfaceExample interface_;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final List<InterfaceExample> interface_list;

    /**
     *  <p><i>Like Models in general, <b>the returned collection is immutable.</b> Attempts to modify it will throw an exception.</i>
     */
    @Nullable
    public final Map<String, InterfaceExample> interface_map;

    public final Declared declared;

    private ThingExample _identity;

    /**
     * Lazy init'd and cached during idkey()
     */
    private String _idkey;

    private ThingExample(Builder builder, Declared declared) {
        this.declared = declared;
        this.id = builder.id;
        this.id_derived = builder.id_derived;
        this.hash_target = builder.hash_target;
        this.val = builder.val;
        this.val_list = builder.val_list;
        this.val_map = builder.val_map;
        this.bool = builder.bool;
        this.id_thing = builder.id_thing;
        this.id_list = builder.id_list;
        this.id_map = builder.id_map;
        this.non_id_thing = builder.non_id_thing;
        this.non_id_list = builder.non_id_list;
        this.non_id_map = builder.non_id_map;
        this.derived_first_available = builder.derived_first_available;
        this.remap_target = builder.remap_target;
        this.derived_remap = builder.derived_remap;
        this.reactive_to_type = builder.reactive_to_type;
        this.reactive_to_type_field = builder.reactive_to_type_field;
        this.reactive_to_sibling_field = builder.reactive_to_sibling_field;
        this.reactive_to_any_sibling_field = builder.reactive_to_any_sibling_field;
        this.reactive_collection_field = builder.reactive_collection_field;
        this.dangerous = builder.dangerous;
        this.dangerous_list = builder.dangerous_list;
        this.dangerous_map = builder.dangerous_map;
        this.has_dangerous = builder.has_dangerous;
        this.has_dangerous_list = builder.has_dangerous_list;
        this.has_dangerous_map = builder.has_dangerous_map;
        this.variety = builder.variety;
        this.variety_list = builder.variety_list;
        this.variety_map = builder.variety_map;
        this.interface_ = builder.interface_;
        this.interface_list = builder.interface_list;
        this.interface_map = builder.interface_map;
    }

    @NotNull
    @Override
    public GraphQlSupport graphQl() {
        return GRAPHQL;
    }

    @Override
    public ExamplesAuthType auth() {
        return ExamplesAuthType.USER;
    }

    public static ThingExample from(JsonNode jsonNode, JsonConfig _config, Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("id");
        if (value != null) builder.id(Modeller.asString(value));
        value = json.get("id_derived");
        if (value != null) builder.id_derived(Modeller.asString(value));
        value = json.get("hash_target");
        if (value != null) builder.hash_target(Modeller.asString(value));
        value = json.get("val");
        if (value != null) builder.val(Modeller.asString(value));
        value = json.get("val_list");
        if (value != null) builder.val_list(Modeller.asList(value, Modeller.STRING_CREATOR));
        value = json.get("val_map");
        if (value != null) builder.val_map(Modeller.asMap(value, Modeller.STRING_CREATOR));
        value = json.get("bool");
        if (value != null) builder.bool(Modeller.asBoolean(value));
        value = json.get("id_thing");
        if (value != null) builder.id_thing(IdentifiableByValue.from(value, _config, allowed));
        value = json.get("id_list");
        if (value != null) builder.id_list(Modeller.asList(value, IdentifiableByValue.JSON_CREATOR, _config, allowed));
        value = json.get("id_map");
        if (value != null) builder.id_map(Modeller.asMap(value, IdentifiableByValue.JSON_CREATOR, _config, allowed));
        value = json.get("non_id_thing");
        if (value != null) builder.non_id_thing(NonIdentifiable.from(value, _config, allowed));
        value = json.get("non_id_list");
        if (value != null) builder.non_id_list(Modeller.asList(value, NonIdentifiable.JSON_CREATOR, _config, allowed));
        value = json.get("non_id_map");
        if (value != null) builder.non_id_map(Modeller.asMap(value, NonIdentifiable.JSON_CREATOR, _config, allowed));
        value = json.get("derived_first_available");
        if (value != null) builder.derived_first_available(Modeller.asString(value));
        value = json.get("remap_target");
        if (value != null) builder.remap_target(Modeller.asList(value, IdentifiableByValue.JSON_CREATOR, _config, allowed));
        List<String> derived_remap = Modeller.remap(json.get("remap_target"), "val", Modeller.STRING_CREATOR);
        if (derived_remap != null) builder.derived_remap(derived_remap);
        value = json.get("reactive_to_type");
        if (value != null) builder.reactive_to_type(Modeller.asString(value));
        value = json.get("reactive_to_type_field");
        if (value != null) builder.reactive_to_type_field(Modeller.asString(value));
        value = json.get("reactive_to_sibling_field");
        if (value != null) builder.reactive_to_sibling_field(Modeller.asString(value));
        value = json.get("reactive_to_any_sibling_field");
        if (value != null) builder.reactive_to_any_sibling_field(Modeller.asString(value));
        value = json.get("reactive_collection_field");
        if (value != null) builder.reactive_collection_field(Modeller.asString(value));
        value = json.get("dangerous");
        if (value != null) builder.dangerous(Modeller.asDangerous(value));
        value = json.get("dangerous_list");
        if (value != null) builder.dangerous_list(Modeller.asList(value, Modeller.DANGEROUS_CREATOR));
        value = json.get("dangerous_map");
        if (value != null) builder.dangerous_map(Modeller.asMap(value, Modeller.DANGEROUS_CREATOR));
        value = json.get("has_dangerous");
        if (value != null) builder.has_dangerous(HasDangerousValue.from(value, _config, allowed));
        value = json.get("has_dangerous_list");
        if (value != null) builder.has_dangerous_list(Modeller.asList(value, HasDangerousValue.JSON_CREATOR, _config, allowed));
        value = json.get("has_dangerous_map");
        if (value != null) builder.has_dangerous_map(Modeller.asMap(value, HasDangerousValue.JSON_CREATOR, _config, allowed));
        value = json.get("variety");
        if (value != null) builder.variety(VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR.create(value, _config, allowed));
        value = json.get("variety_list");
        if (value != null) builder.variety_list(Modeller.asList(value, VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _config, allowed));
        value = json.get("variety_map");
        if (value != null) builder.variety_map(Modeller.asMap(value, VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _config, allowed));
        value = json.get("interface");
        if (value != null) builder.interface_(InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR.create(value, _config, allowed));
        value = json.get("interface_list");
        if (value != null) builder.interface_list(Modeller.asList(value, InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _config, allowed));
        value = json.get("interface_map");
        if (value != null) builder.interface_map(Modeller.asMap(value, InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _config, allowed));
        return builder.build();
    }

    public static ThingExample from(JsonParser parser, JsonConfig _config, Allow... allowed) throws
            IOException {
        if (parser == null) return null;
        if (parser.currentToken() == null) parser.nextToken();
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        if (!parser.isExpectedStartObjectToken()) throw new RuntimeException("Unexpected start token " + JsonUtil.errorLocation(parser));
        final Builder builder = new Builder();
        while (parser.nextToken() != JsonToken.END_OBJECT && !parser.isClosed()) {
            final String currentName = parser.getCurrentName();
            parser.nextToken();
            if (currentName == null) {
                parser.skipChildren();
            } else if (currentName.equals("id")) {
                builder.id(Modeller.asString(parser));
            } else if (currentName.equals("id_derived")) {
                builder.id_derived(Modeller.asString(parser));
            } else if (currentName.equals("hash_target")) {
                builder.hash_target(Modeller.asString(parser));
            } else if (currentName.equals("val")) {
                builder.val(Modeller.asString(parser));
            } else if (currentName.equals("val_list")) {
                builder.val_list(Modeller.asList(parser, Modeller.STRING_STREAMING_CREATOR));
            } else if (currentName.equals("val_map")) {
                builder.val_map(Modeller.asMap(parser, Modeller.STRING_STREAMING_CREATOR));
            } else if (currentName.equals("bool")) {
                builder.bool(Modeller.asBoolean(parser));
            } else if (currentName.equals("id_thing")) {
                builder.id_thing(IdentifiableByValue.from(parser, _config, allowed));
            } else if (currentName.equals("id_list")) {
                builder.id_list(Modeller.asList(parser, IdentifiableByValue.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("id_map")) {
                builder.id_map(Modeller.asMap(parser, IdentifiableByValue.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("non_id_thing")) {
                builder.non_id_thing(NonIdentifiable.from(parser, _config, allowed));
            } else if (currentName.equals("non_id_list")) {
                builder.non_id_list(Modeller.asList(parser, NonIdentifiable.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("non_id_map")) {
                builder.non_id_map(Modeller.asMap(parser, NonIdentifiable.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("derived_first_available")) {
                builder.derived_first_available(Modeller.asString(parser));
            } else if (currentName.equals("remap_target")) {
                // WARNING: LEAVING STREAMING MODE
                // To handle remaps we currently parse the field using the object mapper.
                // This way we can iterate through the list multiple times, so we can build both
                // the actual list and all the remapped lists.
                // We could do it fully in streaming mode to make sure we get the best performance,
                // but we would have to add fake fields to Item (or other remap targets) that aren't part of its state
                // or figure out another way to pass these extra passed values to the parent thing (like Get)
                // so it could build the remap fields from them.
                final JsonNode value = Modeller.OBJECT_MAPPER.readTree(parser);
                if (value != null) builder.remap_target(Modeller.asList(value, IdentifiableByValue.JSON_CREATOR, _config));
                List<String> derived_remap = Modeller.remap(value, "val", Modeller.STRING_CREATOR);
                if (derived_remap != null) builder.derived_remap(derived_remap);
            } else if (currentName.equals("reactive_to_type")) {
                builder.reactive_to_type(Modeller.asString(parser));
            } else if (currentName.equals("reactive_to_type_field")) {
                builder.reactive_to_type_field(Modeller.asString(parser));
            } else if (currentName.equals("reactive_to_sibling_field")) {
                builder.reactive_to_sibling_field(Modeller.asString(parser));
            } else if (currentName.equals("reactive_to_any_sibling_field")) {
                builder.reactive_to_any_sibling_field(Modeller.asString(parser));
            } else if (currentName.equals("reactive_collection_field")) {
                builder.reactive_collection_field(Modeller.asString(parser));
            } else if (currentName.equals("dangerous")) {
                builder.dangerous(Modeller.asDangerous(parser));
            } else if (currentName.equals("dangerous_list")) {
                builder.dangerous_list(Modeller.asList(parser, Modeller.DANGEROUS_STREAMING_CREATOR));
            } else if (currentName.equals("dangerous_map")) {
                builder.dangerous_map(Modeller.asMap(parser, Modeller.DANGEROUS_STREAMING_CREATOR));
            } else if (currentName.equals("has_dangerous")) {
                builder.has_dangerous(HasDangerousValue.from(parser, _config, allowed));
            } else if (currentName.equals("has_dangerous_list")) {
                builder.has_dangerous_list(Modeller.asList(parser, HasDangerousValue.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("has_dangerous_map")) {
                builder.has_dangerous_map(Modeller.asMap(parser, HasDangerousValue.STREAMING_JSON_CREATOR, _config, allowed));
            } else if (currentName.equals("variety")) {
                builder.variety(VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR.create(parser, _config, allowed));
            } else if (currentName.equals("variety_list")) {
                builder.variety_list(Modeller.asList(parser, VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _config, allowed));
            } else if (currentName.equals("variety_map")) {
                builder.variety_map(Modeller.asMap(parser, VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _config, allowed));
            } else if (currentName.equals("interface")) {
                builder.interface_(InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR.create(parser, _config, allowed));
            } else if (currentName.equals("interface_list")) {
                builder.interface_list(Modeller.asList(parser, InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _config, allowed));
            } else if (currentName.equals("interface_map")) {
                builder.interface_map(Modeller.asMap(parser, InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _config, allowed));
            } else {
                parser.skipChildren();
            }
        }
        return builder.build();
    }

    @Override
    public ObjectNode toJson(JsonConfig _config, Include... includes) {
        ObjectNode json = Modeller.OBJECT_MAPPER.createObjectNode();
        if (Include.contains(includes, Include.OPEN_TYPE)) {
            json.put(Thing.SERIALIZATION_TYPE_KEY, THING_TYPE);
            includes = Include.removeAssumingPresent(includes, Include.OPEN_TYPE);
        }
        boolean includeDangerous = Include.contains(includes, Include.DANGEROUS);
        if (declared.bool) json.put("bool", Modeller.toJsonValue(bool));
        if (includeDangerous && declared.dangerous) json.put("dangerous", Modeller.toJsonValue(dangerous, includes));
        if (includeDangerous && declared.dangerous_list) json.put("dangerous_list", Modeller.toJsonValue(dangerous_list, includes));
        if (includeDangerous && declared.dangerous_map) json.put("dangerous_map", Modeller.toJsonValue(dangerous_map, includes));
        if (declared.derived_first_available) json.put("derived_first_available", Modeller.toJsonValue(derived_first_available));
        if (declared.derived_remap) json.put("derived_remap", Modeller.toJsonValue(derived_remap, _config, includes));
        if (declared.has_dangerous) json.put("has_dangerous", Modeller.toJsonValue(has_dangerous, _config, includes));
        if (declared.has_dangerous_list) json.put("has_dangerous_list", Modeller.toJsonValue(has_dangerous_list, _config, includes));
        if (declared.has_dangerous_map) json.put("has_dangerous_map", Modeller.toJsonValue(has_dangerous_map, _config, includes));
        if (declared.hash_target) json.put("hash_target", Modeller.toJsonValue(hash_target));
        if (declared.id) json.put("id", Modeller.toJsonValue(id));
        if (declared.id_derived) json.put("id_derived", Modeller.toJsonValue(id_derived));
        if (declared.id_list) json.put("id_list", Modeller.toJsonValue(id_list, _config, includes));
        if (declared.id_map) json.put("id_map", Modeller.toJsonValue(id_map, _config, includes));
        if (declared.id_thing) json.put("id_thing", Modeller.toJsonValue(id_thing, _config, includes));
        if (declared.interface_) json.put("interface", Modeller.toJsonValue(interface_, _config, Include.add(includes, Include.OPEN_TYPE)));
        if (declared.interface_list) json.put("interface_list", Modeller.toJsonValue(interface_list, _config, Include.add(includes, Include.OPEN_TYPE)));
        if (declared.interface_map) json.put("interface_map", Modeller.toJsonValue(interface_map, _config, Include.add(includes, Include.OPEN_TYPE)));
        if (declared.non_id_list) json.put("non_id_list", Modeller.toJsonValue(non_id_list, _config, includes));
        if (declared.non_id_map) json.put("non_id_map", Modeller.toJsonValue(non_id_map, _config, includes));
        if (declared.non_id_thing) json.put("non_id_thing", Modeller.toJsonValue(non_id_thing, _config, includes));
        if (declared.reactive_collection_field) json.put("reactive_collection_field", Modeller.toJsonValue(reactive_collection_field));
        if (declared.reactive_to_any_sibling_field) json.put("reactive_to_any_sibling_field", Modeller.toJsonValue(reactive_to_any_sibling_field));
        if (declared.reactive_to_sibling_field) json.put("reactive_to_sibling_field", Modeller.toJsonValue(reactive_to_sibling_field));
        if (declared.reactive_to_type) json.put("reactive_to_type", Modeller.toJsonValue(reactive_to_type));
        if (declared.reactive_to_type_field) json.put("reactive_to_type_field", Modeller.toJsonValue(reactive_to_type_field));
        if (declared.remap_target) json.put("remap_target", Modeller.toJsonValue(remap_target, _config, includes));
        if (declared.val) json.put("val", Modeller.toJsonValue(val));
        if (declared.val_list) json.put("val_list", Modeller.toJsonValue(val_list, _config, includes));
        if (declared.val_map) json.put("val_map", Modeller.toJsonValue(val_map, _config, includes));
        if (declared.variety) json.put("variety", Modeller.toJsonValue(variety, _config, Include.add(includes, Include.OPEN_TYPE)));
        if (declared.variety_list) json.put("variety_list", Modeller.toJsonValue(variety_list, _config, Include.add(includes, Include.OPEN_TYPE)));
        if (declared.variety_map) json.put("variety_map", Modeller.toJsonValue(variety_map, _config, Include.add(includes, Include.OPEN_TYPE)));
        return json;
    }

    @Override
    public String toString() {
        return toJson(new JsonConfig(REMOTE.style, true), Include.OPEN_TYPE).toString();
    }

    @Override
    public String type() {
        return THING_TYPE;
    }

    @Override
    public Remote remote() {
        return REMOTE;
    }

    @Override
    public SyncableParser getCreator() {
        return JSON_CREATOR;
    }

    @Override
    public StreamingThingParser getStreamingCreator() {
        return STREAMING_JSON_CREATOR;
    }

    @Override
    public ByteTypeParser getByteCreator() {
        return BYTE_CREATOR;
    }

    @Override
    public ThingExample identity() {
        if (_identity != null) return _identity;
        _identity = new IdBuilder(this).build();
        _identity._identity = _identity;
        return _identity;
    }

    @Override
    public boolean isIdentifiable() {
        return true;
    }

    @Override
    public String rootValue() {
        return null;
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
        if (declared.id) map.put("id", id);
        if (declared.id_derived) map.put("id_derived", id_derived);
        if (declared.hash_target) map.put("hash_target", hash_target);
        if (declared.val) map.put("val", val);
        if (declared.val_list) map.put("val_list", val_list);
        if (declared.val_map) map.put("val_map", val_map);
        if (declared.bool) map.put("bool", bool);
        if (declared.id_thing) map.put("id_thing", id_thing);
        if (declared.id_list) map.put("id_list", id_list);
        if (declared.id_map) map.put("id_map", id_map);
        if (declared.non_id_thing) map.put("non_id_thing", non_id_thing);
        if (declared.non_id_list) map.put("non_id_list", non_id_list);
        if (declared.non_id_map) map.put("non_id_map", non_id_map);
        if (declared.derived_first_available) map.put("derived_first_available", derived_first_available);
        if (declared.remap_target) map.put("remap_target", remap_target);
        if (declared.derived_remap) map.put("derived_remap", derived_remap);
        if (declared.reactive_to_type) map.put("reactive_to_type", reactive_to_type);
        if (declared.reactive_to_type_field) map.put("reactive_to_type_field", reactive_to_type_field);
        if (declared.reactive_to_sibling_field) map.put("reactive_to_sibling_field", reactive_to_sibling_field);
        if (declared.reactive_to_any_sibling_field) map.put("reactive_to_any_sibling_field", reactive_to_any_sibling_field);
        if (declared.reactive_collection_field) map.put("reactive_collection_field", reactive_collection_field);
        if (includeDangerous && declared.dangerous) map.put("dangerous", dangerous);
        if (includeDangerous && declared.dangerous_list) map.put("dangerous_list", dangerous_list);
        if (includeDangerous && declared.dangerous_map) map.put("dangerous_map", dangerous_map);
        if (declared.has_dangerous) map.put("has_dangerous", has_dangerous);
        if (declared.has_dangerous_list) map.put("has_dangerous_list", has_dangerous_list);
        if (declared.has_dangerous_map) map.put("has_dangerous_map", has_dangerous_map);
        if (declared.variety) map.put("variety", variety);
        if (declared.variety_list) map.put("variety_list", variety_list);
        if (declared.variety_map) map.put("variety_map", variety_map);
        if (declared.interface_) map.put("interface", interface_);
        if (declared.interface_list) map.put("interface_list", interface_list);
        if (declared.interface_map) map.put("interface_map", interface_map);
        return map;
    }

    @Override
    public int hashCode() {
        return hashCode(Thing.Equality.IDENTITY);
    }

    @Override
    public int hashCode(Thing.Equality e) {
        int _result = 0;
        if (e == null) e = Thing.Equality.IDENTITY;
        _result = 31 * _result + (id != null ? id.hashCode() : 0);
        _result = 31 * _result + (id_derived != null ? id_derived.hashCode() : 0);
        _result = 31 * _result + (hash_target != null ? hash_target.hashCode() : 0);
        if (e == Thing.Equality.IDENTITY) return _result;
        _result = 31 * _result + (val != null ? val.hashCode() : 0);
        _result = 31 * _result + (val_list != null ? val_list.hashCode() : 0);
        _result = 31 * _result + (val_map != null ? val_map.hashCode() : 0);
        _result = 31 * _result + (bool != null ? bool.hashCode() : 0);
        _result = 31 * _result + ThingUtil.fieldHashCode(e, id_thing);
        _result = 31 * _result + (id_list != null ? ThingUtil.collectionHashCode(e, id_list) : 0);
        _result = 31 * _result + (id_map != null ? ThingUtil.mapHashCode(e, id_map) : 0);
        _result = 31 * _result + ThingUtil.fieldHashCode(e, non_id_thing);
        _result = 31 * _result + (non_id_list != null ? ThingUtil.collectionHashCode(e, non_id_list) : 0);
        _result = 31 * _result + (non_id_map != null ? ThingUtil.mapHashCode(e, non_id_map) : 0);
        _result = 31 * _result + (derived_first_available != null ? derived_first_available.hashCode() : 0);
        _result = 31 * _result + (remap_target != null ? ThingUtil.collectionHashCode(e, remap_target) : 0);
        _result = 31 * _result + (derived_remap != null ? derived_remap.hashCode() : 0);
        _result = 31 * _result + (reactive_to_type != null ? reactive_to_type.hashCode() : 0);
        _result = 31 * _result + (reactive_to_type_field != null ? reactive_to_type_field.hashCode() : 0);
        _result = 31 * _result + (reactive_to_sibling_field != null ? reactive_to_sibling_field.hashCode() : 0);
        _result = 31 * _result + (reactive_to_any_sibling_field != null ? reactive_to_any_sibling_field.hashCode() : 0);
        _result = 31 * _result + (reactive_collection_field != null ? reactive_collection_field.hashCode() : 0);
        _result = 31 * _result + (dangerous != null ? dangerous.hashCode() : 0);
        _result = 31 * _result + (dangerous_list != null ? dangerous_list.hashCode() : 0);
        _result = 31 * _result + (dangerous_map != null ? dangerous_map.hashCode() : 0);
        _result = 31 * _result + ThingUtil.fieldHashCode(e, has_dangerous);
        _result = 31 * _result + (has_dangerous_list != null ? ThingUtil.collectionHashCode(e, has_dangerous_list) : 0);
        _result = 31 * _result + (has_dangerous_map != null ? ThingUtil.mapHashCode(e, has_dangerous_map) : 0);
        _result = 31 * _result + (variety != null ? variety.hashCode() : 0);
        _result = 31 * _result + (variety_list != null ? variety_list.hashCode() : 0);
        _result = 31 * _result + (variety_map != null ? variety_map.hashCode() : 0);
        _result = 31 * _result + ThingUtil.fieldHashCode(e, interface_);
        _result = 31 * _result + (interface_list != null ? ThingUtil.collectionHashCode(e, interface_list) : 0);
        _result = 31 * _result + (interface_map != null ? ThingUtil.mapHashCode(e, interface_map) : 0);
        return _result;
    }

    @Override
    public boolean equals(Object o) {
        return equals(Thing.Equality.IDENTITY, o);
    }

    @Override
    public boolean equals(Thing.Equality e, Object o) {
        if (e == null) e = Thing.Equality.IDENTITY;
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThingExample that = (ThingExample) o;
        if (e == Thing.Equality.STATE_DECLARED) {
            if (that.declared.id && declared.id) if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (that.declared.id_derived && declared.id_derived) if (id_derived != null ? !id_derived.equals(that.id_derived) : that.id_derived != null) return false;
            if (that.declared.hash_target && declared.hash_target) if (hash_target != null ? !hash_target.equals(that.hash_target) : that.hash_target != null) return false;
            if (that.declared.val && declared.val) if (val != null ? !val.equals(that.val) : that.val != null) return false;
            if (that.declared.val_list && declared.val_list) if (val_list != null ? !val_list.equals(that.val_list) : that.val_list != null) return false;
            if (that.declared.val_map && declared.val_map) if (val_map != null ? !val_map.equals(that.val_map) : that.val_map != null) return false;
            if (that.declared.bool && declared.bool) if (bool != null ? !bool.equals(that.bool) : that.bool != null) return false;
            if (that.declared.id_thing && declared.id_thing) if (!ThingUtil.fieldEquals(e, id_thing, that.id_thing)) return false;
            if (that.declared.id_list && declared.id_list) if (!ThingUtil.listEquals(e, id_list, that.id_list)) return false;
            if (that.declared.id_map && declared.id_map) if (!ThingUtil.mapEquals(e, id_map, that.id_map)) return false;
            if (that.declared.non_id_thing && declared.non_id_thing) if (!ThingUtil.fieldEquals(e, non_id_thing, that.non_id_thing)) return false;
            if (that.declared.non_id_list && declared.non_id_list) if (!ThingUtil.listEquals(e, non_id_list, that.non_id_list)) return false;
            if (that.declared.non_id_map && declared.non_id_map) if (!ThingUtil.mapEquals(e, non_id_map, that.non_id_map)) return false;
            if (that.declared.derived_first_available && declared.derived_first_available) if (derived_first_available != null ? !derived_first_available.equals(that.derived_first_available) : that.derived_first_available != null) return false;
            if (that.declared.remap_target && declared.remap_target) if (!ThingUtil.listEquals(e, remap_target, that.remap_target)) return false;
            if (that.declared.derived_remap && declared.derived_remap) if (derived_remap != null ? !derived_remap.equals(that.derived_remap) : that.derived_remap != null) return false;
            if (that.declared.reactive_to_type && declared.reactive_to_type) if (reactive_to_type != null ? !reactive_to_type.equals(that.reactive_to_type) : that.reactive_to_type != null) return false;
            if (that.declared.reactive_to_type_field && declared.reactive_to_type_field) if (reactive_to_type_field != null ? !reactive_to_type_field.equals(that.reactive_to_type_field) : that.reactive_to_type_field != null) return false;
            if (that.declared.reactive_to_sibling_field && declared.reactive_to_sibling_field) if (reactive_to_sibling_field != null ? !reactive_to_sibling_field.equals(that.reactive_to_sibling_field) : that.reactive_to_sibling_field != null) return false;
            if (that.declared.reactive_to_any_sibling_field && declared.reactive_to_any_sibling_field) if (reactive_to_any_sibling_field != null ? !reactive_to_any_sibling_field.equals(that.reactive_to_any_sibling_field) : that.reactive_to_any_sibling_field != null) return false;
            if (that.declared.reactive_collection_field && declared.reactive_collection_field) if (reactive_collection_field != null ? !reactive_collection_field.equals(that.reactive_collection_field) : that.reactive_collection_field != null) return false;
            if (that.declared.dangerous && declared.dangerous) if (dangerous != null ? !dangerous.equals(that.dangerous) : that.dangerous != null) return false;
            if (that.declared.dangerous_list && declared.dangerous_list) if (dangerous_list != null ? !dangerous_list.equals(that.dangerous_list) : that.dangerous_list != null) return false;
            if (that.declared.dangerous_map && declared.dangerous_map) if (dangerous_map != null ? !dangerous_map.equals(that.dangerous_map) : that.dangerous_map != null) return false;
            if (that.declared.has_dangerous && declared.has_dangerous) if (!ThingUtil.fieldEquals(e, has_dangerous, that.has_dangerous)) return false;
            if (that.declared.has_dangerous_list && declared.has_dangerous_list) if (!ThingUtil.listEquals(e, has_dangerous_list, that.has_dangerous_list)) return false;
            if (that.declared.has_dangerous_map && declared.has_dangerous_map) if (!ThingUtil.mapEquals(e, has_dangerous_map, that.has_dangerous_map)) return false;
            if (that.declared.variety && declared.variety) if (variety != null ? !variety.equals(that.variety) : that.variety != null) return false;
            if (that.declared.variety_list && declared.variety_list) if (variety_list != null ? !variety_list.equals(that.variety_list) : that.variety_list != null) return false;
            if (that.declared.variety_map && declared.variety_map) if (variety_map != null ? !variety_map.equals(that.variety_map) : that.variety_map != null) return false;
            if (that.declared.interface_ && declared.interface_) if (!ThingUtil.fieldEquals(e, interface_, that.interface_)) return false;
            if (that.declared.interface_list && declared.interface_list) if (!ThingUtil.listEquals(e, interface_list, that.interface_list)) return false;
            if (that.declared.interface_map && declared.interface_map) if (!ThingUtil.mapEquals(e, interface_map, that.interface_map)) return false;
            return true;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (id_derived != null ? !id_derived.equals(that.id_derived) : that.id_derived != null) return false;
        if (hash_target != null ? !hash_target.equals(that.hash_target) : that.hash_target != null) return false;
        if (e == Thing.Equality.IDENTITY) return true;
        if (val != null ? !val.equals(that.val) : that.val != null) return false;
        if (val_list != null ? !val_list.equals(that.val_list) : that.val_list != null) return false;
        if (val_map != null ? !val_map.equals(that.val_map) : that.val_map != null) return false;
        if (bool != null ? !bool.equals(that.bool) : that.bool != null) return false;
        if (!ThingUtil.fieldEquals(e, id_thing, that.id_thing)) return false;
        if (!ThingUtil.listEquals(e, id_list, that.id_list)) return false;
        if (!ThingUtil.mapEquals(e, id_map, that.id_map)) return false;
        if (!ThingUtil.fieldEquals(e, non_id_thing, that.non_id_thing)) return false;
        if (!ThingUtil.listEquals(e, non_id_list, that.non_id_list)) return false;
        if (!ThingUtil.mapEquals(e, non_id_map, that.non_id_map)) return false;
        if (derived_first_available != null ? !derived_first_available.equals(that.derived_first_available) : that.derived_first_available != null) return false;
        if (!ThingUtil.listEquals(e, remap_target, that.remap_target)) return false;
        if (derived_remap != null ? !derived_remap.equals(that.derived_remap) : that.derived_remap != null) return false;
        if (reactive_to_type != null ? !reactive_to_type.equals(that.reactive_to_type) : that.reactive_to_type != null) return false;
        if (reactive_to_type_field != null ? !reactive_to_type_field.equals(that.reactive_to_type_field) : that.reactive_to_type_field != null) return false;
        if (reactive_to_sibling_field != null ? !reactive_to_sibling_field.equals(that.reactive_to_sibling_field) : that.reactive_to_sibling_field != null) return false;
        if (reactive_to_any_sibling_field != null ? !reactive_to_any_sibling_field.equals(that.reactive_to_any_sibling_field) : that.reactive_to_any_sibling_field != null) return false;
        if (reactive_collection_field != null ? !reactive_collection_field.equals(that.reactive_collection_field) : that.reactive_collection_field != null) return false;
        if (dangerous != null ? !dangerous.equals(that.dangerous) : that.dangerous != null) return false;
        if (dangerous_list != null ? !dangerous_list.equals(that.dangerous_list) : that.dangerous_list != null) return false;
        if (dangerous_map != null ? !dangerous_map.equals(that.dangerous_map) : that.dangerous_map != null) return false;
        if (!ThingUtil.fieldEquals(e, has_dangerous, that.has_dangerous)) return false;
        if (!ThingUtil.listEquals(e, has_dangerous_list, that.has_dangerous_list)) return false;
        if (!ThingUtil.mapEquals(e, has_dangerous_map, that.has_dangerous_map)) return false;
        if (variety != null ? !variety.equals(that.variety) : that.variety != null) return false;
        if (variety_list != null ? !variety_list.equals(that.variety_list) : that.variety_list != null) return false;
        if (variety_map != null ? !variety_map.equals(that.variety_map) : that.variety_map != null) return false;
        if (!ThingUtil.fieldEquals(e, interface_, that.interface_)) return false;
        if (!ThingUtil.listEquals(e, interface_list, that.interface_list)) return false;
        if (!ThingUtil.mapEquals(e, interface_map, that.interface_map)) return false;
        return true;
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public Mutable mutable(Mutables mutables, MutableThing root) {
        return new Mutable(this, mutables);
    }

    @Override
    public String idkey() {
        if (_idkey != null) return _idkey;
        ByteWriter writer = new ByteWriter();
        writer.writeString(THING_TYPE);
        writer.writeString(identity().toJson(NO_ALIASES, Include.DANGEROUS).toString());
        _idkey = writer.sha256();
        return _idkey;
    }

    @Override
    public void reactions(Thing was_t, Thing is_t, Diff diff, Reactions output) {
        ThingExample was = (ThingExample) was_t;
        ThingExample is = (ThingExample) is_t;
        if (!is.declared.id_derived) {
            output.thing(this, "id_derived");
        }
        if (!is.declared.derived_first_available) {
            output.thing(this, "derived_first_available");
        }
        if (!is.declared.derived_remap) {
            output.thing(this, "derived_remap");
        }
        if (!is.declared.reactive_to_type) {
            output.thing(this, "reactive_to_type");
        }
        if (!is.declared.reactive_to_type_field) {
            output.thing(this, "reactive_to_type_field");
        }
        if (!is.declared.reactive_to_sibling_field) {
            output.thing(this, "reactive_to_sibling_field");
        }
        if (!is.declared.reactive_to_any_sibling_field) {
            output.thing(this, "reactive_to_any_sibling_field");
        }
        if (!is.declared.reactive_collection_field) {
            output.thing(this, "reactive_collection_field");
        }
        if (!(is != null && is.declared.reactive_to_any_sibling_field && (was == null || !(was != null && was.declared.reactive_to_any_sibling_field) || ObjectUtils.notEqual(was != null ? was.reactive_to_any_sibling_field : null, is != null ? is.reactive_to_any_sibling_field : null)))) {
            output.thing(this, "reactive_to_any_sibling_field");
        }
        if (is != null && is.declared.id && (was == null || !(was != null && was.declared.id) || ObjectUtils.notEqual(was != null ? was.id : null, is != null ? is.id : null))) {
            output.thing(this, "derived_first_available");
            output.thing(this, "id_derived");
        }
        boolean id_list___id_list_val___changed = false;
        List<IdentifiableByValue> id_list___id_list_val___changed_tmp = was != null ? was.id_list : null;
        if (id_list___id_list_val___changed_tmp != null) {
            for (IdentifiableByValue t : id_list___id_list_val___changed_tmp) {
                Change<IdentifiableByValue> i = diff.find(t);
                if (i != null) {
                    if (i.latest != null && i.latest.declared.val && (i.previous == null || !(i.previous != null && i.previous.declared.val) || ObjectUtils.notEqual(i.previous != null ? i.previous.val : null, i.latest != null ? i.latest.val : null))) {
                        id_list___id_list_val___changed = true;
                        break;
                    }
                }
            }
        }
        if (id_list___id_list_val___changed) {
            output.thing(this, "reactive_collection_field");
        }
        if (is != null && is.declared.remap_target && (was == null || !(was != null && was.declared.remap_target) || ObjectUtils.notEqual(was != null ? was.remap_target : null, is != null ? is.remap_target : null))) {
            output.thing(this, "derived_remap");
        }
        if (is != null && is.declared.val && (was == null || !(was != null && was.declared.val) || ObjectUtils.notEqual(was != null ? was.val : null, is != null ? is.val : null))) {
            output.thing(this, "derived_first_available");
            output.thing(this, "id_derived");
            output.thing(this, "reactive_to_sibling_field");
        }
    }

    @Override
    public void subthings(FlatUtils.Output out) {
        if (id_thing != null) out.add(id_thing, false);
        if (id_list != null) out.addAll(id_list, false);
        if (id_map != null) out.addAll(id_map, false);
        if (remap_target != null) out.addAll(remap_target, false);
    }

    @Override
    public ThingExample flat() {
        Builder builder = builder();
        if (id_thing != null) builder.id_thing(id_thing.identity());
        if (id_list != null && !id_list.isEmpty()) {
            List<IdentifiableByValue> _list = new ArrayList<>(id_list);
            for (int i = 0, len = _list.size(); i < len; i++) {
                IdentifiableByValue v = _list.get(i);
                if (v != null) _list.set(i, v.identity());
            }
            builder.id_list(_list);
        }
        if (id_map != null && !id_map.isEmpty()) {
            Map<String,IdentifiableByValue> _map = new HashMap<>(id_map);
            for (Map.Entry<String,IdentifiableByValue> e : _map.entrySet()) {
                IdentifiableByValue v = e.getValue();
                if (v != null) _map.put(e.getKey(), v.identity());
            }
            builder.id_map(_map);
        }
        if (remap_target != null && !remap_target.isEmpty()) {
            List<IdentifiableByValue> _list = new ArrayList<>(remap_target);
            for (int i = 0, len = _list.size(); i < len; i++) {
                IdentifiableByValue v = _list.get(i);
                if (v != null) _list.set(i, v.identity());
            }
            builder.remap_target(_list);
        }
        return builder.build();
    }

    @Override
    public ThingExample with(Changes.ThingMatch match, Thing replace) {
        Object _replaced;
        _replaced = BaseModeller.with(id_thing, match, replace, false);
        if (_replaced != null) return new Builder(this).id_thing((IdentifiableByValue) _replaced).build();
        _replaced = BaseModeller.with(id_list, IdentifiableByValue.class, match, replace, false);
        if (_replaced != null) return new Builder(this).id_list((List<IdentifiableByValue>) _replaced).build();
        _replaced = BaseModeller.with(id_map, IdentifiableByValue.class, match, replace, false);
        if (_replaced != null) return new Builder(this).id_map((Map<String, IdentifiableByValue>) _replaced).build();
        _replaced = BaseModeller.with(remap_target, IdentifiableByValue.class, match, replace, false);
        if (_replaced != null) return new Builder(this).remap_target((List<IdentifiableByValue>) _replaced).build();
        return null;
    }

    @Override
    public ThingExample redact(StringEncrypter e) {
        Builder _builder = builder();
        if (dangerous != null) _builder.dangerous(Modeller.redact(dangerous, e));
        if (dangerous_list != null && !dangerous_list.isEmpty()) {
            List<String> _list = new ArrayList<>(dangerous_list.size());
            for (String _v : dangerous_list) {
                _list.add(Modeller.redact(_v, e));
            }
            _builder.dangerous_list(_list);
        }
        if (dangerous_map != null && !dangerous_map.isEmpty()) {
            Map<String,String> _map = new HashMap<>(dangerous_map.size());
            for (Map.Entry<String,String> _e : dangerous_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.redact(_e.getValue(), e));
            }
            _builder.dangerous_map(_map);
        }
        if (has_dangerous != null) _builder.has_dangerous(Modeller.redact(has_dangerous, e));
        if (has_dangerous_list != null && !has_dangerous_list.isEmpty()) {
            List<HasDangerousValue> _list = new ArrayList<>(has_dangerous_list.size());
            for (HasDangerousValue _v : has_dangerous_list) {
                _list.add(Modeller.redact(_v, e));
            }
            _builder.has_dangerous_list(_list);
        }
        if (has_dangerous_map != null && !has_dangerous_map.isEmpty()) {
            Map<String,HasDangerousValue> _map = new HashMap<>(has_dangerous_map.size());
            for (Map.Entry<String,HasDangerousValue> _e : has_dangerous_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.redact(_e.getValue(), e));
            }
            _builder.has_dangerous_map(_map);
        }
        if (interface_ != null) _builder.interface_(Modeller.redact(interface_, e));
        if (interface_list != null && !interface_list.isEmpty()) {
            List<InterfaceExample> _list = new ArrayList<>(interface_list.size());
            for (InterfaceExample _v : interface_list) {
                _list.add(Modeller.redact(_v, e));
            }
            _builder.interface_list(_list);
        }
        if (interface_map != null && !interface_map.isEmpty()) {
            Map<String,InterfaceExample> _map = new HashMap<>(interface_map.size());
            for (Map.Entry<String,InterfaceExample> _e : interface_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.redact(_e.getValue(), e));
            }
            _builder.interface_map(_map);
        }
        if (variety != null) _builder.variety(Modeller.redact(variety, e));
        if (variety_list != null && !variety_list.isEmpty()) {
            List<VarietyExample> _list = new ArrayList<>(variety_list.size());
            for (VarietyExample _v : variety_list) {
                _list.add(Modeller.redact(_v, e));
            }
            _builder.variety_list(_list);
        }
        if (variety_map != null && !variety_map.isEmpty()) {
            Map<String,VarietyExample> _map = new HashMap<>(variety_map.size());
            for (Map.Entry<String,VarietyExample> _e : variety_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.redact(_e.getValue(), e));
            }
            _builder.variety_map(_map);
        }
        return _builder.build();
    }

    @Override
    public ThingExample unredact(StringEncrypter e) {
        Builder _builder = builder();
        if (dangerous != null) _builder.dangerous(Modeller.unredact(dangerous, e));
        if (dangerous_list != null && !dangerous_list.isEmpty()) {
            List<String> _list = new ArrayList<>(dangerous_list.size());
            for (String _v : dangerous_list) {
                _list.add(Modeller.unredact(_v, e));
            }
            _builder.dangerous_list(_list);
        }
        if (dangerous_map != null && !dangerous_map.isEmpty()) {
            Map<String,String> _map = new HashMap<>(dangerous_map.size());
            for (Map.Entry<String,String> _e : dangerous_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.unredact(_e.getValue(), e));
            }
            _builder.dangerous_map(_map);
        }
        if (has_dangerous != null) _builder.has_dangerous(Modeller.unredact(has_dangerous, e));
        if (has_dangerous_list != null && !has_dangerous_list.isEmpty()) {
            List<HasDangerousValue> _list = new ArrayList<>(has_dangerous_list.size());
            for (HasDangerousValue _v : has_dangerous_list) {
                _list.add(Modeller.unredact(_v, e));
            }
            _builder.has_dangerous_list(_list);
        }
        if (has_dangerous_map != null && !has_dangerous_map.isEmpty()) {
            Map<String,HasDangerousValue> _map = new HashMap<>(has_dangerous_map.size());
            for (Map.Entry<String,HasDangerousValue> _e : has_dangerous_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.unredact(_e.getValue(), e));
            }
            _builder.has_dangerous_map(_map);
        }
        if (interface_ != null) _builder.interface_(Modeller.unredact(interface_, e));
        if (interface_list != null && !interface_list.isEmpty()) {
            List<InterfaceExample> _list = new ArrayList<>(interface_list.size());
            for (InterfaceExample _v : interface_list) {
                _list.add(Modeller.unredact(_v, e));
            }
            _builder.interface_list(_list);
        }
        if (interface_map != null && !interface_map.isEmpty()) {
            Map<String,InterfaceExample> _map = new HashMap<>(interface_map.size());
            for (Map.Entry<String,InterfaceExample> _e : interface_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.unredact(_e.getValue(), e));
            }
            _builder.interface_map(_map);
        }
        if (variety != null) _builder.variety(Modeller.unredact(variety, e));
        if (variety_list != null && !variety_list.isEmpty()) {
            List<VarietyExample> _list = new ArrayList<>(variety_list.size());
            for (VarietyExample _v : variety_list) {
                _list.add(Modeller.unredact(_v, e));
            }
            _builder.variety_list(_list);
        }
        if (variety_map != null && !variety_map.isEmpty()) {
            Map<String,VarietyExample> _map = new HashMap<>(variety_map.size());
            for (Map.Entry<String,VarietyExample> _e : variety_map.entrySet()) {
                _map.put(_e.getKey(), Modeller.unredact(_e.getValue(), e));
            }
            _builder.variety_map(_map);
        }
        return _builder.build();
    }

    @Override
    public void compress(ByteWriter out) {
        out.writeInt(32);
        if (out.writeBit(declared.id)) {
            out.writeBit(id != null);
        }
        if (out.writeBit(declared.id_derived)) {
            out.writeBit(id_derived != null);
        }
        if (out.writeBit(declared.hash_target)) {
            out.writeBit(hash_target != null);
        }
        if (out.writeBit(declared.val)) {
            out.writeBit(val != null);
        }
        boolean _nullable_elements_val_list = false;
        if (out.writeBit(declared.val_list)) {
            if (out.writeBit(val_list != null)) {
                if (out.writeBit(!val_list.isEmpty())) {
                    out.writeBit((_nullable_elements_val_list = val_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_val_map = false;
        if (out.writeBit(declared.val_map)) {
            if (out.writeBit(val_map != null)) {
                if (out.writeBit(!val_map.isEmpty())) {
                    out.writeBit((_nullable_elements_val_map = val_map.containsValue(null)));
                }
            }
        }
        if (out.writeBit(declared.bool)) {
            if (out.writeBit(bool != null)) {
                out.writeBit(Modeller.asBoolean(bool));
            }
        }
        if (out.writeBit(declared.id_thing)) {
            out.writeBit(id_thing != null);
        }
        boolean _nullable_elements_id_list = false;
        if (out.writeBit(declared.id_list)) {
            if (out.writeBit(id_list != null)) {
                if (out.writeBit(!id_list.isEmpty())) {
                    out.writeBit((_nullable_elements_id_list = id_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_id_map = false;
        if (out.writeBit(declared.id_map)) {
            if (out.writeBit(id_map != null)) {
                if (out.writeBit(!id_map.isEmpty())) {
                    out.writeBit((_nullable_elements_id_map = id_map.containsValue(null)));
                }
            }
        }
        if (out.writeBit(declared.non_id_thing)) {
            out.writeBit(non_id_thing != null);
        }
        boolean _nullable_elements_non_id_list = false;
        if (out.writeBit(declared.non_id_list)) {
            if (out.writeBit(non_id_list != null)) {
                if (out.writeBit(!non_id_list.isEmpty())) {
                    out.writeBit((_nullable_elements_non_id_list = non_id_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_non_id_map = false;
        if (out.writeBit(declared.non_id_map)) {
            if (out.writeBit(non_id_map != null)) {
                if (out.writeBit(!non_id_map.isEmpty())) {
                    out.writeBit((_nullable_elements_non_id_map = non_id_map.containsValue(null)));
                }
            }
        }
        boolean _nullable_elements_remap_target = false;
        if (out.writeBit(declared.remap_target)) {
            if (out.writeBit(remap_target != null)) {
                if (out.writeBit(!remap_target.isEmpty())) {
                    out.writeBit((_nullable_elements_remap_target = remap_target.contains(null)));
                }
            }
        }
        boolean _nullable_elements_derived_remap = false;
        if (out.writeBit(declared.derived_remap)) {
            if (out.writeBit(derived_remap != null)) {
                if (out.writeBit(!derived_remap.isEmpty())) {
                    out.writeBit((_nullable_elements_derived_remap = derived_remap.contains(null)));
                }
            }
        }
        if (out.writeBit(declared.reactive_to_type)) {
            out.writeBit(reactive_to_type != null);
        }
        if (out.writeBit(declared.reactive_to_type_field)) {
            out.writeBit(reactive_to_type_field != null);
        }
        if (out.writeBit(declared.reactive_to_sibling_field)) {
            out.writeBit(reactive_to_sibling_field != null);
        }
        if (out.writeBit(declared.reactive_to_any_sibling_field)) {
            out.writeBit(reactive_to_any_sibling_field != null);
        }
        if (out.writeBit(declared.reactive_collection_field)) {
            out.writeBit(reactive_collection_field != null);
        }
        if (out.writeBit(declared.dangerous)) {
            out.writeBit(dangerous != null);
        }
        boolean _nullable_elements_dangerous_list = false;
        if (out.writeBit(declared.dangerous_list)) {
            if (out.writeBit(dangerous_list != null)) {
                if (out.writeBit(!dangerous_list.isEmpty())) {
                    out.writeBit((_nullable_elements_dangerous_list = dangerous_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_dangerous_map = false;
        if (out.writeBit(declared.dangerous_map)) {
            if (out.writeBit(dangerous_map != null)) {
                if (out.writeBit(!dangerous_map.isEmpty())) {
                    out.writeBit((_nullable_elements_dangerous_map = dangerous_map.containsValue(null)));
                }
            }
        }
        if (out.writeBit(declared.has_dangerous)) {
            out.writeBit(has_dangerous != null);
        }
        boolean _nullable_elements_has_dangerous_list = false;
        if (out.writeBit(declared.has_dangerous_list)) {
            if (out.writeBit(has_dangerous_list != null)) {
                if (out.writeBit(!has_dangerous_list.isEmpty())) {
                    out.writeBit((_nullable_elements_has_dangerous_list = has_dangerous_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_has_dangerous_map = false;
        if (out.writeBit(declared.has_dangerous_map)) {
            if (out.writeBit(has_dangerous_map != null)) {
                if (out.writeBit(!has_dangerous_map.isEmpty())) {
                    out.writeBit((_nullable_elements_has_dangerous_map = has_dangerous_map.containsValue(null)));
                }
            }
        }
        if (out.writeBit(declared.variety)) {
            out.writeBit(variety != null);
        }
        boolean _nullable_elements_variety_list = false;
        if (out.writeBit(declared.variety_list)) {
            if (out.writeBit(variety_list != null)) {
                if (out.writeBit(!variety_list.isEmpty())) {
                    out.writeBit((_nullable_elements_variety_list = variety_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_variety_map = false;
        if (out.writeBit(declared.variety_map)) {
            if (out.writeBit(variety_map != null)) {
                if (out.writeBit(!variety_map.isEmpty())) {
                    out.writeBit((_nullable_elements_variety_map = variety_map.containsValue(null)));
                }
            }
        }
        if (out.writeBit(declared.interface_)) {
            out.writeBit(interface_ != null);
        }
        boolean _nullable_elements_interface_list = false;
        if (out.writeBit(declared.interface_list)) {
            if (out.writeBit(interface_list != null)) {
                if (out.writeBit(!interface_list.isEmpty())) {
                    out.writeBit((_nullable_elements_interface_list = interface_list.contains(null)));
                }
            }
        }
        boolean _nullable_elements_interface_map = false;
        if (out.writeBit(declared.interface_map)) {
            if (out.writeBit(interface_map != null)) {
                if (out.writeBit(!interface_map.isEmpty())) {
                    out.writeBit((_nullable_elements_interface_map = interface_map.containsValue(null)));
                }
            }
        }
        out.finishByte();
        if (id != null) {
            out.writeString(id);
        }
        if (id_derived != null) {
            out.writeString(id_derived);
        }
        if (hash_target != null) {
            out.writeString(hash_target);
        }
        if (val != null) {
            out.writeString(val);
        }
        if (val_list != null && !val_list.isEmpty()) {
            out.writeInt(val_list.size());
            for (String _e : val_list) {
                if (_nullable_elements_val_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        out.writeString(_e);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_e);
                }
            }
        }
        if (val_map != null && !val_map.isEmpty()) {
            out.writeInt(val_map.size());
            for (Map.Entry<String,String> _e : val_map.entrySet()) {
                String _k = _e.getKey();
                String _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_val_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        out.writeString(_v);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_v);
                }
            }
        }
        if (id_thing != null) {
            id_thing.compress(out);
        }
        if (id_list != null && !id_list.isEmpty()) {
            out.writeInt(id_list.size());
            for (IdentifiableByValue _e : id_list) {
                if (_nullable_elements_id_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _e.compress(out);
                }
            }
        }
        if (id_map != null && !id_map.isEmpty()) {
            out.writeInt(id_map.size());
            for (Map.Entry<String,IdentifiableByValue> _e : id_map.entrySet()) {
                String _k = _e.getKey();
                IdentifiableByValue _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_id_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        _v.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _v.compress(out);
                }
            }
        }
        if (non_id_thing != null) {
            non_id_thing.compress(out);
        }
        if (non_id_list != null && !non_id_list.isEmpty()) {
            out.writeInt(non_id_list.size());
            for (NonIdentifiable _e : non_id_list) {
                if (_nullable_elements_non_id_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _e.compress(out);
                }
            }
        }
        if (non_id_map != null && !non_id_map.isEmpty()) {
            out.writeInt(non_id_map.size());
            for (Map.Entry<String,NonIdentifiable> _e : non_id_map.entrySet()) {
                String _k = _e.getKey();
                NonIdentifiable _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_non_id_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        _v.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _v.compress(out);
                }
            }
        }
        if (remap_target != null && !remap_target.isEmpty()) {
            out.writeInt(remap_target.size());
            for (IdentifiableByValue _e : remap_target) {
                if (_nullable_elements_remap_target) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _e.compress(out);
                }
            }
        }
        if (derived_remap != null && !derived_remap.isEmpty()) {
            out.writeInt(derived_remap.size());
            for (String _e : derived_remap) {
                if (_nullable_elements_derived_remap) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        out.writeString(_e);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_e);
                }
            }
        }
        if (reactive_to_type != null) {
            out.writeString(reactive_to_type);
        }
        if (reactive_to_type_field != null) {
            out.writeString(reactive_to_type_field);
        }
        if (reactive_to_sibling_field != null) {
            out.writeString(reactive_to_sibling_field);
        }
        if (reactive_to_any_sibling_field != null) {
            out.writeString(reactive_to_any_sibling_field);
        }
        if (reactive_collection_field != null) {
            out.writeString(reactive_collection_field);
        }
        if (dangerous != null) {
            out.writeString(dangerous);
        }
        if (dangerous_list != null && !dangerous_list.isEmpty()) {
            out.writeInt(dangerous_list.size());
            for (String _e : dangerous_list) {
                if (_nullable_elements_dangerous_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        out.writeString(_e);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_e);
                }
            }
        }
        if (dangerous_map != null && !dangerous_map.isEmpty()) {
            out.writeInt(dangerous_map.size());
            for (Map.Entry<String,String> _e : dangerous_map.entrySet()) {
                String _k = _e.getKey();
                String _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_dangerous_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        out.writeString(_v);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_v);
                }
            }
        }
        if (has_dangerous != null) {
            has_dangerous.compress(out);
        }
        if (has_dangerous_list != null && !has_dangerous_list.isEmpty()) {
            out.writeInt(has_dangerous_list.size());
            for (HasDangerousValue _e : has_dangerous_list) {
                if (_nullable_elements_has_dangerous_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _e.compress(out);
                }
            }
        }
        if (has_dangerous_map != null && !has_dangerous_map.isEmpty()) {
            out.writeInt(has_dangerous_map.size());
            for (Map.Entry<String,HasDangerousValue> _e : has_dangerous_map.entrySet()) {
                String _k = _e.getKey();
                HasDangerousValue _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_has_dangerous_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        _v.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    _v.compress(out);
                }
            }
        }
        if (variety != null) {
            out.writeString(variety.type());
            variety.compress(out);
        }
        if (variety_list != null && !variety_list.isEmpty()) {
            out.writeInt(variety_list.size());
            for (VarietyExample _e : variety_list) {
                if (_nullable_elements_variety_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        out.writeString(_e.type());
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_e.type());
                    _e.compress(out);
                }
            }
        }
        if (variety_map != null && !variety_map.isEmpty()) {
            out.writeInt(variety_map.size());
            for (Map.Entry<String,VarietyExample> _e : variety_map.entrySet()) {
                String _k = _e.getKey();
                VarietyExample _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_variety_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        out.writeString(_v.type());
                        _v.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_v.type());
                    _v.compress(out);
                }
            }
        }
        if (interface_ != null) {
            out.writeString(interface_.type());
            interface_.compress(out);
        }
        if (interface_list != null && !interface_list.isEmpty()) {
            out.writeInt(interface_list.size());
            for (InterfaceExample _e : interface_list) {
                if (_nullable_elements_interface_list) {
                    if (_e != null) {
                        out.writeBoolean(true);
                        out.writeString(_e.type());
                        _e.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_e.type());
                    _e.compress(out);
                }
            }
        }
        if (interface_map != null && !interface_map.isEmpty()) {
            out.writeInt(interface_map.size());
            for (Map.Entry<String,InterfaceExample> _e : interface_map.entrySet()) {
                String _k = _e.getKey();
                InterfaceExample _v = _e.getValue();
                out.writeString(_k);
                if (_nullable_elements_interface_map) {
                    if (_v != null) {
                        out.writeBoolean(true);
                        out.writeString(_v.type());
                        _v.compress(out);
                    } else {
                        out.writeBoolean(false);
                    }
                } else {
                    out.writeString(_v.type());
                    _v.compress(out);
                }
            }
        }
    }

    public static ThingExample uncompress(ByteReader _in) {
        Builder _builder = new Builder();
        int _fields = _in.readInt();
        boolean _read_id = false;
        boolean _read_id_derived = false;
        boolean _read_hash_target = false;
        boolean _read_val = false;
        int _read_val_list = 0;
        int _read_val_map = 0;
        boolean _read_id_thing = false;
        int _read_id_list = 0;
        int _read_id_map = 0;
        boolean _read_non_id_thing = false;
        int _read_non_id_list = 0;
        int _read_non_id_map = 0;
        int _read_remap_target = 0;
        int _read_derived_remap = 0;
        boolean _read_reactive_to_type = false;
        boolean _read_reactive_to_type_field = false;
        boolean _read_reactive_to_sibling_field = false;
        boolean _read_reactive_to_any_sibling_field = false;
        boolean _read_reactive_collection_field = false;
        boolean _read_dangerous = false;
        int _read_dangerous_list = 0;
        int _read_dangerous_map = 0;
        boolean _read_has_dangerous = false;
        int _read_has_dangerous_list = 0;
        int _read_has_dangerous_map = 0;
        boolean _read_variety = false;
        int _read_variety_list = 0;
        int _read_variety_map = 0;
        boolean _read_interface_ = false;
        int _read_interface_list = 0;
        int _read_interface_map = 0;
        _fields_break :  {
            if (0 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_id = _in.readBit())) _builder.id(null);
            if (1 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_id_derived = _in.readBit())) _builder.id_derived(null);
            if (2 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_hash_target = _in.readBit())) _builder.hash_target(null);
            if (3 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_val = _in.readBit())) _builder.val(null);
            if (4 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_val_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.val_list(Collections.emptyList());
                    }
                } else {
                    _builder.val_list(null);
                }
            }
            if (5 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_val_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.val_map(Collections.emptyMap());
                    }
                } else {
                    _builder.val_map(null);
                }
            }
            if (6 >= _fields) break _fields_break;
            if (_in.readBit()) _builder.bool(_in.readBit() ? _in.readBit() : null);
            if (7 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_id_thing = _in.readBit())) _builder.id_thing(null);
            if (8 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_id_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.id_list(Collections.emptyList());
                    }
                } else {
                    _builder.id_list(null);
                }
            }
            if (9 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_id_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.id_map(Collections.emptyMap());
                    }
                } else {
                    _builder.id_map(null);
                }
            }
            if (10 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_non_id_thing = _in.readBit())) _builder.non_id_thing(null);
            if (11 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_non_id_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.non_id_list(Collections.emptyList());
                    }
                } else {
                    _builder.non_id_list(null);
                }
            }
            if (12 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_non_id_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.non_id_map(Collections.emptyMap());
                    }
                } else {
                    _builder.non_id_map(null);
                }
            }
            if (13 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_remap_target = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.remap_target(Collections.emptyList());
                    }
                } else {
                    _builder.remap_target(null);
                }
            }
            if (14 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_derived_remap = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.derived_remap(Collections.emptyList());
                    }
                } else {
                    _builder.derived_remap(null);
                }
            }
            if (15 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_reactive_to_type = _in.readBit())) _builder.reactive_to_type(null);
            if (16 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_reactive_to_type_field = _in.readBit())) _builder.reactive_to_type_field(null);
            if (17 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_reactive_to_sibling_field = _in.readBit())) _builder.reactive_to_sibling_field(null);
            if (18 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_reactive_to_any_sibling_field = _in.readBit())) _builder.reactive_to_any_sibling_field(null);
            if (19 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_reactive_collection_field = _in.readBit())) _builder.reactive_collection_field(null);
            if (20 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_dangerous = _in.readBit())) _builder.dangerous(null);
            if (21 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_dangerous_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.dangerous_list(Collections.emptyList());
                    }
                } else {
                    _builder.dangerous_list(null);
                }
            }
            if (22 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_dangerous_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.dangerous_map(Collections.emptyMap());
                    }
                } else {
                    _builder.dangerous_map(null);
                }
            }
            if (23 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_has_dangerous = _in.readBit())) _builder.has_dangerous(null);
            if (24 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_has_dangerous_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.has_dangerous_list(Collections.emptyList());
                    }
                } else {
                    _builder.has_dangerous_list(null);
                }
            }
            if (25 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_has_dangerous_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.has_dangerous_map(Collections.emptyMap());
                    }
                } else {
                    _builder.has_dangerous_map(null);
                }
            }
            if (26 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_variety = _in.readBit())) _builder.variety(null);
            if (27 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_variety_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.variety_list(Collections.emptyList());
                    }
                } else {
                    _builder.variety_list(null);
                }
            }
            if (28 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_variety_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.variety_map(Collections.emptyMap());
                    }
                } else {
                    _builder.variety_map(null);
                }
            }
            if (29 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_interface_ = _in.readBit())) _builder.interface_(null);
            if (30 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_interface_list = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.interface_list(Collections.emptyList());
                    }
                } else {
                    _builder.interface_list(null);
                }
            }
            if (31 >= _fields) break _fields_break;
            if (_in.readBit()) {
                if (_in.readBit()) {
                    if (_in.readBit()) {
                        _read_interface_map = _in.readBit() ? 2 : 1;
                    } else {
                        _builder.interface_map(Collections.emptyMap());
                    }
                } else {
                    _builder.interface_map(null);
                }
            }
        }
        _in.finishByte();
        if (_read_id) _builder.id(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_id_derived) _builder.id_derived(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_hash_target) _builder.hash_target(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_val) _builder.val(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_val_list > 0) _builder.val_list(_in.readList(Modeller.STRING_BYTE_CREATOR, _read_val_list == 2));
        if (_read_val_map > 0) _builder.val_map(_in.readMap(Modeller.STRING_BYTE_CREATOR, _read_val_map == 2));
        if (_read_id_thing) _builder.id_thing(IdentifiableByValue.uncompress(_in));
        if (_read_id_list > 0) _builder.id_list(_in.readList(IdentifiableByValue.BYTE_CREATOR, _read_id_list == 2));
        if (_read_id_map > 0) _builder.id_map(_in.readMap(IdentifiableByValue.BYTE_CREATOR, _read_id_map == 2));
        if (_read_non_id_thing) _builder.non_id_thing(NonIdentifiable.uncompress(_in));
        if (_read_non_id_list > 0) _builder.non_id_list(_in.readList(NonIdentifiable.BYTE_CREATOR, _read_non_id_list == 2));
        if (_read_non_id_map > 0) _builder.non_id_map(_in.readMap(NonIdentifiable.BYTE_CREATOR, _read_non_id_map == 2));
        if (_read_remap_target > 0) _builder.remap_target(_in.readList(IdentifiableByValue.BYTE_CREATOR, _read_remap_target == 2));
        if (_read_derived_remap > 0) _builder.derived_remap(_in.readList(Modeller.STRING_BYTE_CREATOR, _read_derived_remap == 2));
        if (_read_reactive_to_type) _builder.reactive_to_type(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_reactive_to_type_field) _builder.reactive_to_type_field(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_reactive_to_sibling_field) _builder.reactive_to_sibling_field(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_reactive_to_any_sibling_field) _builder.reactive_to_any_sibling_field(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_reactive_collection_field) _builder.reactive_collection_field(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_dangerous) _builder.dangerous(Modeller.DANGEROUS_BYTE_CREATOR.create(_in));
        if (_read_dangerous_list > 0) _builder.dangerous_list(_in.readList(Modeller.DANGEROUS_BYTE_CREATOR, _read_dangerous_list == 2));
        if (_read_dangerous_map > 0) _builder.dangerous_map(_in.readMap(Modeller.DANGEROUS_BYTE_CREATOR, _read_dangerous_map == 2));
        if (_read_has_dangerous) _builder.has_dangerous(HasDangerousValue.uncompress(_in));
        if (_read_has_dangerous_list > 0) _builder.has_dangerous_list(_in.readList(HasDangerousValue.BYTE_CREATOR, _read_has_dangerous_list == 2));
        if (_read_has_dangerous_map > 0) _builder.has_dangerous_map(_in.readMap(HasDangerousValue.BYTE_CREATOR, _read_has_dangerous_map == 2));
        if (_read_variety) _builder.variety(VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR.create(_in));
        if (_read_variety_list > 0) _builder.variety_list(_in.readList(VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _read_variety_list == 2));
        if (_read_variety_map > 0) _builder.variety_map(_in.readMap(VarietyExample.VARIETY_VARIETYEXAMPLE_CREATOR, _read_variety_map == 2));
        if (_read_interface_) _builder.interface_(InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR.create(_in));
        if (_read_interface_list > 0) _builder.interface_list(_in.readList(InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _read_interface_list == 2));
        if (_read_interface_map > 0) _builder.interface_map(_in.readMap(InterfaceExample.INTERFACE_INTERFACEEXAMPLE_CREATOR, _read_interface_map == 2));
        return _builder.build();
    }

    private static class GraphQl implements GraphQlSupport {
        @Nullable
        @Override
        public String operation() {
            return null;
        }
    }

    private static class DeclaredMutable {
        private boolean id;

        private boolean id_derived;

        private boolean hash_target;

        private boolean val;

        private boolean val_list;

        private boolean val_map;

        private boolean bool;

        private boolean id_thing;

        private boolean id_list;

        private boolean id_map;

        private boolean non_id_thing;

        private boolean non_id_list;

        private boolean non_id_map;

        private boolean derived_first_available;

        private boolean remap_target;

        private boolean derived_remap;

        private boolean reactive_to_type;

        private boolean reactive_to_type_field;

        private boolean reactive_to_sibling_field;

        private boolean reactive_to_any_sibling_field;

        private boolean reactive_collection_field;

        private boolean dangerous;

        private boolean dangerous_list;

        private boolean dangerous_map;

        private boolean has_dangerous;

        private boolean has_dangerous_list;

        private boolean has_dangerous_map;

        private boolean variety;

        private boolean variety_list;

        private boolean variety_map;

        private boolean interface_;

        private boolean interface_list;

        private boolean interface_map;
    }

    public static class Declared {
        public final boolean id;

        public final boolean id_derived;

        public final boolean hash_target;

        public final boolean val;

        public final boolean val_list;

        public final boolean val_map;

        public final boolean bool;

        public final boolean id_thing;

        public final boolean id_list;

        public final boolean id_map;

        public final boolean non_id_thing;

        public final boolean non_id_list;

        public final boolean non_id_map;

        public final boolean derived_first_available;

        public final boolean remap_target;

        public final boolean derived_remap;

        public final boolean reactive_to_type;

        public final boolean reactive_to_type_field;

        public final boolean reactive_to_sibling_field;

        public final boolean reactive_to_any_sibling_field;

        public final boolean reactive_collection_field;

        public final boolean dangerous;

        public final boolean dangerous_list;

        public final boolean dangerous_map;

        public final boolean has_dangerous;

        public final boolean has_dangerous_list;

        public final boolean has_dangerous_map;

        public final boolean variety;

        public final boolean variety_list;

        public final boolean variety_map;

        public final boolean interface_;

        public final boolean interface_list;

        public final boolean interface_map;

        private Declared(DeclaredMutable declared) {
            this.id = declared.id;
            this.id_derived = declared.id_derived;
            this.hash_target = declared.hash_target;
            this.val = declared.val;
            this.val_list = declared.val_list;
            this.val_map = declared.val_map;
            this.bool = declared.bool;
            this.id_thing = declared.id_thing;
            this.id_list = declared.id_list;
            this.id_map = declared.id_map;
            this.non_id_thing = declared.non_id_thing;
            this.non_id_list = declared.non_id_list;
            this.non_id_map = declared.non_id_map;
            this.derived_first_available = declared.derived_first_available;
            this.remap_target = declared.remap_target;
            this.derived_remap = declared.derived_remap;
            this.reactive_to_type = declared.reactive_to_type;
            this.reactive_to_type_field = declared.reactive_to_type_field;
            this.reactive_to_sibling_field = declared.reactive_to_sibling_field;
            this.reactive_to_any_sibling_field = declared.reactive_to_any_sibling_field;
            this.reactive_collection_field = declared.reactive_collection_field;
            this.dangerous = declared.dangerous;
            this.dangerous_list = declared.dangerous_list;
            this.dangerous_map = declared.dangerous_map;
            this.has_dangerous = declared.has_dangerous;
            this.has_dangerous_list = declared.has_dangerous_list;
            this.has_dangerous_map = declared.has_dangerous_map;
            this.variety = declared.variety;
            this.variety_list = declared.variety_list;
            this.variety_map = declared.variety_map;
            this.interface_ = declared.interface_;
            this.interface_list = declared.interface_list;
            this.interface_map = declared.interface_map;
        }
    }

    public static class Builder implements ThingBuilder<ThingExample> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected String id;

        protected String id_derived;

        protected String hash_target;

        protected String val;

        protected List<String> val_list;

        protected Map<String, String> val_map;

        protected Boolean bool;

        protected IdentifiableByValue id_thing;

        protected List<IdentifiableByValue> id_list;

        protected Map<String, IdentifiableByValue> id_map;

        protected NonIdentifiable non_id_thing;

        protected List<NonIdentifiable> non_id_list;

        protected Map<String, NonIdentifiable> non_id_map;

        protected String derived_first_available;

        protected List<IdentifiableByValue> remap_target;

        protected List<String> derived_remap;

        protected String reactive_to_type;

        protected String reactive_to_type_field;

        protected String reactive_to_sibling_field;

        protected String reactive_to_any_sibling_field;

        protected String reactive_collection_field;

        protected String dangerous;

        protected List<String> dangerous_list;

        protected Map<String, String> dangerous_map;

        protected HasDangerousValue has_dangerous;

        protected List<HasDangerousValue> has_dangerous_list;

        protected Map<String, HasDangerousValue> has_dangerous_map;

        protected VarietyExample variety;

        protected List<VarietyExample> variety_list;

        protected Map<String, VarietyExample> variety_map;

        protected InterfaceExample interface_;

        protected List<InterfaceExample> interface_list;

        protected Map<String, InterfaceExample> interface_map;

        public Builder() {
        }

        public Builder(ThingExample src) {
            set(src);
        }

        @Override
        public Builder set(ThingExample src) {
            if (src.declared.id) {
                declared.id = true;
                this.id = src.id;
            }
            if (src.declared.id_derived) {
                declared.id_derived = true;
                this.id_derived = src.id_derived;
            }
            if (src.declared.hash_target) {
                declared.hash_target = true;
                this.hash_target = src.hash_target;
            }
            if (src.declared.val) {
                declared.val = true;
                this.val = src.val;
            }
            if (src.declared.val_list) {
                declared.val_list = true;
                this.val_list = src.val_list;
            }
            if (src.declared.val_map) {
                declared.val_map = true;
                this.val_map = src.val_map;
            }
            if (src.declared.bool) {
                declared.bool = true;
                this.bool = src.bool;
            }
            if (src.declared.id_thing) {
                declared.id_thing = true;
                this.id_thing = src.id_thing;
            }
            if (src.declared.id_list) {
                declared.id_list = true;
                this.id_list = src.id_list;
            }
            if (src.declared.id_map) {
                declared.id_map = true;
                this.id_map = src.id_map;
            }
            if (src.declared.non_id_thing) {
                declared.non_id_thing = true;
                this.non_id_thing = src.non_id_thing;
            }
            if (src.declared.non_id_list) {
                declared.non_id_list = true;
                this.non_id_list = src.non_id_list;
            }
            if (src.declared.non_id_map) {
                declared.non_id_map = true;
                this.non_id_map = src.non_id_map;
            }
            if (src.declared.derived_first_available) {
                declared.derived_first_available = true;
                this.derived_first_available = src.derived_first_available;
            }
            if (src.declared.remap_target) {
                declared.remap_target = true;
                this.remap_target = src.remap_target;
            }
            if (src.declared.derived_remap) {
                declared.derived_remap = true;
                this.derived_remap = src.derived_remap;
            }
            if (src.declared.reactive_to_type) {
                declared.reactive_to_type = true;
                this.reactive_to_type = src.reactive_to_type;
            }
            if (src.declared.reactive_to_type_field) {
                declared.reactive_to_type_field = true;
                this.reactive_to_type_field = src.reactive_to_type_field;
            }
            if (src.declared.reactive_to_sibling_field) {
                declared.reactive_to_sibling_field = true;
                this.reactive_to_sibling_field = src.reactive_to_sibling_field;
            }
            if (src.declared.reactive_to_any_sibling_field) {
                declared.reactive_to_any_sibling_field = true;
                this.reactive_to_any_sibling_field = src.reactive_to_any_sibling_field;
            }
            if (src.declared.reactive_collection_field) {
                declared.reactive_collection_field = true;
                this.reactive_collection_field = src.reactive_collection_field;
            }
            if (src.declared.dangerous) {
                declared.dangerous = true;
                this.dangerous = src.dangerous;
            }
            if (src.declared.dangerous_list) {
                declared.dangerous_list = true;
                this.dangerous_list = src.dangerous_list;
            }
            if (src.declared.dangerous_map) {
                declared.dangerous_map = true;
                this.dangerous_map = src.dangerous_map;
            }
            if (src.declared.has_dangerous) {
                declared.has_dangerous = true;
                this.has_dangerous = src.has_dangerous;
            }
            if (src.declared.has_dangerous_list) {
                declared.has_dangerous_list = true;
                this.has_dangerous_list = src.has_dangerous_list;
            }
            if (src.declared.has_dangerous_map) {
                declared.has_dangerous_map = true;
                this.has_dangerous_map = src.has_dangerous_map;
            }
            if (src.declared.variety) {
                declared.variety = true;
                this.variety = src.variety;
            }
            if (src.declared.variety_list) {
                declared.variety_list = true;
                this.variety_list = src.variety_list;
            }
            if (src.declared.variety_map) {
                declared.variety_map = true;
                this.variety_map = src.variety_map;
            }
            if (src.declared.interface_) {
                declared.interface_ = true;
                this.interface_ = src.interface_;
            }
            if (src.declared.interface_list) {
                declared.interface_list = true;
                this.interface_list = src.interface_list;
            }
            if (src.declared.interface_map) {
                declared.interface_map = true;
                this.interface_map = src.interface_map;
            }
            return this;
        }

        public Builder id(String value) {
            declared.id = true;
            this.id = Modeller.immutable(value);
            return this;
        }

        /**
         * (This field is self derived and will be automatically calculated during {@link #build()})
         */
        private Builder id_derived(String value) {
            declared.id_derived = true;
            this.id_derived = Modeller.immutable(value);
            return this;
        }

        public Builder hash_target(String value) {
            declared.hash_target = true;
            this.hash_target = Modeller.immutable(value);
            return this;
        }

        public Builder val(String value) {
            declared.val = true;
            this.val = Modeller.immutable(value);
            return this;
        }

        public Builder val_list(List<String> value) {
            declared.val_list = true;
            this.val_list = Modeller.immutable(value);
            return this;
        }

        public Builder val_map(Map<String, String> value) {
            declared.val_map = true;
            this.val_map = Modeller.immutable(value);
            return this;
        }

        public Builder bool(Boolean value) {
            declared.bool = true;
            this.bool = Modeller.immutable(value);
            return this;
        }

        public Builder id_thing(IdentifiableByValue value) {
            declared.id_thing = true;
            this.id_thing = Modeller.immutable(value);
            return this;
        }

        public Builder id_list(List<IdentifiableByValue> value) {
            declared.id_list = true;
            this.id_list = Modeller.immutable(value);
            return this;
        }

        public Builder id_map(Map<String, IdentifiableByValue> value) {
            declared.id_map = true;
            this.id_map = Modeller.immutable(value);
            return this;
        }

        public Builder non_id_thing(NonIdentifiable value) {
            declared.non_id_thing = true;
            this.non_id_thing = Modeller.immutable(value);
            return this;
        }

        public Builder non_id_list(List<NonIdentifiable> value) {
            declared.non_id_list = true;
            this.non_id_list = Modeller.immutable(value);
            return this;
        }

        public Builder non_id_map(Map<String, NonIdentifiable> value) {
            declared.non_id_map = true;
            this.non_id_map = Modeller.immutable(value);
            return this;
        }

        /**
         * (This field is self derived and will be automatically calculated during {@link #build()})
         */
        private Builder derived_first_available(String value) {
            declared.derived_first_available = true;
            this.derived_first_available = Modeller.immutable(value);
            return this;
        }

        public Builder remap_target(List<IdentifiableByValue> value) {
            declared.remap_target = true;
            this.remap_target = Modeller.immutable(value);
            return this;
        }

        public Builder derived_remap(List<String> value) {
            declared.derived_remap = true;
            this.derived_remap = Modeller.immutable(value);
            return this;
        }

        public Builder reactive_to_type(String value) {
            declared.reactive_to_type = true;
            this.reactive_to_type = Modeller.immutable(value);
            return this;
        }

        public Builder reactive_to_type_field(String value) {
            declared.reactive_to_type_field = true;
            this.reactive_to_type_field = Modeller.immutable(value);
            return this;
        }

        public Builder reactive_to_sibling_field(String value) {
            declared.reactive_to_sibling_field = true;
            this.reactive_to_sibling_field = Modeller.immutable(value);
            return this;
        }

        public Builder reactive_to_any_sibling_field(String value) {
            declared.reactive_to_any_sibling_field = true;
            this.reactive_to_any_sibling_field = Modeller.immutable(value);
            return this;
        }

        public Builder reactive_collection_field(String value) {
            declared.reactive_collection_field = true;
            this.reactive_collection_field = Modeller.immutable(value);
            return this;
        }

        public Builder dangerous(String value) {
            declared.dangerous = true;
            this.dangerous = Modeller.immutable(value);
            return this;
        }

        public Builder dangerous_list(List<String> value) {
            declared.dangerous_list = true;
            this.dangerous_list = Modeller.immutable(value);
            return this;
        }

        public Builder dangerous_map(Map<String, String> value) {
            declared.dangerous_map = true;
            this.dangerous_map = Modeller.immutable(value);
            return this;
        }

        public Builder has_dangerous(HasDangerousValue value) {
            declared.has_dangerous = true;
            this.has_dangerous = Modeller.immutable(value);
            return this;
        }

        public Builder has_dangerous_list(List<HasDangerousValue> value) {
            declared.has_dangerous_list = true;
            this.has_dangerous_list = Modeller.immutable(value);
            return this;
        }

        public Builder has_dangerous_map(Map<String, HasDangerousValue> value) {
            declared.has_dangerous_map = true;
            this.has_dangerous_map = Modeller.immutable(value);
            return this;
        }

        public Builder variety(VarietyExample value) {
            declared.variety = true;
            this.variety = Modeller.immutable(value);
            return this;
        }

        public Builder variety_list(List<VarietyExample> value) {
            declared.variety_list = true;
            this.variety_list = Modeller.immutable(value);
            return this;
        }

        public Builder variety_map(Map<String, VarietyExample> value) {
            declared.variety_map = true;
            this.variety_map = Modeller.immutable(value);
            return this;
        }

        public Builder interface_(InterfaceExample value) {
            declared.interface_ = true;
            this.interface_ = Modeller.immutable(value);
            return this;
        }

        public Builder interface_list(List<InterfaceExample> value) {
            declared.interface_list = true;
            this.interface_list = Modeller.immutable(value);
            return this;
        }

        public Builder interface_map(Map<String, InterfaceExample> value) {
            declared.interface_map = true;
            this.interface_map = Modeller.immutable(value);
            return this;
        }

        @Override
        public ThingExample build() {
            Derive.derived_first_available(this);
            Derive.id_derived(this);
            return new ThingExample(this, new Declared(declared));
        }
    }

    public static class IdBuilder implements ThingBuilder<ThingExample> {
        private final Builder builder = new Builder();

        public IdBuilder() {
        }

        public IdBuilder(ThingExample src) {
            set(src);
        }

        @Override
        public IdBuilder set(ThingExample src) {
            if (src.declared.id) {
                builder.declared.id = true;
                builder.id = src.id;
            }
            if (src.declared.id_derived) {
                builder.declared.id_derived = true;
                builder.id_derived = src.id_derived;
            }
            if (src.declared.hash_target) {
                builder.declared.hash_target = true;
                builder.hash_target = src.hash_target;
            }
            return this;
        }

        public IdBuilder id(String value) {
            builder.id(value);
            return this;
        }

        public IdBuilder id_derived(String value) {
            builder.id_derived(value);
            return this;
        }

        public IdBuilder hash_target(String value) {
            builder.hash_target(value);
            return this;
        }

        @Override
        public ThingExample build() {
            Derive.id_derived(builder);
            return new ThingExample(builder, new Declared(builder.declared));
        }
    }

    public static class Mutable implements MutableThing<ThingExample> {
        private final Builder builder = new Builder();

        private final ThingExample _identity;

        private ThingExample _built;

        private ThingExample _previous;

        private MutableThing _root;

        private MutableThing<IdentifiableByValue> id_thing;

        private List<MutableThing<IdentifiableByValue>> id_list;

        private Map<String, MutableThing<IdentifiableByValue>> id_map;

        private List<MutableThing<IdentifiableByValue>> remap_target;

        private Mutable(ThingExample src, Mutables mutables) {
            _identity = src.identity();
            _root = this;
            if (src.declared.id)  {
                builder.declared.id = true;
                builder.id = src.id;
            }
            if (src.declared.id_derived)  {
                builder.declared.id_derived = true;
                builder.id_derived = src.id_derived;
            }
            if (src.declared.hash_target)  {
                builder.declared.hash_target = true;
                builder.hash_target = src.hash_target;
            }
            if (src.declared.val)  {
                builder.declared.val = true;
                builder.val = src.val;
            }
            if (src.declared.val_list)  {
                builder.declared.val_list = true;
                builder.val_list = src.val_list;
            }
            if (src.declared.val_map)  {
                builder.declared.val_map = true;
                builder.val_map = src.val_map;
            }
            if (src.declared.bool)  {
                builder.declared.bool = true;
                builder.bool = src.bool;
            }
            if (src.declared.id_thing)  {
                builder.declared.id_thing = true;
                id_thing = mutables.imprint(src.id_thing, _root);
                mutables.link(this, id_thing);
            }
            if (src.declared.id_list)  {
                builder.declared.id_list = true;
                id_list = mutables.imprint(src.id_list, _root);
                mutables.link(this, id_list);
            }
            if (src.declared.id_map)  {
                builder.declared.id_map = true;
                id_map = mutables.imprint(src.id_map, _root);
                mutables.link(this, id_map);
            }
            if (src.declared.non_id_thing)  {
                builder.declared.non_id_thing = true;
                builder.non_id_thing = src.non_id_thing;
            }
            if (src.declared.non_id_list)  {
                builder.declared.non_id_list = true;
                builder.non_id_list = src.non_id_list;
            }
            if (src.declared.non_id_map)  {
                builder.declared.non_id_map = true;
                builder.non_id_map = src.non_id_map;
            }
            if (src.declared.derived_first_available)  {
                builder.declared.derived_first_available = true;
                builder.derived_first_available = src.derived_first_available;
            }
            if (src.declared.remap_target)  {
                builder.declared.remap_target = true;
                remap_target = mutables.imprint(src.remap_target, _root);
                mutables.link(this, remap_target);
            }
            if (src.declared.derived_remap)  {
                builder.declared.derived_remap = true;
                builder.derived_remap = src.derived_remap;
            }
            if (src.declared.reactive_to_type)  {
                builder.declared.reactive_to_type = true;
                builder.reactive_to_type = src.reactive_to_type;
            }
            if (src.declared.reactive_to_type_field)  {
                builder.declared.reactive_to_type_field = true;
                builder.reactive_to_type_field = src.reactive_to_type_field;
            }
            if (src.declared.reactive_to_sibling_field)  {
                builder.declared.reactive_to_sibling_field = true;
                builder.reactive_to_sibling_field = src.reactive_to_sibling_field;
            }
            if (src.declared.reactive_to_any_sibling_field)  {
                builder.declared.reactive_to_any_sibling_field = true;
                builder.reactive_to_any_sibling_field = src.reactive_to_any_sibling_field;
            }
            if (src.declared.reactive_collection_field)  {
                builder.declared.reactive_collection_field = true;
                builder.reactive_collection_field = src.reactive_collection_field;
            }
            if (src.declared.dangerous)  {
                builder.declared.dangerous = true;
                builder.dangerous = src.dangerous;
            }
            if (src.declared.dangerous_list)  {
                builder.declared.dangerous_list = true;
                builder.dangerous_list = src.dangerous_list;
            }
            if (src.declared.dangerous_map)  {
                builder.declared.dangerous_map = true;
                builder.dangerous_map = src.dangerous_map;
            }
            if (src.declared.has_dangerous)  {
                builder.declared.has_dangerous = true;
                builder.has_dangerous = src.has_dangerous;
            }
            if (src.declared.has_dangerous_list)  {
                builder.declared.has_dangerous_list = true;
                builder.has_dangerous_list = src.has_dangerous_list;
            }
            if (src.declared.has_dangerous_map)  {
                builder.declared.has_dangerous_map = true;
                builder.has_dangerous_map = src.has_dangerous_map;
            }
            if (src.declared.variety)  {
                builder.declared.variety = true;
                builder.variety = src.variety;
            }
            if (src.declared.variety_list)  {
                builder.declared.variety_list = true;
                builder.variety_list = src.variety_list;
            }
            if (src.declared.variety_map)  {
                builder.declared.variety_map = true;
                builder.variety_map = src.variety_map;
            }
            if (src.declared.interface_)  {
                builder.declared.interface_ = true;
                builder.interface_ = src.interface_;
            }
            if (src.declared.interface_list)  {
                builder.declared.interface_list = true;
                builder.interface_list = src.interface_list;
            }
            if (src.declared.interface_map)  {
                builder.declared.interface_map = true;
                builder.interface_map = src.interface_map;
            }
        }

        @Override
        public Collection<? extends MutableThing> references() {
            List<MutableThing> _out = new ArrayList<>();
            if (id_thing != null) _out.add(id_thing);
            if (id_list != null) _out.addAll(id_list);
            if (id_map != null) _out.addAll(id_map.values());
            if (remap_target != null) _out.addAll(remap_target);
            return _out;
        }

        @Override
        public MutableThing root() {
            return _root;
        }

        @Override
        public void imprint(ThingExample value, Mutables mutables) {
            boolean changed = false;
            if (value.declared.id) {
                builder.declared.id = true;
                changed = changed || Mutables.changed(builder.id, value.id);
                builder.id = value.id;
            }
            if (value.declared.id_derived) {
                builder.declared.id_derived = true;
                changed = changed || Mutables.changed(builder.id_derived, value.id_derived);
                builder.id_derived = value.id_derived;
            }
            if (value.declared.hash_target) {
                builder.declared.hash_target = true;
                changed = changed || Mutables.changed(builder.hash_target, value.hash_target);
                builder.hash_target = value.hash_target;
            }
            if (value.declared.val) {
                builder.declared.val = true;
                changed = changed || Mutables.changed(builder.val, value.val);
                builder.val = value.val;
            }
            if (value.declared.val_list) {
                builder.declared.val_list = true;
                changed = changed || Mutables.changed(builder.val_list, value.val_list);
                builder.val_list = value.val_list;
            }
            if (value.declared.val_map) {
                builder.declared.val_map = true;
                changed = changed || Mutables.changed(builder.val_map, value.val_map);
                builder.val_map = value.val_map;
            }
            if (value.declared.bool) {
                builder.declared.bool = true;
                changed = changed || Mutables.changed(builder.bool, value.bool);
                builder.bool = value.bool;
            }
            if (value.declared.id_thing) {
                builder.declared.id_thing = true;
                changed = changed || Mutables.changed(id_thing, value.id_thing);
                if (changed) mutables.unlink(this, id_thing);
                id_thing = mutables.imprint(value.id_thing, _root);
                if (changed) mutables.link(this, id_thing);
            }
            if (value.declared.id_list) {
                builder.declared.id_list = true;
                changed = changed || Mutables.changed(id_list, value.id_list);
                if (changed) mutables.unlink(this, id_list);
                id_list = mutables.imprint(value.id_list, _root);
                if (changed) mutables.link(this, id_list);
            }
            if (value.declared.id_map) {
                builder.declared.id_map = true;
                changed = changed || Mutables.changed(id_map, value.id_map);
                if (changed) mutables.unlink(this, id_map);
                id_map = mutables.imprint(value.id_map, _root);
                if (changed) mutables.link(this, id_map);
            }
            if (value.declared.non_id_thing) {
                builder.declared.non_id_thing = true;
                changed = changed || Mutables.changed(builder.non_id_thing, value.non_id_thing);
                builder.non_id_thing = value.non_id_thing;
            }
            if (value.declared.non_id_list) {
                builder.declared.non_id_list = true;
                changed = changed || Mutables.changed(builder.non_id_list, value.non_id_list);
                builder.non_id_list = value.non_id_list;
            }
            if (value.declared.non_id_map) {
                builder.declared.non_id_map = true;
                changed = changed || Mutables.changed(builder.non_id_map, value.non_id_map);
                builder.non_id_map = value.non_id_map;
            }
            if (value.declared.derived_first_available) {
                builder.declared.derived_first_available = true;
                changed = changed || Mutables.changed(builder.derived_first_available, value.derived_first_available);
                builder.derived_first_available = value.derived_first_available;
            }
            if (value.declared.remap_target) {
                builder.declared.remap_target = true;
                changed = changed || Mutables.changed(remap_target, value.remap_target);
                if (changed) mutables.unlink(this, remap_target);
                remap_target = mutables.imprint(value.remap_target, _root);
                if (changed) mutables.link(this, remap_target);
            }
            if (value.declared.derived_remap) {
                builder.declared.derived_remap = true;
                changed = changed || Mutables.changed(builder.derived_remap, value.derived_remap);
                builder.derived_remap = value.derived_remap;
            }
            if (value.declared.reactive_to_type) {
                builder.declared.reactive_to_type = true;
                changed = changed || Mutables.changed(builder.reactive_to_type, value.reactive_to_type);
                builder.reactive_to_type = value.reactive_to_type;
            }
            if (value.declared.reactive_to_type_field) {
                builder.declared.reactive_to_type_field = true;
                changed = changed || Mutables.changed(builder.reactive_to_type_field, value.reactive_to_type_field);
                builder.reactive_to_type_field = value.reactive_to_type_field;
            }
            if (value.declared.reactive_to_sibling_field) {
                builder.declared.reactive_to_sibling_field = true;
                changed = changed || Mutables.changed(builder.reactive_to_sibling_field, value.reactive_to_sibling_field);
                builder.reactive_to_sibling_field = value.reactive_to_sibling_field;
            }
            if (value.declared.reactive_to_any_sibling_field) {
                builder.declared.reactive_to_any_sibling_field = true;
                changed = changed || Mutables.changed(builder.reactive_to_any_sibling_field, value.reactive_to_any_sibling_field);
                builder.reactive_to_any_sibling_field = value.reactive_to_any_sibling_field;
            }
            if (value.declared.reactive_collection_field) {
                builder.declared.reactive_collection_field = true;
                changed = changed || Mutables.changed(builder.reactive_collection_field, value.reactive_collection_field);
                builder.reactive_collection_field = value.reactive_collection_field;
            }
            if (value.declared.dangerous) {
                builder.declared.dangerous = true;
                changed = changed || Mutables.changed(builder.dangerous, value.dangerous);
                builder.dangerous = value.dangerous;
            }
            if (value.declared.dangerous_list) {
                builder.declared.dangerous_list = true;
                changed = changed || Mutables.changed(builder.dangerous_list, value.dangerous_list);
                builder.dangerous_list = value.dangerous_list;
            }
            if (value.declared.dangerous_map) {
                builder.declared.dangerous_map = true;
                changed = changed || Mutables.changed(builder.dangerous_map, value.dangerous_map);
                builder.dangerous_map = value.dangerous_map;
            }
            if (value.declared.has_dangerous) {
                builder.declared.has_dangerous = true;
                changed = changed || Mutables.changed(builder.has_dangerous, value.has_dangerous);
                builder.has_dangerous = value.has_dangerous;
            }
            if (value.declared.has_dangerous_list) {
                builder.declared.has_dangerous_list = true;
                changed = changed || Mutables.changed(builder.has_dangerous_list, value.has_dangerous_list);
                builder.has_dangerous_list = value.has_dangerous_list;
            }
            if (value.declared.has_dangerous_map) {
                builder.declared.has_dangerous_map = true;
                changed = changed || Mutables.changed(builder.has_dangerous_map, value.has_dangerous_map);
                builder.has_dangerous_map = value.has_dangerous_map;
            }
            if (value.declared.variety) {
                builder.declared.variety = true;
                changed = changed || Mutables.changed(builder.variety, value.variety);
                builder.variety = value.variety;
            }
            if (value.declared.variety_list) {
                builder.declared.variety_list = true;
                changed = changed || Mutables.changed(builder.variety_list, value.variety_list);
                builder.variety_list = value.variety_list;
            }
            if (value.declared.variety_map) {
                builder.declared.variety_map = true;
                changed = changed || Mutables.changed(builder.variety_map, value.variety_map);
                builder.variety_map = value.variety_map;
            }
            if (value.declared.interface_) {
                builder.declared.interface_ = true;
                changed = changed || Mutables.changed(builder.interface_, value.interface_);
                builder.interface_ = value.interface_;
            }
            if (value.declared.interface_list) {
                builder.declared.interface_list = true;
                changed = changed || Mutables.changed(builder.interface_list, value.interface_list);
                builder.interface_list = value.interface_list;
            }
            if (value.declared.interface_map) {
                builder.declared.interface_map = true;
                changed = changed || Mutables.changed(builder.interface_map, value.interface_map);
                builder.interface_map = value.interface_map;
            }
            if (changed) mutables.flagChanged(this);
        }

        @Override
        public ThingExample build() {
            if (_built != null) return _built;
            builder.id_thing = Mutables.build(id_thing);
            builder.id_list = Mutables.build(id_list);
            builder.id_map = Mutables.build(id_map);
            builder.remap_target = Mutables.build(remap_target);
            _built = builder.build();
            return _built;
        }

        @Override
        public ThingExample identity() {
            return _identity;
        }

        @Override
        public ThingExample previous() {
            ThingExample v = _previous;
            _previous = null;
            return v;
        }

        @Override
        public void invalidate() {
            if (_built != null) _previous = _built;
            _built = null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return _identity.equals(((Mutable)o)._identity);
        }

        @Override
        public int hashCode() {
            return _identity.hashCode();
        }
    }

    /**
     * Methods for deriving fields. Only includes fields that can be purely derived without a spec. If a field isn't included here, it requires a spec. See {@link com.pocket.sync.spec.Spec#derive()}
     */
    public static class Derive {
        public static Builder derived_first_available(Builder builder) {
            boolean _tmp_declared1 = builder != null && builder.declared.val;
            if (_tmp_declared1) {
                String _tmp = builder != null ? builder.val : null;
                if (!Modeller.isBlank(_tmp)) {
                    return builder.derived_first_available(_tmp);
                }
            }
            boolean _tmp_declared2 = builder != null && builder.declared.id;
            if (_tmp_declared2) {
                String _tmp = builder != null ? builder.id : null;
                if (!Modeller.isBlank(_tmp)) {
                    return builder.derived_first_available(_tmp);
                }
            }
            return builder;
        }

        public static Builder id_derived(Builder builder) {
            boolean _tmp_declared1 = builder != null && builder.declared.id;
            if (_tmp_declared1) {
                String _tmp = builder != null ? builder.id : null;
                if (!Modeller.isBlank(_tmp)) {
                    return builder.id_derived(_tmp);
                }
            }
            boolean _tmp_declared2 = builder != null && builder.declared.val;
            if (_tmp_declared2) {
                String _tmp = builder != null ? builder.val : null;
                if (!Modeller.isBlank(_tmp)) {
                    return builder.id_derived(_tmp);
                }
            }
            return builder;
        }
    }
}
