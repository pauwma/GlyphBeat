package com.pauwma.glyphbeat.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pauwma.glyphbeat.theme.*

data class PillNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
    val contentDescription: String
)

val pillNavItems = listOf(
    PillNavItem(
        route = "media_player",
        icon = Icons.Filled.MusicNote,
        label = "Player",
        contentDescription = "Media Player"
    ),
    PillNavItem(
        route = "track_control",
        icon = Icons.Filled.SkipNext,
        label = "Controls",
        contentDescription = "Track Control"
    ),
    PillNavItem(
        route = "settings",
        icon = Icons.Filled.Settings,
        label = "Settings",
        contentDescription = "Settings"
    )
)

@Composable
fun PillNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // Blur gradient overlay behind the navigation bar - full width
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.BottomCenter)
                .blur(radius = 30.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars) // Proper system navigation bar padding
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.75f) // Reduced width to minimize corner spacing
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pillNavItems.forEachIndexed { index, item ->
                    PillNavigationItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                    if (index < pillNavItems.size - 1) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PillNavigationItem(
    item: PillNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) NothingRed else Color.Transparent,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) NothingWhite else Color(0xFF808080),
        animationSpec = tween(300),
        label = "iconColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(
                horizontal = if (isSelected) 20.dp else 16.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.contentDescription,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            if (isSelected) {
                Text(
                    text = item.label,
                    color = iconColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PillNavigationBarAlternative(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Alternative design with individual pills
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent) // Transparent background
            .windowInsetsPadding(WindowInsets.navigationBars) // Proper system navigation bar padding
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pillNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) NothingRed else Color(0xFF2A2A2A),
                    animationSpec = tween(300),
                    label = "backgroundColor"
                )
                
                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) NothingWhite else NothingWhite.copy(alpha = 0.7f),
                    animationSpec = tween(300),
                    label = "iconColor"
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = if (isSelected) 16.dp else 8.dp,
                                shape = RoundedCornerShape(28.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .background(backgroundColor)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        color = if (isSelected) NothingRed else NothingWhite.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}