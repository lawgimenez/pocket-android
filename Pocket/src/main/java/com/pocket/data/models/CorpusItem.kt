package com.pocket.data.models

import com.pocket.sdk.api.generated.enums.SavedItemStatus

data class CorpusItem(
    val title: String,
    val publisher: String,
    val excerpt: String,
    val imageUrl: String,
    val url: String,
    val isSaved: Boolean,
)

fun com.pocket.sdk.api.generated.thing.CorpusItem.toDomainCorpusItem(): CorpusItem =
    CorpusItem(
        title = preview?._title().orEmpty(),
        publisher = preview?._domain()?.name.orEmpty(),
        excerpt = preview?._excerpt().orEmpty(),
        imageUrl = preview?._image()?.url?.url.orEmpty(),
        url = preview!!._url()!!.url,
        isSaved = isSaved,
    )

private val com.pocket.sdk.api.generated.thing.CorpusItem.isSaved: Boolean
    get() = savedItem?.status == SavedItemStatus.ARCHIVED || savedItem?.status == SavedItemStatus.UNREAD