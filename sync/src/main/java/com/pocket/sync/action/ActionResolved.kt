package com.pocket.sync.action

import com.fasterxml.jackson.databind.JsonNode
import com.pocket.sync.source.JsonConfig
import com.pocket.sync.thing.Thing
import com.pocket.sync.value.*

/**
 * Action's can have a resolved/return value during syncing with a remote source.
 * See the resolved section in Figment's syntax spec for details.
 *
 * This interface provides helpers for parsing the expected type.
 */
class ActionResolved<T>(
        val json: SyncableParser<T?>,
        val streaming: StreamingThingParser<T?>
)
