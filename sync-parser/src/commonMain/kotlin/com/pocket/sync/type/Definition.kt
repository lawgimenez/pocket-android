package com.pocket.sync.type

import com.pocket.sync.Figments
import com.pocket.sync.parse.*
import com.pocket.sync.print.figment.toFigment
import com.pocket.sync.type.path.Flavor
import com.pocket.sync.type.path.Reference
import com.pocket.sync.type.path.ReferenceData
import com.pocket.sync.util.reactiveTo as referenceReactiveTo


/**
 * The classes in this file are concrete, fully typed, resolved, validated instances of spec/schema definitions.
 *
 * All properties should extend [Property] and definitions [Definition].
 *
 * All classes should be fully immutable and perform all internal resolving/validation during the constructor.
 *
 * Resolving references and accomplishing immutability can be pretty tricky and special care needs to be taken to avoid infinite loops.
 * To aid in this, any properties/parameters that reference other definitions, or the properties of other definitions, should use [Resolver.resolve].
 *
 */


// Common

/**
 * A validated/resolved version of a property.
 * When defining a new property you will also want to create a [PropertyData] and a [Syntax] for it.
 */
interface Property {
    /** The original source data that defined this property. */
    val data: PropertyData
    /** The [Definition] that hosts this property. Represented as a [Context] in case this property can be interfaced. If this property is a [Definition] this will just be itself. */
    val context : Context

    /** Enforce that implementations handle equality. All implementations likely should just delegate to [dataEquals]. */
    override fun equals(other: Any?) : Boolean
    /** Enforce that implementations handle equality. All implementations likely should just delegate to [dataHashCode]. */
    override fun hashCode() : Int
}

/** Compares the equality by using [Property.data], which should be a good equality comparison since these instances just represent the underlying data. And since they are all data classes, this avoids a lot of boilerplate. */
fun <P : Property> dataEquals(self: P, other: Any?) : Boolean {
    if (self === other) return true
    if (other == null || self::class != other::class) return false
    other as P
    return self.data == other.data
}
/** The hashCode sibling of [dataEquals] */
fun dataHashCode(p: Property) : Int = p.data.hashCode()

interface Definition : Property {
    val name : String
    val deprecated: Boolean
    val related: RelatedFeatures
    val description: DefinitionDescription
    val aspects: List<Aspect>
    /** The schema this belongs to. */
    @OnlyAfterResolving
    val schema: Figments
}
abstract class DefinitionBase(final override val data: DefinitionData, resolver: Resolver) : Definition {
    private val def = data.definition
    final override val name = def.name
    final override val context = Context(this)
    final override val deprecated = def.deprecated
    final override val related = RelatedFeatures(def.related, resolver, context)
    final override val description = DefinitionDescription(def.description, resolver, context)
    final override val schema = resolver.schema()
    override val aspects = emptyList<Aspect>()

    final override fun toString() = name
    final override fun equals(other: Any?) = dataEquals(this, other)
    final override fun hashCode() = dataHashCode(this)
}

interface Interface : Definition
interface Synthetic : Definition

interface Syncable<I : Interface> : Definition {
    val auth: RequiredAuth
    val remote: RemoteInfo
    val endpoint: EndpointInfo
    /** A query or a mutation a GraphQL source can use to build a request. */
    val operation: String?
    /** A list of interfaces this implements directly, plus the most appropriate base if applicable. (does not include interfaces that those interfaces implement). See [interfacesRecursive]. */
    val interfaces: List<I>
    val isInterface: Boolean
    val fields: Fields
    val hashTarget: Field?
    /** Returns itself, plus, if it [isInterface], also returns all syncables that are compatible with this interface (could be used where this interface is a type). */
    val compatible: Set<Syncable<I>>
    /** Same as [compatible] but does not include itself */
    val implementationsOf: Set<Syncable<I>>
    /** Returns all of the [Interface]s that make up this definition, including interfaces of those interfaces recursively. Also see [interfaces]. */
    fun interfacesRecursive() : Set<I>
}
abstract class SyncableBase<I : Interface>(data: SyncableData, resolver: Resolver)
    : DefinitionBase(data, resolver)
    , Syncable<I> {

    private val def = data.syncable
    override val auth = RequiredAuth(def.auth, resolver, context)
    override val remote = RemoteInfo(def.remote, resolver, context)
    override val endpoint = EndpointInfo(def.endpoint, resolver, context)
    override val operation = def.operation
    override val isInterface = def.isInterface
    override val interfaces: List<I> by resolver.resolve(this) {
        val all = def.interfaces.map { definition(it) as I }
        if (all.contains(definition(name))) throw ResolvingException("Interface $name references itself", data) // TODO do we need to check for cyclic inheritance with sub interfaces?
        if (all.distinctBy { it.name }.size != all.size) throw ResolvingException("duplicate interfaces declared", data)
        all
    }
    override val fields = Fields(def.fields, resolver, context)
    override val hashTarget by resolver.resolve(this) { fields.hashTarget() }
    override val implementationsOf by resolver.resolve(this) { implementationsOf<I, Syncable<I>>(name).toSet() }
    override val compatible by resolver.resolve(this) { implementationsOf.plus(this@SyncableBase as Syncable<I>).toSet() }

    override fun interfacesRecursive() : Set<I> = (interfaces + interfaces.flatMap { (it as Syncable<I>).interfacesRecursive() }).toSet()

    override val aspects by resolver.resolve(this) { fields.all }
}




