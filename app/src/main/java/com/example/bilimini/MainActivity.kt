package com.example.bilimini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bilimini.ui.navigation.WiliWiliRoot
import com.example.bilimini.ui.theme.WiliWiliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WiliWiliApplication).appContainer
        setContent {
            WiliWiliTheme {
                WiliWiliRoot(container = container)
            }
        }
    }
}
