package com.pauwma.glyphbeat.themes.animation.preview

import android.graphics.*
import android.util.Log
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.*
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueFloat
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getToggleValue
import kotlin.math.pow

/**
 * CoverArtPreviewRenderer - Handles all rendering operations for CoverArtTheme previews.
 * 
 * This class is responsible for:
 * - Processing album art bitmaps for preview display
 * - Applying theme settings (brightness, contrast, rotation)
 * - Converting bitmaps to matrix frame format
 * - Generating fallback patterns when no media is available
 * - Optimizing rendering for preview size and performance
 * 
 * The renderer uses efficient bitmap processing techniques and caching
 * to ensure smooth preview animations without impacting performance.
 * 
 * Key Features:
 * - Intelligent bitmap scaling and sampling
 * - Hardware-accelerated image processing when available
 * - Circular masking for Glyph Matrix shape
 * - Smooth rotation with anti-aliasing
 * - Contrast enhancement algorithms
 */
class CoverArtPreviewRenderer {
    
    // =================================================================================
    // BITMAP PROCESSING
    // =================================================================================
    
    /**
     * Process album art for preview display with all settings applied.
     * 
     * @param albumArt Original album art bitmap
     * @param targetSize Target size for the preview (typically 25x25)
     * @param settings Theme settings to apply
     * @return Processed bitmap ready for preview display
     */
    fun processAlbumArtForPreview(
        albumArt: Bitmap,
        targetSize: Int = 25,
        settings: ThemeSettings? = null
    ): Bitmap {
        try {
            // Step 1: Scale to target size efficiently
            val scaledBitmap = scaleBitmapEfficiently(albumArt, targetSize)
            
            // Step 2: Apply visual enhancements based on settings
            val processedBitmap = if (settings != null) {
                applyVisualEnhancements(scaledBitmap, settings)
            } else {
                scaledBitmap
            }
            
            Log.v(LOG_TAG, "Processed album art for preview: ${targetSize}x${targetSize}")
            return processedBitmap
            
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error processing album art: ${e.message}")
            return createFallbackBitmap(targetSize)
        }
    }
    
    /**
     * Efficiently scale bitmap to target size using optimal sampling.
     */
    private fun scaleBitmapEfficiently(source: Bitmap, targetSize: Int): Bitmap {
        // Calculate optimal inSampleSize for memory efficiency
        val inSampleSize = calculateInSampleSize(source.width, source.height, targetSize)
        
        // Use matrix for precise scaling
        val matrix = Matrix()
        val scale = targetSize.toFloat() / maxOf(source.width, source.height)
        matrix.postScale(scale, scale)
        
        // Create scaled bitmap with bilinear filtering for smooth result
        val scaledBitmap = Bitmap.createBitmap(
            source, 0, 0,
            source.width, source.height,
            matrix, true // Enable filtering
        )
        
        // Ensure exact target size
        return if (scaledBitmap.width != targetSize || scaledBitmap.height != targetSize) {
            Bitmap.createScaledBitmap(scaledBitmap, targetSize, targetSize, true)
        } else {
            scaledBitmap
        }
    }
    
    /**
     * Apply visual enhancements based on theme settings.
     */
    private fun applyVisualEnhancements(bitmap: Bitmap, settings: ThemeSettings): Bitmap {
        val enhancedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Apply contrast enhancement first if enabled
        if (settings.getToggleValue("enhance_contrast", true)) {
            applyContrastEnhancement(enhancedBitmap)
        }
        
        // Apply brightness adjustment
        val brightness = settings.getSliderValueFloat("cover_brightness", 1.0f)
        if (brightness != 1.0f) {
            val canvas = Canvas(enhancedBitmap)
            val paint = Paint().apply {
                isAntiAlias = true
                isFilterBitmap = true
            }
            
            val colorMatrix = ColorMatrix().apply {
                setScale(brightness, brightness, brightness, 1f)
            }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            
            // Create a temp bitmap to avoid double drawing
            val tempBitmap = enhancedBitmap.copy(Bitmap.Config.ARGB_8888, false)
            canvas.drawBitmap(tempBitmap, 0f, 0f, paint)
            tempBitmap.recycle()
        }
        
        return enhancedBitmap
    }
    
