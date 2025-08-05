package com.pauwma.glyphbeat.themes.trackcontrol

import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.services.trackcontrol.TrackControlThemeRenderer
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.*

/**
 * Animated ripple theme for track control.
 * 
 * Features:
 * - Animated ripple effect on interaction
 * - Different ripple patterns for next/previous
 * - Smooth fade-out effects
 * - Configurable ripple speed and size
 */
class PulseRippleTheme : TrackControlTheme(), TrackControlThemeSettingsProvider {
    
    // Theme metadata
    override fun getThemeName(): String = "Pulse"
    override fun getDescription(): String = "Animated ripple effect with directional feedback"
    
    // Animation parameters
    private var rippleFrameCount: Int = 12
    private var maxRippleRadius: Float = 10f
    private var rippleSpeed: Long = 50L
    private var baseBrightness: Int = 255
    private var showArrowWithRipple: Boolean = true
    private var rippleFadeOut: Boolean = true
    
    // Pre-generated animation frames
    private val animationFrames = mutableMapOf<String, Array<IntArray>>()
    
    override fun getStateFrame(
        state: InteractionState,
        direction: Direction,
        frameIndex: Int
    ): IntArray {
        return when (state) {
            InteractionState.IDLE -> generateIdleFrame(direction)
            InteractionState.PRESSED -> generatePressedFrame(direction, frameIndex)
            InteractionState.LONG_PRESSED -> generateLongPressedFrame(direction, frameIndex)
            InteractionState.RELEASED -> generateReleasedFrame(direction)
        }
    }
    
    private fun generateIdleFrame(direction: Direction): IntArray {
        val grid = GlyphMatrixRenderer.createEmptyFlatArray()
        
        // Show faint arrow in idle state
        if (showArrowWithRipple) {
            TrackControlThemeRenderer.drawArrow(
                grid = grid,
                direction = direction,
                size = 5,
                thickness = 1,
                brightness = baseBrightness / 3 // Faint visibility
            )
        }
        
        return grid
    }
    
    private fun generatePressedFrame(direction: Direction, frameIndex: Int): IntArray {
        val cacheKey = "pressed_${direction.name}"
        
        // Check cache
        animationFrames[cacheKey]?.let { frames ->
            if (frameIndex < frames.size) return frames[frameIndex]
        }
        
        // Generate animation frames
        val frames = generateRippleAnimation(direction, isLongPress = false)
        animationFrames[cacheKey] = frames
        
        return frames[frameIndex.coerceIn(0, frames.size - 1)]
    }
    
    private fun generateLongPressedFrame(direction: Direction, frameIndex: Int): IntArray {
        val cacheKey = "longpress_${direction.name}"
        
        // Check cache
        animationFrames[cacheKey]?.let { frames ->
            if (frameIndex < frames.size) return frames[frameIndex]
        }
        
        // Generate animation frames with more dramatic effect
        val frames = generateRippleAnimation(direction, isLongPress = true)
        animationFrames[cacheKey] = frames
        
        return frames[frameIndex.coerceIn(0, frames.size - 1)]
    }
    
    private fun generateReleasedFrame(direction: Direction): IntArray {
        // Quick fade back to idle
        return generateIdleFrame(direction)
    }
    
    private fun generateRippleAnimation(direction: Direction, isLongPress: Boolean): Array<IntArray> {
        val frames = Array(rippleFrameCount) { frameIndex ->
            val grid = GlyphMatrixRenderer.createEmptyFlatArray()
            
            // Calculate ripple progress
            val progress = frameIndex.toFloat() / (rippleFrameCount - 1)
            
            // Ripple center based on direction
            val centerX = when (direction) {
                Direction.NEXT -> 12 + (progress * 3).toInt() // Ripple moves right
                Direction.PREVIOUS -> 12 - (progress * 3).toInt() // Ripple moves left
            }
            val centerY = 12
            
            // Draw multiple ripples for long press
            val rippleCount = if (isLongPress) 2 else 1
            
            for (i in 0 until rippleCount) {
                val rippleOffset = i * 0.3f
                val adjustedProgress = (progress - rippleOffset).coerceIn(0f, 1f)
                
                if (adjustedProgress > 0) {
                    val radius = adjustedProgress * maxRippleRadius
                    
                    TrackControlThemeRenderer.drawRipple(
                        grid = grid,
                        centerX = centerX,
                        centerY = centerY,
                        radius = radius,
                        maxRadius = maxRippleRadius,
                        brightness = baseBrightness,
                        fadeOut = rippleFadeOut
                    )
                }
            }
            
            // Overlay arrow if enabled
            if (showArrowWithRipple) {
                val arrowBrightness = if (isLongPress) {
                    // Pulse arrow brightness for long press
                    val pulseFactor = (Math.sin(progress * Math.PI * 2) + 1) / 2
                    (baseBrightness * 0.5 + baseBrightness * 0.5 * pulseFactor).toInt()
                } else {
                    (baseBrightness * (0.5 + progress * 0.5)).toInt()
                }
                
                TrackControlThemeRenderer.drawArrow(
                    grid = grid,
                    direction = direction,
                    size = 5,
                    thickness = 1,
                    brightness = arrowBrightness
                )
            }
            
            grid
        }
        
        return frames
    }
    
