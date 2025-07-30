package com.pauwma.glyphbeat.animation.styles

import com.pauwma.glyphbeat.AnimationTheme

/**
 * ANIMATION THEME TEMPLATE - PRE-GENERATED FRAMES WITH INDIVIDUAL FRAME DURATIONS
 * 
 * This template shows how to create themes using pre-generated frame data,
 * similar to the VinylTheme approach. This method is ideal when you have
 * specific pixel-perfect animations or complex patterns.
 * 
 * NEW FEATURE: Individual Frame Durations
 * ========================================
 * You can now specify different display times for each frame, allowing for
 * more sophisticated animations with variable timing.
 * 
 * QUICK START GUIDE:
 * ==================
 * 1. Copy this file and rename it (e.g., "StarFieldTheme.kt")
 * 2. Rename the class to match your theme (e.g., "StarFieldTheme")
 * 3. Replace the frames array with your own animation data
 * 4. Optionally set custom frameDurations for variable timing
 * 5. Update getThemeName() and getDescription() methods
 * 6. Adjust animationSpeed and other parameters as needed
 * 7. Add your theme to MediaPlayerToyService.kt availableThemes list
 * 
 * FRAME DATA FORMAT:
 * ==================
 * Each frame is an IntArray of 625 elements (25x25 matrix)
 * - Values range from 0-255 (0 = off, 255 = maximum brightness)
 * - Frames are stored in the frames array in animation order
 * - The Glyph Matrix has a circular shape, so edge pixels may not be visible
 * 
 * HOW TO CREATE FRAME DATA:
 * =========================
 * 1. Design your animation in https://pauwma.github.io/GlyphMatrixPaint/
 * 3. Export as raw pixel data or use a script to convert images
 * 4. Format as IntArray with comma-separated values
 * 5. Test on device to ensure proper display
 * 
 * ALTERNATIVE: Use GlyphMatrixRenderer utilities to generate frames programmatically:
 * - GlyphMatrixRenderer.createRotatingLineFrames()
 * - GlyphMatrixRenderer.createPulseFrames()
 * - GlyphMatrixRenderer.createWaveFrames()
 */

/**
 * Comprehensive theme template demonstrating declarative theme structure with individual frame durations.
 * 
 * This template shows how to define all theme information as parameters at the top,
 * making themes completely self-contained and easy to configure.
 * 
 * All methods become simple getters that return predefined values.
 */
open class ThemeTemplate() : AnimationTheme() {
    
    // =================================================================================
    // THEME METADATA - Edit these values for your custom theme
    // =================================================================================
    
    /**
     * Display title shown in UI theme cards and selection screens.
     * Keep concise and descriptive (2-4 words recommended).
     */
    protected open val titleTheme: String = "Blinking Cross"
    
    /**
     * Detailed description shown in UI theme cards.
     * Explain what the animation shows and any special features.
     */
    protected open val descriptionTheme: String = "A 4-frame cross pattern that pulses from bright to dim with variable timing, demonstrating both basic animation principles and individual frame duration control."
    
    /**
     * Theme creator/author name.
     * Credit the person who designed this theme.
     */
    open val authorName: String = "GlyphBeat Team"
    
    /**
     * Theme version for compatibility tracking.
     * Use semantic versioning (e.g., "1.0.0", "2.1.3").
     */
    open val version: String = "1.1.0"
    
    /**
     * Theme category for organization and filtering.
     * Common categories: "Music", "Abstract", "Gaming", "Utility", "Minimal".
     */
    open val category: String = "Abstract"
    
    /**
     * Descriptive tags for search and filtering.
     * Use relevant keywords that describe the theme's appearance or purpose.
     */
    open val tags: Array<String> = arrayOf("cross", "pulse", "simple", "template", "demo", "variable-timing")
    
    /**
     * Theme creation timestamp.
     * Use System.currentTimeMillis() for current time.
     */
    protected open val createdDate: Long = 1640995200000L // January 1, 2022
    
    // =================================================================================
    // ANIMATION PROPERTIES - Configure animation behavior
    // =================================================================================
    
    /**
     * Default speed between animation frames in milliseconds.
     * Used when no individual frame duration is specified.
     * Range: 50-1000ms (50ms = very fast, 1000ms = very slow)
     */
    protected open val animationSpeedValue: Long = 150L
    
