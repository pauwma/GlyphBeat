package com.pauwma.glyphbeat.services.trackcontrol

import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import kotlin.math.*

/**
 * Utility class for rendering track control visual elements on the Glyph Matrix.
 * 
 * Provides specialized drawing functions for arrows, ripples, and other
 * track control UI elements, building on top of GlyphMatrixRenderer.
 */
object TrackControlThemeRenderer {
    
    private const val MATRIX_SIZE = 25
    private const val CENTER_X = MATRIX_SIZE / 2
    private const val CENTER_Y = MATRIX_SIZE / 2
    
    /**
     * Draws an arrow pointing in the specified direction.
     * 
     * @param grid IntArray representing the 25x25 matrix
     * @param direction Arrow direction (left for previous, right for next)
     * @param size Arrow size (1-10, where 5 is default)
     * @param thickness Line thickness (1-3)
     * @param brightness Brightness value (0-255)
     * @param centerX Optional X center position (default: matrix center)
     * @param centerY Optional Y center position (default: matrix center)
     */
    fun drawArrow(
        grid: IntArray,
        direction: TrackControlTheme.Direction,
        size: Int = 5,
        thickness: Int = 1,
        brightness: Int = 255,
        centerX: Int = CENTER_X,
        centerY: Int = CENTER_Y
    ) {
        val clampedBrightness = GlyphMatrixRenderer.clampBrightness(brightness)
        val actualSize = size.coerceIn(1, 10)
        
        when (direction) {
            TrackControlTheme.Direction.NEXT -> drawRightArrow(
                grid, centerX, centerY, actualSize, thickness, clampedBrightness
            )
            TrackControlTheme.Direction.PREVIOUS -> drawLeftArrow(
                grid, centerX, centerY, actualSize, thickness, clampedBrightness
            )
        }
    }
    
    /**
     * Draws a ripple effect emanating from a center point.
     * 
     * @param grid IntArray representing the 25x25 matrix
     * @param centerX Ripple center X coordinate
     * @param centerY Ripple center Y coordinate
     * @param radius Current ripple radius (0 to maxRadius)
     * @param maxRadius Maximum ripple radius
     * @param brightness Base brightness value (0-255)
     * @param fadeOut If true, brightness fades as radius increases
     */
    fun drawRipple(
        grid: IntArray,
        centerX: Int,
        centerY: Int,
        radius: Float,
        maxRadius: Float,
        brightness: Int = 255,
        fadeOut: Boolean = true
    ) {
        if (radius <= 0) return
        
        val actualBrightness = if (fadeOut) {
            val fadeFactor = 1.0f - (radius / maxRadius)
            (brightness * fadeFactor).toInt()
        } else {
            brightness
        }
        
        val clampedBrightness = GlyphMatrixRenderer.clampBrightness(actualBrightness)
        
        // Draw circle with anti-aliasing effect
        val innerRadius = (radius - 0.5f).coerceAtLeast(0f)
        val outerRadius = radius + 0.5f
        
        for (y in 0 until MATRIX_SIZE) {
            for (x in 0 until MATRIX_SIZE) {
                val distance = sqrt(
                    ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toFloat()
                )
                
                if (distance in innerRadius..outerRadius) {
                    // Anti-aliasing: fade based on distance from exact radius
                    val alpha = if (distance < radius) {
                        (distance - innerRadius) / (radius - innerRadius)
                    } else {
                        1.0f - (distance - radius) / (outerRadius - radius)
                    }
                    
                    val pixelBrightness = (clampedBrightness * alpha).toInt()
                    val index = y * MATRIX_SIZE + x
                    
                    // Additive blending for overlapping ripples
                    grid[index] = (grid[index] + pixelBrightness).coerceAtMost(255)
                }
            }
        }
    }
    
    /**
     * Draws a double chevron (>>) or (<<) for track control.
     * 
     * @param grid IntArray representing the 25x25 matrix
     * @param direction Chevron direction
     * @param size Chevron size
     * @param spacing Spacing between chevrons
     * @param brightness Brightness value (0-255)
     */
    fun drawDoubleChevron(
        grid: IntArray,
        direction: TrackControlTheme.Direction,
        size: Int = 4,
        spacing: Int = 3,
        brightness: Int = 255
    ) {
        val offset = spacing / 2
        
        when (direction) {
            TrackControlTheme.Direction.NEXT -> {
                drawArrow(grid, direction, size, 1, brightness, CENTER_X - offset, CENTER_Y)
                drawArrow(grid, direction, size, 1, brightness, CENTER_X + offset, CENTER_Y)
            }
            TrackControlTheme.Direction.PREVIOUS -> {
                drawArrow(grid, direction, size, 1, brightness, CENTER_X + offset, CENTER_Y)
                drawArrow(grid, direction, size, 1, brightness, CENTER_X - offset, CENTER_Y)
            }
        }
    }
    
