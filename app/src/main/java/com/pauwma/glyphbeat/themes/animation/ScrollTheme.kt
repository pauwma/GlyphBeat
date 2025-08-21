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
    private var scrollSpeed = 1 // Pixels to move when triggered
    private var frameCounter = 0 // Frame counter for sub-pixel scrolling
    private var framesPerPixel = 1 // How many frames before moving 1 pixel
    private var currentText = DEFAULT_TEXT
    private var lastTrackInfo: MediaControlHelper.TrackInfo? = null
    
    // Pause state tracking
    private var isPaused = false
    private var currentPauseMode = "freeze" // "freeze" or "slow_motion"
    
    // Settings-driven properties
    private var currentBrightness = 1.0f
    private var currentPausedOpacity = 0.5f
    private var currentScrollSpeed = 5 // 1-10 setting
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
    
    override val animationSpeedValue: Long = 50L // Fast frame rate for smooth scrolling
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
    
    // Offline frame shows "?" icon - static frame (625 elements for 25x25 matrix)
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
    
    // Override pausedFrame property to generate the paused frame dynamically
    override val pausedFrame: IntArray
        get() {
            // Ensure we're in paused state for consistent behavior
            if (!isPaused) {
                isPaused = true
                pausedScrollPosition = scrollPosition
            }
            
            // Return the same frame that generateFrame would return when paused
            // This ensures consistency between app open/closed states
            return generateFrame(0)
        }
    
    // =================================================================================
    // FRAME GENERATION
    // =================================================================================
    
    override fun getFrameCount(): Int = 100 // Virtual frame count for continuous scrolling
    
    override fun generateFrame(frameIndex: Int): IntArray {
        // Sync pause state with actual media state
        val isMediaPlaying = try {
            mediaHelper.isPlaying()
        } catch (e: Exception) {
            false
        }
        
        // Update pause state if needed
        if (!isMediaPlaying && !isPaused) {
            // Media is paused but we're not - sync up
            isPaused = true
            pausedScrollPosition = scrollPosition
        } else if (isMediaPlaying && isPaused) {
            // Media is playing but we're paused - sync up
            isPaused = false
            if (currentPauseMode == "freeze") {
                scrollPosition = pausedScrollPosition
            }
            frameCounter = 0
        }
        
        // Update text if needed
        updateTextIfNeeded()
        
        // Update scroll speed if text changed
        if (needsTextUpdate) {
            updateScrollSpeed()
        }
        
        // Handle scrolling based on pause state
        if (!isPaused) {
            // Normal scrolling when playing
            if (currentScrollSpeed <= 4) {
                // For slow speeds, use frame counting
                frameCounter++
                if (frameCounter >= framesPerPixel) {
                    scrollPosition += 1 // Move 1 pixel
                    frameCounter = 0
                }
            } else {
                // For faster speeds, move multiple pixels per frame
                scrollPosition += scrollSpeed
            }
        } else if (currentPauseMode == "slow_motion") {
            // In slow motion mode, scroll very slowly when paused
            frameCounter++
            if (frameCounter >= framesPerPixel * 3) { // 3x slower than normal
                scrollPosition += 1
                frameCounter = 0
            }
        }
        // If paused and freeze mode, don't update scrollPosition at all
        
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
        // In slow_motion mode, continue from current position
        frameCounter = 0 // Reset frame counter on resume
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
        
        // Map speed setting to appropriate scrolling behavior
        when (currentScrollSpeed) {
            1 -> {
                framesPerPixel = 5
                scrollSpeed = 1
            }
            2 -> {
                framesPerPixel = 4
                scrollSpeed = 1
            }
            3, 4 -> {
                framesPerPixel = 3
                scrollSpeed = 1
            }
            5, 6 -> {
                framesPerPixel = 2
                scrollSpeed = 1
            }
            7, 8 -> {
                framesPerPixel = 1
                scrollSpeed = 2
            }
            9, 10 -> {
                framesPerPixel = 1
                scrollSpeed = 3
            }
            else -> {
                framesPerPixel = 4
                scrollSpeed = 1
            }
        }
        
        // Reset frame counter when speed changes
        frameCounter = 0
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
                defaultValue = "freeze",
                options = listOf(
                    DropdownOption("freeze", "Freeze", "Stop scrolling completely"),
                    DropdownOption("slow_motion", "Slow Motion", "Continue scrolling slowly")
                ),
                category = SettingCategories.ANIMATION
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
                defaultValue = 0.5f,
                minValue = 0.1f,
                maxValue = 0.8f,
                stepSize = 0.1f,
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "show_artist",
                displayName = "Show Artist",
                description = "Display artist name",
                defaultValue = true,
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "show_album",
                displayName = "Show Album",
                description = "Display album name",
                defaultValue = false,
                category = SettingCategories.VISUAL
            )
            .addSliderSetting(
                id = "text_spacing",
                displayName = "Character Spacing",
                description = "Space between characters",
                defaultValue = 1,
                minValue = 1,
                maxValue = 5,
                stepSize = 1,
                unit = "px",
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
        currentScrollSpeed = settings.getSliderValueInt("scroll_speed", 5)
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
        lastTrackInfo = null
        needsTextUpdate = true
        updateTextIfNeeded()
        Log.d(LOG_TAG, "Theme activated")
    }
    
    /**
     * Clean up resources when theme is deactivated.
     */
    fun onDeactivate() {
        Log.d(LOG_TAG, "Theme deactivated")
    }
}