package com.pauwma.glyphbeat.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.pauwma.glyphbeat.AnimationTheme
import com.pauwma.glyphbeat.animation.styles.VinylTheme
import com.pauwma.glyphbeat.animation.styles.DancingDuckTheme
import com.pauwma.glyphbeat.animation.styles.PulseTheme
import com.pauwma.glyphbeat.animation.styles.ThemeTemplate
import com.pauwma.glyphbeat.animation.styles.WaveTheme

/**
 * Repository for managing animation themes and their selection state.
 * Handles persistence and provides theme data to UI components.
 */
class ThemeRepository private constructor(private val context: Context) {
    
    companion object {
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
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Available themes list
    val availableThemes = listOf(
        VinylTheme(),
        DancingDuckTheme()
    )
    
    // Current selected theme index state
    private val _selectedThemeIndex = mutableStateOf(
        prefs.getInt(KEY_SELECTED_THEME_INDEX, DEFAULT_THEME_INDEX)
    )
    val selectedThemeIndex: State<Int> = _selectedThemeIndex
    
    // Current selected theme
    val selectedTheme: AnimationTheme
        get() = availableThemes[_selectedThemeIndex.value]
    
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
}

/**
 * Extension function to get short description for theme cards
 */
fun AnimationTheme.getShortDescription(): String {
    return when (this.getThemeName()) {
        "Vinyl" -> "Detailed vinyl record"
        "Dancing Duck" -> "Classic duck animation"
        else -> "Animation theme"
    }
}