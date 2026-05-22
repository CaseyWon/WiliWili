package com.example.bilimini.data.repository

import androidx.core.text.HtmlCompat
import com.example.bilimini.data.api.BiliApiClient
import com.example.bilimini.data.model.DanmakuItem
import com.example.bilimini.data.model.DynamicItem
import com.example.bilimini.data.model.DynamicOrigin
import com.example.bilimini.data.model.PlayableSource
import com.example.bilimini.data.model.UserProfile
import com.example.bilimini.data.model.VideoDetail
import com.example.bilimini.data.model.VideoStat
import com.example.bilimini.data.model.VideoSummary
import com.example.bilimini.data.recommendation.HomeFeedRanker
import com.example.bilimini.data.recommendation.RecommendationProfileStore
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.longOrNull

class BiliRepository(
    private val apiClient: BiliApiClient,
    private val homeFeedRanker: HomeFeedRanker,
    private val recommendationProfileStore: RecommendationProfileStore,
) {
    private val videoDetailCache = mutableMapOf<String, VideoDetail>()
    suspend fun fetchHomeVideos(page: Int = 1): List<VideoSummary> {
        val recommendUrl = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/index/top/feed/rcmd",
            params = mapOf(
                "fresh_type" to "4",
                "feed_version" to "V8",
                "fresh_idx" to page.toString(),
                "fresh_idx_1h" to page.toString(),
                "brush" to page.toString(),
                "homepage_ver" to "1",
                "ps" to "20",
            ),
        )
        val recommendPayload = apiClient.getJson(recommendUrl)
        val recommendItems = recommendPayload
            ?.dataObject()
            ?.arrayValue("item")
            .orEmpty()
            .mapNotNull(::parseVideoSummary)
        if (recommendItems.isNotEmpty()) {
            return homeFeedRanker.rerank(recommendItems)
        }

        val popularUrl = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/popular",
            params = mapOf(
                "pn" to page.toString(),
                "ps" to "20",
            ),
        )
        val popularPayload = apiClient.getJson(popularUrl) ?: return emptyList()
        val popularItems = popularPayload.dataObject()
            ?.arrayValue("list")
            .orEmpty()
            .mapNotNull(::parseVideoSummary)
        return homeFeedRanker.rerank(popularItems)
    }

    suspend fun fetchDynamicItems(page: Int = 1): List<DynamicItem> {
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all",
            params = mapOf(
                "page" to page.toString(),
                "type" to "all",
            ),
        )
        val payload = apiClient.getJson(
            url = url,
            authenticated = true,
        ) ?: return emptyList()
        val items = payload.dataObject()
            ?.arrayValue("items")
            .orEmpty()
            .mapNotNull(::parseDynamicItem)
        // Enrich items with blank text (e.g. DRAW items without desc in feed API)
        if (page == 1 && items.isNotEmpty()) {
            return enrichDynamicItems(items)
        }
        return items
    }

    suspend fun fetchUserDynamicItems(mid: Long, offset: String? = null): Pair<List<DynamicItem>, String?> {
        if (mid <= 0L) {
            return Pair(emptyList(), null)
        }
        val params = mutableMapOf("host_mid" to mid.toString())
        if (!offset.isNullOrBlank()) {
            params["offset"] = offset
        }
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space",
            params = params,
        )
        val payload = apiClient.getJson(
            url = url,
            authenticated = true,
        ) ?: return Pair(emptyList(), null)
        val data = payload.dataObject() ?: return Pair(emptyList(), null)
        val items = data.arrayValue("items")
            .orEmpty()
            .mapNotNull(::parseDynamicItem)
        val enriched = enrichDynamicItems(items)
        val nextOffset = data.stringValue("offset")
        return Pair(enriched, nextOffset)
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
        val payload = apiClient.getJson(url, authenticated = true) ?: return emptyList()
        return payload.dataObject()
            ?.arrayValue("result")
            .orEmpty()
            .mapNotNull(::parseSearchSummary)
    }

    suspend fun fetchVideoDetail(bvid: String): VideoDetail? {
        videoDetailCache[bvid]?.let { return it }
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/view",
            params = mapOf("bvid" to bvid),
        )
        val payload = apiClient.getJson(url) ?: return null
        val data = payload.dataObject() ?: return null
        val stat = data.objectValue("stat")
        val durationSeconds = data.longValue("duration") ?: 0L
        val detail = VideoDetail(
            bvid = data.stringValue("bvid").orEmpty(),
            aid = data.longValue("aid") ?: 0L,
            cid = data.longValue("cid") ?: 0L,
            title = decodeHtml(data.stringValue("title")),
            description = data.stringValue("desc").orEmpty().ifBlank { "No description yet." },
            coverUrl = normalizeUrl(data.stringValue("pic").orEmpty()),
            author = data.objectValue("owner")?.stringValue("name").orEmpty(),
            ownerMid = data.objectValue("owner")?.longValue("mid") ?: 0L,
            authorAvatar = normalizeUrl(data.objectValue("owner")?.stringValue("face").orEmpty()),
            durationSeconds = durationSeconds,
            durationText = formatDuration(durationSeconds),
            publishedText = formatUnixTime(data.longValue("pubdate")),
            stats = listOf(
                VideoStat("\u64ad\u653e", formatCount(stat?.longValue("view"))),
                VideoStat("\u5f39\u5e55", formatCount(stat?.longValue("danmaku"))),
            ),
        )
        videoDetailCache[bvid] = detail
        return detail
    }

    fun recordVideoOpen(detail: VideoDetail) {
        recommendationProfileStore.recordVideoOpen(detail)
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
            avatarUrl = normalizeUrl(data.stringValue("face").orEmpty()),
            level = data.objectValue("level_info")?.primitive("current_level")?.contentOrNull?.toIntOrNull(),
            sign = data.stringValue("sign"),
            coins = data.primitive("money")?.doubleOrNull,
        )
    }

    suspend fun fetchUserProfile(mid: Long): UserProfile? {
        if (mid <= 0L) {
            return null
        }
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/web-interface/card",
            params = mapOf("mid" to mid.toString()),
        )
        val payload = apiClient.getJson(url) ?: return null
        val card = payload.dataObject()?.objectValue("card") ?: return null
        return UserProfile(
            mid = card.stringValue("mid")?.toLongOrNull() ?: mid,
            name = decodeHtml(card.stringValue("name")).ifBlank { "Bilibili 用户" },
            avatarUrl = normalizeUrl(card.stringValue("face").orEmpty()),
            level = card.objectValue("level_info")?.primitive("current_level")?.contentOrNull?.toIntOrNull(),
            sign = decodeHtml(card.stringValue("sign")),
            coins = null,
        )
    }

    suspend fun fetchUserVideos(
        mid: Long,
        page: Int = 1,
    ): List<VideoSummary> {
        if (mid <= 0L) {
            return emptyList()
        }
        val url = apiClient.buildWbiSignedUrl(
            base = "https://api.bilibili.com/x/space/wbi/arc/search",
            params = mapOf(
                "mid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to "20",
                "order" to "pubdate",
                "tid" to "0",
            ),
        ) ?: return emptyList()
        val payload = apiClient.getJson(
            url = url,
            authenticated = true,
        ) ?: return emptyList()
        return payload.dataObject()
            ?.objectValue("list")
            ?.arrayValue("vlist")
            .orEmpty()
            .mapNotNull(::parseUserVideoSummary)
    }

    suspend fun fetchPlayableSource(bvid: String): PlayableSource? {
        val detail = fetchVideoDetail(bvid) ?: return null
        fetchPlayableSourceFromApi(detail)?.let { return it }
        return fetchPlayableSourceFromHtml(detail.bvid)
    }

    suspend fun fetchDanmaku(cid: Long): List<DanmakuItem> {
        if (cid <= 0) {
            return emptyList()
        }
        val xml = apiClient.getText(
            url = "https://api.bilibili.com/x/v1/dm/list.so?oid=$cid",
            authenticated = true,
            referer = "https://www.bilibili.com/",
        ) ?: return emptyList()
        return DANMAKU_REGEX.findAll(xml).mapNotNull { match ->
            val meta = match.groups[1]?.value?.split(",").orEmpty()
            if (meta.size < 4) {
                return@mapNotNull null
            }
            val timeMs = (meta[0].toDoubleOrNull()?.times(1000))?.toLong() ?: return@mapNotNull null
            DanmakuItem(
                id = meta.getOrNull(7).orEmpty(),
                timeMs = timeMs,
                text = decodeHtml(match.groups[2]?.value),
                mode = meta.getOrNull(1)?.toIntOrNull() ?: 1,
                color = meta.getOrNull(3)?.toLongOrNull() ?: 0xFFFFFFFF,
            )
        }.toList()
    }

    fun buildVideoPageUrl(bvid: String): String = "https://m.bilibili.com/video/$bvid"

    private suspend fun fetchPlayableSourceFromApi(detail: VideoDetail): PlayableSource? {
        if (detail.cid == 0L) {
            return null
        }
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/player/playurl",
            params = mapOf(
                "bvid" to detail.bvid,
                "cid" to detail.cid.toString(),
                "fnval" to "4048",
                "fourk" to "1",
                "qn" to "80",
            ),
        )
        val payload = apiClient.getJson(
            url = url,
            authenticated = true,
        ) ?: return null
        return parsePlayablePayload(
            payload = payload.dataObject(),
            fallbackPageUrl = buildVideoPageUrl(detail.bvid),
        )
    }

    private suspend fun fetchPlayableSourceFromHtml(bvid: String): PlayableSource? {
        val html = apiClient.getText(
            url = "https://www.bilibili.com/video/$bvid",
            authenticated = true,
            referer = "https://www.bilibili.com/",
        ) ?: return null
        val playInfoText = PLAY_INFO_REGEX.find(html)?.groups?.get(1)?.value ?: return null
        val payload = apiClient.parseJson(playInfoText) ?: return null
        return parsePlayablePayload(
            payload = (payload["data"] as? JsonObject) ?: payload,
            fallbackPageUrl = buildVideoPageUrl(bvid),
        )
    }

    private fun parsePlayablePayload(
        payload: JsonObject?,
        fallbackPageUrl: String,
    ): PlayableSource? {
        payload ?: return null
        val dash = payload.objectValue("dash")
        if (dash != null) {
            val videoObject = dash.arrayValue("video")
                .mapNotNull { it as? JsonObject }
                .sortedByDescending { it.longValue("bandwidth") ?: 0L }
                .firstOrNull()
            val audioObject = dash.arrayValue("audio")
                .mapNotNull { it as? JsonObject }
                .sortedByDescending { it.longValue("bandwidth") ?: 0L }
                .firstOrNull()
            val videoUrl = videoObject?.stringValue("baseUrl") ?: videoObject?.stringValue("base_url")
            if (!videoUrl.isNullOrBlank()) {
                return PlayableSource(
                    videoUrl = videoUrl,
                    audioUrl = audioObject?.stringValue("baseUrl") ?: audioObject?.stringValue("base_url"),
                    headers = apiClient.defaultMediaHeaders(),
                    fallbackPageUrl = fallbackPageUrl,
                    qualityLabel = "Q${videoObject?.stringValue("id") ?: "auto"}",
                )
            }
        }

        val directUrl = payload.arrayValue("durl")
            .mapNotNull { it as? JsonObject }
            .firstOrNull()
            ?.stringValue("url")
        if (!directUrl.isNullOrBlank()) {
            return PlayableSource(
                videoUrl = directUrl,
                headers = apiClient.defaultMediaHeaders(),
                fallbackPageUrl = fallbackPageUrl,
                qualityLabel = "Direct",
            )
        }
        return null
    }

    private fun parseVideoSummary(element: JsonElement): VideoSummary? {
        val item = element as? JsonObject ?: return null
        val durationSeconds = item.longValue("duration")
        val playCount = item.objectValue("stat")?.longValue("view")
        return VideoSummary(
            bvid = item.stringValue("bvid").orEmpty(),
            title = decodeHtml(item.stringValue("title")),
            author = item.objectValue("owner")?.stringValue("name").orEmpty(),
            coverUrl = normalizeUrl(item.stringValue("pic").orEmpty()),
            durationText = formatDuration(durationSeconds ?: 0L),
            playText = formatCount(playCount),
            badge = item.stringValue("rcmd_reason"),
            durationSeconds = durationSeconds,
            playCount = playCount,
        )
    }

    private fun parseSearchSummary(element: JsonElement): VideoSummary? {
        val item = element as? JsonObject ?: return null
        return VideoSummary(
            bvid = item.stringValue("bvid").orEmpty(),
            title = decodeHtml(item.stringValue("title")),
            author = item.stringValue("author").orEmpty(),
            coverUrl = normalizeUrl(item.stringValue("pic").orEmpty()),
            durationText = item.stringValue("duration").orEmpty(),
            playText = item.stringValue("play").orEmpty().ifBlank { "--" },
            badge = item.stringValue("typename"),
            durationSeconds = parseDurationText(item.stringValue("duration").orEmpty()),
            playCount = parseCountText(item.stringValue("play")),
        )
    }

    suspend fun enrichDynamicDetail(idStr: String): DynamicItem? {
        val url = apiClient.buildUrl(
            base = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail",
            params = mapOf("id" to idStr, "features" to "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote"),
        )
        val payload = apiClient.getJson(url = url, authenticated = true) ?: return null
        val data = payload.dataObject() ?: return null
        val item = data.objectValue("item") ?: return null
        return parseDynamicItem(item)
    }

    private suspend fun enrichDynamicItems(items: List<DynamicItem>): List<DynamicItem> {
        val needEnrich = items.filter { it.text.isBlank() }
        if (needEnrich.isEmpty()) return items
        return coroutineScope {
            needEnrich.map { item ->
                async {
                    enrichDynamicDetail(item.id)?.let { enriched ->
                        item.copy(
                            text = enriched.text,
                            title = enriched.title.ifBlank { item.title },
                            images = enriched.images.ifEmpty { item.images },
                            coverUrl = enriched.coverUrl.ifBlank { item.coverUrl },
                        )
                    } ?: item
                }
            }.awaitAll().let { enrichedList ->
                val enrichedById = enrichedList.associateBy { it.id }
                items.map { enrichedById[it.id] ?: it }
            }
        }
    }

    private fun parseDynamicItem(element: JsonElement): DynamicItem? {
        val item = element as? JsonObject ?: return null
        val modules = item.objectValue("modules")
        val author = modules?.objectValue("module_author")
        val content = parseDynamicContent(item)

        return DynamicItem(
            id = item.stringValue("id_str")
                ?: item.stringValue("id")
                ?: return null,
            authorMid = author?.longValue("mid") ?: 0L,
            author = decodeHtml(author?.stringValue("name")).ifBlank { "Bilibili 用户" },
            avatarUrl = normalizeUrl(author?.stringValue("face").orEmpty()),
            publishText = author?.stringValue("pub_time").orEmpty().ifBlank {
                formatUnixTime(author?.longValue("pub_ts"))
            },
            text = content.text,
            title = content.title,
            coverUrl = content.coverUrl,
            badge = content.badge,
            bvid = content.bvid,
            images = content.images,
            origin = content.origin,
        )
    }

    private fun parseUserVideoSummary(element: JsonElement): VideoSummary? {
        val item = element as? JsonObject ?: return null
        val durationText = item.stringValue("length").orEmpty()
        val playValue = item.longValue("play") ?: item.stringValue("play")?.toLongOrNull()
        return VideoSummary(
            bvid = item.stringValue("bvid").orEmpty(),
            title = decodeHtml(item.stringValue("title")),
            author = decodeHtml(item.stringValue("author")).ifBlank { "UP 主" },
            coverUrl = normalizeUrl(item.stringValue("pic").orEmpty()),
            durationText = durationText.ifBlank { formatDuration(item.longValue("length") ?: 0L) },
            playText = formatCount(playValue),
            badge = item.stringValue("created")?.let { "最新发布" },
            durationSeconds = parseDurationText(durationText),
            playCount = playValue,
        )
    }

    private fun parseDynamicContent(item: JsonObject): DynamicContent {
        val modules = item.objectValue("modules")
        val dynamic = modules?.objectValue("module_dynamic")
        val major = dynamic?.objectValue("major")
        val descText = extractDynamicText(dynamic?.objectValue("desc"))
        // Broad fallback: try to get text directly from item top-level fields
        val itemTextFallback = listOfNotNull(
            item.stringValue("content"),
            item.stringValue("text"),
        ).firstOrNull { it.isNotBlank() }?.let(::decodeHtml).orEmpty()
        val type = item.stringValue("type")
        val badge = mapDynamicType(type, major?.stringValue("type"))
        val archive = major?.objectValue("archive")
        val draw = major?.objectValue("draw")
        val article = major?.objectValue("article")
        val opus = major?.objectValue("opus")
        val common = major?.objectValue("common")
        val pgc = major?.objectValue("pgc")
        val live = major?.objectValue("live_rcmd")
        val liveContent = live?.objectValue("content")
        val origin = item.objectValue("orig")?.let(::parseDynamicOrigin)

        val baseContent = when {
            archive != null -> DynamicContent(
                title = decodeHtml(archive.stringValue("title")).ifBlank { "发布了视频" },
                text = firstNotBlank(
                    descText,
                    decodeHtml(archive.stringValue("desc")),
                    "打开查看视频详情",
                ),
                coverUrl = normalizeUrl(archive.stringValue("cover").orEmpty()),
                badge = badge ?: "视频",
                bvid = archive.stringValue("bvid"),
            )

            draw != null -> {
                val drawItems = draw.arrayValue("items")
                val drawText = firstNotBlank(descText, extractDrawText(draw), itemTextFallback, "")
                DynamicContent(
                    title = drawText.take(80),
                    text = drawText,
                    coverUrl = normalizeUrl(
                        (drawItems.firstOrNull() as? JsonObject)?.stringValue("src").orEmpty(),
                    ),
                    badge = badge ?: "图文",
                    bvid = null,
                    images = drawItems.mapNotNull { item ->
                        normalizeUrl((item as? JsonObject)?.stringValue("src").orEmpty())
                            .takeIf { it.isNotBlank() }
                    },
                )
            }

            opus != null -> {
                val opusText = firstNotBlank(descText, extractOpusSummary(opus), itemTextFallback, "")
                DynamicContent(
                    title = decodeHtml(opus.stringValue("title")).orEmpty().takeIf { it.isNotBlank() }
                        ?: opusText.take(80),
                    text = opusText,
                    coverUrl = normalizeUrl(extractOpusCover(opus)),
                    badge = badge ?: "图文",
                    bvid = null,
                    images = opus.arrayValue("pics").mapNotNull { pic ->
                        normalizeUrl((pic as? JsonObject)?.stringValue("url").orEmpty())
                            .takeIf { it.isNotBlank() }
                    },
                )
            }

            article != null -> DynamicContent(
                title = decodeHtml(article.stringValue("title")).ifBlank { "发布了专栏" },
                text = firstNotBlank(descText, decodeHtml(article.stringValue("desc")), "打开查看专栏内容"),
                coverUrl = normalizeUrl(
                    article.arrayValue("covers")
                        .firstOrNull()
                        ?.let { (it as? JsonPrimitive)?.contentOrNull }
                        .orEmpty(),
                ),
                badge = badge ?: "专栏",
                bvid = null,
            )

            common != null -> DynamicContent(
                title = decodeHtml(common.stringValue("title")).ifBlank { badge ?: "动态" },
                text = firstNotBlank(
                    descText,
                    decodeHtml(common.stringValue("desc")),
                    "打开查看动态详情",
                ),
                coverUrl = normalizeUrl(common.stringValue("cover").orEmpty()),
                badge = badge,
                bvid = extractBvidFromUrl(common.stringValue("jump_url")),
            )

            pgc != null -> DynamicContent(
                title = decodeHtml(pgc.stringValue("title")).ifBlank { "发布了番剧内容" },
                text = firstNotBlank(
                    descText,
                    decodeHtml(pgc.stringValue("sub_title")),
                    "打开查看番剧内容",
                ),
                coverUrl = normalizeUrl(pgc.stringValue("cover").orEmpty()),
                badge = badge ?: "番剧",
                bvid = extractBvidFromUrl(pgc.stringValue("jump_url")),
            )

            liveContent != null -> DynamicContent(
                title = decodeHtml(liveContent.stringValue("title")).ifBlank { "直播推荐" },
                text = firstNotBlank(
                    descText,
                    decodeHtml(liveContent.stringValue("desc_first")),
                    "打开查看直播内容",
                ),
                coverUrl = normalizeUrl(
                    liveContent.stringValue("cover")
                        ?: liveContent.stringValue("cover_from_user")
                        ?: "",
                ),
                badge = badge ?: "直播",
                bvid = null,
            )

            type == "DYNAMIC_TYPE_FORWARD" -> DynamicContent(
                title = "转发了动态",
                text = descText.ifBlank { "下方是原动态内容" },
                coverUrl = "",
                badge = badge ?: "转发",
                bvid = null,
                origin = origin ?: DynamicOrigin(
                    authorMid = 0L,
                    author = "原动态",
                    title = "原动态暂不可见",
                    text = "这条转发的原始内容目前没有从接口里拿到。",
                    coverUrl = "",
                    badge = "转发",
                    bvid = null,
                ),
            )

            else -> DynamicContent(
                title = "",
                text = firstNotBlank(descText, itemTextFallback, "打开查看动态详情"),
                coverUrl = "",
                badge = badge,
                bvid = null,
            )
        }

        return if (type == "DYNAMIC_TYPE_FORWARD" && baseContent.origin == null) {
            baseContent.copy(origin = origin)
        } else {
            baseContent
        }
    }

    private fun parseDynamicOrigin(item: JsonObject): DynamicOrigin {
        val modules = item.objectValue("modules")
        val author = modules?.objectValue("module_author")
        val content = parseDynamicContent(item).copy(origin = null)
        return DynamicOrigin(
            authorMid = author?.longValue("mid") ?: 0L,
            author = decodeHtml(author?.stringValue("name")).ifBlank { "原动态作者" },
            title = content.title,
            text = content.text,
            coverUrl = content.coverUrl,
            badge = content.badge,
            bvid = content.bvid,
        )
    }

    private fun extractDynamicText(desc: JsonObject?): String {
        desc ?: return ""
        val directText = decodeHtml(desc.stringValue("text"))
        if (directText.isNotBlank()) {
            return directText
        }
        return desc.arrayValue("rich_text_nodes")
            .mapNotNull { node ->
                (node as? JsonObject)?.stringValue("text")
            }
            .joinToString(separator = "")
            .let(::decodeHtml)
            .trim()
    }

    private fun extractDrawText(draw: JsonObject): String {
        val fromItems = draw.arrayValue("items")
            .mapNotNull { node ->
                (node as? JsonObject)?.stringValue("desc")
            }
            .joinToString(separator = "\n")
            .let(::decodeHtml)
            .trim()
        if (fromItems.isNotBlank()) return fromItems
        // fallback: try content/description at draw root level
        return decodeHtml(draw.stringValue("content") ?: draw.stringValue("text") ?: "").trim()
    }

    private fun extractOpusSummary(opus: JsonObject): String {
        // try multiple possible text locations
        val sources = listOfNotNull(
            opus.stringValue("text"),
            opus.objectValue("summary")?.stringValue("text"),
            opus.objectValue("content")?.stringValue("text"),
            opus.stringValue("sub_title"),
        )
        sources.firstOrNull { it.isNotBlank() }?.let {
            return decodeHtml(it).trim()
        }
        // try rich text nodes in summary
        return opus.objectValue("summary")
            ?.arrayValue("rich_text_nodes")
            .orEmpty()
            .mapNotNull { node -> (node as? JsonObject)?.stringValue("text") }
            .joinToString(separator = "")
            .let(::decodeHtml)
            .trim()
    }

    private fun extractOpusCover(opus: JsonObject): String {
        return opus.arrayValue("pics")
            .mapNotNull { it as? JsonObject }
            .firstOrNull()
            ?.stringValue("url")
            .orEmpty()
    }

    private fun mapDynamicType(
        type: String?,
        majorType: String?,
    ): String? {
        return when {
            type == "DYNAMIC_TYPE_FORWARD" -> "转发"
            type == "DYNAMIC_TYPE_AV" || majorType == "MAJOR_TYPE_ARCHIVE" -> "视频"
            type == "DYNAMIC_TYPE_DRAW" || majorType == "MAJOR_TYPE_DRAW" || majorType == "MAJOR_TYPE_OPUS" -> "图文"
            type == "DYNAMIC_TYPE_WORD" || type == "DYNAMIC_TYPE_NONE" -> "动态"
            type == "DYNAMIC_TYPE_ARTICLE" || majorType == "MAJOR_TYPE_ARTICLE" -> "专栏"
            type == "DYNAMIC_TYPE_PGC" || majorType == "MAJOR_TYPE_PGC" -> "番剧"
            majorType == "MAJOR_TYPE_COMMON" -> "活动"
            majorType == "MAJOR_TYPE_LIVE_RCMD" -> "直播"
            else -> null
        }
    }

    private fun extractBvidFromUrl(url: String?): String? {
        val safe = url.orEmpty()
        return BV_REGEX.find(safe)?.value
    }

    private fun firstNotBlank(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()
    }

    private fun JsonObject.dataObject(): JsonObject? = this["data"] as? JsonObject

    private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arrayValue(key: String): List<JsonElement> {
        return (this[key] as? JsonArray)?.toList().orEmpty()
    }

    private fun JsonObject.stringValue(key: String): String? = primitive(key)?.contentOrNull

    private fun JsonObject.longValue(key: String): Long? = primitive(key)?.longOrNull

    private fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

    private fun normalizeUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> url.replaceFirst("http://", "https://")
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
            safe >= 100_000_000L -> String.format("%.1f\u4ebf", safe / 100_000_000f)
            safe >= 10_000L -> String.format("%.1f\u4e07", safe / 10_000f)
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

    private fun parseDurationText(value: String): Long? {
        val parts = value.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) {
            return null
        }
        return when (parts.size) {
            2 -> (parts[0] * 60) + parts[1]
            3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2]
            else -> null
        }
    }

    private fun parseCountText(value: String?): Long? {
        val normalized = value.orEmpty().trim()
        if (normalized.isBlank()) {
            return null
        }
        val compact = normalized.replace(",", "").replace(" ", "")
        return when {
            compact.endsWith("亿") -> (compact.removeSuffix("亿").toDoubleOrNull()?.times(100_000_000))?.toLong()
            compact.endsWith("万") -> (compact.removeSuffix("万").toDoubleOrNull()?.times(10_000))?.toLong()
            else -> compact.toLongOrNull()
        }
    }

    private fun formatUnixTime(value: Long?): String {
        val safe = value ?: return "刚刚"
        val instant = java.time.Instant.ofEpochSecond(safe)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(java.time.ZoneId.systemDefault())
        return formatter.format(instant)
    }

    private companion object {
        val PLAY_INFO_REGEX = Regex("""__playinfo__=(\{.*?\})</script>""")
        val DANMAKU_REGEX = Regex("""<d p="([^"]+)">(.*?)</d>""")
        val BV_REGEX = Regex("""BV[0-9A-Za-z]+""")
    }

	    private data class DynamicContent(
	        val title: String,
	        val text: String,
	        val coverUrl: String,
	        val badge: String?,
	        val bvid: String?,
	        val images: List<String> = emptyList(),
	        val origin: DynamicOrigin? = null,
	    )
}
