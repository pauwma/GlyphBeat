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
import com.pauwma.glyphbeat.ui.ThemeSelectionScreen
import com.pauwma.glyphbeat.ui.SettingsScreen

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
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "themes",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("themes") {
                            ThemeSelectionScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                        composable("settings") {
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

fun isMediaControlServiceWorking(context: android.content.Context): Boolean {
    if (!isNotificationAccessGranted(context)) return false
    
    return try {
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

fun openNotificationAccessSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    context.startActivity(intent)
}