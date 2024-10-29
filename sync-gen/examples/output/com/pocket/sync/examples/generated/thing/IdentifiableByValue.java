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
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.mutable.MutableThing;
import com.pocket.sync.space.mutable.Mutables;
import com.pocket.sync.spec.Reactions;
import com.pocket.sync.thing.FlatUtils;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingBuilder;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.StreamingThingParser;
import com.pocket.sync.value.SyncableParser;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.binary.ByteWriter;
import com.pocket.sync.value.protect.StringEncrypter;
import com.pocket.util.java.JsonUtil;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.RuntimeException;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Identifying fields are values
 */
public final class IdentifiableByValue implements GraphQlSyncable, Thing {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<IdentifiableByValue> JSON_CREATOR = IdentifiableByValue::from;

    public static final StreamingThingParser<IdentifiableByValue> STREAMING_JSON_CREATOR = IdentifiableByValue::from;

    public static final String THING_TYPE = "IdentifiableByValue";

    public static final Remote REMOTE = new Remote(null, Remote.Method.GET, ExamplesRemoteStyle.REMOTE, null);

    public static final ByteTypeParser<IdentifiableByValue> BYTE_CREATOR = IdentifiableByValue::uncompress;

    @Nullable
    public final String id;

    @Nullable
    public final String val;

    public final Declared declared;

    private IdentifiableByValue _identity;

    /**
     * Lazy init'd and cached during idkey()
     */
    private String _idkey;

    private IdentifiableByValue(Builder builder, Declared declared) {
        this.declared = declared;
        this.id = builder.id;
        this.val = builder.val;
    }

    @NotNull
    @Override
    public GraphQlSupport graphQl() {
        return GRAPHQL;
    }

    @Override
    public ExamplesAuthType auth() {
        return ExamplesAuthType.NO;
    }

    public static IdentifiableByValue from(JsonNode jsonNode, JsonConfig _config,
            Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("id");
        if (value != null) builder.id(Modeller.asString(value));
        value = json.get("val");
        if (value != null) builder.val(Modeller.asString(value));
        return builder.build();
    }

    public static IdentifiableByValue from(JsonParser parser, JsonConfig _config, Allow... allowed)
            throws IOException {
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
            } else if (currentName.equals("val")) {
                builder.val(Modeller.asString(parser));
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
        }
        if (declared.id) json.put("id", Modeller.toJsonValue(id));
        if (declared.val) json.put("val", Modeller.toJsonValue(val));
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
    public IdentifiableByValue identity() {
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
        if (declared.val) map.put("val", val);
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
        if (e == Thing.Equality.IDENTITY) return _result;
        _result = 31 * _result + (val != null ? val.hashCode() : 0);
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
        IdentifiableByValue that = (IdentifiableByValue) o;
        if (e == Thing.Equality.STATE_DECLARED) {
            if (that.declared.id && declared.id) if (id != null ? !id.equals(that.id) : that.id != null) return false;
            if (that.declared.val && declared.val) if (val != null ? !val.equals(that.val) : that.val != null) return false;
            return true;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (e == Thing.Equality.IDENTITY) return true;
        if (val != null ? !val.equals(that.val) : that.val != null) return false;
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
        IdentifiableByValue was = (IdentifiableByValue) was_t;
        IdentifiableByValue is = (IdentifiableByValue) is_t;
        output.type("ThingExample", "reactive_to_type");
        if (is != null && is.declared.val && (was == null || !(was != null && was.declared.val) || ObjectUtils.notEqual(was != null ? was.val : null, is != null ? is.val : null))) {
            output.type("ThingExample", "reactive_to_type_field");
        }
    }

    @Override
    public void subthings(FlatUtils.Output out) {
    }

    @Override
    public IdentifiableByValue flat() {
        return this;
    }

    @Override
    public IdentifiableByValue with(Changes.ThingMatch match, Thing replace) {
        return null;
    }

    @Override
    public IdentifiableByValue redact(StringEncrypter e) {
        return this;
    }

    @Override
    public IdentifiableByValue unredact(StringEncrypter e) {
        return this;
    }

    @Override
    public void compress(ByteWriter out) {
        out.writeInt(2);
        if (out.writeBit(declared.id)) {
            out.writeBit(id != null);
        }
        if (out.writeBit(declared.val)) {
            out.writeBit(val != null);
        }
        out.finishByte();
        if (id != null) {
            out.writeString(id);
        }
        if (val != null) {
            out.writeString(val);
        }
    }

    public static IdentifiableByValue uncompress(ByteReader _in) {
        Builder _builder = new Builder();
        int _fields = _in.readInt();
        boolean _read_id = false;
        boolean _read_val = false;
        _fields_break :  {
            if (0 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_id = _in.readBit())) _builder.id(null);
            if (1 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_val = _in.readBit())) _builder.val(null);
        }
        _in.finishByte();
        if (_read_id) _builder.id(Modeller.STRING_BYTE_CREATOR.create(_in));
        if (_read_val) _builder.val(Modeller.STRING_BYTE_CREATOR.create(_in));
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

