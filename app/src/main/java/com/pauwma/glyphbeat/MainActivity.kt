package com.pauwma.glyphbeat

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pauwma.glyphbeat.theme.NothingAndroidSDKDemoTheme
import com.pauwma.glyphbeat.ui.screens.ThemeSelectionScreen
import com.pauwma.glyphbeat.ui.screens.SettingsScreen
import com.pauwma.glyphbeat.ui.navigation.GlyphBeatBottomNavigation
import com.pauwma.glyphbeat.ui.navigation.MediaPlayerScreen
import com.pauwma.glyphbeat.ui.navigation.TrackControlScreen
import com.pauwma.glyphbeat.ui.navigation.SettingsNavigationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check and request notification access permission
        if (!isNotificationAccessGranted(this)) {
            requestNotificationAccess()
        }
        
        setContent {
            NothingAndroidSDKDemoTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        GlyphBeatBottomNavigation(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "media_player",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Media Player Tab
                        composable("media_player") {
                            MediaPlayerScreen(
                                onNavigateToSettings = {
                                    navController.navigate("media_player_settings")
                                }
                            )
                        }
                        
                        // Track Control Tab
                        composable("track_control") {
                            TrackControlScreen()
                        }
                        
                        // Settings Tab
                        composable("settings") {
                            SettingsNavigationScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Media player settings screen (accessed from within media player)
                        composable("media_player_settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                    }
                }
            }
        }
    }
    
    private fun requestNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
}

fun isNotificationAccessGranted(context: android.content.Context): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return enabledListeners.contains(context.packageName)
}

suspend fun isMediaControlServiceWorking(context: android.content.Context): Boolean {
    if (!isNotificationAccessGranted(context)) return false
    
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val mediaHelper = com.pauwma.glyphbeat.sound.MediaControlHelper(context)
            // Try to get active sessions - this will fail if service isn't properly connected
            val controller = mediaHelper.getActiveMediaController()
            // If we don't get a SecurityException, the service is working
            true
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            // Service might be connected but no active sessions - still working
            true
        }
    }
}

fun openNotificationAccessSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    context.startActivity(intent)
}