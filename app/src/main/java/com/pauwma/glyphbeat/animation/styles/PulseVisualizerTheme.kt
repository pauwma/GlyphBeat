package com.pauwma.glyphbeat.animation.styles

import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.sound.AudioReactiveTheme
import com.pauwma.glyphbeat.GlyphMatrixRenderer
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsBuilder
import com.pauwma.glyphbeat.ui.settings.SettingCategories
import com.pauwma.glyphbeat.ui.settings.DropdownOption
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Minimalist audio-reactive pulsing circle/sphere visualizer.
 * 
 * Features a single, centered circle that pulses and transforms based on audio input.
 * Creates a hypnotic focal point with smooth transitions and advanced effects.
 * 
 * Core Features:
 * - Centered circle with dynamic scaling (0.3x to 2.5x)
 * - Smooth cubic-bezier and elastic easing transitions
 * - Beat-synchronized pulsing with configurable sensitivity
 * - Clean, minimalist aesthetic
 * 
 * Advanced Features:
 * - Ring effects that emanate outward on beat detection
 * - Particle system for high-energy moments
 * - Sharp scale jumps on detected beats
 * - Smooth interpolation between audio samples
 * - Custom offline and paused state frames
 * 
 * @param frameCount Number of frames in the animation loop (default: 16)
 * @param animationSpeed Speed in milliseconds between frames (default: 80ms)
 * @param brightness Maximum brightness (default: 255)
 * @param sensitivity Audio sensitivity multiplier (default: 1.0f)
 * @param enableRings Enable concentric ring effects (default: true)
 * @param enableParticles Enable particle effects (default: true)
 * @param easingType Type of easing to use for transitions (default: "cubic")
 */
