package com.example.bilimini.ui.components

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerControlView
import androidx.media3.ui.PlayerView

@Composable
fun BiliPlayerView(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    useController: Boolean = true,
    controllerTimeoutMs: Int = 1800,
    onControllerVisibilityChanged: ((Boolean) -> Unit)? = null,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                this.useController = useController
                controllerAutoShow = useController
                controllerShowTimeoutMs = controllerTimeoutMs
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShutterBackgroundColor(Color.BLACK)
                setBackgroundColor(Color.BLACK)
                this.resizeMode = resizeMode
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                setControllerVisibilityListener(
                    PlayerControlView.VisibilityListener { visibility ->
                        onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                    },
                )
            }
        },
        update = { view ->
            view.player = player
            view.useController = useController
            view.controllerAutoShow = useController
            view.controllerShowTimeoutMs = controllerTimeoutMs
            view.resizeMode = resizeMode
            view.setControllerVisibilityListener(
                PlayerControlView.VisibilityListener { visibility ->
                    onControllerVisibilityChanged?.invoke(visibility == View.VISIBLE)
                },
            )
        },
    )
}
