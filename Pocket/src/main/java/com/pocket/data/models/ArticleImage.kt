package com.pocket.data.models

data class ArticleImage(
    val imageId: Int,
    val localFileUrl: String,
    val caption: String,
    val credit: String,
    val originalUrl: String,
)