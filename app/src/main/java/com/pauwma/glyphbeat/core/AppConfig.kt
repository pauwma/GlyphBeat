package com.pauwma.glyphbeat.core

/**
 * Central configuration object for the GlyphBeat application.
 * Contains app-wide constants that can be easily updated in one place.
 */
object AppConfig {
    /**
     * Current application version.
     * Update this value when releasing a new version.
     */
    const val APP_VERSION = "1.2.3_internal_testing"

    /**
     * Version code matching the build.gradle.kts versionCode.
     * Used for internal version tracking.
     */
    const val VERSION_CODE = 13

    /**
     * Enable update dialog for new versions.
     * Set to false to disable update notifications.
     */
    const val ENABLE_UPDATE_DIALOG = true

    /**
     * Minimum version code to show update dialog.
     * Update dialogs won't be shown for versions below this.
     */
    const val MIN_VERSION_FOR_UPDATES = 7
}