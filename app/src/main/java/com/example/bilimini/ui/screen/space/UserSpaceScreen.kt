package com.example.bilimini.ui.screen.space

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.bilimini.ui.screen.dynamic.DynamicDetailHolder
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
fun UserSpaceScreen(
    mid: Long,
    repository: BiliRepository,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenUserSpace: (Long) -> Unit,
    onOpenDetail: (DynamicItem) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    // Videos state
    var videos by remember { mutableStateOf<List<VideoSummary>>(emptyList()) }
    var videoPage by remember { mutableIntStateOf(1) }
    var loadingMoreVideos by remember { mutableStateOf(false) }
    var hasMoreVideos by remember { mutableStateOf(true) }
    var videoMessage by remember { mutableStateOf<String?>(null) }

    // Dynamics state
    var dynamics by remember { mutableStateOf<List<DynamicItem>>(emptyList()) }
    var dynamicsOffset by remember { mutableStateOf<String?>(null) }
    var loadingMoreDynamics by remember { mutableStateOf(false) }
    var hasMoreDynamics by remember { mutableStateOf(true) }
    var dynamicMessage by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(mid) {
        loading = true
        coroutineScope {
            val profileJob = async { repository.fetchUserProfile(mid) }
            val videosJob = async { repository.fetchUserVideos(mid) }
            val dynamicsJob = async { repository.fetchUserDynamicItems(mid) }

            profile = profileJob.await()

            val fetchedVideos = videosJob.await()
            videos = fetchedVideos
            videoMessage = if (fetchedVideos.isEmpty()) "暂时没有读到这个用户的视频内容。" else null

            val (fetchedDynamics, nextOff) = dynamicsJob.await()
            dynamics = fetchedDynamics
            dynamicsOffset = nextOff
            dynamicMessage = if (fetchedDynamics.isEmpty()) "暂时没有读到这个用户的动态，可能需要更完整的登录态。" else null
        }
        loading = false
    }

    // Infinite scroll: videos
    LaunchedEffect(listState, selectedTab) {
        if (selectedTab != 0) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (!loadingMoreVideos && hasMoreVideos && lastVisible >= totalItems - 3 && totalItems > 0) {
                loadingMoreVideos = true
                val nextPage = videoPage + 1
                val result = repository.fetchUserVideos(mid, nextPage)
                if (result.isNotEmpty()) {
                    videoPage = nextPage
                    videos = videos + result
                } else {
                    hasMoreVideos = false
                }
                loadingMoreVideos = false
            }
        }
    }

    // Infinite scroll: dynamics
    LaunchedEffect(listState, selectedTab) {
        if (selectedTab != 1) return@LaunchedEffect
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (!loadingMoreDynamics && hasMoreDynamics && lastVisible >= totalItems - 3 && totalItems > 0) {
                loadingMoreDynamics = true
                val (result, nextOff) = repository.fetchUserDynamicItems(mid, dynamicsOffset)
                if (result.isNotEmpty()) {
                    dynamicsOffset = nextOff
                    dynamics = dynamics + result
                } else {
                    hasMoreDynamics = false
                }
                loadingMoreDynamics = false
            }
        }
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
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PageBanner(
                title = profile?.name ?: "用户主页",
                trailing = {
                    Button(onClick = onBack) {
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
            if (loadingMoreVideos) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
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
                    onClick = {
                        if (item.bvid != null || item.origin?.bvid != null) {
                            onOpenVideo(item.bvid ?: item.origin!!.bvid!!)
                        } else {
                            onOpenDetail(item)
                        }
                    },
                    onAvatarClick = item.authorMid.takeIf { it > 0L }?.let { authorMid ->
                        { onOpenUserSpace(authorMid) }
                    },
                )
            }
            if (loadingMoreDynamics) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
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
