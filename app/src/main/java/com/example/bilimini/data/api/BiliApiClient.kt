package com.example.bilimini.data.api

import com.example.bilimini.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class BiliApiClient(
    private val sessionManager: SessionManager,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableSetOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val result = cookieStore.filter { it.matches(url) }.toMutableList()
            val header = sessionManager.getCookieHeader()
            if (header.isNotBlank()) {
                header.split(";")
                    .map { it.trim() }
                    .filter { it.contains("=") }
                    .forEach { pair ->
                        val name = pair.substringBefore("=").trim()
                        if (result.none { it.name == name }) {
                            Cookie.parse(url, "$pair; Domain=.bilibili.com; Path=/")?.let(result::add)
                        }
                    }
            }
            return result
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .build()
    private var cachedWbiKey: String? = null
    private var cachedWbiKeyAtMs: Long = 0L

    suspend fun getJson(
        url: String,
        authenticated: Boolean = false,
    ): JsonObject? = withContext(Dispatchers.IO) {
        val request = requestBuilder(
            url = url,
            authenticated = authenticated,
        ).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) {
                return@withContext null
            }
            runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        }
    }

    suspend fun getText(
        url: String,
        authenticated: Boolean = false,
        mobileUserAgent: Boolean = false,
        referer: String = "https://www.bilibili.com/",
    ): String? = withContext(Dispatchers.IO) {
        val request = requestBuilder(
            url = url,
            authenticated = authenticated,
            mobileUserAgent = mobileUserAgent,
            referer = referer,
            accept = "*/*",
        ).build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            response.body?.string()
        }
    }

    fun parseJson(text: String): JsonObject? {
        return runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
    }

    suspend fun postForm(
        url: String,
        form: Map<String, String>,
        authenticated: Boolean = true,
        referer: String = "https://www.bilibili.com/",
    ): JsonObject? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply {
            form.forEach { (key, value) ->
                add(key, value)
            }
        }.build()

        val request = requestBuilder(
            url = url,
            authenticated = authenticated,
            referer = referer,
            accept = "application/json, text/plain, */*",
        )
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext null
            }
            val raw = response.body?.string().orEmpty()
            if (raw.isBlank()) {
                return@withContext null
            }
            parseJson(raw)
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

    suspend fun buildWbiSignedUrl(
        base: String,
        params: Map<String, String>,
    ): String? {
        val mixinKey = getWbiMixinKey() ?: return null
        val signedParams = params + ("wts" to (System.currentTimeMillis() / 1000L).toString())
        val query = signedParams.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(sanitizeWbiValue(value))}"
        }
        val wRid = md5("$query$mixinKey")
        return "$base?$query&w_rid=$wRid"
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private suspend fun getWbiMixinKey(): String? {
        val now = System.currentTimeMillis()
        cachedWbiKey?.takeIf { now - cachedWbiKeyAtMs < 6 * 60 * 60 * 1000L }?.let { return it }

        val payload = getJson(
            url = "https://api.bilibili.com/x/web-interface/nav",
            authenticated = true,
        ) ?: return null
        val data = payload["data"] as? JsonObject ?: return null
        val wbi = data["wbi_img"] as? JsonObject ?: return null
        val imgKey = wbi["img_url"]?.jsonObjectOrNullContent()
            ?.substringAfterLast("/")
            ?.substringBefore(".")
            .orEmpty()
        val subKey = wbi["sub_url"]?.jsonObjectOrNullContent()
            ?.substringAfterLast("/")
            ?.substringBefore(".")
            .orEmpty()
        if (imgKey.isBlank() || subKey.isBlank()) {
            return null
        }
        val raw = imgKey + subKey
        val mixed = buildString {
            MIXIN_KEY_TABLE.forEach { index ->
                raw.getOrNull(index)?.let(::append)
            }
        }.take(32)
        if (mixed.isBlank()) {
            return null
        }
        cachedWbiKey = mixed
        cachedWbiKeyAtMs = now
        return mixed
    }

    private fun sanitizeWbiValue(value: String): String = value.replace(WBI_FILTER_REGEX, "")

    private fun md5(value: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNullContent(): String? {
        return (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
    }

    fun defaultMediaHeaders(): Map<String, String> {
        val headers = linkedMapOf(
            "User-Agent" to DESKTOP_USER_AGENT,
            "Referer" to "https://www.bilibili.com/",
            "Origin" to "https://www.bilibili.com",
        )
        val cookieHeader = sessionManager.getCookieHeader()
        if (cookieHeader.isNotBlank()) {
            headers["Cookie"] = cookieHeader
        }
        return headers
    }

    fun defaultImageHeaders(): Map<String, String> {
        return linkedMapOf(
            "User-Agent" to DESKTOP_USER_AGENT,
            "Referer" to "https://www.bilibili.com/",
            "Origin" to "https://www.bilibili.com",
        )
    }

    fun csrfToken(): String? = sessionManager.getCsrfToken()

    private fun requestBuilder(
        url: String,
        authenticated: Boolean, // kept for API clarity; cookies are now handled by CookieJar
        mobileUserAgent: Boolean = false,
        referer: String = "https://www.bilibili.com/",
        accept: String = "application/json, text/plain, */*",
    ): Request.Builder {
        return Request.Builder()
            .url(url)
            .header("User-Agent", if (mobileUserAgent) MOBILE_USER_AGENT else DESKTOP_USER_AGENT)
            .header("Referer", referer)
            .header("Accept", accept)
    }

    private companion object {
        val MIXIN_KEY_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )
        val WBI_FILTER_REGEX = Regex("""[!'()*]""")
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}