    /**
     * Apply contrast enhancement to improve visibility on small display.
     */
    private fun applyContrastEnhancement(bitmap: Bitmap) {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        // Calculate histogram for adaptive contrast
        val histogram = IntArray(256)
        pixels.forEach { pixel ->
            val luminance = calculateLuminance(pixel)
            histogram[luminance]++
        }
        
        // Find min and max values (excluding outliers)
        val totalPixels = pixels.size
        val outlierThreshold = (totalPixels * 0.01).toInt() // 1% outliers
        
        var min = 0
        var max = 255
        var count = 0
        
        // Find min
        for (i in 0..255) {
            count += histogram[i]
            if (count > outlierThreshold) {
                min = i
                break
            }
        }
        
        // Find max
        count = 0
        for (i in 255 downTo 0) {
            count += histogram[i]
            if (count > outlierThreshold) {
                max = i
                break
            }
        }
        
        // Apply contrast stretch
        val range = (max - min).toFloat()
        if (range > 0) {
            for (i in pixels.indices) {
                pixels[i] = enhancePixelContrast(pixels[i], min, range)
            }
            bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
    }
    
    /**
     * Enhance contrast for a single pixel.
     */
    private fun enhancePixelContrast(pixel: Int, min: Int, range: Float): Int {
        val alpha = (pixel shr 24) and 0xFF
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Apply contrast stretch to each channel
        val enhancedRed = ((red - min) * 255f / range).toInt().coerceIn(0, 255)
        val enhancedGreen = ((green - min) * 255f / range).toInt().coerceIn(0, 255)
        val enhancedBlue = ((blue - min) * 255f / range).toInt().coerceIn(0, 255)
        
        return (alpha shl 24) or (enhancedRed shl 16) or (enhancedGreen shl 8) or enhancedBlue
    }
    
    // =================================================================================
    // FRAME CONVERSION
    // =================================================================================
    
    /**
     * Convert bitmap to preview frame format with optional rotation.
     * 
     * @param bitmap Source bitmap (must be 25x25)
     * @param rotation Rotation angle in degrees
     * @param settings Theme settings for additional processing
     * @return IntArray of 625 elements with brightness values (0-255)
     */
    fun bitmapToPreviewFrame(
        bitmap: Bitmap,
        rotation: Float = 0f,
        settings: ThemeSettings? = null
    ): IntArray {
        // Apply rotation if needed
        val rotatedBitmap = if (rotation != 0f) {
            rotateBitmap(bitmap, rotation)
        } else {
            bitmap
        }
        
        val frame = IntArray(625) // 25x25
        val centerX = 12.0
        val centerY = 12.0
        
        // Convert to grayscale with circular masking
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val index = row * 25 + col
                val distance = kotlin.math.sqrt(
                    (col - centerX) * (col - centerX) + 
                    (row - centerY) * (row - centerY)
                )
                
                // Apply circular mask
                if (distance <= 12.5) {
                    val pixel = rotatedBitmap.getPixel(col, row)
                    frame[index] = calculateLuminance(pixel)
                    
                    // Apply edge fade for smoother appearance
                    if (distance > 11.5) {
                        val fade = (12.5 - distance) / 1.0
                        frame[index] = (frame[index] * fade).toInt()
                    }
                } else {
                    frame[index] = 0
                }
            }
        }
        
