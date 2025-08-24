package com.pauwma.glyphbeat.themes.trackcontrol

import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.*
import kotlin.math.roundToInt

/**
 * Minimal arrow theme for track control with dual-brightness system.
 * 
 * Features:
 * - Static arrow icons (left/right) with different styles
 * - Long-press only interaction model (no short-press)
 * - Dual brightness system with smooth animations
 * - Configurable brightness levels and feedback duration
 * - Simple, clean design optimized for clarity
 */
class MinimalArrowTheme : TrackControlTheme(), TrackControlThemeSettingsProvider {
    
    // Theme metadata
    override fun getThemeName(): String = "Minimal"
    override fun getDescription(): String = "Simple arrow icons with brightness feedback"
    
    // Brightness system - using float multipliers (0.1-1.0) matching other Animation Themes
    private var idleBrightness: Float = 0.4f        // Dimmed idle state
    private var pressedBrightness: Float = 1.0f     // Full brightness on long-press
    private var feedbackDuration: Float = 1.0f      // Duration in seconds for long-press feedback
    private var arrowStyle: String = "skip"         // Arrow visual style
    
    // Time-based brightness animation system
    private var longPressStartTime: Long = 0L
    private var isAnimating: Boolean = false
    private var currentBrightnessMultiplier: Float = idleBrightness
    
    // State change tracking to prevent animation restarts
    private var lastProcessedState: InteractionState = InteractionState.IDLE
    private var lastStateChangeTime: Long = 0L
    
    // Initialize theme with proper settings
    init {
        // Ensure we start with idle brightness
        currentBrightnessMultiplier = idleBrightness
        isAnimating = false
    }

    
    // Frame cache for different states (simplified for long-press only)
    private val frameCache = mutableMapOf<String, IntArray>()
    
    override fun getStateFrame(
        state: InteractionState,
        direction: Direction,
        frameIndex: Int
    ): IntArray {
        val currentTime = System.currentTimeMillis()
        
        // Only process genuine state changes, not repeated calls
        if (state != lastProcessedState) {
            lastProcessedState = state
            lastStateChangeTime = currentTime
            
            when (state) {
                InteractionState.LONG_PRESSED -> {
                    // Start new animation only if not already animating
                    if (!isAnimating) {
                        longPressStartTime = currentTime
                        isAnimating = true
                    }
                }
                InteractionState.IDLE -> {
                    // Don't stop animation on IDLE - let it complete naturally
                    // This handles service returning to IDLE immediately after long-press
                }
                else -> {
                    // Ignore short-press states (PRESSED, RELEASED, POST_ACTION)
                    // Return idle frame for compatibility
                    return getIdleFrame(direction)
                }
            }
        }
        
        // Update current brightness based on animation timing
        updateCurrentBrightness()
        
        // Create cache key based on current animation state
        val effectiveState = if (isAnimating) "ANIMATING" else "IDLE"
        val cacheKey = "${effectiveState}_${direction.name}_${arrowStyle}"
        
        // Check cache first
        frameCache[cacheKey]?.let { cachedFrame ->
            return applyBrightnessToFrame(cachedFrame)
        }
        
        // Generate new frame
        val frame = generateStateFrame(direction)
        
        // Cache the base frame (without brightness applied)
        frameCache[cacheKey] = frame.clone()
        
        // Return frame with current brightness applied
        return applyBrightnessToFrame(frame)
    }
    
    /**
     * Generates the base frame for a given direction (without brightness applied).
     */
    private fun generateStateFrame(direction: Direction): IntArray {
        val shapedFrame = when (direction) {
            Direction.PREVIOUS -> getLeftArrowFrame(arrowStyle)
            Direction.NEXT -> getRightArrowFrame(arrowStyle)
        }
        
        return convertShapedToFlat(shapedFrame)
    }
    
    /**
     * Gets the idle frame for a specific direction (for fallback use).
     */
    private fun getIdleFrame(direction: Direction): IntArray {
        // Reset animation state and use idle brightness
        isAnimating = false
        currentBrightnessMultiplier = idleBrightness
        return applyBrightnessToFrame(generateStateFrame(direction))
    }
    
