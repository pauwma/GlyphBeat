package com.pauwma.glyphbeat.themes.animation

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransition
import com.pauwma.glyphbeat.themes.animation.preview.CoverArtPreviewManager
import com.pauwma.glyphbeat.themes.animation.preview.CoverArtPreviewRenderer
import com.pauwma.glyphbeat.ui.settings.*
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueFloat
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueInt
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueLong
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getToggleValue

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
class CoverArtTheme(private val ctx: Context) : ThemeTemplate(), ThemeSettingsProvider {

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

    // =================================================================================
    // THEME METADATA
    // =================================================================================

    override val titleTheme: String = ctx.getString(R.string.theme_cover_art_title)

    override val descriptionTheme: String = ctx.getString(R.string.theme_cover_art_desc)

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

    // Dynamic brightness based on coverBrightness setting
    override val brightnessValue: Int
        get() = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.multiplierToBrightness(coverBrightness)

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
     * Preview frame shown in theme selection, using the offline frame pattern
     * for consistency with the actual theme behavior when no media is available.
     */
    override val previewFrame: IntArray by lazy {
        // Use the offline frame for preview to show the same pattern users will see
        offlineFrame
    }

    // =================================================================================
    // MEDIA HELPER AND CACHING
    // =================================================================================

    private val mediaHelper: MediaControlHelper by lazy {
        MediaControlHelper(ctx).also { helper ->
            // Load saved rotation state first
            loadRotationState()

            // Register for state changes to handle pause/resume
            helper.registerStateChangeCallback(object : MediaControlHelper.StateChangeCallback {
                override fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveMedia: Boolean) {
                    handlePlaybackStateChange(isPlaying, hasActiveMedia)
                }

                override fun onActiveAppChanged(packageName: String?, appName: String?) {
                    // Track info will update through onPlaybackStateChanged when media changes
                    Log.v(LOG_TAG, "Active app changed to: $appName ($packageName)")
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
        ctx.getSharedPreferences("cover_art_rotation_state", Context.MODE_PRIVATE)
    }

    // =================================================================================
    // DYNAMIC FRAME GENERATION
    // =================================================================================

    /**
     * Generate frames dynamically based on current media.
     * This overrides the static frames approach to provide dynamic content.
     * When rotation is enabled, generates all rotation frames.
     */
    override val frames: Array<IntArray>
        get() = if (enableRotation) {
            // Generate all rotation frames for smooth animation
            Array(rotationFrameCount) { frameIndex ->
                val angle = (360f / rotationFrameCount) * frameIndex
                getCurrentAlbumArtFrame(angle)
            }
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

                // Log.v(LOG_TAG, "Rotation angle: ${calculatedAngle}° (elapsed: ${elapsedTime}ms)")
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
                // Log.v(LOG_TAG, "Converting album art for track: ${trackInfo.title} (rotation: ${rotationAngle}°)")

                // Apply rotation and contrast settings (brightness handled by unified model)
                mediaHelper.bitmapToMatrixArray(
                    bitmap = trackInfo.albumArt,
                    brightnessMultiplier = coverBrightness,
                    enhanceContrast = enhanceContrast,
                    rotationAngle = rotationAngle
                )
            } else {
                Log.v(LOG_TAG, "No album art available, using fallback pattern")
                mediaHelper.bitmapToMatrixArray(null, coverBrightness, false) // Fallback (brightness handled by unified model)
            }

            // Update cache only for non-rotating frames
            if (!enableRotation && rotationAngle == 0f) {
                cachedTrackTitle = trackInfo?.title
                cachedAlbumArt = trackInfo?.albumArt
                cachedFrameData = frameData

                // Cache paused frame data (opacity will be handled by unified model)
                cachedPausedFrameData = frameData
            }

            frameData
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error generating album art frame: ${e.message}")
            // Return fallback pattern on error
            mediaHelper.bitmapToMatrixArray(null, 1f, false)
        }
    }

    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================

    /**
     * Paused frame shows the same album art but at current rotation position with reduced opacity.
     * The opacity reduction is handled by returning a dimmer frame that will be further
     * processed by the unified brightness model.
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

            // Apply paused opacity to the raw pixel values
            // The unified brightness model will then apply theme brightness on top
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
            if (trackInfo?.albumArt != null) {
                // We have album art, use time-based rotation when enabled
                if (enableRotation) {
                    // Use real-time rotation angle for smooth pause/resume
                    val currentAngle = calculateCurrentRotationAngle()
                    getCurrentAlbumArtFrame(currentAngle)
                } else {
                    getCurrentAlbumArtFrame()
                }
            } else {
                // No album art available, return the static offline frame
                // NEVER rotate the offline frame, even if rotation is enabled
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
            // Cleanup preview manager if it exists
            previewManager?.cleanup()
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error during cleanup: ${e.message}")
        }
    }

    // =================================================================================
    // PREVIEW-SPECIFIC METHODS
    // =================================================================================

    // Lazy initialization of preview components
    private var previewManager: CoverArtPreviewManager? = null
    private val previewRenderer: CoverArtPreviewRenderer by lazy {
        CoverArtPreviewRenderer()
    }

    /**
     * Get a preview frame with current settings applied.
     * This method is optimized for preview display and returns a frame
     * suitable for the preview UI with all settings properly applied.
     *
     * @param settings Optional theme settings to apply
     * @return Preview frame data as IntArray
     */
    fun getPreviewFrame(settings: ThemeSettings? = null): IntArray {
        // Initialize preview manager if needed
        if (previewManager == null) {
            previewManager = CoverArtPreviewManager(ctx)
        }

        return previewManager?.generatePreviewFrame(settings ?: getSettingsSchema())
            ?: previewFrame
    }

    /**
     * Get a preview frame for specific media with settings.
     * Useful for showing preview of how specific album art would look.
     *
     * @param trackInfo Track information with album art
     * @param settings Theme settings to apply
     * @return Preview frame data as IntArray
     */
    fun getPreviewFrameForMedia(
        trackInfo: MediaControlHelper.TrackInfo?,
        settings: ThemeSettings? = null
    ): IntArray {
        if (trackInfo?.albumArt == null) {
            return previewRenderer.generateFallbackPreview(settings)
        }

        return try {
            val processedBitmap = previewRenderer.processAlbumArtForPreview(
                albumArt = trackInfo.albumArt,
                targetSize = 25,
                settings = settings
            )

            val rotation = if (settings?.getToggleValue("enable_rotation", false) == true) {
                currentRotationAngle
            } else {
                0f
            }

            previewRenderer.bitmapToPreviewFrame(
                bitmap = processedBitmap,
                rotation = rotation,
                settings = settings
            )
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error generating preview frame for media: ${e.message}")
            previewRenderer.generateFallbackPreview(settings)
        }
    }

    /**
     * Check if the preview needs to be updated.
     * Returns true if the preview should be refreshed due to media changes
     * or settings updates.
     *
     * @return True if preview needs update, false otherwise
     */
    fun shouldUpdatePreview(): Boolean {
        return previewManager?.shouldUpdatePreview() ?: false
    }

    /**
     * Get the preview manager instance for advanced preview control.
     * Creates the manager if it doesn't exist.
     *
     * @return CoverArtPreviewManager instance
     */
    fun getPreviewManager(): CoverArtPreviewManager {
        if (previewManager == null) {
            previewManager = CoverArtPreviewManager(ctx)
        }
        return previewManager!!
    }

    /**
     * Check if this theme should use enhanced preview.
     * CoverArtTheme always uses enhanced preview for better accuracy.
     *
     * @return True to use enhanced preview
     */
    fun useEnhancedPreview(): Boolean = true

    // =================================================================================
    // THEME SETTINGS PROVIDER IMPLEMENTATION
    // =================================================================================

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "cover_brightness",
                displayName = ctx.getString(R.string.set_cover_brightness_title),
                description = ctx.getString(R.string.set_cover_brightness_desc),
                defaultValue = 1.0f,
                minValue = 0.1f,
                maxValue = 1.0f,
                stepSize = 0.1f,
                unit = null,
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "enhance_contrast",
                displayName = ctx.getString(R.string.set_cover_contrast_title),
                description = ctx.getString(R.string.set_cover_contrast_desc),
                defaultValue = true,
                category = SettingCategories.EFFECTS
            )
            .addToggleSetting(
                id = "enable_rotation",
                displayName = ctx.getString(R.string.set_cover_rotation_title),
                description = ctx.getString(R.string.set_cover_rotation_desc),
                defaultValue = false,
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "rotation_speed",
                displayName = ctx.getString(R.string.set_rotation_speed_title),
                description = ctx.getString(R.string.set_cover_rotation_speed_desc),
                defaultValue = 150L,
                minValue = 50L,
                maxValue = 1000L,
                stepSize = 50L,
                unit = "ms",
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "rotation_smoothness",
                displayName = ctx.getString(R.string.set_cover_rotation_smooth_title),
                description = ctx.getString(R.string.set_cover_rotation_smooth_desc),
                defaultValue = 30,
                minValue = 12,
                maxValue = 36,
                stepSize = 6,
                unit = "steps",
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "paused_opacity",
                displayName = ctx.getString(R.string.set_paused_opacity_title),
                description = ctx.getString(R.string.set_paused_opacity_desc),
                defaultValue = 0.4f,
                minValue = 0.1f,
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