        // Apply paused opacity if media is paused
        if (settings != null && !isMediaPlaying()) {
            val pausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.4f)
            for (i in frame.indices) {
                frame[i] = (frame[i] * pausedOpacity).toInt()
            }
        }
        
        return frame
    }
    
    /**
     * Rotate bitmap by specified angle with proper centering.
     */
    private fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        // Create a larger canvas to avoid clipping during rotation
        val diagonal = kotlin.math.sqrt(2.0) * source.width
        val targetSize = kotlin.math.ceil(diagonal).toInt()
        
        // Create a new bitmap with padding for rotation
        val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        
        // Calculate offset to center the original bitmap
        val offset = (targetSize - source.width) / 2f
        
        // Apply rotation around the center
        canvas.save()
        canvas.rotate(angle, targetSize / 2f, targetSize / 2f)
        canvas.drawBitmap(source, offset, offset, null)
        canvas.restore()
        
        // Crop back to original size from center
        val cropOffset = (targetSize - source.width) / 2
        return Bitmap.createBitmap(
            paddedBitmap,
            cropOffset,
            cropOffset,
            source.width,
            source.height
        )
    }
    
    /**
     * Calculate luminance value from RGB pixel.
     */
    private fun calculateLuminance(pixel: Int): Int {
        val red = (pixel shr 16) and 0xFF
        val green = (pixel shr 8) and 0xFF
        val blue = pixel and 0xFF
        
        // Use standard luminance formula
        return (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
    }
    
    // =================================================================================
    // FALLBACK PATTERNS
    // =================================================================================
    
    /**
     * Generate fallback preview when no album art is available.
     * Uses the same MinimalTheme-style pattern as the actual offline frame.
     */
    fun generateFallbackPreview(settings: ThemeSettings? = null): IntArray {
        // Use the exact same pattern as CoverArtTheme.offlineFrame
        // This is the MinimalTheme offline frame pattern converted to flat array
        val minimalOfflineFrameShaped = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
        
        // Convert shaped data to flat 25x25 array with circular masking (same as CoverArtTheme.offlineFrame)
        val frame = IntArray(625) { 0 }
        val centerX = 12.0
        val centerY = 12.0
        var shapedIndex = 0

        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Check if this pixel is within the circular matrix shape
                if (distance <= 12.5 && shapedIndex < minimalOfflineFrameShaped.size) {
                    frame[flatIndex] = minimalOfflineFrameShaped[shapedIndex]
                    shapedIndex++
                }
            }
        }
        
        // Apply brightness settings if provided
        if (settings != null) {
            val brightness = settings.getSliderValueFloat("cover_brightness", 1.0f)
            if (brightness != 1.0f) {
                for (i in frame.indices) {
                    frame[i] = (frame[i] * brightness).toInt().coerceIn(0, 255)
                }
            }
            
            // Apply paused opacity if media is paused
            if (!isMediaPlaying()) {
                val pausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.4f)
                for (i in frame.indices) {
                    frame[i] = (frame[i] * pausedOpacity).toInt()
                }
            }
        }
        
        return frame
    }
    
    /**
     * Create a fallback bitmap for error cases.
     */
    private fun createFallbackBitmap(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.FILL
        }
        
        // Draw simple gradient
        for (i in 0 until size) {
            val shade = (255 * (i.toFloat() / size)).toInt()
            paint.color = Color.rgb(shade, shade, shade)
            canvas.drawLine(0f, i.toFloat(), size.toFloat(), i.toFloat(), paint)
        }
        
        return bitmap
    }
    
    // =================================================================================
    // UTILITY METHODS
    // =================================================================================
    
    /**
     * Calculate optimal sample size for bitmap decoding.
     */
    private fun calculateInSampleSize(
        width: Int, 
        height: Int, 
        reqSize: Int
    ): Int {
        var inSampleSize = 1
        
        if (height > reqSize || width > reqSize) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqSize &&
                   (halfWidth / inSampleSize) >= reqSize) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Check if media is currently playing (stub - would connect to MediaControlHelper).
     */
    private fun isMediaPlaying(): Boolean {
        // This would typically check actual media state
        // For preview purposes, we'll return true
        return true
    }
    
    /**
     * Apply gamma correction for better preview visibility.
     */
    fun applyGammaCorrection(value: Int, gamma: Float = 2.2f): Int {
        val normalized = value / 255f
        val corrected = normalized.pow(1f / gamma)
        return (corrected * 255).toInt().coerceIn(0, 255)
    }
    
    companion object {
        private val LOG_TAG = CoverArtPreviewRenderer::class.java.simpleName
    }
}