// Definitions


open class Thing(data: ThingData, resolver: Resolver)
    : SyncableBase<ThingInterface>(data, resolver)
    , StatefulDefinition, AllowedInVariety {
    val unique by resolver.resolve(this) { UniqueInfo(data.unique, resolver, context) }

    /**
     * Returns true if this thing has any identifying fields.
     *
     * Special consideration should be taken for [ThingInterface]s though.
     * Think about whether your use case also wants to consider [implementationsOf] of the interface as well.
     * The [ThingInterface] may not have any identifying fields, but perhaps some of its implementations do.
     * When looking at all possible implementations of an interface, you may have all non-identifiable, all identifiable
     * or a mix of some identifiable and non-identifiable things.
     *
     * This method only reflects what this [Thing] / [ThingInterface]'s fields are, this does NOT factor in anything about any implementations of this.
     */
    fun isIdentifiable() = unique.merged != null || fields.ids().isNotEmpty()
    /** If this thing has a root value, this will be non null */
    fun rootValue() = fields.all.find { it.root }
}
class ThingInterface(data: ThingData, resolver: Resolver, val unknown: UnknownThing) : Thing(data, resolver), Interface
class UnknownThing(data: ThingData, resolver: Resolver) : Thing(data, resolver), Synthetic

open class Action(data: ActionData, resolver: Resolver)
    : SyncableBase<ActionInterface>(data, resolver) {
    val isBase by resolver.resolve(this) { data.isBase }
    val priority by resolver.resolve(this) { PriorityInfo(data.priority, resolver, context) }
    val effect = Effect(data.effect, resolver, context)
    val remoteBaseOf by resolver.resolve(this) { data.remoteBaseOf?.let { remote(it) } }
    val resolves by resolver.resolve(this) { ResolvesInfo(data.resolves, resolver, context) }
    override val interfaces: List<ActionInterface> by resolver.resolve(this) {
        val all = data.syncable.interfaces.map { action(it) as ActionInterface }.toMutableList()

        // Don't allow base actions to be explicitly mentioned as interfaces.  They should be implied, a definition shouldn't say something like `action Concrete : base_action {`
        // Also cannot extend a remote base explicitly either
        val baseNames = baseNames()
        all.forEach { if (baseNames.contains(it.name)) throw ResolvingException("base actions cannot be included in interface list, they are only implied.", it, data)}

        // Add a base if needed.
        // There will be 0 or 1 base. It picks the most suitable one, either none, a remote base, or the main base.
        var base: ActionInterface?
        if (isBase) {
            // When this action is a base itself
            if (data.remoteBaseOf != null) {
                // This action is a remote base, the only allowed base is the main base
                base = baseAction() as ActionInterface?
            } else {
                // This action is THE main base, it won't have a base
                base = null
                if (all.isNotEmpty()) throw ResolvingException("The main base action may not extend other actions.", data)
            }
        } else {
            // Normal actions
            // Want to check if this action has an assigned remote... but remotes are interfaced properties, which depend on knowing what interfaces it has
            // So we can't reference [remote.merged] yet, otherwise it will infinite loop.
            // So we have look up the merged remote manually. We won't validate it, we'll leave the remote merging to handle that
            val remote = data.syncable.remote ?: all.mapNotNull { (it.data as ActionData).syncable.remote }.getOrNull(0)
            if (remote != null) {
                // If this action has a remote declared, and that remote has a base, use that
                base = remote(remote.name).baseAction
            } else {
                // If it doesn't declare a specific remote, check for the default remote
                base = defaultRemote()?.baseAction
            }
            // If no base came from the remotes, use the general purpose base
            // It doesn't need to add the main base if it has a remote base, because the remote base will inherit from the main base.
            if (base == null) base = baseAction() as ActionInterface?
        }
        if (base != null) all.add(base)
        if (this@Action is ActionInterface && all.contains(this@Action)) throw RuntimeException("Interface $name references itself") // TODO do we need to check for cyclic inheritence with sub interfaces?
        all.toList()
    }
}