    /**
     * Individual frame durations in milliseconds (NEW FEATURE).
     * Each element corresponds to one frame's display time.
     * Set to null to use global animationSpeed for all frames.
     * 
     * Example: Frame 0 displays for 250ms, Frame 1 for 150ms, etc.
     * This allows for sophisticated timing like slow builds and quick flashes.
     */
    protected open val frameDurations: LongArray? = longArrayOf(
        250L,  // Frame 0: Bright cross (longer display for emphasis)
        150L,  // Frame 1: Medium cross (normal speed)
        100L,  // Frame 2: Dim cross (quick transition)
        400L   // Frame 3: Off (longer pause before repeat)
    )
    
    /**
     * Default brightness level for the entire theme.
     * Range: 1-255 (1 = very dim, 255 = maximum brightness)
     */
    protected open val brightnessValue: Int = 255
    
    /**
     * Animation loop behavior.
     * Options: "normal" (1→2→3→4→1), "reverse" (4→3→2→1→4), "ping-pong" (1→2→3→4→3→2→1)
     */
    protected open val loopMode: String = "normal"
    
    /**
     * Theme complexity level for user guidance.
     * Options: "Simple", "Medium", "Complex"
     */
    open val complexity: String = "Simple"
    
    // =================================================================================
    // BEHAVIOR SETTINGS - Define theme capabilities
    // =================================================================================
    
    /**
     * Whether this theme responds to audio analysis data.
     * Set to true if the theme changes based on music volume, beats, etc.
     */
    open val isReactive: Boolean = false
    
    /**
     * Whether theme supports smooth transitions between states.
     * Most themes should support this for better user experience.
     */
    open val supportsFadeTransitions: Boolean = true
    
    // =================================================================================
    // TECHNICAL METADATA - Compatibility and format information
    // =================================================================================
    
    /**
     * Minimum app version required for this theme.
     * Use app version numbers to ensure compatibility.
     */
    protected open val compatibilityVersion: String = "1.0.0"
    
    /**
     * Frame data format specification.
     * Options: "flat" (25x25 array), "shaped" (circular layout), "hybrid"
     */
    protected open val frameDataFormat: String = "flat"
    
    // =================================================================================
    // ANIMATION FRAMES - Define all visual content
    // =================================================================================
    
    /**
     * Main animation frames array.
     * Each IntArray represents one frame of 625 pixels (25x25 matrix).
     * Values range from 0-255 (0 = off, 255 = maximum brightness).
     * 
     * EXAMPLE: 4-frame blinking cross animation
     * Frame 0: Bright cross (255 brightness)
     * Frame 1: Medium cross (170 brightness)  
     * Frame 2: Dim cross (85 brightness)
     * Frame 3: Off (0 brightness)
     */
    protected open val frames = arrayOf(
     /**
     * Replace this array with your own animation data.
     * Each IntArray represents one frame of 625 pixels (25x25).
     *
     * EXAMPLE: 4-frame blinking cross animation
     * Frame 1: Bright cross
     * Frame 2: Medium cross
     * Frame 3: Dim cross
     * Frame 4: Off
     */
        // Frame 1: Bright cross (255 brightness)
        intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0
        ),

