package com.pauwma.glyphbeat.ui.settings

import com.google.gson.annotations.SerializedName

/**
 * Main container for all theme settings.
 * This class holds all customizable settings for a specific theme.
 * 
 * @param themeId Unique identifier for the theme (e.g., "vinyl", "minimal", "pulse")
 * @param version Settings schema version for compatibility
 * @param settings Map of setting ID to setting configuration
 * @param userValues Map of setting ID to user-chosen value
 */
data class ThemeSettings(
    @SerializedName("theme_id")
    val themeId: String,
    
    @SerializedName("version")
    val version: Int = CURRENT_VERSION,
    
    @SerializedName("settings")
    val settings: Map<String, ThemeSetting>,
    
    @SerializedName("user_values")
    val userValues: Map<String, Any> = emptyMap()
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
    
    /**
     * Get the current value for a setting, falling back to default if not set.
     * 
     * @param settingId The ID of the setting to retrieve
     * @return The current value, or default value if not customized
     */
    fun getValue(settingId: String): Any? {
        val setting = settings[settingId] ?: return null
        return userValues[settingId] ?: setting.defaultValue
    }
    
    /**
     * Get a typed value for a setting with proper casting.
     * 
     * @param settingId The ID of the setting to retrieve
     * @param defaultValue Fallback value if setting doesn't exist or cast fails
     * @return The typed value or fallback
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getTypedValue(settingId: String, defaultValue: T): T {
        return try {
            getValue(settingId) as? T ?: defaultValue
        } catch (e: ClassCastException) {
            defaultValue
        }
    }
    
    /**
     * Check if a setting has been customized by the user.
     * 
     * @param settingId The ID of the setting to check
     * @return True if user has set a custom value
     */
    fun hasCustomValue(settingId: String): Boolean {
        return userValues.containsKey(settingId)
    }
    
    /**
     * Create a new ThemeSettings with an updated user value.
     * 
     * @param settingId The ID of the setting to update
     * @param value The new value to set
     * @return New ThemeSettings instance with updated value
     */
    fun withUpdatedValue(settingId: String, value: Any): ThemeSettings {
        val newUserValues = userValues.toMutableMap()
        newUserValues[settingId] = value
        return copy(userValues = newUserValues)
    }
    
    /**
     * Create a new ThemeSettings with a setting reset to default.
     * 
     * @param settingId The ID of the setting to reset
     * @return New ThemeSettings instance with setting reset
     */
    fun withResetValue(settingId: String): ThemeSettings {
        val newUserValues = userValues.toMutableMap()
        newUserValues.remove(settingId)
        return copy(userValues = newUserValues)
    }
    
    /**
     * Create a new ThemeSettings with all settings reset to defaults.
     * 
     * @return New ThemeSettings instance with all settings reset
     */
    fun withAllValuesReset(): ThemeSettings {
        return copy(userValues = emptyMap())
    }
}

/**
 * Base sealed class for different types of theme settings.
 * Each setting type defines its own validation rules and UI presentation hints.
 */
sealed class ThemeSetting {
    abstract val id: String
    abstract val displayName: String
    abstract val description: String
    abstract val defaultValue: Any
    abstract val category: String
    
    /**
     * Validate that a value is acceptable for this setting.
     * 
     * @param value The value to validate
     * @return True if value is valid, false otherwise
     */
    abstract fun isValidValue(value: Any?): Boolean
    
    /**
     * Coerce a value to be within acceptable bounds for this setting.
     * 
     * @param value The value to coerce
     * @return A valid value within bounds
     */
    abstract fun coerceValue(value: Any?): Any
}

/**
 * Numeric setting with slider control.
 * Used for settings like animation speed, brightness, frame count, etc.
 * 
 * @param id Unique identifier for this setting
 * @param displayName Human-readable name shown in UI
 * @param description Detailed explanation of what this setting does
 * @param defaultValue Default numeric value
 * @param minValue Minimum allowed value (inclusive)
 * @param maxValue Maximum allowed value (inclusive)
 * @param stepSize Step increment for slider (e.g., 1 for integers, 0.1 for decimals)
 * @param unit Optional unit label (e.g., "ms", "%", "px")
 * @param category Setting category for grouping (e.g., "Animation", "Visual", "Timing")
 */
data class SliderSetting(
    override val id: String,
    override val displayName: String,
    override val description: String,
    override val defaultValue: Number,
    override val category: String = "General",
    
    @SerializedName("min_value")
    val minValue: Number,
    
    @SerializedName("max_value") 
    val maxValue: Number,
    
    @SerializedName("step_size")
    val stepSize: Number = 1,
    
    @SerializedName("unit")
    val unit: String? = null,
    
    @SerializedName("show_value")
    val showValue: Boolean = true
) : ThemeSetting() {
    
    override fun isValidValue(value: Any?): Boolean {
        return when (value) {
            is Number -> {
                val doubleValue = value.toDouble()
                doubleValue >= minValue.toDouble() && doubleValue <= maxValue.toDouble()
            }
            else -> false
        }
    }
    
    override fun coerceValue(value: Any?): Number {
        return when (value) {
            is Number -> {
                val doubleValue = value.toDouble()
                doubleValue.coerceIn(minValue.toDouble(), maxValue.toDouble())
            }
            else -> defaultValue
        }
    }
    
    /**
     * Get the number of steps between min and max values.
     * Useful for determining slider granularity.
     */
    fun getStepCount(): Int {
        val range = maxValue.toDouble() - minValue.toDouble()
        return (range / stepSize.toDouble()).toInt()
    }
}

