package com.pocket.sync.usage

import com.pocket.sync.Figments
import com.pocket.sync.type.*

/**
 * NOTE: Looks like `sync-gen` doesn't differentiate between [NORMAL] and [COMPAT].
 * It just checks [SKIP] and skips generating code for these definitions/fields/etc.
 * 
 * So while looks like Max had an idea that for some definitions maybe we don't need full classes,
 * but some sort of "compat mode" that only makes sure we don't break restoring from disk
 * or something, this was either not possible or just never implemented.
 */
enum class UsageMode {
    /** Actively used and not deprecated. */
    NORMAL,
    /** Deprecated, but it was previously used according to the [UsageFile], so should still support it or support it in a compat mode. */
    COMPAT,
    /** Not used, can be safely excluded from your code generation. */
    SKIP
}


/**
 * Calculates what actually needs to be generated. Skips deprecated or unused definitions were possible or determines
 * when something only needs to be generated for backwards compatibility.
 * See Sync's Backwards Comparability section for more details.
 *
 * Also registers all definitions and aspects with the provided [UsageFile], but does not commit them. Use [commitUsageFile] to do so.
 */
class UsageModeCalculator(val figments: Figments, private val compat: UsageFile) {

    private val usages = UsageMap(figments)
    private val definitionModes = mutableMapOf<Definition, UsageMode>()
    private val aspectModes = mutableMapOf<Pair<Definition, Aspect>, UsageMode>()

    init {
        // Pre-calculate modes and ids and register with back compat as needed.
        figments.all()
            .filter { it.isUsageTracked() }
            .forEach { definition ->
                val mode = mode(definition) // <-- Pre-calculates all modes
                // For any definitions we skipped, make sure there is an exclude in the usage file.
                if (mode == UsageMode.SKIP) {
                    compat.exclude(definition)
                } else {
                    // For any aspects (fields/enum values) of an included definition that we skipped, make sure there is an exclude in the usage file.
                    definition.aspects
                        .filter { mode(it) == UsageMode.SKIP }
                        .forEach {
                            compat.exclude(it)
                        }
                }
            }
    }

    fun mode(definition: Definition): UsageMode {
        if (definitionModes.contains(definition)) return definitionModes[definition]!!
        if (!definition.isUsageTracked()) throw RuntimeException("$definition is a definition not tracked in usage files.")
        if (compat.excluded(definition)) return UsageMode.SKIP
        return when (definition) {
            is Value -> when {
                compat.included(definition) -> UsageMode.NORMAL
                else -> UsageMode.SKIP
            }
            is StatefulDefinition -> when {
                definition.deprecated -> when {
                    usages.isUsedActively(definition, this) -> UsageMode.NORMAL
                    compat.included(definition) -> UsageMode.COMPAT
                    else -> UsageMode.SKIP
                }
                else -> when {
                    compat.included(definition) -> UsageMode.NORMAL
                    else -> UsageMode.SKIP
                }
            }
            is Action -> when {
                definition.deprecated -> when {
                    compat.included(definition) -> UsageMode.COMPAT
                    else -> UsageMode.SKIP
                }
                else -> when {
                    compat.included(definition) -> UsageMode.NORMAL
                    else -> UsageMode.SKIP
                }
            }
            else -> throw RuntimeException("unexpected type $definition")
        }.also { definitionModes[definition] = it }
    }

    fun mode(aspect: Aspect): UsageMode {
        val key = Pair(aspect.context.current, aspect)
        if (aspectModes.contains(key)) return aspectModes[key]!!
        if (compat.excluded(aspect)) return UsageMode.SKIP
        return when {
            aspect.deprecated -> when {
                compat.included(aspect) -> UsageMode.COMPAT
                else -> UsageMode.SKIP
            }
            else -> when {
                compat.included(aspect) -> UsageMode.NORMAL
                else -> UsageMode.SKIP
            }
        }.also { aspectModes[key] = it }
    }

    /** See [UsageFile.commit] */
    fun commitUsageFile() = compat.commit()

    /** See [UsageFile.id] */
    fun id(aspect: Aspect) = compat.id(aspect)

    /** Returns a new list when any [UsageMode.SKIP] definitions excluded. */
    fun <D : Definition> removeSkips(definitions: List<D>) : List<D> = definitions.filter { mode(it) != UsageMode.SKIP  }

    /** Returns a list of [syncable]'s fields excluding any with [UsageMode.SKIP]. */
    fun getActiveFields(syncable: Syncable<*>): List<Field> {
        return syncable.fields.all.filter { mode(it) != UsageMode.SKIP }
    }
}


/**
 * This creates a map of all of the [StatefulDefinition]s and who is using them.
 *
 * The key is the type, and the value is a list of who using them. The who is a pair of the field, and the thing/action the field is within.
 *
 * Since this is only meant for use within the [.isUsedActively] method, any things/actions or fields that are deprecated,
 * are not mapped, so this map will only include usages of the type that are not part of something that is deprecated.
 *
 * After creation of an instance, you can query usages using [map].
 */
private class UsageMap(figments: Figments) {

    private val map = mutableMapOf<StatefulDefinition, MutableSet<Field>>()

    init {
        figments.syncables().plus(figments.remoteBases()).filter { !it.deprecated }.flatMap { it.fields.all }.filter { !it.deprecated }.forEach {
            map(it.type, it)
        }
    }

    private fun put(used: StatefulDefinition, user: Field) {
        map.getOrPut(used) { mutableSetOf() }.add(user)
    }

    private fun map(type: FieldType, user: Field) {
        when (type) {
            is InterfaceType -> type.compatible().forEach { put(it, user) }.also { put(type.definition, user) }
            is VarietyType -> type.compatible().forEach { put(it, user) }
            is CollectionType -> map(type.inner, user)
            is DefinitionType<*> -> put(type.definition, user)
        }
    }


    /**
     * Is this used directly by any non-deprecated things/fields?
     *
     *
     * This method helps us understand if we might be able to skip generating code for this definition.
     * If the only usages of this definition are in deprecated cases, then we say it isn't actively used and doesn't need to be generated.
     *
     *
     * To check this, we look at all of the fields that use the provided definition as its value type.
     * If all of those fields are deprecated or within deprecated things, we can consider it not actively used.
     * More specifically:
     *
     *  * If the field using this type is deprecated
     *  * If the thing that contains the field is deprecated
     *  * If the thing that contains the field is only used by deprecated/not-actively used things
     *
     * If all usages fall into one of those groups, then we consider this not actively used.
     *
     *
     * For open types like interfaces and varieties, worth calling out the logic there as well:
     *
     *  * If the provided type is an interface, it will only be marked as not actively used if it, and all of the definitions that extend out from it are not actively used.  If any implementations, regardless of depth of hierarchy are actively used, the interface will be considered to be actively used as well.
     *  * If the provided type has any interfaces, it will also check for cases that are using that interface. For example if you have an Animal interface and a Bird thing that implements Animal, if nothing is using Bird directly, but something is using Animal directly, then Bird is considered to still be actively used.
     *  * Same for if the provided type is used in any active varieties, then it will be considered as actively used.
     *
     * Also see [UsageMap]
     */
    fun isUsedActively(type: StatefulDefinition, modes: UsageModeCalculator): Boolean {
        val uses = map[type]
        if (uses.isNullOrEmpty()) return false
        for (user in uses) {
            if (user.context.current == type) continue  // Self usage doesn't count
            when (modes.mode(user)) {
                UsageMode.NORMAL -> return true
                UsageMode.COMPAT -> return true
                UsageMode.SKIP -> {} // Continue looking
            }
        }
        return false
    }
}
