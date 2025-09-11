package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pauwma.glyphbeat.theme.NothingRed
import com.pauwma.glyphbeat.theme.NothingBlack
import com.pauwma.glyphbeat.theme.NothingWhite

/**
 * Bottom navigation item configuration.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Available bottom navigation items.
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = "media_player",
        title = "Media Player",
        selectedIcon = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow
    ),
    BottomNavItem(
        route = "track_control",
        title = "Track Control",
        selectedIcon = Icons.Filled.FastForward,
        unselectedIcon = Icons.Outlined.FastForward
    ),
    BottomNavItem(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

/**
 * Bottom navigation bar component.
 */
@Composable
fun GlyphBeatBott2omNavigation(
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = NothingBlack,
        contentColor = NothingWhite,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == item.route) {
                            item.selectedIcon
                        } else {
                            item.unselectedIcon
                        },
                        contentDescription = item.title,
                        tint = if (currentRoute == item.route) {
                            NothingRed
                        } else {
                            NothingWhite
                        }
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        color = if (currentRoute == item.route) {
                            NothingRed
                        } else {
                            NothingWhite
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NothingRed,
                    selectedTextColor = NothingRed,
                    unselectedIconColor = NothingWhite,
                    unselectedTextColor = NothingWhite,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}