        // Frame 2: Medium cross (170 brightness)
        intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,170,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,170,0,0,0,0,0,0,0,0,0,0,0,0
        ),

        // Frame 3: Dim cross (85 brightness)
        intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,85,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,85,0,0,0,0,0,0,0,0,0,0,0,0
        ),

        // Frame 4: Off (0 brightness)
        intArrayOf(
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
        )
    )

    /**
     * Automatic frame count calculation based on frames array size.
     * This is computed automatically - do not modify.
     */
    protected open val framesCount: Int by lazy { frames.size }

    /**
     * Preview frame for UI display (typically first frame).
     * This uses the first animation frame as the preview.
     */
    protected open val previewFrame: IntArray by lazy { frames[0].clone() }

    // =================================================================================
    // STATE-SPECIFIC FRAMES - Define frames for different application states
    // =================================================================================

    /**
     * Frame displayed when media is paused.
     * Uses the medium brightness cross (frame 1) to indicate paused state.
     * Set to intArrayOf() to use the last played frame instead.
     */
    open val pausedFrame: IntArray by lazy { frames[1].clone() } // Medium brightness cross

    /**
     * Frame displayed when no media is detected/available.
     * Uses the off frame (frame 3) to indicate no media activity.
     * Set to intArrayOf() to use the last played frame instead.
     */
    open val offlineFrame: IntArray by lazy { frames[3].clone() } // Off/dark frame

    /**
     * Frame displayed when media is loading/buffering.
     * Uses the dim cross (frame 2) to indicate loading state.
     * Set to intArrayOf() to use default loading animation.
     */
    open val loadingFrame: IntArray by lazy { frames[2].clone() } // Dim cross

    /**
     * Frame displayed when there's an error state.
     * Uses a specific pattern to indicate error condition.
     * Set to intArrayOf() to use default error indication.
     */
    open val errorFrame: IntArray = intArrayOf() // Use default error indication

    // =================================================================================
    // VALIDATION AND INITIALIZATION
    // =================================================================================

    /**
     * Validation method called after full initialization.
     * This ensures all overridden properties are properly set.
     */
    private fun validateTheme() {
        // Validate animation speed
        require(animationSpeedValue in 50L..1000L) {
            "Animation speed must be between 50ms and 1000ms, got ${animationSpeedValue}ms"
        }

        // Validate brightness
        require(brightnessValue in 1..255) {
            "Brightness must be between 1 and 255, got $brightnessValue"
        }

        // Validate frames array
        require(frames.isNotEmpty()) {
            "Frames array cannot be empty"
        }

        // Validate each frame has reasonable size based on format
        when (frameDataFormat) {
            "flat" -> {
                frames.forEachIndexed { index, frame ->
                    require(frame.size == 625) {
                        "Frame $index has ${frame.size} pixels, expected 625 for flat format (25x25 matrix)"
                    }
                }
            }
            "shaped" -> {
                // Shaped format can vary in size depending on the circular layout
                // Accept common sizes: 489 (DancingDuck), 512 (VinylTheme), or other reasonable circular sizes
                frames.forEachIndexed { index, frame ->
                    require(frame.size in 480..520) {
                        "Frame $index has ${frame.size} pixels, expected 480-520 for shaped format (circular matrix)"
                    }
                }
            }
            "hybrid" -> {
                frames.forEachIndexed { index, frame ->
                    require(frame.size == 625) {
                        "Frame $index has ${frame.size} pixels, expected 625 for hybrid format (25x25 matrix)"
                    }
                }
            }
            else -> {
                // Default to flat format validation
                frames.forEachIndexed { index, frame ->
                    require(frame.size == 625) {
                        "Frame $index has ${frame.size} pixels, expected 625 for default format (25x25 matrix)"
                    }
                }
            }
        }

        // Validate frame durations if provided
        frameDurations?.let { durations ->
            require(durations.size == frames.size) {
                "Frame durations array size (${durations.size}) must match frames array size (${frames.size})"
            }
            
            durations.forEachIndexed { index, duration ->
                require(duration in 50L..2000L) {
                    "Frame duration at index $index must be between 50ms and 2000ms, got ${duration}ms"
                }
            }
        }

        // Validate loop mode
        require(loopMode in arrayOf("normal", "reverse", "ping-pong")) {
            "Loop mode must be 'normal', 'reverse', or 'ping-pong', got '$loopMode'"
        }

        // Validate complexity
        require(complexity in arrayOf("Simple", "Medium", "Complex")) {
            "Complexity must be 'Simple', 'Medium', or 'Complex', got '$complexity'"
        }

        // Validate category
        require(category.isNotBlank()) {
            "Category cannot be blank"
        }

        // Validate title and description
        require(titleTheme.isNotBlank()) {
            "Title theme cannot be blank"
        }
        require(descriptionTheme.isNotBlank()) {
            "Description theme cannot be blank"
        }
    }
    
    // Track validation state
    private var isValidated = false
    
    /**
     * Ensure theme is validated before use.
     * This handles deferred validation to avoid initialization order issues.
     */
    private fun ensureValidated() {
        if (!isValidated) {
            validateTheme()
            isValidated = true
        }
    }
    
    /**
     * Constructor validation - ensures all parameters are valid.
     * Validation is deferred to allow child class properties to be fully initialized.
     */
    init {
        // Defer validation to ensure child class properties are initialized
        try {
            validateTheme()
            isValidated = true
        } catch (e: Exception) {
            // If validation fails during init, it will be called when methods are first accessed
            isValidated = false
        }
    }

    // =================================================================================
    // SIMPLE GETTER METHODS - All methods return predefined parameter values
    // =================================================================================

    // Core Animation Methods
    open override fun getFrameCount(): Int {
        ensureValidated()
        return framesCount
    }

    open override fun generateFrame(frameIndex: Int): IntArray {
        ensureValidated()
        validateFrameIndex(frameIndex)
        return frames[frameIndex].clone()
    }

    open override fun getThemeName(): String {
        ensureValidated()
        return titleTheme
    }
    
    open override fun getAnimationSpeed(): Long {
        ensureValidated()
        return animationSpeedValue
    }
    
    open override fun getBrightness(): Int {
        ensureValidated()
        return brightnessValue
    }
    
    open override fun getDescription(): String {
        ensureValidated()
        return descriptionTheme
    }

    // Individual Frame Duration Support (NEW FEATURE)
    /**
     * Get the display duration for a specific frame.
     * Returns individual frame duration if specified, otherwise returns global animation speed.
     * 
     * @param frameIndex The frame index (0 to getFrameCount() - 1)
     * @return Duration in milliseconds for the specified frame
     */
    fun getFrameDuration(frameIndex: Int): Long {
        ensureValidated()
        validateFrameIndex(frameIndex)
        return frameDurations?.getOrNull(frameIndex) ?: animationSpeedValue
    }
    
    /**
     * Check if this theme uses individual frame durations.
     * @return true if frameDurations is defined, false if using global animation speed
     */
    fun hasIndividualFrameDurations(): Boolean {
        ensureValidated()
        return frameDurations != null
    }

    // Note: All getter methods are automatically generated from the properties above
    // No need to define explicit getters - they cause JVM signature clashes
}

