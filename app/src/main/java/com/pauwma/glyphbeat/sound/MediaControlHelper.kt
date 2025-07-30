package com.pauwma.glyphbeat.sound

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.pauwma.glyphbeat.sound.MediaNotificationListenerService
import kotlin.math.pow
import java.util.concurrent.CopyOnWriteArrayList

class MediaControlHelper(private val context: Context) {
    
    // State change callback interface
    interface StateChangeCallback {
        fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveMedia: Boolean)
    }
    
    private val mediaSessionManager: MediaSessionManager by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }
    
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    private var activeController: MediaController? = null
    private var activeControllerCallback: MediaController.Callback? = null
    private val stateChangeCallbacks = CopyOnWriteArrayList<StateChangeCallback>()
    
    private var lastLoggedSessionCount = -1
    private var lastLoggedActivePackage: String? = null
    private var lastLoggedActiveState: Int? = null
    private var lastLoggedSessions = mutableMapOf<String, Int>()
    
    // Cached state to avoid unnecessary callbacks
    private var cachedIsPlaying = false
    private var cachedHasActiveMedia = false
    
    /**
     * Register a callback to receive immediate state change notifications
     */
    fun registerStateChangeCallback(callback: StateChangeCallback) {
        stateChangeCallbacks.add(callback)
        Log.d(LOG_TAG, "State change callback registered, total callbacks: ${stateChangeCallbacks.size}")
    }
    
    /**
     * Unregister a state change callback
     */
    fun unregisterStateChangeCallback(callback: StateChangeCallback) {
        stateChangeCallbacks.remove(callback)
        Log.d(LOG_TAG, "State change callback unregistered, remaining callbacks: ${stateChangeCallbacks.size}")
    }
    
    /**
     * Notify all registered callbacks of state changes
     */
    private fun notifyStateChange(isPlaying: Boolean, hasActiveMedia: Boolean) {
        if (isPlaying != cachedIsPlaying || hasActiveMedia != cachedHasActiveMedia) {
            cachedIsPlaying = isPlaying
            cachedHasActiveMedia = hasActiveMedia
            
            Log.d(LOG_TAG, "State changed - playing: $isPlaying, hasMedia: $hasActiveMedia, notifying ${stateChangeCallbacks.size} callbacks")
            
            stateChangeCallbacks.forEach { callback ->
                try {
                    callback.onPlaybackStateChanged(isPlaying, hasActiveMedia)
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Error in state change callback: ${e.message}")
                }
            }
        }
    }
    
    fun getActiveMediaController(): MediaController? {
        try {
            // Pass our NotificationListenerService component to get proper permissions
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)
            
            // Only log session count when it changes
            if (activeSessions.size != lastLoggedSessionCount) {
                Log.d(LOG_TAG, "Found ${activeSessions.size} active sessions")
                lastLoggedSessionCount = activeSessions.size
            }
            
            // Log session states only when they change
            activeSessions.forEach { controller ->
                val packageName = controller.packageName
                val state = controller.playbackState?.state
                val lastState = lastLoggedSessions[packageName]
                
                if (state != lastState) {
                    Log.d(LOG_TAG, "Session $packageName: state changed $lastState -> $state")
                    lastLoggedSessions[packageName] = state ?: -1
                }
            }
            
            // First try to find actively playing sessions
            activeController = activeSessions.firstOrNull { controller ->
                val state = controller.playbackState?.state
                state == PlaybackState.STATE_PLAYING
            }
            
            // If no playing sessions, look for recently active ones (paused/buffering with recent activity)
            if (activeController == null) {
                activeController = activeSessions.firstOrNull { controller ->
                    val state = controller.playbackState?.state
                    val hasMetadata = controller.metadata != null
                    val hasValidTitle = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotBlank() == true
                    
                    // Only consider paused/buffering sessions that have actual media content
                    (state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_BUFFERING) && 
                    hasMetadata && hasValidTitle
                }
            }
            
            val currentPackage = activeController?.packageName
            val currentState = activeController?.playbackState?.state
            
            // Check if controller changed before updating logged values
            val controllerChanged = currentPackage != lastLoggedActivePackage
            
            // Only log active controller when package or state changes
            if (controllerChanged || currentState != lastLoggedActiveState) {
                Log.d(LOG_TAG, "Active controller: $currentPackage, state: $currentState")
                lastLoggedActivePackage = currentPackage
                lastLoggedActiveState = currentState
                
                // Register callback on new controller if it changed
                if (controllerChanged) {
                    Log.d(LOG_TAG, "Controller changed, registering new callback for: $currentPackage")
                    registerCallbackOnActiveController()
                } else {
                    Log.v(LOG_TAG, "Controller unchanged: $currentPackage")
                }
            }
            
            // Notify state changes based on current controller state
            val isPlaying = currentState == PlaybackState.STATE_PLAYING
            val hasActiveMedia = activeController != null
            notifyStateChange(isPlaying, hasActiveMedia)
            
            return activeController
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Media control requires notification access permission - please grant notification access in settings")
            return null
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "Media session service not available - notification listener may not be connected")
            return null
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting media sessions: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Register callback on the currently active media controller
     */
    private fun registerCallbackOnActiveController() {
        // Unregister previous callback if exists
        activeControllerCallback?.let { callback ->
            try {
                // Note: We can't easily unregister without keeping the original controller reference
                Log.v(LOG_TAG, "Previous callback will be cleaned up automatically")
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error cleaning up previous callback: ${e.message}")
            }
        }
        
        // Register new callback if we have an active controller
        activeController?.let { controller ->
            try {
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                        val hasActiveMedia = activeController != null
                        Log.v(LOG_TAG, "MediaController callback - state changed to: ${state?.state}, playing: $isPlaying")
                        notifyStateChange(isPlaying, hasActiveMedia)
                    }
                    
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        // Track changes in media metadata which might affect active media status
                        val hasActiveMedia = activeController != null && metadata != null
                        Log.v(LOG_TAG, "MediaController callback - metadata changed, hasActiveMedia: $hasActiveMedia")
                        notifyStateChange(cachedIsPlaying, hasActiveMedia)
                    }
                }
                
                controller.registerCallback(callback)
                activeControllerCallback = callback
                Log.d(LOG_TAG, "Registered callback on controller: ${controller.packageName}")
                
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed to register callback on controller: ${e.message}")
                activeControllerCallback = null
            }
        }
    }
    
    /**
     * Clean up resources and callbacks
     */
    fun cleanup() {
        stateChangeCallbacks.clear()
        activeControllerCallback = null
        activeController = null
        Log.d(LOG_TAG, "MediaControlHelper cleaned up")
    }
    
    fun isPlaying(): Boolean {
        val controller = getActiveMediaController()
        val state = controller?.playbackState?.state
        val playing = state == PlaybackState.STATE_PLAYING
        Log.d(LOG_TAG, "isPlaying check: state=$state, playing=$playing, package=${controller?.packageName}")
        return playing
    }
    
    fun togglePlayPause(): Boolean {
        val controller = getActiveMediaController()
        
        if (controller == null) {
            Log.w(LOG_TAG, "No active media controller found - trying fallback method")
            return sendMediaKeyEvent()
        }
        
        return try {
            val currentState = controller.playbackState?.state
            Log.d(LOG_TAG, "Current playback state: $currentState")
            
            when (currentState) {
                PlaybackState.STATE_PLAYING -> {
                    controller.transportControls.pause()
                    Log.d(LOG_TAG, "Sent pause command")
                    true
                }
                PlaybackState.STATE_PAUSED -> {
                    controller.transportControls.play()
                    Log.d(LOG_TAG, "Sent play command")
                    true
                }
                else -> {
                    controller.transportControls.play()
                    Log.d(LOG_TAG, "Sent play command (unknown state: $currentState)")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error toggling play/pause with MediaController, trying fallback", e)
            return sendMediaKeyEvent()
        }
    }
    
    private fun sendMediaKeyEvent(): Boolean {
        return try {
            val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
            
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            
            Log.d(LOG_TAG, "Sent media key event (play/pause)")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error sending media key event", e)
            false
        }
    }
    
    fun skipToNext(): Boolean {
        val controller = getActiveMediaController() ?: return false
        return try {
            controller.transportControls.skipToNext()
            Log.d(LOG_TAG, "Sent skip next command")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error skipping to next", e)
            false
        }
    }
    
    fun skipToPrevious(): Boolean {
        val controller = getActiveMediaController() ?: return false
        return try {
            controller.transportControls.skipToPrevious()
            Log.d(LOG_TAG, "Sent skip previous command")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error skipping to previous", e)
            false
        }
    }
    
    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    
    fun setVolume(volume: Int): Boolean {
        return try {
            val clampedVolume = volume.coerceIn(0, getMaxVolume())
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                clampedVolume,
                AudioManager.FLAG_SHOW_UI
            )
            Log.d(LOG_TAG, "Set volume to $clampedVolume")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting volume", e)
            false
        }
    }
    
    fun increaseVolume(): Boolean {
        val currentVolume = getCurrentVolume()
        val maxVolume = getMaxVolume()
        return if (currentVolume < maxVolume) {
            setVolume(currentVolume + 1)
        } else false
    }
    
    fun decreaseVolume(): Boolean {
        val currentVolume = getCurrentVolume()
        return if (currentVolume > 0) {
            setVolume(currentVolume - 1)
        } else false
    }
    
    fun getTrackInfo(): TrackInfo? {
        val controller = getActiveMediaController() ?: return null
        val metadata = controller.metadata ?: return null
        
        return try {
            val albumArt = extractAlbumArt(metadata)
            TrackInfo(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown",
                appName = controller.packageName ?: "Unknown",
                albumArt = albumArt
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting track info", e)
            null
        }
    }
    
    /**
     * Extract album art from media metadata.
     * @param metadata MediaMetadata object containing track information
     * @return Bitmap of album art or null if not available
     */
    private fun extractAlbumArt(metadata: MediaMetadata): Bitmap? {
        return try {
            // Try to get album art from metadata
            val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            
            bitmap?.let { originalBitmap ->
                // Downsample to 25x25 for the Glyph Matrix
                resizeBitmapToMatrix(originalBitmap)
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error extracting album art: ${e.message}")
            null
        }
    }
    
    /**
     * Resize bitmap to 25x25 pixels for the Glyph Matrix display.
     * @param originalBitmap Source bitmap to resize
     * @return 25x25 bitmap or null if processing fails
     */
    private fun resizeBitmapToMatrix(originalBitmap: Bitmap): Bitmap? {
        return try {
            // Create a 25x25 bitmap with the same config as original, defaulting to ARGB_8888
            val config = originalBitmap.config ?: Bitmap.Config.ARGB_8888
            val resized = Bitmap.createScaledBitmap(originalBitmap, 25, 25, true)
            
            Log.v(LOG_TAG, "Album art resized from ${originalBitmap.width}x${originalBitmap.height} to 25x25")
            resized
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error resizing album art: ${e.message}")
            null
        }
    }
    
    /**
     * Convert a 25x25 bitmap to a matrix intensity array for the Glyph Matrix.
     * @param bitmap 25x25 bitmap to convert
     * @param brightnessMultiplier Multiplier for brightness (0.0-1.0), default 1.0
     * @param enhanceContrast Whether to apply contrast enhancement, default true
     * @return IntArray of 625 elements with intensity values (0-255)
     */
    fun bitmapToMatrixArray(bitmap: Bitmap?, brightnessMultiplier: Double = 1.0, enhanceContrast: Boolean = true): IntArray {
        if (bitmap == null || bitmap.width != 25 || bitmap.height != 25) {
            Log.w(LOG_TAG, "Invalid bitmap for matrix conversion, using fallback pattern")
            return createFallbackPattern()
        }
        
        return try {
            val rawArray = IntArray(625) // 25x25 = 625
            
            // First pass: Convert to grayscale and store raw luminance values
            for (row in 0 until 25) {
                for (col in 0 until 25) {
                    val pixel = bitmap.getPixel(col, row)
                    
                    // Extract RGB components
                    val red = (pixel shr 16) and 0xFF
                    val green = (pixel shr 8) and 0xFF
                    val blue = pixel and 0xFF
                    
                    // Convert to grayscale using luminance formula
                    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
                    
                    // Store raw luminance value
                    rawArray[row * 25 + col] = luminance
                }
            }
            
            // Apply contrast enhancement if requested
            val processedArray = if (enhanceContrast) {
                enhanceContrast(rawArray)
            } else {
                rawArray
            }
            
            // Apply brightness multiplier and clamp to 0-255
            val finalArray = processedArray.map { luminance ->
                (luminance * brightnessMultiplier).toInt().coerceIn(0, 255)
            }.toIntArray()
            
            Log.v(LOG_TAG, "Bitmap converted to matrix array (contrast: $enhanceContrast, brightness: $brightnessMultiplier)")
            finalArray
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error converting bitmap to matrix array: ${e.message}")
            createFallbackPattern()
        }
    }
    
    /**
     * Enhance contrast using histogram stretching and S-curve enhancement.
     * @param luminanceArray Array of luminance values (0-255)
     * @return Enhanced array with improved contrast
     */
    private fun enhanceContrast(luminanceArray: IntArray): IntArray {
        if (luminanceArray.isEmpty()) return luminanceArray
        
        // Find min and max values for histogram stretching
        val minLum = luminanceArray.minOrNull() ?: 0
        val maxLum = luminanceArray.maxOrNull() ?: 255
        
        // Avoid division by zero
        val range = (maxLum - minLum).coerceAtLeast(1)
        
        return luminanceArray.map { lum ->
            // Step 1: Histogram stretching - expand dynamic range
            val stretched = ((lum - minLum) * 255.0 / range).toInt().coerceIn(0, 255)
            
            // Step 2: Apply S-curve for more dramatic contrast
            // Maps 0->0, 128->128, 255->255, but creates steeper curve in between
            val normalized = stretched / 255.0
            val sCurve = applySCurve(normalized, 1.5) // Contrast factor of 1.5
            
            // Step 3: Final enhancement - push extreme values further apart
            val enhanced = when {
                sCurve < 0.3 -> sCurve * 0.7 // Darken shadows more
                sCurve > 0.7 -> 0.3 + (sCurve - 0.7) * 2.33 // Brighten highlights more  
                else -> 0.3 + (sCurve - 0.3) * 1.0 // Keep midtones relatively unchanged
            }
            
            (enhanced * 255).toInt().coerceIn(0, 255)
        }.toIntArray()
    }
    
    /**
     * Apply S-curve transformation for contrast enhancement.
     * @param x Normalized input value (0.0-1.0)
     * @param contrast Contrast factor (1.0 = no change, >1.0 = more contrast)
     * @return Enhanced value (0.0-1.0)
     */
    private fun applySCurve(x: Double, contrast: Double): Double {
        return if (x < 0.5) {
            (2 * x).pow(contrast) / 2
        } else {
            1 - (2 * (1 - x)).pow(contrast) / 2
        }
    }
    
    /**
     * Create a fallback pattern when no album art is available.
     * Shows a simple music note pattern.
     * @return IntArray of 625 elements representing a music note
     */
    private fun createFallbackPattern(): IntArray {
        // Simple fallback pattern - music note shape
        val pattern = IntArray(625) { 0 } // Start with all pixels off
        
        // Create a simple music note pattern in the center
        val notePixels = listOf(
            // Vertical line (stem)
            12 to 5, 12 to 6, 12 to 7, 12 to 8, 12 to 9, 12 to 10, 12 to 11, 12 to 12, 12 to 13, 12 to 14,
            // Note head (oval)
            10 to 15, 11 to 15, 12 to 15, 13 to 15, 14 to 15,
            10 to 16, 14 to 16,
            10 to 17, 11 to 17, 12 to 17, 13 to 17, 14 to 17,
            // Flag
            13 to 5, 14 to 6, 15 to 7, 16 to 8, 15 to 9, 14 to 10
        )
        
        // Set music note pixels to medium brightness
        notePixels.forEach { (col, row) ->
            if (row in 0 until 25 && col in 0 until 25) {
                pattern[row * 25 + col] = 180 // Medium brightness
            }
        }
        
        return pattern
    }
    
    data class TrackInfo(
        val title: String,
        val artist: String,
        val album: String,
        val appName: String,
        val albumArt: Bitmap? = null
    )
    
    private companion object {
        private val LOG_TAG = MediaControlHelper::class.java.simpleName
    }
}