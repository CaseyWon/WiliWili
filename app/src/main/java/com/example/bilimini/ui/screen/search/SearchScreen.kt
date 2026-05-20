package com.example.bilimini.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun SearchScreen(
    repository: BiliRepository,
    onOpenVideo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var keyword by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageBanner(
                title = "\u641c\u7d22",
                showWordmark = true,
            )
        }
        item {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                label = { Text("\u8bf7\u8f93\u5165\u89c6\u9891\u5173\u952e\u8bcd") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        searching = true
                        errorText = null
                        result = repository.searchVideos(keyword.trim())
                        errorText = if (result.isEmpty()) {
                            "\u6ca1\u6709\u627e\u5230\u7ed3\u679c\uff0c\u6216\u8005\u5f53\u524d\u63a5\u53e3\u6682\u65f6\u4e0d\u53ef\u7528\u3002"
                        } else {
                            null
                        }
                        searching = false
                    }
                },
                enabled = keyword.isNotBlank() && !searching,
            ) {
                Text(if (searching) "\u641c\u7d22\u4e2d" else "\u641c\u7d22")
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
    }
}
