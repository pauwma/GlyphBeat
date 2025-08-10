package com.pauwma.glyphbeat.services.trackcontrol

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pauwma.glyphbeat.themes.base.TrackControlTheme
import com.pauwma.glyphbeat.themes.base.TrackControlThemeSettingsProvider
import com.pauwma.glyphbeat.themes.trackcontrol.MinimalArrowTheme
import com.pauwma.glyphbeat.ui.settings.ThemeSettings
import com.pauwma.glyphbeat.ui.settings.ThemeSettingsPersistence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager for track control themes.
 * 
 * Manages shared theme state between NextTrackToyService and PreviousTrackToyService,
 * ensuring both services use the same theme and settings.
 * 
 * Features:
 * - Centralized theme selection and persistence
 * - Shared settings management
 * - Theme change notifications via StateFlow
 * - Thread-safe singleton implementation
 */
class TrackControlThemeManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "TrackControlThemeManager"
        private const val PREFS_NAME = "track_control_theme_prefs"
        private const val KEY_SELECTED_THEME_INDEX = "selected_track_control_theme"
        private const val DEFAULT_THEME_INDEX = 0
        
        @Volatile
        private var INSTANCE: TrackControlThemeManager? = null
        
        /**
         * Gets the singleton instance of TrackControlThemeManager.
         * 
         * @param context Application context
         * @return The singleton instance
         */
        fun getInstance(context: Context): TrackControlThemeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrackControlThemeManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val settingsPersistence: ThemeSettingsPersistence = ThemeSettingsPersistence.getInstance(context)
    
    // Available track control themes
    val availableThemes: List<TrackControlTheme> = listOf(
        MinimalArrowTheme()
        // Future themes can be added here
    )
    
    // Current theme state
    private val _currentThemeFlow = MutableStateFlow(loadSelectedTheme())
    val currentThemeFlow: StateFlow<TrackControlTheme> = _currentThemeFlow.asStateFlow()
    
    // Theme index state for UI
    private val _selectedThemeIndexFlow = MutableStateFlow(loadSelectedThemeIndex())
    val selectedThemeIndexFlow: StateFlow<Int> = _selectedThemeIndexFlow.asStateFlow()
    
    // Settings change notifications
    private val _settingsChangedFlow = MutableStateFlow<Pair<String, ThemeSettings>?>(null)
    val settingsChangedFlow: StateFlow<Pair<String, ThemeSettings>?> = _settingsChangedFlow.asStateFlow()
    
    /**
     * Gets the current selected theme.
     */
    val currentTheme: TrackControlTheme
        get() = _currentThemeFlow.value
    
    /**
     * Gets the current selected theme index.
     */
    val selectedThemeIndex: Int
        get() = _selectedThemeIndexFlow.value
    
    /**
     * Selects a theme by index.
     * 
     * @param index The index of the theme in availableThemes
     */
    fun selectTheme(index: Int) {
        if (index !in availableThemes.indices) {
            Log.w(TAG, "Invalid theme index: $index")
            return
        }
        
        Log.d(TAG, "Selecting theme at index $index: ${availableThemes[index].getThemeName()}")
        
        // Update theme
        _selectedThemeIndexFlow.value = index
        _currentThemeFlow.value = availableThemes[index]
        
        // Save selection
        saveSelectedThemeIndex(index)
        
        // Apply saved settings to the new theme
        applySettingsToCurrentTheme()
    }
    
    /**
     * Selects a theme by name.
     * 
     * @param themeName The name of the theme to select
     */
    fun selectTheme(themeName: String) {
        val index = availableThemes.indexOfFirst { it.getThemeName() == themeName }
        if (index != -1) {
            selectTheme(index)
        } else {
            Log.w(TAG, "Theme not found: $themeName")
        }
    }
    
    /**
     * Gets theme settings for the current theme if it supports settings.
     * 
     * @return ThemeSettings or null if theme doesn't support settings
     */
    fun getCurrentThemeSettings(): ThemeSettings? {
        val theme = currentTheme
        return if (theme is TrackControlThemeSettingsProvider) {
            val themeId = theme.getSettingsId()
            val schema = theme.getSettingsSchema()
            // Load saved settings with schema as fallback for new/unsaved themes
            settingsPersistence.loadThemeSettings(themeId, schema)
        } else {
            null
        }
    }
    
    /**
     * Updates settings for the current theme.
     * 
     * @param settings The updated theme settings
     */
    fun updateCurrentThemeSettings(settings: ThemeSettings) {
        val theme = currentTheme
        if (theme is TrackControlThemeSettingsProvider) {
            // Save settings
            settingsPersistence.saveThemeSettings(settings)
            
            // Apply to current theme
            theme.applySettings(settings)
            
            // Notify observers
            _settingsChangedFlow.value = Pair(settings.themeId, settings)
            
            Log.d(TAG, "Updated settings for theme: ${settings.themeId}")
        }
    }
    
    /**
     * Cycles to the next available theme.
     * Useful for quick theme switching via user interaction.
     */
    fun cycleToNextTheme() {
        val nextIndex = (selectedThemeIndex + 1) % availableThemes.size
        selectTheme(nextIndex)
    }
    
    /**
     * Gets a preview frame for the specified theme and direction.
     * 
     * @param themeIndex Index of the theme
     * @param direction Track control direction
     * @return Preview frame data
     */
    fun getThemePreview(themeIndex: Int, direction: TrackControlTheme.Direction): IntArray {
        return if (themeIndex in availableThemes.indices) {
            availableThemes[themeIndex].getPreviewFrame(direction)
        } else {
            IntArray(625) { 0 }
        }
    }
    
    /**
     * Initializes the manager and applies settings to the current theme.
     * Should be called when services start.
     */
    fun initialize() {
        Log.d(TAG, "Initializing TrackControlThemeManager")
        applySettingsToCurrentTheme()
    }
    
    // Private helper methods
    
    private fun loadSelectedThemeIndex(): Int {
        return prefs.getInt(KEY_SELECTED_THEME_INDEX, DEFAULT_THEME_INDEX)
            .coerceIn(availableThemes.indices)
    }
    
    private fun loadSelectedTheme(): TrackControlTheme {
        val index = loadSelectedThemeIndex()
        return availableThemes[index]
    }
    
    private fun saveSelectedThemeIndex(index: Int) {
        prefs.edit().putInt(KEY_SELECTED_THEME_INDEX, index).apply()
    }
    
    private fun applySettingsToCurrentTheme() {
        val theme = currentTheme
        if (theme is TrackControlThemeSettingsProvider) {
            val themeId = theme.getSettingsId()
            val schema = theme.getSettingsSchema()
            // Load saved settings with schema as fallback for new/unsaved themes
            val settings = settingsPersistence.loadThemeSettings(themeId, schema)
            
            if (settings != null) {
                theme.applySettings(settings)
                Log.d(TAG, "Applied settings to theme: $themeId")
            } else {
                Log.d(TAG, "No settings available for theme: $themeId")
            }
        }
    }
}