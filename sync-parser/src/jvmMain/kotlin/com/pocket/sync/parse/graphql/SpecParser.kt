package com.pocket.sync.parse.graphql

import com.apollographql.apollo3.ast.*
import com.pocket.sync.parse.*
import com.pocket.sync.parse.DescriptionType.EFFECT
import com.pocket.sync.parse.DescriptionType.INSTRUCTION
import com.pocket.sync.type.path.ReferenceData
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.source

@Suppress("SpellCheckingInspection")
private const val SPEC_FILE_EXTENSION = "graphqls"

/** Parse GraphQL schemas into the Figment models that the code generation understands. */
class SpecParser : Parser {
    override fun parse(fs: FileSystem, path: Path): FigmentsData {
        return fs.allWithExtension(path, SPEC_FILE_EXTENSION)
            .flatMap {
                fs.read(it) { parseAsGQLDocument(it.toString()).valueAssertNoErrors().definitions }
            }
            .let { parsed -> FigmentsData(parsed.toFigment()) }
    }

    override fun parse(text: String): FigmentsData {
        val source = text.byteInputStream().source().buffer()
        val parsed = source.parseAsGQLDocument().valueAssertNoErrors().definitions
        return FigmentsData(parsed.toFigment())
    }
}

/** Map parsed GraphQL definitions into the Figment models. */
private fun List<GQLDefinition>.toFigment(): List<DefinitionData> {
    val definitions = groupByName()

    val remotes = Remotes()
    for ((name, nodes) in definitions) {
        val schema = nodes.firstOrNull() as? GQLSchemaExtension ?: continue
        val filePath = schema.sourceLocation.filePath ?: continue
        remotes[filePath] = name
    }

    return definitions.values
        .map { nodes ->
            // Usually there's one node with any given name, so it's enough to look at the first
            // (and only) node.
            // Extensions are exceptions. For example when a type is defined in the main Pocket Graph
            // file, but we need to add some properties related to local state or the V3 API,
            // then for one name we'll have both the Pocket Graph definition and an extension.
            // In this case we'll try to merge them into one definition, before converting to Figment.

            // How GraphQL naming matches Figment naming:
            // * object types and input types are things,
            // * interfaces are also things
            //   (or specifically thing interfaces, modelled with the same data class
            //    with an isInterface = true flag),
            // * enums are.. enums (yay!),
            // * unions are varieties,
            // * scalars are values,
            // * mutations[1] are actions ([1]: or more specifically: fields of the Mutation type),
            // * it's a bit of a hack, but we've used schema extensions to define remotes
            //   with their base actions per file.
            when (val node = nodes.first()) {
                is GQLSchemaExtension -> node.toRemoteData()
                is GQLObjectTypeDefinition, is GQLObjectTypeExtension -> {
                    val definition = nodes.definition<GQLObjectTypeDefinition>()
                    val extension = nodes.filterIsInstance<GQLObjectTypeExtension>().singleOrNull()
                    definition.mergeWith(extension).toThingData(remotes.forNode(definition))
                }
                is GQLInputObjectTypeDefinition -> {
                    // Input objects are usually just Figment things.
                    // But for Snowplow we've made one exception, where a union type is defined
                    // as an input type, so have to handle this here.
                    if (node.directives.any { it.name == "variety" }) {
                        node.toVarietyData()
                    } else {
                        node.toThingData(remotes.forNode(node))
                    }
                }
                is GQLInterfaceTypeDefinition -> node.toThingData()

                is GQLEnumTypeDefinition, is GQLEnumTypeExtension -> {
                    val definition = nodes.definition<GQLEnumTypeDefinition>()
                    val extension = nodes.filterIsInstance<GQLEnumTypeExtension>().singleOrNull()
                    definition.mergeWith(extension).toEnumData()
                }

                is GQLUnionTypeDefinition -> node.toVarietyData()
                is GQLScalarTypeDefinition -> node.toValueData()

                is GQLFieldDefinition -> {
                    node.toActionData(remotes.forNode(node))
                }
                else -> TODO("${node.name} is of unsupported type ${node.javaClass.simpleName} at ${node.sourceLocation.pretty()}")
            }
        }
        .plus(
            // Hardcoding some Figment definitions the code generation expects,
            // but couldn't or didn't want to include in schema files for one reason or another.
            listOf(
                // Add built-in GraphQL scalars.
                ValueData("Integer".toDefinitionProperties()),
                ValueData("String".toDefinitionProperties()),
                ValueData("Boolean".toDefinitionProperties()),
                ValueData("ID".toDefinitionProperties()),
                ValueData("Float".toDefinitionProperties()),
                // Add a base action which adds a time field to all actions.
                baseAction(),
                // Add auth type definitions.
                AuthData("NoAuth".toDefinitionProperties(), true),
                AuthData("GuidAuth".toDefinitionProperties(), false),
                AuthData("UserAuth".toDefinitionProperties(), false),
                AuthData("UserOptionalAuth".toDefinitionProperties(), false),
                AuthData("LoginAuth".toDefinitionProperties(), false),
                AuthData("AccountModAuth".toDefinitionProperties(), false),
                // Add Pocket Graph/Client API remote, since it's not defined in the file.
                RemoteData(
                    definition = "CLIENT_API".toDefinitionProperties(),
                    default = true,
                    baseAction = null,
                ),
            )
        )
}

