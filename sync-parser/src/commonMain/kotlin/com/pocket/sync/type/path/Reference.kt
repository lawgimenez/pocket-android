package com.pocket.sync.type.path

import com.pocket.sync.Figments
import com.pocket.sync.parse.*
import com.pocket.sync.type.*
import com.pocket.sync.type.Enum


/**
 * A reference to a definition or a specific field, parameter or value within a definition.
 * See figment spec on References for syntax and rules.
 *
 * <pre>
 * Type			type will be non-null, path will be null
 * Type.field	both type and path will be non null
 * .field		type will be null, path non null
 * .			both type and path will be null
 * </pre>
 *
 * This is just the raw data, see [Reference] for a validated/resolved version.
 *
 * @param text The reference text, such as "Type.path.path"
 * @param context The [Definition.name] of the definition this reference was found in. For example, if this reference was used in a description for a Thing, this is the name of that thing. This is used to resolve the parent for paths that don't specify a type. Such as ".field.path"
 */
data class ReferenceData(val text: String, val context: ContextData) {

    constructor(type: String?, path: Path?, context: ContextData) : this(type + (path ?: ""), context)

    /** The full reference string such as "Type.path" */
    val reference = text.trim()

    /** The type part, or null if its a path that doesn't declare a Type  */
    val type: String?

    /** The path part, or null if there is no path or if the path was just '.'  */
    val path: Path?

    val flavor: Flavor

    init {
        // Parse the text into Type.Path
        if (reference[0] != '.') {
            if (reference.contains(".")) {
                flavor = Flavor.TYPE_FIELD
                type = reference.substringBefore(".")
                path = Path.from("." + reference.substringAfter("."))
            } else {
                flavor = Flavor.TYPE
                type = reference
                path = null
            }
        } else if (reference == ".") {
            flavor = Flavor.SELF
            type = null
            path = null
        } else {
            flavor = Flavor.SELF_FIELD
            type = null
            path = Path.from(reference)
        }
    }

    override fun toString(): String = reference

    fun equalsTarget(other: ReferenceData): Boolean {
        if (this === other) return true
        if (reference != other.reference) return false
        if (flavor != other.flavor) return false
        return when (flavor) { // Context is only important if this is a self reference
            Flavor.SELF, Flavor.SELF_FIELD -> context == other.context
            else -> true
        }
    }
}

enum class Flavor {
    /** References a type, such as "Type" */
    TYPE,
    /** References a field in a specific type, such as "Type.field" */
    TYPE_FIELD,
    /** References a field within the context, such as ".field" */
    SELF_FIELD,
    /** References the context, such as "." */
    SELF
}


/**
 * A resolved reference
 */
class Reference(val reference: ReferenceData, resolver: Resolver) {
    /**
     * Helper for creating from a reference string.
     * @param context See [ReferenceData.context]
     */
    constructor(reference: String, context: ContextData, resolver: Resolver) : this(ReferenceData(reference, context), resolver)

    /** If you want to create an instance later on, after resolving, use this constructor. (During resolving, use the primary constructor) */
    constructor(reference: String, context: ContextData, figments: Figments) : this(ReferenceData(reference, context), FigmentsResolver(figments)) { path }
    /** If you want to create an instance later on, after resolving, use this constructor. (During resolving, use the primary constructor) */
    constructor(context: Definition, path: com.pocket.sync.type.path.Path) : this(path.toString(), ContextData(context.name), context.schema)

    val type: Definition by resolver.resolve(this) { definition(reference.type ?: reference.context.definitionName) }
    val path: Path? by resolver.resolve(this) {
        if (reference.path != null) {
            val def = type
            when (def) {
                is Enum -> Path(enum = def.options.find { it.name == reference.path.parts[0].value })
                !is Syncable<*> -> throw ResolvingException("path not expected for definition type in reference", reference)
                else -> {
                    val typedPath = mutableListOf<ReferenceSegment>()
                    for ((i, segment) in reference.path.parts.withIndex()) {
                        val parent = if (i > 0) typedPath[i - 1] else null
                        var root: Definition?
                        var field: Field?
                        var segmentType: FieldType
                        var mode: ReferenceSegment.Mode

                        if (i == 0) {
                            // First segment must refer to a field
                            root = def
                            if (segment.type != PathSegmentType.FIELD) throw ResolvingException("invalid path. must begin with a field or parameter", reference)
                            mode = ReferenceSegment.Mode.FIELD
                            if (def !is Syncable<*>) throw ResolvingException("unexpected root while parsing", reference, type)
                            field = def.fields.all.find { it.name == segment.value }
                            if (field == null) throw ResolvingException("did not find parameter $segment", reference, type)
                            segmentType = field.type

                        } else {
                            // Deeper paths could be a field or collection
                            when (segment.type) {
                                PathSegmentType.FIELD -> {
                                    if (parent != null && parent.type is CollectionType) {
                                        root = (parent.type.inner as ReferenceType<*>).definition
                                        mode = ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD
                                    } else {
                                        root = (parent!!.type as ReferenceType<*>).definition as Thing
                                        mode = ReferenceSegment.Mode.FIELD
                                    }
                                    field =
                                        (root as Thing).fields.all.find { it.name == segment.value } // TODO this was cast to Thing in java, why?
                                    if (field == null) throw ResolvingException("did not find field " + segment + " within " + root.name + " while parsing " + reference)
                                    segmentType = field.type
                                }
                                PathSegmentType.MAP_KEY, PathSegmentType.ARRAY_INDEX -> {
                                    root = parent!!.root
                                    field = parent.field
                                    segmentType = (parent.type as CollectionType).inner
                                    mode = ReferenceSegment.Mode.COLLECTION_KEY
                                }
                            }
                        }
                        typedPath.add(
                            ReferenceSegment(
                                mode,
                                root,
                                parent,
                                segment,
                                field,
                                segmentType
                            )
                        )
                    }
                    Path(typedPath.toList())
                }
            }
        } else {
            null
        }
    }

