package com.pauwma.glyphbeat.services.autostart

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nothing.ketchum.GlyphMatrixManager
import com.pauwma.glyphbeat.MainActivity
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.services.notification.MediaNotificationListenerService
import kotlinx.coroutines.*

/**
 * Background service that monitors MediaSession changes to automatically
 * activate/deactivate the Glyph Beat service when music starts/stops playing.
 * 
 * This service runs as a foreground service to ensure reliability but uses
 * minimal resources by leveraging MediaSession callbacks instead of polling.
 */
class MusicDetectionService : Service() {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var mediaSessionManager: MediaSessionManager
    private lateinit var whitelistManager: MusicAppWhitelistManager
    private lateinit var preferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    
    // State tracking
    private var isGlyphServiceActive = false
    private var isBindingInProgress = false
    private var currentPlayingApp: String? = null
    private var lastStateChangeTime = 0L
    private var autoStartDelay = 0L // Default instant activation
    private var autoStopDelay = 3000L // Default 3 second delay before stopping
    
    // Service connection for MediaPlayerToyService
    private var mediaPlayerServiceConnection: ServiceConnection? = null
    private var mediaPlayerServiceBound = false
    
    // MediaController callbacks
    private val activeControllers = mutableMapOf<String, MediaController.Callback>()
    private var mediaHelper: com.pauwma.glyphbeat.sound.MediaControlHelper? = null
    
    // Broadcast receiver for whitelist changes
    private val whitelistChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.pauwma.glyphbeat.WHITELIST_CHANGED" -> {
                    val changedPackage = intent.getStringExtra("changed_package")
                    val isWhitelisted = intent.getBooleanExtra("is_whitelisted", false)
                    
                    Log.d(LOG_TAG, "Whitelist change received: $changedPackage -> whitelisted: $isWhitelisted")
                    
