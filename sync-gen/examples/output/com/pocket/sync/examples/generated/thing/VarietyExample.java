package com.pocket.sync.examples.generated.thing;

import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.OpenParser;
import com.pocket.sync.value.Variety;

public interface VarietyExample extends Thing, Variety {
    OpenParser<VarietyExample> VARIETY_VARIETYEXAMPLE_CREATOR = new OpenParser<VarietyExample>(Modeller.THINGS, Modeller.OBJECT_MAPPER, "UnknownVarietyExample");
}
