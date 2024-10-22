package com.pocket.sync.value

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.pocket.sync.thing.Thing
import kotlin.properties.ReadOnlyProperty

fun Bundle.putThing(key: String, thing: Thing) {
    Parceller.put(this, key, thing)
}

fun <T : Thing> thingArg(
    key: String,
    creator: SyncableParser<T>,
) = ReadOnlyProperty<Fragment, T> { thisRef, _ ->
    Parceller.get(thisRef.requireArguments(), key, creator)
}
