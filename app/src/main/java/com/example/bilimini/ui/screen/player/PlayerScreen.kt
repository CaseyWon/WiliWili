package com.example.bilimini.ui.screen.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.BiliPlayerView

@Composable
fun PlayerScreen(
    bvid: String,
    repository: BiliRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val player = remember(bvid) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
    }
    var source by remember(bvid) { mutableStateOf<PlayableSource?>(null) }
    var loading by remember(bvid) { mutableStateOf(true) }
    var errorText by remember(bvid) { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    DisposableEffect(activity) {
        val currentActivity = activity
        val oldOrientation = currentActivity?.requestedOrientation
        val window = currentActivity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        currentActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            if (oldOrientation != null) {
                currentActivity.requestedOrientation = oldOrientation
            }
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
        }
    }

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
                errorText = "\u539f\u751f\u64ad\u653e\u5668\u52a0\u8f7d\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002"
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
    ) {
        if (source != null) {
            BiliPlayerView(
                player = player,
                modifier = Modifier.fillMaxSize(),
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onControllerVisibilityChanged = { visible ->
                    controlsVisible = visible
                },
            )
        }

        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (controlsVisible && source != null) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                Text("退出全屏")
            }
        }

        errorText?.let {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
