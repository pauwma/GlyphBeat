package com.pauwma.glyphbeat.services.media

import com.pauwma.glyphbeat.core.GlyphMatrixRenderer

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.pauwma.glyphbeat.services.GlyphMatrixService
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.base.ThemeTemplate
import com.pauwma.glyphbeat.themes.base.FrameTransitionSequence
import com.pauwma.glyphbeat.sound.AudioAnalyzer
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.themes.base.AudioReactiveTheme
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.themes.animation.ScrollTheme
import com.pauwma.glyphbeat.data.ThemeRepository
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import com.pauwma.glyphbeat.services.shake.ShakeDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Enhanced Media Player Toy Service with optimized state change detection
 * and reduced frame update delays.
 * 
 * Key Features:
 * - Immediate state change detection via MediaController callbacks
 * - Optimized frame update pipeline with reduced delays
 * - Variable frame timing support (each frame can have different duration)
 * - State-specific frame rendering (paused, offline, loading, error)
 * - Enhanced theme metadata integration
 * - Improved audio reactivity
 */
class MediaPlayerToyService : GlyphMatrixService("MediaPlayer-Demo") {

    // Player state enum for better state management
    enum class PlayerState {
        PLAYING,    // Media is actively playing
        PAUSED,     // Media is paused
        OFFLINE,    // No media available
        LOADING,    // Media is buffering/loading
        ERROR       // Error state
    }

    private val backgroundScope = CoroutineScope(Dispatchers.IO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var currentFrameIndex = 0 // Current frame in the animation
    private var pausedFrameIndex = 0 // Frame index when paused (for smooth resume)
    private var isPlaying = false
    private var hasActiveMedia = false
    private var currentPlayerState = PlayerState.PAUSED
    
    // State prediction tracking for instant UI updates
    private var predictedPlayerState: PlayerState? = null
    private var predictionTimestamp = 0L
    private val predictionTimeoutMs = 1000L // 1 second timeout for predictions
    private var debugFrameCount = 0 // For debug logging
    
    // Frame transition support
    private var frameTransitionSequence: FrameTransitionSequence? = null
    private var isUsingTransitions = false
    
    // Logging state management to avoid spam
    private var lastLoggedThemeName = ""
    private var lastLoggedFrameType = ""
    
    // Core components
    private lateinit var mediaHelper: MediaControlHelper
    private lateinit var themeRepository: ThemeRepository
    private lateinit var audioAnalyzer: AudioAnalyzer
    private var matrixManager: GlyphMatrixManager? = null
    
    // Shake detection
    private var shakeDetector: ShakeDetector? = null
    private var isShakeEnabled = false
    private var shakeSensitivity = ShakeDetector.SENSITIVITY_MEDIUM
    private var shakePreferences: SharedPreferences? = null
    private val shakePreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        handleShakePreferenceChange(key)
    }
    
    private var currentTheme: AnimationTheme? = null
    private var currentAudioData: AudioData = AudioData(0.0, 0.0, 0.0, 0.0, false)
    
    // Settings management and caching
    private var cachedThemeSettings: ThemeSettings? = null
    private var lastSettingsCheckTime = 0L
    private val settingsCheckInterval = 5000L // Check for settings changes every 5 seconds as fallback
    private var isSettingsListenerActive = false
    
    // Audio analysis throttling to reduce log spam and CPU usage
    private var lastAudioAnalysisTime = 0L
    private var audioAnalysisInterval = 200L // Default: Update audio analysis every 200ms to reduce overhead
    
    // Static image pixel data for when no media is playing (shaped format)
    private val staticImageShapedPixels = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,0,0,0,0,0,0,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,255,0,0,0,255,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,255,255,255,0,0,0,0,255,255,255,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"
    
    private val staticImageArray by lazy { 
        try {
            // Parse the shaped pixel data and convert to flat array
            val shapedPixels = staticImageShapedPixels.split(",").map { it.trim().toInt() }
            Log.d(LOG_TAG, "Shaped pixels count: ${shapedPixels.size}")
            
            // Convert to shaped grid format
            val shapedGrid = GlyphMatrixRenderer.createEmptyShapedGrid()
            var pixelIndex = 0
            
            for (row in 0 until GlyphMatrixRenderer.TOTAL_ROWS) {
                val pixelsInRow = GlyphMatrixRenderer.getPixelsInRow(row)
                for (col in 0 until pixelsInRow) {
                    if (pixelIndex < shapedPixels.size) {
                        shapedGrid[row][col] = shapedPixels[pixelIndex]
                        pixelIndex++
                    }
                }
            }
            
            // Convert to flat array for the matrix
            GlyphMatrixRenderer.shapedGridToFlatArray(shapedGrid)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to parse static image pixels: ${e.message}")
            // Fallback to empty array if parsing fails
            IntArray(625) { 0 }
        }
    }


    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        Log.d(LOG_TAG, "MediaPlayerToyService connected - starting enhanced animation loop")
        
