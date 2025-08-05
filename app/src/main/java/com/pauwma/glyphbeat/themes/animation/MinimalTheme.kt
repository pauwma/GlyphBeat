package com.pauwma.glyphbeat.themes.animation

import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransition
import com.pauwma.glyphbeat.ui.settings.*


/**
 * Minimal Theme - A static, single-frame theme with state-specific frames.
 *
 * This theme displays a simple pattern without animation, perfect for users who want
 * a consistent visual without motion. It includes different frames for various states:
 * - Main frame: Bright static pattern
 * - Paused frame: Dimmed version of the pattern
 * - Offline frame: Dark with minimal accent pattern
 */
class MinimalTheme : ThemeTemplate(), ThemeSettingsProvider {

    // =================================================================================
    // THEME METADATA
    // =================================================================================

    override val titleTheme: String = "Minimal"

    override val descriptionTheme: String = "I'm empty inside I just want nothing."

    override val authorName: String = "pauwma"

    override val version: String = "1.0.0"

    override val category: String = "Minimal"

    override val tags: Array<String> = arrayOf("minimal", "static", "simple", "clean", "no-animation")

    override val createdDate: Long = System.currentTimeMillis()

    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================

    // No animation needed for static theme
    override val animationSpeedValue: Long = 1000L // Irrelevant for single frame

    // No individual frame durations needed
    override val frameDurations: LongArray? = null

    // No frame transitions needed
    override val frameTransitions: List<FrameTransition>? = null

    override val brightnessValue: Int = 255

    override val loopMode: String = "normal"

    override val complexity: String = "Simple"

    // =================================================================================
    // BEHAVIOR SETTINGS
    // =================================================================================

    override val isReactive: Boolean = false

    override val supportsFadeTransitions: Boolean = true

    // =================================================================================
    // TECHNICAL METADATA
    // =================================================================================

    override val compatibilityVersion: String = "1.0.0"

    override val frameDataFormat: String = "shaped"
    
    // Mutable properties for settings that can be changed at runtime
    private var showBorder: Boolean = false
    private var currentBrightness: Int = brightnessValue
    private var pausedOpacity: Float = 0.4f

    // =================================================================================
    // ANIMATION FRAMES
    // =================================================================================

    // Main frame with border (original)
    private val mainFrameWithBorder = intArrayOf(
        255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255
    )
    
    // Main frame without border (border pixels set to 0)
    private val mainFrameNoBorder = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0    )

    /**
     * Single static frame that switches based on border setting and applies current brightness
     */
    override val frames: Array<IntArray>
        get() {
            val baseFrame = if (showBorder) mainFrameWithBorder else mainFrameNoBorder
            val adjustedFrame = baseFrame.map { value ->
                if (value > 0) {
                    // Apply brightness scaling
                    (value * currentBrightness / 255.0).toInt().coerceIn(0, 255)
                } else 0
            }.toIntArray()
            return arrayOf(adjustedFrame)
        }

    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================

    // Paused frame with border
    private val pausedFrameWithBorder = intArrayOf(125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125)
    
    // Paused frame without border
    private val pausedFrameNoBorder = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
    
    // Offline frame with border
    private val offlineFrameWithBorder = intArrayOf(255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,255,255,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255)
    
    // Offline frame without border
    private val offlineFrameNoBorder = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
    
    override val pausedFrame: IntArray 
        get() {
            val baseFrame = if (showBorder) pausedFrameWithBorder else pausedFrameNoBorder
            return baseFrame.map { value ->
                if (value > 0) {
                    // Apply both brightness and paused opacity to the hardcoded paused frame
                    val adjustedValue = (value * currentBrightness / 255.0 * pausedOpacity).toInt()
                    adjustedValue.coerceIn(0, 255)
                } else 0
            }.toIntArray()
        }
        
    override val offlineFrame: IntArray 
        get() = if (showBorder) offlineFrameWithBorder else offlineFrameNoBorder


    /**
     * Frame displayed when media is loading/buffering.
     * Uses the paused frame as a loading indicator.
     */
    override val loadingFrame: IntArray 
        get() = pausedFrame.clone()

    /**
     * Frame displayed when there's an error state.
     * Uses default error indication (empty array).
     */
    override val errorFrame: IntArray = intArrayOf()

    // =================================================================================
    // OVERRIDDEN METHODS - Convert shaped data to flat array format
    // =================================================================================

    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)

        // Convert shaped grid data to flat 25x25 array
        val shapedData = frames[frameIndex]
        val flatArray = createEmptyFrame()

        // The shaped data represents the circular matrix layout
        // We need to map it to the proper positions in a 25x25 grid
        var shapedIndex = 0

        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col

                // Check if this pixel is within the circular matrix shape
                val centerX = 12.0
                val centerY = 12.0
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                if (distance <= 12.5 && shapedIndex < shapedData.size) {
                    flatArray[flatIndex] = shapedData[shapedIndex]
                    shapedIndex++
                }
            }
        }

        return flatArray
    }

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addToggleSetting(
                id = "outside_border",
                displayName = "Border",
                description = "Enable outside border stroke",
                defaultValue = false,
                category = SettingCategories.LAYOUT
            )
            .addSliderSetting(
                id = "minimal_brightness",
                displayName = "Brightness",
                description = "Brightness multiplier for icons",
                defaultValue = 255,
                minValue = 10,
                maxValue = 255,
                stepSize = 5,
                unit = null,
                category = SettingCategories.VISUAL
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
        // Apply the border toggle setting
        showBorder = settings.getToggleValue("outside_border", false)
        
        // Apply brightness setting
        currentBrightness = settings.getSliderValueInt("minimal_brightness", brightnessValue)
        
        // Apply paused opacity setting
        pausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.4f)
    }

    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = animationSpeedValue
    override fun getBrightness(): Int = currentBrightness
    override fun getDescription(): String = descriptionTheme
}