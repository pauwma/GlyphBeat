package com.pauwma.glyphbeat.themes.trackcontrol

import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.*

/**
 * Minimal arrow theme for track control.
 * 
 * Features:
 * - Static arrow icons (left/right)
 * - Brightness changes based on interaction state
 * - Configurable arrow size and thickness
 * - Simple, clean design
 */
class MinimalArrowTheme : TrackControlTheme(), TrackControlThemeSettingsProvider {
    
    // Theme metadata
    override fun getThemeName(): String = "Minimal"
    override fun getDescription(): String = "Simple arrow icons with brightness feedback"
    
    // Default theme parameters
    private var idleBrightness: Float = 0.5f
    private var pressedBrightness: Float = 1.0f
    private var arrowStyle: String = "skip"
    private var currentStateBrightness: Float = idleBrightness // Initialize with idle brightness

    
    // Pre-generated frames cache
    private val frameCache = mutableMapOf<String, IntArray>()
    private val postActionFrameCache = mutableMapOf<String, Array<IntArray>>()
    
    override fun getStateFrame(
        state: InteractionState,
        direction: Direction,
        frameIndex: Int
    ): IntArray {
        // Update current brightness based on state (even when using cache)
        currentStateBrightness = when (state) {
            InteractionState.IDLE -> idleBrightness
            InteractionState.PRESSED -> pressedBrightness  // Not currently used by services
            InteractionState.LONG_PRESSED -> pressedBrightness  // Use pressed brightness for long press
            InteractionState.RELEASED -> idleBrightness
            InteractionState.POST_ACTION -> pressedBrightness  // Use pressed brightness for post-action
        }
        
        // Create cache key for regular states
        val cacheKey = "${state.name}_${direction.name}"
        
        // Check cache first
        frameCache[cacheKey]?.let { return it }
        
        // Generate new frame (returns flat array)
        val frame = generateStateFrame(state, direction)
        
        // Cache it
        frameCache[cacheKey] = frame
        
        return frame
    }
    
    private fun generateStateFrame(state: InteractionState, direction: Direction): IntArray {
        // Get the shaped base frame based on direction and style
        val shapedFrame = when (direction) {
            Direction.PREVIOUS -> getLeftArrowFrame(arrowStyle)
            Direction.NEXT -> getRightArrowFrame(arrowStyle)
        }
        
        // Don't apply brightness here - let getBrightness() handle it
        // Just convert shaped data to flat array for the matrix display
        return convertShapedToFlat(shapedFrame)
    }
    
    private fun convertShapedToFlat(shapedData: IntArray): IntArray {
        val flatArray = IntArray(625) { 0 } // 25x25 flat array
        
        // Map shaped data to flat array positions
        var shapedIndex = 0
        val glyphShape = intArrayOf(
            7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
            25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
            19, 17, 15, 11, 7
        )
        
        for (row in 0 until 25) {
            val pixelsInRow = glyphShape[row]
            val startCol = (25 - pixelsInRow) / 2
            
            for (colInRow in 0 until pixelsInRow) {
                val col = startCol + colInRow
                val flatIndex = row * 25 + col
                
                if (shapedIndex < shapedData.size) {
                    flatArray[flatIndex] = shapedData[shapedIndex]
                    shapedIndex++
                }
            }
        }
        
        return flatArray
    }
    
    // Right arrow frame in shaped format (489 pixels) with different styles
    private val rightArrowBold = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )
    
    private val rightArrowThin = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )

    private val rightArrowSkip = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )
    
    private fun getRightArrowFrame(style: String): IntArray {
        return when (style) {
            "bold" -> rightArrowBold
            "thin" -> rightArrowThin
            else -> rightArrowSkip // Default
        }
    }
    
    // Left arrow frame in shaped format (489 pixels) with different styles
    private val leftArrowBold = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )
    
    private val leftArrowThin = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )

    private val leftArrowSkip = intArrayOf(
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
    )

    private fun getLeftArrowFrame(style: String): IntArray {
        return when (style) {
            "bold" -> leftArrowBold
            "thin" -> leftArrowThin
            else -> leftArrowSkip // Default
        }
    }

    override fun getBrightness(): Int = (currentStateBrightness * 255).toInt() // Dynamic brightness based on state
    
    // Override preview to return flat array for UI display
    override fun getPreviewFrame(direction: Direction): IntArray {
        // getStateFrame already returns flat array data
        return getStateFrame(InteractionState.IDLE, direction, 0)
    }
    
    // AnimationTheme required methods
    override fun getFrameCount(): Int = 1 // Single frame theme
    
    override fun generateFrame(frameIndex: Int): IntArray {
        // For preview purposes, return right arrow
        return getStateFrame(InteractionState.IDLE, Direction.NEXT, 0)
    }
    
    override fun getAnimationSpeed(): Long = 100L // Not used for static theme
    
    // Settings support
    override fun getSettingsId(): String = "track_control_minimal_arrow"
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettings(
            themeId = getSettingsId(),
            settings = mapOf(
                "arrow_style" to DropdownSetting(
                    id = "arrow_style",
                    displayName = "Arrow Style",
                    description = "Visual style of the arrow",
                    defaultValue = "skip",
                    options = listOf(
                        DropdownOption("skip", "Skip"),
                        DropdownOption("bold", "Bold"),
                        DropdownOption("thin", "Thin")
                    ),
                    category = "Visual"
                ),
/*                "idle_brightness" to SliderSetting(
                    id = "idle_brightness",
                    displayName = "Idle Brightness",
                    description = "Brightness when not pressed",
                    defaultValue = 5,
                    minValue = 1,
                    maxValue = 10,
                    stepSize = 1,
                    unit = "x",
                    category = "Visual"
                ),
                "pressed_brightness" to SliderSetting(
                    id = "pressed_brightness",
                    displayName = "Pressed Brightness",
                    description = "Brightness when pressed",
                    defaultValue = 10,
                    minValue = 5,
                    maxValue = 10,
                    stepSize = 1,
                    unit = "x",
                    category = "Visual"
                )*/
            )
        )
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Apply arrow style
        settings.getTypedValue("arrow_style", "skip").let {
            arrowStyle = (it as? String) ?: "skip"
        }
        
        // Apply idle brightness (convert from 1-10 to 0.1-1.0 multiplier)
        settings.getTypedValue("idle_brightness", 5).let {
            idleBrightness = ((it as? Number)?.toFloat() ?: 5f) / 10f
        }

        // Apply pressed brightness (convert from 1-10 to 0.1-1.0 multiplier)
        settings.getTypedValue("pressed_brightness", 10).let {
            pressedBrightness = ((it as? Number)?.toFloat() ?: 10f) / 10f
        }
        
        // Update current brightness to idle brightness (for proper initialization/restart)
        currentStateBrightness = idleBrightness

        // Clear frame caches to regenerate with new settings
        frameCache.clear()
        postActionFrameCache.clear()
    }
}