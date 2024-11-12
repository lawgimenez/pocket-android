package com.pocket.sync.util

import com.pocket.sync.type.*
import com.pocket.sync.type.path.Flavor
import com.pocket.sync.type.path.Reference
import kotlin.Comparator


/**
 * Contains utilities that may be useful for consumers of this module, such as code generation,
 * but are specific enough that they would just add clutter and noise to [Definition] or [Figments] if they were contained there.
 */



/**
 * Returns a new list, sorted such that fields that depend on other derived fields are after those it depends on.
 */
fun List<Field>.sortByDependencies() : List<Field> {
    return sortedWith(Comparator { f1, f2 ->
        // Does f1 depend on f2?
        var f1Siblings = false
        f1.derives.reactiveTo.map{ it.current }.forEach {
            if (it.reference.flavor == Flavor.SELF_FIELD && it.path?.syncable?.get(0)?.field == f2) {
                return@Comparator 1
            } else if (it.reference.flavor == Flavor.SELF) {
                f1Siblings = true
            }
        }
        // Does f2 depend on f1?
        var f2Siblings = false
        f2.derives.reactiveTo.map{ it.current }.forEach {
            if (it.reference.flavor == Flavor.SELF_FIELD && it.path?.syncable?.get(0)?.field == f1) {
                return@Comparator -1
            } else if (it.reference.flavor == Flavor.SELF) {
                f2Siblings = true
            }
        }
        if (f1Siblings == f2Siblings) {
            f1.name.compareTo(f2.name)
        } else {
            if (f1Siblings) 1 else -1
        }
    })
}


/**
 * @return [VarietyType]s that include the provided thing type as an option.
 * Note if this thing is an interface it does not include all of its implementations,
 * it only includes varieties that reference this type directly.
 */
fun Thing.varietiesIncluding() : Collection<Variety> {
    return schema.varieties().filter { it.options.any { o -> o.type == this } }
}

/**
 * Fields that are derived using [Remap] and require using this field's value
 */
fun Field.remappedBy() : Collection<Field> {
    return (this.context.current as Syncable<*>).fields.all.filter { it.derives.remap?.list?.current?.end()?.field == this }
}

/**
 * All reactive paths ([Derive.reactiveTo]) that reference this [Thing].
 * When interfaces are involved, the logic is a bit trickier and this includes paths that aren't
 * explicitly declared anywhere ([Derive.reactiveToDeclared]), but are implied by paths referencing
 * an interface that makes up this thing.
 *
 * ---
 * Explaining in more depth, with the following figments:
 *
 * ```
 * thing Cause {
 *    trigger : String
 *    effected_1 : String {
 *      -> .trigger
 *    }
 * }
 * thing Effect {
 *    effected_2 : String {
 *       (Cause.trigger)
 *    }
 *    effected_3 : String {
 *       (Cause)
 *    }
 * }
 * ```
 * Invoking this on `Cause` would return `[".trigger", "Cause.trigger", "Cause"]`
 *
 * If interfaces were involved, such as below:
 *
 * ```
 * interface thing Cause {
 *    trigger : String
 *    effected_1 : String {
 *      -> .trigger
 *      # This is a self reference and in some sense has no meaning until you look at an implementation of this.
 *      # When this is inherited, "self" will become that implementation.
 *      # For example, in {CauseImpl.effected_1} this translates to being reactive to {CauseImpl.trigger} only.
 *    }
 * }
 * thing CauseImpl : Cause
 *
 * thing Effect {
 *    effected_2 : String {
 *       (Cause.trigger)
 *       # Since {Cause} is an interface, this is also watching for changes to any implementations.
 *       # So this is equal to: (Cause.trigger, CauseImpl.trigger)
 *    }
 *    effected_3 : String {
 *       (Cause)
 *       # Since {Cause} is an interface, this is equal to (Cause, CauseImpl)
 *    }
 *    effected_4 : String {
 *       (CauseImpl)
 *       # Since {CauseImpl} is a concrete implementation, it is only watching changes to {CauseImpl}
 *    }
 * }
 * ```
 *
 * Invoking this on `Cause` would return `[".trigger", "Cause.trigger", "Cause"]`
 *
 * Invoking this on `CauseImpl` would return `[".trigger", "CauseImpl.trigger", "CauseImpl"]`
 *
 */
