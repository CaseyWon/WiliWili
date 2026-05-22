package com.example.bilimini.data.model

data class DynamicItem(
    val id: String,
    val authorMid: Long,
    val author: String,
    val avatarUrl: String,
    val publishText: String,
    val text: String,
    val title: String,
    val coverUrl: String,
    val badge: String?,
    val bvid: String? = null,
    val images: List<String> = emptyList(),
    val origin: DynamicOrigin? = null,
)

data class DynamicOrigin(
    val authorMid: Long,
    val author: String,
    val title: String,
    val text: String,
    val coverUrl: String,
    val badge: String?,
    val bvid: String? = null,
)
