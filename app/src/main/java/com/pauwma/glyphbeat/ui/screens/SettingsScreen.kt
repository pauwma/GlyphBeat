package com.pauwma.glyphbeat.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Bento
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material3.*
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.pauwma.glyphbeat.services.shake.ShakeDetector
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.isNotificationAccessGranted
import com.pauwma.glyphbeat.isMediaControlServiceWorking
import com.pauwma.glyphbeat.openNotificationAccessSettings

@Composable
private fun TestResultCard(
    isLoading: Boolean,
    trackInfo: com.pauwma.glyphbeat.sound.MediaControlHelper.TrackInfo?,
    error: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable { onDismiss() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            trackInfo != null -> {
                // Media found - show beautiful track info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art or music icon - increased size for better quality
                    Card(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        if (trackInfo.albumArt != null) {
                            Image(
                                bitmap = trackInfo.albumArt.asImageBitmap(),
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Track info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Title
                        Text(
                            text = trackInfo.title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // Artist
                        Text(
                            text = trackInfo.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // App name with icon and brand colors
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val brandColor = getAppBrandColor(trackInfo.appName) 
                                ?: MaterialTheme.colorScheme.primary
                            val appIcon = getAppIcon(context, trackInfo.appName)
                            
                            // Show app icon if available, otherwise music icon
                            if (appIcon != null) {
                                Image(
                                    bitmap = appIcon.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = brandColor
                                )
                            }
                            
                            Text(
                                text = getAppNameFromPackage(context, trackInfo.appName),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = brandColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            error != null -> {
                // Error or no media state
                val isNoActiveMedia = error.contains("No active")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNoActiveMedia) 
                                Icons.Default.MusicOff 
                            else 
                                Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isNoActiveMedia)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    // Message
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isNoActiveMedia) 
                                "No Music Playing" 
                            else 
                                "Test Failed",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isNoActiveMedia)
                                "Start playing music to test the connection"
                            else
                                error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to get brand color for popular music apps
private fun getAppBrandColor(packageName: String): Color? {
    return when {
        packageName.contains("spotify", ignoreCase = true) -> Color(0xFF1DB954) // Spotify Green
        packageName.contains("youtube", ignoreCase = true) -> Color(0xFFFF0000) // YouTube Red
        packageName.contains("apple", ignoreCase = true) -> Color(0xFFFA243C) // Apple Music Red
        packageName.contains("amazon", ignoreCase = true) -> Color(0xFF00A8E1) // Amazon Music Blue
        packageName.contains("soundcloud", ignoreCase = true) -> Color(0xFFFF5500) // SoundCloud Orange
        packageName.contains("tidal", ignoreCase = true) -> Color(0xFF000000) // Tidal Black
        packageName.contains("deezer", ignoreCase = true) -> Color(0xFFFF6D3A) // Deezer Orange
        packageName.contains("pandora", ignoreCase = true) -> Color(0xFF224099) // Pandora Blue
        else -> null
    }
}

// Helper function to get app icon
private fun getAppIcon(context: android.content.Context, packageName: String): android.graphics.Bitmap? {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        val drawable = packageManager.getApplicationIcon(appInfo)
        drawable.toBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
    } catch (e: Exception) {
        null
    }
}

// Helper function to get readable app name from package name
private fun getAppNameFromPackage(context: android.content.Context, packageName: String): String {
    return try {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        // Fallback to beautifying the package name
        when {
            packageName.contains("spotify", ignoreCase = true) -> "Spotify"
            packageName.contains("youtube", ignoreCase = true) -> "YouTube Music"
            packageName.contains("amazon", ignoreCase = true) -> "Amazon Music"
            packageName.contains("apple", ignoreCase = true) -> "Apple Music"
            packageName.contains("tidal", ignoreCase = true) -> "Tidal"
            packageName.contains("deezer", ignoreCase = true) -> "Deezer"
            packageName.contains("soundcloud", ignoreCase = true) -> "SoundCloud"
            packageName.contains("pandora", ignoreCase = true) -> "Pandora"
            else -> packageName.substringAfterLast('.').capitalize()
        }
    }
}

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
    var testTrackInfo by remember { mutableStateOf<com.pauwma.glyphbeat.sound.MediaControlHelper.TrackInfo?>(null) }
    var showTestResult by remember { mutableStateOf(false) }
    var isTestLoading by remember { mutableStateOf(false) }
    var testError by remember { mutableStateOf<String?>(null) }
    
    // Shake settings state - initialize with defaults, load actual values asynchronously
    var shakeEnabled by remember { mutableStateOf(false) }
    var shakeSensitivity by remember { mutableStateOf(ShakeDetector.SENSITIVITY_MEDIUM) }
    var shakeSkipWhenPaused by remember { mutableStateOf(false) }
    var shakeSkipWhenUnlocked by remember { mutableStateOf(false) }
    var hapticFeedbackWhenShaked by remember { mutableStateOf(true) }
    var skipDelay by remember { mutableStateOf(3500L) }
    // Use rememberSaveable to persist collapse state during navigation, but defaults to false on app restart
    var shakeControlsExpanded by rememberSaveable { mutableStateOf(false) }
    
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
            shakeSkipWhenUnlocked = prefs.getBoolean("shake_skip_when_unlocked", false)
            hapticFeedbackWhenShaked = prefs.getBoolean("haptic_feedback_when_shaked", true)
            skipDelay = prefs.getLong("shake_skip_delay", 3500L)
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
    
    // Clear test result after 5 seconds
    LaunchedEffect(showTestResult) {
        if (showTestResult) {
            delay(5000L)
            showTestResult = false
            testTrackInfo = null
            testError = null
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
                modifier = Modifier.padding(16.dp)
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
                    text = "Required to detect and control music playback from apps like Spotify, YouTube Music, etc",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )


                if (notificationAccessGranted && !mediaServiceWorking) {
                    Text(
                        text = "The notification listener service may need to be restarted. Try toggling the permission off and on in settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Button row for permission actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            openNotificationAccessSettings(context)
                        },
                        modifier = Modifier.weight(1f),
                        colors = if (mediaServiceWorking) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
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
                                    isTestLoading = true
                                    showTestResult = true
                                    testTrackInfo = null
                                    testError = null
                                    
                                    withContext(Dispatchers.IO) {
                                        val mediaHelper = com.pauwma.glyphbeat.sound.MediaControlHelper(context)
                                        val controller = mediaHelper.getActiveMediaController()
                                        val trackInfo = mediaHelper.getTrackInfoForUI()

                                        isTestLoading = false
                                        
                                        if (controller != null) {
                                            testTrackInfo = trackInfo
                                            if (trackInfo == null) {
                                                testError = "Active session found but no track info available"
                                            }
                                        } else {
                                            testError = "No active music sessions found"
                                        }

                                        // Also refresh the service status when testing
                                        notificationAccessGranted = isNotificationAccessGranted(context)
                                        mediaServiceWorking = isMediaControlServiceWorking(context)
                                    }
                                } catch (e: Exception) {
                                    isTestLoading = false
                                    testError = "Error: ${e.message}"
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

                // Show test results card with animation  
                AnimatedVisibility(
                    visible = showTestResult,
                    enter = expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ) + fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(300)
                    ),
                    exit = shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(200)
                    ) + fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(200)
                    )
                ) {
                    TestResultCard(
                        isLoading = isTestLoading,
                        trackInfo = testTrackInfo,
                        error = testError,
                        onDismiss = {
                            showTestResult = false
                            testTrackInfo = null
                            testError = null
                        }
                    )
                }
            }
        }


        // Glyph Interface Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Glyph Interface Settings",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = customFont
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Access Glyph Toys configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Glyph Toys button
                Button(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                component = android.content.ComponentName(
                                    "com.nothing.thirdparty",
                                    "com.nothing.thirdparty.matrix.toys.preview.ToysPreviewActivity"
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                            Log.d("SettingsScreen", "Opened Glyph Toys preview")
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to open Glyph Toys: ${e.message}")
                            try {
                                // Fallback to general settings
                                val fallbackIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                Log.e("SettingsScreen", "Failed to open settings: ${e2.message}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pix,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Glyph Toys")
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
                // Clickable header with expand/collapse icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { shakeControlsExpanded = !shakeControlsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                            text = "Skip to the next track with a shake gesture",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Animated expand/collapse icon
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (shakeControlsExpanded) 180f else 0f,
                        label = "expandIcon"
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (shakeControlsExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Enable/Disable switch (always visible)
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
                            // Auto-expand when enabling, auto-collapse when disabling
                            if (enabled && !shakeControlsExpanded) {
                                shakeControlsExpanded = true
                            } else if (!enabled && shakeControlsExpanded) {
                                shakeControlsExpanded = false
                            }
                        }
                    )
                }

                // Animated visibility for detailed settings
                AnimatedVisibility(
                    visible = shakeControlsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Divider between header and settings
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Sensitivity slider
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
                                        text = "Skip when unlocked",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Allow shake to skip when phone is unlocked",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                    )
                                }

                                Switch(
                                    checked = shakeSkipWhenUnlocked,
                                    onCheckedChange = { enabled ->
                                        shakeSkipWhenUnlocked = enabled
                                        prefs.edit().putBoolean("shake_skip_when_unlocked", enabled).apply()
                                        Log.d("SettingsScreen", "Skip when unlocked: $enabled")
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
                    } // End of AnimatedVisibility Column
                } // End of AnimatedVisibility
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
                modifier = Modifier.padding(16.dp)
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
                    text = "Found a bug or need help? Contact me for support",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
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

        // Support/Donation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Support GlyphBeat",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = customFont
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "GlyphBeat is a passion project, completely ad-free. " +
                           "If you're enjoying the app and would like to fuel future updates and new features, " +
                           "consider buying me a coffee! Every contribution, no matter how small, makes a real " +
                           "difference and keeps me motivated. Thank you for being awesome! 🎵",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Main donation button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.buymeacoffee.com/pauwma")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buy me a coffee")
                }

                // Alternative donation options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://paypal.me/pauwma")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PayPal", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://github.com/sponsors/pauwma")
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("GitHub", style = MaterialTheme.typography.bodySmall)
                    }
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
                modifier = Modifier.padding(16.dp)
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Button(
                    onClick = {
                        // Reset tutorial and launch it
                        com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences.resetTutorial(context)
                        val intent = Intent(context, com.pauwma.glyphbeat.tutorial.TutorialActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
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
                modifier = Modifier.padding(16.dp)
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                ClickableText(
                    modifier = Modifier.padding(top = 8.dp),
                    text = buildAnnotatedString {
                        append("Version: 1.1.0 - ")
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
                            append("Version: 1.1.0 - ")
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

