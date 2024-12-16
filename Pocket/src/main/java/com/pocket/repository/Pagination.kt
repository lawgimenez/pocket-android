package com.pocket.repository

import androidx.paging.PagingConfig
import com.pocket.sdk.api.generated.thing.PageInfo

@JvmInline value class Cursor(private val value: String) {
    override fun toString() = value
}

fun String.toCursor() = Cursor(this)

fun PageInfo.toPreviousPageCursor(): Cursor? {
    return if (hasPreviousPage!!) {
        startCursor?.toCursor()
    } else {
        null
    }
}

fun PageInfo.toNextPageCursor(): Cursor? {
    return if (hasNextPage!!) {
        endCursor?.toCursor()
    } else {
        null
    }
}

const val DefaultPageSize = 30
val DefaultPagingConfig = PagingConfig(
    pageSize = DefaultPageSize,
    initialLoadSize = DefaultPageSize,
)
