package com.pocket.sync.examples.generated;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.value.IdString;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.BaseModeller;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.IntegerEnum;
import com.pocket.sync.value.StreamingTypeParser;
import com.pocket.sync.value.StringEnum;
import com.pocket.sync.value.TypeParser;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.protect.StringEncrypter;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.Long;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Modeller extends BaseModeller {
    public static final ExamplesThings THINGS = new ExamplesThings();

    public static TypeParser<Boolean> BOOLEAN_CREATOR = Modeller::asBoolean;

    public static StreamingTypeParser<Boolean> BOOLEAN_STREAMING_CREATOR = Modeller::asBoolean;

    public static ByteTypeParser<Boolean> BOOLEAN_BYTE_CREATOR = Modeller::uncompressAsBoolean;

    public static TypeParser<Double> FLOAT_CREATOR = Modeller::asFloat;

    public static StreamingTypeParser<Double> FLOAT_STREAMING_CREATOR = Modeller::asFloat;

    public static ByteTypeParser<Double> FLOAT_BYTE_CREATOR = Modeller::uncompressAsFloat;

    public static TypeParser<IdString> ID_CREATOR = Modeller::asID;

    public static StreamingTypeParser<IdString> ID_STREAMING_CREATOR = Modeller::asID;

    public static ByteTypeParser<IdString> ID_BYTE_CREATOR = Modeller::uncompressAsID;

    public static TypeParser<Integer> INTEGER_CREATOR = Modeller::asInteger;

    public static StreamingTypeParser<Integer> INTEGER_STREAMING_CREATOR = Modeller::asInteger;

    public static ByteTypeParser<Integer> INTEGER_BYTE_CREATOR = Modeller::uncompressAsInteger;

    public static TypeParser<String> STRING_CREATOR = Modeller::asString;

    public static StreamingTypeParser<String> STRING_STREAMING_CREATOR = Modeller::asString;

    public static ByteTypeParser<String> STRING_BYTE_CREATOR = Modeller::uncompressAsString;

    public static TypeParser<String> DANGEROUS_CREATOR = Modeller::asDangerous;

    public static StreamingTypeParser<String> DANGEROUS_STREAMING_CREATOR = Modeller::asDangerous;

    public static ByteTypeParser<String> DANGEROUS_BYTE_CREATOR = Modeller::uncompressAsDangerous;

    public static TypeParser<Timestamp> TIMESTAMP_CREATOR = Modeller::asTimestamp;

    public static StreamingTypeParser<Timestamp> TIMESTAMP_STREAMING_CREATOR = Modeller::asTimestamp;

    public static ByteTypeParser<Timestamp> TIMESTAMP_BYTE_CREATOR = Modeller::uncompressAsTimestamp;

    public static Boolean asBoolean(JsonNode value) {
        if (isNull(value)) return null;
        return asBoolean(value);
    }

    public static Boolean asBoolean(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return asBoolean(parser);
    }

    public static Boolean toJsonValue(Boolean value) {
        if (value == null) return null;
        return value;
    }

    public static Boolean immutable(Boolean value) {
        if (value == null) return null;
        return value;
    }

    public static boolean asBoolean(Boolean from) {
        return from;
    }

    public static Boolean uncompressAsBoolean(ByteReader from) {
        return from.readBoolean();
    }

    public static boolean isBlank(Boolean value) {
        if (value == null) return true;
        return false;
    }

    public static Double asFloat(JsonNode value) {
        if (isNull(value)) return null;
        return value.asDouble();
    }

    public static Double asFloat(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return asDouble(parser);
    }

    public static Double toJsonValue(Double value) {
        if (value == null) return null;
        return value;
    }

    public static Double immutable(Double value) {
        if (value == null) return null;
        return value;
    }

    public static Double uncompressAsFloat(ByteReader from) {
        return from.readDouble();
    }

    public static boolean isBlank(Double value) {
        if (value == null) return true;
        return false;
    }

    public static IdString asID(JsonNode value) {
        if (isNull(value)) return null;
        return new IdString(asString(value));
    }

    public static IdString asID(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return new IdString(asString(parser));
    }

    public static String toJsonValue(IdString value) {
        if (value == null) return null;
        return value.id;
    }

    public static IdString immutable(IdString value) {
        if (value == null) return null;
        return value;
    }

    public static IdString uncompressAsID(ByteReader from) {
        return new IdString(from.readString());
    }

    public static boolean isBlank(IdString value) {
        if (value == null) return true;
        return false;
    }

    public static Integer asInteger(JsonNode value) {
        if (isNull(value)) return null;
        return value.asInt();
    }

    public static Integer asInteger(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return BaseModeller.asInteger(parser);
    }

    public static Integer toJsonValue(Integer value) {
        if (value == null) return null;
        return value;
    }

    public static Integer immutable(Integer value) {
        if (value == null) return null;
        return value;
    }

    public static Integer uncompressAsInteger(ByteReader from) {
        return from.readInt();
    }

    public static boolean isBlank(Integer value) {
        if (value == null) return true;
        return false;
    }

    public static String asString(JsonNode value) {
        if (isNull(value)) return null;
        return value.asText();
    }

    public static String asString(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return BaseModeller.asString(parser);
    }

    public static String toJsonValue(String value) {
        if (value == null) return null;
        return value;
    }

    public static String immutable(String value) {
        if (value == null) return null;
        return value;
    }

    public static String uncompressAsString(ByteReader from) {
        return from.readString();
    }

    public static boolean isBlank(String value) {
        if (value == null) return true;
        return StringUtils.isBlank(value);
    }

    public static String asDangerous(JsonNode value) {
        if (isNull(value)) return null;
        return value.asText();
    }

    public static String asDangerous(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return BaseModeller.asString(parser);
    }

    /**
     * See {@link Thing#toJson(JsonConfig, Include[])} for warnings about dangerous values and the protections you should used when accessing them. This method should not be accessed at all unless you mean to, but to ensure you do this will throw error if you don't pass in Include.DANGEROUS to ensure you mean it. If you don't want dangerous values, don't even access this value keep it undeclared instead.
     */
    public static String toJsonValue(String value, Include[] includes) {
        if (!ArrayUtils.contains(includes, Include.DANGEROUS)) throw new RuntimeException("invalid usage");
        if (value == null) return null;
        return value;
    }

    public static String redact(String v, StringEncrypter e) {
        if (v == null) return null;
        return new String(e.encrypt(v.value));
    }

    public static String unredact(String v, StringEncrypter e) {
        if (v == null) return null;
        return new String(e.decrypt(v.value));
    }

    public static String immutable(String value) {
        if (value == null) return null;
        return value;
    }

    public static String uncompressAsDangerous(ByteReader from) {
        return from.readString();
    }

    public static boolean isBlank(String value) {
        if (value == null) return true;
        return StringUtils.isBlank(value);
    }

    public static Timestamp asTimestamp(JsonNode value) {
        if (isNull(value)) return null;
        return new Timestamp(value.asLong());
    }

    public static Timestamp asTimestamp(JsonParser parser) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) return null;
        return new Timestamp(asLong(parser));
    }

    public static Long toJsonValue(Timestamp value) {
        if (value == null) return null;
        return value.unixSeconds;
    }

    public static Timestamp immutable(Timestamp value) {
        if (value == null) return null;
        return value;
    }

    public static Timestamp uncompressAsTimestamp(ByteReader from) {
        return new Timestamp(from.readLong());
    }

    public static boolean isBlank(Timestamp value) {
        if (value == null) return true;
        return false;
    }

    public static ArrayNode toJsonValue(List value, JsonConfig _config, Include... includes) {
        if (value == null) return null;
        ArrayNode out = OBJECT_MAPPER.createArrayNode();
        for (Object v : value) {
            if (v instanceof Thing) {
                out.add(((Thing) v).toJson(_config, includes));
            } else if (v instanceof StringEnum) {
                out.add(((StringEnum) v).getValue());
            } else if (v instanceof IntegerEnum) {
                out.add(((IntegerEnum) v).getValue());
            } else if (v instanceof Boolean) {
                out.add(toJsonValue((Boolean) v));
            } else if (v instanceof Double) {
                out.add(toJsonValue((Double) v));
            } else if (v instanceof IdString) {
                out.add(toJsonValue((IdString) v));
            } else if (v instanceof Integer) {
                out.add(toJsonValue((Integer) v));
            } else if (v instanceof String) {
                out.add(toJsonValue((String) v));
            } else if (v instanceof String) {
                out.add(toJsonValue((String) v, includes));
            } else if (v instanceof Timestamp) {
                out.add(toJsonValue((Timestamp) v));
            } else if (v == null) {
                out.add(NullNode.getInstance());
            } else {
                throw new RuntimeException("unknown type " + v);
            }
        }
        return out;
    }

    public static ObjectNode toJsonValue(Map<String, ?> value, JsonConfig _config,
            Include... includes) {
        if (value == null) return null;
        ObjectNode out = OBJECT_MAPPER.createObjectNode();
        for (Map.Entry<String,?> entry : value.entrySet()) {
            String key = entry.getKey();
            Object v = entry.getValue();
            if (v instanceof Thing) {
                out.set(key, ((Thing) v).toJson(_config, includes));
            } else if (v instanceof StringEnum) {
                out.put(key, ((StringEnum) v).getValue());
            } else if (v instanceof IntegerEnum) {
                out.put(key, ((IntegerEnum) v).getValue());
            } else if (v instanceof Boolean) {
                out.put(key, toJsonValue((Boolean) v));
            } else if (v instanceof Double) {
                out.put(key, toJsonValue((Double) v));
            } else if (v instanceof IdString) {
                out.put(key, toJsonValue((IdString) v));
            } else if (v instanceof Integer) {
                out.put(key, toJsonValue((Integer) v));
            } else if (v instanceof String) {
                out.put(key, toJsonValue((String) v));
            } else if (v instanceof String) {
                out.put(key, toJsonValue((String) v, includes));
            } else if (v instanceof Timestamp) {
                out.put(key, toJsonValue((Timestamp) v));
            } else if (v == null) {
                out.put(key, NullNode.getInstance());
            } else {
                throw new RuntimeException("unknown type " + v);
            }
        }
        return out;
    }
}
