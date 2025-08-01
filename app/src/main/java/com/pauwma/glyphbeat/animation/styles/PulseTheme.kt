package com.pauwma.glyphbeat.animation.styles

import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.sound.AudioReactiveTheme
import com.pauwma.glyphbeat.GlyphMatrixRenderer

/**
 * Audio-reactive pulsing circle animation theme.
 * 
 * Displays a pulsing circle that responds to music playback.
 * Creates dynamic beats that match the rhythm and intensity of the music.
 * 
 * @param frameCount Number of frames in the animation loop (default: 16)
 * @param maxRadius Maximum radius of the pulse (default: 11)
 * @param animationSpeed Speed in milliseconds between frames (default: 80ms)
 * @param brightness Brightness of the circle (default: 255)
 */
class PulseTheme(
    private val frameCount: Int = 16,
    private val maxRadius: Int = 11,
    private val animationSpeed: Long = 80L,
    private val brightness: Int = 255
) : AnimationTheme(), AudioReactiveTheme {
    
    init {
        require(frameCount in 8..24) { 
            "Frame count must be between 8 and 24, got $frameCount" 
        }
        require(maxRadius in 4..12) { 
            "Max radius must be between 4 and 12, got $maxRadius" 
        }
        require(animationSpeed in 50L..150L) { 
            "Animation speed must be between 50ms and 150ms for beat matching, got ${animationSpeed}ms" 
        }
    }
    
    // Generate dynamic beat-responsive frames
    private val frames: Array<IntArray> by lazy {
        Array(frameCount) { frameIndex ->
            val frame = createEmptyFrame()
            val progress = frameIndex.toFloat() / frameCount.toFloat()
            
            // Multiple pulse layers for complex beat patterns
            val mainBeat = kotlin.math.sin(progress * 2 * kotlin.math.PI)
            val subBeat = kotlin.math.sin(progress * 4 * kotlin.math.PI) * 0.4
            val microBeat = kotlin.math.sin(progress * 8 * kotlin.math.PI) * 0.2
            
            // Combine for dynamic rhythm (normalize to 0-1)
            val beatIntensity = kotlin.math.max(0.0, (mainBeat + subBeat + microBeat + 1.4) / 2.8)
            
            // Dynamic radius based on beat intensity
            val currentRadius = (beatIntensity * maxRadius).toInt()
            
            // Multi-layer pulse effect
            for (layer in 1..3) {
                val layerRadius = kotlin.math.max(1, (currentRadius * layer * 0.35).toInt())
                if (layerRadius <= maxRadius) {
                    val layerIntensity = when (layer) {
                        1 -> beatIntensity // Core pulse
                        2 -> beatIntensity * 0.7 // Mid layer
                        3 -> beatIntensity * 0.4 // Outer glow
                        else -> beatIntensity
                    }
                    val layerBrightness = (brightness * layerIntensity).toInt()
                    
                    GlyphMatrixRenderer.drawCircle(
                        frame, 12, 12, layerRadius, 
                        clampBrightness(layerBrightness)
                    )
                }
            }
            
            // Strong beat center dot
            if (beatIntensity > 0.8) {
                val pixelIndex = 12 * 25 + 12
                frame[pixelIndex] = clampBrightness(brightness)
            }
            
            frame
        }
    }
    
    // Custom offline frame - minimalist circle logo
    private val offlineFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        
        // Draw a simple static circle logo
        GlyphMatrixRenderer.drawCircle(frame, 12, 12, 6, (brightness * 0.4).toInt())
        GlyphMatrixRenderer.drawCircle(frame, 12, 12, 3, (brightness * 0.6).toInt())
        GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, brightness)
        
        frame
    }
    
    // Paused state frame - dim pulsing circle
    private val pausedFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        
        // Draw a gentle pulsing circle for paused state
        GlyphMatrixRenderer.drawCircle(frame, 12, 12, 5, (brightness * 0.3).toInt())
        GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, (brightness * 0.5).toInt())
        
        frame
    }
    
    override fun getFrameCount(): Int = frameCount
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        return frames[frameIndex].clone()
    }
    
    override fun getThemeName(): String = "Pulse"
    
    override fun getAnimationSpeed(): Long = animationSpeed
    
    override fun getBrightness(): Int = brightness
    
    override fun getDescription(): String {
        return "Beat-responsive pulsing circles that match music rhythm"
    }
    
    override fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray {
        val frame = createEmptyFrame()
        
        // Use frameIndex for consistent animation timing base
        val animationPhase = frameIndex.toFloat() / frameCount.toFloat()
        val basePulse = kotlin.math.sin(animationPhase * 2 * kotlin.math.PI)
        
        // Enhanced audio intensity calculation with frequency weighting
        val bassWeight = 1.8f  // Emphasize bass for pulse effects
        val midWeight = 1.0f   // Normal mid frequency response
        val trebleWeight = 0.6f // Reduce treble impact for pulse
        
        val weightedIntensity = (
            audioData.beatIntensity + 
            audioData.bassLevel * bassWeight + 
            audioData.midLevel * midWeight + 
            audioData.trebleLevel * trebleWeight
        ) / (1 + bassWeight + midWeight + trebleWeight)
        
        // Combine animation phase with audio intensity for synchronized reactivity
        val normalizedIntensity = kotlin.math.max(0.0, kotlin.math.min(1.0, 
            weightedIntensity * 0.7 + (basePulse + 1.0) * 0.15))
        
        // ENHANCED MULTI-LAYER PULSE SYSTEM
        // Dynamic layer count based on audio intensity
        val maxLayers = when {
            normalizedIntensity < 0.2 -> 2  // Quiet: minimal layers
            normalizedIntensity < 0.5 -> 3  // Moderate: normal layers
            normalizedIntensity < 0.8 -> 4  // Loud: more layers
            else -> 5                       // Very loud: maximum layers
        }
        
        // Create expanding pulse layers with frequency-based effects
        for (layer in 1..maxLayers) {
            val layerProgress = layer.toFloat() / maxLayers
            
            // Calculate layer radius with audio-reactive scaling
            val baseLayerRadius = (normalizedIntensity * maxRadius * layerProgress).toInt()
            val bassBoost = (audioData.bassLevel * maxRadius * 0.3 * layerProgress).toInt()
            val layerRadius = kotlin.math.min(maxRadius, baseLayerRadius + bassBoost)
            
            if (layerRadius > 0) {
                // Frequency-specific layer intensities
                val layerIntensity = when (layer) {
                    1 -> {
                        // Core pulse - maximum intensity with beat emphasis
                        audioData.beatIntensity * 1.3 + audioData.bassLevel * 0.5
                    }
                    2 -> {
                        // Bass layer - strong bass response
                        audioData.bassLevel * 1.2 + audioData.beatIntensity * 0.7
                    }
                    3 -> {
                        // Mid layer - balanced frequencies
                        audioData.midLevel * 1.0 + audioData.beatIntensity * 0.5
                    }
                    4 -> {
                        // Treble layer - high frequency sparkle
                        audioData.trebleLevel * 0.8 + audioData.midLevel * 0.4
                    }
                    5 -> {
                        // Outer glow - subtle enhancement on very loud beats
                        normalizedIntensity * 0.4
                    }
                    else -> normalizedIntensity * 0.5
                }
                
                // Calculate final brightness with layer-specific modulation
                val layerBrightness = (brightness * kotlin.math.min(1.0, layerIntensity)).toInt()
                
                // Only draw layers that are bright enough to be visible
                if (layerBrightness > 8) {
                    // Add layer-specific visual effects
                    when (layer) {
                        1 -> {
                            // Core: solid circle
                            GlyphMatrixRenderer.drawDot(frame, 12, 12, layerRadius, layerBrightness)
                        }
                        else -> {
                            // Outer layers: circle outlines for layered effect
                            GlyphMatrixRenderer.drawCircle(frame, 12, 12, layerRadius, layerBrightness)
                        }
                    }
                }
            }
        }
        
        // ENHANCED CENTER DOT with beat prediction
        if (audioData.beatIntensity > 0.5 || audioData.bassLevel > 0.6) {
            val centerIntensity = kotlin.math.max(audioData.beatIntensity, audioData.bassLevel)
            val centerBrightness = (brightness * centerIntensity).toInt()
            val pixelIndex = 12 * 25 + 12
            frame[pixelIndex] = clampBrightness(centerBrightness)
        }
        
        // ENHANCED SPARKLE SYSTEM with frequency-based positioning
        when {
            normalizedIntensity > 0.85 -> {
                // Maximum sparkles: full pattern
                drawSparklePattern(frame, audioData, SparkleLevel.MAXIMUM)
            }
            normalizedIntensity > 0.7 -> {
                // High sparkles: corner pattern
                drawSparklePattern(frame, audioData, SparkleLevel.HIGH)
            }
            normalizedIntensity > 0.5 && audioData.trebleLevel > 0.6 -> {
                // Treble sparkles: minimal pattern for high frequencies
                drawSparklePattern(frame, audioData, SparkleLevel.TREBLE)
            }
        }
        
        return frame
    }
    
    /**
     * Enhanced sparkle system with different intensity levels
     */
    private enum class SparkleLevel { TREBLE, HIGH, MAXIMUM }
    
    private fun drawSparklePattern(frame: IntArray, audioData: AudioData, level: SparkleLevel) {
        val positions = when (level) {
            SparkleLevel.TREBLE -> listOf(Pair(10, 10), Pair(14, 14))
            SparkleLevel.HIGH -> listOf(Pair(10, 10), Pair(10, 14), Pair(14, 10), Pair(14, 14))
            SparkleLevel.MAXIMUM -> listOf(
                Pair(10, 10), Pair(10, 14), Pair(14, 10), Pair(14, 14),
                Pair(8, 8), Pair(8, 16), Pair(16, 8), Pair(16, 16),
                Pair(6, 12), Pair(18, 12), Pair(12, 6), Pair(12, 18)
            )
        }
        
        val baseIntensity = when (level) {
            SparkleLevel.TREBLE -> audioData.trebleLevel
            SparkleLevel.HIGH -> audioData.beatIntensity
            SparkleLevel.MAXIMUM -> kotlin.math.max(audioData.beatIntensity, audioData.bassLevel)
        }
        
        val sparkleBrightness = (brightness * baseIntensity * 0.7).toInt()
        
        positions.forEach { (row, col) ->
            // Add randomness for flickering effect
            if (kotlin.random.Random.nextFloat() < 0.8f) {
                val pixelIndex = row * 25 + col
                if (pixelIndex in 0 until 625) {
                    frame[pixelIndex] = clampBrightness(sparkleBrightness)
                }
            }
        }
    }
}