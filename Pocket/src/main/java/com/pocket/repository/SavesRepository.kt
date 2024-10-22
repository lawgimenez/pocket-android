package com.pocket.repository

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.enums.ItemSortKey
import com.pocket.sdk.api.generated.enums.ItemStatusKey
import com.pocket.sdk.api.generated.thing.Saves
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavesRepository @Inject constructor(
    private val pocket: Pocket
) {

    fun getRecentSavesAsFlow(count: Int): Flow<Saves> =
        pocket.bindLocalAsFlow(
            pocket.spec().things().saves()
                .count(count)
                .state(ItemStatusKey.UNREAD)
                .sort(ItemSortKey.NEWEST)
                .build()
        )
}