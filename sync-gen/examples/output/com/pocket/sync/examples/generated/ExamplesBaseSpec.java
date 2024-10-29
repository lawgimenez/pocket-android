package com.pocket.sync.examples.generated;

import com.pocket.sync.action.Action;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Spec;
import java.lang.Override;

/**
 * A helper for building a {@link Spec} for Examples. 
 * It contains everything that was generated from schema, but just needs implementations of how to apply actions and derive fields.
 * To use, create your spec as subclass that passes in implementations for {@link ExamplesDerives} and {@link ExamplesApplier} into the constructor.
 */
public class ExamplesBaseSpec implements Spec {
    ExamplesThings things = new ExamplesThings();

    ExamplesActions actions = new ExamplesActions();

    ExamplesDerives deriver;

    ExamplesApplier applier;

    protected ExamplesBaseSpec(ExamplesDerives deriver, ExamplesApplier applier) {
        this.deriver = deriver;
        this.applier = applier;
        this.deriver.setSpec(this);
        this.applier.setSpec(this);
    }

    @Override
    public ExamplesThings things() {
        return things;
    }

    @Override
    public ExamplesActions actions() {
        return actions;
    }

    @Override
    public ExamplesDerives derive() {
        return deriver;
    }

    @Override
    public void apply(Action action, Space space) {
        applier.apply(action, space);
    }
}
