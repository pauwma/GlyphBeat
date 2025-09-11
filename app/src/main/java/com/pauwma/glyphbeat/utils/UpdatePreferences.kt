package com.pauwma.glyphbeat.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility object for managing app update preferences and tracking version updates.
 * Ensures update dialogs are shown only once per version.
 */
object UpdatePreferences {

    private const val PREFS_NAME = "update_preferences"
    private const val KEY_LAST_SHOWN_VERSION = "last_shown_version"
    private const val KEY_DIALOG_SHOWN_FOR_VERSION = "dialog_shown_for_version"
    private const val KEY_LAST_LAUNCH_VERSION = "last_launch_version"
    private const val KEY_UPDATE_DISMISSED_TIME = "update_dismissed_time"

    // Test mode flag - set to true to always show dialog
    var testMode = false

    // Force show for testing - ignores all checks
    var forceShow = false

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if the update dialog should be shown for the current version.
     * Returns true if the app version has changed and dialog hasn't been shown yet.
     */
    fun shouldShowUpdateDialog(context: Context, currentVersionCode: Int): Boolean {
        // Force show for testing
        if (forceShow) {
            android.util.Log.d("UpdatePreferences", "Force show enabled - returning true")
            return true
        }

        // In test mode, always show if not already shown for this version
        if (testMode) {
            val prefs = getPreferences(context)
            val lastShownVersion = prefs.getInt(KEY_LAST_SHOWN_VERSION, -1)
            val shouldShow = currentVersionCode > lastShownVersion
            android.util.Log.d("UpdatePreferences", "Test mode: currentVersion=$currentVersionCode, lastShown=$lastShownVersion, shouldShow=$shouldShow")
            return shouldShow
        }

        val prefs = getPreferences(context)
        val lastShownVersion = prefs.getInt(KEY_LAST_SHOWN_VERSION, -1)
        val lastLaunchVersion = prefs.getInt(KEY_LAST_LAUNCH_VERSION, -1)

        // Show dialog if:
        // 1. This is not the first launch (lastLaunchVersion != -1)
        // 2. Version has changed since last launch
        // 3. Dialog hasn't been shown for this version yet
        val shouldShow = lastLaunchVersion != -1 &&
                currentVersionCode > lastLaunchVersion &&
                currentVersionCode > lastShownVersion

        android.util.Log.d("UpdatePreferences",
            "Normal mode: currentVersion=$currentVersionCode, lastLaunch=$lastLaunchVersion, lastShown=$lastShownVersion, shouldShow=$shouldShow")

        return shouldShow
    }

    /**
     * Mark that the update dialog has been shown for the current version.
     */
    fun markUpdateDialogShown(context: Context, versionCode: Int) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putInt(KEY_LAST_SHOWN_VERSION, versionCode)
            putLong(KEY_UPDATE_DISMISSED_TIME, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Update the last launch version to track version changes.
     * Should be called on every app launch.
     */
    fun updateLastLaunchVersion(context: Context, versionCode: Int) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putInt(KEY_LAST_LAUNCH_VERSION, versionCode)
            apply()
        }
    }

    /**
     * Get the last version where the update dialog was shown.
     */
    fun getLastShownVersion(context: Context): Int {
        val prefs = getPreferences(context)
        return prefs.getInt(KEY_LAST_SHOWN_VERSION, -1)
    }

    /**
     * Check if this is the first launch ever (for any version).
     */
    fun isFirstLaunchEver(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getInt(KEY_LAST_LAUNCH_VERSION, -1) == -1
    }

    /**
     * Reset update dialog status (useful for testing).
     */
    fun resetUpdateStatus(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            remove(KEY_LAST_SHOWN_VERSION)
            remove(KEY_UPDATE_DISMISSED_TIME)
            apply()
        }
    }

    /**
     * Force show update dialog on next launch (useful for testing).
     */
    fun forceShowOnNextLaunch(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putInt(KEY_LAST_SHOWN_VERSION, -1)
            apply()
        }
    }
}