/**
 * A [MutableMap] specialised for definitions of "remotes" and with convenience "getters".
 *
 * There is an assumption here that the Pocket Graph (fka Client API) file is the only one that
 * doesn't have a remote declaration. On the other hand if there's no file at all (like in tests)
 * then we don't want to return CLIENT_API by default.
 */
private class Remotes : LinkedHashMap<String, String>() {
    fun forFile(path: String?) = if (path == null) null else getOrDefault(path, "CLIENT_API")
    fun forNode(node: GQLNode) = forFile(node.sourceLocation.filePath)
}

/**
 * Group definitions by their name, so it's easier to merge type declarations with their extensions.
 */
private fun List<GQLDefinition>.groupByName(): Map<String, List<GQLNode>> {

    // Local method to make easier to build the map with expected default values.
    fun MutableMap<String, List<GQLNode>>.addToKey(key: String, element: GQLNode) {
        val list = getOrDefault(key, emptyList())
        put(key, list + element)
    }

    return buildMap {
        for (definition in this@groupByName) {
            when (definition) {
                is GQLSchemaExtension -> {
                    // Find out the remote for this file.
                    val remote = definition.directives.find("remote", "name").singleOrNull()
                    if (remote != null) {
                        addToKey(remote.value, definition)
                    }
                }

                is GQLObjectTypeDefinition -> addToKey(definition.name, definition)
                is GQLObjectTypeExtension -> {
                    when (definition.name) {
                        "Mutation" -> {
                            // Legacy actions.
                            for (field in definition.fields) {
                                addToKey(field.name, field)
                            }
                        }
                        "Query" -> {
                            throw AssertionError("Expected that Pocket Graph declares all queries in Query definition (not extensions).")
                        }
                        else -> {
                            // Extensions of Pocket Graph types.
                            addToKey(definition.name, definition)
                        }
                    }
                }

                is GQLInputObjectTypeDefinition -> addToKey(definition.name, definition)
                is GQLInterfaceTypeDefinition -> addToKey(definition.name, definition)
                is GQLUnionTypeDefinition -> addToKey(definition.name, definition)
                is GQLScalarTypeDefinition -> addToKey(definition.name, definition)

                is GQLEnumTypeDefinition -> addToKey(definition.name, definition)
                is GQLEnumTypeExtension -> addToKey(definition.name, definition)

                is GQLDirectiveDefinition, is GQLSchemaDefinition -> {} // Ignore.
                else -> TODO("${definition.name} is of unsupported type ${definition.javaClass.simpleName} at ${definition.sourceLocation.pretty()}")
            }
        }
    }
}

private inline fun <reified Definition : GQLNamed> List<GQLNode>.definition(): Definition {
    val definitions = filterIsInstance<Definition>()
    return when (definitions.size) {
        1 -> definitions[0]
        else -> {
            val message = "${definitions[0].name} defined more than once. Choose one primary definition and convert others to extensions."
            throw IllegalArgumentException(message)
        }
    }
}

