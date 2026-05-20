package com.example.bilimini.data.recommendation

import kotlinx.serialization.Serializable

@Serializable
data class RecommendationHistoryEntry(
    val bvid: String,
    val title: String,
    val author: String,
    val durationSeconds: Long?,
    val openedAtEpochMs: Long,
)
