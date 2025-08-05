package com.pauwma.glyphbeat.themes.animation

import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.themes.base.AudioReactiveTheme
import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsBuilder
import com.pauwma.glyphbeat.ui.settings.SettingCategories
import com.pauwma.glyphbeat.ui.settings.DropdownOption
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Horizontal waveform visualizer theme.
 * 
 * Displays a single horizontal line that accurately represents the audio waveform in real-time.
 * Similar to professional audio software, with smooth scrolling and accurate amplitude mapping.
 * 
 * Core Features:
 * - Real-time waveform display across canvas width
 * - Centered vertically with positive/negative wave representation
 * - Continuous left-to-right scrolling (oscilloscope style)
 * - Direct audio amplitude to line height correlation
 * 
 * Visual Style:
 * - Clean, thin line (configurable 1-3px thickness)
 * - Solid color with optional subtle glow effect
 * - Smooth anti-aliasing for professional appearance
 * - Optional gradient from center outward
 * 
 * Advanced Features:
 * - Frequency-colored segments option
 * - Configurable time window (2-5 seconds of history)
 * - Multiple display modes (waveform, spectrum, combined)
 * - Smooth pause transitions
 * - Custom offline visualization
 * 
 * @param frameCount Number of frames in the animation loop (default: 32)
 * @param animationSpeed Speed in milliseconds between frames (default: 50ms)
 * @param brightness Maximum brightness (default: 255)
 * @param lineThickness Waveform line thickness (default: 2)
 * @param displayMode Display mode: "waveform", "spectrum", "combined" (default: "waveform")
 * @param enableGlow Enable subtle glow effect (default: true)
 * @param timeWindow Seconds of audio history to display (default: 3.0f)
 * @param sensitivity Audio sensitivity multiplier (default: 1.0f)
 */
