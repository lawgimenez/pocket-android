package com.pocket.util.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RegisterReceiverFlags
import androidx.fragment.app.Fragment
import kotlin.properties.ReadOnlyProperty

fun Context.registerReceiverCompat(
    receiver: BroadcastReceiver?,
    filter: IntentFilter,
    @RegisterReceiverFlags flags: Int,
): Intent? {
    return ContextCompat.registerReceiver(this, receiver, filter, flags)
}

fun <E: Enum<E>> Bundle.putEnum(key: String, value: Enum<E>) {
    putString(key, value.toString())
}
inline fun <reified E: Enum<E>> enumArg(key: String) = ReadOnlyProperty<Fragment, E> { thisRef, _ ->
    val name = thisRef.requireArguments().getString(key)!!
    enumValueOf<E>(name)
}

fun stringArg(key: String) = ReadOnlyProperty<Fragment, String> { thisRef, _ ->
    thisRef.requireArguments().getString(key)!!
}
