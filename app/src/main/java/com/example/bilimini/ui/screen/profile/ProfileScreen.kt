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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Me",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "You are not signed in. The MVP uses an embedded web login so the app does not need to collect your password directly.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onLoginClick) {
                Text("Sign in")
            }
        }
    }
}

@Composable
private fun LoggedInPane(
    profile: UserProfile?,
    onLogout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Me",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (profile == null) {
                Text(
                    text = "You appear signed in, but the profile call returned no data. Cookies may be expired, or the response shape may have changed.",
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
                    text = "UID ${profile.mid} | Lv.${profile.level ?: "--"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile.sign?.takeIf { it.isNotBlank() }?.let { sign ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = sign,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = "Coins ${profile.coins ?: "--"}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Button(onClick = onLogout) {
                Text("Sign out")
            }
        }
    }
}
