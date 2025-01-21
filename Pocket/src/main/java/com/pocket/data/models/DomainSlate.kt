package com.pocket.data.models

data class DomainSlate(
    val title: String?,
    val subheadline: String?,
    val recommendations: List<DomainRecommendation>
)