                    // If app was disabled and it's currently the active app, check for alternatives
                    if (!isWhitelisted && changedPackage == currentPlayingApp && isGlyphServiceActive) {
                        Log.i(LOG_TAG, "Currently active app $changedPackage was disabled - checking for alternatives")
                        
                        // Find alternative playing apps (excluding the disabled one and blacklisted packages)
                        val activeController = mediaHelper?.getActiveMediaController()
                        val whitelistedApps = getWhitelistedApps()
                        val excludedPackages = BLACKLISTED_PACKAGES + setOfNotNull(changedPackage)
                        
                        // Check if there's an active media session from a different whitelisted app
                        val alternativeController = mediaHelper?.getActiveMediaControllerExcluding(excludedPackages)
                        
                        if (alternativeController != null && 
                            whitelistedApps.contains(alternativeController.packageName) &&
                            (mediaHelper?.isPlaying() == true)) {
                            
                            // Switch to the other playing app
                            currentPlayingApp = alternativeController.packageName
                            Log.i(LOG_TAG, "Switching to alternative playing app: ${alternativeController.packageName}")
                        } else {
                            // No other whitelisted apps playing, stop the service
                            Log.i(LOG_TAG, "No other whitelisted apps playing - stopping service")
                            currentPlayingApp = null
                            stopGlyphService()
                        }
                    }
                    // If app was enabled and it's currently playing but service isn't active, start it
                    else if (isWhitelisted && changedPackage != null) {
                        val isPlaying = mediaHelper?.isPlaying() ?: false
                        val activeController = mediaHelper?.getActiveMediaController()
                        
                        if (isPlaying && activeController?.packageName == changedPackage && !isGlyphServiceActive) {
                            Log.i(LOG_TAG, "Newly whitelisted app $changedPackage is playing - starting service")
                            currentPlayingApp = changedPackage
                            lastStateChangeTime = System.currentTimeMillis()
                            
                            if (autoStartDelay == 0L) {
                                startGlyphService(changedPackage)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Broadcast receiver for service stop notifications
    private val serviceStopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.pauwma.glyphbeat.SERVICE_STOPPED" -> {
                    val serviceName = intent.getStringExtra("service_name")
                    Log.d(LOG_TAG, "Service stop broadcast received: $serviceName")
                    
                    if (serviceName == "MediaPlayer-Demo" && isGlyphServiceActive) {
                        Log.w(LOG_TAG, "MediaPlayerToyService stopped - resetting state for potential restart")
                        
                        // Reset state variables
                        isGlyphServiceActive = false
                        mediaPlayerServiceBound = false
                        isBindingInProgress = false
                        currentPlayingApp = null
                        mediaPlayerServiceConnection = null
                        
                        // Update notification
                        updateNotification("Monitoring for music playback")
                    }
                }
            }
        }
    }
    
    private fun getWhitelistedApps(): Set<String> {
        val prefs = getSharedPreferences("whitelist_settings", Context.MODE_PRIVATE)
        return prefs.getStringSet("whitelisted_apps", emptySet()) ?: emptySet()
    }
    
    companion object {
        private const val LOG_TAG = "MusicDetectionService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "music_detection_channel"
        
        // Packages to ignore for auto-start (system apps, games, etc.)
        private val BLACKLISTED_PACKAGES = setOf(
            "com.nothing.hearthstone",      // Nothing Phone system app/game
            "com.android.systemui",         // Android system UI
            "com.google.android.googlequicksearchbox", // Google search
            "com.android.chrome",           // Chrome browser media sessions
            "com.google.android.apps.maps", // Google Maps navigation sounds
            "com.android.phone",            // Phone app call sounds
            "com.android.bluetooth",        // Bluetooth audio
            "com.google.android.dialer"     // Dialer app
        )
        
        fun start(context: Context) {
            val intent = Intent(context, MusicDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, MusicDetectionService::class.java)
            context.stopService(intent)
        }
        
        private fun isPackageBlacklisted(packageName: String?): Boolean {
            return packageName != null && BLACKLISTED_PACKAGES.contains(packageName)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "MusicDetectionService created")
        
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        whitelistManager = MusicAppWhitelistManager.getInstance(this)
        preferences = getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
        
        // Load delay settings
        autoStartDelay = preferences.getLong("auto_start_delay", 0L)
        autoStopDelay = preferences.getLong("auto_stop_delay", 3000L)
        
        // Register broadcast receiver for whitelist changes
        val whitelistFilter = IntentFilter("com.pauwma.glyphbeat.WHITELIST_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(whitelistChangeReceiver, whitelistFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(whitelistChangeReceiver, whitelistFilter)
        }
        Log.d(LOG_TAG, "Registered whitelist change receiver")
        
        // Register broadcast receiver for service stop notifications
        val serviceStopFilter = IntentFilter("com.pauwma.glyphbeat.SERVICE_STOPPED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStopReceiver, serviceStopFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(serviceStopReceiver, serviceStopFilter)
        }
        Log.d(LOG_TAG, "Registered service stop receiver")
        
        // Create notification channel and start as foreground service
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(LOG_TAG, "Successfully started as foreground service")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start foreground service: ${e.message}", e)
            // Service will still run but without foreground status
        }
        
        // Start monitoring
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(LOG_TAG, "MusicDetectionService started")
        return START_STICKY // Restart if killed
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        Log.d(LOG_TAG, "MusicDetectionService destroyed")
        
        try {
            stopMonitoring()
            scope.cancel()
            
            // Unregister broadcast receivers
            try {
                unregisterReceiver(whitelistChangeReceiver)
                Log.d(LOG_TAG, "Unregistered whitelist change receiver")
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error unregistering whitelist receiver: ${e.message}")
            }
            
            try {
                unregisterReceiver(serviceStopReceiver)
                Log.d(LOG_TAG, "Unregistered service stop receiver")
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error unregistering service stop receiver: ${e.message}")
            }
            
            // Clean up service connection if bound
            if (mediaPlayerServiceBound && mediaPlayerServiceConnection != null) {
                try {
                    unbindService(mediaPlayerServiceConnection!!)
                    Log.d(LOG_TAG, "Unbound MediaPlayerToyService during cleanup")
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Error unbinding service during cleanup: ${e.message}")
                }
            }
            
            // Clear all handlers
            handler.removeCallbacksAndMessages(null)
            
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error during service cleanup", e)
        } finally {
            super.onDestroy()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors music playback to automatically activate Glyph Beat"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Beat Auto-Start")
            .setContentText("Monitoring for music playback")
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }
    
    private fun startMonitoring() {
        Log.d(LOG_TAG, "Starting music detection monitoring with callback-based approach")
        
        // Use MediaControlHelper for efficient callback-based monitoring
        mediaHelper = com.pauwma.glyphbeat.sound.MediaControlHelper(this)
        
        mediaHelper?.registerStateChangeCallback(object : com.pauwma.glyphbeat.sound.MediaControlHelper.StateChangeCallback {
            override fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveMedia: Boolean) {
                handlePlaybackStateChange(isPlaying, hasActiveMedia)
            }
            
            override fun onActiveAppChanged(packageName: String?, appName: String?) {
                handleActiveAppChange(packageName, appName)
            }
        })
        
        // Initial check
        checkMediaSessions()
        
        // Start periodic monitoring as backup to ensure callbacks don't fail
        startPeriodicMonitoring()
    }
    
    private fun stopMonitoring() {
        Log.d(LOG_TAG, "Stopping music detection monitoring")
        
        // Cleanup MediaControlHelper
        mediaHelper?.cleanup()
        mediaHelper = null
        
        // Unregister all MediaController callbacks
        activeControllers.clear()
        
        // Stop Glyph service if active
        if (isGlyphServiceActive) {
            stopGlyphService()
        }
    }
    
    /**
     * Handle playback state changes from MediaControlHelper callbacks
     */
    private fun handlePlaybackStateChange(isPlaying: Boolean, hasActiveMedia: Boolean) {
        val currentTime = System.currentTimeMillis()
        Log.d(LOG_TAG, "CALLBACK RECEIVED - isPlaying: $isPlaying, hasActiveMedia: $hasActiveMedia, serviceActive: $isGlyphServiceActive")
        
        when {
            // Music started playing - check if from whitelisted app
            isPlaying && !isGlyphServiceActive -> {
                // Get the active controller to check if it's whitelisted
                val activeController = mediaHelper?.getActiveMediaController()
                val packageName = activeController?.packageName
                
                Log.d(LOG_TAG, "Music playing detected - isPlaying: $isPlaying, hasActiveMedia: $hasActiveMedia, packageName: $packageName")
                
                if (packageName != null && whitelistManager.isAppWhitelisted(packageName)) {
                    if (currentPlayingApp != packageName) {
                        currentPlayingApp = packageName
                        lastStateChangeTime = currentTime
                        Log.i(LOG_TAG, "Detected music playing from whitelisted app: $packageName")
                    }
                    
                    // Start service instantly (autoStartDelay is 0L)
                    if (autoStartDelay == 0L) {
                        startGlyphService(packageName)
                    } else if (currentTime - lastStateChangeTime >= autoStartDelay) {
                        startGlyphService(packageName)
                    } else {
                        // Schedule delayed activation
                        handler.postDelayed({
                            if (mediaHelper?.isPlaying() == true && currentPlayingApp == packageName) {
                                startGlyphService(packageName)
                            }
                        }, autoStartDelay - (currentTime - lastStateChangeTime))
                    }
                } else {
                    Log.d(LOG_TAG, "Music playing from non-whitelisted or unknown app: $packageName")
                }
            }
            
            // Music stopped
            !isPlaying && isGlyphServiceActive -> {
                if (currentPlayingApp != null) {
                    currentPlayingApp = null
                    lastStateChangeTime = currentTime
                    Log.d(LOG_TAG, "Music playback stopped")
                    
                    // Schedule delayed deactivation
                    handler.postDelayed({
                        if (mediaHelper?.isPlaying() != true) {
                            stopGlyphService()
                        }
                    }, autoStopDelay)
                }
            }
            
            // No media available - app closed or removed
            !hasActiveMedia && isGlyphServiceActive -> {
                Log.i(LOG_TAG, "No active media detected - stopping Glyph service immediately")
                currentPlayingApp = null
                stopGlyphService() // Stop immediately, no delay needed when app is closed
            }
        }
    }
    
    /**
     * Handle active app changes from MediaControlHelper callbacks
     */
    private fun handleActiveAppChange(packageName: String?, appName: String?) {
        Log.d(LOG_TAG, "CALLBACK RECEIVED - Active app changed to: $appName ($packageName)")
        
        // If new app is playing and whitelisted, activate service or update current app
        if (packageName != null && whitelistManager.isAppWhitelisted(packageName)) {
            val isPlaying = mediaHelper?.isPlaying() ?: false
            Log.d(LOG_TAG, "Whitelisted app $packageName is ${if (isPlaying) "playing" else "not playing"}")
            
            if (isPlaying) {
                if (!isGlyphServiceActive) {
                    // New whitelisted app started playing - activate service
                    currentPlayingApp = packageName
                    lastStateChangeTime = System.currentTimeMillis()
                    Log.i(LOG_TAG, "New whitelisted app started playing: $packageName")
                    
                    if (autoStartDelay == 0L) {
                        startGlyphService(packageName)
                    }
                } else if (currentPlayingApp != packageName) {
                    // Different whitelisted app - update reference but keep service active
                    currentPlayingApp = packageName
                    Log.d(LOG_TAG, "Music app changed to whitelisted app: $packageName")
                }
            }
        } else {
            Log.d(LOG_TAG, "App change to non-whitelisted or unknown app: $packageName")
        }
    }
    
    private fun checkMediaSessions() {
        try {
            val componentName = ComponentName(this, MediaNotificationListenerService::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            
            // Find playing sessions from whitelisted apps
            val playingApp = sessions.firstOrNull { controller ->
                val packageName = controller.packageName
                val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
                val isWhitelisted = whitelistManager.isAppWhitelisted(packageName)
                
                if (isPlaying && isWhitelisted) {
                    registerControllerCallback(controller)
                }
                
                isPlaying && isWhitelisted
            }?.packageName
            
            val currentTime = System.currentTimeMillis()
            
            when {
                // Music started playing from whitelisted app
                playingApp != null && !isGlyphServiceActive -> {
                    if (currentPlayingApp != playingApp) {
                        currentPlayingApp = playingApp
                        lastStateChangeTime = currentTime
                        Log.d(LOG_TAG, "Detected music playing from: $playingApp")
                    }
                    
                    // Start service after delay to avoid false triggers
                    if (currentTime - lastStateChangeTime >= autoStartDelay) {
                        startGlyphService(playingApp)
                    }
                }
                
                // Music stopped or app changed
                playingApp == null && isGlyphServiceActive -> {
                    if (currentPlayingApp != null) {
                        currentPlayingApp = null
                        lastStateChangeTime = currentTime
                        Log.d(LOG_TAG, "Music playback stopped")
                    }
                    
                    // Stop service after delay
                    if (currentTime - lastStateChangeTime >= autoStopDelay) {
                        stopGlyphService()
                    }
                }
                
                // App changed while playing
                playingApp != null && playingApp != currentPlayingApp && isGlyphServiceActive -> {
                    currentPlayingApp = playingApp
                    Log.d(LOG_TAG, "Music app changed to: $playingApp")
                    // Keep service active, just update the app
                }
            }
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Missing notification access permission")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error checking media sessions", e)
        }
    }
    
    private fun registerControllerCallback(controller: MediaController) {
        val packageName = controller.packageName
        
        // Skip if already registered
        if (activeControllers.containsKey(packageName)) return
        
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.v(LOG_TAG, "Playback state changed for $packageName: ${state?.state}")
                // State change will be detected in next check cycle
            }
        }
        
        try {
            controller.registerCallback(callback, handler)
            activeControllers[packageName] = callback
            Log.d(LOG_TAG, "Registered callback for: $packageName")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to register callback for $packageName", e)
        }
    }
    
    /**
     * Start periodic monitoring as backup to callback system
     */
    private fun startPeriodicMonitoring() {
        Log.d(LOG_TAG, "Starting periodic media session monitoring")
        
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    checkMediaSessionsDirectly()
                    // Continue periodic checks every 2 seconds
                    handler.postDelayed(this, 2000L)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Error in periodic monitoring: ${e.message}")
                    // Continue monitoring even if there's an error
                    handler.postDelayed(this, 2000L)
                }
            }
        }, 2000L) // Start after 2 seconds
    }
    
    /**
     * Direct media session check as backup to callback system
     */
    private fun checkMediaSessionsDirectly() {
        val hasActiveMedia = mediaHelper?.hasActiveMediaSession(BLACKLISTED_PACKAGES) ?: false
        val isPlaying = mediaHelper?.isPlaying() ?: false
        
        
        Log.v(LOG_TAG, "Periodic check - hasActiveMedia: $hasActiveMedia, isPlaying: $isPlaying, serviceActive: $isGlyphServiceActive")
        
        when {
            // No media sessions at all and service is active - stop immediately
            !hasActiveMedia && isGlyphServiceActive -> {
                Log.i(LOG_TAG, "Periodic check: No active media detected - stopping service immediately")
                currentPlayingApp = null
                stopGlyphService()
            }
            
            // Music playing from whitelisted app but service not active - start it
            isPlaying && !isGlyphServiceActive -> {
                val activeController = mediaHelper?.getActiveMediaController()
                val packageName = activeController?.packageName
                
                if (packageName != null && whitelistManager.isAppWhitelisted(packageName)) {
                    Log.i(LOG_TAG, "Periodic check: Found music playing from whitelisted app - starting service: $packageName")
                    currentPlayingApp = packageName
                    lastStateChangeTime = System.currentTimeMillis()
                    
                    if (autoStartDelay == 0L) {
                        startGlyphService(packageName)
                    }
                }
            }
        }
    }
    
    private fun startGlyphService(appName: String) {
        Log.i(LOG_TAG, "Auto-activating Media Player service for: $appName")
        
        // Don't bind if already bound or binding in progress
        if (mediaPlayerServiceBound || isBindingInProgress) {
            Log.d(LOG_TAG, "MediaPlayerToyService already bound/binding, skipping activation (bound: $mediaPlayerServiceBound, binding: $isBindingInProgress)")
            return
        }
        
        try {
            // Create service connection for MediaPlayerToyService
            mediaPlayerServiceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Log.d(LOG_TAG, "MediaPlayerToyService connected - Glyph matrix should now be active")
                    mediaPlayerServiceBound = true
                    isGlyphServiceActive = true
                    isBindingInProgress = false // Clear binding flag
                    
                    // Update notification to show active state
                    updateNotification("Media Player active for: $appName")
                    
                    // Store activation time for analytics
                    preferences.edit()
                        .putLong("last_auto_start_time", System.currentTimeMillis())
                        .putString("last_auto_start_app", appName)
                        .apply()
                }
                
                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.d(LOG_TAG, "MediaPlayerToyService disconnected")
                    mediaPlayerServiceBound = false
                    isGlyphServiceActive = false
                    isBindingInProgress = false // Clear binding flag
                    mediaPlayerServiceConnection = null
                    
                    // Update notification
                    updateNotification("Monitoring for music playback")
                }
            }
            
            // Create intent to bind to MediaPlayerToyService
            val serviceIntent = Intent().apply {
                component = ComponentName("com.pauwma.glyphbeat", "com.pauwma.glyphbeat.services.media.MediaPlayerToyService")
                putExtra("auto_activated", true)
            }
            
            // Set binding in progress flag before attempting bind
            isBindingInProgress = true
            
            // Bind to the service to activate it
            val bindSuccess = bindService(serviceIntent, mediaPlayerServiceConnection!!, Context.BIND_AUTO_CREATE)
            
            if (bindSuccess) {
                Log.i(LOG_TAG, "Successfully initiated MediaPlayerToyService binding")
            } else {
                Log.e(LOG_TAG, "Failed to bind to MediaPlayerToyService")
                isBindingInProgress = false // Clear flag on failure
                mediaPlayerServiceConnection = null
            }
                
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to auto-activate Media Player service", e)
            isGlyphServiceActive = false
            isBindingInProgress = false // Clear flag on exception
            mediaPlayerServiceConnection = null
        }
    }
    
    private fun stopGlyphService() {
        Log.i(LOG_TAG, "Auto-deactivating Media Player service")
        
        // Don't unbind if not bound
        if (!mediaPlayerServiceBound || mediaPlayerServiceConnection == null) {
            Log.d(LOG_TAG, "MediaPlayerToyService not bound, skipping deactivation")
            return
        }
        
        try {
            // Unbind from the MediaPlayerToyService to deactivate it
            unbindService(mediaPlayerServiceConnection!!)
            
            // Track that we've deactivated the service
            isGlyphServiceActive = false
            mediaPlayerServiceBound = false
            isBindingInProgress = false
            currentPlayingApp = null
            mediaPlayerServiceConnection = null
            
            // Update notification
            updateNotification("Monitoring for music playback")
            
            Log.i(LOG_TAG, "MediaPlayerToyService unbound successfully")
            
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to auto-deactivate Media Player service", e)
            // Cleanup state even if unbind fails
            isGlyphServiceActive = false
            mediaPlayerServiceBound = false
            isBindingInProgress = false
            currentPlayingApp = null
            mediaPlayerServiceConnection = null
        }
    }
    
    
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Glyph Beat Auto-Start")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}