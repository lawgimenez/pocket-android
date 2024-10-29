package com.pocket.sync.examples.generated;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.action.Action;
import com.pocket.sync.examples.generated.action.ActionExample;
import com.pocket.sync.examples.generated.action.ExampleMutation;
import com.pocket.sync.examples.generated.action.NoEffectAction;
import com.pocket.sync.examples.generated.action.ResolvedAction;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Spec;
import java.lang.Override;

/**
 * A helper class for implementing Spec.actions().In your Spec, create an final instance of this class and return it from Spec.actions().It will have a method for each action, that returns a builder for that action.Each method will also have a java doc about that action.
 */
public class ExamplesActions implements Spec.Actions {
    /**
     * An example action with all of the syntax features
     * {@link ActionExample} Description
     */
    public ActionExample.Builder action_example() {
        return new ActionExample.Builder();
    }

    public ExampleMutation.Builder exampleMutation() {
        return new ExampleMutation.Builder();
    }

    /**
     * An example action that doesn't declare an effect
     */
    public NoEffectAction.Builder no_effect_action() {
        return new NoEffectAction.Builder();
    }

    /**
     * An example action that resolves to Thing
     */
    public ResolvedAction.Builder resolved_action() {
        return new ResolvedAction.Builder();
    }

    @Override
    public Action action(ObjectNode json, JsonConfig _config) {
        switch (json.get("action").asText()) {
            case ActionExample.ACTION_NAME: return ActionExample.from(json, _config);
            case ExampleMutation.ACTION_NAME: return ExampleMutation.from(json, _config);
            case NoEffectAction.ACTION_NAME: return NoEffectAction.from(json, _config);
            case ResolvedAction.ACTION_NAME: return ResolvedAction.from(json, _config);
            default: return null;
        }
    }
}
