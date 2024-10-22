package com.pocket.sync.examples.generated.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.StreamingTypeParser;
import com.pocket.sync.value.StringEnum;
import com.pocket.sync.value.TypeParser;
import com.pocket.sync.value.binary.ByteReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BasicEnum extends StringEnum {
    /**
     * All enums that have been created via create() during this process. Includes those known at compile time and any unknown ones that were created at runtime.
     */
    private static Map<String, BasicEnum> KNOWN = new HashMap<>();

    public static final TypeParser<BasicEnum> JSON_CREATOR = BasicEnum::create;

    public static final StreamingTypeParser<BasicEnum> STREAMING_JSON_CREATOR = BasicEnum::from;

    public static final BasicEnum ONE = make("one", 1, "one");

    public static final BasicEnum TWO = make("two", 2, "two");

    public static final ByteTypeParser<BasicEnum> BYTE_CREATOR = BasicEnum::uncompress;

    private static final Collection<BasicEnum> VALUES = Collections.unmodifiableCollection(KNOWN.values());

    private BasicEnum(String value, int id, String name) {
        super(value, id, name);
    }

    /**
     * Creates a new value. If the value is empty or null, or if the value already exists, this throws an error.
     */
    private static BasicEnum make(String value, int id, String name) {
        if (Modeller.isBlank(value)) throw new IllegalArgumentException("empty value");
        BasicEnum e = KNOWN.get(value);
        if (e == null) {
            e = new BasicEnum(value, id, name);
            KNOWN.put(e.value, e);
        } else {
            throw new IllegalArgumentException("already exists");
        }
        return e;
    }

    /**
     * Find or create an instance from this value. If the value is empty or null, this returns null and no value is created.
     */
    public static BasicEnum create(String value) {
        if (Modeller.isBlank(value)) return null;
        BasicEnum e = KNOWN.get(value);
        if (e == null) {
            e = new BasicEnum(value, 0, value.toString());
            KNOWN.put(e.value, e);
        }
        return e;
    }

    /**
     * If this value matches one of the known values at compile time, return the instance.
     */
    public static BasicEnum find(String value) {
        for (BasicEnum e : VALUES) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }

    public static BasicEnum create(JsonNode json) {
        if (json == null || json.isNull()) return null;
        return create(json.asText());
    }

    public static BasicEnum from(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return create(Modeller.asString(parser));
    }

    /**
     * An immutable list of all enum values that were known at compile time. Similar to a normal java enum.values(). Does not include those found at runtime via create()
     */
    public static Collection<BasicEnum> values() {
        return VALUES;
    }

    public static BasicEnum uncompress(ByteReader from) {
        int id = from.readInt();
        switch(id) {
            case 0: return create(from.readString());
            case 1: return ONE;
            case 2: return TWO;
            default: throw new RuntimeException();
        }
    }
}
