package com.example.bilimini.ui.screen.dynamic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.DynamicItem
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.RemoteImage

object DynamicDetailHolder {
    var item: DynamicItem? = null
}

@Composable
fun DynamicDetailScreen(
    repository: BiliRepository,
    onBack: () -> Unit,
) {
    val holderItem = DynamicDetailHolder.item
    var enrichedItem by remember { mutableStateOf<DynamicItem?>(null) }

    LaunchedEffect(holderItem?.id) {
        val id = holderItem?.id ?: return@LaunchedEffect
        val enriched = repository.enrichDynamicDetail(id)
        if (enriched != null) {
            enrichedItem = enriched
        }
    }

    val item = enrichedItem ?: holderItem
    if (item == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("动态内容不可用", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onBack) {
            Text("返回")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(
                    imageUrl = item.avatarUrl,
                    contentDescription = item.author,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                )
                Column {
                    Text(
                        text = item.author,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.publishText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item.images.forEachIndexed { index, imageUrl ->
                RemoteImage(
                    imageUrl = imageUrl,
                    contentDescription = "图片 ${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            // Spacer at bottom
            Box(modifier = Modifier.size(16.dp))
        }
    }
}
