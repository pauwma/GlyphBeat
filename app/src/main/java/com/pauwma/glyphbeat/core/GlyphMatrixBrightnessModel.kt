package com.pauwma.glyphbeat.core

/**
 * Unified brightness model for the Glyph Matrix display.
 * 
 * This object ensures that brightness calculations are consistent between:
 * - The actual Glyph Matrix hardware display
 * - The preview UI components
 * - Theme brightness settings
 * 
 * The model implements a single source of truth for how brightness values
 * are transformed from theme settings to final display output.
 */
object GlyphMatrixBrightnessModel {
    
    /**
     * Calculate the final pixel brightness value for the Glyph Matrix.
     * 
     * This method applies the theme brightness setting to a pixel value,
     * producing the final brightness that should be sent to the hardware.
     * 
     * @param pixelValue The base pixel value from the theme (0-255)
     * @param themeBrightness The theme's brightness setting (0-255, where 255 = 100%)
     * @return The final pixel brightness value (0-255)
     */
    fun calculateFinalBrightness(pixelValue: Int, themeBrightness: Int): Int {
        if (pixelValue == 0) return 0
        
        // Apply theme brightness as a multiplier
        val normalizedThemeBrightness = themeBrightness / 255f
        val adjustedValue = (pixelValue * normalizedThemeBrightness).toInt()
        
        return adjustedValue.coerceIn(0, 255)
    }
    
    /**
     * Calculate the alpha value for preview rendering.
     * 
     * This method converts a pixel brightness value to an alpha value for
     * the preview UI, ensuring the preview matches the actual display appearance.
     * 
     * @param pixelValue The base pixel value from the theme (0-255)
     * @param themeBrightness The theme's brightness setting (0-255)
     * @return The alpha value for preview rendering (0.0-1.0)
     */
    fun calculatePreviewAlpha(pixelValue: Int, themeBrightness: Int): Float {
        if (pixelValue == 0) return 0f
        
        // Calculate the final brightness using the same logic as the hardware
        val finalBrightness = calculateFinalBrightness(pixelValue, themeBrightness)
        
        // The Glyph Matrix hardware appears to have a minimum visible brightness threshold
        // and a non-linear response curve that makes low values appear brighter than expected
        val normalizedBrightness = finalBrightness / 255f
        
        // The hardware seems to have a minimum brightness threshold around 0.5-0.6
        // even when set to very low values like 0.1
        // This suggests the hardware applies a non-linear curve with a high floor
        
        // Map the brightness to match hardware behavior:
        // 0.1 setting (25.5/255) should appear as ~0.6 brightness
        // 1.0 setting should still be full brightness
        
        // Use a power curve that starts high and gradually approaches linear
        // This simulates the hardware's minimum brightness threshold
        val minVisibleAlpha = 0.5f  // Minimum visible brightness on hardware
        val range = 1.0f - minVisibleAlpha
        
        // Apply a square root curve to simulate the hardware's response
        val adjustedAlpha = minVisibleAlpha + (kotlin.math.sqrt(normalizedBrightness) * range)
        
        return adjustedAlpha.coerceIn(0f, 1f)
    }
    
    /**
     * Convert a brightness multiplier (0.1x - 1.0x) to a brightness value (0-255).
     * 
     * @param multiplier The brightness multiplier from settings (0.1 to 1.0)
     * @return The brightness value (0-255)
     */
    fun multiplierToBrightness(multiplier: Float): Int {
        return (multiplier * 255).toInt().coerceIn(0, 255)
    }
    
    /**
     * Convert a brightness value (0-255) to a multiplier (0.0x - 1.0x).
     * 
     * @param brightness The brightness value (0-255)
     * @return The brightness multiplier (0.0 to 1.0)
     */
    fun brightnessToMultiplier(brightness: Int): Float {
        return (brightness / 255f).coerceIn(0f, 1f)
    }
}