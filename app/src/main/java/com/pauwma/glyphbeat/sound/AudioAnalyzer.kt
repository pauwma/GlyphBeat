package com.pauwma.glyphbeat.sound

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Audio analyzer that attempts to extract rhythm and beat information
 * from available media session data and system audio state.
 * 
 * Note: Due to Android privacy restrictions, we cannot directly access
 * other apps' audio streams, so this uses available metadata and system
 * state to simulate audio-reactive behavior.
 */
class AudioAnalyzer(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mediaHelper = MediaControlHelper(context)
    
    // Enhanced audio detection with Visualizer API
    private var visualizer: Visualizer? = null
    private var isVisualizerEnabled = false
    private var lastWaveform: ByteArray? = null
    private var lastFft: ByteArray? = null
    
    // Audio state flows
    private val _beatIntensity = MutableStateFlow(0.0)
    val beatIntensity: StateFlow<Double> = _beatIntensity
    
    private val _bassLevel = MutableStateFlow(0.0)
    val bassLevel: StateFlow<Double> = _bassLevel
    
    private val _midLevel = MutableStateFlow(0.0)
    val midLevel: StateFlow<Double> = _midLevel

    private val _trebleLevel = MutableStateFlow(0.0)
    val trebleLevel: StateFlow<Double> = _trebleLevel
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying
    
    // Analysis state
    private var lastPosition: Long = 0
    private var lastVolumeLevel: Int = 0
    private var lastVolumeCheckTime: Long = 0
    private var simulatedTime: Double = 0.0
    private var lastUpdateTime: Long = System.currentTimeMillis()
    
    // Beat detection state
    private var beatHistory = mutableListOf<Double>()
    private var lastBeatTime: Long = 0
    private val beatThreshold = 0.3

    // Visualizer retry state (retries after permission is granted)
    private var lastVisualizerRetryTime: Long = 0

    // 25-band spectrum from FFT (one per grid column), smoothed
    private val spectrumBands = FloatArray(25) { 0f }
    
    init {
        initializeVisualizer()
    }
    
    /**
     * Initialize Android Visualizer API for enhanced audio detection
     */
    private fun initializeVisualizer() {
        try {
            // Release any previous instance first
            visualizer?.apply {
                try { enabled = false } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
            visualizer = null
            isVisualizerEnabled = false

            if (Visualizer.getMaxCaptureRate() > 0) {
                val vis = Visualizer(0)
                // Must disable before configuring capture size
                vis.enabled = false
                vis.captureSize = Visualizer.getCaptureSizeRange()[1]

                vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let {
                            lastWaveform = it.clone()
                            processWaveformData(it)
                        }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        fft?.let {
                            lastFft = it.clone()
                            processFftData(it)
                        }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)

                vis.enabled = true
                visualizer = vis
                isVisualizerEnabled = true
                Log.i(LOG_TAG, "Visualizer initialized successfully")
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to initialize Visualizer: ${e.message}")
            isVisualizerEnabled = false
        }
    }
    
    /**
     * Process waveform data for beat detection
     */
    private fun processWaveformData(waveform: ByteArray) {
        // Calculate RMS (Root Mean Square) for overall intensity
        var sum = 0.0
        for (byte in waveform) {
            val sample = (byte.toInt() and 0xFF).toDouble() - 128.0
            sum += sample * sample
        }
        val rms = sqrt(sum / waveform.size)
        val normalizedRms = (rms / 128.0).coerceIn(0.0, 1.0)
        
        // Beat detection using energy-based approach
        val currentTime = System.currentTimeMillis()
        if (normalizedRms > beatThreshold && currentTime - lastBeatTime > 200) { // Minimum 200ms between beats
            lastBeatTime = currentTime
            _beatIntensity.value = normalizedRms
        } else {
            // Decay beat intensity over time
            _beatIntensity.value = (_beatIntensity.value * 0.95).coerceAtLeast(0.0)
        }
    }
    
    /**
     * Process FFT data for frequency band analysis and 25-band spectrum.
     */
    private fun processFftData(fft: ByteArray) {
        val numBins = fft.size / 2  // Number of frequency bins (real/imaginary pairs)
        if (numBins < 4) return

        // Compute magnitude for each FFT bin
        val magnitudes = FloatArray(numBins)
        var maxMag = 1f
        for (i in 0 until numBins) {
            val real = fft[i * 2].toFloat()
            val imaginary = fft[i * 2 + 1].toFloat()
            magnitudes[i] = sqrt(real * real + imaginary * imaginary)
            if (magnitudes[i] > maxMag) maxMag = magnitudes[i]
        }

        // --- 25-band spectrum with logarithmic frequency mapping ---
        val bandCount = 25
        val minBin = 1  // Skip DC component
        val maxBin = numBins - 1
        val logMin = kotlin.math.ln(minBin.toFloat())
        val logMax = kotlin.math.ln(maxBin.toFloat())

        for (band in 0 until bandCount) {
            val logStart = logMin + (logMax - logMin) * band / bandCount
            val logEnd = logMin + (logMax - logMin) * (band + 1) / bandCount
            val binStart = kotlin.math.exp(logStart).toInt().coerceIn(minBin, maxBin)
            val binEnd = kotlin.math.exp(logEnd).toInt().coerceIn(binStart + 1, maxBin + 1)

            var sum = 0f
            var count = 0
            for (bin in binStart until binEnd) {
                sum += magnitudes[bin]
                count++
            }

            val avg = if (count > 0) sum / count else 0f
            val normalized = (avg / maxMag).coerceIn(0f, 1f)

            // Smooth with previous value for fluid animation
            spectrumBands[band] = spectrumBands[band] * 0.7f + normalized * 0.3f
        }

        // --- Bass / Mid / Treble summary levels (from the 25 bands) ---
        var bassSum = 0f
        for (i in 0 until 8) bassSum += spectrumBands[i]
        _bassLevel.value = (bassSum / 8.0).coerceIn(0.0, 1.0)

        var midSum = 0f
        for (i in 8 until 18) midSum += spectrumBands[i]
        _midLevel.value = (midSum / 10.0).coerceIn(0.0, 1.0)

        var trebleSum = 0f
        for (i in 18 until 25) trebleSum += spectrumBands[i]
        _trebleLevel.value = (trebleSum / 7.0).coerceIn(0.0, 1.0)
    }
    
    /**
     * Update audio analysis based on current media state with enhanced detection
     */
    fun updateAudioAnalysis(): AudioData {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0
        lastUpdateTime = currentTime

        // Retry Visualizer initialization if not yet enabled (e.g., permission granted after startup)
        if (!isVisualizerEnabled && currentTime - lastVisualizerRetryTime > 5000) {
            lastVisualizerRetryTime = currentTime
            initializeVisualizer()
        }

        try {
            val controller = mediaHelper.getActiveMediaController()
            val playbackState = controller?.playbackState
            val isCurrentlyPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            _isPlaying.value = isCurrentlyPlaying
            
            return when {
                isVisualizerEnabled && isCurrentlyPlaying -> {
                    // Use enhanced Visualizer data when available
                    AudioData(
                        beatIntensity = _beatIntensity.value,
                        bassLevel = _bassLevel.value,
                        midLevel = _midLevel.value,
                        trebleLevel = _trebleLevel.value,
                        isPlaying = true,
                        spectrumBands = spectrumBands.clone()
                    )
                }
                isCurrentlyPlaying -> {
                    // Fallback to MediaController analysis
                    analyzeRealAudio(controller!!, deltaTime)
                }
                else -> {
                    // Return silent state or simulated demo
                    if (_isPlaying.value) {
                        generateSimulatedAudio(deltaTime)
                    } else {
                        AudioData(
                            beatIntensity = 0.0,
                            bassLevel = 0.0,
                            midLevel = 0.0,
                            trebleLevel = 0.0,
                            isPlaying = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Audio analysis failed: ${e.message}")
            return generateSimulatedAudio(deltaTime)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            visualizer?.apply {
                enabled = false
                release()
            }
            visualizer = null
            isVisualizerEnabled = false
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error cleaning up visualizer: ${e.message}")
        }
    }
    
    /**
     * Analyze real audio using available MediaController data
     */
    private fun analyzeRealAudio(controller: MediaController, deltaTime: Double): AudioData {
        val playbackState = controller.playbackState
        val metadata = controller.metadata
        
        // Get current position and track info
        val position = playbackState?.position ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        
        // Track position changes
        lastPosition = position

        // Fallback BPM when Visualizer is unavailable
        val estimatedBPM = 120.0
        
        // Skip volume checking to avoid AudioManager log spam
        // Volume changes are not critical for beat detection
        val volumeDelta = 0
        
        // Generate beat pattern based on estimated BPM and position
        val beatProgress = (position / 1000.0) * (estimatedBPM / 60.0) % 1.0
        val beatIntensity = generateBeatPattern(beatProgress, volumeDelta > 0)
        
        // Simulate frequency bands based on beat and volume
        val bassLevel = beatIntensity * 0.8 + (volumeDelta * 0.1)
        val midLevel = beatIntensity * 0.6 + (volumeDelta * 0.05)
        val trebleLevel = beatIntensity * 0.4 + (volumeDelta * 0.03)
        
        _beatIntensity.value = beatIntensity
        _bassLevel.value = bassLevel
        _midLevel.value = midLevel
        _trebleLevel.value = trebleLevel
        
        return AudioData(
            beatIntensity = beatIntensity,
            bassLevel = bassLevel,
            midLevel = midLevel,
            trebleLevel = trebleLevel,
            isPlaying = true
        )
    }
    
    /**
     * Generate simulated audio for demo mode or when no media is available
     */
    private fun generateSimulatedAudio(deltaTime: Double): AudioData {
        simulatedTime += deltaTime
        
        // Simulate realistic music patterns
        val bpm = 128.0 // Typical electronic music BPM
        val beatProgress = (simulatedTime * bpm / 60.0) % 1.0
        
        val beatIntensity = generateBeatPattern(beatProgress, false)
        val bassLevel = sin(simulatedTime * 2.0) * 0.5 + 0.5
        val midLevel = sin(simulatedTime * 3.0) * 0.3 + 0.4
        val trebleLevel = sin(simulatedTime * 5.0) * 0.2 + 0.3
        
        return AudioData(
            beatIntensity = beatIntensity,
            bassLevel = bassLevel,
            midLevel = midLevel,
            trebleLevel = trebleLevel,
            isPlaying = false
        )
    }
    
    /**
     * Generate beat pattern with emphasis on downbeats
     */
    private fun generateBeatPattern(beatProgress: Double, hasVolumeChange: Boolean): Double {
        // Strong emphasis on beat 1, moderate on beat 3
        val mainBeat = sin(beatProgress * 2 * kotlin.math.PI).let { 
            if (it > 0) it * it else 0.0 // Square positive values for punch
        }
        
        // Add sub-beats
        val subBeat = sin(beatProgress * 4 * kotlin.math.PI) * 0.3
        val microBeat = sin(beatProgress * 8 * kotlin.math.PI) * 0.1
        
        // Volume change detection adds extra intensity
        val volumeBoost = if (hasVolumeChange) 0.2 else 0.0
        
        return kotlin.math.max(0.0, kotlin.math.min(1.0, mainBeat + subBeat + microBeat + volumeBoost))
    }
    
    companion object {
        private val LOG_TAG = AudioAnalyzer::class.java.simpleName
    }
}

/**
 * Data class containing current audio analysis results.
 *
 * @param spectrumBands 25-element array of frequency band magnitudes (0.0-1.0),
 *   mapped logarithmically from the FFT. Null when Visualizer is unavailable.
 */
data class AudioData(
    val beatIntensity: Double,
    val bassLevel: Double,
    val midLevel: Double,
    val trebleLevel: Double,
    val isPlaying: Boolean,
    val spectrumBands: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioData) return false
        return beatIntensity == other.beatIntensity &&
                bassLevel == other.bassLevel &&
                midLevel == other.midLevel &&
                trebleLevel == other.trebleLevel &&
                isPlaying == other.isPlaying &&
                spectrumBands.contentEquals(other.spectrumBands)
    }

    override fun hashCode(): Int {
        var result = beatIntensity.hashCode()
        result = 31 * result + bassLevel.hashCode()
        result = 31 * result + midLevel.hashCode()
        result = 31 * result + trebleLevel.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + (spectrumBands?.contentHashCode() ?: 0)
        return result
    }
}