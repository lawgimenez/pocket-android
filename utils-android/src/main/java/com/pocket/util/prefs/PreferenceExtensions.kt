package com.pocket.util.prefs

import kotlin.reflect.KProperty

operator fun BooleanPreference.getValue(owner: Any?, property: KProperty<*>) = get()
operator fun BooleanPreference.setValue(owner: Any?, property: KProperty<*>, b: Boolean) = set(b)

operator fun <E : Enum<E>> EnumPreference<E>.getValue(owner: Any?, property: KProperty<*>): E? {
    return get()
}
operator fun <E : Enum<E>> EnumPreference<E>.setValue(owner: Any?, property: KProperty<*>, e: E?) {
    set(e)
}

operator fun FloatPreference.getValue(owner: Any?, property: KProperty<*>) = get()
operator fun FloatPreference.setValue(owner: Any?, property: KProperty<*>, f: Float) = set(f)

operator fun IntPreference.getValue(owner: Any?, property: KProperty<*>) = get()
operator fun IntPreference.setValue(owner: Any?, property: KProperty<*>, i: Int) = set(i)

operator fun LongPreference.getValue(owner: Any?, property: KProperty<*>) = get()
operator fun LongPreference.setValue(owner: Any?, property: KProperty<*>, l: Long) = set(l)

operator fun StringPreference.getValue(owner: Any?, property: KProperty<*>): String? = get()
operator fun StringPreference.setValue(owner: Any?, property: KProperty<*>, s: String?) = set(s)

operator fun StringSetPreference.getValue(owner: Any?, property: KProperty<*>): Set<String?>? {
    return get()
}
operator fun StringSetPreference.setValue(owner: Any?, property: KProperty<*>, ss: Set<String?>?) {
    set(ss)
}
