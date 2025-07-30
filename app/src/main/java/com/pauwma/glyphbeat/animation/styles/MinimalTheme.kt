package com.pauwma.glyphbeat.animation.styles


/**
 * Minimal Theme - A static, single-frame theme with state-specific frames.
 * 
 * This theme displays a simple pattern without animation, perfect for users who want
 * a consistent visual without motion. It includes different frames for various states:
 * - Main frame: Bright static pattern
 * - Paused frame: Dimmed version of the pattern
 * - Offline frame: Dark with minimal accent pattern
 */
class MinimalTheme : ThemeTemplate() {
    
    // =================================================================================
    // THEME METADATA
    // =================================================================================
    
    override val titleTheme: String = "Minimal"
    
    override val descriptionTheme: String = "I'm empty inside I just want nothing."
    
    override val authorName: String = "pauwma"
    
    override val version: String = "1.0.0"
    
    override val category: String = "Minimal"
    
    override val tags: Array<String> = arrayOf("minimal", "static", "simple", "clean", "no-animation")
    
    override val createdDate: Long = System.currentTimeMillis()
    
    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================
    
    // No animation needed for static theme
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
    
    override val isReactive: Boolean = false
    
    override val supportsFadeTransitions: Boolean = true
    
    // =================================================================================
    // TECHNICAL METADATA
    // =================================================================================
    
    override val compatibilityVersion: String = "1.0.0"
    
    override val frameDataFormat: String = "shaped"
    
    // =================================================================================
    // ANIMATION FRAMES
    // =================================================================================
    
    /**
     * Single static frame with your provided minimal pattern.
     * Uses 489-pixel shaped data like Duck and Vinyl themes.
     */
    override val frames = arrayOf(
        // Main frame: Your original minimal pattern (489 pixels - shaped format)
        intArrayOf(
            255,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,255,255,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,255
        )
    )
    
    // =================================================================================
    // STATE-SPECIFIC FRAMES
    // =================================================================================

    override val pausedFrame: IntArray = intArrayOf(125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,125,125,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,125,125,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,125,125,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,125,125,125,0,0,0,0,0,0,0,0,0,0,0,125,125,125,125,0,0,0,0,0,0,0,125,125,125,125,125,125,125,125,125) // Use first frame as paused state
    override val offlineFrame: IntArray = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0) // Empty frame when offline


    /**
     * Frame displayed when media is loading/buffering.
     * Uses the paused frame as a loading indicator.
     */
    override val loadingFrame: IntArray by lazy { pausedFrame.clone() }
    
    /**
     * Frame displayed when there's an error state.
     * Uses default error indication (empty array).
     */
    override val errorFrame: IntArray = intArrayOf()
    
    // =================================================================================
    // OVERRIDDEN METHODS - Convert shaped data to flat array format
    // =================================================================================
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        
        // Convert shaped grid data to flat 25x25 array
        val shapedData = frames[frameIndex]
        val flatArray = createEmptyFrame()
        
        // The shaped data represents the circular matrix layout
        // We need to map it to the proper positions in a 25x25 grid
        var shapedIndex = 0
        
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                
                // Check if this pixel is within the circular matrix shape
                val centerX = 12.0
                val centerY = 12.0
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))
                
                if (distance <= 12.5 && shapedIndex < shapedData.size) {
                    flatArray[flatIndex] = shapedData[shapedIndex]
                    shapedIndex++
                }
            }
        }
        
        return flatArray
    }
    
    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = animationSpeedValue
    override fun getBrightness(): Int = brightnessValue
    override fun getDescription(): String = descriptionTheme
}