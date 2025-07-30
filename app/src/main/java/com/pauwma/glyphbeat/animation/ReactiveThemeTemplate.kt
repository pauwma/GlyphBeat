package com.pauwma.glyphbeat.animation

import com.pauwma.glyphbeat.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.sound.AudioReactiveTheme
import com.pauwma.glyphbeat.GlyphMatrixRenderer

/**
 * AUDIO-REACTIVE THEME TEMPLATE
 * 
 * This template provides a comprehensive guide for creating audio-reactive themes
 * that respond to music and sound in real-time using multiple audio detection methods.
 * 
 * QUICK START GUIDE:
 * ==================
 * 1. Copy this file and rename it (e.g., "MusicSpectrumTheme.kt")
 * 2. Rename the class to match your theme name
 * 3. Customize the sound level thresholds and corresponding visual effects
 * 4. Implement your audio-reactive animation logic
 * 5. Add your theme to MediaPlayerToyService.kt availableThemes list
 * 
 * AUDIO DETECTION SYSTEM:
 * =======================
 * The system uses multiple audio detection methods:
 * - MediaController position tracking for beat estimation
 * - Volume level changes for beat detection
 * - Frequency band simulation (bass, mid, treble)
 * - Android Visualizer API (when available and permitted)
 * - Fallback to simulated audio patterns for demo mode
 * 
 * AUDIO DATA STRUCTURE:
 * =====================
 * AudioData contains:
 * - beatIntensity: 0.0-1.0 (main beat strength)
 * - bassLevel: 0.0-1.0 (low frequency content)
 * - midLevel: 0.0-1.0 (mid frequency content)  
 * - trebleLevel: 0.0-1.0 (high frequency content)
 * - isPlaying: Boolean (whether music is actively playing)
 * 
 * SOUND LEVEL THRESHOLDS:
 * =======================
 * Define different visual effects based on audio intensity levels:
 * - QUIET (0.0-0.2): Subtle animations, low brightness
 * - MODERATE (0.2-0.5): Normal animations, medium brightness
 * - LOUD (0.5-0.8): Enhanced animations, high brightness
 * - VERY_LOUD (0.8-1.0): Maximum effects, full brightness, special effects
 */

/**
 * Template for creating audio-reactive themes with sound level thresholds.
 * 
 * This example demonstrates a multi-level pulsing effect that changes behavior
 * based on different audio intensity thresholds.
 * 
 * @param frameCount Number of frames in the animation loop (8-32 recommended for reactivity)
 * @param animationSpeed Speed between frames in milliseconds (50-150ms for audio sync)
 * @param brightness Maximum brightness for the theme (0-255)
 * @param bassBoost Multiplier for bass frequency response (0.5-2.0)
 * @param enableSparkles Whether to show sparkle effects on very loud beats
 */