class ActionInterface(data: ActionData, resolver: Resolver)
    : Action(data, resolver)
    , Interface

class Value(data: ValueData, resolver: Resolver)
    : DefinitionBase(data, resolver)
    , StatefulDefinition

class Enum(data: EnumData, resolver: Resolver)
    : DefinitionBase(data, resolver)
    , StatefulDefinition {
    val options: List<EnumOption> by resolver.resolve(this) { data.options.map { EnumOption(it, resolver, context) } }
    override val aspects by resolver.resolve(this) { options }
    fun hasIntegerValues() : Boolean = options.filter { it.value.toIntOrNull() != null }.size == options.size
}

class Feature(data: FeatureData, resolver: Resolver)
    : DefinitionBase(data, resolver)

class Auth(data: AuthData, resolver: Resolver)
    : DefinitionBase(data, resolver) {
    val default = data.default
}

class Remote(
    data: RemoteData, resolver: Resolver,
    val baseAction: ActionInterface? = null
) : DefinitionBase(data, resolver) {
    val default = data.default
}

interface AllowedInVariety : StatefulDefinition

class Variety(data: VarietyData, resolver: Resolver, val unknown: UnknownThing)
    : DefinitionBase(data, resolver)
    , StatefulDefinition, AllowedInVariety {
    val options: List<VarietyOption> by resolver.resolve(this) {
        data.options
            .plus(VarietyOptionData(unknown.name, emptyList(), data.source))
            .map { VarietyOption(it, resolver, context) }
    }
    /** Similar to [Syncable.compatible], all things this variety can ultimately represent. */
    val compatible: Set<Thing> by resolver.resolve(this) { options.flatMap {
            when (val type = it.type) {
                is Thing -> type.compatible
                is Variety -> type.compatible
                else -> throw ResolvingException("$type is not a type allowed in variety options.")
            }
        }.map { it as Thing }.toSet()
    }
}

class Slice(data: SliceData, resolver: Resolver) : DefinitionBase(data, resolver) {
    val rules: List<SliceRule> by resolver.resolve(this) { data.rules.map { SliceRule(it, resolver, context) } }
}




// Fields, Types and Values

/** A [Property] of a [Definition] that can be represented by the path in a [Reference] */
interface Aspect : Property {
    /** The name this aspect is referenced by in [Reference.path] */
    val name : String
    val deprecated : Boolean
}

class Fields(
    data: List<FieldData>,
    resolver: Resolver,
    context: Context
): InterfacedListProperty<FieldData, Field, Syncable<*>>(
    data = data,
    resolver = resolver,
    create = { context, data -> Field(data, resolver, context) },
    context = context.current as Syncable<*>,
    propertyGetter = { it.fields.all },
    propertyId = {it.name}
) {
    init {
        resolver.resolve(this) {
            // Validate there are no duplicates within self declared fields
            if (self.distinct().size != self.size) throw ResolvingException("definition has multiple fields with the same name", this)
            // Validate there is 0 or 1 hash target
            if (all.filter { it.hashTarget }.size > 1) throw ResolvingException("definition has multiple fields marked as hash target", this)
            // Validate root fields
            if (context.current is Thing) {
                if (all.filter {
                        if (it.root && (it.identifying || it.hashTarget || it.deprecated || it.aliases.isNotEmpty() || it.localOnly)) throw ResolvingException("root fields shouldn't have aliases or flags", this)
                        it.root
                    }.size > 1) throw ResolvingException("definition has multiple fields marked as a root value", this)
            } else {
                if (all.any { it.root }) throw ResolvingException("only things can have a root field", this)
            }
        }
    }

    fun ids() = all.filter { it.identifying }
    fun states() = all.filter { !it.identifying }
    fun hashTarget() = all.find { it.hashTarget }
    /** Returns the field with this name or throws an error if not found.*/
    fun get(name: String) : Field = all.find { it.name == name } ?: throw RuntimeException("No field found with name '$name'")
}