        // Store reference to matrix manager for immediate updates
        this.matrixManager = glyphMatrixManager
        
        // Initialize components
        mediaHelper = MediaControlHelper(context)
        themeRepository = ThemeRepository.getInstance(context)
        audioAnalyzer = AudioAnalyzer(context)
        currentTheme = themeRepository.selectedTheme
        initializeThemeTransitions(currentTheme)
        
        // Initialize theme settings
        initializeThemeSettings(currentTheme)
        
        // Start settings monitoring for real-time updates
        startSettingsMonitoring()
        
        // Log initial theme information
        logThemeInfo(currentTheme)
        
        // Delay shake detection initialization to prevent false positives at startup
        // This gives the service time to stabilize and prevents detecting the initial sensor variations
        backgroundScope.launch {
            delay(1500L) // 1.5 second delay before enabling shake detection
            Log.d(LOG_TAG, "Initializing shake detection after startup delay")
            initializeShakeDetection(context)
            registerShakePreferencesListener(context)
        }

        // Get initial state
        val initialController = mediaHelper.getActiveMediaController()
        Log.d(LOG_TAG, "Initial controller: ${initialController?.packageName}, state: ${initialController?.playbackState?.state}")
        val initialPlayerState = when {
            initialController?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING -> PlayerState.PLAYING
            initialController != null -> PlayerState.PAUSED
            else -> PlayerState.OFFLINE
        }
        currentPlayerState = initialPlayerState
        isPlaying = initialController?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
        hasActiveMedia = initialController != null
        
        // Start with proper initial frame based on actual media state
        val shouldAnimate = initialPlayerState == PlayerState.PLAYING
        val initialFrame = generateFrame(shouldAnimate, initialPlayerState)
        val themeBrightness = currentTheme?.getBrightness() ?: 255
        uiScope.launch {
            // Always use SDK brightness of 255 - themes now handle brightness in pixel values
            val matrixFrame = GlyphMatrixRenderer.createMatrixFrameWithBrightness(applicationContext, initialFrame, 255)
            glyphMatrixManager.setMatrixFrame(matrixFrame.render())
        }

