package com.pocket.sync.examples.generated;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.examples.generated.thing.ExampleQuery;
import com.pocket.sync.examples.generated.thing.HasDangerousValue;
import com.pocket.sync.examples.generated.thing.IdentifiableByIdentifiableThing;
import com.pocket.sync.examples.generated.thing.IdentifiableByNonIdentifiableThing;
import com.pocket.sync.examples.generated.thing.IdentifiableByValue;
import com.pocket.sync.examples.generated.thing.NonIdentifiable;
import com.pocket.sync.examples.generated.thing.ThingExample;
import com.pocket.sync.examples.generated.thing.UnknownInterfaceExample;
import com.pocket.sync.examples.generated.thing.UnknownVarietyExample;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Spec;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.binary.ByteReader;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;

/**
 * A helper class for implementing Spec.things().In your Spec, create an final instance of this class and return it from Spec.things().It will have a method for each thing, that returns a builder for that thing.Each method will also have a java doc about that thing.
 */
public class ExamplesThings implements Spec.Things {
    public HasDangerousValue.Builder hasDangerousValue() {
        return new HasDangerousValue.Builder();
    }

    /**
     * Identifying fields are things that have identity themselves
     */
    public IdentifiableByIdentifiableThing.Builder identifiableByIdentifiableThing() {
        return new IdentifiableByIdentifiableThing.Builder();
    }

    /**
     * Identifying fields are things that are non identifiable
     */
    public IdentifiableByNonIdentifiableThing.Builder identifiableByNonIdentifiableThing() {
        return new IdentifiableByNonIdentifiableThing.Builder();
    }

    /**
     * Identifying fields are values
     */
    public IdentifiableByValue.Builder identifiableByValue() {
        return new IdentifiableByValue.Builder();
    }

    /**
     * Has no identifying fields
     */
    public NonIdentifiable.Builder nonIdentifiable() {
        return new NonIdentifiable.Builder();
    }

    /**
     * An example thing with all of the syntax features
     */
    public ThingExample.Builder thingExample() {
        return new ThingExample.Builder();
    }

    public UnknownInterfaceExample.Builder unknownInterfaceExample() {
        return new UnknownInterfaceExample.Builder();
    }

    public UnknownVarietyExample.Builder unknownVarietyExample() {
        return new UnknownVarietyExample.Builder();
    }

    public ExampleQuery.Builder exampleQuery() {
        return new ExampleQuery.Builder();
    }

    @Override
    public Thing thing(String type, ObjectNode json, JsonConfig _config) {
        switch (type) {
            case HasDangerousValue.THING_TYPE: return HasDangerousValue.from(json, _config);
            case IdentifiableByIdentifiableThing.THING_TYPE: return IdentifiableByIdentifiableThing.from(json, _config);
            case IdentifiableByNonIdentifiableThing.THING_TYPE: return IdentifiableByNonIdentifiableThing.from(json, _config);
            case IdentifiableByValue.THING_TYPE: return IdentifiableByValue.from(json, _config);
            case NonIdentifiable.THING_TYPE: return NonIdentifiable.from(json, _config);
            case ThingExample.THING_TYPE: return ThingExample.from(json, _config);
            case UnknownInterfaceExample.THING_TYPE: return UnknownInterfaceExample.from(json, _config);
            case UnknownVarietyExample.THING_TYPE: return UnknownVarietyExample.from(json, _config);
            case ExampleQuery.THING_TYPE: return ExampleQuery.from(json, _config);
            default: return null;
        }
    }

    @Override
    public Thing thing(String type, JsonParser parser, JsonConfig _config) throws IOException {
        switch (type) {
            case HasDangerousValue.THING_TYPE: return HasDangerousValue.from(parser, _config);
            case IdentifiableByIdentifiableThing.THING_TYPE: return IdentifiableByIdentifiableThing.from(parser, _config);
            case IdentifiableByNonIdentifiableThing.THING_TYPE: return IdentifiableByNonIdentifiableThing.from(parser, _config);
            case IdentifiableByValue.THING_TYPE: return IdentifiableByValue.from(parser, _config);
            case NonIdentifiable.THING_TYPE: return NonIdentifiable.from(parser, _config);
            case ThingExample.THING_TYPE: return ThingExample.from(parser, _config);
            case UnknownInterfaceExample.THING_TYPE: return UnknownInterfaceExample.from(parser, _config);
            case UnknownVarietyExample.THING_TYPE: return UnknownVarietyExample.from(parser, _config);
            case ExampleQuery.THING_TYPE: return ExampleQuery.from(parser, _config);
            default: return null;
        }
    }

    @Override
    public Thing thing(String type, ByteReader in) {
        switch (type) {
            case HasDangerousValue.THING_TYPE: return HasDangerousValue.uncompress(in);
            case IdentifiableByIdentifiableThing.THING_TYPE: return IdentifiableByIdentifiableThing.uncompress(in);
            case IdentifiableByNonIdentifiableThing.THING_TYPE: return IdentifiableByNonIdentifiableThing.uncompress(in);
            case IdentifiableByValue.THING_TYPE: return IdentifiableByValue.uncompress(in);
            case NonIdentifiable.THING_TYPE: return NonIdentifiable.uncompress(in);
            case ThingExample.THING_TYPE: return ThingExample.uncompress(in);
            case UnknownInterfaceExample.THING_TYPE: return UnknownInterfaceExample.uncompress(in);
            case UnknownVarietyExample.THING_TYPE: return UnknownVarietyExample.uncompress(in);
            case ExampleQuery.THING_TYPE: return ExampleQuery.uncompress(in);
            default: return null;
        }
    }
}
