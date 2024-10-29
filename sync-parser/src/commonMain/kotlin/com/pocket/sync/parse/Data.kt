package com.pocket.sync.parse

import com.pocket.sync.type.path.ReferenceData
import okio.Path


/**
 * The classes in this file are an in-memory representations of raw data from spec/schema files.
 * For historical reasons this representation is called Figment.
 * But it's kind of fitting that Figments only exist in the parser's imagination.. I mean memory!
 *
 * References, such as type of a field, are just strings and have not yet been resolved or validated.
 *
 * All properties should extend [PropertyData] and definitions [DefinitionData].
 *
 * All should be data classes and just hold values, leave most validation up to the concrete implements ([Definition]) to handle.
 *
 */


/**
 * Raw data from a [Parser].
 * References are unresolved and unvalidated.
 * Use [FigmentsData.resolve] to obtain the validated and resolved definitions.
 */
data class FigmentsData(val definitions : List<DefinitionData>) {
    operator fun plus(figments: FigmentsData) : FigmentsData = FigmentsData(definitions.plus(figments.definitions))
}



// Common


/**
 * The raw data of a property. Not yet validated or resolved.
 * When defining a new property you will also want to create a [com.pocket.sync.type.Property] for it.
 */
interface PropertyData {
    /** Where this property was found. */
    val source: Source

    // Enforce that implementations have equality checks based on their actual data. Since mostly using data classes, this should be built in.
    override fun equals(other: Any?) : Boolean
    override fun hashCode() : Int
}

interface DefinitionData : PropertyData {
    val definition: DefinitionProperties
}

/** A location in a schema file. */
data class Source(
    /** The line this property started on. 1..n based. */
    var startLine : Int,
    /** The last line this property spanned. 1..n based. */
    var endLine : Int,
    var path : Path
){
    fun lines() = endLine - startLine + 1
    fun isMultiline() = lines() > 1
    fun isSingleLine() = lines() == 1
    override fun toString() = "[${path.name}#L$startLine-$endLine]"
}

/** Common properties all definitions share. */
data class DefinitionProperties(
    val name : String,
    val deprecated: Boolean,
    val related: List<RelatedFeatureFlagData>,
    val description: List<DescriptionData>,
    override val source: Source
) : PropertyData

interface SyncableData : DefinitionData {
    val syncable: SyncableProperties
}

/**
 * Common properties all syncable definitions share.
 * @property operation A query or a mutation a GraphQL source can use to build a request.
 */
data class SyncableProperties(
    val auth: AuthFlagData?,
    val remote : RemoteFlagData?,
    val endpoint : EndpointFlagData?,
    val interfaces : List<String>,
    val isInterface : Boolean,
    val fields: List<FieldData>,
    override val source: Source,
    val operation : String? = null,
) : PropertyData




// Definitions

data class ThingData(
    override val definition : DefinitionProperties,
    override val syncable : SyncableProperties,
    val unique : UniqueFlagData?,
    override val source: Source = definition.source
) : SyncableData

data class ActionData(
    override val definition : DefinitionProperties,
    override val syncable : SyncableProperties,
    val isBase : Boolean,
    val priority: PriorityFlagData?,
    val effect: List<DescriptionData>,
    /** If this is a remote base action, the name of the remote it belongs to. */
    val remoteBaseOf: String?,
    val resolves: ResolvesFlagData?,
    override val source: Source = definition.source
) : SyncableData

data class AuthData(
    override val definition : DefinitionProperties,
    val default: Boolean,
    override val source: Source = definition.source
) : DefinitionData

data class RemoteData(
    override val definition : DefinitionProperties,
    val default: Boolean,
    val baseAction: ActionData?,
    override val source: Source = definition.source
) : DefinitionData

data class FeatureData(
    override val definition : DefinitionProperties,
    override val source: Source = definition.source
) : DefinitionData

data class ValueData(
    override val definition : DefinitionProperties,
    override val source: Source = definition.source
) : DefinitionData

data class EnumData(
    override val definition : DefinitionProperties,
    val options: List<EnumOptionData>,
    override val source: Source = definition.source
) : DefinitionData

data class VarietyData(
    override val definition : DefinitionProperties,
    val options : List<VarietyOptionData>,
    override val source: Source = definition.source
) : DefinitionData

