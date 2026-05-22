package com.example.bilimini.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
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
import com.example.bilimini.ui.screen.feed.FeedScreenState
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
fun WiliWiliRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val bottomItems = listOf(
        BottomNavItem(
            route = AppDestination.Feed.route,
            label = "首页",
            icon = { Icon(Icons.Rounded.Home, contentDescription = "首页") },
        ),
        BottomNavItem(
            route = AppDestination.Dynamic.route,
            label = "动态",
            icon = { Icon(Icons.Rounded.DynamicFeed, contentDescription = "动态") },
        ),
        BottomNavItem(
            route = AppDestination.Search.route,
            label = "搜索",
            icon = { Icon(Icons.Rounded.Search, contentDescription = "搜索") },
        ),
        BottomNavItem(
            route = AppDestination.Profile.route,
            label = "我的",
            icon = { Icon(Icons.Rounded.AccountCircle, contentDescription = "我的") },
        ),
    )
    val showBottomBar = currentDestination?.route in bottomItems.map { it.route }
    val feedScreenState = remember { FeedScreenState() }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Feed.route,
            ) {
                composable(AppDestination.Feed.route) {
                    FeedScreen(
                        repository = container.repository,
                        state = feedScreenState,
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
                    enterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
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
                    enterTransition = {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300),
                        ) + fadeOut(animationSpec = tween(300))
                    },
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
}