        backgroundScope.launch {
            while (isActive) {
                // Very fast polling for immediate state detection
                val controller = mediaHelper.getActiveMediaController()
                val currentlyPlaying = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
                val mediaAvailable = controller != null
                
                // Determine current state
                val newPlayerState = when {
                    mediaAvailable && currentlyPlaying -> PlayerState.PLAYING
                    mediaAvailable && !currentlyPlaying -> PlayerState.PAUSED
                    !mediaAvailable -> PlayerState.OFFLINE
                    else -> PlayerState.PAUSED
                }
                
                // Handle prediction validation and state changes
                val hasPrediction = predictedPlayerState != null && !isPredictionTimedOut()
                
                if (hasPrediction) {
                    // Validate prediction against actual state
                    if (newPlayerState == predictedPlayerState) {
                        Log.d(LOG_TAG, "Prediction validated: ${predictedPlayerState} matches actual state")
                        // Prediction was correct, clear it
                        predictedPlayerState = null
                        predictionTimestamp = 0L
                    } else if (isPredictionTimedOut()) {
                        Log.w(LOG_TAG, "Prediction timed out, correcting state: predicted=${predictedPlayerState}, actual=$newPlayerState")
                        // Prediction timed out, correct the state
                        predictedPlayerState = null
                        predictionTimestamp = 0L
                        // Force update with actual state
                        currentPlayerState = newPlayerState
                        isPlaying = currentlyPlaying
                        hasActiveMedia = mediaAvailable
                    }
                    // If prediction is active and not timed out, don't override the predicted state yet
                } else {
                    // No active prediction, check for normal state changes
                    val stateChanged = newPlayerState != currentPlayerState
                    val playingChanged = currentlyPlaying != isPlaying
                    
                    if (stateChanged || playingChanged) {
                        val previousState = currentPlayerState
                        
                        // Update state immediately
                        currentPlayerState = newPlayerState
                        isPlaying = currentlyPlaying
                        hasActiveMedia = mediaAvailable
                        
                        // Handle pause/resume frame logic (only when not predicted)
                        if (previousState == PlayerState.PLAYING && newPlayerState == PlayerState.PAUSED) {
                            pausedFrameIndex = currentFrameIndex
                            Log.d(LOG_TAG, "Animation paused at frame $pausedFrameIndex")
                        } else if (previousState == PlayerState.PAUSED && newPlayerState == PlayerState.PLAYING) {
                            // Handle ScrollTheme resume
                            (currentTheme as? ScrollTheme)?.resumeScrolling()
                            // Reset animation to include opening sequence when resuming
                            if (isUsingTransitions) {
                                frameTransitionSequence?.reset(includeOpening = true)
                                currentFrameIndex = frameTransitionSequence?.getCurrentFrameIndex() ?: 0
                                Log.d(LOG_TAG, "Animation reset to opening sequence")
                            } else if (currentTheme !is ScrollTheme) {
                                currentFrameIndex = 0 // Start from beginning for non-transition themes (except ScrollTheme)
                                Log.d(LOG_TAG, "Animation reset to frame 0")
                            }
                        }
                        
                        Log.d(LOG_TAG, "State change detected: $previousState -> $newPlayerState (playing: $currentlyPlaying)")
                    }
                }
                
                // Throttled audio analysis 
                val currentTime = System.currentTimeMillis()
                audioAnalysisInterval = if (isPlaying) 100L else 200L
                if (currentTime - lastAudioAnalysisTime > audioAnalysisInterval) {
                    currentAudioData = audioAnalyzer.updateAudioAnalysis()
                    lastAudioAnalysisTime = currentTime
                }
                
                // Validate cached settings periodically (fallback mechanism) - DISABLED due to ANR risk
                // validateCachedSettings() // Settings updates work via flow notifications
                
                // Determine animation behavior based on effective state (includes predictions)
                val effectiveState = getEffectivePlayerState()
                val shouldAnimate = effectiveState == PlayerState.PLAYING
                
                // Generate frame based on effective state
                val pixelArray = generateFrame(shouldAnimate, effectiveState)
                
                // Get current theme brightness
                val themeBrightness = currentTheme?.getBrightness() ?: 255
                
                // Debug: Log brightness info
                debugFrameCount++
                if (debugFrameCount % 300 == 0) { // Log every 300 frames to avoid spam
                    val maxPixelValue = pixelArray.maxOrNull() ?: 0
                    val nonZeroPixels = pixelArray.count { it > 0 }
                    if (nonZeroPixels > 0) {
                        Log.d(LOG_TAG, "Using GlyphMatrixObject API - Theme brightness: $themeBrightness, Max pixel value: $maxPixelValue, Non-zero pixels: $nonZeroPixels, Theme: ${currentTheme?.getThemeName()}")
                    }
                }

                uiScope.launch(Dispatchers.Main.immediate) {
                    // Always use SDK brightness of 255 - themes now handle brightness in pixel values
                    val matrixFrame = GlyphMatrixRenderer.createMatrixFrameWithBrightness(applicationContext, pixelArray, 255)
                    glyphMatrixManager.setMatrixFrame(matrixFrame.render())
                }

                // Check if theme was changed and update current theme
                val selectedTheme = themeRepository.selectedTheme
                if (currentTheme?.getThemeName() != selectedTheme.getThemeName()) {
                    // Deactivate old theme
                    (currentTheme as? ScrollTheme)?.onDeactivate()
                    
                    currentTheme = selectedTheme
                    initializeThemeTransitions(selectedTheme)
                    initializeThemeSettings(selectedTheme) // Apply settings to new theme
                    logThemeInfo(currentTheme)
                    
                    // Activate new theme
                    (currentTheme as? ScrollTheme)?.onActivate()
                    
                    Log.d(LOG_TAG, "Theme changed to: ${currentTheme?.getThemeName()}")
                }

                // Calculate frame duration with individual frame support
                val frameDuration = calculateFrameDuration()
                
                // Apply audio reactivity to frame duration more conservatively
                val audioReactiveSpeed = if (currentTheme is AudioReactiveTheme && currentAudioData.isPlaying) {
                    // For audio-reactive themes, use consistent timing to avoid desync
                    // Only slight speed variations to maintain visual flow
                    val speedModifier = if (currentAudioData.beatIntensity > 0.7) 0.85 else 1.0
                    (frameDuration * speedModifier).toLong().coerceAtLeast(40L)
                } else if (currentAudioData.isPlaying && currentAudioData.beatIntensity > 0.1) {
                    // For non-audio-reactive themes, allow more variation
                    (frameDuration * (1.0 - currentAudioData.beatIntensity * 0.3)).toLong().coerceAtLeast(25L)
                } else {
                    frameDuration
                }
                
                // Very fast polling for immediate state response
                val finalDelay = when {
                    shouldAnimate -> audioReactiveSpeed.coerceAtLeast(25L) // Minimum 25ms for smooth animation
                    hasActiveMedia -> 10L // Very fast polling when media available for immediate pause response
                    else -> 100L // Moderate updates when offline
                }
                
                delay(finalDelay)

                // Advance animation frame if should animate
                if (shouldAnimate) {
                    advanceFrameIndex()
                }
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        Log.d(LOG_TAG, "MediaPlayerToyService disconnected")
        
        // Cleanup resources
        mediaHelper.cleanup()
        audioAnalyzer.cleanup()
        
        // Cleanup shake detector and preferences listener
        unregisterShakePreferencesListener()
        shakeDetector?.cleanup()
        shakeDetector = null
        
        // Clear settings cache and monitoring
        cachedThemeSettings = null
        isSettingsListenerActive = false
        
        // Cancel coroutine scopes
        backgroundScope.cancel()
        uiScope.cancel()
    }

    override fun onTouchPointPressed() {
        // Short press disabled - no functionality
        Log.v(LOG_TAG, "Short press detected but no action assigned")
    }

    override fun onTouchPointLongPress() {
        // Long press to toggle play/pause with instant visual feedback
        Log.d(LOG_TAG, "Long press detected - toggling media playback with instant feedback")
        
        // Debug: Test with raw 255 values
        // To test maximum brightness capability:
        // 1. Uncomment the next two lines
        // 2. Build and run the app
        // 3. Long press on the Glyph Matrix
        // 4. Check if the center circle appears at full brightness
        // 5. Check logcat for "Test frame - Max brightness: 255"
        // testMaxBrightness()
        // return
        
        try {
            // Predict the new state immediately for instant UI feedback
            val predictedState = predictNextPlayerState()
            if (predictedState != null) {
                Log.d(LOG_TAG, "Predicting state change: ${currentPlayerState} -> $predictedState")
                
                // Apply instant state change
                applyInstantStateChange(predictedState)
                
                // Force immediate frame update
                val pixelArray = generateFrame(predictedState == PlayerState.PLAYING, predictedState)
                val themeBrightness = currentTheme?.getBrightness() ?: 255
                uiScope.launch(Dispatchers.Main.immediate) {
                    // Always use SDK brightness of 255 - themes now handle brightness in pixel values
                    val matrixFrame = GlyphMatrixRenderer.createMatrixFrameWithBrightness(applicationContext, pixelArray, 255)
                    matrixManager?.setMatrixFrame(matrixFrame.render())
                }
            }
            
            // Send the actual media command
            val success = mediaHelper.togglePlayPause()
            if (success) {
                Log.d(LOG_TAG, "Media toggle command sent after instant UI update")
            } else {
                Log.w(LOG_TAG, "Failed to toggle media playback - reverting prediction")
                // Revert prediction if command failed
                revertPrediction()
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Media control error: ${e.message}")
            // Revert prediction on any error
            revertPrediction()
        }
    }


    /**
     * Calculate frame duration with support for individual frame durations
     */
    private fun calculateFrameDuration(): Long {
        val theme = currentTheme ?: return 150L
        
        return when (theme) {
            is ThemeTemplate -> {
                if (isUsingTransitions) {
                    // Use transition sequence duration
                    frameTransitionSequence?.getCurrentDuration() ?: theme.getAnimationSpeed()
                } else if (theme.hasIndividualFrameDurations()) {
                    // Use individual frame durations
                    theme.getFrameDuration(currentFrameIndex)
                } else {
                    // Use global animation speed
                    theme.getAnimationSpeed()
                }
            }
            else -> theme.getAnimationSpeed()
        }
    }

    /**
     * Advance frame index with consistent progression for audio-reactive themes
     */
    private fun advanceFrameIndex() {
        if (isUsingTransitions) {
            // Use frame transitions
            frameTransitionSequence?.advance()
            // Update currentFrameIndex to match the transition sequence
            currentFrameIndex = frameTransitionSequence?.getCurrentFrameIndex() ?: 0
        } else {
            // Use standard frame progression
            val frameCount = currentTheme?.getFrameCount() ?: 1
            
            // For audio-reactive themes, maintain consistent frame progression
            // Frame skipping should be handled within the theme's audio-reactive logic, not here
            if (currentTheme is AudioReactiveTheme && currentAudioData.isPlaying) {
                // Always advance by 1 for audio-reactive themes to maintain sync
                currentFrameIndex = (currentFrameIndex + 1) % frameCount
                
                // Only log beats occasionally to reduce spam
                if (currentAudioData.beatIntensity > 0.5 && currentFrameIndex % 4 == 0) {
                    Log.v(LOG_TAG, "Beat! Frame: $currentFrameIndex/$frameCount, intensity: ${String.format("%.2f", currentAudioData.beatIntensity)}, bass: ${String.format("%.2f", currentAudioData.bassLevel)}")
                }
            } else {
                // For non-audio-reactive themes, use normal progression (no beat skipping)
                currentFrameIndex = (currentFrameIndex + 1) % frameCount
            }
        }
    }
    
    /**
     * Initialize frame transitions for the given theme.
     */
    private fun initializeThemeTransitions(theme: AnimationTheme?) {
        if (theme is ThemeTemplate && theme.hasFrameTransitions()) {
            try {
                frameTransitionSequence = theme.createTransitionSequence()
                isUsingTransitions = true
                currentFrameIndex = frameTransitionSequence?.getCurrentFrameIndex() ?: 0
                Log.d(LOG_TAG, "Initialized frame transitions for theme: ${theme.getThemeName()}")
                Log.v(LOG_TAG, "Transition debug: ${frameTransitionSequence?.getDebugInfo()}")
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Failed to initialize transitions for theme ${theme.getThemeName()}: ${e.message}")
                frameTransitionSequence = null
                isUsingTransitions = false
            }
        } else {
            frameTransitionSequence = null
            isUsingTransitions = false
            currentFrameIndex = 0
        }
    }

    /**
     * Enhanced frame generation with state-specific frame support
     */
    private fun generateFrame(shouldAnimate: Boolean = false, playerState: PlayerState = currentPlayerState): IntArray {
        val theme = currentTheme ?: return IntArray(625) { 0 }
        
        // Use state-specific frames for ThemeTemplate when not animating
        if (!shouldAnimate && theme is ThemeTemplate) {
            val frameType = when (playerState) {
                PlayerState.PAUSED -> "PAUSED"
                PlayerState.OFFLINE -> "OFFLINE"
                PlayerState.LOADING -> "LOADING"
                PlayerState.ERROR -> "ERROR"
                else -> "NONE"
            }
            
            // Only log when frame type changes
            if (frameType != lastLoggedFrameType) {
                Log.d(LOG_TAG, "Switching to $frameType frame for ${playerState.name} state")
                lastLoggedFrameType = frameType
            }
            
            val stateFrame = when (playerState) {
                PlayerState.PAUSED -> {
                    // Use theme's custom paused frame if available and not empty, 
                    // otherwise use smooth pause (freeze on current animation frame)
                    if (theme.pausedFrame.isNotEmpty()) {
                        theme.pausedFrame
                    } else {
                        // No custom paused frame - freeze on the frame where animation was paused
                        theme.generateFrame(pausedFrameIndex)
                    }
                }
                PlayerState.OFFLINE -> theme.offlineFrame
                PlayerState.LOADING -> theme.loadingFrame
                PlayerState.ERROR -> theme.errorFrame
                else -> null
            }
            
            // Use state-specific frame if available and not empty
            if (stateFrame != null && stateFrame.size > 0) {
                return stateFrame
            } else if (frameType != "NONE") {
                Log.w(LOG_TAG, "State frame for ${playerState.name} is null or empty, falling back to animation frame")
            }
        } else {
            // Clear logged frame type when animating
            if (lastLoggedFrameType.isNotEmpty()) {
                Log.d(LOG_TAG, "Switching to animation frames")
                lastLoggedFrameType = ""
            }
        }
        
        // Use audio-reactive frame generation if theme supports it and we have audio data
        return if (theme is AudioReactiveTheme && currentAudioData.isPlaying) {
            theme.generateAudioReactiveFrame(currentFrameIndex, currentAudioData)
        } else {
            theme.generateFrame(currentFrameIndex)
        }
    }

    /**
     * Predict the next player state based on current state for instant UI feedback
     */
    private fun predictNextPlayerState(): PlayerState? {
        return when (currentPlayerState) {
            PlayerState.PLAYING -> PlayerState.PAUSED
            PlayerState.PAUSED -> PlayerState.PLAYING
            PlayerState.OFFLINE -> null // Can't toggle when offline
            PlayerState.LOADING -> null // Don't predict during loading
            PlayerState.ERROR -> null // Don't predict during error
        }
    }
    
    /**
     * Apply instant state change for immediate UI feedback
     */
    private fun applyInstantStateChange(newState: PlayerState) {
        predictedPlayerState = newState
        predictionTimestamp = System.currentTimeMillis()
        
        // Handle pause/resume frame logic for prediction
        if (currentPlayerState == PlayerState.PLAYING && newState == PlayerState.PAUSED) {
            pausedFrameIndex = currentFrameIndex
            Log.d(LOG_TAG, "Predicted animation pause at frame $pausedFrameIndex")
        } else if (currentPlayerState == PlayerState.PAUSED && newState == PlayerState.PLAYING) {
            // Reset animation to include opening sequence when resuming
            if (isUsingTransitions) {
                frameTransitionSequence?.reset(includeOpening = true)
                currentFrameIndex = frameTransitionSequence?.getCurrentFrameIndex() ?: 0
                Log.d(LOG_TAG, "Predicted animation reset to opening sequence")
            } else {
                currentFrameIndex = 0 // Start from beginning for non-transition themes
                Log.d(LOG_TAG, "Predicted animation reset to frame 0")
            }
        }
        
        // Update current state for immediate effect (will be validated later)
        currentPlayerState = newState
        isPlaying = newState == PlayerState.PLAYING
    }
    
    /**
     * Revert prediction if media command failed
     */
    private fun revertPrediction() {
        if (predictedPlayerState != null) {
            Log.d(LOG_TAG, "Reverting prediction, restoring previous state")
            
            // Revert to previous state based on prediction
            currentPlayerState = when (predictedPlayerState) {
                PlayerState.PLAYING -> PlayerState.PAUSED
                PlayerState.PAUSED -> PlayerState.PLAYING
                else -> currentPlayerState
            }
            isPlaying = currentPlayerState == PlayerState.PLAYING
            
            // Clear prediction
            predictedPlayerState = null
            predictionTimestamp = 0L
            
            // Force frame update with reverted state
            val pixelArray = generateFrame(isPlaying, currentPlayerState)
            val themeBrightness = currentTheme?.getBrightness() ?: 255
            uiScope.launch(Dispatchers.Main.immediate) {
                // Always use SDK brightness of 255 - themes now handle brightness in pixel values
                val matrixFrame = GlyphMatrixRenderer.createMatrixFrameWithBrightness(applicationContext, pixelArray, 255)
                matrixManager?.setMatrixFrame(matrixFrame.render())
            }
        }
    }
    
    /**
     * Check if current prediction has timed out
     */
    private fun isPredictionTimedOut(): Boolean {
        return predictedPlayerState != null && 
               (System.currentTimeMillis() - predictionTimestamp > predictionTimeoutMs)
    }
    
    /**
     * Get the effective player state (predicted state if active, otherwise current state)
     */
    private fun getEffectivePlayerState(): PlayerState {
        return if (predictedPlayerState != null && !isPredictionTimedOut()) {
            predictedPlayerState!!
        } else {
            currentPlayerState
        }
    }

    /**
     * Log comprehensive theme information
     */
    private fun logThemeInfo(theme: AnimationTheme?) {
        if (theme == null) return
        
        val themeName = theme.getThemeName()
        if (themeName == lastLoggedThemeName) return // Avoid duplicate logging
        
        Log.i(LOG_TAG, "=== Theme Information ===")
        Log.i(LOG_TAG, "Name: $themeName")
        Log.i(LOG_TAG, "Frame Count: ${theme.getFrameCount()}")
        Log.i(LOG_TAG, "Animation Speed: ${theme.getAnimationSpeed()}ms")
        Log.i(LOG_TAG, "Brightness: ${theme.getBrightness()}")
        Log.i(LOG_TAG, "Description: ${theme.getDescription()}")
        
        // Enhanced logging for ThemeTemplate
        if (theme is ThemeTemplate) {
            Log.i(LOG_TAG, "--- Enhanced Theme Metadata ---")
            Log.i(LOG_TAG, "Author: ${theme.authorName}")
            Log.i(LOG_TAG, "Version: ${theme.version}")
            Log.i(LOG_TAG, "Category: ${theme.category}")
            Log.i(LOG_TAG, "Complexity: ${theme.complexity}")
            Log.i(LOG_TAG, "Tags: ${theme.tags.joinToString(", ")}")
            Log.i(LOG_TAG, "Individual Frame Durations: ${theme.hasIndividualFrameDurations()}")
            Log.i(LOG_TAG, "Reactive: ${theme.isReactive}")
            Log.i(LOG_TAG, "Fade Transitions: ${theme.supportsFadeTransitions}")
            
            if (theme.hasIndividualFrameDurations()) {
                val durations = (0 until theme.getFrameCount()).map { 
                    "Frame $it: ${theme.getFrameDuration(it)}ms" 
                }.joinToString(", ")
                Log.i(LOG_TAG, "Frame Durations: $durations")
            }
        }
        
        // Audio reactive theme info
        if (theme is AudioReactiveTheme) {
            Log.i(LOG_TAG, "Audio Reactive: Yes")
        }
        
        Log.i(LOG_TAG, "========================")
        lastLoggedThemeName = themeName
    }

    /**
     * Initialize and apply theme settings for the given theme.
     * This method loads saved settings and applies them to the theme instance.
     */
    private fun initializeThemeSettings(theme: AnimationTheme?) {
        if (theme == null) {
            Log.d(LOG_TAG, "No theme provided for settings initialization")
            cachedThemeSettings = null
            return
        }
        
        try {
            if (theme is ThemeSettingsProvider) {
                val themeId = theme.getSettingsId()
                val settings = themeRepository.getThemeSettings(themeId)
                
                if (settings != null) {
                    // Apply the loaded settings to the theme
                    theme.applySettings(settings)
                    cachedThemeSettings = settings
                    Log.d(LOG_TAG, "Applied cached settings to theme: $themeId")
                    
                    // Log key settings for debugging
                    logThemeSettings(themeId, settings)
                } else {
                    Log.d(LOG_TAG, "No saved settings found for theme: $themeId, using defaults")
                    cachedThemeSettings = null
                }
            } else {
                Log.d(LOG_TAG, "Theme ${theme.getThemeName()} does not support settings")
                cachedThemeSettings = null
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize settings for theme: ${theme.getThemeName()}", e)
            cachedThemeSettings = null
        }
    }
    
    /**
     * Start monitoring for settings changes using flow-based notifications.
     * This enables real-time settings application without service restart.
     */
    private fun startSettingsMonitoring() {
        if (isSettingsListenerActive) {
            Log.d(LOG_TAG, "Settings monitoring already active")
            return
        }
        
        try {
            backgroundScope.launch {
                themeRepository.settingsChangedFlow.collect { (themeId, settings) ->
                    try {
                        Log.d(LOG_TAG, "Received settings change notification for theme: $themeId")
                        
                        // Check if this is for the current theme
                        val currentThemeId = (currentTheme as? ThemeSettingsProvider)?.getSettingsId()
                        if (currentThemeId == themeId) {
                            // Apply settings immediately
                            (currentTheme as? ThemeSettingsProvider)?.applySettings(settings)
                            cachedThemeSettings = settings
                            
                            // Recreate transition sequence if theme uses transitions
                            // This ensures animation style changes take effect immediately
                            val theme = currentTheme
                            if (theme is AnimationTheme) {
                                initializeThemeTransitions(theme)
                                Log.d(LOG_TAG, "Recreated transition sequence after settings update")
                            }
                            
                            Log.i(LOG_TAG, "Applied real-time settings update to current theme: $themeId")
                            logThemeSettings(themeId, settings)
                        } else {
                            Log.d(LOG_TAG, "Settings change is for different theme ($themeId), current is $currentThemeId")
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error applying real-time settings update: ${e.message}", e)
                    }
                }
            }
            isSettingsListenerActive = true
            Log.d(LOG_TAG, "Started settings monitoring with flow-based notifications")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to start settings monitoring: ${e.message}", e)
            isSettingsListenerActive = false
        }
    }
    
    /**
     * Log theme settings for debugging purposes.
     */
    private fun logThemeSettings(themeId: String, settings: ThemeSettings) {
        Log.v(LOG_TAG, "=== Theme Settings Debug ===")
        Log.v(LOG_TAG, "Theme ID: $themeId")
        Log.v(LOG_TAG, "Settings values: ${settings.userValues}")
        
        // Log common settings values if present
        try {
            val animSpeed = settings.userValues["animation_speed"] ?: settings.userValues["rotation_speed"]
            val brightness = settings.userValues["brightness"]
            val frameCount = settings.userValues["frame_count"]
            
            if (animSpeed != null) Log.v(LOG_TAG, "Animation Speed: $animSpeed")
            if (brightness != null) Log.v(LOG_TAG, "Brightness: $brightness")
            if (frameCount != null) Log.v(LOG_TAG, "Frame Count: $frameCount")
        } catch (e: Exception) {
            Log.v(LOG_TAG, "Could not log specific setting values: ${e.message}")
        }
        
        Log.v(LOG_TAG, "===========================")
    }
    
    /**
     * Validate and refresh cached settings if needed.
     * This serves as a fallback mechanism in case flow notifications fail.
     */
    private fun validateCachedSettings() {
        val currentTime = System.currentTimeMillis()
        
        // Only check periodically to avoid performance impact
        if (currentTime - lastSettingsCheckTime < settingsCheckInterval) {
            return
        }
        
        lastSettingsCheckTime = currentTime
        
        try {
            val theme = currentTheme
            if (theme is ThemeSettingsProvider) {
                val themeId = theme.getSettingsId()
                val latestSettings = themeRepository.getThemeSettings(themeId)
                
                // Compare with cached settings (simple check)
                if (latestSettings != null && latestSettings != cachedThemeSettings) {
                    Log.d(LOG_TAG, "Settings changed detected via fallback check for theme: $themeId")
                    theme.applySettings(latestSettings)
                    cachedThemeSettings = latestSettings
                    
                    // Recreate transition sequence if theme uses transitions
                    if (theme is AnimationTheme) {
                        initializeThemeTransitions(theme)
                        Log.d(LOG_TAG, "Recreated transition sequence after fallback settings update")
                    }
                    
                    logThemeSettings(themeId, latestSettings)
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error during fallback settings validation: ${e.message}")
        }
    }

    /**
     * Initialize shake detection based on user preferences
     */
    private fun initializeShakeDetection(context: Context) {
        try {
            // Load shake preferences
            val prefs = context.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
            isShakeEnabled = prefs.getBoolean("shake_to_skip_enabled", false)
            val sensitivityValue = prefs.getFloat("shake_sensitivity", ShakeDetector.SENSITIVITY_MEDIUM)
            shakeSensitivity = sensitivityValue
            val skipDelay = prefs.getLong("shake_skip_delay", 2000L)
            
            if (isShakeEnabled) {
                Log.d(LOG_TAG, "Initializing shake detection - enabled: $isShakeEnabled, sensitivity: $shakeSensitivity, delay: ${skipDelay}ms")
                
                // Create and configure shake detector
                shakeDetector = ShakeDetector(context)
                shakeDetector?.apply {
                    initialize()
                    setSensitivity(shakeSensitivity)
                    setCooldown(skipDelay)
                    setOnShakeListener(object : ShakeDetector.OnShakeListener {
                        override fun onShake(force: Float) {
                            handleShakeDetected(force)
                        }
                    })
                    startListening()
                }
                
                Log.i(LOG_TAG, "Shake detection started with sensitivity: $shakeSensitivity, cooldown: ${skipDelay}ms")
            } else {
                Log.d(LOG_TAG, "Shake detection disabled by user preference")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to initialize shake detection: ${e.message}", e)
        }
    }
    
    /**
     * Handle shake event - skip to next track
     */
    private fun handleShakeDetected(force: Float) {
        Log.d(LOG_TAG, "Shake detected with force: $force")
        
        // Check if we have active media
        if (!hasActiveMedia) {
            Log.d(LOG_TAG, "Shake detected but no active media to control")
            return
        }
        
        // Check if media is paused and whether we should skip when paused
        if (currentPlayerState == PlayerState.PAUSED) {
            val prefs = applicationContext.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
            val skipWhenPaused = prefs.getBoolean("shake_skip_when_paused", false)

            if (!skipWhenPaused) {
                Log.d(LOG_TAG, "Media is paused and skip when paused is disabled - ignoring shake")
                return
            }

            Log.d(LOG_TAG, "Media is paused but skip when paused is enabled - proceeding with skip")
        }

        // Proceed with skip
        val success = mediaHelper.skipToNext()
        if (success) {
            Log.i(LOG_TAG, "Skipped to next track via shake gesture")

            val prefs = applicationContext.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
            val feedback_when_shaked = prefs.getBoolean("feedback_when_shaked", false)

            if (feedback_when_shaked){
                // Provide haptic feedback for successful skip
                shakeDetector?.provideHapticFeedback()
            }

            // Optional: Provide visual feedback via Glyph animation
            // Could show a brief "skip" animation here
        } else {
            Log.w(LOG_TAG, "Failed to skip to next track")
        }
    }
    
    /**
     * Update shake detection settings dynamically
     */
    private fun updateShakeSettings(enabled: Boolean, sensitivity: Float, skipDelay: Long = 2000L) {
        Log.d(LOG_TAG, "Updating shake settings - enabled: $enabled, sensitivity: $sensitivity, delay: ${skipDelay}ms")
        
        isShakeEnabled = enabled
        shakeSensitivity = sensitivity
        
        if (enabled) {
            if (shakeDetector == null) {
                initializeShakeDetection(applicationContext)
            } else {
                shakeDetector?.setSensitivity(sensitivity)
                shakeDetector?.setCooldown(skipDelay)
                if (!shakeDetector!!.isActive()) {
                    shakeDetector?.startListening()
                }
            }
        } else {
            shakeDetector?.stopListening()
        }
    }

    /**
     * Register listener for shake preferences changes
     */
    private fun registerShakePreferencesListener(context: Context) {
        try {
            shakePreferences = context.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
            shakePreferences?.registerOnSharedPreferenceChangeListener(shakePreferenceListener)
            Log.d(LOG_TAG, "Registered shake preferences listener")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to register shake preferences listener: ${e.message}", e)
        }
    }

    /**
     * Unregister shake preferences listener
     */
    private fun unregisterShakePreferencesListener() {
        try {
            shakePreferences?.unregisterOnSharedPreferenceChangeListener(shakePreferenceListener)
            shakePreferences = null
            Log.d(LOG_TAG, "Unregistered shake preferences listener")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to unregister shake preferences listener: ${e.message}", e)
        }
    }

    /**
     * Handle changes to shake-related preferences
     */
    private fun handleShakePreferenceChange(key: String?) {
        when (key) {
            "shake_to_skip_enabled", "shake_sensitivity", "shake_skip_delay" -> {
                Log.d(LOG_TAG, "Shake preference changed: $key")
                
                // Reload all shake preferences
                shakePreferences?.let { prefs ->
                    val enabled = prefs.getBoolean("shake_to_skip_enabled", false)
                    val sensitivity = prefs.getFloat("shake_sensitivity", ShakeDetector.SENSITIVITY_MEDIUM)
                    val skipDelay = prefs.getLong("shake_skip_delay", 2000L)
                    
                    // Update shake settings with new values
                    updateShakeSettings(enabled, sensitivity, skipDelay)
                }
            }
        }
    }

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
        private val LOG_TAG = MediaPlayerToyService::class.java.simpleName
    }
}