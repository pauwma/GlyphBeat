package com.pauwma.glyphbeat.themes.base

import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider

/**
 * Abstract base class for track control themes in the Glyph Matrix display.
 * 
 * Track control themes are designed for media navigation (next/previous track)
 * and support different visual states based on user interaction.
 * 
 * Key features:
 * - State-based rendering (idle, pressed, long-pressed)
 * - Support for both static and animated frames
 * - Direction-aware visuals (next vs previous)
 * - Shared theme management between services
 * - Optional settings support via ThemeSettingsProvider
 */
abstract class TrackControlTheme : AnimationTheme() {
    
    /**
     * Interaction states for track control themes
     */
    enum class InteractionState {
        IDLE,           // No user interaction
        PRESSED,        // User has pressed but not released
        LONG_PRESSED,   // User has performed a long press
        RELEASED        // User has released after interaction
    }
    
    /**
     * Direction for track control (affects visual representation)
     */
    enum class Direction {
        NEXT,     // Next track (typically right arrow or forward motion)
        PREVIOUS  // Previous track (typically left arrow or backward motion)
    }
    
    // Settings support - themes can override these to be settings-driven
    protected open var settingsBrightness: Int? = null
    protected open var settingsOpacityIdle: Int? = null
    protected open var settingsOpacityPressed: Int? = null
    protected open var settingsAnimationSpeed: Long? = null
    
    /**
     * Gets the frame for a specific interaction state and direction.
     * This is the primary method for state-based rendering.
     * 
     * @param state Current interaction state
     * @param direction Track control direction (next/previous)
     * @param frameIndex Current animation frame index (for animated themes)
     * @return IntArray representing the 25x25 matrix frame
     */
    abstract fun getStateFrame(
        state: InteractionState,
        direction: Direction,
        frameIndex: Int = 0
    ): IntArray
    
    /**
     * Indicates whether this theme uses animation for the given state.
     * Static themes should return false, animated themes return true.
     * 
     * @param state The interaction state to check
     * @return True if the theme animates in this state
     */
    open fun isAnimatedForState(state: InteractionState): Boolean = false
    
    /**
     * Gets the number of animation frames for a specific state.
     * Only relevant for animated themes.
     * 
     * @param state The interaction state
     * @return Number of frames (1 for static themes)
     */
    open fun getFrameCountForState(state: InteractionState): Int = 1
    
    /**
     * Gets the animation speed for a specific state.
     * Allows different speeds for different interaction states.
     * 
     * @param state The interaction state
     * @return Animation speed in milliseconds
     */
    open fun getAnimationSpeedForState(state: InteractionState): Long {
        return settingsAnimationSpeed ?: getAnimationSpeed()
    }
    
    /**
     * Gets the brightness/opacity for a specific state.
     * Allows themes to dim/brighten based on interaction.
     * 
     * @param state The interaction state
     * @return Brightness value 0-255
     */
    open fun getBrightnessForState(state: InteractionState): Int {
        return when (state) {
            InteractionState.IDLE -> settingsOpacityIdle ?: (getBrightness() / 2)
            InteractionState.PRESSED -> settingsOpacityPressed ?: getBrightness()
            InteractionState.LONG_PRESSED -> settingsOpacityPressed ?: getBrightness()
            InteractionState.RELEASED -> settingsOpacityIdle ?: (getBrightness() / 2)
        }
    }
    
    /**
     * Override from AnimationTheme to delegate to state-based rendering.
     * Track control themes should implement getStateFrame instead.
     */
    override fun generateFrame(frameIndex: Int): IntArray {
        // Default implementation for compatibility
        // Actual services will use getStateFrame with proper parameters
        return getStateFrame(InteractionState.IDLE, Direction.NEXT, frameIndex)
    }
    
    /**
     * Override from AnimationTheme to provide state-aware frame count.
     */
    override fun getFrameCount(): Int {
        // Return maximum frame count across all states
        return InteractionState.values().maxOf { getFrameCountForState(it) }
    }
    
    /**
     * Gets a preview frame for UI display purposes.
     * 
     * @param direction The direction to preview
     * @return Preview frame data
     */
    open fun getPreviewFrame(direction: Direction): IntArray {
        return getStateFrame(InteractionState.IDLE, direction, 0)
    }
    
    /**
     * Indicates whether this theme supports fade transitions between states.
     * 
     * @return True if fade transitions are supported
     */
    open fun supportsFadeTransitions(): Boolean = false
    
    /**
     * Gets the transition duration between states in milliseconds.
     * 
     * @return Transition duration (0 for instant transitions)
     */
    open fun getStateTransitionDuration(): Long = 0L
    
    /**
     * Gets theme metadata for enhanced UI display.
     * Track control themes can provide additional context.
     */
    open fun getThemeMetadata(): TrackControlThemeMetadata {
        return TrackControlThemeMetadata(
            supportsAnimation = InteractionState.values().any { isAnimatedForState(it) },
            supportsDirections = true,
            supportsFadeTransitions = supportsFadeTransitions(),
            category = "Track Control"
        )
    }
}

/**
 * Metadata for track control themes.
 * Provides additional information for UI and theme management.
 */
data class TrackControlThemeMetadata(
    val supportsAnimation: Boolean,
    val supportsDirections: Boolean,
    val supportsFadeTransitions: Boolean,
    val category: String,
    val tags: List<String> = emptyList()
)

/**
 * Interface for track control themes that support settings.
 * Extends the base settings provider with track-control-specific settings.
 */
interface TrackControlThemeSettingsProvider : ThemeSettingsProvider {
    /**
     * Apply track control specific settings.
     * Implementations should call super to apply base settings.
     */
    override fun applySettings(settings: ThemeSettings) {
        // Base implementation can extract common track control settings
        // Individual themes override and call super, then apply their specific settings
    }
}