    /**
     * Creates a pulsing effect by modulating brightness.
     * 
     * @param baseBrightness Base brightness value
     * @param pulsePhase Current phase of the pulse (0.0 to 1.0)
     * @param minBrightness Minimum brightness during pulse
     * @return Modulated brightness value
     */
    fun calculatePulseBrightness(
        baseBrightness: Int,
        pulsePhase: Float,
        minBrightness: Int = 50
    ): Int {
        val pulseFactor = (sin(pulsePhase * 2 * PI).toFloat() + 1f) / 2f
        val brightness = minBrightness + ((baseBrightness - minBrightness) * pulseFactor).toInt()
        return GlyphMatrixRenderer.clampBrightness(brightness)
    }
    
    /**
     * Draws a media control icon (play, pause, etc.) - useful for future expansion.
     * 
     * @param grid IntArray representing the 25x25 matrix
     * @param iconType Type of media icon to draw
     * @param brightness Brightness value (0-255)
     */
    fun drawMediaIcon(
        grid: IntArray,
        iconType: MediaIconType,
        brightness: Int = 255
    ) {
        when (iconType) {
            MediaIconType.SKIP_NEXT -> drawDoubleChevron(
                grid, TrackControlTheme.Direction.NEXT, 5, 4, brightness
            )
            MediaIconType.SKIP_PREVIOUS -> drawDoubleChevron(
                grid, TrackControlTheme.Direction.PREVIOUS, 5, 4, brightness
            )
            MediaIconType.FAST_FORWARD -> {
                // Triple chevron for fast forward
                drawArrow(grid, TrackControlTheme.Direction.NEXT, 4, 1, brightness, CENTER_X - 4, CENTER_Y)
                drawArrow(grid, TrackControlTheme.Direction.NEXT, 4, 1, brightness, CENTER_X, CENTER_Y)
                drawArrow(grid, TrackControlTheme.Direction.NEXT, 4, 1, brightness, CENTER_X + 4, CENTER_Y)
            }
            MediaIconType.REWIND -> {
                // Triple chevron for rewind
                drawArrow(grid, TrackControlTheme.Direction.PREVIOUS, 4, 1, brightness, CENTER_X + 4, CENTER_Y)
                drawArrow(grid, TrackControlTheme.Direction.PREVIOUS, 4, 1, brightness, CENTER_X, CENTER_Y)
                drawArrow(grid, TrackControlTheme.Direction.PREVIOUS, 4, 1, brightness, CENTER_X - 4, CENTER_Y)
            }
        }
    }
    
    /**
     * Creates a transition frame between two states using crossfade.
     * 
     * @param fromFrame Source frame data
     * @param toFrame Target frame data
     * @param progress Transition progress (0.0 to 1.0)
     * @return Blended frame data
     */
    fun createTransitionFrame(
        fromFrame: IntArray,
        toFrame: IntArray,
        progress: Float
    ): IntArray {
        require(fromFrame.size == toFrame.size) { "Frames must be the same size" }
        
        val result = IntArray(fromFrame.size)
        val clampedProgress = progress.coerceIn(0f, 1f)
        
        for (i in fromFrame.indices) {
            result[i] = (fromFrame[i] * (1f - clampedProgress) + toFrame[i] * clampedProgress).toInt()
                .coerceIn(0, 255)
        }
        
        return result
    }
    
    // Private helper methods
    
    private fun drawRightArrow(
        grid: IntArray,
        centerX: Int,
        centerY: Int,
        size: Int,
        thickness: Int,
        brightness: Int
    ) {
        // Main line (horizontal)
        val startX = centerX - size
        val endX = centerX + size
        
        for (t in 0 until thickness) {
            GlyphMatrixRenderer.drawLine(grid, startX, centerY + t, endX, centerY + t, brightness)
        }
        
        // Arrowhead (pointing right)
        val headSize = size * 2 / 3
        for (t in 0 until thickness) {
            // Upper diagonal
            GlyphMatrixRenderer.drawLine(
                grid,
                endX - headSize, centerY - headSize + t,
                endX, centerY + t,
                brightness
            )
            // Lower diagonal
            GlyphMatrixRenderer.drawLine(
                grid,
                endX - headSize, centerY + headSize + t,
                endX, centerY + t,
                brightness
            )
        }
    }
    
    private fun drawLeftArrow(
        grid: IntArray,
        centerX: Int,
        centerY: Int,
        size: Int,
        thickness: Int,
        brightness: Int
    ) {
        // Main line (horizontal)
        val startX = centerX - size
        val endX = centerX + size
        
        for (t in 0 until thickness) {
            GlyphMatrixRenderer.drawLine(grid, startX, centerY + t, endX, centerY + t, brightness)
        }
        
        // Arrowhead (pointing left)
        val headSize = size * 2 / 3
        for (t in 0 until thickness) {
            // Upper diagonal
            GlyphMatrixRenderer.drawLine(
                grid,
                startX + headSize, centerY - headSize + t,
                startX, centerY + t,
                brightness
            )
            // Lower diagonal
            GlyphMatrixRenderer.drawLine(
                grid,
                startX + headSize, centerY + headSize + t,
                startX, centerY + t,
                brightness
            )
        }
    }
    
    /**
     * Media icon types for future expansion
     */
    enum class MediaIconType {
        SKIP_NEXT,
        SKIP_PREVIOUS,
        FAST_FORWARD,
        REWIND
    }
}