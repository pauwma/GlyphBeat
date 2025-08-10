package com.pauwma.glyphbeat.themes.animation.preview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.getToggleValue
import com.pauwma.glyphbeat.ui.settings.getSliderValueLong
import com.pauwma.glyphbeat.ui.settings.getSliderValueInt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CoverArtPreviewManager - Manages the preview state and frame generation for CoverArtTheme.
 * 
 * This class handles all preview-specific logic for the CoverArtTheme, including:
 * - Media monitoring and album art fetching
 * - Preview frame caching and updates
 * - Rotation state management for animated previews
 * - Settings application for real-time preview updates
 * 
 * The manager is designed to be efficient and responsive, providing smooth
 * preview updates while minimizing resource usage through intelligent caching
 * and state management.
 * 
 * Architecture:
 * - Uses StateFlow for reactive preview updates
 * - Caches processed frames to avoid redundant processing
 * - Manages rotation animation independently from main theme
 * - Handles all media state transitions gracefully
 */
class CoverArtPreviewManager(private val context: Context) {
    
    // =================================================================================
    // STATE MANAGEMENT
    // =================================================================================
    
    /**
     * Preview state representing the current preview frame and metadata
     */
    data class PreviewState(
        val frameData: IntArray,
        val isPlaying: Boolean = false,
        val hasMedia: Boolean = false,
        val trackTitle: String? = null,
        val rotationAngle: Float = 0f,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PreviewState) return false
            return frameData.contentEquals(other.frameData) &&
                   isPlaying == other.isPlaying &&
                   hasMedia == other.hasMedia &&
                   trackTitle == other.trackTitle &&
                   rotationAngle == other.rotationAngle
        }
        
        override fun hashCode(): Int {
            var result = frameData.contentHashCode()
            result = 31 * result + isPlaying.hashCode()
            result = 31 * result + hasMedia.hashCode()
            result = 31 * result + (trackTitle?.hashCode() ?: 0)
            result = 31 * result + rotationAngle.hashCode()
            return result
        }
    }
    
    // Reactive state flow for preview updates
    private val _previewState = MutableStateFlow(
        PreviewState(frameData = createDefaultPreviewFrame())
    )
    val previewState: StateFlow<PreviewState> = _previewState.asStateFlow()
    
    // =================================================================================
    // DEPENDENCIES AND CACHING
    // =================================================================================
    
    private val mediaHelper: MediaControlHelper by lazy { 
        MediaControlHelper(context)
    }
    
    private val renderer: CoverArtPreviewRenderer by lazy {
        CoverArtPreviewRenderer()
    }
    
    // Cache for processed album art
    private var cachedAlbumArt: Bitmap? = null
    private var cachedTrackTitle: String? = null
    private var cachedProcessedFrame: IntArray? = null
    
    // Rotation animation state with thread-safe angle management
    private var rotationJob: Job? = null
    private val atomicRotationAngle = AtomicReference(0f)
    private var currentRotationAngle: Float
        get() = atomicRotationAngle.get()
        set(value) = atomicRotationAngle.set(value)
    private val rotationMutex = Mutex()
    private var rotationStartTime = 0L
    private var pausedRotationAngle = 0f
    private var isRotationPaused = false
    private var lastRotationSpeed = 0L
    private var isUpdatingSettings = false
    
    // Media monitoring
    private var mediaMonitorJob: Job? = null
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // =================================================================================
    // INITIALIZATION AND LIFECYCLE
    // =================================================================================
    
    init {
        // Register for media state changes
        mediaHelper.registerStateChangeCallback(object : MediaControlHelper.StateChangeCallback {
            override fun onPlaybackStateChanged(isPlaying: Boolean, hasActiveMedia: Boolean) {
                handlePlaybackStateChange(isPlaying, hasActiveMedia)
            }
        })
        
        // Start with current media state - asynchronously to avoid blocking
        monitorScope.launch {
            updatePreviewState()
        }
    }
    
    /**
     * Start monitoring media changes for preview updates.
     * Should be called when the preview becomes visible.
     */
    fun startMonitoring() {
        stopMonitoring() // Cancel any existing monitoring
        
        mediaMonitorJob = monitorScope.launch {
            while (isActive) {
                updatePreviewState()
                delay(3000) // Check every 3 seconds for preview
            }
        }
        
        Log.d(LOG_TAG, "Started media monitoring for preview")
    }
    
    /**
     * Stop monitoring media changes.
     * Should be called when the preview is no longer visible.
     */
    fun stopMonitoring() {
        mediaMonitorJob?.cancel()
        mediaMonitorJob = null
        Log.d(LOG_TAG, "Stopped media monitoring for preview")
    }
    
    /**
     * Clean up resources and stop all coroutines.
     */
    fun cleanup() {
        stopMonitoring()
        stopRotation()
        monitorScope.cancel()
        mediaHelper.cleanup()
        cachedAlbumArt?.recycle()
        cachedAlbumArt = null
        Log.d(LOG_TAG, "Preview manager cleaned up")
    }
    
    // =================================================================================
    // PREVIEW STATE UPDATES
    // =================================================================================
    
    /**
     * Update the preview state based on current media and settings.
     * This is the main method that generates preview frames.
     */
    private fun updatePreviewState(settings: ThemeSettings? = null) {
        try {
            // Don't update if we're in the middle of updating settings
            if (isUpdatingSettings) {
                return
            }
            
            val trackInfo = mediaHelper.getTrackInfo()
            val isPlaying = mediaHelper.isPlaying()
            val hasMedia = trackInfo != null
            
            // Get current rotation angle atomically
            val safeRotationAngle = atomicRotationAngle.get()
            
            // For rotation, we always need to regenerate the frame
            val isRotating = rotationJob != null && settings?.getToggleValue("enable_rotation", false) != false
            
            // Check if we need to update the cached frame
            val needsUpdate = trackInfo?.title != cachedTrackTitle || 
                             settings != null || 
                             cachedProcessedFrame == null ||
                             isRotating  // Always update when rotating
            
            if (needsUpdate) {
                // Clear cache for new track
                if (trackInfo?.title != cachedTrackTitle) {
                    cachedAlbumArt?.recycle()
                    cachedAlbumArt = null
                    cachedTrackTitle = trackInfo?.title
                    Log.d(LOG_TAG, "Track changed to: ${trackInfo?.title}")
                }
                
                // Generate new preview frame
                val frameData = if (trackInfo?.albumArt != null) {
                    // Process album art for preview (only process once if not cached)
                    val previewBitmap = cachedAlbumArt ?: renderer.processAlbumArtForPreview(
                        albumArt = trackInfo.albumArt,
                        targetSize = PREVIEW_SIZE,
                        settings = settings
                    )
                    
                    if (cachedAlbumArt == null) {
                        cachedAlbumArt = previewBitmap
                    }
                    
                    // Convert to frame data with current rotation
                    // Use rotation angle if rotation is active, even if settings say false (during transition)
                    renderer.bitmapToPreviewFrame(
                        bitmap = previewBitmap,
                        rotation = if (isRotating || settings?.getToggleValue("enable_rotation", false) == true) {
                            safeRotationAngle
                        } else {
                            0f
                        },
                        settings = settings
                    )
                } else {
                    // Use fallback pattern WITHOUT rotation - offline frame should always be static
                    renderer.generateFallbackPreview(settings)
                }
                
                // Only cache if not rotating
                if (!isRotating) {
                    cachedProcessedFrame = frameData
                } else {
                    // Use the fresh frame data directly when rotating
                    _previewState.value = PreviewState(
                        frameData = frameData,
                        isPlaying = isPlaying,
                        hasMedia = hasMedia,
                        trackTitle = trackInfo?.title,
                        rotationAngle = safeRotationAngle
                    )
                    return
                }
            }
            
            // Update state flow with new or cached frame
            _previewState.value = PreviewState(
                frameData = cachedProcessedFrame ?: createDefaultPreviewFrame(),
                isPlaying = isPlaying,
                hasMedia = hasMedia,
                trackTitle = trackInfo?.title,
                rotationAngle = safeRotationAngle
            )
            
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error updating preview state: ${e.message}")
            _previewState.value = PreviewState(
                frameData = createDefaultPreviewFrame(),
                isPlaying = false,
                hasMedia = false,
                rotationAngle = 0f
            )
        }
    }
    
    // Store current settings for rotation updates
    private var currentSettings: ThemeSettings? = null
    
    /**
     * Apply new settings and update the preview accordingly.
     */
    fun applySettings(settings: ThemeSettings, isSelected: Boolean = false) {
        monitorScope.launch {
            rotationMutex.withLock {
                isUpdatingSettings = true
                
                // Store settings for rotation updates
                currentSettings = settings
                
                // Handle rotation setting changes - only animate if selected
                val enableRotation = settings.getToggleValue("enable_rotation", false) && isSelected
                val rotationSpeed = settings.getSliderValueLong("rotation_speed", 150L)
                
                // Check if rotation-specific settings changed
                val rotationSettingsChanged = this@CoverArtPreviewManager.lastRotationSpeed != rotationSpeed
                val wasRotating = rotationJob != null
                
                if (enableRotation) {
                    // Preserve current angle before any changes
                    val preservedAngle = currentRotationAngle
                    
                    // Only restart rotation if it wasn't running or speed changed
                    if (!wasRotating) {
                        // Start fresh rotation
                        startRotationSafe(rotationSpeed, 0f)
                        this@CoverArtPreviewManager.lastRotationSpeed = rotationSpeed
                    } else if (rotationSettingsChanged) {
                        // Speed changed, restart with current angle preserved
                        startRotationSafe(rotationSpeed, preservedAngle)
                        this@CoverArtPreviewManager.lastRotationSpeed = rotationSpeed
                    } else {
                        // Rotation is already running with same speed
                        // Just update the preview with new non-rotation settings
                        // Make sure angle is preserved
                        atomicRotationAngle.set(preservedAngle)
                        withContext(Dispatchers.Main) {
                            cachedProcessedFrame = null
                            updatePreviewState(settings)
                        }
                    }
                } else {
                    // Only stop if actually rotating
                    if (wasRotating) {
                        stopRotationSafe()
                        atomicRotationAngle.set(0f)
                    }
                    // Clear frame cache and update
                    withContext(Dispatchers.Main) {
                        cachedProcessedFrame = null
                        updatePreviewState(settings)
                    }
                }
                
                isUpdatingSettings = false
            }
        }
    }
    
    // =================================================================================
    // ROTATION ANIMATION
    // =================================================================================
    
    /**
     * Start rotation animation for the preview (thread-safe version).
     */
    private suspend fun startRotationSafe(speedMs: Long, initialAngle: Float = 0f) {
        // Cancel existing rotation job if any
        rotationJob?.cancelAndJoin()
        rotationJob = null
        
        // Set initial angle atomically
        atomicRotationAngle.set(initialAngle)
        
        val frameCount = currentSettings?.getSliderValueInt("rotation_smoothness", 30) ?: 30
        val degreesPerFrame = 360f / frameCount
        
        rotationJob = monitorScope.launch {
            rotationStartTime = System.currentTimeMillis()
            
            while (isActive) {
                if (!isRotationPaused && !isUpdatingSettings) {
                    // Calculate next angle atomically
                    val currentAngle = atomicRotationAngle.get()
                    val nextAngle = (currentAngle + degreesPerFrame) % 360f
                    atomicRotationAngle.set(nextAngle)
                    
                    // Update preview with new rotation angle
                    withContext(Dispatchers.Main) {
                        // Clear frame cache to force regeneration with new angle
                        cachedProcessedFrame = null
                        // Update with stored settings
                        currentSettings?.let { updatePreviewState(it) }
                    }
                }
                
                delay(speedMs)
            }
        }
        
        Log.d(LOG_TAG, "Started preview rotation animation from angle: $initialAngle")
    }
    
    /**
     * Start rotation animation for the preview (legacy version for compatibility).
     */
    private fun startRotation(speedMs: Long, initialAngle: Float = 0f) {
        runBlocking {
            rotationMutex.withLock {
                startRotationSafe(speedMs, initialAngle)
            }
        }
    }
    
    /**
     * Stop rotation animation (thread-safe version).
     */
    private suspend fun stopRotationSafe() {
        rotationJob?.cancelAndJoin()
        rotationJob = null
        atomicRotationAngle.set(0f)
        pausedRotationAngle = 0f
        isRotationPaused = false
        Log.d(LOG_TAG, "Stopped preview rotation animation")
    }
    
    /**
     * Stop rotation animation (legacy version for compatibility).
     */
    private fun stopRotation() {
        runBlocking {
            rotationMutex.withLock {
                stopRotationSafe()
            }
        }
    }
    
    /**
     * Handle playback state changes for rotation pause/resume.
     */
    private fun handlePlaybackStateChange(isPlaying: Boolean, hasActiveMedia: Boolean) {
        if (rotationJob != null) {
            if (isPlaying && hasActiveMedia) {
                // Resume rotation
                if (isRotationPaused) {
                    atomicRotationAngle.set(pausedRotationAngle)
                    isRotationPaused = false
                    Log.d(LOG_TAG, "Resumed preview rotation from ${pausedRotationAngle}°")
                }
            } else if (hasActiveMedia) {
                // Pause rotation
                if (!isRotationPaused) {
                    pausedRotationAngle = atomicRotationAngle.get()
                    isRotationPaused = true
                    Log.d(LOG_TAG, "Paused preview rotation at ${pausedRotationAngle}°")
                }
            }
        }
        
        // Update preview state
        updatePreviewState()
    }
    
    // =================================================================================
    // PREVIEW FRAME GENERATION
    // =================================================================================
    
    /**
     * Generate a preview frame for specific settings without updating state.
     * Useful for settings UI previews.
     */
    fun generatePreviewFrame(settings: ThemeSettings): IntArray {
        return try {
            val trackInfo = mediaHelper.getTrackInfo()
            val safeRotationAngle = atomicRotationAngle.get()
            
            if (trackInfo?.albumArt != null) {
                val previewBitmap = renderer.processAlbumArtForPreview(
                    albumArt = trackInfo.albumArt,
                    targetSize = PREVIEW_SIZE,
                    settings = settings
                )
                
                renderer.bitmapToPreviewFrame(
                    bitmap = previewBitmap,
                    rotation = if (settings.getToggleValue("enable_rotation", false)) {
                        safeRotationAngle
                    } else {
                        0f
                    },
                    settings = settings
                )
            } else {
                renderer.generateFallbackPreview(settings)
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error generating preview frame: ${e.message}")
            createDefaultPreviewFrame()
        }
    }
    
    /**
     * Check if the preview needs an update based on current state.
     */
    fun shouldUpdatePreview(): Boolean {
        val trackInfo = mediaHelper.getTrackInfo()
        return trackInfo?.title != cachedTrackTitle || 
               cachedProcessedFrame == null ||
               (System.currentTimeMillis() - _previewState.value.timestamp) > UPDATE_THRESHOLD_MS
    }
    
    // =================================================================================
    // HELPER METHODS
    // =================================================================================
    
    /**
     * Create a default preview frame (concentric circles pattern).
     */
    private fun createDefaultPreviewFrame(): IntArray {
        val frame = IntArray(625) { 0 }
        val centerX = 12.0
        val centerY = 12.0
        
        for (row in 0 until 25) {
            for (col in 0 until 25) {
                val index = row * 25 + col
                val distance = kotlin.math.sqrt(
                    (col - centerX) * (col - centerX) + 
                    (row - centerY) * (row - centerY)
                )
                
                if (distance <= 12.5) {
                    when {
                        distance <= 3.0 -> frame[index] = 255
                        distance <= 5.0 -> frame[index] = 0
                        distance <= 7.0 -> frame[index] = 180
                        distance <= 9.0 -> frame[index] = 0
                        distance <= 11.0 -> frame[index] = 120
                        else -> frame[index] = 0
                    }
                }
            }
        }
        
        return frame
    }
    
    companion object {
        private val LOG_TAG = CoverArtPreviewManager::class.java.simpleName
        private const val PREVIEW_SIZE = 25
        private const val UPDATE_THRESHOLD_MS = 30000 // 30 seconds
    }
}