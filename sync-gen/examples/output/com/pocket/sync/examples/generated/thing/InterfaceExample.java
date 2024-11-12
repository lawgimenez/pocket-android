package com.pocket.sync.examples.generated.thing;

import com.pocket.sync.examples.generated.Modeller;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.OpenParser;
import java.lang.Override;
import java.lang.String;

public interface InterfaceExample extends Thing {
    OpenParser<InterfaceExample> INTERFACE_INTERFACEEXAMPLE_CREATOR = new OpenParser<InterfaceExample>(Modeller.THINGS, Modeller.OBJECT_MAPPER, "UnknownInterfaceExample");

    @Override
    InterfaceExample flat();

    String _dangerous();
}
