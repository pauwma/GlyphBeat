package com.pauwma.glyphbeat.themes.animation

import kotlin.math.sqrt
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransition
import com.pauwma.glyphbeat.ui.settings.*

/**
 * Dancing Duck theme extending ThemeTemplate with comprehensive features.
 * Features a cute dancing duck animation with individual frame durations.
 * Now extends ThemeTemplate for enhanced features including state-specific frames
 * and supports customizable settings.
 */
class DancingDuckTheme : ThemeTemplate(), ThemeSettingsProvider {
    
    // Settings-driven properties with default values
    private var currentDancingSpeed: Long = 150L
    private var currentAnimationPattern: String = "normal" // Default should have transitions
    private var currentBrightness: Int = 255
    
    // =================================================================================
    // THEME METADATA - Dancing duck theme information
    // =================================================================================
    
    override val titleTheme: String = "Dancing Duck"
    override val descriptionTheme: String = "He knows exactly what he's doing."
    override val authorName: String = "pauwma"
    override val version: String = "1.0.0" 
    override val category: String = "Fun"
    override val tags: Array<String> = arrayOf("duck", "dancing", "cute", "animated", "fun", "character")
    override val createdDate: Long = 1640995200000L // January 1, 2022
    
    // =================================================================================
    // ANIMATION PROPERTIES
    // =================================================================================
    
    override val animationSpeedValue: Long = 150L // Moderate speed for duck dancing
    override val brightnessValue: Int = 255
    override val loopMode: String = "normal"
    override val complexity: String = "Medium"
    
    // Individual frame durations for dancing rhythm (2 frames)
    override val frameDurations: LongArray? = null
    
    // =================================================================================
    // BEHAVIOR SETTINGS
    // =================================================================================
    
    override val isReactive: Boolean = false
    override val supportsFadeTransitions: Boolean = true
    
    // =================================================================================
    // TECHNICAL METADATA
    // =================================================================================
    
    override val compatibilityVersion: String = "1.0.0"
    override val frameDataFormat: String = "shaped" // Uses circular matrix layout

    // =================================================================================
    // ANIMATION FRAMES - 2 frames of dancing duck
    // =================================================================================

    override val frames = arrayOf(
        // Frame 1 (F0) - Up Position
        intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,255,0,0,0,255,0),

