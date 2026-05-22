package com.example.bilimini.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun SearchScreen(
    repository: BiliRepository,
    onOpenVideo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var keyword by remember { mutableStateOf("") }
    var lastKeyword by remember { mutableStateOf("") }
    var currentPage by remember { mutableStateOf(1) }
    var searching by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (!loadingMore && hasMore && lastKeyword.isNotBlank() && lastVisible >= totalItems - 3 && totalItems > 0) {
                loadingMore = true
                val nextPage = currentPage + 1
                val more = repository.searchVideos(lastKeyword, nextPage)
                if (more.isNotEmpty()) {
                    currentPage = nextPage
                    result = result + more
                } else {
                    hasMore = false
                }
                loadingMore = false
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageBanner(
                title = "搜索",
                showWordmark = true,
            )
        }
        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("请输入视频关键词") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        lastKeyword = keyword.trim()
                        currentPage = 1
                        hasMore = true
                        searching = true
                        errorText = null
                        result = repository.searchVideos(lastKeyword, 1)
                        if (result.isEmpty()) {
                            hasMore = false
                            errorText = "没有找到结果，或者当前接口暂时不可用。"
                        }
                        searching = false
                    }
                },
                enabled = keyword.isNotBlank() && !searching,
            ) {
                Text(if (searching) "搜索中" else "搜索")
            }
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
        items(result, key = { it.bvid }) { video ->
            VideoCard(
                video = video,
                onClick = { onOpenVideo(video.bvid) },
            )
        }
        if (loadingMore) {
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