/** Merge a type definition with its extension. */
private fun GQLObjectTypeDefinition.mergeWith(
    extension: GQLObjectTypeExtension?,
): GQLObjectTypeDefinition {
    return if (extension == null) {
        this
    } else {
        // Grab fields extended by the @extend directive.
        val (extendedFieldList, otherDirectives) = extension.directives
            .partition { it.name == "extend" }
        val extendedFields = extendedFieldList.mapNotNull { it.arguments?.arguments }
            .associateBy { arguments ->
                (arguments.single { it.name == "field" }.value as GQLStringValue).value
            }
        // Grab fields extended by adding an alias and new fields not present in the definition.
        val (aliasedFieldList, addedFields) = extension.fields
            .partition { it.directives.find("figment", "client_api_alias").any() }
        val aliasedFields = aliasedFieldList.associateBy { field ->
            field.directives
                .find("figment", "client_api_alias")
                .map { it.value }
                .single()
        }
        // Make a copy of the definition with the necessary changes to merge properties from the extension.
        this.copy(
            directives = directives + otherDirectives,
            fields = fields
                .map { field ->
                    // Merge fields extended by directive.
                    val extended = extendedFields[field.name]
                    if (extended != null) {
                        val id = extended.singleOrNull { it.name == "id" }
                        val reactive = extended.singleOrNull { it.name == "reactive" }
                        val instructions = extended.singleOrNull { it.name == "instructions" }
                        return@map field.copy(
                            directives = buildList {
                                addAll(field.directives)
                                if (id != null && (id.value as GQLBooleanValue).value) {
                                    add(id.toDirective("id", arguments = null))
                                }
                                if (reactive != null) {
                                    add(reactive.toDirective("derives"))
                                }
                                if (instructions != null) {
                                    add(instructions.toDirective("derives"))
                                }
                            }
                        )
                    }
                    // Merge aliased fields.
                    val aliased = aliasedFields[field.name]
                    if (aliased != null) {
                        return@map field.copy(
                            name = aliased.name,
                            directives = field.directives + aliased.directives,
                        )
                    }
                    // If the field has neither an extension nor an alias, just return it unchanged.
                    return@map field
                }
                .plus(addedFields) // Add fields not present in the original declaration.
        )
    }
}

private fun GQLArgument.toDirective(
    name: String,
    arguments: GQLArguments? = GQLArguments(listOf(this)),
): GQLDirective {
    return GQLDirective(sourceLocation, name, arguments)
}

/** Merge an enum definition with its extension. */
private fun GQLEnumTypeDefinition.mergeWith(
    extension: GQLEnumTypeExtension?,
): GQLEnumTypeDefinition {
    return if (extension == null) {
        this
    } else {
        val (valueDirectives, otherDirectives) =
            extension.directives.partition { it.name == "enum_value" }
        val values = valueDirectives.mapNotNull { it.arguments?.arguments }
            .associateBy(
                keySelector = { arguments ->
                    val name = arguments.single { it.name == "name" }
                        .value as GQLStringValue
                    name.value
                },
                valueTransform = { arguments ->
                    arguments.single { it.name == "value" }.value as GQLStringValue
                }
            )
        copy(
            directives = directives + otherDirectives,
            enumValues = enumValues.map {
                val value = values[it.name]
                if (value == null) {
                    it
                } else {
                    it.copy(
                        directives = it.directives + GQLDirective(
                            sourceLocation = extension.sourceLocation,
                            name = "figment",
                            arguments = GQLArguments(
                                arguments = listOf(
                                    GQLArgument(
                                        name = "enum_value",
                                        value = value
                                    )
                                )
                            )
                        )
                    )
                }
            }
        )
    }
}

/** Types are Figment things. */
private fun GQLObjectTypeDefinition.toThingData(remote: String?): ThingData {
    val context = ContextData(name)
    val typeSource = sourceLocation.toSource()
    return ThingData(
        definition = toDefinitionProperties(),
        syncable = SyncableProperties(
            auth = directives.find("figment", "auth")
                .map { AuthFlagData(it.value, it.sourceLocation.toSource()) }
                .singleOrNull(),
            remote = remote?.let { RemoteFlagData(it, typeSource) },
            endpoint = directives.find("figment", "address")
                .map { EndpointFlagData(it.value, null, it.sourceLocation.toSource()) }
                .singleOrNull(),
            interfaces = implementsInterfaces,
            isInterface = false,
            fields = fields.map { it.toFieldData(context) },
            source = typeSource,
        ),
        unique = directives.singleOrNull { it.name == "unique" }
            ?.let { UniqueFlagData(it.sourceLocation.toSource()) },
    )
}

