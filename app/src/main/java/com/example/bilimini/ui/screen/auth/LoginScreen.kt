package com.example.bilimini.ui.screen.auth

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
            onLoggedIn()
        }
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        sessionManager.seedCookiesIntoWebView()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.loadsImagesAutomatically = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
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

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp)
                .size(64.dp)
                .clickable(onClick = onBack),
        )
    }
}
