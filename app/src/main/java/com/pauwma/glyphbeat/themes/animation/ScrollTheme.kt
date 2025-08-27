package com.pauwma.glyphbeat.themes.animation

import android.content.Context
import android.util.Log
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.ui.settings.*
import com.pauwma.glyphbeat.utils.TextRenderer
import com.pauwma.glyphbeat.utils.PixelFont
import com.pauwma.glyphbeat.sound.MediaControlHelper

/**
 * Text scrolling theme that displays song metadata with smooth horizontal scrolling.
 * Features:
 * - Smooth pixel-by-pixel text scrolling
 * - Media metadata display (title, artist, album)
 * - Pause/resume with position persistence
 * - Comprehensive customization settings
 * - Optimized performance with text caching
 */
class ScrollTheme(private val context: Context) : ThemeTemplate(), ThemeSettingsProvider {

    companion object {
        private const val LOG_TAG = "ScrollTheme"
        private const val DEFAULT_TEXT = "GlyphBeat Music Player"
        private const val OFFLINE_TEXT = "Nothing Playing"
        private const val SEPARATOR = " - "
        private const val TEXT_REPEAT_GAP = 20 // Pixels between text repeats
    }

    // Media helper for getting track info
    private val mediaHelper: MediaControlHelper by lazy {
        MediaControlHelper(context)
    }

    // Scroll state
    private var scrollPosition = 0
    private var pausedScrollPosition = 0
    private var currentText = DEFAULT_TEXT
    private var lastTrackInfo: MediaControlHelper.TrackInfo? = null
    
    // Offline scroll state
    private var offlineScrollPosition = 0
    private var lastOfflineFrameTime = System.currentTimeMillis()
    private var accumulatedOfflineScrollTime = 0f
    
    // Time-based scrolling to prevent multiple callers from affecting speed
    private var lastFrameTime = System.currentTimeMillis()
    private var accumulatedScrollTime = 0f // For sub-pixel precision

    // Pause state tracking
    private var isPaused = false
    private var currentPauseMode = "slow_motion" // "freeze" or "slow_motion"

    // Settings-driven properties
    private var currentBrightness = 1.0f
    private var currentPausedOpacity = 0.5f
    private var currentScrollSpeed = 6 // 1-10 setting, 6 is medium speed
    private var currentShowArtist = true
    private var currentShowAlbum = true
    private var currentTextSpacing = 1

    // Cache management
    private var needsTextUpdate = true
    private var lastUpdateTime = 0L
    private val updateInterval = 1000L // Check for track changes every second

    // =================================================================================
    // THEME METADATA
    // =================================================================================

    override val titleTheme: String = "Scroll Text"
    override val descriptionTheme: String = "Title, artist, album all flexin’ on a nonstop scroll"
    override val authorName: String = "GlyphBeat Team"
    override val version: String = "1.0.0"
    override val category: String = "Information"
    override val tags: Array<String> = arrayOf("text", "scroll", "metadata", "information", "title", "artist")
    override val createdDate: Long = System.currentTimeMillis()

    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================

    override val animationSpeedValue: Long = 50L // Slower frame rate for consistent scrolling
    override val brightnessValue: Int = 255
    override val loopMode: String = "normal"
    override val complexity: String = "Medium"

    // Dynamic frame generation - no predefined frames
    override val frames = arrayOf(generateDefaultFrame())
    override val frameDurations: LongArray? = null

    // =================================================================================
    // BEHAVIOR SETTINGS
    // =================================================================================

    override val isReactive: Boolean = false
    override val supportsFadeTransitions: Boolean = true

    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================

