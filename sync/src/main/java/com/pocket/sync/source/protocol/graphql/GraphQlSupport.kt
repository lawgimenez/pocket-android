package com.pocket.sync.source.protocol.graphql

import com.pocket.sync.source.RemoteStyle

/**
 * GraphQL helpers for a [GraphQlSyncable]. Provides the interface that [GraphQlSource] requires to function.
 */
interface GraphQlSupport {
    /** A query or a mutation to send in a request. */
    fun operation(): String?
}