    /**
     * Applies current brightness multiplier to a frame.
     */
    private fun applyBrightnessToFrame(baseFrame: IntArray): IntArray {
        return baseFrame.map { pixel ->
            if (pixel > 0) {
                (pixel * currentBrightnessMultiplier).roundToInt().coerceIn(0, 255)
            } else {
                0
            }
        }.toIntArray()
    }
    
    /**
     * Updates current brightness based on animation timing.
     * Called on every frame to calculate time-based brightness.
     */
    private fun updateCurrentBrightness() {
        if (!isAnimating) {
            currentBrightnessMultiplier = idleBrightness
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val elapsedMs = currentTime - longPressStartTime
        val feedbackDurationMs = (feedbackDuration * 1000).toLong()
        val fadeDurationMs = 300L // 300ms fade duration
        
        when {
            // Phase 1: Hold at pressed brightness for feedback duration
            elapsedMs < feedbackDurationMs -> {
                currentBrightnessMultiplier = pressedBrightness
            }
            // Phase 2: Smooth fade back to idle brightness
            elapsedMs < feedbackDurationMs + fadeDurationMs -> {
                val fadeProgress = (elapsedMs - feedbackDurationMs).toFloat() / fadeDurationMs.toFloat()
                val fadeProgressClamped = fadeProgress.coerceIn(0f, 1f)
                currentBrightnessMultiplier = pressedBrightness - (fadeProgressClamped * (pressedBrightness - idleBrightness))
            }
            // Phase 3: Animation complete - return to idle
            else -> {
                isAnimating = false
                currentBrightnessMultiplier = idleBrightness
            }
        }
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

    override fun getBrightness(): Int = 255 // Full base brightness - theme applies multipliers to frames
    
    // Override base class getBrightnessForState to prevent interference
    override fun getBrightnessForState(state: InteractionState): Int = 255 // Always full brightness
    
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
                "idle_brightness" to SliderSetting(
                    id = "idle_brightness",
                    displayName = "Idle Brightness",
                    description = "Brightness level when not interacting (dimmed)",
                    defaultValue = 0.4f,
                    minValue = 0.1f,
                    maxValue = 0.8f,
                    stepSize = 0.1f,
                    unit = "x",
                    category = "Visual"
                ),
                "pressed_brightness" to SliderSetting(
                    id = "pressed_brightness",
                    displayName = "Long-Press Brightness",
                    description = "Brightness level during long-press feedback",
                    defaultValue = 1.0f,
                    minValue = 0.1f,
                    maxValue = 1.0f,
                    stepSize = 0.1f,
                    unit = "x",
                    category = "Visual"
                ),
                "feedback_duration" to SliderSetting(
                    id = "feedback_duration",
                    displayName = "Feedback Duration",
                    description = "How long to hold brightness after long-press",
                    defaultValue = 1.0f,
                    minValue = 0.5f,
                    maxValue = 2.0f,
                    stepSize = 0.5f,
                    unit = "s",
                    category = "Interaction"
                )
            )
        )
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Apply arrow style
        settings.getTypedValue("arrow_style", "skip").let {
            arrowStyle = (it as? String) ?: "skip"
        }
        
        // Apply idle brightness with validation
        settings.getTypedValue("idle_brightness", 0.4f).let {
            val value = (it as? Number)?.toFloat() ?: 0.4f
            idleBrightness = value.coerceIn(0.1f, 0.8f)
        }

        // Apply pressed brightness with validation
        settings.getTypedValue("pressed_brightness", 1.0f).let {
            val value = (it as? Number)?.toFloat() ?: 1.0f
            pressedBrightness = value.coerceIn(0.1f, 1.0f)
        }
        
        // Apply feedback duration with validation
        settings.getTypedValue("feedback_duration", 1.0f).let {
            val value = (it as? Number)?.toFloat() ?: 1.0f
            feedbackDuration = value.coerceIn(0.5f, 2.0f)
        }
        
        // Reset animation state and apply new idle brightness immediately
        isAnimating = false
        currentBrightnessMultiplier = idleBrightness
        lastProcessedState = InteractionState.IDLE
        lastStateChangeTime = System.currentTimeMillis()

        // Clear frame cache to regenerate with new settings
        frameCache.clear()
    }
    
    /**
     * Cleanup method to reset animation state when theme is destroyed.
     */
    fun cleanup() {
        isAnimating = false
        currentBrightnessMultiplier = idleBrightness
    }
}