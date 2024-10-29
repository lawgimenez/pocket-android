package com.pocket.sync.examples.generated;

import com.pocket.sync.action.Action;
import com.pocket.sync.examples.generated.action.ActionExample;
import com.pocket.sync.examples.generated.action.ExampleMutation;
import com.pocket.sync.examples.generated.action.NoEffectAction;
import com.pocket.sync.examples.generated.action.ResolvedAction;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Applier;

/**
 * A helper class for applying actions in a spec. In your spec create an implementation of this class and delegate your spec's apply() method to this classes apply() method.Then this class provides a method for every action, just override them to write your implementation.Actions that have an effect defined in the schema will be abstract and must be overridden.Actions that don't have an effect will have a default implementation that doesn't do anything, but can be overridden.
 */
public abstract class ExamplesApplier implements Applier {
    private ExamplesBaseSpec spec;

    protected void setSpec(ExamplesBaseSpec spec) {
        if (this.spec != null) throw new RuntimeException("Spec should be effectively final. Cannot be changed.");
        this.spec = spec;
    }

    protected ExamplesBaseSpec spec() {
        return spec;
    }

    /**
     * An example action with all of the syntax features
     * {@link ActionExample} Description
     *
     * <h3>Effects</h3>
     * <pre>
     * ~ Effect
     * </pre>
     */
    protected abstract void action_example(ActionExample action, Space space);

    /**
     *
     * <h3>Effects</h3>
     * <pre>
     * </pre>
     */
    protected void exampleMutation(ExampleMutation action, Space space) {
    }

    /**
     * An example action that doesn't declare an effect
     *
     * <h3>Effects</h3>
     * <pre>
     * </pre>
     */
    protected void no_effect_action(NoEffectAction action, Space space) {
    }

    /**
     * An example action that resolves to Thing
     *
     * <h3>Effects</h3>
     * <pre>
     * </pre>
     */
    protected void resolved_action(ResolvedAction action, Space space) {
    }

    public void apply(Action action, Space space) {
        switch (action.action()) {
            case ActionExample.ACTION_NAME: action_example((ActionExample) action, space); break;
            case ExampleMutation.ACTION_NAME: exampleMutation((ExampleMutation) action, space); break;
            case NoEffectAction.ACTION_NAME: no_effect_action((NoEffectAction) action, space); break;
            case ResolvedAction.ACTION_NAME: resolved_action((ResolvedAction) action, space); break;
            default: unknown(action, space); break;
        }
    }

    /**
     * This action doesn't match any known actions and was not routed to a specific method.
     */
    protected void unknown(Action action, Space space) {
    }
}
