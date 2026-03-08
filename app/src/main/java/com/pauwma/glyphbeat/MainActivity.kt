package com.pauwma.glyphbeat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
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
import com.pauwma.glyphbeat.ui.navigation.NavigationAnimations
import com.pauwma.glyphbeat.theme.NothingAndroidSDKDemoTheme
import com.pauwma.glyphbeat.ui.screens.SettingsScreen
import com.pauwma.glyphbeat.ui.navigation.PillNavigationBar
import com.pauwma.glyphbeat.ui.navigation.MediaPlayerScreen
import com.pauwma.glyphbeat.ui.navigation.TrackControlScreen
import com.pauwma.glyphbeat.ui.navigation.SettingsNavigationScreen
import com.pauwma.glyphbeat.tutorial.TutorialActivity
import com.pauwma.glyphbeat.tutorial.utils.TutorialPreferences
import com.pauwma.glyphbeat.core.AppConfig
import com.pauwma.glyphbeat.utils.UpdatePreferences
import com.pauwma.glyphbeat.utils.UidObfuscation
import com.pauwma.glyphbeat.data.UpdateManager
import com.pauwma.glyphbeat.data.ImportThemeManager
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.ui.dialogs.UpdateDialog
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("MainActivity", "RECORD_AUDIO permission granted - audio visualization enabled")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Splash Screen
        installSplashScreen()

        // Check if tutorial should be shown
        if (!TutorialPreferences.isTutorialCompleted(this)) {
            // Launch tutorial activity
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Update version tracking
        UpdatePreferences.updateLastLaunchVersion(this, AppConfig.VERSION_CODE)

        // Initialize app language based on system locale (first install only)
        initializeAppLanguage(this)

        enableEdgeToEdge()

        // Check and request notification access permission if not in tutorial
        if (!isNotificationAccessGranted(this)) {
            // Only request if user didn't skip in tutorial
            if (!TutorialPreferences.hasSkippedPermissions(this)) {
                requestNotificationAccess()
            }
        }

        // Request microphone permission for audio visualization (Visualizer API)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // Start auto-start service if enabled
        val prefs = getSharedPreferences("glyph_settings", MODE_PRIVATE)
        val isAutoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
        if (isAutoStartEnabled) {
            com.pauwma.glyphbeat.services.autostart.MusicDetectionService.start(this)
        }

        // Handle deep link if launched via glyphbeat://import
        handleImportDeepLink(intent)

        setContent {
            NothingAndroidSDKDemoTheme {
                val navController = rememberNavController()

                // Update dialog state
                var showUpdateDialog by remember { mutableStateOf(false) }
                val updateContent = remember { UpdateManager.getUpdateContent(AppConfig.VERSION_CODE) }

                // Check if we should show update dialog
                LaunchedEffect(Unit) {
                    android.util.Log.d("MainActivity", "Checking update dialog...")
                    android.util.Log.d("MainActivity", "ENABLE_UPDATE_DIALOG: ${AppConfig.ENABLE_UPDATE_DIALOG}")
                    android.util.Log.d("MainActivity", "VERSION_CODE: ${AppConfig.VERSION_CODE}")
                    android.util.Log.d("MainActivity", "MIN_VERSION_FOR_UPDATES: ${AppConfig.MIN_VERSION_FOR_UPDATES}")
                    android.util.Log.d("MainActivity", "updateContent: $updateContent")

                    val shouldShow = UpdatePreferences.shouldShowUpdateDialog(this@MainActivity, AppConfig.VERSION_CODE)
                    android.util.Log.d("MainActivity", "shouldShowUpdateDialog: $shouldShow")

                    if (AppConfig.ENABLE_UPDATE_DIALOG &&
                        AppConfig.VERSION_CODE >= AppConfig.MIN_VERSION_FOR_UPDATES &&
                        updateContent != null &&
                        shouldShow) {
                        android.util.Log.d("MainActivity", "Showing update dialog in 500ms...")
                        kotlinx.coroutines.delay(500) // Small delay for better UX
                        android.util.Log.d("MainActivity", "Setting showUpdateDialog to true")
                        showUpdateDialog = true
                        android.util.Log.d("MainActivity", "showUpdateDialog is now: $showUpdateDialog")
                    } else {
                        android.util.Log.d("MainActivity", "Not showing update dialog")
                    }
                }

                // Show update dialog
                if (showUpdateDialog && updateContent != null) {
                    android.util.Log.d("MainActivity", "Calling UpdateDialog composable")
                    UpdateDialog(
                        updateContent = updateContent,
                        onDismiss = {
                            android.util.Log.d("MainActivity", "UpdateDialog dismissed")
                            showUpdateDialog = false
                            UpdatePreferences.markUpdateDialogShown(this@MainActivity, AppConfig.VERSION_CODE)
                        }
                    )
                } else {
                    android.util.Log.d("MainActivity", "Not showing dialog: showUpdateDialog=$showUpdateDialog, updateContent=$updateContent")
                }

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
                        enterTransition = { 
                            NavigationAnimations.getEnterTransition(
                                initialState.destination.route,
                                targetState.destination.route ?: ""
                            )
                        },
                        exitTransition = { 
                            NavigationAnimations.getExitTransition(
                                initialState.destination.route ?: "",
                                targetState.destination.route
                            )
                        },
                        popEnterTransition = { 
                            NavigationAnimations.getEnterTransition(
                                initialState.destination.route,
                                targetState.destination.route ?: ""
                            )
                        },
                        popExitTransition = { 
                            NavigationAnimations.getExitTransition(
                                initialState.destination.route ?: "",
                                targetState.destination.route
                            )
                        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleImportDeepLink(intent)
    }

    private fun handleImportDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "glyphbeat" || data.host != "import") return

        val postIdStr = data.getQueryParameter("postId") ?: return
        val token = data.getQueryParameter("token") ?: return

        val postId = postIdStr.toLongOrNull() ?: return
        val uid = try {
            UidObfuscation.decode(token)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to decode import token", e)
            return
        }

        Log.d("MainActivity", "Import deep link received: postId=$postId")
        Toast.makeText(this, "Importing theme...", Toast.LENGTH_SHORT).show()

        val importManager = ImportThemeManager(this)
        CoroutineScope(Dispatchers.Main).launch {
            val result = importManager.importTheme(postId, uid)
            when (result) {
                is ImportThemeManager.ImportResult.Success -> {
                    // Reload themes in the repository
                    ThemeRepository.getInstance(this@MainActivity).reloadCustomThemes()
                    Toast.makeText(this@MainActivity, "Theme \"${result.themeName}\" imported!", Toast.LENGTH_LONG).show()
                }
                is ImportThemeManager.ImportResult.AlreadyImported -> {
                    Toast.makeText(this@MainActivity, "This theme is already imported", Toast.LENGTH_SHORT).show()
                }
                is ImportThemeManager.ImportResult.NotSupporter -> {
                    Toast.makeText(this@MainActivity, "Supporter status required to import themes", Toast.LENGTH_LONG).show()
                }
                is ImportThemeManager.ImportResult.ThemeLimitReached -> {
                    Toast.makeText(this@MainActivity, "Maximum custom themes reached (20). Delete some to import more.", Toast.LENGTH_LONG).show()
                }
                is ImportThemeManager.ImportResult.Error -> {
                    Toast.makeText(this@MainActivity, "Import failed: ${result.message}", Toast.LENGTH_LONG).show()
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

/**
 * Detects the system language and returns the corresponding app language code.
 * Uses centralized language mapping from AppConfig.
 * Falls back to default language for unsupported languages.
 */
fun detectSystemLanguage(): String {
    val systemLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        android.content.res.Resources.getSystem().configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        android.content.res.Resources.getSystem().configuration.locale
    }

    return AppConfig.SupportedLanguages.mapSystemLanguage(systemLocale.language)
}

/**
 * Initializes app language on first install by detecting system language.
 * Only sets language if user hasn't manually chosen one before.
 */
fun initializeAppLanguage(context: android.content.Context) {
    val prefs = context.getSharedPreferences("glyph_settings", android.content.Context.MODE_PRIVATE)

    // Check if user has manually set language before
    val isManuallySet = prefs.getBoolean("user_language_manually_set", false)
    val hasLanguagePreference = prefs.contains("app_language")

    // Only auto-detect on first install (no existing language preference) and not manually set
    if (!hasLanguagePreference && !isManuallySet) {
        val systemLanguage = detectSystemLanguage()

        // Save the detected system language
        prefs.edit()
            .putString("app_language", systemLanguage)
            .putBoolean("user_language_manually_set", false)  // Mark as auto-detected, not manual
            .apply()

        // Apply the detected language
        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
            androidx.core.os.LocaleListCompat.forLanguageTags(systemLanguage)
        )
    }
}