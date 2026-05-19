package com.example.bilimini.ui.screen.player

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bilimini.session.SessionManager

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(
    pageUrl: String,
    sessionManager: SessionManager,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) {
        sessionManager.seedCookiesIntoWebView()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Text(
            text = "Player",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = "This build prioritizes playback reliability by using the embedded web player.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    webChromeClient = WebChromeClient()
                    loadUrl(pageUrl)
                }
            },
            update = { webView ->
                if (webView.url != pageUrl) {
                    webView.loadUrl(pageUrl)
                }
            },
        )
    }
}
