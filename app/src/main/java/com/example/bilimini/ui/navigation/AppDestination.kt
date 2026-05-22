package com.example.bilimini.ui.navigation

sealed class AppDestination(val route: String) {
    data object Feed : AppDestination("feed")
    data object Dynamic : AppDestination("dynamic")
    data object Search : AppDestination("search")
    data object Profile : AppDestination("profile")
    data object Login : AppDestination("login")
    data object UserSpace : AppDestination("space/{mid}") {
        fun createRoute(mid: Long): String = "space/$mid"
    }
    data object Detail : AppDestination("detail/{bvid}") {
        fun createRoute(bvid: String): String = "detail/$bvid"
    }
    data object Player : AppDestination("player/{bvid}") {
        fun createRoute(bvid: String): String = "player/$bvid"
    }
    data object DynamicDetail : AppDestination("dynamic_detail")
}
