package com.pauwma.glyphbeat.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.State
import com.pauwma.glyphbeat.themes.base.AnimationTheme
import com.pauwma.glyphbeat.themes.animation.VinylTheme
import com.pauwma.glyphbeat.themes.animation.DancingDuckTheme
import com.pauwma.glyphbeat.themes.animation.ShapeTheme
import com.pauwma.glyphbeat.themes.animation.CoverArtTheme
import com.pauwma.glyphbeat.themes.animation.GlyphyTheme
import com.pauwma.glyphbeat.themes.animation.MinimalTheme
import com.pauwma.glyphbeat.themes.animation.PulseVisualizerTheme
import com.pauwma.glyphbeat.themes.animation.WaveformTheme
import com.pauwma.glyphbeat.themes.animation.ScrollTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsPersistence
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Repository for managing animation themes and their selection state.
 * Handles persistence and provides theme data to UI components.
 */
class ThemeRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ThemeRepository"
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_SELECTED_THEME_INDEX = "selected_theme_index"
        private const val DEFAULT_THEME_INDEX = 0

        @Volatile
        private var INSTANCE: ThemeRepository? = null

        fun getInstance(context: Context): ThemeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Clears the singleton instance to force recreation with new context.
         * This is useful when locale changes and theme objects need fresh string resources.
         */
        fun refreshForLocaleChange(context: Context): ThemeRepository {
            synchronized(this) {
                INSTANCE = null  // Clear the cached instance
                return getInstance(context)  // Create new instance with current context
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settingsPersistence: ThemeSettingsPersistence = ThemeSettingsPersistence.getInstance(context)

    // Settings change notifications with replay for late subscribers
    private val _settingsChangedFlow = MutableSharedFlow<Pair<String, ThemeSettings>>(
        replay = 1,  // Keep last emission for late subscribers
        extraBufferCapacity = 1  // Allow buffering of emissions
    )
    val settingsChangedFlow: SharedFlow<Pair<String, ThemeSettings>> = _settingsChangedFlow.asSharedFlow()

    // Available themes list
    val availableThemes: List<AnimationTheme> = listOf<AnimationTheme>(
        VinylTheme(context),
        DancingDuckTheme(context),
        CoverArtTheme(context),
        ScrollTheme(context),
        MinimalTheme(context),
        GlyphyTheme(context),
        ShapeTheme(context),
        PulseVisualizerTheme(),
        WaveformTheme()
    )

    // Current selected theme index state
    private val _selectedThemeIndex = mutableIntStateOf(
        prefs.getInt(KEY_SELECTED_THEME_INDEX, DEFAULT_THEME_INDEX)
    )
    val selectedThemeIndex: State<Int> = _selectedThemeIndex

    // Current selected theme
    val selectedTheme: AnimationTheme
        get() {
            // Refresh from SharedPreferences to ensure we have the latest value
            val currentPrefsValue = prefs.getInt(KEY_SELECTED_THEME_INDEX, DEFAULT_THEME_INDEX)
            if (_selectedThemeIndex.value != currentPrefsValue) {
                _selectedThemeIndex.value = currentPrefsValue
                Log.d(TAG, "Theme index refreshed from preferences: $currentPrefsValue")
            }
            return availableThemes[_selectedThemeIndex.value]
        }

    /**
     * Select a theme by index
     */
    fun selectTheme(index: Int) {
        if (index in availableThemes.indices) {
            _selectedThemeIndex.value = index
            saveSelectedThemeIndex(index)
        }
    }

    /**
     * Select a theme by theme object
     */
    fun selectTheme(theme: AnimationTheme) {
        val index = availableThemes.indexOfFirst {
            it.getThemeName() == theme.getThemeName()
        }
        if (index != -1) {
            selectTheme(index)
        }
    }

    /**
     * Get theme by index
     */
    fun getTheme(index: Int): AnimationTheme? {
        return if (index in availableThemes.indices) {
            availableThemes[index]
        } else {
            null
        }
    }

    /**
     * Check if a theme is currently selected
     */
    fun isThemeSelected(theme: AnimationTheme): Boolean {
        return selectedTheme.getThemeName() == theme.getThemeName()
    }

    /**
     * Check if a theme index is currently selected
     */
    fun isThemeSelected(index: Int): Boolean {
        return _selectedThemeIndex.value == index
    }

    /**
     * Get the total number of available themes
     */
    fun getThemeCount(): Int = availableThemes.size

    /**
     * Save the selected theme index to SharedPreferences
     */
    private fun saveSelectedThemeIndex(index: Int) {
        prefs.edit()
            .putInt(KEY_SELECTED_THEME_INDEX, index)
            .apply()
    }

    /**
     * Get theme descriptions for UI display
     */
    fun getThemeInfo(): List<Pair<String, String>> {
        return availableThemes.map { theme ->
            theme.getThemeName() to theme.getDescription()
        }
    }

    /**
     * Clear all corrupted theme settings.
     * This can be called on app startup to ensure clean state.
     */
    fun clearCorruptedSettings() {
        try {
            Log.d(TAG, "Clearing all corrupted theme settings...")
            settingsPersistence.resetAllSettings()
            Log.d(TAG, "All theme settings cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear corrupted settings", e)
        }
    }

    // =================================================================================
    // THEME SETTINGS MANAGEMENT
    // =================================================================================

    /**
     * Get settings for a specific theme.
     * If the theme implements ThemeSettingsProvider, uses its schema.
     * Otherwise returns null.
     *
     * @param themeId The unique identifier for the theme
     * @return ThemeSettings with current user values, or null if theme doesn't support settings
     */
    fun getThemeSettings(themeId: String): ThemeSettings? {
        return try {
            // Find the theme by ID
            val theme = findThemeById(themeId)
            if (theme is ThemeSettingsProvider) {
                // Get the schema from the theme
                val schema = theme.getSettingsSchema()

                // Load saved user values and merge with schema
                val savedSettings = settingsPersistence.loadThemeSettings(themeId, schema)
                savedSettings ?: schema
            } else {
                Log.d(TAG, "Theme $themeId does not support settings")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get settings for theme: $themeId", e)
            null
        }
    }

    /**
     * Save settings for a specific theme.
     *
     * @param themeId The unique identifier for the theme
     * @param settings The settings to save
     * @return True if saved successfully
     */
    fun saveThemeSettings(themeId: String, settings: ThemeSettings): Boolean {
        return try {
            val success = settingsPersistence.saveThemeSettings(settings)
            if (success) {
                // Apply settings to the theme if it's currently loaded
                val theme = findThemeById(themeId)
                if (theme is ThemeSettingsProvider) {
                    theme.applySettings(settings)
                    Log.d(TAG, "Applied settings to theme: $themeId")
                }

                // Emit settings change notification for real-time updates
                try {
                    _settingsChangedFlow.tryEmit(themeId to settings)
                    Log.d(TAG, "Emitted settings change notification for theme: $themeId")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to emit settings change notification: ${e.message}")
                }
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings for theme: $themeId", e)
            false
        }
    }

    /**
     * Update a single setting value for a theme.
     *
     * @param themeId The unique identifier for the theme
     * @param settingId The ID of the setting to update
     * @param value The new value
     * @return True if updated successfully
     */
    fun updateThemeSetting(themeId: String, settingId: String, value: Any): Boolean {
        return try {
            val currentSettings = getThemeSettings(themeId) ?: return false
            val updatedSettings = currentSettings.withUpdatedValue(settingId, value)
            saveThemeSettings(themeId, updatedSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update setting $settingId for theme: $themeId", e)
            false
        }
    }

    /**
     * Reset a single setting to its default value.
     *
     * @param themeId The unique identifier for the theme
     * @param settingId The ID of the setting to reset
     * @return True if reset successfully
     */
    fun resetThemeSetting(themeId: String, settingId: String): Boolean {
        return try {
            val currentSettings = getThemeSettings(themeId) ?: return false
            val resetSettings = currentSettings.withResetValue(settingId)
            saveThemeSettings(themeId, resetSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset setting $settingId for theme: $themeId", e)
            false
        }
    }

    /**
     * Reset all settings for a theme to their default values.
     *
     * @param themeId The unique identifier for the theme
     * @return True if reset successfully
     */
    fun resetAllThemeSettings(themeId: String): Boolean {
        return try {
            val currentSettings = getThemeSettings(themeId) ?: return false
            val resetSettings = currentSettings.withAllValuesReset()
            saveThemeSettings(themeId, resetSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset all settings for theme: $themeId", e)
            false
        }
    }

    /**
     * Check if a theme supports customizable settings.
     *
     * @param themeId The unique identifier for the theme
     * @return True if theme supports settings
     */
    fun themeSupportsSettings(themeId: String): Boolean {
        return findThemeById(themeId) is ThemeSettingsProvider
    }

    /**
     * Get all themes that support customizable settings.
     *
     * @return List of themes that implement ThemeSettingsProvider
     */
    fun getSettingsCapableThemes(): List<AnimationTheme> {
        return availableThemes.filter { it is ThemeSettingsProvider }
    }

    /**
     * Get a list of all theme IDs that have saved settings.
     *
     * @return List of theme IDs with saved settings
     */
    fun getThemesWithSavedSettings(): List<String> {
        return settingsPersistence.getSavedThemeIds()
    }

    /**
     * Delete all saved settings for a theme.
     * This will cause the theme to revert to default settings.
     *
     * @param themeId The unique identifier for the theme
     * @return True if deleted successfully
     */
    fun deleteThemeSettings(themeId: String): Boolean {
        return settingsPersistence.deleteThemeSettings(themeId)
    }

    /**
     * Export all theme settings to a JSON string for backup.
     *
     * @return JSON string containing all settings, or null if export fails
     */
    fun exportAllSettings(): String? {
        return settingsPersistence.exportAllSettings()
    }

    /**
     * Import theme settings from a JSON string.
     * This will overwrite existing settings for matching themes.
     *
     * @param jsonData JSON string containing theme settings
     * @return True if import was successful
     */
    fun importAllSettings(jsonData: String): Boolean {
        val success = settingsPersistence.importAllSettings(jsonData)
        if (success) {
            // Apply imported settings to currently loaded themes
            availableThemes.forEach { theme ->
                if (theme is ThemeSettingsProvider) {
                    val settings = getThemeSettings(theme.getSettingsId())
                    if (settings != null) {
                        theme.applySettings(settings)
                    }
                }
            }
        }
        return success
    }

    /**
     * Get storage statistics for theme settings.
     *
     * @return Map containing storage usage information
     */
    fun getSettingsStorageStats(): Map<String, Any> {
        return settingsPersistence.getStorageStats()
    }

    /**
     * Validate theme settings and return any errors found.
     *
     * @param themeId The unique identifier for the theme
     * @return List of validation errors (empty if valid)
     */
    fun validateThemeSettings(themeId: String): List<String> {
        return try {
            val settings = getThemeSettings(themeId)
            if (settings != null) {
                com.pauwma.glyphbeat.ui.settings.ThemeSettingsValidator.validate(settings)
            } else {
                listOf("Theme does not support settings or was not found")
            }
        } catch (e: Exception) {
            listOf("Validation failed: ${e.message}")
        }
    }

    // =================================================================================
    // HELPER METHODS
    // =================================================================================

    /**
     * Find a theme by its settings ID.
     * First tries to find by ThemeSettingsProvider.getSettingsId(),
     * then falls back to theme name matching.
     *
     * @param themeId The theme ID to search for
     * @return The matching theme, or null if not found
     */
    private fun findThemeById(themeId: String): AnimationTheme? {
        // First try to find by settings ID for themes that implement ThemeSettingsProvider
        availableThemes.forEach { theme ->
            if (theme is ThemeSettingsProvider && theme.getSettingsId() == themeId) {
                return theme
            }
        }

        // Fallback: try to find by theme name (converted to ID format)
        availableThemes.forEach { theme ->
            val generatedId = theme.getThemeName().lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
            if (generatedId == themeId) {
                return theme
            }
        }

        return null
    }
}