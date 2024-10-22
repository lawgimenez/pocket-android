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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An example action with all of the syntax features
 * {@link ActionExample} Description
 */
public class ActionExample implements GraphQlSyncable, BaseAction, Action {
    public static GraphQlSupport GRAPHQL = new GraphQl();

    public static final SyncableParser<ActionExample> JSON_CREATOR = ActionExample::from;

    public static final Remote REMOTE = new Remote("endpoint/{.input}", Remote.Method.GET, ExamplesRemoteStyle.REMOTE, null);

    public static final String ACTION_NAME = "action_example";

    public static final RemotePriority PRIORITY = RemotePriority.SOON;

    /**
     * When the action occurred.
     */
    @Nullable
    public final Timestamp time;

    /**
     * {@link ActionExample#input} Description
     */
    @Nullable
    public final String input;

    @Nullable
    public final ThingExample input_thing;

    public final Declared declared;

    private ActionExample(Builder builder, Declared declared) {
        this.declared = declared;
        this.time = builder.time;
        this.input = builder.input;
        this.input_thing = builder.input_thing;
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

    @Override
    public Timestamp _time() {
        return time;
    }

    public static ActionExample from(JsonNode jsonNode, JsonConfig _config, Allow... allowed) {
        if (jsonNode == null || jsonNode.isNull()) return null;
        ObjectNode json = jsonNode.deepCopy();
        Builder builder = new Builder();
        JsonNode value;
        value = json.get("time");
        if (value != null) builder.time(Modeller.asTimestamp(value));
        value = json.get("input");
        if (value != null) builder.input(Modeller.asString(value));
        value = json.get("input_thing");
        if (value != null) builder.input_thing(ThingExample.from(value, _config, allowed));
        return builder.build();
    }

    @Override
    public ObjectNode toJson(JsonConfig _config, Include... includes) {
        ObjectNode json = Modeller.OBJECT_MAPPER.createObjectNode();
        if (Include.contains(includes, Include.OPEN_TYPE)) {
            json.put(Thing.SERIALIZATION_TYPE_KEY, ACTION_NAME);
            includes = Include.removeAssumingPresent(includes, Include.OPEN_TYPE);
        }
        if (declared.input) json.put("input", Modeller.toJsonValue(input));
        if (declared.input_thing) json.put("input_thing", Modeller.toJsonValue(input_thing, _config, includes));
        if (declared.time) json.put("time", Modeller.toJsonValue(time));
        json.put("action", "action_example");
        return json;
    }

    @Override
    public Map<String, Object> toMap(Include... includes) {
        Map<String, Object> map = new HashMap<String, Object>();
        boolean includeDangerous = ArrayUtils.contains(includes, Include.DANGEROUS);
        if (declared.time) map.put("time", time);
        if (declared.input) map.put("input", input);
        if (declared.input_thing) map.put("input_thing", input_thing);
        map.put("action", "action_example");
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
        _result = 31 * _result + ThingUtil.fieldHashCode(e, input_thing);
        return _result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionExample that = (ActionExample) o;
        Thing.Equality e = Thing.Equality.STATE;
        if (time != null ? !time.equals(that.time) : that.time != null) return false;
        if (input != null ? !input.equals(that.input) : that.input != null) return false;
        if (!ThingUtil.fieldEquals(e, input_thing, that.input_thing)) return false;
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

        private boolean input_thing;
    }

    public static class Declared {
        public final boolean time;

        public final boolean input;

        public final boolean input_thing;

        private Declared(DeclaredMutable declared) {
            this.time = declared.time;
            this.input = declared.input;
            this.input_thing = declared.input_thing;
        }
    }

    public static class Builder implements ActionBuilder<ActionExample> {
        private DeclaredMutable declared = new DeclaredMutable();

        protected Timestamp time;

        protected String input;

        protected ThingExample input_thing;

        public Builder() {
        }

        public Builder(ActionExample src) {
            set(src);
        }

        @Override
        public Builder set(ActionExample src) {
            if (src.declared.time) {
                declared.time = true;
                this.time = src.time;
            }
            if (src.declared.input) {
                declared.input = true;
                this.input = src.input;
            }
            if (src.declared.input_thing) {
                declared.input_thing = true;
                this.input_thing = src.input_thing;
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

        /**
         * {@link ActionExample#input} Description
         */
        public Builder input(String value) {
            declared.input = true;
            this.input = Modeller.immutable(value);
            return this;
        }

        public Builder input_thing(ThingExample value) {
            declared.input_thing = true;
            this.input_thing = Modeller.immutable(value);
            return this;
        }

        @Override
        public ActionExample build() {
            return new ActionExample(this, new Declared(declared));
        }
    }
}
