package com.example.bilimini.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class BiliApiEnvelope(
    val code: Int = 0,
    val message: String = "",
    val data: JsonElement? = null,
)

@Serializable
data class OwnerInfo(
    val name: String = "",
    val mid: Long = 0,
    val face: String = "",
)

@Serializable
data class StatInfo(
    val view: JsonElement? = null,
    val danmaku: JsonElement? = null,
)

@Serializable
data class VideoSummaryResponse(
    val bvid: String = "",
    val title: String = "",
    val pic: String = "",
    val duration: Long = 0,
    val owner: OwnerInfo? = null,
    val stat: StatInfo? = null,
    val rcmd_reason: String? = null,
    val author: String = "",
    val play: JsonElement? = null,
    val typename: String? = null,
    val length: String = "",
    val created: JsonElement? = null,
    val aid: Long = 0,
    val cid: Long = 0,
)

@Serializable
data class VideoViewResponse(
    val bvid: String = "",
    val aid: Long = 0,
    val cid: Long = 0,
    val title: String = "",
    val desc: String = "",
    val pic: String = "",
    val duration: Long = 0,
    val pubdate: Long = 0,
    val owner: OwnerInfo? = null,
    val stat: StatInfo? = null,
)

@Serializable
data class NavResponse(
    val isLogin: Boolean = false,
    val mid: Long = 0,
    val uname: String = "",
    val face: String = "",
    val money: Double? = null,
    val level_info: LevelInfo? = null,
    val wbi_img: JsonElement? = null,
)

@Serializable
data class LevelInfo(
    val current_level: Int = 0,
)

@Serializable
data class CardResponse(
    val card: CardInfo? = null,
)

@Serializable
data class CardInfo(
    val mid: String? = null,
    val name: String = "",
    val face: String = "",
    val sign: String? = null,
    val level_info: LevelInfo? = null,
)

fun StatInfo.longValue(name: String): Long? {
    val element = when (name) {
        "view" -> view
        "danmaku" -> danmaku
        else -> return null
    }
    return element?.jsonPrimitive?.longOrNull
}

fun VideoSummaryResponse.toVideoSummary(
    decodeHtml: (String?) -> String,
    normalizeUrl: (String) -> String,
    formatDuration: (Long) -> String,
    formatCount: (Long?) -> String,
): VideoSummary {
    return VideoSummary(
        bvid = bvid,
        title = decodeHtml(title),
        author = owner?.name.orEmpty(),
        coverUrl = normalizeUrl(pic),
        durationText = formatDuration(duration),
        playText = formatCount(stat?.longValue("view")),
        badge = rcmd_reason,
        durationSeconds = duration,
        playCount = stat?.longValue("view"),
    )
}

fun VideoSummaryResponse.toSearchSummary(): VideoSummary {
    return VideoSummary(
        bvid = bvid,
        title = author.ifBlank { "无标题" },
        author = author,
        coverUrl = pic,
        durationText = length.ifBlank { "--:--" },
        playText = play?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "--" },
        badge = typename,
    )
}

fun VideoSummaryResponse.toUserVideoSummary(
    decodeHtml: (String?) -> String,
    normalizeUrl: (String) -> String,
): VideoSummary {
    return VideoSummary(
        bvid = bvid,
        title = decodeHtml(title),
        author = decodeHtml(author).ifBlank { "UP 主" },
        coverUrl = normalizeUrl(pic),
        durationText = length.ifBlank { formatDurationText(duration.toString()) },
        playText = play?.jsonPrimitive?.contentOrNull.orEmpty().ifBlank { "--" },
        badge = if (created != null) "最新发布" else null,
    )
}

private fun formatDurationText(raw: String): String {
    val seconds = raw.toLongOrNull() ?: return "--:--"
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs)
    else "%02d:%02d".format(minutes, secs)
}