    // Offline frame shows scrolling "Nothing Playing" text with reduced brightness
    override val offlineFrame: IntArray
        get() {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = (currentTime - lastOfflineFrameTime).toFloat()
            
            // Update offline scroll position with slow speed
            if (elapsedTime > 0) {
                // Use very slow scroll speed for offline text
                val offlinePixelsPerMs = 0.015f // Slower than the slowest normal speed
                accumulatedOfflineScrollTime += elapsedTime * offlinePixelsPerMs
                
                val pixelsToMove = accumulatedOfflineScrollTime.toInt()
                if (pixelsToMove > 0) {
                    offlineScrollPosition += pixelsToMove
                    accumulatedOfflineScrollTime -= pixelsToMove.toFloat()
                }
                
                lastOfflineFrameTime = currentTime
            }
            
            // Render "Nothing Playing" with reduced brightness
            return TextRenderer.renderTextToMatrix(
                OFFLINE_TEXT,
                scrollOffset = offlineScrollPosition,
                spacing = currentTextSpacing,
                brightness = currentBrightness * currentPausedOpacity // Use paused opacity for offline
            )
        }

    // Override pausedFrame property to generate the paused frame dynamically
    override val pausedFrame: IntArray
        get() {
            // Just return the current frame without modifying state
            // This prevents acceleration when paused
            return TextRenderer.renderTextToMatrix(
                currentText,
                scrollOffset = scrollPosition,  // Use current position
                spacing = currentTextSpacing,
                brightness = currentBrightness * currentPausedOpacity
            )
        }

    // =================================================================================
    // FRAME GENERATION
    // =================================================================================

    override fun getFrameCount(): Int = 100 // Virtual frame count for continuous scrolling

    override fun generateFrame(frameIndex: Int): IntArray {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - lastFrameTime).toFloat()
        
        // Sync pause state with actual media state
        val isMediaPlaying = try {
            mediaHelper.isPlaying()
        } catch (e: Exception) {
            false
        }

        // Only update state on actual transitions
        val wasPlaying = !isPaused
        val shouldBePaused = !isMediaPlaying

        if (wasPlaying && shouldBePaused) {
            // Transition from playing to paused
            isPaused = true
            pausedScrollPosition = scrollPosition
            // Reset timing when pausing
            lastFrameTime = currentTime
            accumulatedScrollTime = 0f
        } else if (!wasPlaying && !shouldBePaused) {
            // Transition from paused to playing
            isPaused = false
            if (currentPauseMode == "freeze") {
                scrollPosition = pausedScrollPosition
            }
            // Reset timing when resuming
            lastFrameTime = currentTime
            accumulatedScrollTime = 0f
        }

        // Update text if needed
        updateTextIfNeeded()

        // Update scroll speed if text changed
        if (needsTextUpdate) {
            updateScrollSpeed()
        }

        // Handle time-based scrolling (only advances based on elapsed time, not call frequency)
        if (!isPaused && elapsedTime > 0) {
            // Calculate pixels per millisecond based on current speed setting
            // More granular speed control with slower options
            val pixelsPerMs = when (currentScrollSpeed) {
                1 -> 0.005f  // Ultra slow
                2 -> 0.008f  // Very slow
                3 -> 0.012f  // Slow
                4 -> 0.016f  // Slow-medium
                5 -> 0.020f  // Medium-slow
                6 -> 0.025f  // Medium
                7 -> 0.030f  // Medium-fast
                8 -> 0.040f  // Fast
                9 -> 0.055f  // Very fast
                10 -> 0.075f // Ultra fast
                else -> 0.025f
            }
            
            // Accumulate scroll time
            accumulatedScrollTime += elapsedTime * pixelsPerMs
            
            // Move whole pixels only
            val pixelsToMove = accumulatedScrollTime.toInt()
            if (pixelsToMove > 0) {
                scrollPosition += pixelsToMove
                accumulatedScrollTime -= pixelsToMove.toFloat()
            }
            
            lastFrameTime = currentTime
        } else if (currentPauseMode == "slow_motion" && elapsedTime > 0) {
            // In slow motion mode, scroll very slowly when paused
            val slowPixelsPerMs = 0.01f // Very slow scrolling
            accumulatedScrollTime += elapsedTime * slowPixelsPerMs
            
            val pixelsToMove = accumulatedScrollTime.toInt()
            if (pixelsToMove > 0) {
                scrollPosition += pixelsToMove
                accumulatedScrollTime -= pixelsToMove.toFloat()
            }
            
            lastFrameTime = currentTime
        } else if (isPaused) {
            // Keep updating lastFrameTime when paused to prevent large jumps when resuming
            lastFrameTime = currentTime
        }

