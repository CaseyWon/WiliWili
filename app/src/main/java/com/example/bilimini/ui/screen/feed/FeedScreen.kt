package com.example.bilimini.ui.screen.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.VideoSummary
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.PageBanner
import com.example.bilimini.ui.components.VideoCard
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    repository: BiliRepository,
    onOpenVideo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(1) }
    var videos by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    suspend fun load(page: Int) {
        loading = true
        errorText = null
        val result = repository.fetchHomeVideos(page)
        videos = result
        errorText = if (result.isEmpty()) {
            "\u6682\u65f6\u6ca1\u6709\u62ff\u5230\u63a8\u8350\u5185\u5bb9\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002"
        } else {
            null
        }
        loading = false
    }

    LaunchedEffect(Unit) {
        load(currentPage)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageBanner(
                title = "\u9996\u9875\u63a8\u8350",
                showWordmark = true,
                trailing = {
                    Button(
                        onClick = {
                            scope.launch {
                                val nextPage = if (currentPage >= 8) 1 else currentPage + 1
                                currentPage = nextPage
                                load(nextPage)
                            }
                        },
                    ) {
                        Text(if (loading) "\u5237\u65b0\u4e2d" else "\u6362\u4e00\u6279")
                    }
                },
            )
        }
        if (errorText != null) {
            item {
                Text(
                    text = errorText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items(videos, key = { it.bvid }) { video ->
            VideoCard(
                video = video,
                onClick = { onOpenVideo(video.bvid) },
            )
        }
    }
}
