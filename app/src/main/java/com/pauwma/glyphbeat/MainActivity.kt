package com.pauwma.glyphbeat

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        setContent {
            NothingAndroidSDKDemoTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "themes",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("permissions") {
                            PermissionScreen(
                                onNavigateToThemes = {
                                    navController.navigate("themes")
                                }
                            )
                        }
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
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    onNavigateToThemes: () -> Unit = {}
) {
    val context = LocalContext.current
    var notificationAccessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Glyph Dial Demo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "This app needs permissions to control media playback and display animations on the Glyph Matrix.",
            style = MaterialTheme.typography.bodyMedium
        )

        // Notification Access Permission Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notification Access",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Required to detect and control music playback from apps like Spotify, YouTube Music, etc.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = if (notificationAccessGranted) "✅ Granted" else "❌ Not Granted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notificationAccessGranted)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )

                if (!notificationAccessGranted) {
                    Button(
                        onClick = {
                            openNotificationAccessSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Notification Access")
                    }
                }
            }
        }

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How to Use",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "1. Grant the notification access permission above\n" +
                            "2. Go to Glyph Interface in Settings\n" +
                            "3. Activate the Vinyl Player toy\n" +
                            "4. Play music and watch the Glyph Matrix animate!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Themes button
        Button(
            onClick = onNavigateToThemes,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Animation Themes")
        }

        // Refresh button
        Button(
            onClick = {
                notificationAccessGranted = isNotificationAccessGranted(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Permission Status")
        }
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

@Preview(showBackground = true)
@Composable
fun PermissionScreenPreview() {
    NothingAndroidSDKDemoTheme {
        PermissionScreen()
    }
}