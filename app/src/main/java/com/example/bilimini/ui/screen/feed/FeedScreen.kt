package com.example.bilimini.ui.screen.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.VideoSummary
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.PageBanner
import com.example.bilimini.ui.components.VideoCard
import kotlinx.coroutines.launch

class FeedScreenState {
    var currentPage by mutableStateOf(1)
    var videos by mutableStateOf<List<VideoSummary>>(emptyList())
    var loading by mutableStateOf(true)
    var loadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var errorText by mutableStateOf<String?>(null)
    val listState = LazyListState()
}

@Composable
fun FeedScreen(
    repository: BiliRepository,
    state: FeedScreenState,
    onOpenVideo: (bvid: String) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Initial load only if no cached data
    LaunchedEffect(Unit) {
        if (state.videos.isEmpty()) {
            state.loading = true
            state.errorText = null
            val result = repository.fetchHomeVideos(1)
            if (result.isEmpty()) {
                state.hasMore = false
                state.errorText = "暂时没有拿到推荐内容，请稍后再试。"
            }
            state.videos = result
            state.loading = false
        }
    }

    // Infinite scroll: auto-load next page when reaching the bottom
    LaunchedEffect(state.listState) {
        snapshotFlow {
            val layoutInfo = state.listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            // Trigger when within 3 items of the end
            if (!state.loadingMore && state.hasMore && lastVisible >= totalItems - 3 && totalItems > 0) {
                state.loadingMore = true
                val nextPage = state.currentPage + 1
                val result = repository.fetchHomeVideos(nextPage)
                if (result.isNotEmpty()) {
                    state.currentPage = nextPage
                    state.videos = state.videos + result
                } else {
                    state.hasMore = false
                }
                state.loadingMore = false
            }
        }
    }

    LazyColumn(
        state = state.listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            PageBanner(
                title = "首页推荐",
                showWordmark = true,
                trailing = {
                    Button(
                        onClick = {
                            scope.launch {
                                state.currentPage = 1
                                state.hasMore = true
                                state.loading = true
                                state.errorText = null
                                val result = repository.fetchHomeVideos(1)
                                state.videos = result
                                if (result.isEmpty()) {
                                    state.hasMore = false
                                    state.errorText = "暂时没有拿到推荐内容，请稍后再试。"
                                }
                                state.loading = false
                            }
                        },
                    ) {
                        Text(if (state.loading) "刷新中" else "换一批")
                    }
                },
            )
        }
        if (state.errorText != null && state.videos.isEmpty()) {
            item {
                Text(
                    text = state.errorText.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        items(state.videos, key = { it.bvid }) { video ->
            VideoCard(
                video = video,
                onClick = { onOpenVideo(video.bvid) },
            )
        }
        if (state.loadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
