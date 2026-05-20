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
import com.example.bilimini.ui.components.PageBanner

private const val LOGIN_URL = "https://passport.bilibili.com/login"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
) {
    var statusText by remember {
        mutableStateOf("\u8bf7\u5728\u5185\u7f6e\u9875\u9762\u5b8c\u6210 B \u7ad9\u767b\u5f55\uff0c\u5efa\u8bae\u4f18\u5148\u4f7f\u7528\u626b\u7801\u6216\u9a8c\u8bc1\u7801\u6d41\u7a0b\u3002")
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
            statusText = "\u5df2\u6355\u83b7\u767b\u5f55\u72b6\u6001\uff0c\u5e76\u5b89\u5168\u4fdd\u5b58\u5230\u672c\u5730\u3002"
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
            Text("\u8fd4\u56de")
        }
        PageBanner(
            title = "\u8d26\u53f7\u767b\u5f55",
            subtitle = statusText,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Button(
            onClick = {
                sessionManager.clearSession()
                alreadyHandled = false
                statusText = "\u5df2\u6e05\u9664\u672c\u5730\u767b\u5f55\u72b6\u6001\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\u3002"
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text("\u6e05\u9664\u672c\u5730\u767b\u5f55\u72b6\u6001")
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
