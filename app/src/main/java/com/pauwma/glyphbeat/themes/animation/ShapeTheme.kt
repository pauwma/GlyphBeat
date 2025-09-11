package com.pauwma.glyphbeat.themes.animation

import android.content.Context
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransition
import com.pauwma.glyphbeat.ui.settings.*
import com.pauwma.glyphbeat.R

/**
 * Minimal Theme - A static, single-frame theme with state-specific frames.
 *
 * This theme displays a simple pattern without animation, perfect for users who want
 * a consistent visual without motion. It includes different frames for various states:
 * - Main frame: Bright static pattern
 * - Paused frame: Dimmed version of the pattern
 * - Offline frame: Dark with minimal accent pattern
 */
class ShapeTheme(private val ctx: Context) : ThemeTemplate(), ThemeSettingsProvider {

    // =================================================================================
    // THEME METADATA
    // =================================================================================

    override val titleTheme: String = ctx.getString(R.string.theme_shapes_title)
    override val descriptionTheme: String = ctx.getString(R.string.theme_shapes_desc)

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
    // STATE-SPECIFIC FRAMES - Using dynamic patterns with brightness support
    // =================================================================================

    // Dynamic state frames that apply current brightness and pattern
    override val pausedFrame: IntArray
        get() = generatePatternByStyle(currentPatternStyle).map { value ->
            if (value > 0) (value * pausedOpacity).toInt().coerceIn(0, 255) else 0
        }.toIntArray()

    override val offlineFrame: IntArray
        get() = generateSimpleOfflinePattern()

    override val loadingFrame: IntArray
        get() = generatePatternByStyle(currentPatternStyle).map { value ->
            if (value > 0) (value * 0.7).toInt().coerceIn(0, 255) else 0
        }.toIntArray()

    override val errorFrame: IntArray = IntArray(625) { 0 } // Error frame stays black