fun Thing.triggers() : List<Reference> {
    return schema.things()
        .asSequence()
        .flatMap { t -> t.fields.all }
        .flatMap { f -> f.derives.reactiveTo }
        .map { contextual -> contextual.current }
        .filter { ref -> ref.type == this }
        .distinctBy { it.reference.reference }
        .toList()
}

/**
 * All fields that contain this reference in their [Derive.reactiveTo].
 *
 * For example, with the following figments:
 *
 * ```
 * thing Reactive {
 *    observer : String {
 *      (Reaction.observed)
 *    }
 * }
 *
 * thing Reaction {
 *    observed : String
 * }
 * ```
 * Querying this method on a reference of `Reaction` would return `["Reactive.observed"]`.
 *
 *
 *
 * When interfaces are involved, the logic is a bit trickier and needs careful consideration:
 *
 * ```
 * interface thing Reactive {
 *    observerA : String {
 *      (Reaction.observed)
 *      # Since {Reaction} is an interface, this is also watching for changes to any implementations.
 *      # So this is equal to: (Reaction.observed, ReactiveImpl.observed)
 *    }
 *    observerB : String {
 *      (ReactionImpl.observed)
 *      # Since {ReactionImpl} is a concrete implementation, it is only watching changes to {ReactionImpl.observed}
 *    }
 *    observerC : String {
 *      (Reaction)
 *      # Since {Reaction} is an interface, this is equal to (Reaction, ReactionImpl)
 *    }
 *    observerD : String {
 *      (ReactionImpl)
 *      # Since {ReactionImpl} is a concrete implementation, it is only watching changes to {ReactionImpl}
 *    }
 *    observerE : String {
 *      (.observerD)
 *      # This is a self reference and in some sense has no meaning until you look at an implementation of this.
 *      # When this is inherited, "self" will become that implementation.
 *      # For example, in {ReactiveImpl.observerE} this translates to being reactive to {ReactiveImpl.observerD} only.
 *    }
 *    observerF : String {
 *      (.)
 *      # This is a self reference and in some sense has no meaning until you look at an implementation of this.
 *      # When this is inherited, "self" will become that implementation.
 *      # For example, in {ReactiveImpl.observerF} this translates to being reactive to any changes of {ReactiveImpl}.
 *    }
 * }
 * thing ReactiveImpl : Reactive {
 *    # This inherits all of the fields from {Reactive} and is reactive to the same changes.
 * }
 * interface thing Reaction {
 *    observed : String
 * }
 * thing ReactionImpl : Reaction
 * ```
 *
 * Walking through some examples:
 *
 * Querying this on a reference of `Reaction` would return `["Reactive.observerC", "ReactiveImpl.observerC"]`.
 *
 * `ReactionImpl` would return `["Reactive.observerC", "ReactiveImpl.observerC", "Reactive.observerD", "ReactiveImpl.observerD"]`.
 *
 * `Reaction.observed` would return `["Reactive.observerA", "ReactiveImpl.observerA"]`.
 *
 * `ReactionImpl.observed` would return `["Reactive.observerA", "ReactiveImpl.observerA", "Reactive.observerB", "ReactiveImpl.observerB"]`.
 *
 * `.observerD` within the context of a `Reactive` would return `["Reactive.observerE"]`.
 *
 * `.observerD` within the context of a `ReactiveImpl` would return `["ReactiveImpl.observerE"]`.
 *
 * `.` within the context of a `Reactive` would return `["Reactive.observerF"]`.
 *
 * `.` within the context of a `ReactiveImpl` would return `["ReactiveImpl.observerF"]`.
 *
 */
fun Reference.reactiveTo() : List<Field> = type.schema.syncables()
            .flatMap { t -> t.fields.all }
            .filter { f -> f.derives.reactiveTo.any { it.current.equalsTarget(this) } }
