package com.example.bilimini.session

data class SessionState(
    val isLoggedIn: Boolean = false,
    val cookieHeader: String = "",
)
