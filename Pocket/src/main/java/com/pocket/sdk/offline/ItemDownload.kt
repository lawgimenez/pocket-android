package com.pocket.sdk.offline

import com.pocket.sdk.api.generated.enums.PositionType
import com.pocket.sdk.api.generated.thing.Item
import com.pocket.sdk.api.generated.thing.Position

/**
 * A [Position] of an [Item] to download.
 */
data class ItemDownload(val item: Item, val view: PositionType)