/** Input types are also Figment things. (Figment allows normal things as inputs.) */
private fun GQLInputObjectTypeDefinition.toThingData(remote: String?): ThingData {
    val typeSource = sourceLocation.toSource()
    return ThingData(
        definition = this.toDefinitionProperties(),
        syncable = SyncableProperties(
            auth = directives.find("figment", "auth")
                .map { AuthFlagData(it.value, it.sourceLocation.toSource()) }
                .singleOrNull(),
            remote = remote?.let { RemoteFlagData(it, typeSource) },
            endpoint = directives.find("figment", "address")
                .map { EndpointFlagData(it.value, null, it.sourceLocation.toSource()) }
                .singleOrNull(),
            interfaces = emptyList(),
            isInterface = false,
            fields = inputFields.map { it.toFieldData() },
            source = typeSource,
        ),
        unique = directives.singleOrNull { it.name == "unique" }
            ?.let { UniqueFlagData(it.sourceLocation.toSource()) },
    )
}

/** Interfaces are also Figment things, with an isInterface = true flag. */
private fun GQLInterfaceTypeDefinition.toThingData(): ThingData {
    val context = ContextData(name)
    return ThingData(
        definition = this.toDefinitionProperties(),
        syncable = SyncableProperties(
            auth = directives.find("figment", "auth")
                .map { AuthFlagData(it.value, it.sourceLocation.toSource()) }
                .singleOrNull(),
            remote = null,
            endpoint = directives.find("figment", "address")
                .map { EndpointFlagData(it.value, null, it.sourceLocation.toSource()) }
                .singleOrNull(),
            interfaces = implementsInterfaces,
            isInterface = true,
            fields = fields.map { it.toFieldData(context) },
            source = sourceLocation.toSource(),
        ),
        unique = null,
    )
}

/** Fields are also called fields in Figment. */
private fun GQLFieldDefinition.toFieldData(context: ContextData): FieldData {
    return FieldData(
        name = directives.find("figment", "name")
            .map { it.value }
            .singleOrNull()
            ?: name,
        aliases = buildMap {
            directives.find("figment", "v3_alias").singleOrNull()?.let {
                put("Local", name)
                put("V3", it.value)
            }
            directives.find("figment", "client_api_alias").singleOrNull()?.let {
                put("V3", name)
                put("CLIENT_API", it.value)
            }
        },
        type = type.toFieldTypeData(isMap = directives.any { it.name == "map" }),
        identifying = directives.any { it.name == "id" },
        hashTarget = directives.any { it.name == "hash_target" },
        deprecated = deprecated,
        localOnly = directives.any { it.name == "local" },
        root = directives.any { it.name == "root_value" },
        derives = directives
            .asSequence()
            .filter { it.name == "derives" }
            .mapNotNull { it.arguments?.arguments }
            .flatten()
            .map { argument ->
                val argumentSource = argument.sourceLocation.toSource()
                when (argument.name) {
                    "first_available" -> {
                        FirstAvailableData(
                            argument.values.map { ReferenceData(it.value, context) },
                            argumentSource
                        )
                    }
                    "remap" -> {
                        val remap = (argument.value as GQLStringValue).value.split('.')
                        RemapData(remap[0], remap[1], argumentSource)
                    }
                    "reactive" -> {
                        ReactivesData(
                            argument.values.map { ReferenceData(it.value, context) },
                            argumentSource
                        )
                    }
                    "instructions" -> {
                        val instructions = (argument.value as GQLStringValue).value
                        InstructionsData(
                            DescriptionData(instructions, argumentSource, INSTRUCTION),
                            argumentSource
                        )
                    }
                    else -> throw AssertionError("derives' arguments are just: first_available, remap, reactive, instructions'")
                }
            }
            .toList(),
        description = buildList {
            description?.let { add(DescriptionData(it, sourceLocation.toSource())) }
        },
        source = sourceLocation.toSource()
    )
}

/** Unions are Figment varieties. */
private fun GQLUnionTypeDefinition.toVarietyData(): VarietyData {
    return VarietyData(
        definition = this.toDefinitionProperties(),
        options = memberTypes.map {
            VarietyOptionData(
                type = it.name,
                description = emptyList(),
                source = it.sourceLocation.toSource()
            )
        }
    )
}

/** An exception for Snowplow where we defined a union as an input type with special directives. */
private fun GQLInputObjectTypeDefinition.toVarietyData(): VarietyData {
    return VarietyData(
        definition = this.toDefinitionProperties(),
        options = inputFields.map { field ->
            val type = field.directives
                .find("figment", "name")
                .map { it.value }
                .singleOrNull()
                ?: field.name
            VarietyOptionData(
                type = type,
                description = emptyList(),
                source = field.sourceLocation.toSource()
            )
        }
    )
}