class Field(override val data: FieldData, resolver: Resolver, override val context: Context) : Aspect {
    /** Either the only name, or if [aliases] are present, the name of the first one. */
    override val name = data.name
    override val deprecated = data.deprecated
    /** If aliases, non-empty. Key is the remote, value is the field name in that context. */
    val aliases by resolver.resolve(this) { data.aliases.mapKeys { remote(it.key) } }
    val identifying = data.identifying
    val hashTarget = data.hashTarget
    val localOnly = data.localOnly
    val root = data.root
    val description = NonInterfacedDescription(data.description, resolver, context)
    val type by resolver.resolve(this) { type(data.type, this) }
    val derives = Derive(data.derives, context, resolver)
    override fun toString() = context.source.name+"."+name
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

/**
 * Contains info about how a field is derived.
 * [isDerived] can be used to see if there is any derived info.
 * If so, then at least one of the various methods will be non-null.
 */
class Derive(data: List<DeriveData>, context: Context, resolver: Resolver) {
    val firstAvailable by resolver.resolve(this) {
        val found = data.filterIsInstance<FirstAvailableData>()
        if (found.size > 1) throw ResolvingException("Contains more than one first available derive lines", context)
        if (found.isEmpty()) null else FirstAvailable(found[0], resolver, context)
    }
    val remap by resolver.resolve(this) {
        val found = data.filterIsInstance<RemapData>()
        if (found.size > 1) throw ResolvingException("Contains more than one remap derive lines", context)
        if (found.isEmpty()) null else Remap(found[0], resolver, context)
    }
    val reactives by resolver.resolve(this) {
        data.filterIsInstance<ReactivesData>().map { Reactives(it, resolver, context) }
    }
    val instructions by resolver.resolve(this) {
        data.filterIsInstance<InstructionsData>().map { Instructions(it, resolver, context) }
    }
    /** Its reactives as declared in the schema. Also see [reactiveTo]. */
    val reactiveToDeclared: List<ContextualReference> by resolver.resolve(this) {
        when {
            firstAvailable != null -> {
                if (remap != null || reactives.isNotEmpty() || instructions.isNotEmpty()) throw ResolvingException("Cannot have mixtures of deriving methods", context)
                firstAvailable!!.options
            }
            remap != null -> {
                if (firstAvailable != null || reactives.isNotEmpty() || instructions.isNotEmpty()) throw ResolvingException("Cannot have mixtures of deriving methods", context)
                listOf(remap!!.list)
            }
            reactives.isNotEmpty() || instructions.isNotEmpty() -> {
                if (firstAvailable != null || remap != null) throw ResolvingException("Cannot have mixtures of deriving methods", context)
                reactives.flatMap { it.reactives }
            }
            else -> emptyList()
        }
    }
    /** What it is reactive to when factoring in interfaces. For examples and in depth discussion, see [referenceReactiveTo]. Also [reactiveToDeclared]. */
    val reactiveTo: List<ContextualReference> by resolver.resolve(this) {
       reactiveToDeclared.flatMap { reactive ->
           when (reactive.flavor()) {
               Flavor.TYPE, Flavor.TYPE_FIELD -> {
                   val ref = reactive.current
                   // If type is an interface, also reacts to any of its implementations
                   if (ref.type is Interface) {
                       (ref.type as Syncable<*>).compatible.map { c ->
                           val revised = ReferenceData(
                                   ref.reference.text.replaceBefore(".", c.name, c.name),
                                   ref.reference.context)
                           ContextualReference(revised, context, resolver)
                       }
                       .plus(reactive)
                   } else {
                       listOf(reactive)
                   }
               }
               Flavor.SELF, Flavor.SELF_FIELD -> listOf(reactive)
           }
       }
    }
    fun all() : List<DeriveProperty> {
        val all = mutableListOf<DeriveProperty>()
        if (firstAvailable != null) all.add(firstAvailable!!)
        if (remap != null) all.add(remap!!)
        all.addAll(reactives)
        all.addAll(instructions)
        return all.sortedBySource()
    }
    fun isDerived() = firstAvailable != null || remap != null || instructions.isNotEmpty() || reactives.isNotEmpty()
}

private fun type(type: FieldTypeData, referencer: Referencer) : FieldType {
    return when (type) {
        is ReferenceTypeData -> referenceType(type.definition, type.flag, referencer)
        is ListTypeData -> {
            val inner = type(type.inner, referencer) as AllowedInCollectionType
            ListType(inner, type.flag)
        }
        is MapTypeData -> {
            val inner = type(type.inner, referencer) as AllowedInCollectionType
            MapType(inner, type.flag)
        }
        else -> throw ResolvingException("unknown type", type)
    }
}
private fun referenceType(definitionName: String, flag: TypeFlag, referencer: Referencer) : DefinitionType<*> {
    val def = referencer.definition(definitionName) as StatefulDefinition
    return when (def) {
        is ThingInterface -> InterfaceType(def, flag)
        is Variety -> VarietyType(def, flag)
        is Thing -> ReferenceType<Thing>(def, flag)
        else -> ReferenceType<StatefulDefinition>(def, flag)
    }
}

class EnumOption(override val data: EnumOptionData, resolver: Resolver, override val context: Context) : Aspect {
    val value = data.value
    val alias = data.alias
    override val name = alias ?: value
    override val deprecated = data.deprecated
    val description = NonInterfacedDescription(data.description, resolver, context)
    override fun toString() = context.current.name+"."+name
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class VarietyOption(override val data: VarietyOptionData, resolver: Resolver, override val context: Context) : Property {
    val type: AllowedInVariety by resolver.resolve(this) {
        when (val def = definition(data.type)) {
            is AllowedInVariety -> def
            else -> throw ResolvingException("${data.type} is not a type allowed in variety options.")
        }
    }
    val description = NonInterfacedDescription(data.description, resolver, context)

    override fun toString() = type.name
    override fun equals(other: Any?) = type == other // TODO why is this not dataEquals(this, other) ?
    override fun hashCode() = type.hashCode()
}

class SliceRule(override val data: SliceRuleData, resolver: Resolver, override val context: Context) : Property {
    val definition: Definition by resolver.resolve(this) { definition(data.definition) }
    val aspect: Aspect? by resolver.resolve(this) { data.aspect?.let { definition.aspects.find { it.name == data.aspect } } }
    val exclude = data.exclude
    val description = NonInterfacedDescription(data.description, resolver, context)
    override fun toString() = (if (data.exclude) "- " else "") + data.definition + (if (data.aspect != null) ".${data.aspect}" else "")
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}



// Body properties

class EndpointInfo(data: EndpointFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<EndpointFlagData, Endpoint, Syncable<*>>(
    data = data,
    resolver = resolver,
    create = { context, data -> Endpoint(data, resolver, context) },
    context = context.current as Syncable<*>,
    propertyGetter = {i -> i.endpoint.merged}
)
class Endpoint(override val data: EndpointFlagData, resolver: Resolver, override val context: Context) : Property {
    val address : String = data.address
    val method : String? = data.method
    /** A list of any params within [address] that should be replaced with values before using. */
    val addressParams by address.parseLinks(resolver, context)
    override fun toString() = address
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class RelatedFeatures(data: List<RelatedFeatureFlagData>, resolver: Resolver, context: Context): InterfacedListProperty<RelatedFeatureFlagData, RelatedFeature, Definition>(
    data = data,
    resolver = resolver,
    create = { context, data -> RelatedFeature(data, resolver, context) },
    context =  context.current ,
    propertyGetter = { it.related.all }
){
    /** Same as [all] but returns the [Feature]s the properties are pointing at. */
    fun features() : List<Feature> = all.map { it.related }
}
class RelatedFeature(override val data: RelatedFeatureFlagData, resolver: Resolver, override val context: Context) : Property {
    val related by resolver.resolve(this) { feature(data.name) }
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class RequiredAuth(data: AuthFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<AuthFlagData, AuthFlag, Syncable<*>>(
    data = data,
    resolver = resolver,
    create = { context, data -> AuthFlag(data, resolver, context) },
    context = context.current as Syncable<*>,
    propertyGetter = {i -> i.auth.merged}
) {
    /** The [Auth] to use after considering what was self declared, merged and default. */
    val auth: Auth? by resolver.resolve(this) { merged?.auth ?: defaultAuth() }
}
class AuthFlag(override val data: AuthFlagData, resolver: Resolver, override val context: Context) : Property {
    val auth by resolver.resolve(this) { auth(data.name)}
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class RemoteInfo(data: RemoteFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<RemoteFlagData, RemoteFlag, Syncable<*>>(
    data = data,
    resolver = resolver,
    create = { context, data -> RemoteFlag(data, resolver, context) },
    context = context.current as Syncable<*>,
    propertyGetter = {i -> i.remote.merged}
) {
    /** The [Remote] to use after considering what was self declared, merged and default. */
    val remote: Remote? by resolver.resolve(this) { merged?.remote ?: defaultRemote() }
}
class RemoteFlag(override val data: RemoteFlagData, resolver: Resolver, override val context: Context) : Property {
    val remote by resolver.resolve(this) { remote(data.name) }
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class PriorityInfo(data: PriorityFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<PriorityFlagData, PriorityFlag, Action>(
    data = data,
    resolver = resolver,
    create = { context, data -> PriorityFlag(data, context) },
    context = context.current as Action,
    propertyGetter = {i -> i.priority.merged}
)
class PriorityFlag(override val data: PriorityFlagData, override val context: Context) : Property {
    val priority : Priority = Priority.parse(data.priority)!!
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class ResolvesInfo(data: ResolvesFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<ResolvesFlagData, ResolvesFlag, Action>(
    data = data,
    resolver = resolver,
    create = { context, data -> ResolvesFlag(data, context, resolver) },
    context = context.current as Action,
    propertyGetter = {i -> i.resolves.merged}
)
class ResolvesFlag(override val data: ResolvesFlagData, override val context: Context, resolver: Resolver) : Property {
    val type by resolver.resolve(this) { type(data.type, this) }
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

class UniqueInfo(data: UniqueFlagData?, resolver: Resolver, context: Context): InterfacedSingleProperty<UniqueFlagData, UniqueFlag, Thing>(
    data = data,
    resolver = resolver,
    create = { context, data -> UniqueFlag(data, context) },
    context = context.current as Thing,
    propertyGetter = {i -> i.unique.merged}
)
class UniqueFlag(override val data: UniqueFlagData, override val context: Context) : Property {
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

interface DeriveProperty : Property {
    fun reactiveTo() : List<ContextualReference>
}

class FirstAvailable(override val data: FirstAvailableData, resolver: Resolver, override val context: Context) : DeriveProperty {
    val options by resolver.resolve(this) { data.options.map { ContextualReference(it, context, resolver) } }
    override fun reactiveTo(): List<ContextualReference> = options
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}
class Instructions(override val data: InstructionsData, resolver: Resolver, override val context: Context) : DeriveProperty {
    val instruction = NonInterfacedDescription(listOf(data.instruction), resolver, context)
    override fun reactiveTo() = emptyList<ContextualReference>()
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}
class Reactives(override val data: ReactivesData, resolver: Resolver, override val context: Context) : DeriveProperty {
    val reactives by resolver.resolve(this) { data.reactives.map { ContextualReference(it, context, resolver) } }
    override fun reactiveTo(): List<ContextualReference> = reactives
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}
class Remap(override val data: RemapData, resolver: Resolver, override val context: Context) : DeriveProperty {
    val list by resolver.resolve(this) { ContextualReference(ReferenceData(".${data.list}", ContextData(context.current.name)), context, resolver) }
    val field = data.field
    override fun reactiveTo(): List<ContextualReference> = listOf(list)
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

enum class Priority {
    ASAP,
    WHENEVER,
    REMOTE,
    REMOTE_RETRYABLE,
    LOCAL;

    companion object {
        fun parse(value: String?): Priority? {
            if (value == null) return null
            for (a in values()) {
                if (a.name == value) {
                    return a
                }
            }
            throw RuntimeException("unknown value $value")
        }
    }
}




// Helpers for handling interfaced properties

/**
 * Handling for a type of property that a definition can have multiple of.
 * Any interface or implementation can add these properties, and the collective result is all properties declared on the interfaces and implementation.
 * These properties can be reiterated exactly, but cannot conflict, otherwise if a conflict is found an error is thrown.
 */
open class InterfacedListProperty<D : PropertyData, P : Property, C : Definition>(
    /** The raw data of these properties in this implementation */
    data: List<D>,
    /** A resolver */
    resolver: Resolver,
    /** Given the property data, within a context, create an instance of the property */
    create: Referencer.(context: Context, data: D) -> P,
    /** Within a Reference context, return a reference to the definition that these properties belong to */
    private val context: C,
    /** Given an instance of C, return these properties from that instance. The order should be values from the parent interfaces first. */
    propertyGetter: (C) -> List<P>,
    /** Given a property, return a value that ids it. Used to check for duplicates that conflict. */
    private val propertyId: (P) -> Any = {it.toDataComparable()},
    private val allowDuplicates: Boolean = false
) {
    // We need to calculate all of them at once, but lazily.. so do it together into one object first.
    data class Data<P : Property>(val self : List<P>, val interfaced : List<P>, val all : List<P>)
    private val calculated : Data<P> by resolver.resolve(this) {
        // Properties found directly on the implementation
        val self = data.map { create(Context(context), it) }
        // Properties found in its interfaces
        val interfaced = if (context is Syncable<*>) {
            context.interfaces.flatMap { propertyGetter(it as C) } // Get all properties inherited from interfaces
                    .map { create(this, Context(context, it.context.source), it.data as D) } // Recreate them with a context that has this as the current, and whatever the source was
                    .dedupe {a,b -> a} // In this case, either duplicate should have the right context, so just pick one.
        } else {
            emptyList()
        }
        // Combined
        val all = interfaced.plus(self) // Ordering here is interfaced props then self
                .dedupe {a,b -> if (interfaced.contains(a)) a else b } // In this case, we want to prefer the one from interfaced, since it will have source info.

        Data(self, interfaced, all)
    }

    /** Only the properties that appeared in the definition itself. These may still be inherited, if they were reiterated. To know if a property is truly not part of an interface you would need to do self.minus(interfaced). The [Property.context] will have the implementation as [Context.current] and if reiterated, [Context.source] will be the source interface. */
    val self : List<P> by resolver.resolve(this) { calculated.self }
    /** Only the properties that came from interfaces the definition implemented. The [Property.context] will have the implementation as [Context.current] and [Context.source] will be the source interface.*/
    val interfaced : List<P> by resolver.resolve(this) { calculated.interfaced }
    /** All properties, a merging of the definition and all of its interfaces The [Property.context] will have the implementation as [Context.current] and [Context.source] will be the source interface if there was one.*/
    val all : List<P> by resolver.resolve(this) { calculated.all }

    /** Remove duplicates. If there is a conflicting duplicate, throw an error. Otherwise ignore reiteration. */
    private fun List<P>.dedupe(choose: (P, P) -> P) : List<P> {
        if (allowDuplicates) return this
        return distinctBy(propertyId) { a,b ->
            if (a.toDataComparable() != b.toDataComparable()) println("Warning: Merged properties conflict $a vs $b")
            choose(a,b)
        }
    }
}

/**
 * Returns some value that can be compared between two properties that represents its pure data value, regardless of source location.
 * Because [Source] is included as part of the normal equality checks of [Property] and [PropertyData], when doing checks
 * for reiterated properties (like when an implementation of an interface reiterates the same property) they won't be equal
 * because their source is not the same.  Comparing the result of this method instead, it can compare the data values regardless of source location.
 */
fun Property.toDataComparable() = toFigment() // Max: toFigment() should be a perfect proxy for this data
// Marcin:
// Yea, this is the historical reason why we keep the entire "Figment printer" around.
// Just to be clear, no-one wrote the "printer" just for the sake of comparing properties ignoring
// their source location. Max wrote the printer anyway, because he wanted to have an automated way
// to format figment files. And then when he wanted to write this comparator function I guess he
// just decided to take this shortcut and use this existing function, instead of going the "proper"
// way of re-architecting properties to allow comparing ignoring their source location.

/**
 * Handling for a property that will only have one per definition.
 * Any interface or implementation can declare this property, or none may declare it.
 * If more than one declares it, they must be equal, otherwise if a conflict is found an error is thrown.
 */
open class InterfacedSingleProperty<D : PropertyData, P : Property, C : Definition>(
    /** The raw data of the property */
    data: D?,
    /** A resolver */
    resolver: Resolver,
    /** Given the property data, within a Reference context, create an instance of the property */
    create: Referencer.(context: Context, data: D) -> P,
    /** The [Definition] of the definition this property is part of. */
    context: C,
    /** Given an instance of C, return this property from that instance. */
    propertyGetter: ((C) -> P?)
) {
    /** Use [InterfacedListProperty] under the hood to share all of the fancy logic it has. Just need to convert between lists/nullable expectations. */
    private val list = InterfacedListProperty<D, P, C>(
            data?.let { listOf(it) } ?: emptyList(),
            resolver, create, context,
            {c -> propertyGetter(c)?.let { listOf(it) } ?: emptyList()}
    )

    /** The value as it appeared in the definition, can be null if it didn't declare a value. */
    val self : P? by resolver.resolve(this) { list.self.getOrNull(0) }
    /** The value as a result of merging its interfaces or by a provided default. */
    val merged : P? by resolver.resolve(this) { list.all.getOrNull(0) }

    init {
        resolver.resolve(this) { if (list.all.size > 1) throw ResolvingException("Some properties are duplicated", context) }
    }
}

/**
 * Holder of description info.
 */
abstract class Description(data: List<DescriptionData>, resolver: Resolver, context: Context, propertyGetter: ((Definition) -> List<Segment>)): InterfacedListProperty<DescriptionData, Segment, Definition>(
    data = data,
    resolver = resolver,
    create = { context, data -> Segment(data, resolver, context) },
    context = context.current,
    propertyGetter = propertyGetter,
    allowDuplicates = true
){
    fun isEmpty(): Boolean = all.isEmpty()
    override fun toString(): String = all.joinToString(""){ it.text }
}
/** Descriptions of a [Definition]. Supports merging from interfaces when applicable. Use [self] or [all] depending on your use case. */
class DefinitionDescription(data: List<DescriptionData>, resolver: Resolver, context: Context) : Description(data, resolver, context, { it.description.all } )
/** Effects for [Action.effect]. Supports merging from interfaces when applicable. Use [self] or [all] depending on your use case. */
class Effect(data: List<DescriptionData>, resolver: Resolver, context: Context) : Description(data, resolver, context, { (it as Action).effect.all } )
/** Descriptions of a property. Does not support merging descriptions of this property across interfaces. [self] and [all] will be the same. */
class NonInterfacedDescription(data: List<DescriptionData>, resolver: Resolver, context: Context) : Description(data, resolver, context, { emptyList() })

/** A part of a description. */
class Segment(override val data: DescriptionData, resolver: Resolver, override val context: Context) : Property {
    val type = data.type
    val text = data.text
    /** Any [Reference]s found in the description and their location within [] */
    val links by text.parseLinks(resolver, context)
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}

data class Link(
    val reference: ContextualReference,
    /** The 0 based position of the first character of this link within [Segment.text]. */
    val start: Int,
    /** The 0 based position of the 1 past the last character of this link within [Segment.text]. */
    val end: Int)

data class UnresolvedLink(
    val reference: ReferenceData,
    /** The 0 based position of the first character of this link within [Segment.text]. */
    val start: Int,
    /** The 0 based position of the 1 past the last character of this link within [Segment.text]. */
    val end: Int)

/**
 * Returns any [Reference] [Link]s found in this text.
 */
fun String.parseLinks(resolver: Resolver, context: Context) : Lazy<List<Link>> {
    val text = this
    return resolver.resolve(this) {
        text.parseLinks(ContextData(context.source.name)).map {
            Link(ContextualReference(it.reference, context, resolver), it.start, it.end)
        }
    }
}

/**
 * Same as [parseLinks] but doesn't resolve the links, just finds the text ranges that look like links.
 */
fun String.parseLinks(context: ContextData) : List<UnresolvedLink> {
    return linkRegex.findAll(this).mapNotNull {
        var match = it.groupValues[0]
        if (match.length < 3) return@mapNotNull null
        match = match.substring(1, match.length - 1) // Trim off {}
        UnresolvedLink(
            ReferenceData(match, context),
            it.range.start,
            it.range.endInclusive+1
        )
    }.toList()
}

private val name = "[/a-zA-Z0-9_-]+" // Similar to word, but allows / slashes
private val pathIndex = "\\[[0-9]+]"
private val pathKey = "\\.$name"
private val pathPart = "(($pathKey)|($pathIndex))"
private val path = "$pathKey($pathPart)*"
private val reference = "(($name)|($path)|($name$path))"
private val linkRegex = Regex("\\{$reference}")




/** Sort a list of properties so they are ordered in the way they were found in the file. */
fun <P : Property> List<P>.sortedBySource() : List<P> = sortedWith(compareBy<P> { it.data.source.path }.thenBy { it.data.source.startLine })

/**
 * Info about the [Definition] a property belongs to.
 * Also helps about ambiguity when a property is interfaced, since this will force you to make a decision about whether you want [current] or [source].
 */
class Context(
    /**
     * The definition that this property instance belongs to in the current context.
     *
     * For example, if a field was originally defined in an interface, and you are looking at a field instance within an implementation of that interface,
     * [current] would be the implementation that reiterated/inherited it, while [source] point to the interface that originally declared it.
     *
     * But if you query that same field within the [ThingInterface] that defined it, both [current] and [source] would be that interface itself.
     *
     * If the property is not interfaced, [current] and [source] will always be the same.
     */
    val current : Definition,
    /**
     * If this property was originally defined in an interface, this will be the original interface. Otherwise it will just be [current].
     * TODO for overlapping properties, right now this will just pick a source at random. To handle that case properly, this may need to be a List[Definition] and some additional complexity in [InterfacedListProperty] to capture them. Same goes for [ContextualReference.source]
     */
    val source : Definition = current
)

/**
 * A [Reference] that may resolve differently depending on [Context].
 *
 * For example, if an interface has a field whose description or reactives reference `.field`,
 * The [Reference.type] in that case would be the interface itself.
 *
 * But what about when you look at that field within an implementation?  Is [Reference.type] the implementation or the interface?
 *
 * Similarly to [Context], this class helps ensure that you make an explicit choice of what perspective you want.
 *
 * [current] will give you a reference resolved against the implementation. [source] more like it was originally defined.
 *
 * If this property wasn't interfaced, both will be the same.
 */
class ContextualReference(data: ReferenceData, context: Context, resolver: Resolver) {
    /** The reference resolved with [Context.current] as the context. */
    val current : Reference by resolver.resolve(this) { Reference(ReferenceData(data.text, ContextData(context.current.name)), resolver) }
    /** The reference resolved with [Context.source] as the context. */
    val source : Reference by resolver.resolve(this) { Reference(ReferenceData(data.text, ContextData(context.source.name)), resolver) }
    fun flavor() = current.flavor()
    override fun toString(): String = current.toString()
}