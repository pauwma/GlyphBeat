package com.pauwma.glyphbeat.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.remember
import com.pauwma.glyphbeat.services.shake.ShakeDetector

/**
 * Manager class for handling shake control settings persistence and migration
 */
class ShakeControlSettingsManager(private val context: Context) {

    companion object {
        private const val LOG_TAG = "ShakeControlSettings"
        private const val PREFS_NAME = "glyph_settings"
        private const val SETTINGS_VERSION_KEY = "shake_settings_version"
        private const val CURRENT_SETTINGS_VERSION = 3

        // New setting keys (v3)
        private const val SHAKE_ENABLED_KEY = "shake_controls_enabled"
        private const val SHAKE_BEHAVIOR_KEY = "shake_behavior"
        private const val SHAKE_SENSITIVITY_KEY = "shake_sensitivity"
        private const val SHAKE_HAPTIC_FEEDBACK_KEY = "shake_haptic_feedback"
        private const val SHAKE_CONDITION_KEY = "shake_condition"

        // Skip behavior settings
        private const val SKIP_DELAY_KEY = "shake_skip_delay"
        private const val SKIP_WHEN_PAUSED_KEY = "shake_skip_when_paused"

        // Play/Pause behavior settings
        private const val PLAY_PAUSE_AUTO_RESUME_KEY = "play_pause_auto_resume"

        // Legacy v2 keys (for migration)
        private const val LEGACY_SKIP_WHEN_UNLOCKED_KEY = "shake_skip_when_unlocked"
        private const val LEGACY_PLAY_PAUSE_LOCK_SCREEN_KEY = "play_pause_lock_screen"

        // Auto-Start behavior settings
        private const val AUTO_START_TIMEOUT_KEY = "auto_start_timeout"

        // Legacy setting keys (v1) - for migration
        private const val LEGACY_SHAKE_TO_SKIP_ENABLED_KEY = "shake_to_skip_enabled"
        private const val LEGACY_FEEDBACK_WHEN_SHAKED_KEY = "feedback_when_shaked"
    }


    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        migrateSettingsIfNeeded()
    }

    /**
     * Load shake control settings from SharedPreferences
     */
    fun loadSettings(): ShakeControlSettings {
        return try {
            val enabled = prefs.getBoolean(SHAKE_ENABLED_KEY, false)
            val behaviorId = prefs.getString(SHAKE_BEHAVIOR_KEY, ShakeBehavior.SKIP.id) ?: ShakeBehavior.SKIP.id
            val behavior = ShakeBehavior.entries.find { it.id == behaviorId } ?: ShakeBehavior.SKIP
            val sensitivity = prefs.getFloat(SHAKE_SENSITIVITY_KEY, ShakeDetector.SENSITIVITY_MEDIUM)
            val hapticFeedback = prefs.getBoolean(SHAKE_HAPTIC_FEEDBACK_KEY, true)
            val conditionId = prefs.getString(SHAKE_CONDITION_KEY, ShakeCondition.ALWAYS.id) ?: ShakeCondition.ALWAYS.id
            val shakeCondition = ShakeCondition.entries.find { it.id == conditionId } ?: ShakeCondition.ALWAYS

            val behaviorSettings = when (behavior) {
                ShakeBehavior.SKIP -> loadSkipSettings()
                ShakeBehavior.PLAY_PAUSE -> loadPlayPauseSettings()
                ShakeBehavior.AUTO_START -> loadAutoStartSettings()
            }

            ShakeControlSettings(
                enabled = enabled,
                behavior = behavior,
                sensitivity = sensitivity,
                hapticFeedback = hapticFeedback,
                shakeCondition = shakeCondition,
                behaviorSettings = behaviorSettings
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error loading shake control settings", e)
            ShakeControlSettings() // Return defaults
        }
    }

    /**
     * Save shake control settings to SharedPreferences
     */
    fun saveSettings(settings: ShakeControlSettings) {
        try {
            prefs.edit().apply {
                putBoolean(SHAKE_ENABLED_KEY, settings.enabled)
                putString(SHAKE_BEHAVIOR_KEY, settings.behavior.id)
                putFloat(SHAKE_SENSITIVITY_KEY, settings.sensitivity)
                putBoolean(SHAKE_HAPTIC_FEEDBACK_KEY, settings.hapticFeedback)
                putString(SHAKE_CONDITION_KEY, settings.shakeCondition.id)

                when (settings.behaviorSettings) {
                    is BehaviorSettings.SkipSettings -> saveSkipSettings(settings.behaviorSettings)
                    is BehaviorSettings.PlayPauseSettings -> savePlayPauseSettings(settings.behaviorSettings)
                    is BehaviorSettings.AutoStartSettings -> saveAutoStartSettings(settings.behaviorSettings)
                }

                apply()
            }

            Log.d(LOG_TAG, "Shake control settings saved: behavior=${settings.behavior.id}, enabled=${settings.enabled}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error saving shake control settings", e)
        }
    }

    /**
     * Get legacy settings for backward compatibility
     */
    fun getLegacySettings(): Map<String, Any> {
        val settings = loadSettings()
        val skipSettings = settings.getSkipSettings()

        return mapOf(
            LEGACY_SHAKE_TO_SKIP_ENABLED_KEY to (settings.enabled && settings.behavior == ShakeBehavior.SKIP),
            SHAKE_SENSITIVITY_KEY to settings.sensitivity,
            LEGACY_FEEDBACK_WHEN_SHAKED_KEY to settings.hapticFeedback,
            SKIP_DELAY_KEY to (skipSettings?.skipDelay ?: 3500L),
            SKIP_WHEN_PAUSED_KEY to (skipSettings?.skipWhenPaused ?: false)
        )
    }

    private fun loadSkipSettings(): BehaviorSettings.SkipSettings {
        return BehaviorSettings.SkipSettings(
            skipDelay = prefs.getLong(SKIP_DELAY_KEY, 3500L),
            skipWhenPaused = prefs.getBoolean(SKIP_WHEN_PAUSED_KEY, false)
        )
    }

    private fun loadPlayPauseSettings(): BehaviorSettings.PlayPauseSettings {
        return BehaviorSettings.PlayPauseSettings(
            autoResumeDelay = prefs.getLong(PLAY_PAUSE_AUTO_RESUME_KEY, 0L)
        )
    }

    private fun loadAutoStartSettings(): BehaviorSettings.AutoStartSettings {
        return BehaviorSettings.AutoStartSettings(
            timeout = prefs.getLong(AUTO_START_TIMEOUT_KEY, 5000L)
        )
    }

    private fun SharedPreferences.Editor.saveSkipSettings(settings: BehaviorSettings.SkipSettings) {
        putLong(SKIP_DELAY_KEY, settings.skipDelay)
        putBoolean(SKIP_WHEN_PAUSED_KEY, settings.skipWhenPaused)
    }

    private fun SharedPreferences.Editor.savePlayPauseSettings(settings: BehaviorSettings.PlayPauseSettings) {
        putLong(PLAY_PAUSE_AUTO_RESUME_KEY, settings.autoResumeDelay)
    }

    private fun SharedPreferences.Editor.saveAutoStartSettings(settings: BehaviorSettings.AutoStartSettings) {
        putLong(AUTO_START_TIMEOUT_KEY, settings.timeout)
    }

    /**
     * Migrate settings from old format to new format if needed
     */
    private fun migrateSettingsIfNeeded() {
        val currentVersion = prefs.getInt(SETTINGS_VERSION_KEY, 1)

        if (currentVersion < CURRENT_SETTINGS_VERSION) {
            Log.i(LOG_TAG, "Migrating shake control settings from version $currentVersion to $CURRENT_SETTINGS_VERSION")

            when (currentVersion) {
                1 -> {
                    migrateFromV1ToV2()
                    migrateFromV2ToV3()
                }
                2 -> migrateFromV2ToV3()
            }

            // Update version
            prefs.edit().putInt(SETTINGS_VERSION_KEY, CURRENT_SETTINGS_VERSION).apply()
            Log.i(LOG_TAG, "Shake control settings migration completed")
        }
    }

    /**
     * Migrate from version 1 (legacy simple shake-to-skip) to version 2 (enhanced behaviors)
     */
    private fun migrateFromV1ToV2() {
        try {
            // Check if legacy settings exist
            val legacyEnabled = prefs.getBoolean(LEGACY_SHAKE_TO_SKIP_ENABLED_KEY, false)
            val legacySensitivity = prefs.getFloat(SHAKE_SENSITIVITY_KEY, ShakeDetector.SENSITIVITY_MEDIUM)
            val legacyHapticFeedback = prefs.getBoolean(LEGACY_FEEDBACK_WHEN_SHAKED_KEY, true)
            val legacySkipDelay = prefs.getLong(SKIP_DELAY_KEY, 3500L)
            val legacySkipWhenPaused = prefs.getBoolean(SKIP_WHEN_PAUSED_KEY, false)
            val legacySkipWhenUnlocked = prefs.getBoolean(LEGACY_SKIP_WHEN_UNLOCKED_KEY, false)

            // Create new settings based on legacy values
            val newSettings = ShakeControlSettings(
                enabled = legacyEnabled,
                behavior = ShakeBehavior.SKIP, // Legacy was always skip behavior
                sensitivity = legacySensitivity,
                hapticFeedback = legacyHapticFeedback,
                behaviorSettings = BehaviorSettings.SkipSettings(
                    skipDelay = legacySkipDelay,
                    skipWhenPaused = legacySkipWhenPaused
                )
            )

            // Save new settings
            saveSettings(newSettings)

            Log.i(LOG_TAG, "Migrated legacy shake-to-skip settings: enabled=$legacyEnabled, sensitivity=$legacySensitivity")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error migrating shake control settings from v1 to v2", e)
        }
    }

    /**
     * Migrate from version 2 to version 3 (unified shake condition)
     */
    private fun migrateFromV2ToV3() {
        try {
            // Check if old lock/unlock settings exist
            val skipWhenUnlocked = prefs.getBoolean(LEGACY_SKIP_WHEN_UNLOCKED_KEY, false)
            val playPauseLockScreen = prefs.getBoolean(LEGACY_PLAY_PAUSE_LOCK_SCREEN_KEY, true)

            // Determine the appropriate shake condition based on old settings
            val shakeCondition = when {
                // If skip when unlocked was false, it meant skip only worked when locked
                !skipWhenUnlocked -> ShakeCondition.LOCKED_ONLY
                // If play/pause lock screen was false, it meant play/pause only worked when unlocked
                !playPauseLockScreen -> ShakeCondition.UNLOCKED_ONLY
                // Otherwise, default to always
                else -> ShakeCondition.ALWAYS
            }

            // Save the new shake condition
            prefs.edit().putString(SHAKE_CONDITION_KEY, shakeCondition.id).apply()

            // Remove old keys
            prefs.edit().apply {
                remove(LEGACY_SKIP_WHEN_UNLOCKED_KEY)
                remove(LEGACY_PLAY_PAUSE_LOCK_SCREEN_KEY)
                apply()
            }

            Log.i(LOG_TAG, "Migrated to unified shake condition: ${shakeCondition.getDisplayName}")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error migrating shake control settings from v2 to v3", e)
        }
    }

    /**
     * Reset settings to defaults
     */
    fun resetToDefaults() {
        Log.i(LOG_TAG, "Resetting shake control settings to defaults")
        saveSettings(ShakeControlSettings())
    }

    /**
     * Check if any shake control settings exist
     */
    fun hasSettings(): Boolean {
        return prefs.contains(SHAKE_ENABLED_KEY) || prefs.contains(LEGACY_SHAKE_TO_SKIP_ENABLED_KEY)
    }

    /**
     * Export settings for backup
     */
    fun exportSettings(): Map<String, Any> {
        val settings = loadSettings()
        return mapOf(
            "version" to CURRENT_SETTINGS_VERSION,
            "enabled" to settings.enabled,
            "behavior" to settings.behavior.id,
            "sensitivity" to settings.sensitivity,
            "hapticFeedback" to settings.hapticFeedback,
            "shakeCondition" to settings.shakeCondition.id,
            "behaviorSettings" to when (val behaviorSettings = settings.behaviorSettings) {
                is BehaviorSettings.SkipSettings -> mapOf(
                    "type" to "skip",
                    "skipDelay" to behaviorSettings.skipDelay,
                    "skipWhenPaused" to behaviorSettings.skipWhenPaused
                )
                is BehaviorSettings.PlayPauseSettings -> mapOf(
                    "type" to "play_pause",
                    "autoResumeDelay" to behaviorSettings.autoResumeDelay
                )
                is BehaviorSettings.AutoStartSettings -> mapOf(
                    "type" to "auto_start",
                    "timeout" to behaviorSettings.timeout
                )
            }
        )
    }
}