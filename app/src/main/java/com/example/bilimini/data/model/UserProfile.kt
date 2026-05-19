package com.example.bilimini.data.model

data class UserProfile(
    val mid: Long,
    val name: String,
    val avatarUrl: String,
    val level: Int?,
    val sign: String?,
    val coins: Double?,
)
