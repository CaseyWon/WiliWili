package com.example.bilimini.ui.screen.dynamic

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    onOpenUserSpace: (Long) -> Unit,
) {
    val sessionState by sessionManager.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    var itemsState by remember { mutableStateOf<List<DynamicItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        if (!sessionState.isLoggedIn) {
            itemsState = emptyList()
            message = "\u767b\u5f55\u540e\u53ef\u4ee5\u67e5\u770b\u66f4\u5b8c\u6574\u7684\u52a8\u6001\u5185\u5bb9\u3002"
            return
        }
        loading = true
        val result = repository.fetchDynamicItems()
        itemsState = result
        message = if (result.isEmpty()) {
            "\u6682\u65f6\u6ca1\u6709\u8bfb\u5230\u52a8\u6001\u5185\u5bb9\uff0c\u53ef\u80fd\u662f\u767b\u5f55\u6001\u6216\u63a5\u53e3\u8fd4\u56de\u53d1\u751f\u4e86\u53d8\u5316\u3002"
        } else {
            null
        }
        loading = false
    }

    LaunchedEffect(sessionState.isLoggedIn) {
        load()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageBanner(
                title = "\u52a8\u6001",
                showWordmark = true,
                trailing = {
                    Button(
                        onClick = {
                            if (sessionState.isLoggedIn) {
                                scope.launch { load() }
                            } else {
                                onLoginClick()
                            }
                        },
                    ) {
                        Text(
                            if (sessionState.isLoggedIn) {
                                if (loading) "\u5237\u65b0\u4e2d" else "\u5237\u65b0"
                            } else {
                                "\u53bb\u767b\u5f55"
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
        items(itemsState, key = { it.id }) { item ->
            DynamicCard(
                item = item,
                onClick = (item.bvid ?: item.origin?.bvid)?.let { bvid ->
                    { onOpenVideo(bvid) }
                },
                onAvatarClick = item.authorMid.takeIf { it > 0L }?.let { mid ->
                    { onOpenUserSpace(mid) }
                },
            )
        }
    }
}
