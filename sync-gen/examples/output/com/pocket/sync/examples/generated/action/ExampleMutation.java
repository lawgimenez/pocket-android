package com.pocket.sync.examples.generated.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sync.action.Action;
import com.pocket.sync.action.ActionBuilder;
import com.pocket.sync.action.ActionResolved;
import com.pocket.sync.action.Time;
import com.pocket.sync.examples.generated.ExamplesAuthType;
import com.pocket.sync.examples.generated.ExamplesRemoteStyle;
import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.examples.generated.thing.NonIdentifiable;
import com.pocket.sync.examples.generated.thing.ThingExample;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.Remote.Method;
import com.pocket.sync.source.protocol.graphql.GraphQlSupport;
import com.pocket.sync.source.protocol.graphql.GraphQlSyncable;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.thing.ThingUtil;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.SyncableParser;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExampleMutation implements GraphQlSyncable, BaseAction, Action {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<ExampleMutation> JSON_CREATOR = ExampleMutation::from;

    public static final Remote REMOTE = new Remote(null, Remote.Method.GET, ExamplesRemoteStyle.CLIENT_API, null);

    public static final String ACTION_NAME = "exampleMutation";

    public static final RemotePriority PRIORITY = RemotePriority.WHENEVER;

    private static final ActionResolved<ThingExample> RESOLVED = new ActionResolved<>(ThingExample.JSON_CREATOR, ThingExample.STREAMING_JSON_CREATOR);

    /**
     * When the action occurred.
     */
    @Nullable
    public final Timestamp time;

    @Nullable
    public final NonIdentifiable variable;

    public final Declared declared;

    private ExampleMutation(Builder builder, Declared declared) {
        this.declared = declared;
        this.time = builder.time;
        this.variable = builder.variable;
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

    @Override
    public Timestamp _time() {
        return time;
    }

    public static ExampleMutation from(JsonNode jsonNode, JsonConfig _config, Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("time");
        if (value != null) builder.time(Modeller.asTimestamp(value));
        value = json.get("variable");
        if (value != null) builder.variable(NonIdentifiable.from(value, _config, allowed));
        return builder.build();
    }

    @Override
    public ObjectNode toJson(JsonConfig _config, Include... includes) {
        ObjectNode json = Modeller.OBJECT_MAPPER.createObjectNode();
        if (Include.contains(includes, Include.OPEN_TYPE)) {
            json.put(Thing.SERIALIZATION_TYPE_KEY, ACTION_NAME);
            includes = Include.removeAssumingPresent(includes, Include.OPEN_TYPE);
        }
        if (declared.time) json.put("time", Modeller.toJsonValue(time));
        if (declared.variable) json.put("variable", Modeller.toJsonValue(variable, _config, includes));
        json.put("action", "exampleMutation");
        return json;
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
        if (declared.time) map.put("time", time);
        if (declared.variable) map.put("variable", variable);
        map.put("action", "exampleMutation");
        return map;
    }

    @Override
    public String toString() {
        return toJson(new JsonConfig(REMOTE.style, true), Include.OPEN_TYPE).toString();
    }

    @Override
    public Builder builder() {
        return new Builder(this);
    }

    @Override
    public Remote remote() {
        return REMOTE;
    }

    @Override
    public RemotePriority priority() {
        return PRIORITY;
    }

    @Override
    public String action() {
        return ACTION_NAME;
    }

    @Override
    public Time time() {
        return time;
    }

    @Override
    public int hashCode() {
        int _result = 0;
        Thing.Equality e = Thing.Equality.STATE;
        _result = 31 * _result + (time != null ? time.hashCode() : 0);
        _result = 31 * _result + ThingUtil.fieldHashCode(e, variable);
        return _result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExampleMutation that = (ExampleMutation) o;
        Thing.Equality e = Thing.Equality.STATE;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (!ThingUtil.fieldEquals(e, variable, that.variable)) return false;
        return true;
    }

    @Override
    public ActionResolved<ThingExample> resolved() {
        return RESOLVED;
    }

    private static class GraphQl implements GraphQlSupport {
        @Language("GraphQL")
        @Nullable
        @Override
        public String operation() {
            return "mutation exampleMutation($variable: NonIdentifiable) {\n"
                            + "  inputs\n"
                            + "}\n"
                            + "\n";
        }
    }

    private static class DeclaredMutable {
        private boolean time;

        private boolean variable;
    }

    public static class Declared {
        public final boolean time;

        public final boolean variable;

        private Declared(DeclaredMutable declared) {
            this.time = declared.time;
            this.variable = declared.variable;
        }
    }

    public static class Builder implements ActionBuilder<ExampleMutation> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected Timestamp time;

        protected NonIdentifiable variable;

        public Builder() {
        }

        public Builder(ExampleMutation src) {
            set(src);
        }

        @Override
        public Builder set(ExampleMutation src) {
            if (src.declared.time) {
                declared.time = true;
                this.time = src.time;
            }
            if (src.declared.variable) {
                declared.variable = true;
                this.variable = src.variable;
            }
            return this;
        }

        /**
         * When the action occurred.
         */
        public Builder time(Timestamp value) {
            declared.time = true;
            this.time = Modeller.immutable(value);
            return this;
        }

        public Builder variable(NonIdentifiable value) {
            declared.variable = true;
            this.variable = Modeller.immutable(value);
            return this;
        }

        @Override
        public ExampleMutation build() {
            return new ExampleMutation(this, new Declared(declared));
        }
    }
}
