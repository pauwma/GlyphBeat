package com.pauwma.glyphbeat.themes.trackcontrol

import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.services.trackcontrol.TrackControlThemeRenderer
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.*

/**
 * Minimal arrow theme for track control.
 * 
 * Features:
 * - Static arrow icons (left/right)
 * - Opacity changes based on interaction state
 * - Configurable arrow size and thickness
 * - Simple, clean design
 */
class MinimalArrowTheme : TrackControlTheme(), TrackControlThemeSettingsProvider {
    
    // Theme metadata
    override fun getThemeName(): String = "Minimal"
    override fun getDescription(): String = "Simple arrow icons with opacity feedback"
    
    // Default theme parameters
    private var idleOpacity: Float = 0.5f
    private var pressedOpacity: Float = 1.0f
    private var arrowStyle: String = "bold"
    
    // Pre-generated frames cache
    private val frameCache = mutableMapOf<String, IntArray>()
    
    override fun getStateFrame(
        state: InteractionState,
        direction: Direction,
        frameIndex: Int
    ): IntArray {
        // Create cache key
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
        
        // Apply state-based opacity to the frame
        val opacity = when (state) {
            InteractionState.IDLE -> idleOpacity
            InteractionState.PRESSED -> pressedOpacity
            InteractionState.LONG_PRESSED -> 1.0f // Full brightness for action
            InteractionState.RELEASED -> idleOpacity
        }
        
        // Apply opacity to shaped frame
        val shapedWithOpacity = if (opacity < 1.0f) {
            shapedFrame.map { value ->
                if (value > 0) {
                    (value * opacity).toInt().coerceIn(0, 255)
                } else 0
            }.toIntArray()
        } else {
            shapedFrame.clone() // Return a copy at full brightness
        }
        
        // Convert shaped data to flat array for the matrix display
        return convertShapedToFlat(shapedWithOpacity)
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
        // Row 0 (7 pixels)
        0,0,0,0,0,0,0,
        // Row 1 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 2 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 3 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 4 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 5 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,
        // Row 6 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,
        // Row 7 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,
        // Row 8 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,
        // Row 9 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,
        // Row 10 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,
        // Row 11 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,
        // Row 12 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,
        // Row 13 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,
        // Row 14 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,
        // Row 15 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,
        // Row 16 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 17 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 18 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 19 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 20 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 21 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 22 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 23 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 24 (7 pixels)
        0,0,0,0,0,0,0
    )
    
    private fun getRightArrowFrame(style: String): IntArray {
        return when (style) {
            "thin" -> rightArrowThin
            else -> rightArrowBold // Default to bold
        }
    }
    
    // Left arrow frame in shaped format (489 pixels) with different styles
    private val leftArrowBold = intArrayOf(
        // Row 0 (7 pixels)
        0,0,0,0,0,0,0,
        // Row 1 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 2 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 3 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 4 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,
        // Row 5 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,
        // Row 6 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,
        // Row 7 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,
        // Row 8 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,
        // Row 9 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,0,
        // Row 10 (25 pixels)
        0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,0,
        // Row 11 (25 pixels)
        0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,255,255,255,0,0,0,0,
        // Row 12 (25 pixels)
        0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,
        // Row 13 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,
        // Row 14 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,0,
        // Row 15 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,0,0,
        // Row 16 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,
        // Row 17 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,
        // Row 18 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,
        // Row 19 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 20 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 21 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 22 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 23 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 24 (7 pixels)
        0,0,0,0,0,0,0
    )
    
    private val leftArrowThin = intArrayOf(
        // Row 0 (7 pixels)
        0,0,0,0,0,0,0,
        // Row 1 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 2 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 3 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 4 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 5 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,
        // Row 6 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,
        // Row 7 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,
        // Row 8 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,
        // Row 9 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,
        // Row 10 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,
        // Row 11 (25 pixels)
        0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,
        // Row 12 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,
        // Row 13 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,
        // Row 14 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,
        // Row 15 (25 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,
        // Row 16 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 17 (23 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 18 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 19 (21 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 20 (19 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 21 (17 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 22 (15 pixels)
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        // Row 23 (11 pixels)
        0,0,0,0,0,0,0,0,0,0,0,
        // Row 24 (7 pixels)
        0,0,0,0,0,0,0
    )
    
    private fun getLeftArrowFrame(style: String): IntArray {
        return when (style) {
            "thin" -> leftArrowThin
            else -> leftArrowBold // Default to bold
        }
    }
    
    override fun isAnimatedForState(state: InteractionState): Boolean = false
    
    override fun getBrightness(): Int = 255 // Full brightness for pre-baked frames
    
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
                    defaultValue = "bold",
                    options = listOf(
                        DropdownOption("bold", "Bold"),
                        DropdownOption("thin", "Thin")
                    ),
                    category = "Visual"
                ),
                "pressed_opacity" to SliderSetting(
                    id = "pressed_opacity",
                    displayName = "Pressed Opacity",
                    description = "Opacity when pressed",
                    defaultValue = 100,
                    minValue = 50,
                    maxValue = 100,
                    stepSize = 5,
                    unit = "%",
                    category = "Interaction"
                )
            )
        )
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Apply arrow style
        settings.getTypedValue("arrow_style", "bold").let {
            arrowStyle = (it as? String) ?: "bold"
        }
        
        // Apply idle opacity (convert percentage to fraction)
        settings.getTypedValue("idle_opacity", 50).let {
            idleOpacity = ((it as? Number)?.toFloat() ?: 50f) / 100f
        }
        
        // Apply pressed opacity (convert percentage to fraction)
        settings.getTypedValue("pressed_opacity", 100).let {
            pressedOpacity = ((it as? Number)?.toFloat() ?: 100f) / 100f
        }
        
        // Clear frame cache to regenerate with new settings
        frameCache.clear()
    }
}