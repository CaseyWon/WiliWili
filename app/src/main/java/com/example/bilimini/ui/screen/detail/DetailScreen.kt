package com.example.bilimini.ui.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.VideoDetail
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.RemoteImage

@Composable
fun DetailScreen(
    bvid: String,
    repository: BiliRepository,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
) {
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(bvid) {
        loading = true
        detail = repository.fetchVideoDetail(bvid)
        loading = false
    }

    when {
        loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        detail == null -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Failed to load video details")
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }

        else -> DetailContent(
            detail = detail!!,
            onBack = onBack,
            onPlayClick = onPlayClick,
        )
    }
}

@Composable
private fun DetailContent(
    detail: VideoDetail,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Video details",
                    style = MaterialTheme.typography.headlineSmall,
                )
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(24.dp)),
            ) {
                RemoteImage(
                    imageUrl = detail.coverUrl,
                    contentDescription = detail.title,
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Creator ${detail.author} | Published ${detail.publishedText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                detail.stats.forEach { stat ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${stat.label} ${stat.value}") },
                    )
                }
            }
        }
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Playback strategy",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "This MVP uses the embedded web player first so playback can work sooner. We can replace it with a more native player later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Button(
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Play now")
            }
        }
    }
}
