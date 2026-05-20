package com.example.bilimini.data.model

data class VideoDetail(
    val bvid: String,
    val aid: Long,
    val cid: Long,
    val title: String,
    val description: String,
    val coverUrl: String,
    val author: String,
    val ownerMid: Long,
    val durationSeconds: Long,
    val durationText: String,
    val publishedText: String,
    val stats: List<VideoStat>,
)

data class VideoStat(
    val label: String,
    val value: String,
)
