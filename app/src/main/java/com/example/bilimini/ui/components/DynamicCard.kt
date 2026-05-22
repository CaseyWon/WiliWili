package com.example.bilimini.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.DynamicItem

@Composable
fun DynamicCard(
    item: DynamicItem,
    onClick: (() -> Unit)?,
    onAvatarClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteImage(
                    imageUrl = item.avatarUrl,
                    contentDescription = item.author,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .let { base ->
                            if (onAvatarClick != null) base.clickable(onClick = onAvatarClick) else base
                        },
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = if (onAvatarClick != null) {
                        Modifier.clickable(onClick = onAvatarClick)
                    } else {
                        Modifier
                    },
                ) {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = item.publishText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item.title.isNotBlank() && item.title != item.text) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (item.coverUrl.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewingImageUrl = item.coverUrl },
                ) {
                    RemoteImage(
                        imageUrl = item.coverUrl,
                        contentDescription = item.title,
                    )
                }
            }

            item.origin?.let { origin ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "@${origin.author}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (origin.title.isNotBlank() && origin.title != origin.text) {
                            Text(
                                text = origin.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (origin.text.isNotBlank()) {
                            Text(
                                text = origin.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (origin.coverUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewingImageUrl = origin.coverUrl },
                            ) {
                                RemoteImage(
                                    imageUrl = origin.coverUrl,
                                    contentDescription = origin.title,
                                )
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                (item.bvid ?: item.origin?.bvid)?.takeIf { it.isNotBlank() }?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "\u53ef\u70b9\u5f00\u89c6\u9891",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                } ?: item.images.takeIf { it.isNotEmpty() }?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "\u67e5\u770b\u56fe\u6587\u8be6\u60c5",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
    }

    viewingImageUrl?.let { url ->
        val images = item.images.ifEmpty { listOf(url) }
        val page = images.indexOf(url).coerceAtLeast(0)
        ImageViewer(
            imageUrls = images,
            initialPage = page,
            onDismiss = { viewingImageUrl = null },
        )
    }
}
