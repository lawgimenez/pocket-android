package com.pocket.sync.parse

import com.pocket.sync.Figments
import com.pocket.sync.type.*
import com.pocket.sync.type.Enum

fun FigmentsData.resolve() : Figments = Figments(this)

/**
 * Takes the raw [FigmentsData], fully validates it and returns instances that are fully resolved.
 * @param figments The [Figments] instance that should be set as [Definition.schema] for all resolved instances.
 */
fun FigmentsData.resolve(figments: Figments) : Set<Definition> {
    /*
    * This process has some tricky aspects to it, mostly related to the challenges of creating immutable instances,
    * when those instances all reference each other, or even self reference each other in loops.
    *
    * To solve this, we do the following:
    * 1. Create instances of all of the definitions
    * 2. When creating them, pass a [Resolver] into those constructors
    * 3. During construction, if any definition fields rely on references to other definitions, use a [Resolver] to initialize the field, which returns a lazy, and we capture this lazy as a resolving task
    * 4. After all definitions are created, we loop through all of the resolving tasks and invoke them in order to ensure all references actually worked and were validated. This makes sure by the time this method returns Figments, we have guaranteed all resolvers have completed and everything is 100% valid.
    * 5. Those lazy/resolving tasks are invoked within a [Referencer] context, so when they run, they can safely query other definitions, because they are all created at this point.
    * 6. This looping continues until all lazy/resolvers have been invoked and completed. During this process definitions can reference other definitions' fields safely because if they need to be resolved, it will just do so in a cascading fashion.
    *
    * For this to work, the major contract is that all definitions MUST make all fields `val` and use [Resolver.resolve] to do any kind of cross referencing or lazy init.
    */

    val instances = mutableMapOf<String, Definition>()
    val resolvingTasks = mutableListOf<() -> Unit>()

    // We keep track of a stage to try to catch usage of the resolver/referencer out of contract. (Either too early or too late)
    var stage = Stage.CREATING
    val stageChecker = { stage }

    val referencer = Referencer(instances, stageChecker)
    val resolver = ResolverImpl(resolvingTasks, referencer, stageChecker, figments)

    // Create instances for all definitions
    // During this, the resolver will be collecting tasks to resolve.
    for (info in definitions) {
        val definition = when (info) {
            is ThingData -> {
                if (info.syncable.isInterface) {
                    val unknown = unknown(info, resolver)
                    instances.putOnce(unknown)
                    ThingInterface(info, resolver, unknown)
                } else {
                    Thing(info, resolver)
                }
            }
            is ActionData -> if (info.syncable.isInterface) ActionInterface(info, resolver) else Action(info, resolver)
            is ValueData -> Value(info, resolver)
            is EnumData -> Enum(info, resolver)
            is FeatureData -> Feature(info, resolver)
            is AuthData -> Auth(info, resolver)
            is VarietyData -> {
                val unknown = unknown(info, resolver)
                instances.putOnce(unknown)
                Variety(info, resolver, unknown)
            }
            is RemoteData -> {
                if (info.baseAction != null) {
                    val action = ActionInterface(info.baseAction, resolver)
                    instances.putOnce(action)
                    Remote(info, resolver, action)
                } else {
                    Remote(info, resolver)
                }
            }
            is SliceData -> Slice(info, resolver)
            else -> throw ResolvingException("unexpected data", info)
        }
        instances.putOnce(definition)
    }

    // Now that we have all of the definitions created,
    // we can safely resolve references across those definitions
    // This loops since each call could add more tasks
    stage = Stage.RESOLVING
    while (resolvingTasks.isNotEmpty()) {
        val run = resolvingTasks.toList() // Create a copy to iterate on, clear the queue
        resolvingTasks.clear()
        run.forEach { it.invoke() }
    }

    // Validate there is only 1 base action, default remote, default auth, etc.
    referencer.baseAction() // These methods internally do the validation check
    referencer.defaultAuth()
    referencer.defaultRemote()

    stage = Stage.COMPLETED
    return instances.values.toSet()
}

/** Put only once, if already present, throw an error. */
private fun MutableMap<String, Definition>.putOnce(value: Definition) {
    val existing = put(value.name, value)
    if (existing != null) throw ResolvingException("Two definitions with same name: '${existing.name}'", existing, value)
}

/** Create an "unknown" implementation for an open type. */
private fun unknown(info: DefinitionData, resolver: ResolverImpl) = UnknownThing(
        ThingData(
                definition = info.definition.copy(
                        name = "Unknown${info.definition.name}",
                        description = emptyList(),
                ),
                syncable = SyncableProperties(
                        auth = null,
                        remote = null,
                        endpoint = null,
                        interfaces = if (info is ThingData && info.syncable.isInterface)
                            listOf(info.definition.name) else emptyList(),
                        isInterface = false,
                        fields = emptyList(),
                        source = info.source,
                ),
                unique = null,
                source = info.source,
        ),
        resolver
)

enum class Stage {
    CREATING,
    RESOLVING,
    COMPLETED
}

interface Resolver {
    /**
     * Use to lazy initialize a field that requires referencing other definitions or other fields that had to be resolved themselves.
     * @param context Used for better error logging if something goes wrong, pass in the definition, property or whatever you want to appear in logs if your resolving function fails
     * @param initializer A lambda to run, within a [Referencer] context, where you can safely query and grab references to other definitions. See the methods on that class for options.
     */
    fun <R> resolve(context: Any, initializer: Referencer.() -> R): Lazy<R>

