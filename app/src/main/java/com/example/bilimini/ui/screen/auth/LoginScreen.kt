package com.example.bilimini.ui.screen.auth

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bilimini.session.SessionManager

private const val LOGIN_URL = "https://passport.bilibili.com/login"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    var statusText by remember {
        mutableStateOf(
            "Complete Bilibili sign-in inside the embedded page. Prefer QR code or challenge-based sign-in instead of giving credentials to a third-party form."
        )
    }
    var alreadyHandled by remember { mutableStateOf(false) }

    fun tryCaptureCookies() {
        val cookieManager = CookieManager.getInstance()
        val cookies = listOfNotNull(
            cookieManager.getCookie("https://passport.bilibili.com"),
            cookieManager.getCookie("https://www.bilibili.com"),
            cookieManager.getCookie("https://m.bilibili.com"),
        )
        val merged = cookies.joinToString("; ")
        if (!alreadyHandled && merged.contains("SESSDATA=") && merged.contains("DedeUserID=")) {
            sessionManager.saveCookies(cookies)
            alreadyHandled = true
            statusText = "Sign-in state captured and stored securely."
            onLoggedIn()
        }
    }

    LaunchedEffect(Unit) {
        sessionManager.seedCookiesIntoWebView()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("Back")
        }
        Text(
            text = "Sign in",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Button(
            onClick = {
                sessionManager.clearSession()
                alreadyHandled = false
                statusText = "Saved sign-in state cleared. Please sign in again."
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text("Clear saved session")
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.loadsImagesAutomatically = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            tryCaptureCookies()
                        }
                    }
                    loadUrl(LOGIN_URL)
                }
            },
            update = {
                tryCaptureCookies()
            },
        )
    }
}
