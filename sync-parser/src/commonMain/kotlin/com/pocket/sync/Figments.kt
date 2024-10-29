package com.pocket.sync

import com.pocket.sync.parse.FigmentsData
import com.pocket.sync.parse.resolve
import com.pocket.sync.type.*
import com.pocket.sync.type.Enum


/**
 * Helpers and tools for working with definitions
 *
 * All query methods return immutable lists, sorted by [Definition.name] or some other logical sort.
 */
class Figments(val data: FigmentsData) {

    private val definitions = data.resolve(this).sortedByName()

    fun all() = definitions
    fun things() = definitions.filterIsInstance<Thing>()
    fun actions() = definitions.filterIsInstance<Action>()
    fun syncables() = definitions.filterIsInstance<Syncable<*>>()
    fun values() = definitions.filterIsInstance<Value>()
    fun enums() = definitions.filterIsInstance<Enum>()
    fun remotes() = definitions.filterIsInstance<Remote>()
    fun auths() = definitions.filterIsInstance<Auth>()
    fun varieties() = definitions.filterIsInstance<Variety>()

    fun baseAction() : Action? = definitions.find { it is Action && it.isBase && it.remoteBaseOf == null } as Action?
    fun remoteBases() = remotes().mapNotNull { it.baseAction }.sortedBy { it.remoteBaseOf?.name }
    fun defaultRemote() : Remote? = definitions.find { it is Remote && it.default } as Remote?
    fun defaultAuth() : Auth? = definitions.find { it is Auth && it.default } as Auth?

    /** @return Things that are reactive to something */
    fun reactives() : List<Thing> = things().filter { it.fields.all.find { f -> f.derives.reactiveTo.isNotEmpty() } != null }
    fun endpoints() : List<Syncable<*>> = syncables().filter { it.endpoint.merged != null }

    /** Returns the definition matching this name or throws an exception if not found. Also see [find] */
    fun <D : Definition> get(name: String) : D = find<D>(name) ?: throw RuntimeException("No definition matched the name `$name`")

    /** Returns the definition matching this name or null if not found. Also see [get] */
    fun <D : Definition> find(name: String) : D? = definitions.find { it.name == name } as D

    fun <D : Definition> Collection<D>.sortedByName() = sortedBy { it.name }


}