class WaveformTheme(
    private val frameCount: Int = 32,
    private val animationSpeed: Long = 50L,
    private val brightness: Int = 255,
    private var lineThickness: Int = 2,
    private var displayMode: String = "waveform",
    private var enableGlow: Boolean = true,
    private var timeWindow: Float = 3.0f,
    private var sensitivity: Float = 1.0f
) : AnimationTheme(), AudioReactiveTheme, ThemeSettingsProvider {
    
    companion object {
        private const val WAVEFORM_BUFFER_SIZE = 75 // 3x display width for smooth scrolling
        private const val SPECTRUM_BANDS = 25
        private const val CENTER_Y = 12
        private const val MAX_AMPLITUDE = 10
        private const val SCROLL_SPEED = 1
    }
    
    init {
        require(frameCount in 16..48) { 
            "Frame count must be between 16 and 48, got $frameCount" 
        }
        require(animationSpeed in 30L..100L) { 
            "Animation speed must be between 30ms and 100ms, got ${animationSpeed}ms" 
        }
        require(lineThickness in 1..3) {
            "Line thickness must be between 1 and 3, got $lineThickness"
        }
        require(timeWindow in 1.0f..5.0f) {
            "Time window must be between 1.0 and 5.0 seconds, got $timeWindow"
        }
        require(sensitivity in 0.5f..2.0f) {
            "Sensitivity must be between 0.5 and 2.0, got $sensitivity"
        }
    }

    override fun getThemeName(): String = "Waveform (WIP)"

    override fun getAnimationSpeed(): Long = animationSpeed

    override fun getBrightness(): Int = brightness

    override fun getDescription(): String = "Reactive audio waveform visualizer"

    // Theme Settings Implementation
    override fun getSettingsId(): String = "waveform_theme"

    // Waveform data buffer for scrolling display
    private val waveformBuffer = FloatArray(WAVEFORM_BUFFER_SIZE) { 0f }
    private var bufferPosition = 0
    private var scrollOffset = 0f
    
    // Spectrum analyzer data
    private val spectrumData = FloatArray(SPECTRUM_BANDS) { 0f }
    private val spectrumHistory = Array(SPECTRUM_BANDS) { FloatArray(25) { 0f } }
    
    // Peak detection for visual emphasis
    private var recentPeak = 0f
    private var peakDecay = 0f
    
    // Custom offline frame - static waveform pattern
    private val offlineFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        
        // Draw a simple sine wave pattern
        for (x in 0 until 25) {
            val phase = x.toFloat() / 25f * 4 * Math.PI
            val y = CENTER_Y + (sin(phase) * 3).toInt()
            
            // Draw line with thickness
            for (t in 0 until lineThickness.coerceAtMost(2)) {
                val yPos = (y + t).coerceIn(0, 24)
                val pixelIndex = yPos * 25 + x
                frame[pixelIndex] = (brightness * 0.3).toInt()
            }
        }
        
        // Add center line
        for (x in 0 until 25) {
            val pixelIndex = CENTER_Y * 25 + x
            frame[pixelIndex] = (brightness * 0.15).toInt()
        }
        
        frame
    }
    
    // Paused state frames - frozen waveform with subtle fade
    private val pausedFrames: Array<IntArray> by lazy {
        Array(8) { i ->
            val frame = createEmptyFrame()
            val fadeAmount = 0.5f + (i.toFloat() / 8f) * 0.3f
            
            // Draw last known waveform state with fade
            for (x in 0 until 25) {
                val bufferIndex = ((bufferPosition - 25 + x + WAVEFORM_BUFFER_SIZE) % WAVEFORM_BUFFER_SIZE)
                val amplitude = waveformBuffer[bufferIndex] * fadeAmount
                val y = CENTER_Y + (amplitude * MAX_AMPLITUDE / 2).toInt()
                
                for (t in 0 until lineThickness) {
                    val yPos = (y + t - lineThickness / 2).coerceIn(0, 24)
                    val pixelIndex = yPos * 25 + x
                    frame[pixelIndex] = (brightness * fadeAmount * 0.6).toInt()
                }
            }
            
            frame
        }
    }
    
    // Fallback animation - scrolling sine wave
    private val fallbackFrames: Array<IntArray> by lazy {
        Array(frameCount) { frameIndex ->
            val frame = createEmptyFrame()
            val scrollPhase = frameIndex.toFloat() / frameCount * 2 * Math.PI
            
            // Draw scrolling waveform
            for (x in 0 until 25) {
                val phase = (x.toFloat() / 25f * 4 * Math.PI) + scrollPhase
                val amplitude = sin(phase) * 0.7f + sin(phase * 2) * 0.3f
                val y = CENTER_Y + (amplitude * 5).toInt()
                
                // Draw with configured thickness
                for (t in 0 until lineThickness) {
                    val yPos = (y + t - lineThickness / 2).coerceIn(0, 24)
                    val pixelIndex = yPos * 25 + x
                    frame[pixelIndex] = (brightness * 0.7).toInt()
                }
            }
            
            // Add subtle center line
            if (enableGlow) {
                for (x in 0 until 25) {
                    val pixelIndex = CENTER_Y * 25 + x
                    frame[pixelIndex] = max(frame[pixelIndex], (brightness * 0.2).toInt())
                }
            }
            
            frame
        }
    }
    
    override fun getFrameCount(): Int = frameCount
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        return fallbackFrames[frameIndex].clone()
    }
    
    override fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray {
        val frame = createEmptyFrame()
        
        // Return offline frame if not playing
        if (!audioData.isPlaying) {
            return offlineFrame.clone()
        }
        
        // Update audio data buffers
        updateAudioBuffers(audioData)
        
        // Draw based on display mode
        when (displayMode) {
            "waveform" -> drawWaveform(frame, audioData)
            "spectrum" -> drawSpectrum(frame, audioData)
            "combined" -> {
                drawWaveform(frame, audioData, 0.6f)
                drawSpectrum(frame, audioData, 0.4f)
            }
        }
        
        // Add peak indicators
        if (recentPeak > 0.8f) {
            drawPeakIndicators(frame)
        }
        
        // Add glow effect if enabled
        if (enableGlow) {
            applyGlowEffect(frame)
        }
        
        return frame
    }
    
    /**
     * Update audio data buffers with new samples
     */
    private fun updateAudioBuffers(audioData: AudioData) {
        // Calculate combined waveform amplitude
        val amplitude = (audioData.beatIntensity * 0.3 + 
                        audioData.bassLevel * 0.4 + 
                        audioData.midLevel * 0.2 + 
                        audioData.trebleLevel * 0.1) * sensitivity
        
        // Add to waveform buffer with smooth interpolation
        val smoothedAmplitude = if (bufferPosition > 0) {
            val prevAmplitude = waveformBuffer[(bufferPosition - 1 + WAVEFORM_BUFFER_SIZE) % WAVEFORM_BUFFER_SIZE]
            prevAmplitude * 0.7f + amplitude.toFloat() * 0.3f
        } else {
            amplitude.toFloat()
        }
        
        waveformBuffer[bufferPosition] = smoothedAmplitude
        bufferPosition = (bufferPosition + 1) % WAVEFORM_BUFFER_SIZE
        
        // Update spectrum data
        updateSpectrumData(audioData)
        
        // Update peak detection
        if (amplitude > recentPeak) {
            recentPeak = amplitude.toFloat()
            peakDecay = 1.0f
        } else {
            recentPeak *= 0.95f
            peakDecay *= 0.9f
        }
        
        // Update scroll position
        scrollOffset += SCROLL_SPEED * (animationSpeed / 50f)
    }
    
    /**
     * Update spectrum analyzer data
     */
    private fun updateSpectrumData(audioData: AudioData) {
        // Simulate frequency bands from audio data
        for (i in 0 until SPECTRUM_BANDS) {
            val bandPosition = i.toFloat() / SPECTRUM_BANDS
            
            val bandLevel = when {
                bandPosition < 0.3f -> audioData.bassLevel * (1 - bandPosition * 3)
                bandPosition < 0.7f -> audioData.midLevel
                else -> audioData.trebleLevel * ((bandPosition - 0.7f) * 3)
            }
            
            // Smooth spectrum data
            spectrumData[i] = spectrumData[i] * 0.7f + bandLevel.toFloat() * 0.3f
            
            // Update spectrum history for waterfall effect
            System.arraycopy(spectrumHistory[i], 1, spectrumHistory[i], 0, 24)
            spectrumHistory[i][24] = spectrumData[i]
        }
    }
    
    /**
     * Draw waveform visualization
     */
    private fun drawWaveform(frame: IntArray, audioData: AudioData, opacity: Float = 1.0f) {
        val baseBrightness = (brightness * opacity).toInt()
        
        // Draw the waveform from buffer
        for (x in 0 until 25) {
            // Get amplitude from circular buffer with proper scrolling
            val bufferIndex = ((bufferPosition - 25 + x + WAVEFORM_BUFFER_SIZE) % WAVEFORM_BUFFER_SIZE)
            val amplitude = waveformBuffer[bufferIndex]
            
            // Create oscillating waveform (positive and negative)
            val wavePhase = (x + scrollOffset).toFloat() / 5f
            val oscillation = sin(wavePhase) * amplitude
            
            // Calculate Y position
            val yOffset = (oscillation * MAX_AMPLITUDE).toInt()
            val y = CENTER_Y + yOffset
            
            // Draw waveform line with thickness
            for (t in 0 until lineThickness) {
                val yPos = (y + t - lineThickness / 2).coerceIn(0, 24)
                val pixelIndex = yPos * 25 + x
                
                // Calculate brightness with distance from center
                val distanceFromCenter = abs(yPos - CENTER_Y)
                val fadeFactor = 1.0f - (distanceFromCenter / 12f) * 0.3f
                val pixelBrightness = (baseBrightness * fadeFactor).toInt()
                
                frame[pixelIndex] = max(frame[pixelIndex], pixelBrightness)
            }
            
            // Add subtle dots at peaks for emphasis
            if (abs(yOffset) > MAX_AMPLITUDE * 0.7) {
                val peakY = y.coerceIn(0, 24)
                val peakIndex = peakY * 25 + x
                frame[peakIndex] = min(255, frame[peakIndex] + (baseBrightness * 0.3).toInt())
            }
        }
        
        // Draw center reference line
        if (displayMode == "waveform") {
            for (x in 0 until 25 step 3) {
                val pixelIndex = CENTER_Y * 25 + x
                frame[pixelIndex] = max(frame[pixelIndex], (baseBrightness * 0.2).toInt())
            }
        }
    }
    
    /**
     * Draw spectrum analyzer visualization
     */
    private fun drawSpectrum(frame: IntArray, audioData: AudioData, opacity: Float = 1.0f) {
        val baseBrightness = (brightness * opacity).toInt()
        
        // Draw spectrum bars or waterfall
        for (band in 0 until SPECTRUM_BANDS) {
            val x = band
            val level = spectrumData[band] * sensitivity
            
            if (displayMode == "spectrum") {
                // Vertical bars from bottom
                val barHeight = (level * 20).toInt().coerceIn(0, 20)
                for (y in (24 - barHeight)..24) {
                    val pixelIndex = y * 25 + x
                    val barBrightness = (baseBrightness * (1 - (24 - y).toFloat() / 20f)).toInt()
                    frame[pixelIndex] = max(frame[pixelIndex], barBrightness)
                }
            } else {
                // Horizontal lines for combined mode
                val y = (CENTER_Y + (band - SPECTRUM_BANDS / 2) * 0.8).toInt().coerceIn(0, 24)
                val lineBrightness = (baseBrightness * level * 0.7).toInt()
                
                for (x2 in 0 until 25) {
                    if ((x2 + band) % 3 == 0) { // Dashed lines
                        val pixelIndex = y * 25 + x2
                        frame[pixelIndex] = max(frame[pixelIndex], lineBrightness)
                    }
                }
            }
        }
    }
    
    /**
     * Draw peak detection indicators
     */
    private fun drawPeakIndicators(frame: IntArray) {
        val peakBrightness = (brightness * peakDecay * 0.8).toInt()
        
        // Top and bottom peak indicators
        for (x in 0 until 25 step 2) {
            if (peakDecay > 0.5f) {
                // Top indicator
                var pixelIndex = 2 * 25 + x
                frame[pixelIndex] = max(frame[pixelIndex], peakBrightness)
                
                // Bottom indicator
                pixelIndex = 22 * 25 + x
                frame[pixelIndex] = max(frame[pixelIndex], peakBrightness)
            }
        }
    }
    
    /**
     * Apply subtle glow effect to the waveform
     */
    private fun applyGlowEffect(frame: IntArray) {
        val tempFrame = frame.clone()
        
        // Simple box blur for glow
        for (y in 1 until 24) {
            for (x in 1 until 24) {
                val index = y * 25 + x
                if (tempFrame[index] > 0) {
                    // Add glow to neighboring pixels
                    val glowAmount = (tempFrame[index] * 0.3).toInt()
                    
                    // Adjacent pixels
                    frame[(y - 1) * 25 + x] = max(frame[(y - 1) * 25 + x], glowAmount)
                    frame[(y + 1) * 25 + x] = max(frame[(y + 1) * 25 + x], glowAmount)
                    frame[y * 25 + (x - 1)] = max(frame[y * 25 + (x - 1)], glowAmount)
                    frame[y * 25 + (x + 1)] = max(frame[y * 25 + (x + 1)], glowAmount)
                }
            }
        }
    }
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "line_thickness",
                displayName = "Line Thickness",
                description = "Waveform line thickness",
                defaultValue = lineThickness,
                minValue = 1,
                maxValue = 3,
                stepSize = 1,
                category = SettingCategories.VISUAL
            )
            .addDropdownSetting(
                id = "display_mode",
                displayName = "Display Mode",
                description = "Visualization style",
                defaultValue = displayMode,
                options = listOf(
                    DropdownOption("waveform", "Waveform", "Horizontal scrolling waveform"),
                    DropdownOption("spectrum", "Spectrum", "Frequency spectrum analyzer"),
                    DropdownOption("combined", "Combined", "Both waveform and spectrum")
                ),
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "enable_glow",
                displayName = "Glow Effect",
                description = "Add subtle glow to waveform",
                defaultValue = enableGlow,
                category = SettingCategories.EFFECTS
            )
            .addSliderSetting(
                id = "time_window",
                displayName = "Time Window",
                description = "Seconds of audio history",
                defaultValue = timeWindow,
                minValue = 1.0f,
                maxValue = 5.0f,
                stepSize = 0.5f,
                unit = "s",
                category = SettingCategories.TIMING
            )
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
            .build()
    }
    
    override fun applySettings(settings: ThemeSettings) {
        lineThickness = settings.getTypedValue("line_thickness", 2)
        displayMode = settings.getTypedValue("display_mode", "waveform")
        enableGlow = settings.getTypedValue("enable_glow", true)
        timeWindow = settings.getTypedValue("time_window", 3.0f)
        sensitivity = settings.getTypedValue("sensitivity", 1.0f)
    }
}