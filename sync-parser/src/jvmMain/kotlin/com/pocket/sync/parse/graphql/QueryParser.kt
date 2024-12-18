@file:Suppress("ForbiddenComment", "MaxLineLength")

package com.pocket.sync.parse.graphql

import com.apollographql.apollo3.ast.*
import com.pocket.sync.parse.*
import com.pocket.sync.parse.Source
import okio.*
import okio.Path.Companion.toPath

/**
 * Parses files with GraphQL operations (queries and mutations) and generates an in-memory
 * "Figment" representation that is used by the Sync Engine to generate code for things and actions
 * corresponding to the queries and mutations so you can build them and pass to `sync()`.
 */
class QueryParser(
    spec: FigmentsData,
    private val remoteName: String? = null,
) : Parser {
    private val queries =
        try {
            spec.definitions.filterIsInstance<ThingData>().single { it.definition.name == "Query" }
        } catch (t: Throwable) {
            throw AssertionError("Expected Query type in the spec", t)
        }
    private val mutations =
        try {
            spec.definitions
                .filterIsInstance<ThingData>()
                .single { it.definition.name == "Mutation" }
        } catch (t: Throwable) {
            throw AssertionError("Expected Mutation type in the spec", t)
        }
    private val fragments = mutableMapOf<String, GQLFragmentDefinition>()
    private var path = "".toPath()

    override fun parse(fs: FileSystem, path: Path) : FigmentsData {
        val graphQlPaths = fs.allWithExtension(path, "graphql")

        // First find all the fragments.
        fragments.clear()
        graphQlPaths.forEach {
            fs.read(it) { fragments.putAll(findFragments()) }
        }

        // Now build figments for the operations.
        return graphQlPaths
            .flatMap { 
                this.path = it
                fs.read(it) { parse() }
            }
            .let { FigmentsData(it) }
    }

    override fun parse(text: String): FigmentsData {
        fragments.clear()
        fragments.putAll(text.byteInputStream().source().buffer().findFragments())
        return text.byteInputStream().source().buffer().parse().let { FigmentsData(it) }
    }

    private fun BufferedSource.parse(): List<DefinitionData> {
        val remoteFlag = this@QueryParser.remoteName?.let { RemoteFlagData(it, Source(0, 0, path)) }

        return parseAsGQLDocument()
            .valueAssertNoErrors()
            .definitions
            .mapNotNull { definition ->
                when (definition) {
                    is GQLOperationDefinition ->
                        when (definition.operationType) {
                            "query" -> definition.toThingData(remoteFlag)
                            "mutation" -> definition.toActionData(remoteFlag)
                            else -> null
                        }
                    is GQLFragmentDefinition -> null // Ignore. Handled in [findFragments].
                    else -> throw AssertionError("Unsupported definition: ${definition.javaClass.simpleName}")
                }
            }
    }

    private fun BufferedSource.findFragments(): Map<String, GQLFragmentDefinition> {
        return parseAsGQLDocument()
            .valueAssertNoErrors()
            .definitions
            .filterIsInstance<GQLFragmentDefinition>()
            .associateBy { it.name }
    }

    private fun GQLOperationDefinition.toThingData(remote: RemoteFlagData?): ThingData {
        val source = sourceLocation.toSource()
        val selection = selectionSet.selections.single() as GQLField
        val query = try {
            queries.syncable.fields.single { it.name == selection.name }
        } catch (t: Throwable) {
            throw RuntimeException("Operation $name references non-existent query ${selection.name}")
        }
        val rootValue = FieldData(
            selection.alias ?: selection.name,
            aliases = emptyMap(),
            type = query.type,
            identifying = false,
            hashTarget = false,
            deprecated = selection.deprecated,
            localOnly = false,
            root = true,
            derives = emptyList(),
            description = emptyList(),
            source = selection.sourceLocation.toSource()
        )
        return ThingData(
            this.toDefinitionProperties(source),
            this.toSyncableProperties(remote, source, rootValue),
            if (variableDefinitions.isEmpty()) UniqueFlagData(source) else null,
        )
    }

    private fun GQLOperationDefinition.toActionData(remote: RemoteFlagData?): ActionData {
        val source = sourceLocation.toSource()
        return ActionData(
            definition = this.toDefinitionProperties(source),
            syncable = this.toSyncableProperties(remote, source),
            isBase = false,
            priority = directives.find("figment", "priority")
                .map { PriorityFlagData(it.value, it.sourceLocation.toSource()) }
                .singleOrNull(),
            effect = emptyList(),
            remoteBaseOf = null,
            resolves = kotlin.run {
                val selection = selectionSet.selections.singleOrNull()
                if (selection == null) {
                    println("Warning: Resolving mutation response is supported only when there is exactly one mutation in a single request. ($operationType $name)")
                    return@run null
                }
                val field = selection as? GQLField
                if (field == null) {
                    println("Warning: Resolving mutation response is supported only when using Mutation fields directly, not through fragments, etc. ($operationType $name)")
                    return@run null
                }
                val mutation = mutations.syncable.fields.singleOrNull { it.name == field.name }
                if (mutation == null) {
                    println("Warning: Cannot resolve response type for mutation ${selection.name}, because it's missing from schema. ($operationType $name)")
                    return@run null
                }
                ResolvesFlagData(
                    mutation.type,
                    selection.sourceLocation.toSource()
                )
            }
        )
    }

    private fun GQLOperationDefinition.toDefinitionProperties(source: Source): DefinitionProperties {
        return DefinitionProperties(
            name
                ?: throw RuntimeException("$operationType is missing a name at ${sourceLocation.pretty()}"),
            deprecated,
            emptyList(),
            description?.let { description ->
                listOf(
                    DescriptionData(description, source)
                )
            } ?: emptyList(),
            source,
        )
    }

    private fun GQLOperationDefinition.toSyncableProperties(
        remote: RemoteFlagData?,
        source: Source,
        rootValue: FieldData? = null,
    ): SyncableProperties {
        val arguments = variableDefinitions.map { it.toFieldData() }
        val fragments = buildSet {
            for (name in findUsedFragments(selectionSet)) {
                addFragment(fragments[name])
            }
        }
        val operation = buildString {
            append(
                this@toSyncableProperties
                    .transform {
                        // Clean up our local directives so we don't send them to the API.
                        if (it is GQLDirective && it.name == "figment") {
                            TransformResult.Delete
                        } else {
                            TransformResult.Continue
                        }
                    }!!
                    .toUtf8()
            )
            appendLine()
            for (fragment in fragments) {
                append(fragment.toUtf8())
            }
        }

        return SyncableProperties(
            null, // TODO: We could add a custom auth annotation and add extra documentation and runtime protection.
            remote,
            null,
            emptyList(),
            false,
            buildList {
                addAll(arguments)
                rootValue?.let { add(it) }
            },
            source,
            operation,
        )
    }

    /** Recursively traverse the selection set and collect names of all used fragment. */
    private fun findUsedFragments(node: GQLSelectionSet?): Set<String> = buildSet {
        for (selection in (node ?: return@buildSet).selections) {
            when (selection) {
                is GQLFragmentSpread -> add(selection.name)
                is GQLField -> addAll(findUsedFragments(selection.selectionSet))
                is GQLInlineFragment -> addAll(findUsedFragments(selection.selectionSet))
            }
        }
    }

    /** Add a fragment and recursively add any fragments it uses. */
    private fun MutableSet<GQLFragmentDefinition>.addFragment(fragment: GQLFragmentDefinition?) {
        fragment ?: return
        add(fragment)
        for (spread in findUsedFragments(fragment.selectionSet)) {
            addFragment(fragments[spread])
        }
    }

    private fun GQLVariableDefinition.toFieldData() = FieldData(
        name,
        aliases = emptyMap(),
        type.toFieldTypeData(),
        identifying = true,
        hashTarget = false,
        deprecated,
        localOnly = false,
        root = false,
        derives = emptyList(),
        description = emptyList(),
        sourceLocation.toSource(),
    )

    private fun SourceLocation.toSource() = Source(line, line, path)
}

private fun GQLType.toFieldTypeData() = ReferenceTypeData(
    when (val name = rawType().name) {
        "Int" -> "Integer"
        else -> name
    },
    if (this is GQLNonNullType)
        TypeFlag.REQUIRED
    else
        TypeFlag.OPTIONAL
)
