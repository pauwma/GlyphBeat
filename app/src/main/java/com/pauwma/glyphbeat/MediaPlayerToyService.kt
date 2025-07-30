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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Enhanced Media Player Toy Service with support for individual frame durations,
 * state-specific frames, and comprehensive theme metadata integration.
 * 
 * Key Features:
 * - Variable frame timing support (each frame can have different duration)
 * - State-specific frame rendering (paused, offline, loading, error)
 * - Enhanced theme metadata integration
 * - Improved audio reactivity
 * - Better state management and logging
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
    private var currentPlayerState = PlayerState.OFFLINE
    private var pendingStateUpdate = false // Flag to handle immediate state changes
    
    // Frame transition support
    private var frameTransitionSequence: FrameTransitionSequence? = null
    private var isUsingTransitions = false
    
    // Logging state management to avoid spam
    private var lastLoggedMediaState = false
    private var lastLoggedPlayerState = PlayerState.OFFLINE
    private var lastLoggedThemeName = ""
    private var lastLoggedFrameType = ""
    private var lastLoggedControllerAvailable = false
    
    // Core components
    private lateinit var mediaHelper: MediaControlHelper
    private lateinit var themeRepository: ThemeRepository
    private lateinit var audioAnalyzer: AudioAnalyzer
    private var matrixManager: GlyphMatrixManager? = null
    
    private var currentTheme: AnimationTheme? = null
    private var currentAudioData: AudioData = AudioData(0.0, 0.0, 0.0, 0.0, false)
    
    // Audio analysis throttling to reduce log spam
    private var lastAudioAnalysisTime = 0L
    private val audioAnalysisInterval = 100L // Update audio analysis every 100ms instead of every frame
    
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
        
        // Log initial theme information
        logThemeInfo(currentTheme)

        // Start with a clean frame immediately
        val initialFrame = generateFrame()
        uiScope.launch {
            glyphMatrixManager.setMatrixFrame(initialFrame)
        }

        backgroundScope.launch {
            while (isActive) {
                // Throttled audio analysis to reduce AudioManager log spam
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastAudioAnalysisTime > audioAnalysisInterval) {
                    currentAudioData = audioAnalyzer.updateAudioAnalysis()
                    lastAudioAnalysisTime = currentTime
                }
                
                // Determine current player state and animation behavior
                val (shouldAnimate, newPlayerState) = determinePlayerState()
                updatePlayerState(newPlayerState)
                
                // Skip frame generation if we just handled an immediate state update
                if (pendingStateUpdate) {
                    pendingStateUpdate = false
                    continue
                }
                
                // Generate frame based on current state
                val pixelArray = generateFrame(shouldAnimate, currentPlayerState)

                uiScope.launch {
                    glyphMatrixManager.setMatrixFrame(pixelArray)
                }

                // Check if theme was changed and update current theme
                val selectedTheme = themeRepository.selectedTheme
                if (currentTheme?.getThemeName() != selectedTheme.getThemeName()) {
                    currentTheme = selectedTheme
                    initializeThemeTransitions(selectedTheme)
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
                
                // Dynamic delay based on state and animation
                val finalDelay = when {
                    shouldAnimate -> audioReactiveSpeed
                    hasActiveMedia -> 10L // Very fast updates when media available but paused for immediate response
                    else -> 100L // Slower updates when offline
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
        audioAnalyzer.cleanup()
        backgroundScope.cancel()
        uiScope.cancel()
    }

    override fun onTouchPointPressed() {
        // Short press disabled - no functionality
        Log.v(LOG_TAG, "Short press detected but no action assigned")
    }

    override fun onTouchPointLongPress() {
        // Long press to toggle play/pause (main toy function)
        Log.d(LOG_TAG, "Long press detected - toggling media playback")
        try {
            val success = mediaHelper.togglePlayPause()
            if (success) {
                Log.d(LOG_TAG, "Media toggle command sent")
            } else {
                Log.w(LOG_TAG, "Failed to toggle media playback - no active media controller")
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Media control error: ${e.message}")
        }
    }

    /**
     * Determine player state and whether animation should be active
     * Simple logic: Playing = animate, Paused/Offline = static
     * @return Pair of (shouldAnimate, playerState)
     */
    private fun determinePlayerState(): Pair<Boolean, PlayerState> {
        try {
            val controller = mediaHelper.getActiveMediaController()
            val currentlyPlaying = controller?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            val mediaAvailable = controller != null

            // Only log state check when controller availability changes
            if (mediaAvailable != lastLoggedControllerAvailable) {
                Log.d(LOG_TAG, "Controller availability changed: available=$mediaAvailable")
                lastLoggedControllerAvailable = mediaAvailable
            }

            // Update media availability
            if (mediaAvailable != hasActiveMedia) {
                hasActiveMedia = mediaAvailable
                Log.d(LOG_TAG, "Media availability changed: hasActiveMedia = $hasActiveMedia")
            }

            // Update playing state
            if (currentlyPlaying != isPlaying) {
                isPlaying = currentlyPlaying
                Log.d(LOG_TAG, "Media state changed: isPlaying = $isPlaying, beatIntensity: ${currentAudioData.beatIntensity}")
            }

            // Determine state with better logic
            val newState = when {
                !mediaAvailable -> PlayerState.OFFLINE
                currentlyPlaying -> PlayerState.PLAYING
                else -> PlayerState.PAUSED
            }

            // Only animate when actually playing
            val shouldAnimate = newState == PlayerState.PLAYING
            
            return Pair(shouldAnimate, newState)

        } catch (e: Exception) {
            // Handle errors gracefully
            if (e.message?.contains("notification access", ignoreCase = true) == true) {
                Log.w(LOG_TAG, "Media control requires notification access permission")
            } else {
                Log.w(LOG_TAG, "Cannot check media state: ${e.message}")
            }
            hasActiveMedia = false
            return Pair(false, PlayerState.ERROR)
        }
    }

    /**
     * Update player state and log changes
     */
    private fun updatePlayerState(newState: PlayerState) {
        if (currentPlayerState != newState) {
            Log.d(LOG_TAG, "Player state changed: ${currentPlayerState} -> $newState")
            
            // When transitioning from PLAYING to PAUSED, save the current frame and trigger immediate update
            if (currentPlayerState == PlayerState.PLAYING && newState == PlayerState.PAUSED) {
                pausedFrameIndex = currentFrameIndex
                Log.d(LOG_TAG, "Animation paused at frame $pausedFrameIndex - triggering immediate frame update")
                
                // Set flag to skip next regular frame generation
                pendingStateUpdate = true
                
                // Trigger immediate frame update for pause state with higher priority
                uiScope.launch {
                    try {
                        val pauseFrame = generateFrame(shouldAnimate = false, playerState = newState)
                        matrixManager?.setMatrixFrame(pauseFrame)
                        Log.v(LOG_TAG, "Immediate pause frame displayed")
                    } catch (e: Exception) {
                        Log.w(LOG_TAG, "Failed to set immediate pause frame: ${e.message}")
                        pendingStateUpdate = false // Reset flag on error
                    }
                }
            }
            // When transitioning from PAUSED to PLAYING, resume from the paused frame
            else if (currentPlayerState == PlayerState.PAUSED && newState == PlayerState.PLAYING) {
                currentFrameIndex = pausedFrameIndex
                Log.d(LOG_TAG, "Animation resumed from frame $currentFrameIndex")
            }
            
            currentPlayerState = newState
            lastLoggedPlayerState = newState
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

    private companion object {
        private const val WIDTH = 25
        private const val HEIGHT = 25
        private val LOG_TAG = MediaPlayerToyService::class.java.simpleName
    }
}