    /**
     * Use to initialize [Definition.schema].
     */
    fun schema(): Figments
}

class ResolverImpl(
    private val output: MutableList<() -> Unit>,
    private val referencer: Referencer,
    private val stage: () -> Stage,
    private val figments: Figments
) : Resolver {

    override fun <R> resolve(context: Any, initializer: Referencer.() -> R): Lazy<R> {
        if (stage.invoke() == Stage.COMPLETED) throw RuntimeException("Resolving should be completed by now. Make sure all references are resolved during the constructor.")

        // Convert this to a lazy init
        // Wrap it with a `with()` so callers get the resolver context
        // Use a try catch block to provide better error messaging based on the context
        val lazy = lazy {
            // This won't run until something requests the lazy value, such as during the resolving loop.
            try {
                with(referencer, (initializer))
            } catch (t: Throwable) {
                throw ResolvingException("", t, context)
            }
        }

        // Add to the queue to be run later
        output.add { lazy.value } // .value triggers it to run if it hasn't already

        return lazy
    }

    override fun schema() = figments
}

/**
 * A context that you can use to query other definitions.
 */
class Referencer(private val instances : Map<String, Definition>, private val stage: () -> Stage) {

    private fun checkStage() {
        if (stage.invoke() == Stage.CREATING) throw RuntimeException("The referencer should not be used yet. Make sure it is only used within the Resolver context.")
        if (stage.invoke() == Stage.COMPLETED) throw RuntimeException("Resolving should be completed by now. Make sure all references are resolved during the constructor.")
    }

    fun definition(name : String) : Definition {
        checkStage()
        return instances[name] ?: throw ResolvingException("Definition named `$name` not found")
    }
    fun thing(name : String) = definition(name) as Thing
    fun action(name : String) = definition(name) as Action
    fun value(name : String) = definition(name) as Value
    fun enum(name : String) = definition(name) as Enum
    fun feature(name : String) = definition(name) as Feature
    fun auth(name : String) = definition(name) as Auth
    fun remote(name : String) = definition(name) as Remote

    /** Returns a list of all base, remote base action names. */
    fun baseNames() : List<String> {
        val names = instances.values.mapNotNull { if (it is Remote) it.baseAction else null }.map { it.name }
        return if (baseAction() != null) names.plus(baseAction()!!.name) else names
    }

    /** Returns the base action, or null if not found. Also throws an error if more than 1 is found. */
    fun baseAction() : Action? = expectZeroOrOne("Found more than 1 base action") { it is Action && it.isBase && it.remoteBaseOf == null } as Action?
    /** Returns the default remote, or null if not found. Also throws an error if more than 1 is found. */
    fun defaultRemote() : Remote? = expectZeroOrOne("Found more than 1 remote marked as default") { it is Remote && it.default } as Remote?
    /** Returns the default auth, or null if not found. Also throws an error if more than 1 is found. */
    fun defaultAuth() : Auth? = expectZeroOrOne("Found more than 1 auth marked as default") { it is Auth && it.default } as Auth?

    private fun expectZeroOrOne(errorMsg: String, predicate: (Definition) -> Boolean): Definition? {
        val results = instances.values.filter(predicate)
        if (results.size > 1) throw ResolvingException(errorMsg, *results.toTypedArray<Any>())
        return results.getOrNull(0)
    }

    /** Returns a set of all implementations of the interface with the name [name]. Does not include itself. [output] is used to avoid infinite loops during recursion, pass an empty set (or use default value) for the starting call.  */
    fun <I : Interface, S: Syncable<I>> implementationsOf(name: String, output: MutableSet<S> = mutableSetOf()) : MutableSet<S> {
        val def = definition(name)
        if (def !is Syncable<*>) throw ResolvingException("$name is not a syncable type")
        if (!def.isInterface) return output
        instances.values.filter { it is Syncable<*> && it.interfaces.any { it.name == name } }.forEach {
            val resolved = it as S
            if (output.add(resolved) && resolved.isInterface) {
                implementationsOf(resolved.name, output)
            }
        }
        return output
    }

}


class ResolvingException(msg: String = "", vararg elements: Any)
    : RuntimeException(toErrorMessage(msg, elements.filterNot { it is Throwable }), elements.find{ it is Throwable } as Throwable?)

private fun toErrorMessage(msg: String = "", elements: List<Any>) : String {
    return "Validation failed ... '$msg' ${
        if (elements.isNotEmpty()) {
            "\nRelated elements:\n" +
            elements.joinToString(separator = "\n") {
                "${it::class.simpleName}:{$it}:" + if (it is Property) " from ${it.data.source}" else ""
            }
        } else {
            ""
        }
    }"
}

/**
 * Same as [distinctBy] but when a duplicate is found, lets you choose which of the two you prefer
 */
inline fun <T, K> Iterable<T>.distinctBy(selector: (T) -> K, choose: (T, T) -> T): List<T> {
    val map = mutableMapOf<K,T>()
    for (e in this) {
        val key = selector(e)
        val present = map[key]
        if (present != null) {
            map[key] = choose.invoke(present, e)
        } else {
            map[key] = e
        }
    }
    return map.values.toList()
}

/**
 * This method or variable is only safe to access after resolving has completed.
 * Do not access it during the constructor or during [Resolver.resolve]
 */
annotation class OnlyAfterResolving