package com.example.bilimini.data.model

data class VideoSummary(
    val bvid: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val durationText: String,
    val playText: String,
    val badge: String? = null,
    val durationSeconds: Long? = null,
    val playCount: Long? = null,
)