data class SliceData(
    override val definition : DefinitionProperties,
    val rules : List<SliceRuleData>,
    override val source: Source = definition.source
) : DefinitionData



// Fields, Types and Values


data class FieldData(
    /** This will be the name, or if aliases are used, the first alias. */
    val name: String,
    val aliases: Map<String, String>,
    val type: FieldTypeData,
    val identifying: Boolean,
    val hashTarget: Boolean,
    val deprecated: Boolean,
    val localOnly: Boolean,
    val root: Boolean,
    val derives: List<DeriveData>,
    val description: List<DescriptionData>,
    override val source: Source
) : PropertyData

interface FieldTypeData
interface AllowedInCollectionTypeData : FieldTypeData

data class ReferenceTypeData(
    /** The name of the definition this field is a type of. */
    val definition : String,
    val flag : TypeFlag
) : FieldTypeData, AllowedInCollectionTypeData

data class ListTypeData(
    val inner : AllowedInCollectionTypeData,
    val flag : TypeFlag
) : FieldTypeData

data class MapTypeData(
    val inner : AllowedInCollectionTypeData,
    val flag : TypeFlag
) : FieldTypeData

enum class TypeFlag {
    UNSPECIFIED,
    REQUIRED,
    OPTIONAL
}

data class EnumOptionData(
    val value: String,
    val alias: String?,
    val deprecated: Boolean,
    val description: List<DescriptionData>,
    override val source: Source
) : PropertyData

data class VarietyOptionData(
    val type : String,
    val description: List<DescriptionData>,
    override val source: Source
) : PropertyData

data class SliceRuleData(
    val definition : String,
    val aspect : String?,
    val exclude: Boolean,
    val description: List<DescriptionData>,
    override val source: Source
) : PropertyData




// Body Properties

data class DescriptionData(
    val text: String,
    override val source: Source,
    val type: DescriptionType = DescriptionType.DESCRIPTION
) : PropertyData

enum class DescriptionType {
    DESCRIPTION,
    INSTRUCTION,
    EFFECT
}

// "Flag" below refers to many figment syntax rules that are one line and are added within definitions to set/add some additional property.

/**
 * Represents a related feature that can appear in definitions: `^ Feature`
 * Indicates this feature is related to the definition.
 */
data class RelatedFeatureFlagData(
    /** The [Feature.name] */
    val name : String,
    override val source : Source
) : PropertyData

/** Represents an auth flag set on a definition: `! Auth` */
data class AuthFlagData(
    /** The [Auth.name] */
    val name : String,
    override val source : Source
) : PropertyData

/** Represents a remote flag set on a definition: `> Remote` */
data class RemoteFlagData(
    /** The [Remote.name] */
    val name : String,
    override val source : Source
) : PropertyData

/** Represents a unique flag set on a thing: `=` */
data class UniqueFlagData(
    override val source : Source
) : PropertyData

/** Represents an endpoint flag set on a definition: `@ endpoint ` */
data class EndpointFlagData(
    val address : String,
    val method : String?,
    override val source : Source
) : PropertyData

/** Represents a priority flag set on an action: `* ASAP ` */
data class PriorityFlagData(
    val priority: String,
    override val source: Source
) : PropertyData

/** Represents a resolve/return type for an action: `-> Type` */
data class ResolvesFlagData(
    val type: FieldTypeData,
    override val source: Source
) : PropertyData

interface DeriveData : PropertyData

data class FirstAvailableData(
    val options : List<ReferenceData>,
    override val source : Source
) : DeriveData

data class InstructionsData(
    val instruction: DescriptionData,
    override val source : Source
) : DeriveData

data class ReactivesData(
    val reactives : List<ReferenceData>,
    override val source : Source
) : DeriveData

data class RemapData(
    val list : String,
    val field: String,
    override val source : Source
) : DeriveData

/**
 * Info about the definition a property belongs to.  For example, if a property is a field, the definition that field is in.
 * Why not just use a string? Mostly so this concept can be typed and explained once here rather than everywhere a string version of this would appear.
 * @param definitionName The [DefinitionProperties.name] of the definition this property belongs to.
 * @see com.pocket.sync.type.Context
 */
data class ContextData(val definitionName : String)