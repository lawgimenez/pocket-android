package com.pocket.sync.parse.graphql

import com.apollographql.apollo3.ast.*
import com.pocket.sync.parse.Source
import okio.Path.Companion.toPath

/** Most nodes have names, let's just return a default if one doesn't. */
val GQLNode.name get() = (this as? GQLNamed)?.name ?: "unnamed"

/** A lot of nodes have directives, but there's no interface for it sadly. */
val GQLNode.directives: List<GQLDirective>
    get() {
        return when (this) {
            is GQLFieldDefinition -> directives
            is GQLEnumValueDefinition -> directives
            is GQLObjectTypeDefinition -> directives
            is GQLObjectTypeExtension -> directives
            is GQLInputObjectTypeDefinition -> directives
            is GQLInputObjectTypeExtension -> directives
            is GQLEnumTypeDefinition -> directives
            is GQLEnumTypeExtension -> directives
            is GQLInputValueDefinition -> directives
            else -> emptyList()
        }
    }

/** A helper for checking any arbitrary node if it's deprecated. */
val GQLNode.deprecated get() = directives.any { it.name == "deprecated" || it.name == "_deprecated" }

/** Helper to get a description for any node (if it has it). */
val GQLNode.description: String?
    get() {
        return when (this) {
            is GQLDescribed -> description
            is GQLFieldDefinition -> description
            else -> null
        }
    }

/** Convert GraphQL source location our in-memory representation of source. */
fun SourceLocation.toSource() = Source(line, line, filePath?.toPath() ?: "".toPath())

/**
 * Look for a specific argument of a specific directive.
 * @return a sequence, because some callers expect repeating instances, some exactly 1, some 1 or 0
 */
fun List<GQLDirective>.find(directive: String, argument: String): Sequence<GQLStringValue> {
    return asSequence().filter { it.name == directive }
        .mapNotNull { it.arguments?.arguments }
        .flatten()
        .filter { it.name == argument }
        .mapNotNull { it.value as? GQLStringValue }
}
