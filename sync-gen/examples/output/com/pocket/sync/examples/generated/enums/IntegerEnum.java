package com.pocket.sync.examples.generated.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.StreamingTypeParser;
import com.pocket.sync.value.TypeParser;
import com.pocket.sync.value.binary.ByteReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IntegerEnum extends com.pocket.sync.value.IntegerEnum {
    /**
     * All enums that have been created via create() during this process. Includes those known at compile time and any unknown ones that were created at runtime.
     */
    private static Map<Integer, IntegerEnum> KNOWN = new HashMap<>();

    public static final TypeParser<IntegerEnum> JSON_CREATOR = IntegerEnum::create;

    public static final StreamingTypeParser<IntegerEnum> STREAMING_JSON_CREATOR = IntegerEnum::from;

    /**
     * One
     */
    public static final IntegerEnum ONE = make(1, 1, "ONE");

    /**
     * Two
     */
    public static final IntegerEnum TWO = make(2, 2, "TWO");

    public static final ByteTypeParser<IntegerEnum> BYTE_CREATOR = IntegerEnum::uncompress;

    private static final Collection<IntegerEnum> VALUES = Collections.unmodifiableCollection(KNOWN.values());

    private IntegerEnum(Integer value, int id, String name) {
        super(value, id, name);
    }

    /**
     * If this value matches one of the known names at compile time, return the instance.
     */
    public static IntegerEnum find(String name) {
        if (Modeller.isBlank(name)) return null;
        for (IntegerEnum value : KNOWN.values()) {
            if (name.equalsIgnoreCase(value.name)) return value;
        }
        return null;
    }

    public static IntegerEnum findFromName(JsonNode json) {
        if (json == null || json.isNull()) return null;
        return find(json.asText());
    }

    public static IntegerEnum findFromName(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return find(Modeller.asString(parser));
    }

    /**
     * Creates a new value. If the value is empty or null, or if the value already exists, this throws an error.
     */
    private static IntegerEnum make(Integer value, int id, String name) {
        if (Modeller.isBlank(value)) throw new IllegalArgumentException("empty value");
        IntegerEnum e = KNOWN.get(value);
        if (e == null) {
            e = new IntegerEnum(value, id, name);
            KNOWN.put(e.value, e);
        } else {
            throw new IllegalArgumentException("already exists");
        }
        return e;
    }

    /**
     * Find or create an instance from this value. If the value is empty or null, this returns null and no value is created.
     */
    public static IntegerEnum create(Integer value) {
        if (Modeller.isBlank(value)) return null;
        IntegerEnum e = KNOWN.get(value);
        if (e == null) {
            e = new IntegerEnum(value, 0, value.toString());
            KNOWN.put(e.value, e);
        }
        return e;
    }

    /**
     * If this value matches one of the known values at compile time, return the instance.
     */
    public static IntegerEnum find(Integer value) {
        for (IntegerEnum e : VALUES) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }

    public static IntegerEnum create(JsonNode json) {
        if (json == null || json.isNull()) return null;
        return create(json.asInt());
    }

    public static IntegerEnum from(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return create(Modeller.asInteger(parser));
    }

    /**
     * An immutable list of all enum values that were known at compile time. Similar to a normal java enum.values(). Does not include those found at runtime via create()
     */
    public static Collection<IntegerEnum> values() {
        return VALUES;
    }

    public static IntegerEnum uncompress(ByteReader from) {
        int id = from.readInt();
        switch(id) {
            case 0: return create(from.readInt());
            case 1: return ONE;
            case 2: return TWO;
            default: throw new RuntimeException();
        }
    }
}
