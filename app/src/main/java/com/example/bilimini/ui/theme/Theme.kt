package com.example.bilimini.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CoralRed,
    onPrimary = Mist,
    primaryContainer = ColorTokens.PrimaryContainer,
    onPrimaryContainer = DeepInk,
    secondary = TealAccent,
    onSecondary = Mist,
    background = Mist,
    onBackground = DeepInk,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = DeepInk,
    surfaceVariant = Sand,
    onSurfaceVariant = Slate,
)

private val DarkColors = darkColorScheme(
    primary = CoralRed,
    onPrimary = Mist,
    primaryContainer = ColorTokens.PrimaryContainerDark,
    onPrimaryContainer = Mist,
    secondary = TealAccent,
    onSecondary = DeepInk,
    background = DeepInk,
    onBackground = Mist,
    surface = androidx.compose.ui.graphics.Color(0xFF26212C),
    onSurface = Mist,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3A3340),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD7CFC7),
)

@Composable
fun BiliMiniTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}

private object ColorTokens {
    val PrimaryContainer = androidx.compose.ui.graphics.Color(0xFFF8CDC8)
    val PrimaryContainerDark = androidx.compose.ui.graphics.Color(0xFF6A2924)
}
