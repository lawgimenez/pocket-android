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
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExampleQuery implements GraphQlSyncable, Thing {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<ExampleQuery> JSON_CREATOR = ExampleQuery::from;

    public static final StreamingThingParser<ExampleQuery> STREAMING_JSON_CREATOR = ExampleQuery::from;

    public static final String THING_TYPE = "exampleQuery";

    public static final Remote REMOTE = new Remote(null, Remote.Method.GET, ExamplesRemoteStyle.CLIENT_API, null);

    public static final ByteTypeParser<ExampleQuery> BYTE_CREATOR = ExampleQuery::uncompress;

    @Nullable
    public final NonIdentifiable input;

    @Nullable
    public final ThingExample example;

    public final Declared declared;

    private ExampleQuery _identity;

    /**
     * Lazy init'd and cached during idkey()
     */
    private String _idkey;

    private ExampleQuery(Builder builder, Declared declared) {
        this.declared = declared;
        this.input = builder.input;
        this.example = builder.example;
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

    public static ExampleQuery from(JsonNode jsonNode, JsonConfig _config, Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("input");
        if (value != null) builder.input(NonIdentifiable.from(value, _config, allowed));
        value = json.get("example");
        if (value != null) builder.example(ThingExample.from(value, _config, allowed));
        return builder.build();
    }

    public static ExampleQuery from(JsonParser parser, JsonConfig _config, Allow... allowed) throws
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
            } else if (currentName.equals("input")) {
                builder.input(NonIdentifiable.from(parser, _config, allowed));
            } else if (currentName.equals("example")) {
                builder.example(ThingExample.from(parser, _config, allowed));
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
        if (declared.example) json.put("example", Modeller.toJsonValue(example, _config, includes));
        if (declared.input) json.put("input", Modeller.toJsonValue(input, _config, includes));
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
    public ExampleQuery identity() {
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
        return "example";
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
        if (declared.input) map.put("input", input);
        if (declared.example) map.put("example", example);
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
        _result = 31 * _result + ThingUtil.fieldHashCode(e, input);
        if (e == Thing.Equality.IDENTITY) return _result;
        _result = 31 * _result + ThingUtil.fieldHashCode(e, example);
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
        ExampleQuery that = (ExampleQuery) o;
        if (e == Thing.Equality.STATE_DECLARED) {
            if (that.declared.input && declared.input) if (!ThingUtil.fieldEquals(e, input, that.input)) return false;
            if (that.declared.example && declared.example) if (!ThingUtil.fieldEquals(e, example, that.example)) return false;
            return true;
        }
        if (!ThingUtil.fieldEquals(e, input, that.input)) return false;
        if (e == Thing.Equality.IDENTITY) return true;
        if (!ThingUtil.fieldEquals(e, example, that.example)) return false;
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
    }

    @Override
    public void subthings(FlatUtils.Output out) {
        if (example != null) out.add(example, true);
    }

    @Override
    public ExampleQuery flat() {
        Builder builder = builder();
        if (example != null) builder.example(example.identity());
        return builder.build();
    }

    @Override
    public ExampleQuery with(Changes.ThingMatch match, Thing replace) {
        Object _replaced;
        _replaced = BaseModeller.with(example, match, replace, true);
        if (_replaced != null) return new Builder(this).example((ThingExample) _replaced).build();
        return null;
    }

    @Override
    public ExampleQuery redact(StringEncrypter e) {
        Builder _builder = builder();
        if (example != null) _builder.example(Modeller.redact(example, e));
        return _builder.build();
    }

    @Override
    public ExampleQuery unredact(StringEncrypter e) {
        Builder _builder = builder();
        if (example != null) _builder.example(Modeller.unredact(example, e));
        return _builder.build();
    }

    @Override
    public void compress(ByteWriter out) {
        out.writeInt(2);
        if (out.writeBit(declared.input)) {
            out.writeBit(input != null);
        }
        if (out.writeBit(declared.example)) {
            out.writeBit(example != null);
        }
        out.finishByte();
        if (input != null) {
            input.compress(out);
        }
        if (example != null) {
            example.compress(out);
        }
    }

    public static ExampleQuery uncompress(ByteReader _in) {
        Builder _builder = new Builder();
        int _fields = _in.readInt();
        boolean _read_input = false;
        boolean _read_example = false;
        _fields_break :  {
            if (0 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_input = _in.readBit())) _builder.input(null);
            if (1 >= _fields) break _fields_break;
            if (_in.readBit()) if (!(_read_example = _in.readBit())) _builder.example(null);
        }
        _in.finishByte();
        if (_read_input) _builder.input(NonIdentifiable.uncompress(_in));
        if (_read_example) _builder.example(ThingExample.uncompress(_in));
        return _builder.build();
    }

    private static class GraphQl implements GraphQlSupport {
        @Language("GraphQL")
        @Nullable
        @Override
        public String operation() {
            return "query exampleQuery($input: NonIdentifiable) {\n"
                            + "  example\n"
                            + "}\n"
                            + "\n";
        }
    }

    private static class DeclaredMutable {
        private boolean input;

        private boolean example;
    }

    public static class Declared {
        public final boolean input;

        public final boolean example;

        private Declared(DeclaredMutable declared) {
            this.input = declared.input;
            this.example = declared.example;
        }
    }

    public static class Builder implements ThingBuilder<ExampleQuery> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected NonIdentifiable input;

        protected ThingExample example;

        public Builder() {
        }

        public Builder(ExampleQuery src) {
            set(src);
        }

        @Override
        public Builder set(ExampleQuery src) {
            if (src.declared.input) {
                declared.input = true;
                this.input = src.input;
            }
            if (src.declared.example) {
                declared.example = true;
                this.example = src.example;
            }
            return this;
        }

        public Builder input(NonIdentifiable value) {
            declared.input = true;
            this.input = Modeller.immutable(value);
            return this;
        }

        public Builder example(ThingExample value) {
            declared.example = true;
            this.example = Modeller.immutable(value);
            return this;
        }

        @Override
        public ExampleQuery build() {
            return new ExampleQuery(this, new Declared(declared));
        }
    }

    public static class IdBuilder implements ThingBuilder<ExampleQuery> {
        private final Builder builder = new Builder();

        public IdBuilder() {
        }

        public IdBuilder(ExampleQuery src) {
            set(src);
        }

        @Override
        public IdBuilder set(ExampleQuery src) {
            if (src.declared.input) {
                builder.declared.input = true;
                builder.input = src.input;
            }
            return this;
        }

        public IdBuilder input(NonIdentifiable value) {
            builder.input(value);
            return this;
        }

        @Override
        public ExampleQuery build() {
            return new ExampleQuery(builder, new Declared(builder.declared));
        }
    }

    public static class Mutable implements MutableThing<ExampleQuery> {
        private final Builder builder = new Builder();

        private final ExampleQuery _identity;

        private ExampleQuery _built;

        private ExampleQuery _previous;

        private MutableThing _root;

        private MutableThing<ThingExample> example;

        private Mutable(ExampleQuery src, Mutables mutables) {
            _identity = src.identity();
            _root = this;
            if (src.declared.input)  {
                builder.declared.input = true;
                builder.input = src.input;
            }
            if (src.declared.example)  {
                builder.declared.example = true;
                example = mutables.imprint(src.example, _root);
                mutables.link(this, example);
            }
        }

        @Override
        public Collection<? extends MutableThing> references() {
            List<MutableThing> _out = new ArrayList<>();
            if (example != null) _out.add(example);
            return _out;
        }

        @Override
        public MutableThing root() {
            return _root;
        }

        @Override
        public void imprint(ExampleQuery value, Mutables mutables) {
            boolean changed = false;
            if (value.declared.input) {
                builder.declared.input = true;
                changed = changed || Mutables.changed(builder.input, value.input);
                builder.input = value.input;
            }
            if (value.declared.example) {
                builder.declared.example = true;
                changed = changed || Mutables.changed(example, value.example);
                if (changed) mutables.unlink(this, example);
                example = mutables.imprint(value.example, _root);
                if (changed) mutables.link(this, example);
            }
            if (changed) mutables.flagChanged(this);
        }

        @Override
        public ExampleQuery build() {
            if (_built != null) return _built;
            builder.example = Mutables.build(example);
            _built = builder.build();
            return _built;
        }

        @Override
        public ExampleQuery identity() {
            return _identity;
        }

        @Override
        public ExampleQuery previous() {
            ExampleQuery v = _previous;
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