/** Fields of the Mutation type are Figment actions. */
private fun GQLFieldDefinition.toActionData(remote: String?): ActionData {
    val fieldSource = sourceLocation.toSource()
    return ActionData(
        definition = this.toDefinitionProperties(),
        syncable = SyncableProperties(
            auth = directives.find("figment", "auth")
                .map { AuthFlagData(it.value, it.sourceLocation.toSource()) }
                .singleOrNull(),
            remote = remote?.let { RemoteFlagData(it, fieldSource) },
            endpoint = directives.find("figment", "address")
                .map { EndpointFlagData(it.value, null, it.sourceLocation.toSource()) }
                .singleOrNull(),
            interfaces = emptyList(),
            isInterface = false,
            fields = arguments.map { it.toFieldData() },
            source = fieldSource,
        ),
        isBase = false,
        priority = directives.find("figment", "priority")
            .map { PriorityFlagData(it.value, it.sourceLocation.toSource()) }
            .singleOrNull(),
        effect = directives.find("figment", "effect")
            .map { DescriptionData(it.value, it.sourceLocation.toSource(), EFFECT) }
            .toList(),
        remoteBaseOf = null,
        resolves = type.toFieldTypeData()
            .let {
                if (it is ReferenceTypeData && it.definition == "Void") {
                    null
                } else {
                    ResolvesFlagData(it, fieldSource)
                }
            },
    )
}

/** Inputs of Mutation fields are action fields. */
private fun GQLInputValueDefinition.toFieldData(): FieldData {
    return FieldData(
        name = name,
        aliases = emptyMap(),
        type = type.toFieldTypeData(),
        identifying = directives.any { it.name == "id" },
        hashTarget = directives.any { it.name == "hash_target" },
        deprecated = deprecated,
        localOnly = directives.any { it.name == "local" },
        root = directives.any { it.name == "root_value" },
        derives = emptyList(),
        description = buildList {
            description?.let { add(DescriptionData(it, sourceLocation.toSource())) }
        },
        source = sourceLocation.toSource()
    )
}

/** Enums are enums. Phew! */
private fun GQLEnumTypeDefinition.toEnumData(): EnumData {
    return EnumData(
        definition = this.toDefinitionProperties(),
        options = enumValues.map { option ->
            val explicitValue = option.directives
                .find("figment", "enum_value")
                .map { it.value }
                .singleOrNull()
            val name = option.directives
                .find("figment", "name")
                .map { it.value }
                .singleOrNull()
                ?: option.name
            val value: String
            val alias: String?
            if (explicitValue != null) {
                // If there is an explicit value defined in a directive, treat name as an alias.
                value = explicitValue
                alias = name
            } else {
                // Otherwise treat name as the value and there is no alias.
                value = name
                alias = null
            }
            EnumOptionData(
                value = value,
                alias = alias,
                deprecated = option.deprecated,
                description = buildList {
                    option.description?.let { add(DescriptionData(it, option.sourceLocation.toSource())) }
                },
                source = option.sourceLocation.toSource(),
            )
        },
    )
}

/** Scalars are Figment values. */
private fun GQLScalarTypeDefinition.toValueData(): ValueData {
    return ValueData(definition = this.toDefinitionProperties())
}

/** We use schema extension to define Figment remotes. */
private fun GQLSchemaExtension.toRemoteData(): RemoteData {
    val remote = directives.find("remote", "name").single()
    val baseActionName = directives.find("base_action", "name").singleOrNull()
    val baseAction = if (baseActionName == null) {
        null
    } else {
        ActionData(
            definition = DefinitionProperties(
                name = baseActionName.value,
                deprecated = false,
                related = emptyList(),
                description = emptyList(),
                source = baseActionName.sourceLocation.toSource(),
            ),
            syncable = SyncableProperties(
                auth = null,
                remote = null,
                endpoint = null,
                interfaces = emptyList(),
                isInterface = true,
                fields = directives.filter { it.name == "base_action_field" }
                    .mapNotNull { it.toFieldData() },
                source = baseActionName.sourceLocation.toSource(),
            ),
            isBase = true,
            priority = null,
            effect = emptyList(),
            remoteBaseOf = remote.value,
            resolves = null,
        )
    }
    return RemoteData(
        definition = DefinitionProperties(
            name = remote.value,
            deprecated = false,
            related = emptyList(),
            description = emptyList(),
            source = remote.sourceLocation.toSource(),
        ),
        default = false,
        baseAction = baseAction,
    )
}

