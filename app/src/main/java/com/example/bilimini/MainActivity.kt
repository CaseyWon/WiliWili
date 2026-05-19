package com.example.bilimini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bilimini.ui.navigation.BiliMiniRoot
import com.example.bilimini.ui.theme.BiliMiniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as BiliMiniApplication).appContainer
        setContent {
            BiliMiniTheme {
                BiliMiniRoot(container = container)
            }
        }
    }
}
