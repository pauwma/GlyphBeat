package com.pauwma.glyphbeat.animation.styles

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.pauwma.glyphbeat.GlyphMatrixRenderer
import com.pauwma.glyphbeat.sound.MediaControlHelper

/**
 * CoverArtTheme - A dynamic theme that displays the album cover of currently playing media.
 * 
 * This theme shows album artwork from the currently playing track on the Glyph Matrix,
 * converted to a 25x25 grayscale representation. When media is paused, the same image
 * is shown at 50% opacity as requested. Falls back to a music note pattern when no
 * album art is available.
 * 
 * Features:
 * - Dynamic content based on current track's album art
 * - Real-time updates when track changes
 * - 50% opacity reduction for paused state
 * - Fallback music note pattern when no art is available
 * - Efficient bitmap processing and caching
 */
class CoverArtTheme(private val context: Context) : ThemeTemplate() {
    
    // =================================================================================
    // THEME METADATA
    // =================================================================================
    
    override val titleTheme: String = "Cover Art"
    
    override val descriptionTheme: String = "Try to guess what cover thats suppose to be!"
    
    override val authorName: String = "pauwma"
    
    override val version: String = "1.0.0"
    
    override val category: String = "Media"
    
    override val tags: Array<String> = arrayOf("album", "cover", "art", "media", "dynamic", "music")
    
    override val createdDate: Long = System.currentTimeMillis()
    
    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================
    
    // Static theme - no animation needed, just single frame display
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
    
    override val isReactive: Boolean = true // Reacts to media changes
    
    override val supportsFadeTransitions: Boolean = true
    
    // =================================================================================
    // TECHNICAL METADATA
    // =================================================================================
    
    override val compatibilityVersion: String = "1.0.0"
    
    override val frameDataFormat: String = "flat"
    
    // =================================================================================
    // PREVIEW FRAME - Static circle pattern for theme selection
    // =================================================================================
    
    /**
     * Preview frame shown in theme selection, using a simple circular pattern
     * like other themes instead of dynamic album art content.
     */
    override val previewFrame: IntArray by lazy {
        val frame = IntArray(625) { 0 } // 25x25 flat array
        val centerX = 12.0
        val centerY = 12.0

        // Create concentric circles only within the circular display area
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Only draw within the circular display area
                if (distance <= 12.5) {
                    when {
                        distance <= 2.0 -> frame[flatIndex] = 255  // Center bright
                        distance <= 4.0 -> frame[flatIndex] = 0    // Inner ring dark
                        distance <= 6.0 -> frame[flatIndex] = 200  // Middle ring
                        distance <= 8.0 -> frame[flatIndex] = 0    // Gap
                        distance <= 10.0 -> frame[flatIndex] = 150 // Outer ring
                        else -> frame[flatIndex] = 0               // Edge fade
                    }
                }
            }
        }

        frame
    }

    // =================================================================================
    // MEDIA HELPER AND CACHING
    // =================================================================================
    
    private val mediaHelper: MediaControlHelper by lazy { MediaControlHelper(context) }
    
    // Cache for album art to avoid repeated processing
    private var cachedTrackTitle: String? = null
    private var cachedAlbumArt: Bitmap? = null
    private var cachedFrameData: IntArray? = null
    private var cachedPausedFrameData: IntArray? = null
    
    // =================================================================================
    // DYNAMIC FRAME GENERATION
    // =================================================================================
    
    /**
     * Generate frames dynamically based on current media.
     * This overrides the static frames approach to provide dynamic content.
     */
    override val frames: Array<IntArray>
        get() = arrayOf(getCurrentAlbumArtFrame())
    
    /**
     * Get current album art as a matrix frame, with caching for performance.
     */
    private fun getCurrentAlbumArtFrame(): IntArray {
        return try {
            val trackInfo = mediaHelper.getTrackInfo()
            
            // Check cache first to avoid repeated processing
            if (trackInfo?.title == cachedTrackTitle && cachedFrameData != null) {
                return cachedFrameData!!
            }
            
            // Process new album art with enhanced contrast
            val frameData = if (trackInfo?.albumArt != null) {
                Log.d(LOG_TAG, "Converting album art for track: ${trackInfo.title} (with contrast enhancement)")
                mediaHelper.bitmapToMatrixArray(
                    bitmap = trackInfo.albumArt, 
                    brightnessMultiplier = 1.0, 
                    enhanceContrast = true // Enable contrast enhancement
                )
            } else {
                Log.d(LOG_TAG, "No album art available, using fallback pattern")
                mediaHelper.bitmapToMatrixArray(null, 1.0, false) // Fallback without enhancement
            }
            
            // Update cache
            cachedTrackTitle = trackInfo?.title
            cachedAlbumArt = trackInfo?.albumArt
            cachedFrameData = frameData
            
            // Generate paused frame (40% opacity) and cache it
            cachedPausedFrameData = frameData.map { (it * 0.4).toInt().coerceIn(0, 255) }.toIntArray()
            
            frameData
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error generating album art frame: ${e.message}")
            // Return fallback pattern on error
            mediaHelper.bitmapToMatrixArray(null, 1.0, false)
        }
    }
    
    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================
    
    /**
     * Paused frame shows the same album art but at 50% opacity as requested.
     */
    override val pausedFrame: IntArray
        get() {
            // Ensure we have current frame data first
            getCurrentAlbumArtFrame()
            
            // Return cached paused frame or generate it
            return cachedPausedFrameData ?: run {
                val currentFrame = cachedFrameData ?: getCurrentAlbumArtFrame()
                currentFrame.map { (it * 0.4).toInt().coerceIn(0, 255) }.toIntArray()
            }
        }
    
    /**
     * Offline frame shows a dimmed music note pattern.
     */
    override val offlineFrame: IntArray = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0) // Empty frame when offline
    
    /**
     * Loading frame shows medium brightness music note pattern.
     */
    override val loadingFrame: IntArray
        get() = mediaHelper.bitmapToMatrixArray(null, 0.4) // Medium brightness fallback

    /**
     * Error frame shows very dim pattern.
     */
    override val errorFrame: IntArray
        get() = mediaHelper.bitmapToMatrixArray(null, 0.1) // Very dim fallback
    
    // =================================================================================
    // OVERRIDDEN METHODS
    // =================================================================================
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)

        // Always return the current album art frame to match what's displayed on the matrix
        // This ensures UI preview matches the actual display
        return getCurrentAlbumArtFrame()
    }
    
    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = animationSpeedValue
    override fun getBrightness(): Int = brightnessValue
    override fun getDescription(): String = descriptionTheme
    
    /**
     * Clear cache when track changes to ensure fresh content.
     * This method can be called externally to force refresh.
     */
    fun clearCache() {
        cachedTrackTitle = null
        cachedAlbumArt = null
        cachedFrameData = null
        cachedPausedFrameData = null
        Log.d(LOG_TAG, "Album art cache cleared")
    }
    
    private companion object {
        private val LOG_TAG = CoverArtTheme::class.java.simpleName
    }
}