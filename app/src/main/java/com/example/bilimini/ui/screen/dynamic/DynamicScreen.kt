package com.example.bilimini.ui.screen.dynamic

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.DynamicItem
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.session.SessionManager
import com.example.bilimini.ui.components.DynamicCard
import com.example.bilimini.ui.components.PageBanner
import kotlinx.coroutines.launch

@Composable
fun DynamicScreen(
    repository: BiliRepository,
    sessionManager: SessionManager,
    onLoginClick: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenDetail: (DynamicItem) -> Unit,
    onOpenUserSpace: (Long) -> Unit,
) {
    val sessionState by sessionManager.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var currentPage by remember { mutableStateOf(1) }
    var items by remember { mutableStateOf<List<DynamicItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        if (!sessionState.isLoggedIn) {
            items = emptyList()
            currentPage = 1
            hasMore = false
            message = "登录后可以查看更完整的动态内容。"
            return
        }
        currentPage = 1
        hasMore = true
        loading = true
        message = null
        items = repository.fetchDynamicItems(1)
        if (items.isEmpty()) {
            hasMore = false
            message = "暂时没有读到动态内容，可能是登录态或接口返回发生了变化。"
        }
        loading = false
    }

    LaunchedEffect(sessionState.isLoggedIn) {
        refresh()
    }

    // Infinite scroll
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (!loadingMore && hasMore && sessionState.isLoggedIn && lastVisible >= totalItems - 3 && totalItems > 0) {
                loadingMore = true
                val nextPage = currentPage + 1
                val result = repository.fetchDynamicItems(nextPage)
                if (result.isNotEmpty()) {
                    currentPage = nextPage
                    items = items + result
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
                title = "动态",
                showWordmark = true,
                trailing = {
                    Button(
                        onClick = {
                            if (sessionState.isLoggedIn) {
                                scope.launch { refresh() }
                            } else {
                                onLoginClick()
                            }
                        },
                    ) {
                        Text(
                            if (sessionState.isLoggedIn) {
                                if (loading) "刷新中" else "刷新"
                            } else {
                                "去登录"
                            }
                        )
                    }
                },
            )
        }
        if (message != null) {
            item {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sessionState.isLoggedIn) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
        items(items, key = { it.id }) { item ->
            DynamicCard(
                item = item,
                onClick = if (item.bvid != null || item.origin?.bvid != null) {
                    { onOpenVideo(item.bvid ?: item.origin!!.bvid!!) }
                } else {
                    { onOpenDetail(item) }
                },
                onAvatarClick = item.authorMid.takeIf { it > 0L }?.let { mid ->
                    { onOpenUserSpace(mid) }
                },
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
