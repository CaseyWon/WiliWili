package com.example.bilimini.ui.screen.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.bilimini.data.model.PlayableSource
import com.example.bilimini.data.model.VideoDetail
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.BiliPlayerView
import com.example.bilimini.ui.components.RemoteImage

@Composable
fun DetailScreen(
    bvid: String,
    repository: BiliRepository,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
    onOpenUserSpace: (Long) -> Unit,
) {
    var detail by remember { mutableStateOf<VideoDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun load() {
        loading = true
        val loadedDetail = repository.fetchVideoDetail(bvid)
        detail = loadedDetail
        loadedDetail?.let(repository::recordVideoOpen)
        loading = false
    }

    LaunchedEffect(bvid) {
        load()
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
            Text("\u89c6\u9891\u8be6\u60c5\u52a0\u8f7d\u5931\u8d25")
        }

        else -> {
            val d = detail
            if (d == null) return@DetailScreen
            DetailContent(
                bvid = bvid,
                repository = repository,
                detail = d,
                onBack = onBack,
                onPlayClick = onPlayClick,
                onOpenUserSpace = onOpenUserSpace,
            )
        }
    }
}
@Composable
private fun DetailContent(
    bvid: String,
    repository: BiliRepository,
    detail: VideoDetail,
    onBack: () -> Unit,
    onPlayClick: () -> Unit,
    onOpenUserSpace: (Long) -> Unit,
) {
    var inlinePlaying by rememberSaveable(bvid) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (inlinePlaying) {
                InlinePlayerCard(
                    bvid = bvid,
                    repository = repository,
                    onEnterFullscreen = onPlayClick,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(232.dp)
                        .clickable { inlinePlaying = true },
                ) {
                    RemoteImage(
                        imageUrl = detail.coverUrl,
                        contentDescription = detail.title,
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(
                    modifier = Modifier
                        .clickable(enabled = detail.ownerMid > 0L) { onOpenUserSpace(detail.ownerMid) },
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteImage(
                        imageUrl = detail.authorAvatar,
                        contentDescription = detail.author,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    )
                    Column {
                        Text(
                            text = detail.author,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "\u53d1\u5e03\u4e8e ${detail.publishedText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.stats.forEach { stat ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${stat.label} ${stat.value}") },
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (inlinePlaying) {
                        onPlayClick()
                    } else {
                        inlinePlaying = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                Text(if (inlinePlaying) "\u5207\u6362\u5230\u5168\u5c4f" else "\u7acb\u5373\u64ad\u653e")
            }
        }
        item {
            Surface(
                modifier = Modifier.padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = detail.description,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InlinePlayerCard(
    bvid: String,
    repository: BiliRepository,
    onEnterFullscreen: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(bvid) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }
    var source by remember(bvid) { mutableStateOf<PlayableSource?>(null) }
    var loading by remember(bvid) { mutableStateOf(true) }
    var errorText by remember(bvid) { mutableStateOf<String?>(null) }

    LaunchedEffect(bvid) {
        loading = true
        errorText = null
        source = repository.fetchPlayableSource(bvid)
        if (source == null) {
            errorText = "\u6682\u65f6\u6ca1\u6709\u62ff\u5230\u53ef\u64ad\u653e\u7684\u89c6\u9891\u6d41\u3002"
        }
        loading = false
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = "\u64ad\u653e\u5668\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(source) {
        val currentSource = source ?: return@LaunchedEffect
        val factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(currentSource.headers)

        val videoSource = ProgressiveMediaSource.Factory(factory)
            .createMediaSource(MediaItem.fromUri(currentSource.videoUrl))
        val mediaSource = currentSource.audioUrl?.let { audioUrl ->
            val audioSource = ProgressiveMediaSource.Factory(factory)
                .createMediaSource(MediaItem.fromUri(audioUrl))
            MergingMediaSource(videoSource, audioSource)
        } ?: videoSource

        player.setMediaSource(mediaSource)
        player.prepare()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (source != null) {
                BiliPlayerView(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                )
            }
            if (loading) {
                CircularProgressIndicator()
            }
            if (errorText != null && !loading) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = {
                    player.pause()
                    onEnterFullscreen()
                },
                enabled = source != null,
            ) {
                Text("\u5168\u5c4f\u64ad\u653e")
            }
        }
    }
}