    /**
     * Generate a simple offline pattern that's visible regardless of style
     */
    private fun generateSimpleOfflinePattern(): IntArray {
        val flatArray = createEmptyFrame()
        val centerX = 12.0
        val centerY = 12.0
        // Simple border pattern for offline state
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Create simple border for offline
                if (distance <= 12.5 && distance >= 10.5) {
                    // Apply brightness using unified model with reduced base value for offline
                    flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        77, // Reduced base value (255 * 0.3) for dimmer offline appearance
                        currentBrightness
                    )
                }
            }
        }

        return flatArray
    }

    // =================================================================================
    // OVERRIDDEN METHODS - Convert shaped data to flat array format
    // =================================================================================

    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)

        // Generate pattern based on current style
        return generatePatternByStyle(currentPatternStyle)
    }

    /**
     * Generate a pattern based on the selected style
     */
    private fun generatePatternByStyle(style: String): IntArray {
        val flatArray = createEmptyFrame()
        val centerX = 12.0
        val centerY = 12.0
        val radius = 12.5

        when (style) {
            "cross" -> generateCrossPattern(flatArray, centerX, centerY, radius)
            "dots" -> generateDotsPattern(flatArray, centerX, centerY, radius)
            "lines" -> generateLinesPattern(flatArray, centerX, centerY, radius)
            "border" -> generateBorderPattern(flatArray, centerX, centerY, radius)
            "diamond" -> generateDiamondPattern(flatArray, centerX, centerY, radius)
            "grid" -> generateGridPattern(flatArray, centerX, centerY, radius)
            else -> generateCrossPattern(flatArray, centerX, centerY, radius) // Default fallback
        }

        return flatArray
    }

    /**
     * Generate cross pattern (original)
     */
    private fun generateCrossPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                if (distance <= radius) {
                    // Create cross pattern
                    val isVerticalLine = col >= 11 && col <= 13
                    val isHorizontalLine = row >= 11 && row <= 13

                    if (isVerticalLine || isHorizontalLine) {
                        // Apply brightness using unified model with full base value (255)
                        flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                            255, // Full brightness base value for the shape
                            currentBrightness
                        )
                    }
                }
            }
        }
    }

    /**
     * Generate dots pattern
     */
    private fun generateDotsPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        val dotPositions = listOf(
            Pair(6, 6), Pair(6, 12), Pair(6, 18),
            Pair(12, 6), Pair(12, 12), Pair(12, 18),
            Pair(18, 6), Pair(18, 12), Pair(18, 18)
        )

        for ((dotRow, dotCol) in dotPositions) {
            for (row in (dotRow - 1)..(dotRow + 1)) {
                for (col in (dotCol - 1)..(dotCol + 1)) {
                    if (row in 0 until 25 && col in 0 until 25) {
                        val flatIndex = row * 25 + col
                        val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                        if (distance <= radius) {
                            // Apply brightness using unified model with full base value (255)
                            flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                                255, // Full brightness base value for the shape
                                currentBrightness
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate horizontal lines pattern
     */
    private fun generateLinesPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        val lineRows = listOf(4, 8, 12, 16, 20)

        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                if (distance <= radius && row in lineRows) {
                    // Apply brightness using unified model with full base value (255)
                    flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        255, // Full brightness base value for the shape
                        currentBrightness
                    )
                }
            }
        }
    }

    /**
     * Generate border pattern
     */
    private fun generateBorderPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                // Create border effect - only pixels near the edge
                if (distance <= radius && distance >= radius - 2) {
                    // Apply brightness using unified model with full base value (255)
                    flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        255, // Full brightness base value for the shape
                        currentBrightness
                    )
                }
            }
        }
    }

    /**
     * Generate diamond pattern
     */
    private fun generateDiamondPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                if (distance <= radius) {
                    // Create diamond shape using Manhattan distance
                    val manhattanDistance = kotlin.math.abs(col - centerX) + kotlin.math.abs(row - centerY)

                    if (manhattanDistance >= 6 && manhattanDistance <= 8) {
                        // Apply brightness using unified model with full base value (255)
                        flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                            255, // Full brightness base value for the shape
                            currentBrightness
                        )
                    }
                }
            }
        }
    }

    /**
     * Generate grid pattern
     */
    private fun generateGridPattern(flatArray: IntArray, centerX: Double, centerY: Double, radius: Double) {
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val flatIndex = row * 25 + col
                val distance = kotlin.math.sqrt((col - centerX) * (col - centerX) + (row - centerY) * (row - centerY))

                if (distance <= radius) {
                    // Create grid pattern
                    val isGridLine = (row % 4 == 0) || (col % 4 == 0)

                    if (isGridLine) {
                        // Apply brightness using unified model with full base value (255)
                        flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                            255, // Full brightness base value for the shape
                            currentBrightness
                        )
                    }
                }
            }
        }
    }

    override fun getThemeName(): String = titleTheme
    override fun getAnimationSpeed(): Long = animationSpeedValue
    override fun getDescription(): String = descriptionTheme

    // =================================================================================
    // THEME SETTINGS PROVIDER IMPLEMENTATION
    // =================================================================================

    // Mutable properties for settings that can be changed at runtime
    private var currentBrightness: Int = brightnessValue
    private var currentPatternStyle: String = "cross"
    private var pausedOpacity: Float = 0.5f

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addDropdownSetting(
                id = "pattern_style",
                displayName = ctx.getString(R.string.set_shapes_pattern_title),
                description = ctx.getString(R.string.set_shapes_pattern_desc),
                defaultValue = "cross",
                optionsMap = mapOf(
                    "cross" to ctx.getString(R.string.set_shapes_pattern_cross),
                    "dots" to ctx.getString(R.string.set_shapes_pattern_dots),
                    "lines" to ctx.getString(R.string.set_shapes_pattern_lines),
                    "border" to ctx.getString(R.string.set_shapes_pattern_border),
                    "diamond" to ctx.getString(R.string.set_shapes_pattern_diamond),
                    "grid" to ctx.getString(R.string.set_shapes_pattern_grid)
                ),
                category = SettingCategories.LAYOUT
            )
            .addSliderSetting(
                id = CommonSettingIds.BRIGHTNESS,
                displayName = ctx.getString(R.string.set_brightness_title),
                description = ctx.getString(R.string.set_brightness_desc),
                defaultValue = 1.0f,
                minValue = 0.1f,
                maxValue = 1.0f,
                stepSize = 0.1f,
                unit = null,
                category = SettingCategories.VISUAL,
                showValue = true
            )
            .addSliderSetting(
                id = "paused_opacity",
                displayName = ctx.getString(R.string.set_paused_opacity_title),
                description = ctx.getString(R.string.set_paused_opacity_desc),
                defaultValue = 0.5f,
                minValue = 0.1f,
                maxValue = 1.0f,
                stepSize = 0.1f,
                unit = null,
                category = SettingCategories.VISUAL,
                showValue = true
            )
            .build()
    }

    override fun applySettings(settings: ThemeSettings) {
        // Apply brightness setting (convert multiplier to 0-255 range)
        val brightnessMultiplier = settings.getSliderValueFloat(CommonSettingIds.BRIGHTNESS, 1.0f)
        currentBrightness = (brightnessMultiplier * 255).toInt().coerceIn(0, 255)

        // Apply paused opacity setting
        pausedOpacity = settings.getSliderValueFloat("paused_opacity", 0.5f)
            .coerceIn(0.1f, 1.0f)

        // Apply pattern style setting
        currentPatternStyle = settings.getDropdownValue("pattern_style", "cross")
    }

    override fun getSettingsId(): String = "minimal_theme"

    /**
     * Override getBrightness to return current user-configured brightness
     */
    override fun getBrightness(): Int = currentBrightness

    /**
     * Get the current pattern style for external use
     */
    fun getCurrentPatternStyle(): String = currentPatternStyle
}