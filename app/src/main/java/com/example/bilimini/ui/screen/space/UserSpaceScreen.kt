package com.example.bilimini.ui.screen.space

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.DynamicItem
import com.example.bilimini.data.model.UserProfile
import com.example.bilimini.data.model.VideoSummary
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.ui.components.DynamicCard
import com.example.bilimini.ui.components.PageBanner
import com.example.bilimini.ui.components.RemoteImage
import com.example.bilimini.ui.components.VideoCard
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun UserSpaceScreen(
    mid: Long,
    repository: BiliRepository,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenUserSpace: (Long) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var videos by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var dynamics by remember { mutableStateOf<List<DynamicItem>>(emptyList()) }
    var videoMessage by remember { mutableStateOf<String?>(null) }
    var dynamicMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mid) {
        loading = true
        coroutineScope {
            val profileJob = async { repository.fetchUserProfile(mid) }
            val videosJob = async { repository.fetchUserVideos(mid) }
            val dynamicsJob = async { repository.fetchUserDynamicItems(mid) }

            profile = profileJob.await()
            videos = videosJob.await()
            dynamics = dynamicsJob.await()
        }
        videoMessage = if (videos.isEmpty()) "暂时没有读到这个用户的视频内容。" else null
        dynamicMessage = if (dynamics.isEmpty()) "暂时没有读到这个用户的动态，可能需要更完整的登录态。" else null
        loading = false
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageBanner(
                title = profile?.name ?: "用户主页",
                trailing = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        }
        item {
            UserHeader(profile = profile, fallbackMid = mid)
        }
        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("视频") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("动态") },
                )
            }
        }
        if (selectedTab == 0) {
            if (videoMessage != null) {
                item {
                    Text(
                        text = videoMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(videos, key = { it.bvid }) { video ->
                VideoCard(
                    video = video,
                    onClick = { onOpenVideo(video.bvid) },
                )
            }
        } else {
            if (dynamicMessage != null) {
                item {
                    Text(
                        text = dynamicMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(dynamics, key = { it.id }) { item ->
                DynamicCard(
                    item = item,
                    onClick = (item.bvid ?: item.origin?.bvid)?.let { bvid ->
                        { onOpenVideo(bvid) }
                    },
                    onAvatarClick = item.authorMid.takeIf { it > 0L }?.let { authorMid ->
                        { onOpenUserSpace(authorMid) }
                    },
                )
            }
        }
    }
}

@Composable
private fun UserHeader(
    profile: UserProfile?,
    fallbackMid: Long,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(
                imageUrl = profile?.avatarUrl.orEmpty(),
                contentDescription = profile?.name ?: "用户头像",
                modifier = Modifier
                    .size(82.dp)
                    .clip(CircleShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = profile?.name ?: "Bilibili 用户",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "UID ${profile?.mid ?: fallbackMid}  •  Lv.${profile?.level ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile?.sign?.takeIf { it.isNotBlank() }?.let { sign ->
                    Text(
                        text = sign,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
