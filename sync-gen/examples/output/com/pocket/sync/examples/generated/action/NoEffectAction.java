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
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.Remote.Method;
import com.pocket.sync.source.protocol.graphql.GraphQlSupport;
import com.pocket.sync.source.protocol.graphql.GraphQlSyncable;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.SyncableParser;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An example action that doesn't declare an effect
 */
public class NoEffectAction implements GraphQlSyncable, BaseAction, Action {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<NoEffectAction> JSON_CREATOR = NoEffectAction::from;

    public static final Remote REMOTE = new Remote(null, Remote.Method.GET, ExamplesRemoteStyle.REMOTE, null);

    public static final String ACTION_NAME = "no_effect_action";

    public static final RemotePriority PRIORITY = RemotePriority.WHENEVER;

    /**
     * When the action occurred.
     */
    @Nullable
    public final Timestamp time;

    @Nullable
    public final String input;

    public final Declared declared;

    private NoEffectAction(Builder builder, Declared declared) {
        this.declared = declared;
        this.time = builder.time;
        this.input = builder.input;
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

    public static NoEffectAction from(JsonNode jsonNode, JsonConfig _config, Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("time");
        if (value != null) builder.time(Modeller.asTimestamp(value));
        value = json.get("input");
        if (value != null) builder.input(Modeller.asString(value));
        return builder.build();
    }

    @Override
    public ObjectNode toJson(JsonConfig _config, Include... includes) {
        ObjectNode json = Modeller.OBJECT_MAPPER.createObjectNode();
        if (Include.contains(includes, Include.OPEN_TYPE)) {
            json.put(Thing.SERIALIZATION_TYPE_KEY, ACTION_NAME);
        }
        if (declared.input) json.put("input", Modeller.toJsonValue(input));
        if (declared.time) json.put("time", Modeller.toJsonValue(time));
        json.put("action", "no_effect_action");
        return json;
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
        if (declared.time) map.put("time", time);
        if (declared.input) map.put("input", input);
        map.put("action", "no_effect_action");
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
        _result = 31 * _result + (input != null ? input.hashCode() : 0);
        return _result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoEffectAction that = (NoEffectAction) o;
        Thing.Equality e = Thing.Equality.STATE;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (input != null ? !input.equals(that.input) : that.input != null) return false;
        return true;
    }

    @Override
    public ActionResolved resolved() {
        return null;
    }

    private static class GraphQl implements GraphQlSupport {
        @Nullable
        @Override
        public String operation() {
            return null;
        }
    }

    private static class DeclaredMutable {
        private boolean time;

        private boolean input;
    }

    public static class Declared {
        public final boolean time;

        public final boolean input;

        private Declared(DeclaredMutable declared) {
            this.time = declared.time;
            this.input = declared.input;
        }
    }

    public static class Builder implements ActionBuilder<NoEffectAction> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected Timestamp time;

        protected String input;

        public Builder() {
        }

        public Builder(NoEffectAction src) {
            set(src);
        }

        @Override
        public Builder set(NoEffectAction src) {
            if (src.declared.time) {
                declared.time = true;
                this.time = src.time;
            }
            if (src.declared.input) {
                declared.input = true;
                this.input = src.input;
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

        public Builder input(String value) {
            declared.input = true;
            this.input = Modeller.immutable(value);
            return this;
        }

        @Override
        public NoEffectAction build() {
            return new NoEffectAction(this, new Declared(declared));
        }
    }
}
