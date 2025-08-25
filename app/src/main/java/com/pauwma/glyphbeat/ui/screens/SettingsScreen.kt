package com.pauwma.glyphbeat.ui.screens

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Pix
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.pauwma.glyphbeat.services.shake.ShakeDetector
import com.pauwma.glyphbeat.services.autostart.MusicAppWhitelistManager
import com.pauwma.glyphbeat.services.autostart.MusicDetectionService
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pauwma.glyphbeat.theme.NothingRed
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.data.ShakeBehavior
import com.pauwma.glyphbeat.isNotificationAccessGranted
import com.pauwma.glyphbeat.isMediaControlServiceWorking
import com.pauwma.glyphbeat.openNotificationAccessSettings
import com.pauwma.glyphbeat.ui.settings.DropdownOption
import com.pauwma.glyphbeat.ui.settings.DropdownSetting
import com.pauwma.glyphbeat.ui.settings.SettingsDropdown
import com.pauwma.glyphbeat.ui.settings.ShakeControlsSection
import com.pauwma.glyphbeat.data.ShakeControlSettings
import com.pauwma.glyphbeat.data.ShakeControlSettingsManager

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
        packageName.contains("apple", ignoreCase = true) -> Color(0xFFFA2461) // Apple Music Red
        packageName.contains("amazon", ignoreCase = true) -> Color(0xFF00A8E1) // Amazon Music Blue
        packageName.contains("soundcloud", ignoreCase = true) -> Color(0xFFFF5500) // SoundCloud Orange
        packageName.contains("tidal", ignoreCase = true) -> Color(0xFF000000) // Tidal Black
        packageName.contains("deezer", ignoreCase = true) -> Color(0xFFA237FF) // Deezer Orange
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
    
    // Shake control settings - enhanced with new behavior system
    val shakeSettingsManager = remember { ShakeControlSettingsManager(context) }
    var shakeControlSettings by remember { mutableStateOf(ShakeControlSettings()) }
    
    // Legacy state variables for backward compatibility (removed after migration)
    var behaviour by remember { mutableStateOf("skip") }
    
    // Auto-start settings state
    var autoStartEnabled by remember { mutableStateOf(false) }
    var autoStartPaused by remember { mutableStateOf(false) }
    var autoStartDelay by remember { mutableStateOf(1000L) }
    var autoStopDelay by remember { mutableStateOf(3000L) }
    var autoStartControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var musicApps by remember { mutableStateOf<List<MusicAppWhitelistManager.MusicAppInfo>>(emptyList()) }
    var isLoadingMusicApps by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // App priority for sorting (most popular apps first)
    val appPriority = mapOf(
        "com.spotify.music"                       to 1,
        "com.google.android.apps.youtube.music"   to 2,
        "com.google.android.youtube"              to 3,
        "com.amazon.mp3"                         to 4,
        "com.apple.android.music"                to 5,
        "com.soundcloud.android"                 to 6,
        "com.tidal.android"                      to 7,
        "com.deezer.android.app"                 to 8,
        "com.pandora.android"                    to 9,
        "com.shazam.android"                     to 10,
        "com.clearchannel.iheartradio.controller" to 11,
        "com.maxmpz.audioplayer"                 to 12,
        "in.krosbits.musicolet"                  to 13,
        "org.videolan.vlc"                       to 14,
        "com.jetappfactory.jetaudio"             to 15,
        "com.aimp.player"                        to 16,
        "com.tbig.playerprotrial"                to 17,
        "com.musicplayer.blackplayerfree"        to 18,
        "com.foobar2000.foobar2000"              to 19,
        "com.jrtstudio.AnotherMusicPlayer"       to 20,
        "com.gaana"                               to 21,
        "com.jio.media.jiobeats"                 to 22,
        "com.bsbportal.music"                    to 23,
        "com.hungama.myplay.activity"            to 24,
        "com.skysoft.kkbox.android"              to 25,
        "com.tencent.qqmusic"                    to 26,
        "com.tencent.ibg.joox"                   to 27,
        "com.netease.cloudmusic"                 to 28,
        "ru.yandex.music"                        to 29,
        "com.bandcamp.android"                   to 30,
        "com.napster.android"                    to 31,
        "com.moonvideo.android.resso"            to 32,
        "com.audiomack"                           to 33
    )
    val whitelistManager = remember { MusicAppWhitelistManager.getInstance(context) }
    
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // Function to load and sort music apps
    fun loadMusicApps() {
        coroutineScope.launch {
            isLoadingMusicApps = true
            withContext(Dispatchers.IO) {
                try {
                    whitelistManager.debugAppDetection()
                    val apps = whitelistManager.getInstalledMusicApps()
                        .filter { it.isInstalled }
                        .sortedWith(
                            compareBy(
                                { !it.isWhitelisted },  // Whitelisted first
                                { appPriority[it.packageName] ?: 999 },  // Then by priority
                                { it.appName.lowercase() }  // Then alphabetically
                            )
                        )
                    musicApps = apps
                    Log.d("SettingsScreen", "Loaded ${apps.size} music apps:")
                    apps.take(5).forEach { app ->
                        Log.d("SettingsScreen", "  - ${app.appName}: installed=${app.isInstalled}, whitelisted=${app.isWhitelisted}")
                    }
                } catch (e: Exception) {
                    Log.e("SettingsScreen", "Failed to load music apps", e)
                }
            }
            isLoadingMusicApps = false
        }
    }
    
    // Load initial values asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            // Load permissions status
            notificationAccessGranted = isNotificationAccessGranted(context)
            mediaServiceWorking = isMediaControlServiceWorking(context)
            isLoadingPermissions = false
            
            // Load shake control settings using new manager
            shakeControlSettings = shakeSettingsManager.loadSettings()
            
            // Load auto-start preferences
            autoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
            autoStartPaused = prefs.getBoolean("auto_start_paused", false)
            autoStartDelay = 0L // Instant trigger
            autoStopDelay = prefs.getLong("auto_stop_delay", 3000L)
            
            // Load music apps
            loadMusicApps()
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
    
    // Live update auto-start switch when changed by shake controls or other sources
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "auto_start_enabled") {
                autoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
            } else if (key == "auto_start_paused") {
                autoStartPaused = prefs.getBoolean("auto_start_paused", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
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
                        enabled = notificationAccessGranted,
                        modifier = Modifier.height(40.dp).width(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (notificationAccessGranted) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (notificationAccessGranted) 
                                MaterialTheme.colorScheme.onError 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = "Glyph Interface",
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

        // Auto-Start Service Card
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
                        ) { autoStartControlsExpanded = !autoStartControlsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Auto-Start Service",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = customFont
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Automatically activate service when music starts playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Animated expand/collapse icon
                    val autoStartRotationAngle by animateFloatAsState(
                        targetValue = if (autoStartControlsExpanded) 180f else 0f,
                        label = "autoStartExpandIcon"
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (autoStartControlsExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(autoStartRotationAngle),
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
                        text = "Auto-Start",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Custom toggle switch to match shake controls style
                    val isControlledByShake = shakeControlSettings.enabled && 
                                            shakeControlSettings.behavior == ShakeBehavior.AUTO_START
                    
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (autoStartEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .alpha(if (autoStartPaused) 0.5f else 1.0f)
                            .clickable { 
                                val enabled = !autoStartEnabled
                                autoStartEnabled = enabled
                                prefs.edit()
                                    .putBoolean("auto_start_enabled", enabled)
                                    .putBoolean("auto_start_paused", false) // Clear paused state when manually toggling
                                    .apply()
                                
                                // Start/stop the detection service
                                if (enabled) {
                                    MusicDetectionService.start(context)
                                } else {
                                    MusicDetectionService.stop(context)
                                }
                                
                                Log.d("SettingsScreen", "Auto-start service: $enabled")
                                
                                // Auto-expand when enabling, auto-collapse when disabling
                                if (enabled && !autoStartControlsExpanded) {
                                    autoStartControlsExpanded = true
                                } else if (!enabled && autoStartControlsExpanded) {
                                    autoStartControlsExpanded = false
                                }
                            }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .align(if (autoStartEnabled) Alignment.CenterEnd else Alignment.CenterStart)
                        )
                    }
                }

                // Animated visibility for detailed settings
                AnimatedVisibility(
                    visible = autoStartControlsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Divider between header and settings
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 0.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        
                        // Touch control limitation note
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Note",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Note: Glyph button controls are not available when auto-started. Shake controls still works.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                lineHeight = 16.sp
                            )
                        }
                        
                        // Music Apps List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Installed Music Apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                val refreshRotation by animateFloatAsState(
                                    targetValue = if (isLoadingMusicApps) 360f else 0f,
                                    animationSpec = tween(1000),
                                    label = "refreshRotation"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (!isLoadingMusicApps) {
                                                loadMusicApps()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh music apps",
                                        modifier = Modifier
                                            .size(20.dp)
                                            .rotate(refreshRotation),
                                        tint = if (isLoadingMusicApps) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Text(
                                text = "Select which apps should trigger auto-start when music playback is detected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            )
                            
                            if (isLoadingMusicApps) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    musicApps.forEach { app ->
                                        val iconRotation by animateFloatAsState(
                                            targetValue = if (app.isWhitelisted) 45f else 0f,
                                            animationSpec = tween(300),
                                            label = "iconRotation"
                                        )
                                        
                                        val iconColor by animateColorAsState(
                                            targetValue = if (app.isWhitelisted) NothingRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                            animationSpec = tween(300),
                                            label = "iconColor"
                                        )
                                        
                                        var appIcon by remember(app.packageName) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                        
                                        // Load app icon asynchronously
                                        LaunchedEffect(app.packageName) {
                                            withContext(Dispatchers.IO) {
                                                appIcon = getAppIcon(context, app.packageName)
                                            }
                                        }
                                        
                                        OutlinedCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val wasWhitelisted = app.isWhitelisted
                                                    whitelistManager.toggleWhitelist(app.packageName)
                                                    
                                                    // Update only this item instead of refreshing entire list
                                                    musicApps = musicApps.map { appInfo ->
                                                        if (appInfo.packageName == app.packageName) {
                                                            appInfo.copy(isWhitelisted = !appInfo.isWhitelisted)
                                                        } else {
                                                            appInfo
                                                        }
                                                    }
                                                    
                                                    // Live update: If app was whitelisted and now disabled, 
                                                    // and service is running, notify service to check if this app is currently active
                                                    if (wasWhitelisted && !app.isWhitelisted && autoStartEnabled) {
                                                        Log.d("SettingsScreen", "App ${app.appName} disabled - sending update to running service")
                                                        
                                                        // Send broadcast to notify service that whitelist changed
                                                        val updateIntent = Intent("com.pauwma.glyphbeat.WHITELIST_CHANGED")
                                                        updateIntent.putExtra("changed_package", app.packageName)
                                                        updateIntent.putExtra("is_whitelisted", false)
                                                        context.sendBroadcast(updateIntent)
                                                    } else if (!wasWhitelisted && app.isWhitelisted && autoStartEnabled) {
                                                        Log.d("SettingsScreen", "App ${app.appName} enabled - sending update to running service")
                                                        
                                                        // Send broadcast to notify service of new whitelisted app
                                                        val updateIntent = Intent("com.pauwma.glyphbeat.WHITELIST_CHANGED")
                                                        updateIntent.putExtra("changed_package", app.packageName)
                                                        updateIntent.putExtra("is_whitelisted", true)
                                                        context.sendBroadcast(updateIntent)
                                                    }
                                                },
                                            colors = CardDefaults.outlinedCardColors(
                                                containerColor = Color.Transparent
                                            ),
                                            border = BorderStroke(
                                                width = if (app.isWhitelisted) 2.dp else 1.dp,
                                                color = if (app.isWhitelisted) NothingRed else MaterialTheme.colorScheme.outline
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // App icon
                                                Box(
                                                    modifier = Modifier.size(40.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    appIcon?.let { icon ->
                                                        Image(
                                                            bitmap = icon.asImageBitmap(),
                                                            contentDescription = "${app.appName} icon",
                                                            modifier = Modifier
                                                                .size(32.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        )
                                                    } ?: Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = "Music app",
                                                        modifier = Modifier.size(32.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                
                                                // App info
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = app.appName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = app.packageName,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                
                                                // Add/Remove icon with rotation animation
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = if (app.isWhitelisted) "Remove from whitelist" else "Add to whitelist",
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .rotate(iconRotation),
                                                    tint = iconColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } // End of AnimatedVisibility Column
                } // End of AnimatedVisibility
            }
        }

        // Enhanced Shake Controls Section
        ShakeControlsSection(
            settings = shakeControlSettings,
            onSettingsChange = { newSettings ->
                shakeControlSettings = newSettings
                shakeSettingsManager.saveSettings(newSettings)
                Log.d("SettingsScreen", "Shake control settings updated: ${newSettings.behavior.id}, enabled=${newSettings.enabled}")
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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
                    text = "GlyphBeat is ad-free and built with love. Enjoying it? Buy me a coffee to power new features, every bit helps. Thanks! \uD83C\uDFB5 \uD83E\uDD0D",
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

