package com.pauwma.glyphbeat.tutorial.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Utility object for managing tutorial preferences and tracking completion.
 */
object TutorialPreferences {
    
    private const val PREFS_NAME = "tutorial_preferences"
    private const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    private const val KEY_TUTORIAL_VERSION = "tutorial_version"
    private const val KEY_SKIPPED_PERMISSIONS = "skipped_permissions"
    private const val KEY_LAST_SHOWN_DATE = "last_shown_date"
    
    // Current tutorial version - increment this when tutorial changes significantly
    private const val CURRENT_TUTORIAL_VERSION = 1
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if the tutorial has been completed.
     */
    fun isTutorialCompleted(context: Context): Boolean {
        val prefs = getPreferences(context)
        val completed = prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)
        val version = prefs.getInt(KEY_TUTORIAL_VERSION, 0)
        
        // If tutorial version has changed, show tutorial again
        return completed && version == CURRENT_TUTORIAL_VERSION
    }
    
    /**
     * Mark the tutorial as completed.
     */
    fun setTutorialCompleted(context: Context, completed: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putBoolean(KEY_TUTORIAL_COMPLETED, completed)
            putInt(KEY_TUTORIAL_VERSION, CURRENT_TUTORIAL_VERSION)
            putLong(KEY_LAST_SHOWN_DATE, System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Reset tutorial completion status (for replay).
     */
    fun resetTutorial(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putBoolean(KEY_TUTORIAL_COMPLETED, false)
            remove(KEY_SKIPPED_PERMISSIONS)
            apply()
        }
    }
    
    /**
     * Track if user skipped permissions during tutorial.
     */
    fun setSkippedPermissions(context: Context, skipped: Boolean) {
        val prefs = getPreferences(context)
        prefs.edit().apply {
            putBoolean(KEY_SKIPPED_PERMISSIONS, skipped)
            apply()
        }
    }
    
    /**
     * Check if user previously skipped permissions.
     */
    fun hasSkippedPermissions(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.getBoolean(KEY_SKIPPED_PERMISSIONS, false)
    }
    
    /**
     * Get the last date the tutorial was shown.
     */
    fun getLastShownDate(context: Context): Long {
        val prefs = getPreferences(context)
        return prefs.getLong(KEY_LAST_SHOWN_DATE, 0)
    }
    
    /**
     * Check if this is the very first launch of the app.
     */
    fun isFirstLaunch(context: Context): Boolean {
        val prefs = getPreferences(context)
        return !prefs.contains(KEY_TUTORIAL_COMPLETED)
    }
}