/**
 * INTEGRATION AND USAGE INSTRUCTIONS:
 * ====================================
 *
 * After creating your custom theme, add it to the service:
 *
 * 1. Open MediaPlayerToyService.kt
 * 2. Import your theme: import com.pauwma.glyphbeat.animation.styles.YourTheme
 * 3. Add to availableThemes list:
 *    private val availableThemes = listOf(
 *        VinylRecordTheme(),
 *        PulseTheme(),
 *        YourTheme(frameCount = 12, animationSpeed = 150L), // Your custom parameters
 *        // ... other themes
 *    )
 *
 * INDIVIDUAL FRAME DURATIONS USAGE:
 * ==================================
 *
 * // Option 1: Use global animation speed for all frames
 * private val frameDurations: LongArray? = null
 *
 * // Option 2: Specify individual durations for each frame
 * private val frameDurations: LongArray? = longArrayOf(
 *     500L,  // Frame 0: Half second (slow build-up)
 *     100L,  // Frame 1: Quick flash
 *     200L,  // Frame 2: Normal speed
 *     800L   // Frame 3: Long pause before repeat
 * )
 *
 * // Option 3: Mix of fast and slow frames for dramatic effect
 * private val frameDurations: LongArray? = longArrayOf(
 *     50L,   // Very fast
 *     1000L, // Very slow
 *     75L,   // Quick
 *     500L   // Medium
 * )
 *
 * TESTING YOUR THEME:
 * ===================
 *
 * 1. Build and install the app: ./gradlew installDebug
 * 2. Enable the MediaPlayerToy service in Nothing device settings
 * 3. Use short press to cycle through themes and test your animation
 * 4. Use long press to pause/resume animation for debugging
 * 5. Check logcat for any validation errors or crashes
 * 6. Test variable timing by observing frame display durations
 *
 * COMMON PITFALLS TO AVOID:
 * =========================
 *
 * ❌ Don't modify the returned frame arrays after generation
 * ❌ Don't forget to validate frameIndex in generateFrame()
 * ❌ Don't use coordinates outside 0-24 range without bounds checking
 * ❌ Don't create very high frame counts (>60) without performance testing
 * ❌ Don't assume specific brightness values - always clamp them
 * ❌ Don't forget to handle edge cases in mathematical calculations
 * ❌ Don't make frameDurations array size different from frames array size
 * ❌ Don't use frame durations outside 50-2000ms range
 *
 * ✅ Do pre-generate frames for smooth performance
 * ✅ Do validate all constructor parameters
 * ✅ Do use GlyphMatrixRenderer utility functions when possible
 * ✅ Do provide meaningful error messages in require() statements
 * ✅ Do test your theme thoroughly before integration
 * ✅ Do follow the existing code style and naming conventions
 * ✅ Do use individual frame durations for sophisticated timing effects
 * ✅ Do validate frame duration arrays match frame count
 */