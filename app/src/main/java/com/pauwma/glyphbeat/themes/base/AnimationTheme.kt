package com.pauwma.glyphbeat.themes.base

import com.pauwma.glyphbeat.core.DeviceManager
import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.core.GlyphResolution

/**
 * Abstract base class for animation themes in the Glyph Matrix display.
 *
 * Each theme defines its own animation style, frame count, and timing parameters.
 * Themes generate frames dynamically based on the current frame index.
 * Resolution-aware: uses DeviceManager to adapt to Phone 3 (25x25) or Phone 4a Pro (13x13).
 */
abstract class AnimationTheme {

    /** Current device resolution */
    protected val resolution: GlyphResolution get() = DeviceManager.resolution

    /** Grid size for the current device (25 or 13) */
    protected val gridSize: Int get() = resolution.gridSize

    /** Center pixel coordinate for the current device (12 or 6) */
    protected val centerPixel: Int get() = resolution.center

    /** Flat array size for the current device (625 or 169) */
    protected val flatSize: Int get() = resolution.flatSize

    /** Max radius for circular effects on the current device */
    protected val deviceMaxRadius: Float get() = resolution.maxRadius

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
     * @return IntArray representing the matrix frame (sized for current device)
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
     * Creates an empty frame grid for this theme, sized for the current device.
     *
     * @return Empty IntArray sized for the current device resolution
     */
    protected fun createEmptyFrame(): IntArray {
        return GlyphMatrixRenderer.createDeviceEmptyFlatArray()
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