package com.pocket.sync.source.protocol.graphql

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.util.java.JsonUtil

/**
 * Parses GraphQL errors to spec http://spec.graphql.org/draft/#sec-Errors
 * and provides helpers related to them.
 *
 * The format of the errors object looks like:
 * ```json
 * {
 *   "errors": [
 *     {
 *       "message": "Name for character with ID 1002 could not be fetched.",
 *       "locations": [ { "line": 6, "column": 7 } ],
 *       "path": [ "hero", "heroFriends", 1, "name" ],
 *       "extensions": {
 *
 *       }
 *     }
 *   ]
 * }
 * ```
 *
 * Note: We also use Apollo, which contains its own extensions: https://www.apollographql.com/docs/apollo-server/data/errors/#default-information
 *
 * @param json the "errors" value in the response from graphql
 */
class GraphQlErrors(val json: JsonNode?) {
    val errors = (json as? ArrayNode)?.map { GraphQlError(it as ObjectNode) } ?: emptyList()

    /**
     * If this mutation/query had errors, all related errors, otherwise empty.
     * @param name The name of the mutation/query or its alias that was used.
     */
    fun errorsOf(name: String) = errors.filter { name == it.path.getOrNull(0) as? String } // The first path entry should be the name

    /**
     * True if the PERSISTED_QUERY_NOT_FOUND error was returned.
     */
    fun apqError() = errors.any { it.code == "PERSISTED_QUERY_NOT_FOUND" }
}

/**
 * A representation of one of the entries in a standard graphql "errors" response.
 * See [GraphQlErrors] for more details.
 */
data class GraphQlError(val json: ObjectNode) {

    val message : String = JsonUtil.getValueAsText(json, "message", "")

    /** An array of path parts, strings for fields, ints for list indexes. */
    val path : List<Any> = JsonUtil.getValueAsArray(json, "path")?.map { it.textValue() } ?: emptyList()

    val extensions = JsonUtil.getValueAsObject(json, "extensions")

    val code = JsonUtil.getValueAsText(extensions, "code", null)

    // could parse out other fields as needed
}