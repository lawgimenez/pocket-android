package com.pocket.sync.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.binary.ByteReader;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * A helper class for implementing all of the various parsers when there is an Open Type.
 * See Figment docs for more details on Open Types, but tl;dr is when a value could be one of multiple,
 * types of things, such as interfaces and {@link Variety}s.
 * <p>
 * In these cases it is expected that there was a special value added to describe what {@link Thing#type()}
 * the data represents.
 */
public class OpenParser<T extends Thing> implements ByteTypeParser<T>, SyncableParser<T>, StreamingThingParser<T> {

    private final Spec.Things things;
    private final ObjectMapper mapper;
    private final String unknownType;

    public OpenParser(Spec.Things things, ObjectMapper mapper, String unknownType) {
        this.things = things;
        this.mapper = mapper;
        this.unknownType = unknownType;
    }

    @Override
    public T create(ByteReader value) {
        // It is expected that the first bytes are the type name, followed by the thing data.
        String type = value.readString();
        T t = (T) things.thing(type, value);
        if (t == null) t = (T) things.thing(unknownType, value);
        if (t == null) throw new RuntimeException("unknown type " + type);
        return t;
    }

    @Override
    public T create(JsonParser parser, JsonConfig config, Allow... allowed) throws IOException {
        // Since we need to extract the type name before we can route it to the correct parser, we have to come out of streaming mode here.
        return create((ObjectNode) mapper.readTree(parser), config, allowed);
    }

    @Override
    public T create(JsonNode value, JsonConfig config, Allow... allowed) {
        if (value.isNull()) return null;
        // It is expected that the type has been added as a special field
        String type = value.get(Thing.SERIALIZATION_TYPE_KEY).asText();
        if (StringUtils.isBlank(type)) throw new RuntimeException("type name not specified for open type");
        T t = (T) things.thing(type, (ObjectNode) value, config);
        if (t == null && Allow.contains(allowed, Allow.UNKNOWN)) {
            t = (T) things.thing(unknownType, (ObjectNode) value, config);
        }
        if (t == null) throw new RuntimeException("unknown type " + type);
        return t;
    }
}
