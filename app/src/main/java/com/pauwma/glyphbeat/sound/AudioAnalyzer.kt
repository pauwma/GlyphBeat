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
    
    init {
        initializeVisualizer()
    }
    
    /**
     * Initialize Android Visualizer API for enhanced audio detection
     */
    private fun initializeVisualizer() {
        try {
            if (Visualizer.getMaxCaptureRate() > 0) {
                visualizer = Visualizer(0).apply {
                    captureSize = Visualizer.getCaptureSizeRange()[1]
                    
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
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
                    }, Visualizer.getMaxCaptureRate() / 4, true, true)
                    
                    enabled = true
                    isVisualizerEnabled = true
                }
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
            val sample = byte.toDouble()
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
     * Process FFT data for frequency band analysis
     */
    private fun processFftData(fft: ByteArray) {
        val fftSize = fft.size / 2
        
        // Calculate bass level (low frequencies: 0-8% of spectrum)
        var bassSum = 0.0
        val bassEnd = (fftSize * 0.08).toInt()
        for (i in 0 until bassEnd step 2) {
            val real = fft[i].toDouble()
            val imaginary = fft[i + 1].toDouble()
            bassSum += sqrt(real * real + imaginary * imaginary)
        }
        _bassLevel.value = (bassSum / bassEnd * 2 / 128.0).coerceIn(0.0, 1.0)
        
        // Calculate mid level (mid frequencies: 8-40% of spectrum)
        var midSum = 0.0
        val midStart = bassEnd
        val midEnd = (fftSize * 0.4).toInt()
        for (i in midStart until midEnd step 2) {
            val real = fft[i].toDouble()
            val imaginary = fft[i + 1].toDouble()
            midSum += sqrt(real * real + imaginary * imaginary)
        }
        val midRange = midEnd - midStart
        _trebleLevel.value = if (midRange > 0) {
            (midSum / midRange * 2 / 128.0).coerceIn(0.0, 1.0)
        } else 0.0
        
        // Calculate treble level (high frequencies: 40-100% of spectrum)
        var trebleSum = 0.0
        val trebleStart = midEnd
        for (i in trebleStart until fftSize step 2) {
            val real = fft[i].toDouble()
            val imaginary = fft[i + 1].toDouble()
            trebleSum += sqrt(real * real + imaginary * imaginary)
        }
        val trebleRange = fftSize - trebleStart
        _trebleLevel.value = if (trebleRange > 0) {
            (trebleSum / trebleRange * 2 / 128.0).coerceIn(0.0, 1.0)
        } else 0.0
    }
    
    /**
     * Update audio analysis based on current media state with enhanced detection
     */
    fun updateAudioAnalysis(): AudioData {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000.0
        lastUpdateTime = currentTime
        
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
                        midLevel = (_bassLevel.value + _trebleLevel.value) / 2.0, // Estimate mid from bass/treble
                        trebleLevel = _trebleLevel.value,
                        isPlaying = true
                    )
                }
                isCurrentlyPlaying && controller != null -> {
                    // Fallback to MediaController analysis
                    analyzeRealAudio(controller, deltaTime)
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
        
        // Detect position changes for tempo estimation
        val positionDelta = position - lastPosition
        lastPosition = position
        
        // Estimate BPM from position changes (rough approximation)
        val estimatedBPM = if (positionDelta > 0 && deltaTime > 0) {
            // This is a very rough estimation
            120.0 // Default BPM, could be enhanced with metadata
        } else {
            120.0
        }
        
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
 * Data class containing current audio analysis results
 */
data class AudioData(
    val beatIntensity: Double,
    val bassLevel: Double,
    val midLevel: Double,
    val trebleLevel: Double,
    val isPlaying: Boolean
)