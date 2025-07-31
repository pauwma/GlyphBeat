package com.pauwma.glyphbeat.ui.settings

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pauwma.glyphbeat.animation.AnimationTheme
import com.pauwma.glyphbeat.ui.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing theme settings state and operations.
 * Handles loading, saving, and validation of theme settings.
 */
class ThemeSettingsViewModel(
    context: Context,
    private val theme: AnimationTheme
) : ViewModel() {
    
    private val applicationContext = context.applicationContext
    
    private val themeRepository = ThemeRepository.getInstance(applicationContext)
    
    // UI State
    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()
    
    // Current settings
    private val _themeSettings = MutableStateFlow<ThemeSettings?>(null)
    val themeSettings: StateFlow<ThemeSettings?> = _themeSettings.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load theme settings from repository.
     */
    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                val settings = themeRepository.getThemeSettings(theme.getThemeName())
                withContext(Dispatchers.Main) {
                    _themeSettings.value = settings
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load settings: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Update a specific setting value.
     */
    fun updateSetting(settingId: String, value: Any) {
        val currentSettings = _themeSettings.value ?: return
        
        viewModelScope.launch {
            try {
                val updatedSettings = currentSettings.withUpdatedValue(settingId, value)
                
                // Validate the updated settings
                val validationErrors = ThemeSettingsValidator.validate(updatedSettings)
                if (validationErrors.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Validation failed: ${validationErrors.first()}"
                    )
                    return@launch
                }
                
                // Clean and save
                val cleanSettings = ThemeSettingsValidator.clean(updatedSettings)
                themeRepository.saveThemeSettings(theme.getThemeName(), cleanSettings)
                _themeSettings.value = cleanSettings
                
                _uiState.value = _uiState.value.copy(
                    error = null,
                    hasUnsavedChanges = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to save setting: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset a specific setting to its default value.
     */
    fun resetSetting(settingId: String) {
        val currentSettings = _themeSettings.value ?: return
        
        viewModelScope.launch {
            try {
                val updatedSettings = currentSettings.withResetValue(settingId)
                themeRepository.saveThemeSettings(theme.getThemeName(), updatedSettings)
                _themeSettings.value = updatedSettings
                
                _uiState.value = _uiState.value.copy(
                    error = null,
                    hasUnsavedChanges = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reset setting: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset all settings to their default values.
     */
    fun resetAllSettings() {
        val currentSettings = _themeSettings.value ?: return
        
        viewModelScope.launch {
            try {
                val resetSettings = currentSettings.withAllValuesReset()
                themeRepository.saveThemeSettings(theme.getThemeName(), resetSettings)
                _themeSettings.value = resetSettings
                
                _uiState.value = _uiState.value.copy(
                    error = null,
                    hasUnsavedChanges = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to reset all settings: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Check if the theme has any custom settings applied.
     */
    fun hasCustomSettings(): Boolean {
        return _themeSettings.value?.userValues?.isNotEmpty() == true
    }
    
    /**
     * Get the current value for a specific setting.
     */
    fun getSettingValue(settingId: String): Any? {
        return _themeSettings.value?.getValue(settingId)
    }
    
    /**
     * Check if a setting has been customized.
     */
    fun hasCustomValue(settingId: String): Boolean {
        return _themeSettings.value?.hasCustomValue(settingId) == true
    }
}

/**
 * UI State for the theme settings screen.
 */
data class ThemeSettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false,
    val showResetConfirmation: Boolean = false
)

/**
 * Composable function to create and remember a ThemeSettingsViewModel.
 */
@Composable
fun rememberThemeSettingsViewModel(
    context: Context,
    theme: AnimationTheme
): ThemeSettingsViewModel {
    return remember(theme) {
        ThemeSettingsViewModel(context, theme)
    }
}