    /** The last segment of the path. */
    fun end() : ReferenceSegment? = path?.syncable?.lastOrNull()
    /** The first segment of the path. */
    fun start() : ReferenceSegment? = path?.syncable?.firstOrNull()
    fun toPath() = end()?.segment?.path()
    fun flavor() = reference.flavor

    /**
     * Splits this reference into multiple references where there are [ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD] segments.
     * For example, if this reference is to `.collection.field`  where `collection` is an array of things, this would return a list of two references:
     *
     * `.collection`  the first points to the collection of things
     * `.field`       the second is a reference starting within that thing to the field
     */
    fun splitCollectionSearches(): List<Reference> {
        if (type !is Syncable<*> || path?.syncable == null) return listOf(this)

        var root = this
        val split = mutableListOf<Reference>()
        path?.syncable?.forEach {
            if (it.mode == ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD) {
                val collection = it.parent!!
                // Make a new reference that only goes up to this field
                val upTo = Reference(
                    root.reference.toString().substring(0, collection.segment.path().toString().length),
                    ContextData(root.type.name),
                    root.type.schema
                )

                // Make a new reference that starts at the Thing that this collection is made of
                val definition = ((collection.type as CollectionType).inner as DefinitionType<*>).definition // can assume it is a wrapping type of a definition, otherwise it is an invalid reference. Paths aren't allowed to go through a collection of open types.
                val after = Reference(
                    root.reference.toString().substring(collection.segment.path().toString().length),
                    ContextData(definition.name),
                    definition.schema
                )
                split.add(upTo)
                root = after
            }
        }
        split.add(root)
        return split
    }

    /**
     * @return this path split by any [ReferenceSegment.Mode.COLLECTION_KEY]s
     * The first path will start at a thing like normal, but if this returns more than one entry,
     * the subsequent entries will start at a segment whose parent is a [ReferenceSegment.Mode.COLLECTION_KEY].
     * If this contains any [ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD] an error will be thrown.
     */
    fun splitCollections(): List<List<ReferenceSegment>> {
        val split = mutableListOf<List<ReferenceSegment>>()
        val segments = mutableListOf<ReferenceSegment>()
        for (s in path!!.syncable!!) {
            if (s.mode == ReferenceSegment.Mode.COLLECTION_SEARCH_FIELD) throw RuntimeException("unsupported collection search.")
            if (s.parent != null && s.parent.mode == ReferenceSegment.Mode.COLLECTION_KEY) {
                split.add(segments.toList())
                segments.clear()
            }
            segments.add(s)
        }
        if (segments.isNotEmpty()) {
            split.add(segments.toList())
        }
        return split.toList()
    }

    override fun toString() = reference.toString()
    override fun hashCode(): Int = reference.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Reference
        if (reference != other.reference) return false
        return true
    }

    fun equalsTarget(other: Reference) = reference.equalsTarget(other.reference)

    /**
     * The path info, only one of [syncable] or [enum] will be non-null, depending on the context.
     */
    data class Path(val syncable: List<ReferenceSegment>? = null, val enum: EnumOption? = null) {
        init {
            if (syncable == null && enum == null) throw ResolvingException("both may not be null") // Just have a null path instead.
        }
    }
}



/**
 * A [PathSegment] with concrete info about its field and type.
 */
class ReferenceSegment (
    /** A description this segment  */
    val mode : Mode,
    /** The closest [Thing] (or [com.pocket.sync.type.Action] if this is a parameter) in this path. Essentially, the thing that the field belongs to.  */
    val root: Definition,
    /** The parent segment, or null if this is the first segment in a path.  */
    val parent: ReferenceSegment?,
    /** Path info about this segment  */
    val segment: PathSegment,
    /** The field this segment belongs to.  */
    val field: Field,
    /** The value type of this segment. For field segments, this is the value of the field. For collection key segments, this is the inner value of the collection.  */
    val type: FieldType
) {

    enum class Mode {
        /** This segment is a field or parameter  */
        FIELD,

        /** This segment is a field on a collection, used in a reactive path to indicate checking if any thing in a collection had a specific kind of change. For example if a field `a` was an array and a reactive path was `a.b`. See reactive figment docs for more details.  */
        COLLECTION_SEARCH_FIELD,

        /** This segment is a map key or array index  */
        COLLECTION_KEY
    }

    override fun toString(): String = segment.toString()
    override fun hashCode(): Int = segment.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ReferenceSegment
        if (segment != other.segment) return false
        return true
    }

}

private class FigmentsResolver(private val figments: Figments) : Resolver {
    private val referencer = Referencer(figments.all().associateBy { it.name }) { Stage.RESOLVING }
    override fun <R> resolve(context: Any, initializer: Referencer.() -> R): Lazy<R> = lazy { with(referencer, (initializer)) }
    override fun schema() = figments
}
