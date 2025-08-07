package com.pauwma.glyphbeat.services.trackcontrol

import android.content.Context
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.pauwma.glyphbeat.core.GlyphMatrixRenderer
import com.pauwma.glyphbeat.services.GlyphMatrixService
import com.pauwma.glyphbeat.sound.MediaControlHelper
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Glyph Matrix Toy Service for next track media control.
 * 
 * Features:
 * - Long press triggers next track command
 * - Visual feedback with direction-aware animations
 * - Shared theme system with PreviousTrackToyService
 * - State-based rendering (idle, pressed, long-pressed)
 */
class NextTrackToyService : GlyphMatrixService("NextTrack-Toy") {
    
    companion object {
        private const val TAG = "NextTrackToyService"
        private const val ANIMATION_FRAME_DELAY = 50L // Base animation delay
        private const val LONG_PRESS_FEEDBACK_DURATION = 300L // Visual feedback duration
    }
    
    // Core components
    private lateinit var themeManager: TrackControlThemeManager
    private lateinit var mediaHelper: MediaControlHelper
    private var matrixManager: GlyphMatrixManager? = null
    
    // Service state
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var animationJob: Job? = null
    
    // Interaction state
    private var currentState = TrackControlTheme.InteractionState.IDLE
    private var currentFrameIndex = 0
    private var isAnimating = false
    
    override fun performOnServiceConnected(
        context: Context,
        glyphMatrixManager: GlyphMatrixManager
    ) {
        Log.d(TAG, "NextTrackToyService connected")
        
        // Store matrix manager reference
        this.matrixManager = glyphMatrixManager
        
        // Initialize components
        themeManager = TrackControlThemeManager.getInstance(context)
        mediaHelper = MediaControlHelper(context)
        
        // Initialize theme manager
        themeManager.initialize()
        
        // Start with idle state
        updateDisplay(TrackControlTheme.InteractionState.IDLE)
        
        // Start animation loop if needed
        startAnimationLoop()
        
        // Observe theme changes
        serviceScope.launch {
            themeManager.currentThemeFlow.collect { theme ->
                Log.d(TAG, "Theme changed to: ${theme.getThemeName()}")
                currentFrameIndex = 0
                updateDisplay(currentState)
            }
        }
    }
    
    override fun performOnServiceDisconnected(context: Context) {
        Log.d(TAG, "NextTrackToyService disconnected")
        
        // Stop animation
        stopAnimation()
        
        // Cleanup
        mediaHelper.cleanup()
        serviceScope.cancel()
        uiScope.cancel()
    }
    
    override fun onTouchPointPressed() {
        Log.d(TAG, "Touch pressed")
        //updateState(TrackControlTheme.InteractionState.PRESSED)
    }
    
    override fun onTouchPointLongPress() {
        Log.d(TAG, "Long press detected - triggering next track")
        
        // Update to long-pressed state
        updateState(TrackControlTheme.InteractionState.LONG_PRESSED)
        
        // Trigger media action
        serviceScope.launch {
            try {
                val success = mediaHelper.skipToNext()
                if (success) {
                    Log.d(TAG, "Successfully triggered next track")
                    
                    // Keep visual feedback for a moment
                    delay(LONG_PRESS_FEEDBACK_DURATION)
                } else {
                    Log.w(TAG, "Failed to trigger next track")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering next track", e)
            } finally {
                // Return to idle state
                updateState(TrackControlTheme.InteractionState.IDLE)
            }
        }
    }
    
    override fun onTouchPointReleased() {
        Log.d(TAG, "Touch released")
        
        // Only update to released state if we're in pressed state
        // (Long press handles its own state transition)
        if (currentState == TrackControlTheme.InteractionState.PRESSED) {
            updateState(TrackControlTheme.InteractionState.RELEASED)
            
            // Return to idle after a brief moment
            serviceScope.launch {
                delay(150)
                if (currentState == TrackControlTheme.InteractionState.RELEASED) {
                    updateState(TrackControlTheme.InteractionState.IDLE)
                }
            }
        }
    }
    
    /**
     * Updates the current interaction state and refreshes the display.
     */
    private fun updateState(newState: TrackControlTheme.InteractionState) {
        if (currentState != newState) {
            Log.d(TAG, "State transition: $currentState -> $newState")
            currentState = newState
            
            // Reset frame index on state change for animated themes
            if (themeManager.currentTheme.isAnimatedForState(newState)) {
                currentFrameIndex = 0
            }
            
            updateDisplay(newState)
        }
    }
    
    /**
     * Updates the matrix display with the current theme and state.
     */
    private fun updateDisplay(state: TrackControlTheme.InteractionState) {
        val theme = themeManager.currentTheme
        val frame = theme.getStateFrame(
            state = state,
            direction = TrackControlTheme.Direction.NEXT,
            frameIndex = currentFrameIndex
        )
        
        val brightness = theme.getBrightness()
        
        uiScope.launch {
            val matrixFrame = GlyphMatrixRenderer.createMatrixFrameWithBrightness(applicationContext, frame, brightness)
            matrixManager?.setMatrixFrame(matrixFrame.render())
        }
    }
    
    /**
     * Starts the animation loop for animated themes.
     */
    private fun startAnimationLoop() {
        animationJob?.cancel()
        
        animationJob = serviceScope.launch {
            while (isActive) {
                val theme = themeManager.currentTheme
                val state = currentState
                
                // Check if current state supports animation
                if (theme.isAnimatedForState(state)) {
                    isAnimating = true
                    
                    // Get frame count for current state
                    val frameCount = theme.getFrameCountForState(state)
                    
                    if (frameCount > 1) {
                        // Advance frame
                        currentFrameIndex = (currentFrameIndex + 1) % frameCount
                        updateDisplay(state)
                    }
                    
                    // Get animation speed for current state
                    val animationSpeed = theme.getAnimationSpeedForState(state)
                    delay(animationSpeed)
                } else {
                    isAnimating = false
                    // No animation for this state, check less frequently
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Stops the animation loop.
     */
    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        isAnimating = false
    }
}