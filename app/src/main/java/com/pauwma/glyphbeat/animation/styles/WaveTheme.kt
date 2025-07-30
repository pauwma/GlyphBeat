package com.pauwma.glyphbeat.animation.styles

import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.sound.AudioReactiveTheme
import com.pauwma.glyphbeat.GlyphMatrixRenderer

/**
 * Horizontal audio-reactive wave animation theme.
 * 
 * Displays dynamic horizontal sound waves that respond to music frequencies.
 * Creates flowing audio spectrum visualization with separate bass, mid, and treble waves.
 * Each frequency band is positioned at different heights for clear separation.
 * 
 * @param frameCount Number of frames in the animation loop (default: 24)
 * @param maxAmplitude Maximum wave amplitude (default: 6)  
 * @param animationSpeed Speed in milliseconds between frames (default: 60ms)
 * @param brightness Maximum brightness of the waves (default: 255)
 * @param enableSpectrum Whether to show separate frequency bands (default: true)
 */
class WaveTheme(
    private val frameCount: Int = 24,
    private val maxAmplitude: Int = 6,
    private val animationSpeed: Long = 60L,
    private val brightness: Int = 255,
    private val enableSpectrum: Boolean = true
) : AnimationTheme(), AudioReactiveTheme {
    
    init {
        require(frameCount in 12..32) { 
            "Frame count must be between 12 and 32, got $frameCount" 
        }
        require(maxAmplitude in 3..8) { 
            "Max amplitude must be between 3 and 8, got $maxAmplitude" 
        }
        require(animationSpeed in 40L..120L) { 
            "Animation speed must be between 40ms and 120ms for audio responsiveness, got ${animationSpeed}ms" 
        }
    }
    
    // Fallback horizontal wave animation for when no audio is detected
    private val fallbackFrames: Array<IntArray> by lazy {
        GlyphMatrixRenderer.createHorizontalWaveFrames(
            frameCount = frameCount,
            amplitude = maxAmplitude / 2, // Reduced amplitude for fallback
            brightness = (brightness * 0.6).toInt(), // Dimmer for fallback
            wavelength = 1.5f
        )
    }
    
    override fun getFrameCount(): Int = frameCount
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        // Return fallback horizontal wave when no audio data available
        return fallbackFrames[frameIndex].clone()
    }
    
    /**
     * MAIN AUDIO-REACTIVE IMPLEMENTATION
     * 
     * Generates horizontal waves that respond to different frequency bands.
     * Bass frequencies appear at the bottom, treble at the top, with mids in between.
     */
    override fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray {
        val frame = createEmptyFrame()
        
        // Ensure frameIndex is properly used for wave animation consistency
        val normalizedFrameIndex = frameIndex % frameCount
        
        if (enableSpectrum) {
            // Draw separate frequency band waves using the enhanced renderer
            GlyphMatrixRenderer.drawAudioSpectrumWaves(
                grid = frame,
                bassLevel = audioData.bassLevel,
                midLevel = audioData.midLevel,
                trebleLevel = audioData.trebleLevel,
                frameIndex = normalizedFrameIndex,
                frameCount = frameCount,
                maxBrightness = brightness
            )
        } else {
            // Single combined wave based on overall audio intensity
            val combinedIntensity = (audioData.beatIntensity + audioData.bassLevel + 
                                   audioData.midLevel + audioData.trebleLevel) / 4.0
            val dynamicAmplitude = (combinedIntensity * maxAmplitude).toInt().coerceIn(1, maxAmplitude)
            val dynamicBrightness = (brightness * combinedIntensity).toInt()
            
            GlyphMatrixRenderer.drawHorizontalWave(
                grid = frame,
                frameIndex = normalizedFrameIndex,
                frameCount = frameCount,
                amplitude = dynamicAmplitude,
                brightness = dynamicBrightness,
                wavelength = 2.0f,
                thickness = if (combinedIntensity > 0.7) 2 else 1
            )
        }
        
        // Add beat emphasis dots on strong beats
        if (audioData.beatIntensity > 0.8) {
            drawBeatEmphasisDots(frame, audioData)
        }
        
        return frame
    }
    
    /**
     * Adds emphasis dots on strong beats for visual punch
     */
    private fun drawBeatEmphasisDots(frame: IntArray, audioData: AudioData) {
        val dotBrightness = (brightness * audioData.beatIntensity).toInt()
        
        // Add dots at wave intersection points during strong beats
        val emphasisPositions = listOf(
            Pair(6, 4),   // Treble region
            Pair(6, 12),  // Treble center
            Pair(6, 20),  // Treble region
            Pair(12, 2),  // Mid region
            Pair(12, 22), // Mid region
            Pair(18, 8),  // Bass region
            Pair(18, 16)  // Bass region
        )
        
        emphasisPositions.forEach { (row, col) ->
            // Only show dots randomly for flickering effect
            if (kotlin.random.Random.nextFloat() < 0.6f) {
                val pixelIndex = row * 25 + col
                if (pixelIndex in 0 until 625) {
                    frame[pixelIndex] = kotlin.math.max(frame[pixelIndex], dotBrightness)
                }
            }
        }
    }
    
    override fun getThemeName(): String = "Wave"
    
    override fun getAnimationSpeed(): Long = animationSpeed
    
    override fun getBrightness(): Int = brightness
    
    override fun getDescription(): String {
        return if (enableSpectrum) {
            "Horizontal audio spectrum with bass, mid, treble separation"
        } else {
            "Horizontal wave reactive to combined audio intensity"
        }
    }
}