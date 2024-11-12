package com.pocket.data.models

import com.pocket.sdk.api.generated.enums.ItemStatus

data class Collection(
    val authors: List<Author>,
    val title: String,
    val intro: String,
    val stories: List<Story>,
)

data class Author(
    @JvmField val name: String,
)

data class Story(
    val excerpt: String,
    val publisher: String,
    val title: String,
    val url: String,
    val imageUrl: String,
    val isSaved: Boolean,
    val isCollection: Boolean,
)

fun com.pocket.sdk.api.generated.thing.Collection.toDomainCollection(): Collection =
    Collection(
        authors = authors?.mapNotNull {
            Author(
                name = it?.name.orEmpty()
            )
        }.orEmpty(),
        title = title.orEmpty(),
        intro = intro?.value.orEmpty(),
        stories = stories?.mapNotNull {
            Story(
                excerpt = it.excerpt?.value.orEmpty(),
                publisher = it.publisher.orEmpty(),
                title = it.title.orEmpty(),
                url = it.url?.url.orEmpty(),
                imageUrl = it.imageUrl?.url.orEmpty(),
                isSaved = it.item?.status == ItemStatus.ARCHIVED || it.item?.status == ItemStatus.UNREAD,
                isCollection = it.item?.collection != null
            )
        }.orEmpty()
    )

fun com.pocket.sdk.api.generated.thing.Author.toDomainAuthor(): Author = Author(name = name.orEmpty())

fun List<com.pocket.sdk.api.generated.thing.Author>.toDomainAuthors(): List<Author> =
    map { it.toDomainAuthor() }