/**
 * Boolean setting with toggle/switch control.
 * Used for settings like enable/disable features, visual effects, etc.
 * 
 * @param id Unique identifier for this setting
 * @param displayName Human-readable name shown in UI
 * @param description Detailed explanation of what this setting does
 * @param defaultValue Default boolean value
 * @param category Setting category for grouping
 * @param enabledLabel Optional custom label for "true" state (default: "Enabled")
 * @param disabledLabel Optional custom label for "false" state (default: "Disabled")
 */
data class ToggleSetting(
    override val id: String,
    override val displayName: String,
    override val description: String,
    override val defaultValue: Boolean,
    override val category: String = "General",
    
    @SerializedName("enabled_label")
    val enabledLabel: String = "Enabled",
    
    @SerializedName("disabled_label")
    val disabledLabel: String = "Disabled"
) : ThemeSetting() {
    
    override fun isValidValue(value: Any?): Boolean {
        return value is Boolean
    }
    
    override fun coerceValue(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.lowercase() in listOf("true", "1", "yes", "on")
            is Number -> value.toDouble() != 0.0
            else -> defaultValue
        }
    }
}

/**
 * Multiple choice setting with dropdown control.
 * Used for settings like animation patterns, color schemes, behaviors, etc.
 * 
 * @param id Unique identifier for this setting
 * @param displayName Human-readable name shown in UI
 * @param description Detailed explanation of what this setting does
 * @param defaultValue Default selected option
 * @param options List of available options
 * @param category Setting category for grouping
 */
data class DropdownSetting(
    override val id: String,
    override val displayName: String,
    override val description: String,
    override val defaultValue: String,
    override val category: String = "General",
    
    @SerializedName("options")
    val options: List<DropdownOption>
) : ThemeSetting() {
    
    override fun isValidValue(value: Any?): Boolean {
        return value is String && options.any { it.value == value }
    }
    
    override fun coerceValue(value: Any?): String {
        return when (value) {
            is String -> {
                if (options.any { it.value == value }) value else defaultValue
            }
            else -> defaultValue
        }
    }
    
    /**
     * Get the display label for a given value.
     * 
     * @param value The option value
     * @return The display label, or the value itself if not found
     */
    fun getLabelForValue(value: String): String {
        return options.find { it.value == value }?.label ?: value
    }
}

/**
 * Represents a single option in a dropdown setting.
 * 
 * @param value The internal value used by the theme
 * @param label The display label shown to users
 * @param description Optional detailed description of this option
 */
data class DropdownOption(
    @SerializedName("value")
    val value: String,
    
    @SerializedName("label") 
    val label: String,
    
    @SerializedName("description")
    val description: String? = null
)

/**
 * Validation utilities for theme settings.
 */
object ThemeSettingsValidator {
    
    /**
     * Validate a complete ThemeSettings object.
     * 
     * @param themeSettings The settings to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(themeSettings: ThemeSettings): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate theme ID
        if (themeSettings.themeId.isBlank()) {
            errors.add("Theme ID cannot be blank")
        }
        
        // Validate settings structure
        if (themeSettings.settings.isEmpty()) {
            errors.add("Theme must have at least one setting")
        }
        
        // Validate each setting's user value
        themeSettings.userValues.forEach { (settingId, value) ->
            val setting = themeSettings.settings[settingId]
            if (setting == null) {
                errors.add("Unknown setting ID: $settingId")
            } else if (!setting.isValidValue(value)) {
                errors.add("Invalid value for setting '$settingId': $value")
            }
        }
        
        // Validate setting IDs are unique
        val settingIds = themeSettings.settings.keys
        if (settingIds.size != settingIds.toSet().size) {
            errors.add("Duplicate setting IDs found")
        }
        
        return errors
    }
    
    /**
     * Clean up a ThemeSettings object by removing invalid values and coercing others.
     * 
     * @param themeSettings The settings to clean
     * @return Cleaned ThemeSettings with valid values only
     */
    fun clean(themeSettings: ThemeSettings): ThemeSettings {
        val cleanUserValues = mutableMapOf<String, Any>()
        
        themeSettings.userValues.forEach { (settingId, value) ->
            val setting = themeSettings.settings[settingId]
            if (setting != null) {
                if (setting.isValidValue(value)) {
                    cleanUserValues[settingId] = value
                } else {
                    // Try to coerce the value
                    try {
                        cleanUserValues[settingId] = setting.coerceValue(value)
                    } catch (e: Exception) {
                        // Skip invalid values that can't be coerced
                    }
                }
            }
        }
        
        return themeSettings.copy(userValues = cleanUserValues)
    }
}