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
    const val APP_VERSION = "1.2.0"
    
    /**
     * Version code matching the build.gradle.kts versionCode.
     * Used for internal version tracking.
     */
    const val VERSION_CODE = 7
}