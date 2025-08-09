package com.pauwma.glyphbeat.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.pauwma.glyphbeat.services.shake.ShakeDetector
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.isNotificationAccessGranted
import com.pauwma.glyphbeat.isMediaControlServiceWorking
import com.pauwma.glyphbeat.openNotificationAccessSettings

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("glyph_settings", android.content.Context.MODE_PRIVATE) }
    var notificationAccessGranted by remember { mutableStateOf(false) }
    var mediaServiceWorking by remember { mutableStateOf(false) }
    var isLoadingPermissions by remember { mutableStateOf(true) }
    val customFont = FontFamily(Font(R.font.ntype82regular))
    val notoEmojiFont = FontFamily(Font(R.font.notoemoji))
    var testResult by remember { mutableStateOf("") }
    
    // Shake settings state - initialize with defaults, load actual values asynchronously
    var shakeEnabled by remember { mutableStateOf(false) }
    var shakeSensitivity by remember { mutableStateOf(ShakeDetector.SENSITIVITY_MEDIUM) }
    var shakeSkipWhenPaused by remember { mutableStateOf(false) }
    var hapticFeedbackWhenShaked by remember { mutableStateOf(false) }
    var skipDelay by remember { mutableStateOf(2000L) }
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Load initial values asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Load permissions status
            notificationAccessGranted = isNotificationAccessGranted(context)
            mediaServiceWorking = isMediaControlServiceWorking(context)
            isLoadingPermissions = false
            
            // Load preferences
            shakeEnabled = prefs.getBoolean("shake_to_skip_enabled", false)
            shakeSensitivity = prefs.getFloat("shake_sensitivity", ShakeDetector.SENSITIVITY_MEDIUM)
            shakeSkipWhenPaused = prefs.getBoolean("shake_skip_when_paused", false)
            skipDelay = prefs.getLong("shake_skip_delay", 2000L)
        }
    }
    
    // Automatically refresh permission and service status when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        notificationAccessGranted = isNotificationAccessGranted(context)
                        mediaServiceWorking = isMediaControlServiceWorking(context)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Clear test result after 3 seconds
    LaunchedEffect(testResult) {
        if (testResult.isNotEmpty()) {
            delay(3000L)
            testResult = ""
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header to match other tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = customFont
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Scrollable content with scrollbar
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

        // Permission Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Notification Access",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Required to detect and control music playback from apps like Spotify, YouTube Music, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                // Permission status
                if (isLoadingPermissions) {
                    Text(
                        text = "⏳ Checking permissions...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = when {
                            !notificationAccessGranted -> "❌ Permission Not Granted"
                            mediaServiceWorking -> "✅ Permission Granted & Service Active"
                            else -> "⚠️ Permission Granted but Service Not Connected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            !notificationAccessGranted -> MaterialTheme.colorScheme.error
                            mediaServiceWorking -> Color(0xFF00C853)
                            else -> Color(0xFFFF9800) // Orange for warning
                        }
                    )
                }

                if (notificationAccessGranted && !mediaServiceWorking) {
                    Text(
                        text = "The notification listener service may need to be restarted. Try toggling the permission off and on in settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Button row for permission actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            openNotificationAccessSettings(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !mediaServiceWorking
                    ) {
                        Text(
                            when {
                                !notificationAccessGranted -> "Grant Notification Access"
                                !mediaServiceWorking -> "Fix Service Connection"
                                else -> "Service Working"
                            }
                        )
                    }
                    
                    FilledIconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    testResult = "⏳ Testing..."
                                    
                                    withContext(Dispatchers.IO) {
                                        val mediaHelper = com.pauwma.glyphbeat.sound.MediaControlHelper(context)
                                        val controller = mediaHelper.getActiveMediaController()
                                        val trackInfo = mediaHelper.getTrackInfo()

                                        testResult = if (controller != null) {
                                            if (trackInfo != null) {
                                                "✅ Active session found:\n${trackInfo.title} by ${trackInfo.artist}\nApp: ${trackInfo.appName}"
                                            } else {
                                                "✅ Active session found but no track info available"
                                            }
                                        } else {
                                            "ℹ️ No active music sessions found\n(Start playing music to test)"
                                        }

                                        // Also refresh the service status when testing
                                        notificationAccessGranted = isNotificationAccessGranted(context)
                                        mediaServiceWorking = isMediaControlServiceWorking(context)
                                    }
                                } catch (e: Exception) {
                                    testResult = "❌ Error: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.height(40.dp).width(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Show test results if available
                if (testResult.isNotEmpty()) {
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }


        // Shake Control Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Shake Controls",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Skip to the next track with a shake gesture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                // Enable/Disable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shake to Skip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Switch(
                        checked = shakeEnabled,
                        onCheckedChange = { enabled ->
                            shakeEnabled = enabled
                            prefs.edit().putBoolean("shake_to_skip_enabled", enabled).apply()
                            Log.d("SettingsScreen", "Shake to skip: $enabled")
                        }
                    )
                }

                // Sensitivity slider
                if (shakeEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Shake Sensitivity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Low",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Medium",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "High",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Slider(
                            value = when (shakeSensitivity) {
                                ShakeDetector.SENSITIVITY_HIGH -> 2f
                                ShakeDetector.SENSITIVITY_MEDIUM -> 1f
                                ShakeDetector.SENSITIVITY_LOW -> 0f
                                else -> 1f
                            },
                            onValueChange = { value ->
                                shakeSensitivity = when {
                                    value < 0.5f -> ShakeDetector.SENSITIVITY_LOW
                                    value < 1.5f -> ShakeDetector.SENSITIVITY_MEDIUM
                                    else -> ShakeDetector.SENSITIVITY_HIGH
                                }
                                prefs.edit().putFloat("shake_sensitivity", shakeSensitivity).apply()
                                Log.d("SettingsScreen", "Shake sensitivity: $shakeSensitivity")
                            },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        
                        Text(
                            text = when (shakeSensitivity) {
                                ShakeDetector.SENSITIVITY_HIGH -> "High sensitivity - gentle shake required"
                                ShakeDetector.SENSITIVITY_MEDIUM -> "Medium sensitivity - moderate shake required"
                                ShakeDetector.SENSITIVITY_LOW -> "Low sensitivity - strong shake required"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Skip Delay slider
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Skip Delay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "0.5s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "2s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "3.5s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "5s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Slider(
                            value = ((skipDelay - 500f) / 4500f).coerceIn(0f, 1f),
                            onValueChange = { value ->
                                skipDelay = (500 + (value * 4500)).toLong()
                                prefs.edit().putLong("shake_skip_delay", skipDelay).apply()
                                Log.d("SettingsScreen", "Skip delay: ${skipDelay}ms")
                            },
                            valueRange = 0f..1f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        
                        Text(
                            text = "Delay between skips: ${String.format("%.1f", skipDelay / 1000.0)} seconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Skip when paused toggle
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Skip when paused",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Allow shake to skip even when music is paused",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                            
                            Switch(
                                checked = shakeSkipWhenPaused,
                                onCheckedChange = { enabled ->
                                    shakeSkipWhenPaused = enabled
                                    prefs.edit().putBoolean("shake_skip_when_paused", enabled).apply()
                                    Log.d("SettingsScreen", "Skip when paused: $enabled")
                                }
                            )
                        }
                    }

                    // Skip when paused toggle
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Haptic feedback",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Sends a short vibration confirming the action",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }

                            Switch(
                                checked = hapticFeedbackWhenShaked,
                                onCheckedChange = { enabled ->
                                    hapticFeedbackWhenShaked = enabled
                                    prefs.edit().putBoolean("feedback_when_shaked", enabled).apply()
                                    Log.d("SettingsScreen", "Feedback on shaked: $enabled")
                                }
                            )
                        }
                    }
                }
            }
        }

        // Bug Report Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bug Report & Support",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Found a bug or need help? Send me a message and I'll get back to you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("contact+glyphbeat@pauwma.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "GlyphBeat Bug Report")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Hi,\n\nI found an issue with GlyphBeat:\n\n" +
                                "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                                "Android Version: ${android.os.Build.VERSION.RELEASE}\n" +
                                "App Version: 1.0.0\n\n" +
                                "Description of the issue:\n\n\n" +
                                "Steps to reproduce:\n1. \n2. \n3. \n\n" +
                                "Expected behavior:\n\n\n" +
                                "Actual behavior:\n\n"
                            )
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            // Fallback if no email app is available
                            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://github.com/pauwma/GlyphBeat/issues")
                            }
                            context.startActivity(fallbackIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact")
                }
            }
        }

        // Tutorial Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tutorial",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Text(
                    text = "View the app introduction and setup guide again",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                
                Button(
                    onClick = {
                        // Reset tutorial and launch it
                        com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences.resetTutorial(context)
                        val intent = Intent(context, com.pauwma.glyphbeat.tutorial.TutorialActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.pauwma.glyphbeat.theme.NothingRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Replay Tutorial")
                }
            }
        }

        // App Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "GlyphBeat - Animated Media Player Toy for Glyph Matrix",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                ClickableText(
                    text = buildAnnotatedString {
                        append("Version: 1.0.0 - ")
                        pushStringAnnotation(tag = "URL", annotation = "https://privacidad.me/@pauwma")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("pauwma")
                        }
                        pop()
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    onClick = { offset ->
                        val annotations = buildAnnotatedString {
                            append("Version: 1.0.0 - ")
                            pushStringAnnotation(tag = "URL", annotation = "https://privacidad.me/@pauwma")
                            append("pauwma")
                            pop()
                        }.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        annotations.firstOrNull()?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            context.startActivity(intent)
                        }
                    }
                )
            }
        }
            } // End scrollable Column
        } // End Box
    } // End main column
}

