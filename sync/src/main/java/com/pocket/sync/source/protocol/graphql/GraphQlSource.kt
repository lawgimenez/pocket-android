package com.pocket.sync.source.protocol.graphql

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.pocket.sync.action.Action
import com.pocket.sync.source.FullResultSource
import com.pocket.sync.source.JsonConfig
import com.pocket.sync.source.LimitedSource
import com.pocket.sync.source.result.Status
import com.pocket.sync.source.result.SyncResult
import com.pocket.sync.spec.Syncable
import com.pocket.sync.thing.Thing
import com.pocket.sync.value.Allow
import com.pocket.sync.value.BaseModeller
import com.pocket.sync.value.Include
import okio.ByteString.Companion.toByteString
import java.io.InputStream

/**
 * A helper class to perform a [syncFull] to a GraphQL based API.
 * Only [GraphQlSyncable]'s are supported.
 *
 * REVIEW should we try to pass up any of the graphql errors?  We can't be sure they are user facing...
 *
 * @param http An abstraction to handle the actual http call
 * @param jsonConfig The config to use when parsing json from this API.
 * @param apqSupported Automatic Persisted Queries (APQ) support https://www.apollographql.com/docs/apollo-server/performance/apq/
 */
class GraphQlSource(
    private val http: HttpHandler,
    private val jsonConfig: JsonConfig,
    private val apqSupported: Boolean,
) : FullResultSource, LimitedSource {

    private val jsonMapper = BaseModeller.OBJECT_MAPPER

    override fun isSupported(syncable: Syncable) = syncable is GraphQlSyncable

    override fun <T : Thing?> syncFull(thing: T, vararg actions: Action): SyncResult<T> {
        val sr = SyncResult.Builder(thing, actions)
        for (action in actions) {
            execute(
                syncable = action as GraphQlSyncable,
                onSuccess = { resolved ->
                    when (resolved) {
                        is Thing -> sr.resolved(resolved)
                        is Collection<*> -> resolved.forEach { if (it is Thing) sr.resolved(it) }
                        else -> {} // Other values can be ignored, nothing to resolve
                    }
                    sr.action(action, Status.SUCCESS, null, null)
                },
                onError = { discard, details ->
                    sr.action(action, if (discard) Status.FAILED_DISCARD else Status.FAILED, details.errorThrowable, null)
                }
            )
            if (sr.hasFailures()) return sr.build()
        }

        if (thing != null) {
            if (thing !is GraphQlSyncable || thing.graphQl().operation() == null) {
                sr.thing(Status.NOT_ATTEMPTED, null, "Missing query.")
            } else {
                execute(
                    syncable = thing as GraphQlSyncable,
                    onSuccess = { response ->
                        // Merge id fields with response to get the final result
                        response as Thing
                        sr.thing(response.builder().set(thing.identity()).build() as T)
                    },
                    onError = { discard, details ->
                        sr.thing(if (discard) Status.FAILED_DISCARD else Status.FAILED, details.errorThrowable, null)
                    }
                )
            }
        }
        return sr.build()
    }

    private fun execute(
        syncable: GraphQlSyncable,
        onSuccess: (returned: Any?) -> Unit,
        onError: (discard: Boolean, details: GraphQlResult) -> Unit,
    ) {
        val request = buildRequest(syncable)

        // Setup the response handling.
        var nullableResult: GraphQlResult? = null
        var errors: GraphQlErrors? = null
        var parsingError: Throwable? = null
        val response: ResponseHandler = { body, httpStatus, error ->
            if (error != null) {
                // Mark as failed.
                nullableResult =
                    GraphQlResult(false, errorHttp = httpStatus, errorThrowable = error)
            } else {
                // Attempt to read the body.
                try {
                    val parser = jsonMapper.factory.createParser(body)
                    parser.nextToken()
                    while (parser.nextToken() != JsonToken.END_OBJECT && !parser.isClosed) {
                        val field = parser.currentName
                        parser.nextToken()
                        when (field) {
                            "data" -> nullableResult = parseData(parser, syncable)
                            "errors" -> errors = GraphQlErrors(parser.readValueAsTree())
                            else -> parser.skipChildren()
                        }
                    }
                } catch (t: Throwable) {
                    parsingError = t
                }
            }
        }
        // Note: send up a deep copy, so outside callers can't modify our version if they end up tweaking it.
        http.execute(request.deepCopy(), response)

        // Check for APQ error
        if (apqSupported && errors?.apqError() == true) {
            // Retry once, this time with the query
            request.put("query", syncable.graphQl().operation())
            errors = null
            parsingError = null
            http.execute(request.deepCopy(), response)
        }

        // Return the result
        // If we somehow did not get a result, mark it as an error
        var result = nullableResult ?: GraphQlResult(false, errorThrowable = parsingError)

        // If there was an error returned from graphql, swap from success to error
        // REVIEW GraphQl will still return partial results in many errors
        // Assuming we want to fail 100% rather than letting partial data
        // get returned to the sync engine?
        val e = errors?.errors
        if (!e.isNullOrEmpty()) {
            result = result.copy(success = false, errorGraphQl = e)
        }

        if (result.success) {
            onSuccess(result.returned)

        } else {
            val discard = when {
                result.errorGraphQl.isNotEmpty() -> {
                    // If graphql reported a specific error, discard and don't retry
                    true
                }
                result.errorHttp != null || result.errorThrowable != null -> {
                    // If it was a networking error, we can retry it
                    false
                }
                else -> {
                    false
                }
            }
            onError(discard, result)
        }
    }

    private fun buildRequest(syncable: GraphQlSyncable): ObjectNode {
        val request = jsonMapper.createObjectNode()
        request.set("variables", when (syncable) {
            is Thing -> syncable.identity().toJson(jsonConfig, Include.DANGEROUS)
            is Action -> syncable.toJson(jsonConfig, Include.DANGEROUS).apply { remove("action") }
            else -> throw AssertionError("Only Things and Actions are ${GraphQlSyncable::class.simpleName}s")
        })

        val query = requireNotNull(syncable.graphQl().operation())
        if (apqSupported) {
            // Automatic Persisted Queries (APQ) support https://www.apollographql.com/docs/apollo-server/performance/apq/
            val extensions = jsonMapper.createObjectNode()
            val persistedQuery = jsonMapper.createObjectNode()
            persistedQuery.put("version", 1)
            persistedQuery.put("sha256Hash", query.toByteArray().toByteString().sha256().hex())
            extensions.set("persistedQuery", persistedQuery)
            request.set("extensions", extensions)
        } else {
            request.put("query", query)
        }
        return request
    }

    private fun parseData(
        parser: JsonParser,
        syncable: GraphQlSyncable,
    ): GraphQlResult {
        val result = when (syncable) {
            is Thing -> {
                // Break out of streaming mode.
                // Max originally did it to handle root values. We can actually assume all queries
                // have a root value. With this assumption it was pretty easy to tweak the code
                // to continue in streaming mode.
                // ...
                // But, then I discovered streaming mode doesn't support field aliases.
                // They are required for Client API, because of all the renamed fields on Item.
                // When I tried to add alias support to streaming parsing code generation,
                // then I discovered it broke parsing some V3 responses. 🤦
                // ...
                // So.. uh.. Let's keep this old workaround for now? This way V3 uses streaming
                // and ignores aliases. And Client API uses the "tree model" and supports aliases.
                val json = parser.readValueAsTree<JsonNode>()
                syncable.creator.create(json, jsonConfig, Allow.UNKNOWN)
            }
            is Action -> {
                when (parser.currentToken) {
                    JsonToken.VALUE_NULL -> null
                    JsonToken.START_OBJECT -> {
                        // Skip field name containing the mutation name.
                        require(parser.nextToken() == JsonToken.FIELD_NAME)
                        // Advance to the returned/resolved value/object.
                        parser.nextToken()
                        // Parse and return it.
                        syncable.resolved()?.streaming?.create(parser, jsonConfig, Allow.UNKNOWN)
                            .also {
                                // Skip closing brace.
                                require(parser.nextToken() == JsonToken.END_OBJECT)
                            }
                    }
                    else -> {
                        error("Expecting a data object or null, found ${parser.currentToken}")
                    }
                }
            }
            else -> {
                throw AssertionError("Only Things and Actions are ${GraphQlSyncable::class.simpleName}s")
            }
        }
        return GraphQlResult(true, result)
    }

    /**
     * Handles making the actual http call to the API
     */
    interface HttpHandler {
        /**
         * Make a call to the GraphQL server. Block until completed.
         * REVIEW: Apollo can also return an "errors" block for some other status codes, like 4XX: https://www.apollographql.com/docs/react/data/error-handling/
         * Could attempt to parse them if we end up doing something with them, but for sync, either way we will fail, so don't need unless we need those messages.
         *
         * @param request A json object of all of the parameters to send as the request
         * @param response Invoke this with the response details
         */
        fun execute(request: ObjectNode, response: ResponseHandler)
    }

    private data class GraphQlResult(
        var success: Boolean,
        var returned: Any? = null,
        var errorGraphQl: List<GraphQlError> = emptyList(),
        var errorThrowable: Throwable? = null,
        var errorHttp: Int? = null
    )
}

typealias ResponseHandler = (body: InputStream?, httpStatus: Int?, error: Throwable?) -> Unit
