package com.pauwma.glyphbeat.animation.styles

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import com.pauwma.glyphbeat.GlyphMatrixRenderer
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.ui.settings.*

/**
 * CoverArtTheme - A dynamic theme that displays the album cover of currently playing media.
 * 
 * This theme shows album artwork from the currently playing track on the Glyph Matrix,
 * converted to a 25x25 grayscale representation. When media is paused, the same image
 * is shown at 50% opacity as requested. Falls back to a music note pattern when no
 * album art is available.
 * 
 * Features:
 * - Dynamic content based on current track's album art
 * - Real-time updates when track changes
 * - Optional smooth rotation animation (12 frames, 30° per frame)
 * - Configurable rotation speed (50ms - 1000ms per frame)
 * - 50% opacity reduction for paused state
 * - Fallback music note pattern when no art is available
 * - Efficient bitmap processing and caching
 * - Customizable brightness, contrast, and opacity settings
 */
class CoverArtTheme(private val context: Context) : ThemeTemplate(), ThemeSettingsProvider {
    
    // Settings-driven properties with default values
    private var coverBrightness: Float = 1.0f
    private var enhanceContrast: Boolean = true
    private var pausedOpacity: Float = 0.4f
    
    // Rotation settings
    private var enableRotation: Boolean = false
    private var rotationSpeed: Long = 150L // milliseconds per frame
    private var rotationFrameCount: Int = 30 // Number of frames for 360° rotation (default 24 = 15° per frame)
    private var currentRotationAngle: Float = 0f
    
    // Smooth rotation tracking for pause/resume
    private var rotationStartTime: Long = 0L // When rotation started (for time-based calculation)
    private var savedRotationPosition: Float = 0f // Saved angle when paused
    private var isRotationPaused: Boolean = false // Track pause state
    private var lastFrameTime: Long = 0L // Last time frame was calculated
    
    // =================================================================================
    // THEME METADATA
    // =================================================================================
    
    override val titleTheme: String = "Cover Art"
    
    override val descriptionTheme: String = "Try to guess what cover thats suppose to be!"
    
    override val authorName: String = "pauwma"
    
    override val version: String = "1.0.0"
    
    override val category: String = "Media"
    
    override val tags: Array<String> = arrayOf("album", "cover", "art", "media", "dynamic", "music")
    
    override val createdDate: Long = System.currentTimeMillis()
    
    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================
    
    // Dynamic animation speed based on rotation settings
    override val animationSpeedValue: Long
        get() = if (enableRotation) rotationSpeed else 1000L
    
    // Frame durations for rotation animation - each frame shows for rotation speed duration
    override val frameDurations: LongArray?
        get() = if (enableRotation) {
            // Dynamic frame count for configurable rotation smoothness
            LongArray(rotationFrameCount) { rotationSpeed }
        } else null
    
    // No frame transitions needed
    override val frameTransitions: List<FrameTransition>? = null
    
    override val brightnessValue: Int = 255
    
    override val loopMode: String = "normal"
    
    override val complexity: String = "Simple"
    
    // =================================================================================
    // BEHAVIOR SETTINGS
    // =================================================================================
    
    override val isReactive: Boolean = true // Reacts to media changes
    
    override val supportsFadeTransitions: Boolean = true
    
    // =================================================================================
    // TECHNICAL METADATA
    // =================================================================================
    
    override val compatibilityVersion: String = "1.0.0"
    
    override val frameDataFormat: String = "flat"
    
    // =================================================================================
    // PREVIEW FRAME - Static circle pattern for theme selection
    // =================================================================================
    
