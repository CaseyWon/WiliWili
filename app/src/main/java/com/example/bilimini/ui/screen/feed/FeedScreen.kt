package com.example.bilimini.ui.screen.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.VideoSummary
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.VideoCard
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    repository: BiliRepository,
    onOpenVideo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = true
        errorText = null
        val result = repository.fetchHomeVideos()
        if (result.isEmpty()) {
            errorText = "No feed items were returned. Please try again."
        }
        videos = result
        loading = false
    }

    LaunchedEffect(Unit) {
        load()
    }

    when {
        loading -> LoadingPane("Loading feed...")
        errorText != null -> ErrorPane(
            message = errorText.orEmpty(),
            actionLabel = "Retry",
            onAction = { scope.launch { load() } },
        )

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Feed",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            text = "This MVP feed is built from web-facing Bilibili data and is intentionally replaceable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { scope.launch { load() } }) {
                            Text("Refresh")
                        }
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
    }
}

@Composable
private fun LoadingPane(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(text = message)
        }
    }
}

@Composable
private fun ErrorPane(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
