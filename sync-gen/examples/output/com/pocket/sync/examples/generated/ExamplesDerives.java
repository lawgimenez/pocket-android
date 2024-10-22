package com.pocket.sync.examples.generated;

import com.pocket.sync.examples.generated.thing.ThingExample;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Space;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import java.lang.Override;
import java.lang.String;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Creates an abstract structure for handling each derived Thing as its own method.
 * Subclass and implement the abstract methods.
 * Then point your {@link Spec} to this classes' {@link #rederive(Thing, Collection, Diff, Space.Selector)} method.
 */
public abstract class ExamplesDerives implements Spec.Derive {
    private ExamplesBaseSpec spec;

    private final Set<String> fullDeriveSupport = new HashSet<>();

    /**
     * @param fullDeriveSupport A list of thing types that this implementation supports fully deriving (vs just re-deriving)
     */
    protected ExamplesDerives(String... fullDeriveSupport) {
        Collections.addAll(this.fullDeriveSupport, fullDeriveSupport);
    }

    protected void setSpec(ExamplesBaseSpec spec) {
        if (this.spec != null) throw new RuntimeException("Spec should be effectively final. Cannot be changed.");
        this.spec = spec;
    }

    protected ExamplesBaseSpec spec() {
        return spec;
    }

    @Override
    public <T extends Thing> T derive(T thing, Space.Selector selector) {
        if (fullDeriveSupport.contains(thing.type())) {
            return rederive(thing, null, null, selector);
        } else {
            return null;
        }
    }

    @Override
    public <T extends Thing> T rederive(T thing, Collection<String> reactions, Diff diff,
            Space.Selector selector) {
        switch (thing.type()) {
            case ThingExample.THING_TYPE: return (T)thingExample((ThingExample) thing, reactions, diff, selector);
        }
        return thing;
    }

    /**
     * <pre>
     * ~ Do something
     * </pre>
     */
    public String derive__ThingExample__reactive_collection_field(ThingExample t, Diff diff,
            Space.Selector selector) {
        return t.reactive_collection_field;
    }

    /**
     * <pre>
     * ~ Do something
     * </pre>
     */
    public String derive__ThingExample__reactive_to_sibling_field(ThingExample t, Diff diff,
            Space.Selector selector) {
        return t.reactive_to_sibling_field;
    }

    /**
     * <pre>
     * ~ Do something
     * </pre>
     */
    public String derive__ThingExample__reactive_to_type(ThingExample t, Diff diff,
            Space.Selector selector) {
        return t.reactive_to_type;
    }

    /**
     * <pre>
     * ~ Do something
     * </pre>
     */
    public String derive__ThingExample__reactive_to_type_field(ThingExample t, Diff diff,
            Space.Selector selector) {
        return t.reactive_to_type_field;
    }

    /**
     * <pre>
     * ~ Do something
     * </pre>
     */
    public String derive__ThingExample__reactive_to_any_sibling_field(ThingExample t, Diff diff,
            Space.Selector selector) {
        return t.reactive_to_any_sibling_field;
    }

    public ThingExample thingExample(ThingExample t, Collection<String> reactions, Diff diff,
            Space.Selector selector) {
        ThingExample.Builder b = new ThingExample.Builder(t);
        if (!t.declared.derived_first_available || reactions == null || reactions.contains("derived_first_available")) {
            ThingExample.Derive.derived_first_available(b);
        }
        if (!t.declared.id_derived || reactions == null || reactions.contains("id_derived")) {
            ThingExample.Derive.id_derived(b);
        }
        if (!t.declared.reactive_collection_field || reactions == null || reactions.contains("reactive_collection_field")) {
            String v = derive__ThingExample__reactive_collection_field(t, diff, selector);
            b.reactive_collection_field(v);
        }
        if (!t.declared.reactive_to_sibling_field || reactions == null || reactions.contains("reactive_to_sibling_field")) {
            String v = derive__ThingExample__reactive_to_sibling_field(t, diff, selector);
            b.reactive_to_sibling_field(v);
        }
        if (!t.declared.reactive_to_type || reactions == null || reactions.contains("reactive_to_type")) {
            String v = derive__ThingExample__reactive_to_type(t, diff, selector);
            b.reactive_to_type(v);
        }
        if (!t.declared.reactive_to_type_field || reactions == null || reactions.contains("reactive_to_type_field")) {
            String v = derive__ThingExample__reactive_to_type_field(t, diff, selector);
            b.reactive_to_type_field(v);
        }
        if (!t.declared.reactive_to_any_sibling_field || reactions == null || reactions.contains("reactive_to_any_sibling_field")) {
            String v = derive__ThingExample__reactive_to_any_sibling_field(t, diff, selector);
            b.reactive_to_any_sibling_field(v);
        }
        return b != null ? b.build() : t;
    }
}
