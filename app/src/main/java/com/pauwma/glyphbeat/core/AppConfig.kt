package com.pauwma.glyphbeat.core

import android.content.Context
import com.pauwma.glyphbeat.R

/**
 * Central configuration object for the GlyphBeat application.
 * Contains app-wide constants that can be easily updated in one place.
 */
object AppConfig {
    /**
     * Current application version.
     * Update this value when releasing a new version.
     */
    const val APP_VERSION = "1.2.4"

    /**
     * Version code matching the build.gradle.kts versionCode.
     * Used for internal version tracking.
     */
    const val VERSION_CODE = 16

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

    /**
     * Language configuration for the application.
     * Add new languages here to make them available throughout the app.
     */
    object SupportedLanguages {
        /**
         * Data class representing a language configuration
         */
        data class LanguageConfig(
            val code: String,           // Language code like "en", "es", "ja", "nl"
            val nameResId: Int,         // String resource ID for the language name
            val author: String? = null  // Optional translator attribution
        )

        /**
         * List of all supported languages in the application.
         * To add a new language:
         * 1. Add entry here with language code, string resource ID, and optional translator
         * 2. Create values-[code]/strings.xml with translations
         * 3. Update detectSystemLanguage mapping if needed
         */
        val LANGUAGES = listOf(
            LanguageConfig("en", R.string.language_english, null),
            LanguageConfig("es", R.string.language_spanish, null),
            //LanguageConfig("ja", R.string.language_japanese, null),
            LanguageConfig("nl", R.string.language_dutch, "@sjuust")
        )

        /**
         * List of supported language codes for validation
         */
        val LANGUAGE_CODES: List<String> = LANGUAGES.map { it.code }

        /**
         * System language mapping for automatic detection.
         * Maps system locale language codes to app language codes.
         */
        val SYSTEM_LANGUAGE_MAP = mapOf(
            "es" to "es",  // Spanish
            "ja" to "ja",  // Japanese  
            "nl" to "nl"   // Dutch
            // Add new mappings here as needed
        )

        /**
         * Convert language configurations to LanguageInfo objects for UI
         */
        fun getAvailableLanguages(context: Context): List<LanguageInfo> {
            return LANGUAGES.map { config ->
                LanguageInfo(
                    code = config.code,
                    displayName = context.getString(config.nameResId),
                    author = config.author
                )
            }
        }

        /**
         * Check if a language code is supported
         */
        fun isSupported(languageCode: String): Boolean {
            return languageCode in LANGUAGE_CODES
        }

        /**
         * Get the default language code
         */
        fun getDefaultLanguage(): String = "en"

        /**
         * Map system language to supported app language
         */
        fun mapSystemLanguage(systemLanguageCode: String): String {
            return SYSTEM_LANGUAGE_MAP[systemLanguageCode] ?: getDefaultLanguage()
        }
    }
}

/**
 * Data class representing a language option for UI components
 * This is used throughout the app for language selection
 */
data class LanguageInfo(
    val code: String,           // Language code like "en", "es", "ja", "nl"
    val displayName: String,    // Localized display name
    val author: String? = null  // Optional translator attribution
)