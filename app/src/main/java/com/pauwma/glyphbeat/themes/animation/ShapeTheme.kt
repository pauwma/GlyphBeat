package com.pauwma.glyphbeat.themes.animation

import android.content.Context
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransition
import com.pauwma.glyphbeat.ui.settings.*
import com.pauwma.glyphbeat.R
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getDropdownValue
import com.pauwma.glyphbeat.ui.settings.CommonSettingValues.getSliderValueFloat

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

    override val errorFrame: IntArray get() = createEmptyFrame() // Error frame stays black

    /**
     * Generate a simple offline pattern that's visible regardless of style.
     * Resolution-aware: adapts to current device grid size.
     */
    private fun generateSimpleOfflinePattern(): IntArray {
        val flatArray = createEmptyFrame()
        val gs = gridSize
        val cx = centerPixel.toDouble()
        val maxDist = resolution.maxRadius.toDouble()
        val innerDist = maxDist - 2.0

        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val flatIndex = row * gs + col
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))

                if (distance <= maxDist && distance >= innerDist) {
                    flatArray[flatIndex] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(
                        77, currentBrightness
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
     * Generate a pattern based on the selected style.
     * Resolution-aware: all patterns adapt to current device grid size.
     */
    private fun generatePatternByStyle(style: String): IntArray {
        val flatArray = createEmptyFrame()
        val gs = gridSize
        val cx = centerPixel.toDouble()
        val maxDist = resolution.maxRadius.toDouble()

        when (style) {
            "cross" -> generateCrossPattern(flatArray, gs, cx, maxDist)
            "dots" -> generateDotsPattern(flatArray, gs, cx, maxDist)
            "lines" -> generateLinesPattern(flatArray, gs, cx, maxDist)
            "border" -> generateBorderPattern(flatArray, gs, cx, maxDist)
            "diamond" -> generateDiamondPattern(flatArray, gs, cx, maxDist)
            "grid" -> generateGridPattern(flatArray, gs, cx, maxDist)
            else -> generateCrossPattern(flatArray, gs, cx, maxDist)
        }

        return flatArray
    }

    private fun setBrightPixel(flatArray: IntArray, index: Int) {
        flatArray[index] = com.pauwma.glyphbeat.core.GlyphMatrixBrightnessModel.calculateFinalBrightness(255, currentBrightness)
    }

    private fun generateCrossPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        val lineHalf = kotlin.math.max(1, (gs * 3 + 24) / 50)  // ~1 pixel half-width
        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                if (distance <= maxDist) {
                    val isVertical = kotlin.math.abs(col - cx.toInt()) <= lineHalf
                    val isHorizontal = kotlin.math.abs(row - cx.toInt()) <= lineHalf
                    if (isVertical || isHorizontal) {
                        setBrightPixel(flatArray, row * gs + col)
                    }
                }
            }
        }
    }

    private fun generateDotsPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        val c = cx.toInt()
        val off = (gs * 6 + 12) / 25  // ~6 for 25x25, ~3 for 13x13
        val dotPositions = listOf(
            Pair(c - off, c - off), Pair(c - off, c), Pair(c - off, c + off),
            Pair(c, c - off), Pair(c, c), Pair(c, c + off),
            Pair(c + off, c - off), Pair(c + off, c), Pair(c + off, c + off)
        )
        for ((dotRow, dotCol) in dotPositions) {
            for (row in (dotRow - 1)..(dotRow + 1)) {
                for (col in (dotCol - 1)..(dotCol + 1)) {
                    if (row in 0 until gs && col in 0 until gs) {
                        val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                        if (distance <= maxDist) {
                            setBrightPixel(flatArray, row * gs + col)
                        }
                    }
                }
            }
        }
    }

    private fun generateLinesPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        // Scale line rows proportionally
        val lineSpacing = kotlin.math.max(2, gs / 6)
        val lineRows = (0 until gs).filter { (it + lineSpacing / 2) % lineSpacing == 0 }

        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                if (distance <= maxDist && row in lineRows) {
                    setBrightPixel(flatArray, row * gs + col)
                }
            }
        }
    }

    private fun generateBorderPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                if (distance <= maxDist && distance >= maxDist - 2) {
                    setBrightPixel(flatArray, row * gs + col)
                }
            }
        }
    }

    private fun generateDiamondPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        val innerDist = (gs * 6.0 / 25.0)
        val outerDist = (gs * 8.0 / 25.0)
        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                if (distance <= maxDist) {
                    val manhattan = kotlin.math.abs(col - cx) + kotlin.math.abs(row - cx)
                    if (manhattan >= innerDist && manhattan <= outerDist) {
                        setBrightPixel(flatArray, row * gs + col)
                    }
                }
            }
        }
    }

    private fun generateGridPattern(flatArray: IntArray, gs: Int, cx: Double, maxDist: Double) {
        val gridSpacing = kotlin.math.max(2, gs / 6)
        for (row in 0 until gs) {
            for (col in 0 until gs) {
                val distance = kotlin.math.sqrt((col - cx) * (col - cx) + (row - cx) * (row - cx))
                if (distance <= maxDist) {
                    if ((row % gridSpacing == 0) || (col % gridSpacing == 0)) {
                        setBrightPixel(flatArray, row * gs + col)
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