class PulseVisualizerTheme(
    private val frameCount: Int = 16,
    private val animationSpeed: Long = 80L,
    private val brightness: Int = 255,
    private var sensitivity: Float = 1.0f,
    private var enableRings: Boolean = true,
    private var enableParticles: Boolean = true,
    private var easingType: String = "cubic",
    private var visualStyle: String = "big_sphere"
) : AnimationTheme(), AudioReactiveTheme, ThemeSettingsProvider {
    
    // Configuration limits
    companion object {
        private const val MIN_SCALE = 0.3f
        private const val MAX_SCALE = 2.5f
        private const val BEAT_THRESHOLD = 0.7
        private const val PARTICLE_THRESHOLD = 0.85
        private const val RING_FADE_RATE = 0.92f
        private const val MAX_RINGS = 4
        private const val MAX_PARTICLES = 12
        
        // Style-specific scale limits
        private const val SPHERE_MIN_SCALE = 0.5f
        private const val SPHERE_MAX_SCALE = 1.2f
        private const val SPHERE_BASE_RADIUS = 10
    }
    
    init {
        require(frameCount in 8..32) { 
            "Frame count must be between 8 and 32, got $frameCount" 
        }
        require(animationSpeed in 40L..150L) { 
            "Animation speed must be between 40ms and 150ms, got ${animationSpeed}ms" 
        }
        require(sensitivity in 0.5f..2.0f) {
            "Sensitivity must be between 0.5 and 2.0, got $sensitivity"
        }
    }

    override fun getThemeName(): String = "Pulse (WIP)"

    override fun getAnimationSpeed(): Long = animationSpeed

    override fun getBrightness(): Int = brightness

    override fun getDescription(): String {
        return "Reactive visualizer with smooth pulsing effects"
    }

    // Theme Settings Implementation
    override fun getSettingsId(): String = "pulse_visualizer_theme"

    
    // State tracking for advanced effects
    private var currentScale = 1.0f
    private var targetScale = 1.0f
    private var ringEffects = mutableListOf<RingEffect>()
    private var particles = mutableListOf<Particle>()
    private var lastBeatTime = 0L
    private var beatHistory = mutableListOf<Float>()
    
    // Data classes for effects
    private data class RingEffect(val radius: Float, val opacity: Float, val speed: Float = 0.8f)
    private data class Particle(
        val x: Float, 
        val y: Float, 
        val vx: Float, 
        val vy: Float, 
        val life: Float,
        val size: Float = 1f
    )
    
    // Custom offline frame - style-dependent logo
    private val offlineFrame: IntArray by lazy {
        val frame = createEmptyFrame()
        
        when (visualStyle) {
            "big_sphere" -> {
                // Large static sphere with gradient
                drawStaticSphere(frame, 12, 12, 8, brightness * 0.4)
            }
            "minimalist_circle" -> {
                // Three concentric circles for a clean logo
                GlyphMatrixRenderer.drawCircle(frame, 12, 12, 8, (brightness * 0.2).toInt())
                GlyphMatrixRenderer.drawCircle(frame, 12, 12, 5, (brightness * 0.3).toInt())
                GlyphMatrixRenderer.drawCircle(frame, 12, 12, 2, (brightness * 0.5).toInt())
                
                // Center dot
                val pixelIndex = 12 * 25 + 12
                frame[pixelIndex] = (brightness * 0.8).toInt()
            }
            "layered_rings" -> {
                // Multiple ring layers
                for (r in 2..10 step 2) {
                    GlyphMatrixRenderer.drawCircle(frame, 12, 12, r, (brightness * 0.3 * (1 - r / 10f)).toInt())
                }
            }
            else -> {
                // Default to big sphere
                drawStaticSphere(frame, 12, 12, 8, brightness * 0.4)
            }
        }
        
        frame
    }
    
    // Paused state frame - subtle breathing effect based on style
    private val pausedFrames: Array<IntArray> by lazy {
        Array(8) { i ->
            val frame = createEmptyFrame()
            val breathPhase = i.toFloat() / 8f
            val breathIntensity = (sin(breathPhase * 2 * Math.PI) * 0.5 + 0.5).toFloat()
            
            when (visualStyle) {
                "big_sphere" -> {
                    // Breathing sphere
                    val radius = (6 + breathIntensity * 2).toInt()
                    val brightness = this.brightness * (0.25 + breathIntensity * 0.15)
                    drawStaticSphere(frame, 12, 12, radius, brightness)
                }
                "minimalist_circle" -> {
                    // Breathing circle
                    val radius = (4 + breathIntensity * 2).toInt()
                    val brightness = (this.brightness * (0.2 + breathIntensity * 0.2)).toInt()
                    
                    GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius, brightness)
                    GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, (this.brightness * 0.5).toInt())
                }
                "layered_rings" -> {
                    // Breathing rings
                    val maxRadius = (8 + breathIntensity * 2).toInt()
                    for (r in 2..maxRadius step 2) {
                        val ringBrightness = (this.brightness * (0.3 + breathIntensity * 0.2) * (1 - r.toFloat() / maxRadius)).toInt()
                        GlyphMatrixRenderer.drawCircle(frame, 12, 12, r, ringBrightness)
                    }
                }
                else -> {
                    // Default to big sphere
                    val radius = (6 + breathIntensity * 2).toInt()
                    val brightness = this.brightness * (0.25 + breathIntensity * 0.15)
                    drawStaticSphere(frame, 12, 12, radius, brightness)
                }
            }
            
            frame
        }
    }
    
    // Fallback animation for demo mode
    private val fallbackFrames: Array<IntArray> by lazy {
        Array(frameCount) { frameIndex ->
            val frame = createEmptyFrame()
            val progress = frameIndex.toFloat() / frameCount
            
            // Smooth pulsing animation
            val pulsePhase = sin(progress * 2 * Math.PI)
            val scale = 0.5f + pulsePhase * 0.5f
            val radius = (scale * 8).toInt().coerceIn(2, 10)
            
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius, (brightness * 0.6 * scale).toInt())
            
            // Add subtle center glow
            if (scale > 0.7f) {
                GlyphMatrixRenderer.drawDot(frame, 12, 12, 1, (brightness * scale).toInt())
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
        val currentTime = System.currentTimeMillis()
        
        // Return special frames for non-playing states
        if (!audioData.isPlaying) {
            return offlineFrame.clone()
        }
        
        // Calculate weighted audio intensity with sensitivity
        val weightedIntensity = (calculateWeightedIntensity(audioData) * sensitivity).toFloat()
        
        // Update beat history for pattern detection
        updateBeatHistory(weightedIntensity.toFloat())
        
        // Calculate target scale with beat prediction
        val beatBoost = if (detectBeat(audioData)) 1.3f else 1.0f
        
        // Use different scale ranges based on visual style
        val (minScale, maxScale) = when (visualStyle) {
            "big_sphere" -> SPHERE_MIN_SCALE to SPHERE_MAX_SCALE
            else -> MIN_SCALE to MAX_SCALE
        }
        targetScale = (minScale + (weightedIntensity * (maxScale - minScale))) * beatBoost
        
        // Apply smooth easing to current scale
        currentScale = applyEasing(currentScale, targetScale, 0.12f)
        
        // Beat detection for ring effects
        if (enableRings && detectBeat(audioData) && currentTime - lastBeatTime > 180) {
            lastBeatTime = currentTime
            addRingEffect(weightedIntensity.toFloat())
        }
        
        // Update and draw ring effects
        if (enableRings) {
            updateAndDrawRings(frame)
        }
        
        // Draw main pulsing circle with current scale
        drawMainCircle(frame, currentScale, weightedIntensity.toDouble())
        
        // Add particle effects for high energy
        if (enableParticles && (weightedIntensity > PARTICLE_THRESHOLD || detectStrongBeat(audioData))) {
            updateAndDrawParticles(frame, weightedIntensity.toDouble())
        }
        
        return frame
    }
    
    /**
     * Calculate weighted audio intensity with frequency analysis
     */
    private fun calculateWeightedIntensity(audioData: AudioData): Double {
        val bassWeight = 2.0f   // Strong bass emphasis for impact
        val midWeight = 1.0f    // Normal mid response
        val trebleWeight = 0.5f // Subtle treble influence
        
        val weighted = (
            audioData.beatIntensity * 1.2 + 
            audioData.bassLevel * bassWeight + 
            audioData.midLevel * midWeight + 
            audioData.trebleLevel * trebleWeight
        ) / (1.2 + bassWeight + midWeight + trebleWeight)
        
        return weighted.coerceIn(0.0, 1.0)
    }
    
    /**
     * Update beat history for pattern detection
     */
    private fun updateBeatHistory(intensity: Float) {
        beatHistory.add(intensity)
        if (beatHistory.size > 8) {
            beatHistory.removeAt(0)
        }
    }
    
    /**
     * Detect beat using intensity threshold and history
     */
    private fun detectBeat(audioData: AudioData): Boolean {
        if (beatHistory.size < 4) return false
        
        val average = beatHistory.average().toFloat()
        val current = audioData.beatIntensity.toFloat()
        
        return current > BEAT_THRESHOLD && current > average * 1.3f
    }
    
    /**
     * Detect strong beats for special effects
     */
    private fun detectStrongBeat(audioData: AudioData): Boolean {
        return audioData.beatIntensity > 0.85 && audioData.bassLevel > 0.7
    }
    
    /**
     * Apply easing function for smooth transitions
     */
    private fun applyEasing(current: Float, target: Float, factor: Float): Float {
        val t = factor.coerceIn(0f, 1f)
        val diff = target - current
        
        return when (easingType) {
            "linear" -> current + diff * t
            "cubic" -> {
                // Cubic ease-in-out
                val t2 = if (t < 0.5f) {
                    4 * t * t * t
                } else {
                    1 - (-2 * t + 2).pow(3) / 2
                }
                current + diff * t2
            }
            "elastic" -> {
                // Elastic ease-out
                if (t == 0f || t == 1f) {
                    current + diff * t
                } else {
                    val p = 0.3f
                    val s = p / 4
                    val t2 = 2f.pow(-10 * t) * sin((t - s) * (2 * Math.PI / p).toFloat()) + 1
                    current + diff * t2
                }
            }
            else -> current + diff * t
        }
    }
    
    /**
     * Draw static sphere for offline/paused states
     */
    private fun drawStaticSphere(frame: IntArray, centerX: Int, centerY: Int, radius: Int, maxBrightness: Double) {
        val radiusSquared = radius * radius
        
        for (y in (centerY - radius)..(centerY + radius)) {
            for (x in (centerX - radius)..(centerX + radius)) {
                if (x in 0..24 && y in 0..24) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val distanceSquared = dx * dx + dy * dy
                    
                    if (distanceSquared <= radiusSquared) {
                        val distance = sqrt(distanceSquared.toFloat())
                        val normalizedDistance = distance / radius
                        
                        // Create gradient effect - brighter in center
                        val gradientFactor = 1.0 - normalizedDistance.toDouble().pow(1.5)
                        val pixelBrightness = (maxBrightness * gradientFactor).toInt()
                        
                        val pixelIndex = y * 25 + x
                        frame[pixelIndex] = pixelBrightness.coerceIn(0, 255)
                    }
                }
            }
        }
    }
    
    /**
     * Draw big sphere with gradient effect
     */
    private fun drawBigSphere(frame: IntArray, scale: Float, intensity: Double) {
        val baseRadius = SPHERE_BASE_RADIUS
        val radius = (baseRadius * scale).toInt().coerceIn(5, 12)
        val radiusSquared = radius * radius
        
        // Calculate center brightness based on intensity
        val centerBrightness = (brightness * intensity).toInt()
        
        for (y in (12 - radius)..(12 + radius)) {
            for (x in (12 - radius)..(12 + radius)) {
                if (x in 0..24 && y in 0..24) {
                    val dx = x - 12
                    val dy = y - 12
                    val distanceSquared = dx * dx + dy * dy
                    
                    if (distanceSquared <= radiusSquared) {
                        val distance = sqrt(distanceSquared.toFloat())
                        val normalizedDistance = distance / radius
                        
                        // Create smooth gradient with enhanced center
                        val gradientFactor = (1.0 - normalizedDistance.toDouble()).pow(1.2)
                        
                        // Add subtle 3D effect - slightly brighter on one side
                        val highlight = if (dx < 0 && dy < 0) 1.1f else 1.0f
                        
                        val pixelBrightness = (centerBrightness * gradientFactor * highlight).toInt()
                        
                        val pixelIndex = y * 25 + x
                        frame[pixelIndex] = max(frame[pixelIndex], pixelBrightness.coerceIn(0, 255))
                    }
                }
            }
        }
        
        // Add subtle outer glow for high intensity
        if (intensity > 0.7) {
            val glowRadius = radius + 1
            if (glowRadius <= 12) {
                GlyphMatrixRenderer.drawCircle(frame, 12, 12, glowRadius, (brightness * intensity * 0.2).toInt())
            }
        }
    }
    
    /**
     * Draw the main pulsing circle with layered effects
     */
    private fun drawMainCircle(frame: IntArray, scale: Float, intensity: Double) {
        when (visualStyle) {
            "big_sphere" -> drawBigSphere(frame, scale, intensity)
            "minimalist_circle" -> drawMinimalistCircle(frame, scale, intensity)
            "layered_rings" -> drawLayeredRings(frame, scale, intensity)
            else -> drawBigSphere(frame, scale, intensity) // Default to big sphere
        }
    }
    
    /**
     * Draw minimalist circle style (original implementation)
     */
    private fun drawMinimalistCircle(frame: IntArray, scale: Float, intensity: Double) {
        val baseRadius = 10
        val radius = (baseRadius * scale).toInt().coerceIn(3, 12)
        
        // Calculate layer opacities based on intensity
        val outerOpacity = (intensity * 0.3).coerceIn(0.1, 0.4)
        val midOpacity = (intensity * 0.6).coerceIn(0.3, 0.7)
        val coreOpacity = intensity.coerceIn(0.5, 1.0)
        
        // Outer glow layer
        if (radius > 6) {
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius, (brightness * outerOpacity).toInt())
        }
        
        // Mid layer with slight offset for depth
        if (radius > 3) {
            val midRadius = (radius * 0.7).toInt()
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, midRadius, (brightness * midOpacity).toInt())
        }
        
        // Core circle - filled
        val coreRadius = max(1, (radius * 0.4).toInt())
        for (r in 1..coreRadius) {
            val coreBrightness = (brightness * coreOpacity * (1 - r.toFloat() / coreRadius * 0.3)).toInt()
            GlyphMatrixRenderer.drawCircle(frame, 12, 12, r, coreBrightness)
        }
        
        // Center pixel - always bright on strong intensity
        if (intensity > 0.6) {
            val pixelIndex = 12 * 25 + 12
            frame[pixelIndex] = brightness
        }
    }
    
    /**
     * Draw layered rings style
     */
    private fun drawLayeredRings(frame: IntArray, scale: Float, intensity: Double) {
        val baseRadius = 11
        val maxRadius = (baseRadius * scale).toInt().coerceIn(3, 12)
        
        // Draw multiple ring layers from outer to inner
        val ringCount = 5
        for (i in ringCount downTo 1) {
            val ringRadius = (maxRadius * i / ringCount.toFloat()).toInt()
            if (ringRadius > 0) {
                val ringIntensity = intensity * (1.0 - (i - 1) / ringCount.toFloat() * 0.5)
                val ringBrightness = (brightness * ringIntensity * 0.8).toInt()
                
                GlyphMatrixRenderer.drawCircle(frame, 12, 12, ringRadius, ringBrightness)
                
                // Add inner fill for smaller rings
                if (i <= 2 && ringRadius > 1) {
                    for (r in 1 until ringRadius) {
                        val fillBrightness = (ringBrightness * (1 - r.toFloat() / ringRadius * 0.5)).toInt()
                        GlyphMatrixRenderer.drawCircle(frame, 12, 12, r, fillBrightness)
                    }
                }
            }
        }
        
        // Always bright center on high intensity
        if (intensity > 0.6) {
            val pixelIndex = 12 * 25 + 12
            frame[pixelIndex] = brightness
        }
    }
    
    /**
     * Add a new ring effect with dynamic properties
     */
    private fun addRingEffect(intensity: Float) {
        val speed = 0.5f + intensity * 0.8f
        ringEffects.add(RingEffect(1f, 1f, speed))
        
        // Limit ring count
        if (ringEffects.size > MAX_RINGS) {
            ringEffects.removeAt(0)
        }
    }
    
    /**
     * Update and draw expanding ring effects
     */
    private fun updateAndDrawRings(frame: IntArray) {
        val iterator = ringEffects.iterator()
        while (iterator.hasNext()) {
            val ring = iterator.next()
            
            // Update ring expansion
            val newRadius = ring.radius + ring.speed
            val newOpacity = ring.opacity * RING_FADE_RATE
            
            if (newOpacity < 0.05f || newRadius > 13) {
                iterator.remove()
            } else {
                // Update ring state
                val index = ringEffects.indexOf(ring)
                if (index >= 0) {
                    ringEffects[index] = ring.copy(radius = newRadius, opacity = newOpacity)
                }
                
                // Draw ring with smooth anti-aliasing
                val radius = newRadius.toInt()
                if (radius in 2..12) {
                    val ringBrightness = (brightness * newOpacity * 0.8).toInt()
                    GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius, ringBrightness)
                    
                    // Add subtle inner glow
                    if (radius > 3 && newOpacity > 0.5f) {
                        GlyphMatrixRenderer.drawCircle(frame, 12, 12, radius - 1, (ringBrightness * 0.3).toInt())
                    }
                }
            }
        }
    }
    
    /**
     * Update and draw particle effects
     */
    private fun updateAndDrawParticles(frame: IntArray, intensity: Double) {
        // Add new particles based on intensity
        if (particles.size < MAX_PARTICLES && kotlin.random.Random.nextFloat() < intensity * 0.8) {
            val angle = kotlin.random.Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = 0.2f + kotlin.random.Random.nextFloat() * 0.6f * intensity.toFloat()
            val size = if (kotlin.random.Random.nextFloat() < 0.3f) 2f else 1f
            
            particles.add(
                Particle(
                    x = 12f + cos(angle) * 2f,
                    y = 12f + sin(angle) * 2f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    life = 1f,
                    size = size
                )
            )
        }
        
        // Update and draw existing particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            
            // Update particle physics
            val newX = particle.x + particle.vx
            val newY = particle.y + particle.vy
            val newLife = particle.life - 0.08f
            
            // Remove dead or out-of-bounds particles
            if (newLife <= 0 || newX < -1 || newX > 25 || newY < -1 || newY > 25) {
                iterator.remove()
            } else {
                // Update particle state
                val index = particles.indexOf(particle)
                if (index >= 0) {
                    particles[index] = particle.copy(x = newX, y = newY, life = newLife)
                }
                
                // Draw particle with life-based fading
                val px = newX.toInt()
                val py = newY.toInt()
                
                if (px in 0..24 && py in 0..24) {
                    val particleBrightness = (brightness * newLife * 0.8 * intensity).toInt()
                    
                    if (particle.size > 1.5f) {
                        // Large particle - draw as small dot
                        GlyphMatrixRenderer.drawDot(frame, px, py, 1, particleBrightness)
                    } else {
                        // Small particle - single pixel
                        val pixelIndex = py * 25 + px
                        frame[pixelIndex] = max(frame[pixelIndex], particleBrightness)
                    }
                }
            }
        }
    }
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = "sensitivity",
                displayName = "Sensitivity",
                description = "Audio response sensitivity",
                defaultValue = sensitivity,
                minValue = 0.5f,
                maxValue = 2.0f,
                stepSize = 0.1f,
                category = SettingCategories.AUDIO
            )
            .addToggleSetting(
                id = "enable_rings",
                displayName = "Ring Effects",
                description = "Expanding rings on beat detection",
                defaultValue = enableRings,
                category = SettingCategories.EFFECTS
            )
            .addToggleSetting(
                id = "enable_particles",
                displayName = "Particle Effects",
                description = "Particles during high energy",
                defaultValue = enableParticles,
                category = SettingCategories.EFFECTS
            )
            .addDropdownSetting(
                id = "visual_style",
                displayName = "Visual Style",
                description = "Choose visualization style",
                defaultValue = visualStyle,
                options = listOf(
                    DropdownOption("big_sphere", "Big Sphere", "Large gradient sphere that fills the display"),
                    DropdownOption("minimalist_circle", "Minimalist Circle", "Small layered circles with clean aesthetic"),
                    DropdownOption("layered_rings", "Layered Rings", "Multiple concentric rings with depth")
                ),
                category = SettingCategories.VISUAL
            )
            .addDropdownSetting(
                id = "easing_type",
                displayName = "Animation Style",
                description = "Transition smoothing type",
                defaultValue = easingType,
                options = listOf(
                    DropdownOption("linear", "Linear", "Constant speed transitions"),
                    DropdownOption("cubic", "Cubic", "Smooth ease-in-out transitions"),
                    DropdownOption("elastic", "Elastic", "Bouncy elastic transitions")
                ),
                category = SettingCategories.ANIMATION
            )
            .build()
    }
    
    override fun applySettings(settings: ThemeSettings) {
        sensitivity = settings.getTypedValue("sensitivity", 1.0f)
        enableRings = settings.getTypedValue("enable_rings", true)
        enableParticles = settings.getTypedValue("enable_particles", true)
        easingType = settings.getTypedValue("easing_type", "cubic")
        visualStyle = settings.getTypedValue("visual_style", "big_sphere")
    }
}