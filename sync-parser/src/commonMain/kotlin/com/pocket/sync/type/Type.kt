package com.pocket.sync.type

import com.pocket.sync.parse.OnlyAfterResolving
import com.pocket.sync.parse.TypeFlag

/** Represents something that can be a [Field.type] */
interface FieldType {
    val flag : TypeFlag
}

interface CollectionType : FieldType {
    val inner : AllowedInCollectionType
}

interface AllowedInCollectionType : FieldType

/** A type that allows many possible [Thing] types, not just one like other types. */
interface OpenType : AllowedInCollectionType {
    /** All of the [Thing]s that are supported in this field. */
    @OnlyAfterResolving
    fun compatible() : Set<Thing>
}

interface DefinitionType<D : StatefulDefinition> : FieldType, AllowedInCollectionType {
    val definition: D
}

/** A [Definition] that holds or represents state. Such as [Value], [Thing], [Enum]. */
interface StatefulDefinition : Definition



// The possible types:

data class ReferenceType<D : StatefulDefinition>(
    override val definition : D,
    override val flag : TypeFlag
) : DefinitionType<D>

data class ListType(
    override val inner : AllowedInCollectionType,
    override val flag : TypeFlag
) : CollectionType

data class MapType(
    override val inner : AllowedInCollectionType,
    override val flag : TypeFlag
) : CollectionType

data class InterfaceType(
    override val definition : ThingInterface,
    override val flag : TypeFlag
) : DefinitionType<ThingInterface>, OpenType {
    override fun compatible(): Set<Thing> = definition.compatible.map { it as Thing }.toSet()
}

data class VarietyType(
    override val definition : Variety,
    override val flag : TypeFlag
) : DefinitionType<Variety>, OpenType {
    override fun compatible() = definition.compatible
}