    /**
     * Preview frame shown in theme selection, using a simple circular pattern
     * like other themes instead of dynamic album art content.
     */
    override val previewFrame: IntArray by lazy {
        val frame = IntArray(625) { 0 } // 25x25 flat array
        val centerX = 12.0
        val centerY = 12.0

        // Create concentric circles only within the circular display area
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Only draw within the circular display area
                if (distance <= 12.5) {
                    when {
                        distance <= 2.0 -> frame[flatIndex] = 255  // Center bright
                        distance <= 4.0 -> frame[flatIndex] = 0    // Inner ring dark
                        distance <= 6.0 -> frame[flatIndex] = 200  // Middle ring
                        distance <= 8.0 -> frame[flatIndex] = 0    // Gap
                        distance <= 10.0 -> frame[flatIndex] = 150 // Outer ring
                        else -> frame[flatIndex] = 0               // Edge fade
                    }
                }
            }
        }

        frame
    }

    // =================================================================================
    // MEDIA HELPER AND CACHING
    // =================================================================================
    
    private val mediaHelper: MediaControlHelper by lazy { 
        MediaControlHelper(context).also { helper ->
            // Load saved rotation state first
            loadRotationState()
            
            // Register for state changes to handle pause/resume
            helper.registerStateChangeCallback(object : MediaControlHelper.StateChangeCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveMedia: Boolean) {
                    handlePlaybackStateChange(isPlaying, hasActiveMedia)
                }
            })
            
            // Check initial media state and adjust rotation state if needed
            val trackInfo = helper.getTrackInfo()
            val initialIsPlaying = helper.isPlaying()
            val initialHasMedia = trackInfo != null
            
            // If media is currently paused and we have saved paused state, ensure we're in the right state
            if (!initialIsPlaying && initialHasMedia && enableRotation) {
                if (!isRotationPaused) {
                    // Media is paused but we weren't in paused rotation state, fix this
                    isRotationPaused = true
                    Log.d(LOG_TAG, "Initial state: Media paused, setting rotation to paused state at ${savedRotationPosition}°")
                }
            } else if (initialIsPlaying && initialHasMedia && enableRotation && isRotationPaused) {
                // Media is playing but we were in paused state, resume rotation
                resumeRotation()
            }
        }
    }
    
    // Cache for album art to avoid repeated processing
    private var cachedTrackTitle: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedFrameData: IntArray? = null
    private var cachedPausedFrameData: IntArray? = null
    
    // Cache for rotation frames
    private var cachedRotationFrames: Array<IntArray>? = null
    private var cachedRotationTrackTitle: String? = null
    
    // SharedPreferences for rotation state persistence
    private val rotationPrefs: SharedPreferences by lazy { 
        context.getSharedPreferences("cover_art_rotation_state", Context.MODE_PRIVATE)
    }
    
    // =================================================================================
    // DYNAMIC FRAME GENERATION
    // =================================================================================
    
    /**
     * Generate frames dynamically based on current media.
     * This overrides the static frames approach to provide dynamic content.
     * When rotation is enabled, uses time-based rotation for smooth pause/resume.
     */
    override val frames: Array<IntArray>
        get() = if (enableRotation) {
            // Use time-based rotation angle for current frame
            val currentAngle = calculateCurrentRotationAngle()
            arrayOf(getCurrentAlbumArtFrame(currentAngle))
        } else {
            arrayOf(getCurrentAlbumArtFrame())
        }
    
    /**
     * Calculate current rotation angle based on elapsed time for smooth rotation.
     * This enables smooth pause/resume functionality.
     */
    private fun calculateCurrentRotationAngle(): Float {
        if (!enableRotation) return 0f
        
        val currentTime = System.currentTimeMillis()
        
        return if (isRotationPaused) {
            // Return saved position when paused
            savedRotationPosition
        } else {
            // Calculate angle based on elapsed time since start/resume
            if (rotationStartTime == 0L) {
                rotationStartTime = currentTime
                savedRotationPosition
            } else {
                val elapsedTime = currentTime - rotationStartTime
                val rotationsPerSecond = 1000f / (rotationSpeed * rotationFrameCount) // Full rotations per second
                val anglePerMillisecond = 360f * rotationsPerSecond / 1000f
                val calculatedAngle = (savedRotationPosition + (elapsedTime * anglePerMillisecond)) % 360f
                
                Log.v(LOG_TAG, "Rotation angle: ${calculatedAngle}° (elapsed: ${elapsedTime}ms)")
                calculatedAngle
            }
        }
    }
    
    /**
     * Pause rotation at current position for smooth resume.
     */
    fun pauseRotation() {
        if (enableRotation && !isRotationPaused) {
            savedRotationPosition = calculateCurrentRotationAngle()
            isRotationPaused = true
            saveRotationState() // Persist the paused state
            Log.d(LOG_TAG, "Rotation paused at ${savedRotationPosition}°")
        }
    }
    
    /**
     * Resume rotation from saved position.
     */
    fun resumeRotation() {
        if (enableRotation && isRotationPaused) {
            rotationStartTime = System.currentTimeMillis()
            isRotationPaused = false
            Log.d(LOG_TAG, "Rotation resumed from ${savedRotationPosition}°")
        }
    }
    
    /**
     * Start rotation from beginning (called when rotation is first enabled).
     */
    fun startRotation() {
        if (enableRotation && !isRotationPaused) {
            rotationStartTime = System.currentTimeMillis()
            savedRotationPosition = 0f
            // Clear saved state when starting fresh rotation
            rotationPrefs.edit().clear().apply()
            Log.d(LOG_TAG, "Rotation started - cleared saved state")
        }
    }
    
    /**
     * Handle playback state changes for smooth pause/resume.
     */
    private fun handlePlaybackStateChange(isPlaying: Boolean, hasActiveMedia: Boolean) {
        if (!enableRotation) return
        
        if (isPlaying && hasActiveMedia) {
            // Media is playing - resume or start rotation
            if (isRotationPaused) {
                resumeRotation()
            } else if (rotationStartTime == 0L) {
                startRotation()
            }
        } else {
            // Media is paused or stopped - pause rotation
            pauseRotation()
        }
    }
    
    /**
     * Generate cached rotation frames for the current track.
     */
    private fun generateRotationFrames(): Array<IntArray> {
        val trackInfo = mediaHelper.getTrackInfo()
        val currentTrackTitle = trackInfo?.title
        
        // Check if we can use cached rotation frames
        if (cachedRotationFrames != null && 
            cachedRotationTrackTitle == currentTrackTitle && 
            currentTrackTitle != null) {
            return cachedRotationFrames!!
        }
        
        // Generate new rotation frames
        val angleStep = 360f / rotationFrameCount // Calculate degrees per frame
        Log.v(LOG_TAG, "Generating $rotationFrameCount rotation frames (${angleStep}° per frame) for track: $currentTrackTitle")
        
        val rotationFrames = Array(rotationFrameCount) { frameIndex ->
            val rotationAngle = frameIndex * angleStep // Dynamic angle calculation
            getCurrentAlbumArtFrame(rotationAngle)
        }
        
        // Cache the frames
        cachedRotationFrames = rotationFrames
        cachedRotationTrackTitle = currentTrackTitle
        Log.v(LOG_TAG, "Cached $rotationFrameCount rotation frames for: $currentTrackTitle")
        
        return rotationFrames
    }
    
    /**
     * Get current album art as a matrix frame, with caching for performance.
     * @param rotationAngle Optional rotation angle in degrees (default 0f for no rotation)
     */
    private fun getCurrentAlbumArtFrame(rotationAngle: Float = 0f): IntArray {
        return try {
            val trackInfo = mediaHelper.getTrackInfo()
            
            // Check cache first to avoid repeated processing (only for non-rotating frames)
            if (!enableRotation && rotationAngle == 0f && 
                trackInfo?.title == cachedTrackTitle && cachedFrameData != null) {
                return cachedFrameData!!
            }
            
            // Process new album art with settings applied
            val frameData = if (trackInfo?.albumArt != null) {
                Log.v(LOG_TAG, "Converting album art for track: ${trackInfo.title} (rotation: ${rotationAngle}°)")
                
                // Apply rotation, brightness and contrast settings
                mediaHelper.bitmapToMatrixArray(
                    bitmap = trackInfo.albumArt, 
                    brightnessMultiplier = coverBrightness.toDouble(), 
                    enhanceContrast = enhanceContrast,
                    rotationAngle = rotationAngle
                )
            } else {
                Log.v(LOG_TAG, "No album art available, using fallback pattern")
                mediaHelper.bitmapToMatrixArray(null, coverBrightness.toDouble(), false) // Fallback with brightness
            }
            
            // Update cache only for non-rotating frames
            if (!enableRotation && rotationAngle == 0f) {
                cachedTrackTitle = trackInfo?.title
                cachedAlbumArt = trackInfo?.albumArt
                cachedFrameData = frameData
                
                // Generate paused frame with configurable opacity and cache it
                cachedPausedFrameData = frameData.map { (it * pausedOpacity).toInt().coerceIn(0, 255) }.toIntArray()
            }
            
            frameData
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error generating album art frame: ${e.message}")
            // Return fallback pattern on error
            mediaHelper.bitmapToMatrixArray(null, 1.0, false)
        }
    }
    
    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================
    
    /**
     * Paused frame shows the same album art but at current rotation position with reduced opacity.
     */
    override val pausedFrame: IntArray
        get() {
            // Get current frame at the paused rotation position
            val currentFrame = if (enableRotation) {
                // Use the current rotation angle for smooth pause
                val currentAngle = calculateCurrentRotationAngle()
                getCurrentAlbumArtFrame(currentAngle)
            } else {
                getCurrentAlbumArtFrame()
            }
            
            // Apply paused opacity
            return currentFrame.map { (it * pausedOpacity).toInt().coerceIn(0, 255) }.toIntArray()
        }
    
    /**
     * Offline frame shows clean minimal pattern like MinimalTheme with circular masking.
     */
    override val offlineFrame: IntArray by lazy {
        // Start with the exact MinimalTheme offline frame pattern (shaped format)
        val minimalOfflineFrameShaped = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
        
        // Convert shaped data to flat 25x25 array with circular masking (same as MinimalTheme.generateFrame)
        val flatArray = IntArray(625) { 0 }
        val centerX = 12.0
        val centerY = 12.0
        var shapedIndex = 0

        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Check if this pixel is within the circular matrix shape
                if (distance <= 12.5 && shapedIndex < minimalOfflineFrameShaped.size) {
                    flatArray[flatIndex] = minimalOfflineFrameShaped[shapedIndex]
                    shapedIndex++
                }
            }
        }

        flatArray
    }

    /**
     * Loading frame shows medium brightness music note pattern with circular masking.
     */
    override val loadingFrame: IntArray by lazy {
        val frame = IntArray(625) { 0 } // 25x25 flat array
        val centerX = 12.0
        val centerY = 12.0

        // Create loading pattern with circular masking - dimmer than offline
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Only draw within the circular display area
                if (distance <= 12.5) {
                    // Same music note pattern but dimmer (128 instead of 255)
                    when {
                        // Top horizontal bar
                        row == 7 && col in 12..18 -> frame[flatIndex] = 128
                        // Vertical bars
                        col == 12 && row in 8..14 -> frame[flatIndex] = 128
                        col == 18 && row in 8..14 -> frame[flatIndex] = 128
                        // Bottom connecting elements
                        row == 15 && col in 11..13 -> frame[flatIndex] = 128
                        row == 16 && col in 10..14 -> frame[flatIndex] = 128
                        row == 15 && col in 17..19 -> frame[flatIndex] = 128
                        row == 16 && col in 16..20 -> frame[flatIndex] = 128
                        row == 17 && col in 11..13 -> frame[flatIndex] = 128
                        row == 17 && col in 17..19 -> frame[flatIndex] = 128
                    }
                }
            }
        }

        frame
    }

    /**
     * Error frame shows very dim pattern with circular masking.
     */
    override val errorFrame: IntArray by lazy {
        val frame = IntArray(625) { 0 } // 25x25 flat array
        val centerX = 12.0
        val centerY = 12.0

        // Create error pattern with circular masking - very dim
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Only draw within the circular display area
                if (distance <= 12.5) {
                    // Same music note pattern but very dim (64 instead of 255)
                    when {
                        // Top horizontal bar
                        row == 7 && col in 12..18 -> frame[flatIndex] = 64
                        // Vertical bars
                        col == 12 && row in 8..14 -> frame[flatIndex] = 64
                        col == 18 && row in 8..14 -> frame[flatIndex] = 64
                        // Bottom connecting elements
                        row == 15 && col in 11..13 -> frame[flatIndex] = 64
                        row == 16 && col in 10..14 -> frame[flatIndex] = 64
                        row == 15 && col in 17..19 -> frame[flatIndex] = 64
                        row == 16 && col in 16..20 -> frame[flatIndex] = 64
                        row == 17 && col in 11..13 -> frame[flatIndex] = 64
                        row == 17 && col in 17..19 -> frame[flatIndex] = 64
                    }
                }
            }
        }

        frame
    }

    // =================================================================================
    // OVERRIDDEN METHODS
    // =================================================================================
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)

        // Check if we have active media first
        return try {
            val trackInfo = mediaHelper.getTrackInfo()
            if (trackInfo?.albumArt != null || trackInfo?.title != null) {
                // We have active media, use time-based rotation when enabled
                if (enableRotation) {
                    // Use real-time rotation angle for smooth pause/resume
                    val currentAngle = calculateCurrentRotationAngle()
                    getCurrentAlbumArtFrame(currentAngle)
                } else {
                    getCurrentAlbumArtFrame()
                }
            } else {
                // No active media, return the hardcoded offline frame for consistency
                offlineFrame
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error checking media state in generateFrame: ${e.message}")
            // On error, return offline frame for safety
            offlineFrame
        }
    }
    
    override fun getFrameCount(): Int = if (enableRotation) rotationFrameCount else 1
    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = animationSpeedValue
    override fun getBrightness(): Int = brightnessValue
    override fun getDescription(): String = descriptionTheme
    
    /**
     * Clear cache when track changes to ensure fresh content.
     * This method can be called externally to force refresh.
     */
    fun clearCache() {
        cachedTrackTitle = null
        cachedAlbumArt = null
        cachedFrameData = null
        cachedPausedFrameData = null
        cachedRotationFrames = null
        cachedRotationTrackTitle = null
        Log.v(LOG_TAG, "Album art cache cleared")
    }
    
    /**
     * Save current rotation state to SharedPreferences for persistence across service restarts.
     */
    private fun saveRotationState() {
        try {
            val currentAngle = if (isRotationPaused) savedRotationPosition else calculateCurrentRotationAngle()
            
            rotationPrefs.edit().apply {
                putFloat(KEY_SAVED_ROTATION_POSITION, currentAngle)
                putBoolean(KEY_IS_ROTATION_PAUSED, isRotationPaused)
                putLong(KEY_ROTATION_START_TIME, if (isRotationPaused) 0L else rotationStartTime)
                apply()
            }
            
            Log.v(LOG_TAG, "Rotation state saved: angle=${currentAngle}°, paused=$isRotationPaused")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to save rotation state: ${e.message}")
        }
    }
    
    /**
     * Load saved rotation state from SharedPreferences to restore state across service restarts.
     */
    private fun loadRotationState() {
        try {
            if (rotationPrefs.contains(KEY_SAVED_ROTATION_POSITION)) {
                savedRotationPosition = rotationPrefs.getFloat(KEY_SAVED_ROTATION_POSITION, 0f)
                isRotationPaused = rotationPrefs.getBoolean(KEY_IS_ROTATION_PAUSED, false)
                rotationStartTime = rotationPrefs.getLong(KEY_ROTATION_START_TIME, 0L)
                
                // If we're resuming from a non-paused state, reset start time to current time
                if (!isRotationPaused && rotationStartTime > 0L) {
                    rotationStartTime = System.currentTimeMillis()
                }
                
                Log.d(LOG_TAG, "Rotation state loaded: angle=${savedRotationPosition}°, paused=$isRotationPaused")
            } else {
                Log.v(LOG_TAG, "No saved rotation state found, using defaults")
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to load rotation state: ${e.message}")
            // Reset to defaults on error
            savedRotationPosition = 0f
            isRotationPaused = false
            rotationStartTime = 0L
        }
    }
    
    /**
     * Cleanup resources and callbacks.
     */
    fun cleanup() {
        try {
            // Save rotation state before cleanup
            saveRotationState()
            mediaHelper.cleanup()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error during cleanup: ${e.message}")
        }
    }
    
    // =================================================================================
    // THEME SETTINGS PROVIDER IMPLEMENTATION
    // =================================================================================
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "cover_brightness",
                displayName = "Cover Brightness",
                description = "Brightness multiplier for album art",
                defaultValue = 1.0f,
                minValue = 0.2f,
                maxValue = 1.0f,
                stepSize = 0.1f,
                unit = "x",
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "enhance_contrast",
                displayName = "Enhance Contrast",
                description = "Apply contrast enhancement to improve visibility",
                defaultValue = true,
                category = SettingCategories.EFFECTS
            )
            .addToggleSetting(
                id = "enable_rotation",
                displayName = "Enable Rotation",
                description = "Rotate the album art continuously",
                defaultValue = false,
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "rotation_speed",
                displayName = "Rotation Speed",
                description = "How fast the cover art rotates",
                defaultValue = 150L,
                minValue = 50L,
                maxValue = 1000L,
                stepSize = 50L,
                unit = "ms",
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "rotation_smoothness",
                displayName = "Rotation Smoothness",
                description = "Number of steps per rotation (more = smoother)",
                defaultValue = 30,
                minValue = 12,
                maxValue = 36,
                stepSize = 6,
                unit = "steps",
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "paused_opacity",
                displayName = "Paused Opacity",
                description = "Opacity when media is paused",
                defaultValue = 0.4f,
                minValue = 0.2f,
                maxValue = 0.8f,
                stepSize = 0.1f,
                unit = null,
                category = SettingCategories.VISUAL
            )
            .build()
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Apply brightness
        coverBrightness = settings.getSliderValueFloat("cover_brightness", 1.0f)
            .coerceIn(0.2f, 1.0f)
        
        // Apply contrast enhancement
        enhanceContrast = settings.getToggleValue("enhance_contrast", true)
        
        // Apply paused opacity
        pausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.4f)
            .coerceIn(0.2f, 0.8f)
        
        // Store previous rotation setting to detect changes
        val wasRotationEnabled = enableRotation
        
        // Apply rotation settings
        enableRotation = settings.getToggleValue("enable_rotation", false)
        rotationSpeed = settings.getSliderValueLong("rotation_speed", 150L)
            .coerceIn(50L, 1000L)
        rotationFrameCount = settings.getSliderValueInt("rotation_smoothness", 30)
            .coerceIn(12, 36)
        
        // Only reset rotation state if rotation was just enabled/disabled, not for other setting changes
        if (wasRotationEnabled != enableRotation) {
            if (enableRotation) {
                // Rotation was just enabled - load saved state
                loadRotationState()
                Log.d(LOG_TAG, "Rotation enabled - loaded saved state")
            } else {
                // Rotation was just disabled - save current state and reset
                saveRotationState()
                currentRotationAngle = 0f
                rotationStartTime = 0L
                savedRotationPosition = 0f
                isRotationPaused = false
                Log.d(LOG_TAG, "Rotation disabled - saved state and reset")
            }
        }
        
        // Clear cache to force refresh with new settings
        clearCache()
    }
    
    private companion object {
        private val LOG_TAG = CoverArtTheme::class.java.simpleName
        
        // SharedPreferences keys for rotation state persistence
        private const val KEY_SAVED_ROTATION_POSITION = "saved_rotation_position"
        private const val KEY_IS_ROTATION_PAUSED = "is_rotation_paused"
        private const val KEY_ROTATION_START_TIME = "rotation_start_time"
    }
}