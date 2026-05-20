package com.example.bilimini.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DynamicFeed
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.bilimini.AppContainer
import com.example.bilimini.ui.screen.auth.LoginScreen
import com.example.bilimini.ui.screen.detail.DetailScreen
import com.example.bilimini.ui.screen.dynamic.DynamicScreen
import com.example.bilimini.ui.screen.feed.FeedScreen
import com.example.bilimini.ui.screen.player.PlayerScreen
import com.example.bilimini.ui.screen.profile.ProfileScreen
import com.example.bilimini.ui.screen.search.SearchScreen
import com.example.bilimini.ui.screen.space.UserSpaceScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun BiliMiniRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val bottomItems = listOf(
        BottomNavItem(
            route = AppDestination.Feed.route,
            label = "\u9996\u9875",
            icon = { Icon(Icons.Rounded.Home, contentDescription = "\u9996\u9875") },
        ),
        BottomNavItem(
            route = AppDestination.Dynamic.route,
            label = "\u52a8\u6001",
            icon = { Icon(Icons.Rounded.DynamicFeed, contentDescription = "\u52a8\u6001") },
        ),
        BottomNavItem(
            route = AppDestination.Search.route,
            label = "\u641c\u7d22",
            icon = { Icon(Icons.Rounded.Search, contentDescription = "\u641c\u7d22") },
        ),
        BottomNavItem(
            route = AppDestination.Profile.route,
            label = "\u6211\u7684",
            icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = "\u6211\u7684") },
        ),
    )
    val showBottomBar = currentDestination?.route in bottomItems.map { it.route }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = item.icon,
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Feed.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Feed.route) {
                FeedScreen(
                    repository = container.repository,
                    onOpenVideo = { bvid ->
                        navController.navigate(AppDestination.Detail.createRoute(bvid))
                    },
                )
            }
            composable(AppDestination.Dynamic.route) {
                DynamicScreen(
                    repository = container.repository,
                    sessionManager = container.sessionManager,
                    onLoginClick = { navController.navigate(AppDestination.Login.route) },
                    onOpenVideo = { bvid ->
                        navController.navigate(AppDestination.Detail.createRoute(bvid))
                    },
                    onOpenUserSpace = { mid ->
                        navController.navigate(AppDestination.UserSpace.createRoute(mid))
                    },
                )
            }
            composable(AppDestination.Search.route) {
                SearchScreen(
                    repository = container.repository,
                    onOpenVideo = { bvid ->
                        navController.navigate(AppDestination.Detail.createRoute(bvid))
                    },
                )
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(
                    sessionManager = container.sessionManager,
                    repository = container.repository,
                    onLoginClick = { navController.navigate(AppDestination.Login.route) },
                )
            }
            composable(AppDestination.Login.route) {
                LoginScreen(
                    sessionManager = container.sessionManager,
                    onBack = { navController.popBackStack() },
                    onLoggedIn = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestination.UserSpace.route,
                arguments = listOf(navArgument("mid") { type = NavType.LongType }),
            ) { entry ->
                val mid = entry.arguments?.getLong("mid") ?: 0L
                UserSpaceScreen(
                    mid = mid,
                    repository = container.repository,
                    onBack = { navController.popBackStack() },
                    onOpenVideo = { bvid ->
                        navController.navigate(AppDestination.Detail.createRoute(bvid))
                    },
                    onOpenUserSpace = { targetMid ->
                        navController.navigate(AppDestination.UserSpace.createRoute(targetMid))
                    },
                )
            }
            composable(
                route = AppDestination.Detail.route,
                arguments = listOf(navArgument("bvid") { type = NavType.StringType }),
            ) { entry ->
                val bvid = entry.arguments?.getString("bvid").orEmpty()
                DetailScreen(
                    bvid = bvid,
                    repository = container.repository,
                    onBack = { navController.popBackStack() },
                    onPlayClick = {
                        navController.navigate(AppDestination.Player.createRoute(bvid))
                    },
                )
            }
            composable(
                route = AppDestination.Player.route,
                arguments = listOf(navArgument("bvid") { type = NavType.StringType }),
            ) { entry ->
                val bvid = entry.arguments?.getString("bvid").orEmpty()
                PlayerScreen(
                    bvid = bvid,
                    repository = container.repository,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