/** Base action fields are defined as directives on schema extensions. */
private fun GQLDirective.toFieldData(): FieldData? {
    val arguments = arguments?.arguments ?: return null
    return FieldData(
        name = arguments.filter { it.name == "name" }
            .map { (it.value as GQLStringValue).value }
            .single(),
        aliases = emptyMap(),
        type = kotlin.run {
            val flag = arguments.filter { it.name == "required" }
                .map {
                    val required = (it.value as GQLBooleanValue).value
                    if (required) TypeFlag.REQUIRED else TypeFlag.OPTIONAL
                }
                .singleOrNull()
                ?: TypeFlag.UNSPECIFIED
            arguments.filter { it.name == "type" || it.name == "list" }
                .map {
                    if (it.name == "type") {
                        val type = (it.value as GQLStringValue).value
                        ReferenceTypeData(type, flag)
                    } else {
                        val inner = (it.value as GQLStringValue).value
                        ListTypeData(ReferenceTypeData(inner, flag), flag)
                    }
                }
                .single()
        },
        identifying = false,
        hashTarget = false,
        deprecated = arguments.filter { it.name == "deprecated" }
            .map { (it.value as GQLBooleanValue).value }
            .singleOrNull()
            ?: false,
        localOnly = false,
        root = false,
        derives = emptyList(),
        description = arguments.filter { it.name == "description" }
            .map {
                DescriptionData(
                    text = (it.value as GQLStringValue).value,
                    source = it.sourceLocation.toSource(),
                )
            },
        source = sourceLocation.toSource()
    )
}

/** Base Figment definition properties we can build off any GQL node. */
private fun GQLNode.toDefinitionProperties(): DefinitionProperties {
    val source = sourceLocation.toSource()
    val name = directives.find("figment", "name")
        .map { it.value }
        .singleOrNull()
        ?: name
    return DefinitionProperties(
        name = name,
        deprecated = deprecated,
        related = emptyList(),
        description = buildList {
            description?.let { add(DescriptionData(it, source)) }
        },
        source = source
    )
}

/**
 * Helper to build dummy Figment definition properties with just a name,
 * when we don't have a GQL node.
 */
private fun String.toDefinitionProperties() : DefinitionProperties {
    return DefinitionProperties(
        name = this,
        deprecated = false,
        related = emptyList(),
        description = emptyList(),
        source = Source(0, 0, "".toPath()),
    )
}

/** Oof, need all this just to add the time field to all actions. */
private fun baseAction(): ActionData {
    val source = Source(0, 0, "synthetic".toPath())
    return ActionData(
        definition = "base_action".toDefinitionProperties(),
        syncable = SyncableProperties(
            auth = null,
            remote = null,
            endpoint = null,
            interfaces = emptyList(),
            isInterface = true,
            fields = listOf(
                FieldData(
                    name = "time",
                    aliases = emptyMap(),
                    type = ReferenceTypeData("Timestamp", TypeFlag.REQUIRED),
                    identifying = false,
                    hashTarget = false,
                    deprecated = false,
                    localOnly = false,
                    root = false,
                    derives = emptyList(),
                    description = listOf(
                        DescriptionData(
                            text = "When the action occurred.",
                            source = source,
                        )
                    ),
                    source = source
                )
            ),
            source = source
        ),
        isBase = true,
        priority = null,
        effect = emptyList(),
        remoteBaseOf = null,
        resolves = null,
    )
}

/** Field types are also called types in Figment, but modelled a little differently. */
private fun GQLType.toFieldTypeData(
    isMap: Boolean = false,
    flag: TypeFlag = TypeFlag.UNSPECIFIED,
): FieldTypeData {
    return when (this) {
        is GQLNonNullType -> type.toFieldTypeData(isMap, TypeFlag.REQUIRED)
        is GQLListType -> {
            if (isMap) {
                MapTypeData(type.toFieldTypeData() as AllowedInCollectionTypeData, flag)
            } else {
                ListTypeData(type.toFieldTypeData() as AllowedInCollectionTypeData, flag)
            }
        }
        is GQLNamedType -> ReferenceTypeData(
            definition = when (name) {
                "Int" -> "Integer"
                else -> name
            },
            flag = flag
        )
    }
}

/** Helper to get either a single value as list or the entire list. */
private val GQLArgument.values: List<GQLStringValue>
    get() {
        return when (val value = this.value) {
            is GQLStringValue -> listOf(value)
            is GQLListValue -> value.values.filterIsInstance<GQLStringValue>()
            else -> emptyList()
        }
    }