        // Frame 2 (F1) - Down Position
        intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,255,0,0,0,255,0),

        // Frame 3 (F2) - Head move to front
        intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,255,0,255,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,255,0,255,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,255,0,0,0,255,0),

        // Frame 4 (F3) - Head down
        intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,255,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,255,0,0,0,255,0)
    )

    // Frame transitions will be provided by the getter below based on settings


    // =================================================================================
    // STATE-SPECIFIC FRAMES - Using appropriate frames for different states with brightness support
    // =================================================================================
    
    // Base frames for state-specific displays (without brightness applied)
    private val basePausedFrame = intArrayOf(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,0,0,255,0,0,255,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,0,0,255,0,0,255,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,255,125,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,255,255,255,125,0,0,255,0,0,0,0,0,0,0,0,0,0,200,200,0,0,0,255,0,0,255,255,255,125,0,0,255,0,0,0,0,0,0,0,0,0,200,0,0,200,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,200,0,0,255,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,0,0,0,200,200,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,200,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,255,0,0,0,0,255,0,0,0,200,0,0,0,0,255,0,0,0,0,0,0,255,255,255,0,0,0,0,0,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,255,255,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,0,0,0,0,255,0,0,0,255,0,0,0,0,255,0,0,0,255,0)
    private val baseOfflineFrame = intArrayOf(0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,0,129,255,255,255,138,0,1,0,0,0,0,0,0,0,0,255,177,0,0,0,166,255,0,1,0,0,0,0,0,0,0,0,255,44,0,17,0,0,0,30,255,0,0,0,0,0,0,0,0,0,0,255,30,0,225,117,0,0,0,0,2,255,0,0,0,0,0,0,0,0,0,118,202,0,144,146,0,0,1,0,0,0,174,142,0,0,0,0,0,0,0,0,0,255,0,0,255,0,0,0,0,0,1,0,0,255,0,0,0,0,0,0,0,0,0,97,205,0,189,89,0,0,0,0,0,1,222,0,189,112,0,0,0,0,0,0,0,0,0,233,36,1,255,0,0,0,0,0,0,0,0,0,18,252,1,0,1,0,0,0,0,0,0,0,255,0,1,168,0,0,0,0,1,0,0,0,0,0,255,0,0,0,0,0,0,1,0,0,1,255,0,0,0,0,0,1,1,0,0,0,72,207,0,255,0,0,0,0,0,0,0,1,1,0,223,1,0,1,0,0,0,0,0,0,0,0,16,0,208,0,1,1,0,0,0,0,0,0,0,202,0,0,0,0,0,0,0,0,0,0,0,0,0,182,0,0,0,0,0,0,0,0,0,17,229,0,0,0,0,0,0,0,1,0,0,0,0,0,209,0,0,0,0,0,1,0,0,0,0,255,1,0,0,0,0,0,0,0,1,1,0,0,0,252,0,1,0,0,0,0,0,1,0,255,0,0,0,0,0,1,0,89,0,0,0,0,0,255,0,0,1,0,0,1,1,0,169,93,1,226,0,1,1,0,90,0,1,124,12,74,178,0,0,0,0,0,0,0,0,255,0,0,0,0,0,62,46,0,1,0,0,255,0,0,0,0,0,0,0,0,69,255,0,0,255,0,0,0,0,0,0,255,84,0,0,0,0,0,0,0,0,61,255,170,0,1,0,0,1,162,255,68,0,0,1,0,1,1,0,0,0,138,255,255,254,255,255,145,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0)
    
    // Dynamic state frames that apply current brightness
    override val pausedFrame: IntArray
        get() = applyBrightnessToFrame(basePausedFrame)
    
    override val offlineFrame: IntArray
        get() = applyBrightnessToFrame(baseOfflineFrame)
    
    override val loadingFrame: IntArray
        get() = applyBrightnessToFrame(frames[0])
    
    override val errorFrame: IntArray = IntArray(625) { 0 } // Error frame stays black
    
    /**
     * Apply current brightness setting to any frame array
     */
    private fun applyBrightnessToFrame(baseFrame: IntArray): IntArray {
        return baseFrame.map { originalValue ->
            if (originalValue > 0) {
                ((originalValue * currentBrightness) / 255).coerceIn(0, 255)
            } else {
                0
            }
        }.toIntArray()
    }
    
    // Validation is handled by parent ThemeTemplate class
    
    // =================================================================================
    // OVERRIDDEN METHODS - Convert shaped data to flat array format
    // =================================================================================
    
    override fun getFrameCount(): Int = framesCount

    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = currentDancingSpeed
    override fun getBrightness(): Int = currentBrightness
    override fun getDescription(): String = descriptionTheme
    
    // =================================================================================
    // THEME SETTINGS PROVIDER IMPLEMENTATION
    // =================================================================================
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addDropdownSetting(
                id = "animation_pattern",
                displayName = "Animation Pattern",
                description = "Style of duck dancing",
                defaultValue = "normal",
                optionsMap = mapOf(
                    "normal" to "Normal",
                    "head" to "Nodders",
                    "bouncy" to "Bouncy",
                    "fast" to "Fast"
                ),
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = "duck_brightness",
                displayName = "Brightness",
                description = "Brightness of the dancing duck",
                defaultValue = 255,
                minValue = 10,
                maxValue = 255,
                stepSize = 5,
                unit = null,
                category = SettingCategories.VISUAL
            )
            .build()
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Store previous pattern to detect changes
        val previousPattern = currentAnimationPattern
        
        // Apply animation pattern
        currentAnimationPattern = settings.getDropdownValue("animation_pattern", "normal")
        
        // Apply brightness with validation
        val brightness = settings.getSliderValueInt("duck_brightness", 255)
        currentBrightness = brightness.coerceIn(10, 255)
        
        // Update dancing speed based on pattern (since we don't have a separate speed setting)
        currentDancingSpeed = when (currentAnimationPattern) {
            "fast" -> 70L
            "bouncy" -> 120L
            "head" -> 120L
            else -> 150L // normal
        }
        
        // Log settings change for debugging
        if (previousPattern != currentAnimationPattern) {
            android.util.Log.d("DancingDuckTheme", "Animation pattern changed from '$previousPattern' to '$currentAnimationPattern'")
            android.util.Log.d("DancingDuckTheme", "Frame transitions available: ${frameTransitions != null}")
            android.util.Log.d("DancingDuckTheme", "Frame transitions count: ${frameTransitions?.size ?: 0}")
        }
    }
    
    // Override frame transitions based on pattern setting
    override val frameTransitions: List<FrameTransition>?
        get() {
            val transitions = when (currentAnimationPattern) {
            "fast" -> listOf(
                FrameTransition(0, 1, 10, 70L),
                FrameTransition(0, 2, 10, 70L),
                FrameTransition(0, 1, 10, 70L),
                FrameTransition(1, 3, 10, 70L)
            )
            "bouncy" -> listOf(
                FrameTransition(0, 1, 6, 200L), // Slow bounces
                FrameTransition(1, 3, 6, 200L)
            )
            "head" -> listOf(
                FrameTransition(0, 2, 10, 150L),
            )
            else -> listOf( // Normal pattern - simple back and forth
                FrameTransition(0, 1, 10, 150L),
                FrameTransition(0, 2, 10, 150L),
                FrameTransition(0, 1, 10, 150L),
                FrameTransition(1, 3, 10, 150L)
            )
        }
        
        android.util.Log.v("DancingDuckTheme", "frameTransitions getter called: pattern='$currentAnimationPattern', transitions=${transitions?.size ?: 0}")
        return transitions
    }
    
    /**
     * Helper method to check if frame transitions have changed
     * This could be used by the service to detect when transitions need reinitialization
     */
    fun getTransitionSignature(): String {
        return "pattern:$currentAnimationPattern"
    }
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        
        // Convert shaped grid data to flat 25x25 array with circular masking
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
                val distance = sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))
                
                if (distance <= 12.5) { // Within the circular shape
                    if (shapedIndex < shapedData.size) {
                        // Apply brightness adjustment
                        val originalValue = shapedData[shapedIndex]
                        val adjustedValue = if (originalValue > 0) {
                            ((originalValue * currentBrightness) / 255).coerceIn(0, 255)
                        } else {
                            0
                        }
                        flatArray[flatIndex] = adjustedValue
                        shapedIndex++
                    }
                }
            }
        }
        
        return flatArray
    }
    
    // All theme metadata and utility methods are inherited from ThemeTemplate parent class
    // No need to redefine them here - they automatically use the private properties defined above
}