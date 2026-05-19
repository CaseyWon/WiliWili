package com.example.bilimini.data.repository

import androidx.core.text.HtmlCompat
import com.example.bilimini.data.api.BiliApiClient
import com.example.bilimini.data.model.UserProfile
import com.example.bilimini.data.model.VideoDetail
import com.example.bilimini.data.model.VideoStat
import com.example.bilimini.data.model.VideoSummary
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

class BiliRepository(
    private val apiClient: BiliApiClient,
) {
    suspend fun fetchHomeVideos(page: Int = 1): List<VideoSummary> {
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/popular",
            params = mapOf(
                "pn" to page.toString(),
                "ps" to "20",
            ),
        )
        val payload = apiClient.getJson(url) ?: return emptyList()
        val list = payload.dataObject()?.arrayValue("list").orEmpty()
        return list.mapNotNull(::parseVideoSummary)
    }

    suspend fun searchVideos(
        keyword: String,
        page: Int = 1,
    ): List<VideoSummary> {
        if (keyword.isBlank()) {
            return emptyList()
        }
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/search/type",
            params = mapOf(
                "search_type" to "video",
                "keyword" to keyword,
                "page" to page.toString(),
            ),
        )
        val payload = apiClient.getJson(url) ?: return emptyList()
        val result = payload.dataObject()?.arrayValue("result").orEmpty()
        return result.mapNotNull(::parseSearchSummary)
    }

    suspend fun fetchVideoDetail(bvid: String): VideoDetail? {
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/view",
            params = mapOf("bvid" to bvid),
        )
        val payload = apiClient.getJson(url) ?: return null
        val data = payload.dataObject() ?: return null
        val stat = data.objectValue("stat")
        return VideoDetail(
            bvid = data.stringValue("bvid").orEmpty(),
            aid = data.longValue("aid") ?: 0L,
            cid = data.longValue("cid") ?: 0L,
            title = decodeHtml(data.stringValue("title")),
            description = data.stringValue("desc").orEmpty().ifBlank { "No description yet." },
            coverUrl = data.stringValue("pic").orEmpty(),
            author = data.objectValue("owner")?.stringValue("name").orEmpty(),
            ownerMid = data.objectValue("owner")?.longValue("mid") ?: 0L,
            durationText = formatDuration(data.longValue("duration") ?: 0L),
            publishedText = formatUnixTime(data.longValue("pubdate")),
            stats = listOf(
                VideoStat("Views", formatCount(stat?.longValue("view"))),
                VideoStat("Danmaku", formatCount(stat?.longValue("danmaku"))),
                VideoStat("Likes", formatCount(stat?.longValue("like"))),
                VideoStat("Favs", formatCount(stat?.longValue("favorite"))),
            ),
        )
    }

    suspend fun fetchCurrentUser(): UserProfile? {
        val payload = apiClient.getJson(
            url = "https://api.bilibili.com/x/web-interface/nav",
            authenticated = true,
        ) ?: return null
        val data = payload.dataObject() ?: return null
        val isLogin = data.primitive("isLogin")?.booleanOrNull ?: false
        if (!isLogin) {
            return null
        }
        return UserProfile(
            mid = data.longValue("mid") ?: 0L,
            name = data.stringValue("uname").orEmpty(),
            avatarUrl = data.stringValue("face").orEmpty(),
            level = data.objectValue("level_info")?.primitive("current_level")?.contentOrNull?.toIntOrNull(),
            sign = data.stringValue("sign"),
            coins = data.primitive("money")?.doubleOrNull,
        )
    }

    fun buildVideoPageUrl(bvid: String): String = "https://m.bilibili.com/video/$bvid"

    private fun parseVideoSummary(element: JsonElement): VideoSummary? {
        val item = element as? JsonObject ?: return null
        return VideoSummary(
            bvid = item.stringValue("bvid").orEmpty(),
            title = decodeHtml(item.stringValue("title")),
            author = item.objectValue("owner")?.stringValue("name").orEmpty(),
            coverUrl = normalizeCoverUrl(item.stringValue("pic").orEmpty()),
            durationText = formatDuration(item.longValue("duration") ?: 0L),
            playText = formatCount(item.objectValue("stat")?.longValue("view")),
            badge = item.stringValue("rcmd_reason"),
        )
    }

    private fun parseSearchSummary(element: JsonElement): VideoSummary? {
        val item = element as? JsonObject ?: return null
        return VideoSummary(
            bvid = item.stringValue("bvid").orEmpty(),
            title = decodeHtml(item.stringValue("title")),
            author = item.stringValue("author").orEmpty(),
            coverUrl = normalizeCoverUrl(item.stringValue("pic").orEmpty()),
            durationText = item.stringValue("duration").orEmpty(),
            playText = item.stringValue("play").orEmpty().ifBlank { "--" },
            badge = item.stringValue("typename"),
        )
    }

    private fun JsonObject.dataObject(): JsonObject? = this["data"]?.jsonObject

    private fun JsonObject.objectValue(key: String): JsonObject? = this[key]?.jsonObject

    private fun JsonObject.arrayValue(key: String): List<JsonElement> {
        return this[key]?.let { element ->
            runCatching { element.jsonArray.toList() }.getOrDefault(emptyList())
        }.orEmpty()
    }

    private fun JsonObject.stringValue(key: String): String? = primitive(key)?.contentOrNull

    private fun JsonObject.longValue(key: String): Long? = primitive(key)?.longOrNull

    private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    private fun normalizeCoverUrl(url: String): String {
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http") -> url
            else -> "https://$url"
        }
    }

    private fun decodeHtml(source: String?): String {
        return HtmlCompat.fromHtml(source.orEmpty(), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    }

    private fun formatCount(value: Long?): String {
        val safe = value ?: return "--"
        return when {
            safe >= 100_000_000L -> String.format("%.1fB", safe / 100_000_000f)
            safe >= 10_000L -> String.format("%.1fW", safe / 10_000f)
            else -> safe.toString()
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds <= 0) {
            return "--:--"
        }
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, secs)
        } else {
            "%02d:%02d".format(minutes, secs)
        }
    }

    private fun formatUnixTime(value: Long?): String {
        val safe = value ?: return "Unknown"
        val instant = java.time.Instant.ofEpochSecond(safe)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }
}
