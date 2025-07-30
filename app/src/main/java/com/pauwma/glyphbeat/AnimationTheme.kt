package com.pauwma.glyphbeat

/**
 * Abstract base class for animation themes in the Glyph Matrix display.
 * 
 * Each theme defines its own animation style, frame count, and timing parameters.
 * Themes generate frames dynamically based on the current frame index.
 */
abstract class AnimationTheme {
    
    /**
     * Gets the total number of frames in this theme's animation loop.
     * 
     * @return Number of frames (recommended range: 4-60)
     */
    abstract fun getFrameCount(): Int
    
    /**
     * Generates a specific frame of the animation.
     * 
     * @param frameIndex Current frame index (0 to getFrameCount() - 1)
     * @return IntArray representing the 25x25 matrix frame
     */
    abstract fun generateFrame(frameIndex: Int): IntArray
    
    /**
     * Gets the display name of this theme.
     * 
     * @return Human-readable theme name
     */
    abstract fun getThemeName(): String
    
    /**
     * Gets the animation speed for this theme in milliseconds between frames.
     * Override to customize the theme's timing.
     * 
     * @return Delay in milliseconds (default: 150ms)
     */
    open fun getAnimationSpeed(): Long = 150L
    
    /**
     * Gets the default brightness level for this theme.
     * Override to customize the theme's brightness.
     * 
     * @return Brightness value 0-255 (default: 255)
     */
    open fun getBrightness(): Int = 255
    
    /**
     * Gets a description of what this theme displays.
     * 
     * @return Theme description
     */
    open fun getDescription(): String = "Animation theme for Glyph Matrix"
    
    /**
     * Validates that the frame index is within the valid range.
     * 
     * @param frameIndex Frame index to validate
     * @throws IllegalArgumentException if frameIndex is out of range
     */
    protected fun validateFrameIndex(frameIndex: Int) {
        require(frameIndex in 0 until getFrameCount()) {
            "Frame index $frameIndex is out of range [0, ${getFrameCount() - 1}] for theme ${getThemeName()}"
        }
    }
    
    /**
     * Creates an empty frame grid for this theme.
     * 
     * @return Empty IntArray of size 625 (25x25)
     */
    protected fun createEmptyFrame(): IntArray {
        return GlyphMatrixRenderer.createEmptyFlatArray()
    }
    
    /**
     * Clamps brightness to valid range.
     * 
     * @param brightness Brightness value to clamp
     * @return Clamped brightness (0-255)
     */
    protected fun clampBrightness(brightness: Int): Int {
        return GlyphMatrixRenderer.clampBrightness(brightness)
    }
}