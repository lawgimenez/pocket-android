package com.pocket.data.models

import org.threeten.bp.Duration

data class DomainRecommendation(
    val corpusId: String?,
    val itemId: String,
    val url: String,
    val title: String,
    val domain: String,
    val imageUrl: String?,
    val isCollection: Boolean,
    val isSaved: Boolean,
    val excerpt: String,
    // used for analytics
    val index: Int,
    val viewingTime: Duration?,
)