    override fun isAnimatedForState(state: InteractionState): Boolean {
        return state == InteractionState.PRESSED || state == InteractionState.LONG_PRESSED
    }
    
    override fun getFrameCountForState(state: InteractionState): Int {
        return when (state) {
            InteractionState.PRESSED, InteractionState.LONG_PRESSED -> rippleFrameCount
            else -> 1
        }
    }
    
    override fun getAnimationSpeedForState(state: InteractionState): Long {
        return when (state) {
            InteractionState.LONG_PRESSED -> rippleSpeed * 0.8f.toLong() // Slightly faster for long press
            else -> rippleSpeed
        }
    }
    
    override fun getBrightness(): Int = baseBrightness
    
    // Settings support
    override fun getSettingsId(): String = "track_control_pulse_ripple"
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettings(
            themeId = getSettingsId(),
            settings = mapOf(
                "ripple_speed" to SliderSetting(
                    id = "ripple_speed",
                    displayName = "Ripple Speed",
                    description = "Speed of ripple animation",
                    defaultValue = 50,
                    minValue = 20,
                    maxValue = 200,
                    stepSize = 10,
                    unit = "ms",
                    category = "Animation"
                ),
                "ripple_size" to SliderSetting(
                    id = "ripple_size",
                    displayName = "Ripple Size",
                    description = "Maximum ripple radius",
                    defaultValue = 10,
                    minValue = 5,
                    maxValue = 15,
                    stepSize = 1,
                    category = "Animation"
                ),
                "frame_count" to SliderSetting(
                    id = "frame_count",
                    displayName = "Animation Frames",
                    description = "Number of animation frames",
                    defaultValue = 12,
                    minValue = 6,
                    maxValue = 24,
                    stepSize = 2,
                    category = "Animation"
                ),
                "brightness" to SliderSetting(
                    id = "brightness",
                    displayName = "Brightness",
                    description = "Overall brightness",
                    defaultValue = 1.0f,
                    minValue = 0.1f,
                    maxValue = 1.0f,
                    stepSize = 0.1f,
                    unit = "x",
                    category = "Visual"
                ),
                "show_arrow" to ToggleSetting(
                    id = "show_arrow",
                    displayName = "Show Arrow",
                    description = "Display arrow with ripple effect",
                    defaultValue = true,
                    category = "Visual"
                ),
                "fade_out" to ToggleSetting(
                    id = "fade_out",
                    displayName = "Fade Out",
                    description = "Ripple fades as it expands",
                    defaultValue = true,
                    category = "Visual"
                )
            )
        )
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Apply ripple speed
        settings.getTypedValue("ripple_speed", 50).let {
            rippleSpeed = (it as? Number)?.toLong() ?: 50L
            settingsAnimationSpeed = rippleSpeed
        }
        
        // Apply ripple size
        settings.getTypedValue("ripple_size", 10).let {
            maxRippleRadius = (it as? Number)?.toFloat() ?: 10f
        }
        
        // Apply frame count
        settings.getTypedValue("frame_count", 12).let {
            rippleFrameCount = (it as? Number)?.toInt() ?: 12
        }
        
        // Apply brightness (convert multiplier to 0-255 range)
        settings.getTypedValue("brightness", 1.0f).let {
            val multiplier = (it as? Number)?.toFloat() ?: 1.0f
            baseBrightness = (multiplier * 255).toInt().coerceIn(0, 255)
            settingsBrightness = baseBrightness
        }
        
        // Apply show arrow
        showArrowWithRipple = settings.getTypedValue("show_arrow", true)
        
        // Apply fade out
        rippleFadeOut = settings.getTypedValue("fade_out", true)
        
        // Clear animation cache to regenerate with new settings
        animationFrames.clear()
    }
}