        // Calculate brightness based on pause state
        val effectiveBrightness = if (isPaused) {
            currentBrightness * currentPausedOpacity
        } else {
            currentBrightness
        }

        // Use direct rendering which works correctly with circular shape
        return TextRenderer.renderTextToMatrix(
            currentText,
            scrollOffset = scrollPosition,
            spacing = currentTextSpacing,
            brightness = effectiveBrightness
        )
    }

    /**
     * Called when media is paused.
     */
    fun onPause() {
        isPaused = true
        pausedScrollPosition = scrollPosition
        // Reset time tracking to prevent jumps
        lastFrameTime = System.currentTimeMillis()
        accumulatedScrollTime = 0f
    }

    /**
     * Resume scrolling from paused position.
     */
    fun resumeScrolling() {
        isPaused = false
        if (currentPauseMode == "freeze") {
            // In freeze mode, restore the exact position
            scrollPosition = pausedScrollPosition
        }
        // Reset time tracking to prevent jumps when resuming
        lastFrameTime = System.currentTimeMillis()
        accumulatedScrollTime = 0f
    }

    // =================================================================================
    // TEXT MANAGEMENT
    // =================================================================================

    /**
     * Update text content if track has changed.
     */
    private fun updateTextIfNeeded() {
        val currentTime = System.currentTimeMillis()

        // Throttle updates
        if (currentTime - lastUpdateTime < updateInterval) {
            return
        }

        lastUpdateTime = currentTime

        try {
            val trackInfo = mediaHelper.getTrackInfo()

            // Check if track has changed
            if (hasTrackChanged(trackInfo)) {
                lastTrackInfo = trackInfo

                // Format new text
                currentText = if (trackInfo != null) {
                    TextRenderer.formatMediaText(
                        title = trackInfo.title,
                        artist = trackInfo.artist,
                        album = trackInfo.album,
                        showArtist = currentShowArtist,
                        showAlbum = currentShowAlbum,
                        separator = SEPARATOR
                    )
                } else {
                    // Use OFFLINE_TEXT when no media is available
                    // This is handled separately in offlineFrame getter
                    DEFAULT_TEXT
                }

                needsTextUpdate = true
                Log.d(LOG_TAG, "Text updated: $currentText")
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error updating text: ${e.message}")
            currentText = DEFAULT_TEXT
            needsTextUpdate = true
        }
    }

    /**
     * Check if track info has changed.
     */
    private fun hasTrackChanged(newInfo: MediaControlHelper.TrackInfo?): Boolean {
        if (newInfo == null && lastTrackInfo == null) return false
        if (newInfo == null || lastTrackInfo == null) return true

        return newInfo.title != lastTrackInfo?.title ||
                newInfo.artist != lastTrackInfo?.artist ||
                newInfo.album != lastTrackInfo?.album
    }

    /**
     * Update scroll speed based on user setting.
     */
    private fun updateScrollSpeed() {
        needsTextUpdate = false
        
        // Reset time tracking when speed changes to apply new speed immediately
        lastFrameTime = System.currentTimeMillis()
        accumulatedScrollTime = 0f
        
        // Note: The actual speed is now handled in generateFrame() based on currentScrollSpeed
        // This method primarily resets the timing state
    }

    /**
     * Generate a default frame when no scrolling is needed.
     */
    private fun generateDefaultFrame(): IntArray {
        return TextRenderer.renderTextToMatrix(
            DEFAULT_TEXT,
            scrollOffset = 0,
            spacing = currentTextSpacing,
            brightness = currentBrightness
        )
    }

    // =================================================================================
    // SETTINGS IMPLEMENTATION
    // =================================================================================

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "scroll_speed",
                displayName = "Scroll Speed",
                description = "Text scrolling speed",
                defaultValue = 5,
                minValue = 1,
                maxValue = 10,
                stepSize = 1,
                category = SettingCategories.ANIMATION
            )
            .addDropdownSetting(
                id = "pause_mode",
                displayName = "Pause Behavior",
                description = "How text behaves when paused",
                defaultValue = "slow_motion",
                options = listOf(
                    DropdownOption("slow_motion", "Slow Motion", "Continue scrolling slowly"),
                    DropdownOption("freeze", "Freeze", "Stop scrolling completely")
                ),
                category = SettingCategories.ANIMATION
            )
            .addToggleSetting(
                id = "show_artist",
                displayName = "Show Artist",
                description = "Display artist name",
                defaultValue = true,
                category = SettingCategories.DESIGN
            )
            .addToggleSetting(
                id = "show_album",
                displayName = "Show Album",
                description = "Display album name",
                defaultValue = false,
                category = SettingCategories.DESIGN
            )
            .addSliderSetting(
                id = "text_spacing",
                displayName = "Character Spacing",
                description = "Space between characters",
                defaultValue = 2,
                minValue = 1,
                maxValue = 5,
                stepSize = 1,
                unit = "px",
                category = SettingCategories.DESIGN
            )
            .addSliderSetting(
                id = "brightness",
                displayName = "Brightness",
                description = "Text brightness level",
                defaultValue = 1.0f,
                minValue = 0.1f,
                maxValue = 1.0f,
                stepSize = 0.1f,
                category = SettingCategories.VISUAL
            )
            .addSliderSetting(
                id = "paused_opacity",
                displayName = "Paused Opacity",
                description = "Opacity when media is paused",
                defaultValue = 0.2f,
                minValue = 0.1f,
                maxValue = 0.8f,
                stepSize = 0.1f,
                category = SettingCategories.VISUAL
            )
            .build()
    }

    override fun applySettings(settings: ThemeSettings) {
        // Apply brightness
        currentBrightness = settings.getSliderValueFloat("brightness", 1.0f)
        settingsBrightness = (currentBrightness * 255).toInt()

        // Apply paused opacity
        currentPausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.5f)

        // Apply scroll speed
        currentScrollSpeed = settings.getSliderValueInt("scroll_speed", 6)
        updateScrollSpeed() // Update the scroll speed with new setting

        // Apply pause mode
        currentPauseMode = settings.getDropdownValue("pause_mode", "freeze")

        // Apply text display settings
        val newShowArtist = settings.getToggleValue("show_artist", true)
        val newShowAlbum = settings.getToggleValue("show_album", false)
        val newTextSpacing = settings.getSliderValueInt("text_spacing", 1)

        // Check if text format settings changed
        if (newShowArtist != currentShowArtist ||
            newShowAlbum != currentShowAlbum ||
            newTextSpacing != currentTextSpacing) {

            currentShowArtist = newShowArtist
            currentShowAlbum = newShowAlbum
            currentTextSpacing = newTextSpacing

            // Force text update
            lastTrackInfo = null
            needsTextUpdate = true
            updateTextIfNeeded()
        }

        Log.d(LOG_TAG, "Settings applied - Brightness: $currentBrightness, " +
                "PausedOpacity: $currentPausedOpacity, ScrollSpeed: $currentScrollSpeed, " +
                "ShowArtist: $currentShowArtist, ShowAlbum: $currentShowAlbum")
    }

    override fun getSettingsId(): String = "scroll_theme"

    // =================================================================================
    // LIFECYCLE
    // =================================================================================

    /**
     * Reset scroll position when theme is activated.
     */
    fun onActivate() {
        scrollPosition = 0
        pausedScrollPosition = 0
        offlineScrollPosition = 0
        lastTrackInfo = null
        needsTextUpdate = true
        updateTextIfNeeded()
        // Initialize time tracking for consistent scrolling
        lastFrameTime = System.currentTimeMillis()
        lastOfflineFrameTime = System.currentTimeMillis()
        accumulatedScrollTime = 0f
        accumulatedOfflineScrollTime = 0f
        Log.d(LOG_TAG, "Theme activated")
    }

    /**
     * Clean up resources when theme is deactivated.
     */
    fun onDeactivate() {
        Log.d(LOG_TAG, "Theme deactivated")
    }
}