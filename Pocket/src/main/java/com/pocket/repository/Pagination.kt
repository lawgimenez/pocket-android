package com.pocket.repository

import androidx.paging.PagingConfig

@JvmInline value class Cursor(private val value: String)

const val DefaultPageSize = 30
val DefaultPagingConfig = PagingConfig(
    pageSize = DefaultPageSize,
    initialLoadSize = DefaultPageSize,
)