class ReactiveThemeTemplate(
    private val frameCount: Int = 16,
    private val animationSpeed: Long = 80L,
    private val brightness: Int = 255,
    private val bassBoost: Float = 1.2f,
    private val enableSparkles: Boolean = true
) : AnimationTheme(), AudioReactiveTheme {
    
    /**
     * AUDIO INTENSITY THRESHOLDS
     * 
     * Define different behavior levels based on audio intensity.
     * Adjust these values to fine-tune responsiveness.
     */
    companion object {
        private const val QUIET_THRESHOLD = 0.15      // Minimal activity
        private const val MODERATE_THRESHOLD = 0.4    // Normal music levels  
        private const val LOUD_THRESHOLD = 0.7        // Strong beats
        private const val VERY_LOUD_THRESHOLD = 0.85  // Peak moments
        
        // Frequency band weights for different effects
        private const val BASS_WEIGHT = 1.5f          // Emphasize bass for beat detection
        private const val MID_WEIGHT = 1.0f           // Normal mid frequency response
        private const val TREBLE_WEIGHT = 0.8f        // Slightly reduce treble impact
    }
    
    /**
     * CONSTRUCTOR VALIDATION
     */
    init {
        require(frameCount in 8..32) { 
            "Frame count must be between 8 and 32 for audio reactivity, got $frameCount" 
        }
        require(animationSpeed in 50L..150L) { 
            "Animation speed must be between 50ms and 150ms for audio sync, got ${animationSpeed}ms" 
        }
        require(bassBoost in 0.5f..2.0f) { 
            "Bass boost must be between 0.5 and 2.0, got $bassBoost" 
        }
    }
    
    /**
     * FALLBACK ANIMATION FRAMES
     * 
     * Pre-generated frames for when no audio is detected or for demo mode.
     * These should provide a reasonable default animation.
     */
    private val fallbackFrames: Array<IntArray> by lazy {
        Array(frameCount) { frameIndex ->
            val frame = createEmptyFrame()
            val progress = frameIndex.toFloat() / frameCount
            
            // Simple pulsing animation for fallback
            val pulseIntensity = kotlin.math.sin(progress * 2 * kotlin.math.PI) * 0.5 + 0.5
            val radius = (pulseIntensity * 8).toInt()
            
            if (radius > 0) {
                GlyphMatrixRenderer.drawCircle(
                    frame, 12, 12, radius, 
                    (brightness * pulseIntensity * 0.6).toInt()
                )
            }
            
            frame
        }
    }
    
    /**
     * REQUIRED IMPLEMENTATIONS
     */
    
    override fun getFrameCount(): Int = frameCount
    
    override fun generateFrame(frameIndex: Int): IntArray {
        validateFrameIndex(frameIndex)
        // Return fallback animation when no audio data available
        return fallbackFrames[frameIndex].clone()
    }
    
    /**
     * MAIN AUDIO-REACTIVE IMPLEMENTATION
     * 
     * This is where the magic happens - converting audio data into visual effects.
     * The method receives real-time audio analysis and generates corresponding visuals.
     */
    override fun generateAudioReactiveFrame(frameIndex: Int, audioData: AudioData): IntArray {
        val frame = createEmptyFrame()
        
        // Calculate combined audio intensity with frequency weighting
        val weightedBass = audioData.bassLevel * BASS_WEIGHT * bassBoost
        val weightedMid = audioData.midLevel * MID_WEIGHT
        val weightedTreble = audioData.trebleLevel * TREBLE_WEIGHT
        val combinedIntensity = (audioData.beatIntensity + weightedBass + weightedMid + weightedTreble) / 4.0
        
        // Clamp to valid range
        val normalizedIntensity = kotlin.math.max(0.0, kotlin.math.min(1.0, combinedIntensity))
        
        // SOUND LEVEL THRESHOLD LOGIC
        when {
            normalizedIntensity < QUIET_THRESHOLD -> {
                drawQuietEffect(frame, normalizedIntensity, audioData)
            }
            normalizedIntensity < MODERATE_THRESHOLD -> {
                drawModerateEffect(frame, normalizedIntensity, audioData)
            }
            normalizedIntensity < LOUD_THRESHOLD -> {
                drawLoudEffect(frame, normalizedIntensity, audioData)
            }
            normalizedIntensity < VERY_LOUD_THRESHOLD -> {
                drawVeryLoudEffect(frame, normalizedIntensity, audioData)
            }
            else -> {
                drawMaximumEffect(frame, normalizedIntensity, audioData)
            }
        }
        
        return frame
    }
    
    /**
     * THRESHOLD-BASED VISUAL EFFECTS
     * 
     * Each method handles a different intensity level with appropriate visual response.
     * Customize these methods to create your desired audio-reactive behavior.
     */
    
    private fun drawQuietEffect(frame: IntArray, intensity: Double, audioData: AudioData) {
        // QUIET: Minimal subtle glow
        val glowBrightness = (brightness * intensity * 0.3).toInt()
        if (glowBrightness > 5) {
            GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, glowBrightness)
        }
    }
    
    private fun drawModerateEffect(frame: IntArray, intensity: Double, audioData: AudioData) {
        // MODERATE: Small pulsing circle
        val radius = (intensity * 4).toInt()
        val pulseBrightness = (brightness * intensity * 0.6).toInt()
        
        if (radius > 0) {
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius, pulseBrightness)
            // Add center dot
            GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, pulseBrightness)
        }
    }
    
    private fun drawLoudEffect(frame: IntArray, intensity: Double, audioData: AudioData) {
        // LOUD: Multi-layer pulsing with frequency separation
        val baseRadius = (intensity * 6).toInt()
        val bassBrightness = (brightness * audioData.bassLevel * 0.8).toInt()
        val midBrightness = (brightness * audioData.midLevel * 0.6).toInt()
        
        // Bass layer (outer)
        if (baseRadius > 0) {
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, baseRadius, bassBrightness)
        }
        
        // Mid layer (inner)
        val midRadius = (baseRadius * 0.6).toInt()
        if (midRadius > 0) {
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, midRadius, midBrightness)
        }
        
        // Center dot
        val centerBrightness = (brightness * intensity).toInt()
        GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, centerBrightness)
    }
    
    private fun drawVeryLoudEffect(frame: IntArray, intensity: Double, audioData: AudioData) {
        // VERY LOUD: Multiple layers + corner accents
        drawLoudEffect(frame, intensity, audioData) // Include loud effects
        
        // Add corner accent dots for very loud beats
        val accentBrightness = (brightness * audioData.beatIntensity * 0.7).toInt()
        val accentPositions = listOf(
            Pair(10, 10), Pair(10, 14), Pair(14, 10), Pair(14, 14)
        )
        
        accentPositions.forEach { (row, col) ->
            GlyphMatrixRenderer.drawDot(frame, col, row, 1, accentBrightness)
        }
    }
    
    private fun drawMaximumEffect(frame: IntArray, intensity: Double, audioData: AudioData) {
        // MAXIMUM: All effects + special sparkles
        drawVeryLoudEffect(frame, intensity, audioData) // Include very loud effects
        
        if (enableSparkles && audioData.beatIntensity > 0.9) {
            // Add sparkle effects on maximum beats
            val sparklePositions = listOf(
                Pair(8, 8), Pair(8, 16), Pair(16, 8), Pair(16, 16),
                Pair(6, 12), Pair(18, 12), Pair(12, 6), Pair(12, 18)
            )
            
            val sparkleBrightness = (brightness * 0.8).toInt()
            sparklePositions.forEach { (row, col) ->
                // Only show sparkles randomly for flickering effect
                if (kotlin.random.Random.nextFloat() < 0.7f) {
                    val pixelIndex = row * 25 + col
                    if (pixelIndex in 0 until 625) {
                        frame[pixelIndex] = sparkleBrightness
                    }
                }
            }
        }
    }
    
    /**
     * THEME PROPERTIES
     */
    
    override fun getThemeName(): String = "Reactive Template"
    
    override fun getAnimationSpeed(): Long = animationSpeed
    
    override fun getBrightness(): Int = brightness
    
    override fun getDescription(): String {
        return "Multi-level audio reactive template with sound thresholds"
    }
    
    /**
     * ADVANCED CUSTOMIZATION METHODS
     * 
     * Add these methods for more sophisticated reactive themes.
     */
    
    /**
     * Get the current audio intensity category for external reference
     */
    fun getIntensityCategory(audioData: AudioData): String {
        val intensity = calculateCombinedIntensity(audioData)
        return when {
            intensity < QUIET_THRESHOLD -> "QUIET"
            intensity < MODERATE_THRESHOLD -> "MODERATE"
            intensity < LOUD_THRESHOLD -> "LOUD"
            intensity < VERY_LOUD_THRESHOLD -> "VERY_LOUD"
            else -> "MAXIMUM"
        }
    }
    
    /**
     * Calculate weighted audio intensity combining all frequency bands
     */
    private fun calculateCombinedIntensity(audioData: AudioData): Double {
        val weightedBass = audioData.bassLevel * BASS_WEIGHT * bassBoost
        val weightedMid = audioData.midLevel * MID_WEIGHT
        val weightedTreble = audioData.trebleLevel * TREBLE_WEIGHT
        val combined = (audioData.beatIntensity + weightedBass + weightedMid + weightedTreble) / 4.0
        return kotlin.math.max(0.0, kotlin.math.min(1.0, combined))
    }
    
    /**
     * Check if current audio qualifies as a "beat" for rhythm-based effects
     */
    fun isBeatMoment(audioData: AudioData): Boolean {
        return audioData.beatIntensity > 0.6 && audioData.bassLevel > 0.4
    }
    
    /**
     * Get suggested frame switching interval based on detected BPM
     */
    fun getAdaptiveAnimationSpeed(audioData: AudioData): Long {
        // Faster animation for higher energy music
        val energyFactor = calculateCombinedIntensity(audioData)
        val speedMultiplier = 1.0 - (energyFactor * 0.3) // 30% faster at max energy
        return (animationSpeed * speedMultiplier).toLong()
    }
}

