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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class UnknownVarietyExample implements GraphQlSyncable, VarietyExample, Thing {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<UnknownVarietyExample> JSON_CREATOR = UnknownVarietyExample::from;

    public static final StreamingThingParser<UnknownVarietyExample> STREAMING_JSON_CREATOR = UnknownVarietyExample::from;

    public static final String THING_TYPE = "UnknownVarietyExample";

    public static final Remote REMOTE = new Remote(null, Remote.Method.GET, ExamplesRemoteStyle.CLIENT_API, null);

    public static final ByteTypeParser<UnknownVarietyExample> BYTE_CREATOR = UnknownVarietyExample::uncompress;

    public final Declared declared;

    private UnknownVarietyExample _identity;

    /**
     * Lazy init'd and cached during idkey()
     */
    private String _idkey;

    private UnknownVarietyExample(Builder builder, Declared declared) {
        this.declared = declared;
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

    public static UnknownVarietyExample from(JsonNode jsonNode, JsonConfig _config,
            Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        return builder.build();
    }

    public static UnknownVarietyExample from(JsonParser parser, JsonConfig _config,
            Allow... allowed) throws IOException {
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
    public UnknownVarietyExample identity() {
        if (_identity != null) return _identity;
        return this;
    }

    @Override
    public boolean isIdentifiable() {
        return false;
    }

    @Override
    public String rootValue() {
        return null;
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
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
        if (e == Thing.Equality.IDENTITY) e = Thing.Equality.STATE ;
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
        UnknownVarietyExample that = (UnknownVarietyExample) o;
        if (e == Thing.Equality.STATE_DECLARED) {
            return true;
        }
        return true;
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public Mutable mutable(Mutables mutables, MutableThing root) {
        return new Mutable(this, mutables, root);
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
    }

    @Override
    public void subthings(FlatUtils.Output out) {
    }

    @Override
    public UnknownVarietyExample flat() {
        return this;
    }

    @Override
    public UnknownVarietyExample with(Changes.ThingMatch match, Thing replace) {
        return null;
    }

    @Override
    public UnknownVarietyExample redact(StringEncrypter e) {
        return this;
    }

    @Override
    public UnknownVarietyExample unredact(StringEncrypter e) {
        return this;
    }

    @Override
    public void compress(ByteWriter out) {
        out.writeInt(0);
        out.finishByte();
    }

    public static UnknownVarietyExample uncompress(ByteReader _in) {
        Builder _builder = new Builder();
        int _fields = _in.readInt();
        _fields_break :  {
        }
        _in.finishByte();
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
    }

    public static class Declared {
        private Declared(DeclaredMutable declared) {
        }
    }

    public static class Builder implements ThingBuilder<UnknownVarietyExample> {
        private DeclaredMutable declared = new DeclaredMutable();

        public Builder() {
        }

        public Builder(UnknownVarietyExample src) {
            set(src);
        }

        @Override
        public Builder set(UnknownVarietyExample src) {
            return this;
        }

        @Override
        public UnknownVarietyExample build() {
            return new UnknownVarietyExample(this, new Declared(declared));
        }
    }

    public static class Mutable implements MutableThing<UnknownVarietyExample> {
        private final Builder builder = new Builder();

        private final UnknownVarietyExample _identity;

        private UnknownVarietyExample _built;

        private UnknownVarietyExample _previous;

        private MutableThing _root;

        private Mutable(UnknownVarietyExample src, Mutables mutables, MutableThing root) {
            _identity = src.identity();
            _root = root;
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
        public void imprint(UnknownVarietyExample value, Mutables mutables) {
            boolean changed = false;
            if (changed) mutables.flagChanged(this);
        }

        @Override
        public UnknownVarietyExample build() {
            if (_built != null) return _built;
            _built = builder.build();
            return _built;
        }

        @Override
        public UnknownVarietyExample identity() {
            return _identity;
        }

        @Override
        public UnknownVarietyExample previous() {
            UnknownVarietyExample v = _previous;
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
