package com.pauwma.glyphbeat.ui.settings

import android.content.Context
import android.util.Log
import com.pauwma.glyphbeat.data.ThemeRepository

/**
 * Demonstration class showing how to use the theme settings system.
 * This class contains examples and utility methods for testing and demonstrating
 * the theme settings functionality.
 */
class ThemeSettingsDemo(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeSettingsDemo"
    }
    
    private val themeRepository = ThemeRepository.getInstance(context)
    
    /**
     * Demonstrate basic settings operations for a theme.
     */
    fun demonstrateBasicOperations() {
        Log.d(TAG, "=== Basic Theme Settings Demo ===")
        
        // Check which themes support settings
        val settingsThemes = themeRepository.getSettingsCapableThemes()
        Log.d(TAG, "Themes with settings support: ${settingsThemes.size}")
        
        settingsThemes.forEach { theme ->
            if (theme is ThemeSettingsProvider) {
                val themeId = theme.getSettingsId()
                Log.d(TAG, "- ${theme.getThemeName()} (ID: $themeId)")
                
                // Get current settings
                val settings = themeRepository.getThemeSettings(themeId)
                if (settings != null) {
                    Log.d(TAG, "  Settings count: ${settings.settings.size}")
                    Log.d(TAG, "  User customizations: ${settings.userValues.size}")
                    
                    // List all available settings
                    settings.settings.forEach { (settingId, setting) ->
                        Log.d(TAG, "    [$settingId] ${setting.displayName} (${setting::class.simpleName})")
                    }
                }
            }
        }
    }
    
    /**
     * Demonstrate updating settings for a theme.
     */
    fun demonstrateSettingUpdates(themeId: String = "minimal_theme") {
        Log.d(TAG, "=== Settings Update Demo for $themeId ===")
        
        // Check if theme supports settings
        if (!themeRepository.themeSupportsSettings(themeId)) {
            Log.w(TAG, "Theme $themeId does not support settings")
            return
        }
        
        // Get current settings
        val originalSettings = themeRepository.getThemeSettings(themeId)
        if (originalSettings == null) {
            Log.w(TAG, "Could not load settings for theme $themeId")
            return
        }
        
        Log.d(TAG, "Original settings:")
        originalSettings.settings.forEach { (id, setting) ->
            val currentValue = originalSettings.getValue(id)
            Log.d(TAG, "  $id = $currentValue (default: ${setting.defaultValue})")
        }
        
        // Update some settings
        val updates = mapOf(
            "brightness" to 180,
            "show_border" to false,
            "pattern_style" to "dot"
        )
        
        updates.forEach { (settingId, newValue) ->
            if (originalSettings.settings.containsKey(settingId)) {
                val success = themeRepository.updateThemeSetting(themeId, settingId, newValue)
                Log.d(TAG, "Updated $settingId to $newValue: ${if (success) "SUCCESS" else "FAILED"}")
            } else {
                Log.w(TAG, "Setting $settingId not found in theme $themeId")
            }
        }
        
        // Verify updates
        val updatedSettings = themeRepository.getThemeSettings(themeId)
        if (updatedSettings != null) {
            Log.d(TAG, "Updated settings:")
            updates.keys.forEach { settingId ->
                val newValue = updatedSettings.getValue(settingId)
                Log.d(TAG, "  $settingId = $newValue")
            }
        }
    }
    
    /**
     * Demonstrate settings validation.
     */
    fun demonstrateValidation(themeId: String = "minimal_theme") {
        Log.d(TAG, "=== Settings Validation Demo for $themeId ===")
        
        // Validate current settings
        val errors = themeRepository.validateThemeSettings(themeId)
        if (errors.isEmpty()) {
            Log.d(TAG, "All settings are valid ✓")
        } else {
            Log.w(TAG, "Validation errors found:")
            errors.forEach { error ->
                Log.w(TAG, "  - $error")
            }
        }
        
        // Try to set invalid values and see validation in action
        val settings = themeRepository.getThemeSettings(themeId)
        if (settings != null) {
            Log.d(TAG, "Testing validation with invalid values...")
            
            // Test invalid brightness (out of range)
            val brightnessSetting = settings.settings["brightness"] as? SliderSetting
            if (brightnessSetting != null) {
                val invalidValue = 9999
                val isValid = brightnessSetting.isValidValue(invalidValue)
                val coercedValue = brightnessSetting.coerceValue(invalidValue)
                Log.d(TAG, "  Brightness $invalidValue -> valid: $isValid, coerced: $coercedValue")
            }
            
            // Test invalid dropdown value
            val dropdownSetting = settings.settings["pattern_style"] as? DropdownSetting
            if (dropdownSetting != null) {
                val invalidValue = "nonexistent_pattern"
                val isValid = dropdownSetting.isValidValue(invalidValue)
                val coercedValue = dropdownSetting.coerceValue(invalidValue)
                Log.d(TAG, "  Pattern '$invalidValue' -> valid: $isValid, coerced: '$coercedValue'")
            }
        }
    }
    
    /**
     * Demonstrate settings categories and organization.
     */
    fun demonstrateSettingsOrganization(themeId: String = "minimal_theme") {
        Log.d(TAG, "=== Settings Organization Demo for $themeId ===")
        
        val settings = themeRepository.getThemeSettings(themeId)
        if (settings == null) {
            Log.w(TAG, "No settings found for theme $themeId")
            return
        }
        
        // Group settings by category
        val settingsByCategory = settings.settings.values.groupBy { it.category }
        
        Log.d(TAG, "Settings organized by category:")
        settingsByCategory.forEach { (category, settingsInCategory) ->
            Log.d(TAG, "  [$category] (${settingsInCategory.size} settings)")
            settingsInCategory.forEach { setting ->
                val currentValue = settings.getValue(setting.id)
                val hasCustomValue = settings.hasCustomValue(setting.id)
                val customIndicator = if (hasCustomValue) " [CUSTOM]" else ""
                Log.d(TAG, "    - ${setting.displayName}: $currentValue$customIndicator")
                Log.d(TAG, "      ${setting.description}")
            }
        }
    }
    
    /**
     * Demonstrate reset functionality.
     */
    fun demonstrateReset(themeId: String = "minimal_theme") {
        Log.d(TAG, "=== Settings Reset Demo for $themeId ===")
        
        val settingsBefore = themeRepository.getThemeSettings(themeId)
        if (settingsBefore == null) {
            Log.w(TAG, "No settings found for theme $themeId")
            return
        }
        
        Log.d(TAG, "Settings before reset:")
        settingsBefore.userValues.forEach { (id, value) ->
            Log.d(TAG, "  $id = $value")
        }
        
        // Reset a single setting
        val settingToReset = settingsBefore.userValues.keys.firstOrNull()
        if (settingToReset != null) {
            Log.d(TAG, "Resetting setting: $settingToReset")
            val success = themeRepository.resetThemeSetting(themeId, settingToReset)
            Log.d(TAG, "Reset result: ${if (success) "SUCCESS" else "FAILED"}")
            
            // Show the result
            val settingsAfterSingle = themeRepository.getThemeSettings(themeId)
            val newValue = settingsAfterSingle?.getValue(settingToReset)
            val defaultValue = settingsAfterSingle?.settings?.get(settingToReset)?.defaultValue
            Log.d(TAG, "After single reset: $settingToReset = $newValue (default: $defaultValue)")
        }
        
        // Reset all settings
        Log.d(TAG, "Resetting all settings to defaults...")
        val resetAllSuccess = themeRepository.resetAllThemeSettings(themeId)
        Log.d(TAG, "Reset all result: ${if (resetAllSuccess) "SUCCESS" else "FAILED"}")
        
        val settingsAfterReset = themeRepository.getThemeSettings(themeId)
        if (settingsAfterReset != null) {
            Log.d(TAG, "User customizations after reset: ${settingsAfterReset.userValues.size}")
        }
    }
    
    /**
     * Demonstrate export/import functionality.
     */
    fun demonstrateBackupRestore() {
        Log.d(TAG, "=== Backup/Restore Demo ===")
        
        // Export current settings
        val exportData = themeRepository.exportAllSettings()
        if (exportData != null) {
            Log.d(TAG, "Exported settings (${exportData.length} characters)")
            Log.d(TAG, "Export preview: ${exportData.take(200)}...")
            
            // Get storage stats
            val stats = themeRepository.getSettingsStorageStats()
            Log.d(TAG, "Storage stats: $stats")
            
            // In a real app, you would save exportData to a file or send it somewhere
            // For demo purposes, we'll just import it back
            Log.d(TAG, "Re-importing the exported data...")
            val importSuccess = themeRepository.importAllSettings(exportData)
            Log.d(TAG, "Import result: ${if (importSuccess) "SUCCESS" else "FAILED"}")
        } else {
            Log.w(TAG, "Export failed")
        }
    }
    
    /**
     * Run all demonstrations.
     */
    fun runAllDemos() {
        Log.d(TAG, "Starting comprehensive theme settings demonstration...")
        
        demonstrateBasicOperations()
        Thread.sleep(100) // Small delay for cleaner logs
        
        demonstrateSettingUpdates()
        Thread.sleep(100)
        
        demonstrateValidation()
        Thread.sleep(100)
        
        demonstrateSettingsOrganization()
        Thread.sleep(100)
        
        demonstrateReset()
        Thread.sleep(100)
        
        demonstrateBackupRestore()
        
        Log.d(TAG, "Theme settings demonstration completed!")
    }
    
    /**
     * Create sample settings for testing purposes.
     */
    fun createSampleSettings(): ThemeSettings {
        return ThemeSettingsBuilder("demo_theme")
            .addSliderSetting(
                id = "demo_speed",
                displayName = "Demo Speed",
                description = "How fast the demo runs",
                defaultValue = 100L,
                minValue = 10L,
                maxValue = 500L,
                unit = "ms",
                category = "Demo"
            )
            .addToggleSetting(
                id = "demo_enabled",
                displayName = "Enable Demo",
                description = "Turn the demo on or off",
                defaultValue = true,
                category = "Demo"
            )
            .addDropdownSetting(
                id = "demo_mode",
                displayName = "Demo Mode",
                description = "Choose how the demo behaves",
                defaultValue = "normal",
                optionsMap = mapOf(
                    "normal" to "Normal Mode",
                    "debug" to "Debug Mode",
                    "performance" to "Performance Mode"
                ),
                category = "Demo"
            )
            .build()
    }
}