package com.pocket.repository

import com.pocket.sdk.Pocket
import com.pocket.sdk.api.generated.thing.Tags
import com.pocket.sdk.api.value.Timestamp
import com.pocket.sdk.getLocal
import com.pocket.sync.source.bindLocalAsFlow
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val pocket: Pocket
) {

    //TODO broken
    suspend fun getTags(): Tags =
        pocket.getLocal(
            pocket.spec()
                .things()
                .tags()
                .build()
        )

    fun getTagsAsFlow(): Flow<Tags> =
        pocket.bindLocalAsFlow(
            pocket.spec()
                .things()
                .tags()
                .build()
        )

    /**
     * @param tagsMap map where keys are old tag names and values are the new ones
     */
    fun editTags(tagsMap: Map<String, String>) {
        pocket.sync(
            null,
            *tagsMap.map { tagMap ->
                pocket.spec().actions().tag_rename()
                    .old_tag(tagMap.key)
                    .new_tag(tagMap.value)
                    .time(Timestamp.now())
                    .build()
            }.toTypedArray()
        )
    }

    fun deleteTag(tag: String) {
        pocket.sync(
            null,
            pocket.spec().actions().tag_delete()
                .tag(tag)
                .time(Timestamp.now())
                .build()
        )
    }
}