package com.example.bilimini.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = YoutubeRed,
    onPrimary = YoutubeWhite,
    primaryContainer = YoutubeChip,
    onPrimaryContainer = YoutubeBlack,
    secondary = YoutubeBlack,
    onSecondary = YoutubeWhite,
    secondaryContainer = YoutubeChip,
    onSecondaryContainer = YoutubeBlack,
    background = YoutubeWhite,
    onBackground = YoutubeBlack,
    surface = YoutubeSurface,
    onSurface = YoutubeBlack,
    surfaceVariant = YoutubeSurfaceVariant,
    onSurfaceVariant = YoutubeMuted,
    outline = YoutubeBorder,
    error = YoutubeRed,
)

private val DarkColors = darkColorScheme(
    primary = YoutubeRedDark,
    onPrimary = YoutubeWhite,
    primaryContainer = YoutubeChipDark,
    onPrimaryContainer = YoutubeWhite,
    secondary = YoutubeWhite,
    onSecondary = YoutubeBlack,
    secondaryContainer = YoutubeDarkSurfaceRaised,
    onSecondaryContainer = YoutubeWhite,
    background = YoutubeBlack,
    onBackground = YoutubeWhite,
    surface = YoutubeDarkSurface,
    onSurface = YoutubeWhite,
    surfaceVariant = YoutubeDarkSurfaceRaised,
    onSurfaceVariant = YoutubeMutedDark,
    outline = ColorTokens.DarkOutline,
    error = YoutubeRedDark,
)

@Composable
fun WiliWiliTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

private object ColorTokens {
    val DarkOutline = androidx.compose.ui.graphics.Color(0xFF3A3A3A)
}
