package com.pauwma.glyphbeat

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.animation.styles.ThemeTemplate
import com.pauwma.glyphbeat.animation.styles.FrameTransitionSequence
import com.pauwma.glyphbeat.sound.AudioAnalyzer
import com.pauwma.glyphbeat.sound.AudioData
import com.pauwma.glyphbeat.sound.AudioReactiveTheme
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.ui.ThemeRepository
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
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
        
        // No callback registration needed - using fast polling instead for reliability
        
        // Log initial theme information
        logThemeInfo(currentTheme)

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
        uiScope.launch {
            glyphMatrixManager.setMatrixFrame(initialFrame)
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
                            currentFrameIndex = pausedFrameIndex
                            Log.d(LOG_TAG, "Animation resumed from frame $currentFrameIndex")
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

                uiScope.launch(Dispatchers.Main.immediate) {
                    glyphMatrixManager.setMatrixFrame(pixelArray)
                }

                // Check if theme was changed and update current theme
                val selectedTheme = themeRepository.selectedTheme
                if (currentTheme?.getThemeName() != selectedTheme.getThemeName()) {
                    currentTheme = selectedTheme
                    initializeThemeTransitions(selectedTheme)
                    initializeThemeSettings(selectedTheme) // Apply settings to new theme
                    logThemeInfo(currentTheme)
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
        try {
            // Predict the new state immediately for instant UI feedback
            val predictedState = predictNextPlayerState()
            if (predictedState != null) {
                Log.d(LOG_TAG, "Predicting state change: ${currentPlayerState} -> $predictedState")
                
                // Apply instant state change
                applyInstantStateChange(predictedState)
                
                // Force immediate frame update
                val pixelArray = generateFrame(predictedState == PlayerState.PLAYING, predictedState)
                uiScope.launch(Dispatchers.Main.immediate) {
                    matrixManager?.setMatrixFrame(pixelArray)
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
            currentFrameIndex = pausedFrameIndex
            Log.d(LOG_TAG, "Predicted animation resume from frame $currentFrameIndex")
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
            uiScope.launch(Dispatchers.Main.immediate) {
                matrixManager?.setMatrixFrame(pixelArray)
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
                    logThemeSettings(themeId, latestSettings)
                }
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error during fallback settings validation: ${e.message}")
        }
    }

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
        private val LOG_TAG = MediaPlayerToyService::class.java.simpleName
    }
}