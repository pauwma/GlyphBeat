package com.pauwma.glyphbeat.themes.animation

import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.themes.base.AudioReactiveTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsBuilder
import com.pauwma.glyphbeat.ui.settings.SettingCategories
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Mirrored waveform visualizer theme.
 *
 * Displays vertical frequency bars centered on the middle row, extending both
 * upward and downward symmetrically. Left-to-right maps low-to-high frequencies.
 * A 1px center spine at full brightness is always visible as the base.
 * Resolution-aware: adapts to both 25x25 (Phone 3) and 13x13 (Phone 4a Pro).
 */
class WaveformTheme(
    private val frameCount: Int = 32,
    private val animationSpeed: Long = 50L,
    private val brightness: Int = 255,
    private var sensitivity: Float = 1.0f,
    private var smoothing: Float = 0.5f,
    private var spectrumShift: Int = 6,
    private var mirrorMode: Boolean = false
) : AnimationTheme(), AudioReactiveTheme, ThemeSettingsProvider {

    companion object {
        private const val MIN_BAR_LEVEL = 0.05f // Minimal base so bars are never fully gone
    }

    // Resolution-aware dimensions
    private val columns: Int get() = gridSize
    private val centerY: Int get() = centerPixel
    private val maxHalfHeight: Int get() = centerPixel - 1 // Max extension from center

    init {
        require(frameCount in 16..48) {
            "Frame count must be between 16 and 48, got $frameCount"
        }
        require(animationSpeed in 30L..100L) {
            "Animation speed must be between 30ms and 100ms, got ${animationSpeed}ms"
        }
        require(sensitivity in 0.5f..2.0f) {
            "Sensitivity must be between 0.5 and 2.0, got $sensitivity"
        }
        require(smoothing in 0.1f..0.9f) {
            "Smoothing must be between 0.1 and 0.9, got $smoothing"
        }
    }

    override fun getThemeName(): String = "Waveform"
    override fun getAnimationSpeed(): Long = animationSpeed
    override fun getBrightness(): Int = brightness
    override fun getDescription(): String = "Mirrored audio waveform visualizer"
    override fun getSettingsId(): String = "waveform_theme"

    // Current smoothed bar levels (0.0 - 1.0) — lazily sized for device resolution
    private val barHeights: FloatArray by lazy { FloatArray(columns) { 0f } }

    // Per-column target levels from audio data
    private val targetLevels: FloatArray by lazy { FloatArray(columns) { 0f } }

    // Timing
    private var lastUpdateTime = System.currentTimeMillis()

    // Offline frame — single bright center line
    private val offlineFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        for (col in 0 until columns) {
            frame[centerY * columns + col] = brightness
        }
        frame
    }

    // Static preview frame — generate a waveform preview dynamically for current resolution
    private val staticPreviewFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        val gs = columns
        val cy = centerY

        // Draw center line at full brightness
        for (col in 0 until gs) {
            frame[cy * gs + col] = brightness
        }

        // Draw a static waveform-like pattern
        for (col in 0 until gs) {
            val t = col.toFloat() / (gs - 1).coerceAtLeast(1)
            // Create a bell curve pattern peaking in the middle columns
            val bellHeight = kotlin.math.sin(t * kotlin.math.PI).toFloat()
            val barHeight = (bellHeight * maxHalfHeight * 0.7f).toInt()

            for (offset in -barHeight..barHeight) {
                if (offset == 0) continue
                val y = cy + offset
                if (y in 0 until gs) {
                    val dist = abs(offset).toFloat() / max(1, barHeight)
                    val gradientFactor = 0.5f + 0.5f * dist.pow(0.5f)
                    val pixelBrightness = (brightness * gradientFactor).toInt().coerceIn(0, 255)
                    frame[y * gs + col] = max(frame[y * gs + col], pixelBrightness)
                }
            }
        }
        frame
    }

    override fun getFrameCount(): Int = frameCount

    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        return staticPreviewFrame.clone()
    }

    override fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray {
        val frame = createEmptyFrame()

        val currentTime = System.currentTimeMillis()
        val deltaTime = ((currentTime - lastUpdateTime) / 1000.0f).coerceIn(0.001f, 0.1f)
        val deltaFactor = deltaTime * 60f
        lastUpdateTime = currentTime

        if (!audioData.isPlaying) {
            // Set targets to zero so bars decay smoothly
            for (col in 0 until columns) targetLevels[col] = 0f
        } else {
            updateTargetLevels(audioData)
        }

        updateBarHeights(deltaFactor)
        drawBars(frame)

        return frame
    }

    /**
     * Map audio frequency bands to 25 columns (1:1, equal width).
     *
     * A smooth fade curve dims the left side so the visual center of activity
     * shifts right by [spectrumShift] columns without stretching any bands.
     *
     * Mirror mode: bass in center columns, treble on both edges (symmetric).
     */
    private fun updateTargetLevels(audioData: AudioData) {
        val spectrum = audioData.spectrumBands
        val beat = (audioData.beatIntensity * sensitivity).toFloat().coerceIn(0f, 1f)
        val beatBoost = if (beat > 0.6f) beat * 0.15f else 0f
        val cols = columns
        val cy = centerY
        val maxCol = (cols - 1).coerceAtLeast(1)

        if (spectrum != null && spectrum.size == cols) {
            if (mirrorMode) {
                for (col in 0 until cols) {
                    val distFromCenter = abs(col - cy)
                    val specIdx = (distFromCenter * maxCol / cy).coerceIn(0, maxCol)
                    targetLevels[col] = (spectrum[specIdx] * sensitivity + beatBoost).coerceIn(0f, 1f)
                }
            } else {
                // 1:1 mapping with smooth fade on left side
                for (col in 0 until cols) {
                    val raw = (spectrum[col] * sensitivity + beatBoost).coerceIn(0f, 1f)
                    val fade = if (spectrumShift > 0) (col.toFloat() / spectrumShift).coerceIn(0f, 1f) else 1f
                    targetLevels[col] = raw * fade * fade
                }
            }
        } else {
            val bass = (audioData.bassLevel * sensitivity).toFloat().coerceIn(0f, 1f)
            val mid = (audioData.midLevel * sensitivity).toFloat().coerceIn(0f, 1f)
            val treble = (audioData.trebleLevel * sensitivity).toFloat().coerceIn(0f, 1f)

            for (col in 0 until cols) {
                val t = col.toFloat() / maxCol
                val baseLevel = when {
                    t < 0.33f -> bass * (1f - t * 3f) + mid * (t * 3f)
                    t < 0.66f -> mid * (1f - (t - 0.33f) * 3f) + treble * ((t - 0.33f) * 3f)
                    else -> treble
                }
                val fade = if (spectrumShift > 0) (col.toFloat() / spectrumShift).coerceIn(0f, 1f) else 1f
                targetLevels[col] = (baseLevel + beatBoost).coerceIn(0f, 1f) * fade * fade
            }
        }
    }

    /**
     * Smooth bar heights with exponential interpolation.
     */
    private fun updateBarHeights(deltaFactor: Float) {
        for (col in 0 until columns) {
            val minLevel = if (targetLevels[col] > 0f) MIN_BAR_LEVEL else 0f
            val target = targetLevels[col].coerceAtLeast(minLevel)
            val current = barHeights[col]

            barHeights[col] = if (target > current) {
                // Smooth rise
                val riseLerp = (0.25f + (1f - smoothing) * 0.25f) * deltaFactor
                current + (target - current) * riseLerp.coerceAtMost(0.8f)
            } else {
                // Smooth fall — exponential decay
                val fallRate = 0.92f + smoothing * 0.06f
                val decayed = current * fallRate.pow(deltaFactor)
                if (decayed < 0.01f) 0f else decayed.coerceAtLeast(minLevel)
            }
        }
    }

    /**
     * Draw mirrored bars centered on CENTER_Y.
     * Center pixel is always full brightness (1px base line).
     * Bars extend symmetrically up and down with gradient.
     */
    private fun drawBars(frame: IntArray) {
        val gs = columns
        val cy = centerY
        val mhh = maxHalfHeight

        for (col in 0 until gs) {
            // Always draw center pixel at full brightness
            frame[cy * gs + col] = max(frame[cy * gs + col], brightness)

            val halfHeight = (barHeights[col] * mhh).toInt()
            if (halfHeight < 1) continue

            for (offset in -halfHeight..halfHeight) {
                if (offset == 0) continue // Already drew center
                val y = cy + offset
                if (y !in 0 until gs) continue

                val dist = abs(offset).toFloat() / max(1, halfHeight)

                // Brightest at tips, solid through the bar
                val gradientFactor = 0.5f + 0.5f * dist.pow(0.5f)

                // Boost for high-energy bars
                val intensityBoost = if (barHeights[col] > 0.7f) 1.15f else 1f

                val pixelBrightness = (brightness * gradientFactor * intensityBoost)
                    .toInt().coerceIn(0, 255)

                frame[y * gs + col] = max(frame[y * gs + col], pixelBrightness)
            }
        }
    }

    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "sensitivity",
                displayName = "Sensitivity",
                description = "Audio input sensitivity",
                defaultValue = sensitivity,
                minValue = 0.5f,
                maxValue = 2.0f,
                stepSize = 0.1f,
                category = SettingCategories.AUDIO
            )
            .addSliderSetting(
                id = "smoothing",
                displayName = "Smoothing",
                description = "Bar fall speed (higher = smoother)",
                defaultValue = smoothing,
                minValue = 0.1f,
                maxValue = 0.9f,
                stepSize = 0.1f,
                category = SettingCategories.ANIMATION
            )
            .addToggleSetting(
                id = "mirror_mode",
                displayName = "Mirror Mode",
                description = "Mirror bass to center, treble to edges",
                defaultValue = mirrorMode,
                category = SettingCategories.EFFECTS
            )
            .build()
    }

    override fun applySettings(settings: ThemeSettings) {
        sensitivity = settings.getTypedValue("sensitivity", 1.0f)
        smoothing = settings.getTypedValue("smoothing", 0.5f)
        mirrorMode = settings.getTypedValue("mirror_mode", false)
    }
}
