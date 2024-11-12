package com.pocket.sync.source.protocol.graphql

import com.pocket.sync.spec.Syncable

/**
 * GraphQl extensions to [com.pocket.sync.thing.Thing] (Query) and [com.pocket.sync.action.Action] (Mutation)
 */
interface GraphQlSyncable : Syncable {
    /**
     * Access helpers for working with this in GraphQL
     */
    fun graphQl() : GraphQlSupport
}