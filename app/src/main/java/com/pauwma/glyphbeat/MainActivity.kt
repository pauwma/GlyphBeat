package com.pauwma.glyphbeat

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.pauwma.glyphbeat.theme.NothingAndroidSDKDemoTheme
import com.pauwma.glyphbeat.ui.screens.ThemeSelectionScreen
import com.pauwma.glyphbeat.ui.screens.SettingsScreen
import com.pauwma.glyphbeat.ui.navigation.PillNavigationBar
import com.pauwma.glyphbeat.ui.navigation.MediaPlayerScreen
import com.pauwma.glyphbeat.ui.navigation.TrackControlScreen
import com.pauwma.glyphbeat.ui.navigation.SettingsNavigationScreen
import com.pauwma.glyphbeat.tutorial.TutorialActivity
import com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if tutorial should be shown
        if (!TutorialPreferences.isTutorialCompleted(this)) {
            // Launch tutorial activity
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        enableEdgeToEdge()
        
        // Check and request notification access permission if not in tutorial
        if (!isNotificationAccessGranted(this)) {
            // Only request if user didn't skip in tutorial
            if (!TutorialPreferences.hasSkippedPermissions(this)) {
                requestNotificationAccess()
            }
        }
        
        setContent {
            NothingAndroidSDKDemoTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Transparent,
                    bottomBar = {
                        PillNavigationBar(navController = navController)
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "media_player",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        enterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
                        exitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) },
                        popEnterTransition = { fadeIn(animationSpec = androidx.compose.animation.core.tween(150)) },
                        popExitTransition = { fadeOut(animationSpec = androidx.compose.animation.core.tween(150)) }
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