        private boolean val;
    }

    public static class Declared {
        public final boolean id;

        public final boolean val;

        private Declared(DeclaredMutable declared) {
            this.id = declared.id;
            this.val = declared.val;
        }
    }

    public static class Builder implements ThingBuilder<IdentifiableByValue> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected String id;

        protected String val;

        public Builder() {
        }

        public Builder(IdentifiableByValue src) {
            set(src);
        }

        @Override
        public Builder set(IdentifiableByValue src) {
            if (src.declared.id) {
                declared.id = true;
                this.id = src.id;
            }
            if (src.declared.val) {
                declared.val = true;
                this.val = src.val;
            }
            return this;
        }

        public Builder id(String value) {
            declared.id = true;
            this.id = Modeller.immutable(value);
            return this;
        }

        public Builder val(String value) {
            declared.val = true;
            this.val = Modeller.immutable(value);
            return this;
        }

        @Override
        public IdentifiableByValue build() {
            return new IdentifiableByValue(this, new Declared(declared));
        }
    }

    public static class IdBuilder implements ThingBuilder<IdentifiableByValue> {
        private final Builder builder = new Builder();

        public IdBuilder() {
        }

        public IdBuilder(IdentifiableByValue src) {
            set(src);
        }

        @Override
        public IdBuilder set(IdentifiableByValue src) {
            if (src.declared.id) {
                builder.declared.id = true;
                builder.id = src.id;
            }
            return this;
        }

        public IdBuilder id(String value) {
            builder.id(value);
            return this;
        }

        @Override
        public IdentifiableByValue build() {
            return new IdentifiableByValue(builder, new Declared(builder.declared));
        }
    }

    public static class Mutable implements MutableThing<IdentifiableByValue> {
        private final Builder builder = new Builder();

        private final IdentifiableByValue _identity;

        private IdentifiableByValue _built;

        private IdentifiableByValue _previous;

        private MutableThing _root;

        private Mutable(IdentifiableByValue src, Mutables mutables) {
            _identity = src.identity();
            _root = this;
            if (src.declared.id)  {
                builder.declared.id = true;
                builder.id = src.id;
            }
            if (src.declared.val)  {
                builder.declared.val = true;
                builder.val = src.val;
            }
        }

        @Override
        public Collection<? extends MutableThing> references() {
            List<MutableThing> _out = new ArrayList<>();
            return _out;
        }

        @Override
        public MutableThing root() {
            return _root;
        }

        @Override
        public void imprint(IdentifiableByValue value, Mutables mutables) {
            boolean changed = false;
            if (value.declared.id) {
                builder.declared.id = true;
                changed = changed || Mutables.changed(builder.id, value.id);
                builder.id = value.id;
            }
            if (value.declared.val) {
                builder.declared.val = true;
                changed = changed || Mutables.changed(builder.val, value.val);
                builder.val = value.val;
            }
            if (changed) mutables.flagChanged(this);
        }

        @Override
        public IdentifiableByValue build() {
            if (_built != null) return _built;
            _built = builder.build();
            return _built;
        }

        @Override
        public IdentifiableByValue identity() {
            return _identity;
        }

        @Override
        public IdentifiableByValue previous() {
            IdentifiableByValue v = _previous;
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
}
