package com.example.bilimini.session

import android.content.Context
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _sessionState = MutableStateFlow(
        SessionState(
            isLoggedIn = preferences.getBoolean(KEY_LOGGED_IN, false),
            cookieHeader = preferences.getString(KEY_COOKIE_HEADER, "").orEmpty(),
        )
    )
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun saveCookies(rawCookieStrings: List<String>) {
        val header = normalizeCookies(rawCookieStrings)
        val loggedIn = header.contains("SESSDATA=") && header.contains("DedeUserID=")
        preferences.edit()
            .putString(KEY_COOKIE_HEADER, header)
            .putBoolean(KEY_LOGGED_IN, loggedIn)
            .apply()
        _sessionState.value = SessionState(loggedIn, header)
    }

    fun clearSession() {
        preferences.edit().clear().apply()
        _sessionState.value = SessionState()
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    fun seedCookiesIntoWebView() {
        val header = _sessionState.value.cookieHeader
        if (header.isBlank()) {
            return
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        header.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { cookie ->
                cookieManager.setCookie("https://www.bilibili.com", cookie)
                cookieManager.setCookie("https://m.bilibili.com", cookie)
                cookieManager.setCookie("https://passport.bilibili.com", cookie)
            }
        cookieManager.flush()
    }

    fun getCookieHeader(): String = _sessionState.value.cookieHeader

    private fun normalizeCookies(rawCookies: List<String>): String {
        val cookieMap = linkedMapOf<String, String>()
        rawCookies
            .flatMap { it.split(";") }
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { cookie ->
                val key = cookie.substringBefore("=").trim()
                val value = cookie.substringAfter("=").trim()
                if (key.isNotBlank() && value.isNotBlank()) {
                    cookieMap[key] = value
                }
            }
        return cookieMap.entries.joinToString("; ") { (key, value) -> "$key=$value" }
    }

    private companion object {
        const val PREFERENCES_NAME = "bilimini.secure.session"
        const val KEY_COOKIE_HEADER = "cookie_header"
        const val KEY_LOGGED_IN = "logged_in"
    }
}