/**
 * INTEGRATION INSTRUCTIONS:
 * =========================
 * 
 * 1. Copy this file and rename it to your theme name
 * 2. Customize the threshold values and visual effects
 * 3. Implement your specific audio-reactive animation logic
 * 4. Add to MediaPlayerToyService.kt:
 *    
 *    import com.pauwma.glyphbeat.animation.styles.YourReactiveTheme
 *    
 *    private val availableThemes = listOf(
 *        VinylRecordTheme(),
 *        PulseTheme(),
 *        YourReactiveTheme(bassBoost = 1.5f, enableSparkles = true),
 *        // ... other themes
 *    )
 * 
 * TESTING AUDIO-REACTIVE THEMES:
 * ==============================
 * 
 * 1. Build and install: ./gradlew installDebug
 * 2. Play music from any app (Spotify, YouTube Music, etc.)
 * 3. Enable your service in Nothing device settings
 * 4. Test different music genres to validate responsiveness:
 *    - Electronic/EDM: Strong beats, good for testing beat detection
 *    - Classical: Dynamic range, good for testing intensity levels
 *    - Rock: Mixed frequencies, good for testing band separation
 *    - Jazz: Irregular patterns, good for testing adaptivity
 * 
 * ADVANCED AUDIO-REACTIVE PATTERNS:
 * =================================
 * 
 * 1. **Frequency-Based Positioning**: Use bass for bottom pixels, treble for top
 * 2. **Beat Prediction**: Analyze beat patterns to predict next hits
 * 3. **Genre Adaptation**: Different visual styles for different music types
 * 4. **Energy Accumulation**: Build up effects over time for crescendos
 * 5. **Harmonic Response**: React to chord changes and harmonies
 * 6. **Rhythm Quantization**: Snap effects to musical time signatures
 * 
 * PERFORMANCE OPTIMIZATION:
 * =========================
 * 
 * - Pre-calculate expensive operations when possible
 * - Use integer math instead of floating-point where applicable
 * - Cache frequently used calculations
 * - Limit the number of pixels drawn per frame
 * - Consider frame skipping during very high intensity moments
 * 
 * COMMON PITFALLS:
 * ================
 * 
 * ❌ Over-sensitive reactions causing flickering
 * ❌ Under-sensitive reactions appearing non-responsive  
 * ❌ Ignoring frequency band separation
 * ❌ Not handling silence gracefully
 * ❌ Performance issues during peak audio moments
 * 
 * ✅ Smooth threshold transitions with hysteresis
 * ✅ Balanced sensitivity across all music types
 * ✅ Graceful fallback when audio is unavailable
 * ✅ Efficient rendering optimized for 60fps
 * ✅ Comprehensive testing with various audio sources
 */