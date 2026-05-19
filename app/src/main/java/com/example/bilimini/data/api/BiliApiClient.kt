package com.example.bilimini.data.api

import com.example.bilimini.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class BiliApiClient(
    private val sessionManager: SessionManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun getJson(
        url: String,
        authenticated: Boolean = false,
    ): JsonObject? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_USER_AGENT)
            .header("Referer", "https://www.bilibili.com/")
            .header("Accept", "application/json, text/plain, */*")
            .apply {
                if (authenticated) {
                    val cookieHeader = sessionManager.getCookieHeader()
                    if (cookieHeader.isNotBlank()) {
                        header("Cookie", cookieHeader)
                    }
                }
            }
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                return@withContext null
            }
            json.parseToJsonElement(body) as? JsonObject
        }
    }

    fun buildUrl(
        base: String,
        params: Map<String, String>,
    ): String {
        if (params.isEmpty()) {
            return base
        }
        val query = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$base?$query"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private companion object {
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
