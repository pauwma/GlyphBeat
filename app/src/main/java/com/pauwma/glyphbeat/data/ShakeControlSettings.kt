package com.pauwma.glyphbeat.data

import com.pauwma.glyphbeat.services.shake.ShakeDetector

/**
 * Enum representing different shake control behaviors
 */
enum class ShakeBehavior(val id: String, val displayName: String, val description: String) {
    SKIP("skip", "Skip", "Advance to next track on shake"),
    PLAY_PAUSE("play_pause", "Play/Pause", "Toggle playback state on shake"),
    AUTO_START("auto_start", "Auto-Start", "Toggle Auto-Start setting on shake")
}

/**
 * Base class for behavior-specific settings
 */
sealed class BehaviorSettings {
    /**
     * Settings specific to Skip behavior
     */
    data class SkipSettings(
        val skipDelay: Long = 3500L,                    // Delay between shake detections (ms)
        val skipWhenPaused: Boolean = false,            // Allow skipping when media is paused
        val skipWhenUnlocked: Boolean = false           // Allow skipping when device is unlocked
    ) : BehaviorSettings()
    
    /**
     * Settings specific to Play/Pause behavior
     */
    data class PlayPauseSettings(
        val lockScreenBehavior: Boolean = true,         // Allow play/pause when device is locked (default: true)
        val autoResumeDelay: Long = 0L                  // Auto-resume delay in ms (0 = disabled)
    ) : BehaviorSettings()
    
    /**
     * Settings specific to Auto-Start behavior
     */
    data class AutoStartSettings(
        val timeout: Long = 0L,                         // Auto-start timeout in ms (0 = disabled)
        val batteryAwareness: Boolean = false,          // Enable battery-aware auto-start
        val batteryThreshold: Int = 10                  // Battery percentage threshold (5-25%)
    ) : BehaviorSettings()
}

/**
 * Complete shake control settings data class
 */
data class ShakeControlSettings(
    val enabled: Boolean = false,                       // Master enable/disable
    val behavior: ShakeBehavior = ShakeBehavior.SKIP,   // Selected behavior
    val sensitivity: Float = ShakeDetector.SENSITIVITY_MEDIUM, // Shake sensitivity
    val hapticFeedback: Boolean = true,                 // Haptic feedback on shake
    val behaviorSettings: BehaviorSettings = BehaviorSettings.SkipSettings() // Behavior-specific settings
) {
    
    companion object {
        /**
         * Timeout options for sliders (in milliseconds)
         */
        val TIMEOUT_OPTIONS = listOf(
            0L to "Disabled",
            3000L to "3s",
            5000L to "5s", 
            10000L to "10s",
            15000L to "15s",
            30000L to "30s",
            60000L to "1m"
        )

        val TIMEOUT_OPTIONS_SHORT = listOf(
            0L to "Disabled",
            500L to "0.5s",
            1000L to "1s",
            1500L to "1.5s",
            2000L to "2s",
            3000L to "3s",
            5000L to "5s"
        )
        
        /**
         * Get display text for timeout value
         */
        fun getTimeoutDisplayText(timeoutMs: Long): String {
            return TIMEOUT_OPTIONS.find { it.first == timeoutMs }?.second ?: "${timeoutMs}ms"
        }
        
        /**
         * Get default settings for a specific behavior
         */
        fun getDefaultForBehavior(behavior: ShakeBehavior): ShakeControlSettings {
            val behaviorSettings = when (behavior) {
                ShakeBehavior.SKIP -> BehaviorSettings.SkipSettings()
                ShakeBehavior.PLAY_PAUSE -> BehaviorSettings.PlayPauseSettings()
                ShakeBehavior.AUTO_START -> BehaviorSettings.AutoStartSettings()
            }
            
            return ShakeControlSettings(
                behavior = behavior,
                behaviorSettings = behaviorSettings
            )
        }
    }
}

/**
 * Extension functions for easier access to typed behavior settings
 */
fun ShakeControlSettings.getSkipSettings(): BehaviorSettings.SkipSettings? {
    return behaviorSettings as? BehaviorSettings.SkipSettings
}

fun ShakeControlSettings.getPlayPauseSettings(): BehaviorSettings.PlayPauseSettings? {
    return behaviorSettings as? BehaviorSettings.PlayPauseSettings
}

fun ShakeControlSettings.getAutoStartSettings(): BehaviorSettings.AutoStartSettings? {
    return behaviorSettings as? BehaviorSettings.AutoStartSettings
}

/**
 * Helper functions for settings validation
 */
object ShakeControlSettingsValidator {
    
    fun validateBatteryThreshold(threshold: Int): Int {
        return threshold.coerceIn(5, 25)
    }
    
    fun validateTimeout(timeout: Long): Long {
        val validTimeouts = ShakeControlSettings.TIMEOUT_OPTIONS.map { it.first }
        return if (timeout in validTimeouts) timeout else 0L
    }
    
    fun validateSensitivity(sensitivity: Float): Float {
        return when {
            sensitivity <= ShakeDetector.SENSITIVITY_HIGH -> ShakeDetector.SENSITIVITY_HIGH
            sensitivity <= ShakeDetector.SENSITIVITY_MEDIUM -> ShakeDetector.SENSITIVITY_MEDIUM
            else -> ShakeDetector.SENSITIVITY_LOW
        }
    }
}