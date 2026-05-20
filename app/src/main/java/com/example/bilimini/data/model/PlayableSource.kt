package com.example.bilimini.data.model

data class PlayableSource(
    val videoUrl: String,
    val audioUrl: String? = null,
    val headers: Map<String, String>,
    val fallbackPageUrl: String,
    val qualityLabel: String,
)
