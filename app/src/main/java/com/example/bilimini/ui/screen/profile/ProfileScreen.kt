package com.example.bilimini.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.bilimini.data.model.UserProfile
import com.example.bilimini.data.repository.BiliRepository
import com.example.bilimini.session.SessionManager
import com.example.bilimini.ui.components.PageBanner
import com.example.bilimini.ui.components.RemoteImage

@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    repository: BiliRepository,
    onLoginClick: () -> Unit,
) {
    val sessionState by sessionManager.sessionState.collectAsState()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(sessionState.isLoggedIn) {
        if (sessionState.isLoggedIn) {
            loading = true
            sessionManager.seedCookiesIntoWebView()
            profile = repository.fetchCurrentUser()
            loading = false
        } else {
            profile = null
        }
    }

    when {
        !sessionState.isLoggedIn -> LoggedOutPane(onLoginClick = onLoginClick)
        loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        else -> LoggedInPane(
            profile = profile,
            onLogout = { sessionManager.clearSession() },
        )
    }
}

@Composable
private fun LoggedOutPane(
    onLoginClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageBanner(
            title = "\u6211\u7684",
            showWordmark = true,
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "\u5f53\u524d\u8fd8\u6ca1\u6709\u767b\u5f55",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "\u4f7f\u7528\u7ad9\u5185\u7f51\u9875\u767b\u5f55\uff0c\u4fdd\u6301\u57fa\u7840\u529f\u80fd\u4e0d\u53d8\u3002",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onLoginClick) {
                    Text("\u53bb\u767b\u5f55")
                }
            }
        }
    }
}

@Composable
private fun LoggedInPane(
    profile: UserProfile?,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageBanner(
            title = "\u6211\u7684",
            showWordmark = true,
        )
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (profile == null) {
                    Text(
                        text = "\u5df2\u8bc6\u522b\u5230\u767b\u5f55\u6001\uff0c\u4f46\u6682\u65f6\u6ca1\u6709\u8bfb\u5230\u8d44\u6599\u3002",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    RemoteImage(
                        imageUrl = profile.avatarUrl,
                        contentDescription = profile.name,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "UID ${profile.mid}  \u2022  Lv.${profile.level ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    profile.sign?.takeIf { it.isNotBlank() }?.let { sign ->
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "\u786c\u5e01\u4f59\u989d\uff1a${profile.coins ?: "--"}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(onClick = onLogout) {
                    Text("\u9000\u51fa\u767b\u5f55")
                }
            }
        }
    }
}
