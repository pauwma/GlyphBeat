package com.pauwma.glyphbeat.ui.settings

/**
 * Interface for animation themes that support customizable settings.
 * Themes can implement this interface to provide user-configurable options.
 */
interface ThemeSettingsProvider {
    
    /**
     * Get the settings schema for this theme.
     * This defines all available settings, their types, constraints, and default values.
     * 
     * @return ThemeSettings object containing all setting definitions
     */
    fun getSettingsSchema(): ThemeSettings
    
    /**
     * Apply user settings to this theme instance.
     * This method should update the theme's internal state based on user preferences.
     * 
     * @param settings The current theme settings with user values
     */
    fun applySettings(settings: ThemeSettings)
    
    /**
     * Get the unique identifier for this theme's settings.
     * This should match the theme name but be suitable for use as a key.
     * 
     * @return Unique settings ID (e.g., "vinyl_record", "minimal_theme")
     */
    fun getSettingsId(): String {
        // Default implementation uses theme name, converted to lowercase with underscores
        return getThemeName().lowercase().replace(" ", "_").replace(Regex("[^a-z0-9_]"), "")
    }
    
    /**
     * Get the theme name - this should be implemented by the theme class.
     * This is used to generate the default settings ID.
     */
    fun getThemeName(): String
}

/**
 * Builder class to help themes easily create their settings schema.
 * Provides a fluent API for defining theme settings.
 * 
 * Example usage:
 * ```
 * override fun getSettingsSchema(): ThemeSettings {
 *     return ThemeSettingsBuilder(getSettingsId())
 *         .addSliderSetting(
 *             id = "animation_speed",
 *             displayName = "Animation Speed", 
 *             description = "How fast the vinyl spins",
 *             defaultValue = 150L,
 *             minValue = 50L,
 *             maxValue = 1000L,
 *             unit = "ms",
 *             category = "Animation"
 *         )
 *         .addToggleSetting(
 *             id = "show_grooves",
 *             displayName = "Show Grooves",
 *             description = "Display detailed vinyl grooves",
 *             defaultValue = true,
 *             category = "Visual"
 *         )
 *         .build()
 * }
 * ```
 */
class ThemeSettingsBuilder(private val themeId: String) {
    private val settings = mutableMapOf<String, ThemeSetting>()
    
    /**
     * Add a slider setting for numeric values.
     */
    fun addSliderSetting(
        id: String,
        displayName: String,
        description: String,
        defaultValue: Number,
        minValue: Number,
        maxValue: Number,
        stepSize: Number = 1,
        unit: String? = null,
        category: String = "General",
        showValue: Boolean = true
    ): ThemeSettingsBuilder {
        settings[id] = SliderSetting(
            id = id,
            displayName = displayName,
            description = description,
            defaultValue = defaultValue,
            minValue = minValue,
            maxValue = maxValue,
            stepSize = stepSize,
            unit = unit,
            category = category,
            showValue = showValue
        )
        return this
    }
    
    /**
     * Add a toggle setting for boolean values.
     */
    fun addToggleSetting(
        id: String,
        displayName: String,
        description: String,
        defaultValue: Boolean,
        category: String = "General",
        enabledLabel: String = "Enabled",
        disabledLabel: String = "Disabled"
    ): ThemeSettingsBuilder {
        settings[id] = ToggleSetting(
            id = id,
            displayName = displayName,
            description = description,
            defaultValue = defaultValue,
            category = category,
            enabledLabel = enabledLabel,
            disabledLabel = disabledLabel
        )
        return this
    }
    
    /**
     * Add a dropdown setting for multiple choice values.
     */
    fun addDropdownSetting(
        id: String,
        displayName: String,
        description: String,
        defaultValue: String,
        options: List<DropdownOption>,
        category: String = "General"
    ): ThemeSettingsBuilder {
        settings[id] = DropdownSetting(
            id = id,
            displayName = displayName,
            description = description,
            defaultValue = defaultValue,
            options = options,
            category = category
        )
        return this
    }
    
    /**
     * Convenience method to add dropdown options more easily.
     */
    fun addDropdownSetting(
        id: String,
        displayName: String,
        description: String,
        defaultValue: String,
        optionsMap: Map<String, String>, // value -> label
        category: String = "General"
    ): ThemeSettingsBuilder {
        val options = optionsMap.map { (value, label) ->
            DropdownOption(value = value, label = label)
        }
        return addDropdownSetting(id, displayName, description, defaultValue, options, category)
    }
    
    /**
     * Build the final ThemeSettings object.
     */
    fun build(): ThemeSettings {
        return ThemeSettings(
            themeId = themeId,
            settings = settings.toMap(),
            userValues = emptyMap()
        )
    }
}

/**
 * Common setting categories used across themes.
 * These provide consistency in setting organization.
 */
object SettingCategories {
    const val ANIMATION = "Animation"
    const val DESIGN = "Design"
    const val LAYOUT = "Layout"
    const val VISUAL = "Visual"
    const val TIMING = "Timing"
    const val BEHAVIOR = "Behavior"
    const val EFFECTS = "Effects"
    const val AUDIO = "Audio"
}

/**
 * Common setting IDs used across themes.
 * These provide consistency for similar settings across different themes.
 */
object CommonSettingIds {
    const val ANIMATION_SPEED = "animation_speed"
    const val BRIGHTNESS = "brightness" 
    const val FRAME_COUNT = "frame_count"
    const val LOOP_MODE = "loop_mode"
    const val FADE_TRANSITIONS = "fade_transitions"
    const val AUDIO_REACTIVE = "audio_reactive"
    const val COLOR_SCHEME = "color_scheme"
    const val COMPLEXITY = "complexity"
    const val SHOW_DETAILS = "show_details"
    const val PULSE_STRENGTH = "pulse_strength"
    const val ROTATION_SPEED = "rotation_speed"
    const val WAVE_FREQUENCY = "wave_frequency"
    const val PARTICLE_COUNT = "particle_count"
    const val TRAIL_LENGTH = "trail_length"
    const val GLOW_INTENSITY = "glow_intensity"
}

/**
 * Common setting values and options used across themes.
 */
object CommonSettingValues {
    
    object LoopModes {
        const val NORMAL = "normal"
        const val REVERSE = "reverse"
        const val PING_PONG = "ping_pong"
        const val RANDOM = "random"
        
        val OPTIONS = mapOf(
            NORMAL to "Normal",
            REVERSE to "Reverse", 
            PING_PONG to "Ping Pong",
            RANDOM to "Random"
        )
    }
    
    object ColorSchemes {
        const val CLASSIC = "classic"
        const val NEON = "neon"
        const val WARM = "warm"
        const val COOL = "cool"
        const val MONOCHROME = "monochrome"
        
        val OPTIONS = mapOf(
            CLASSIC to "Classic",
            NEON to "Neon",
            WARM to "Warm",
            COOL to "Cool",
            MONOCHROME to "Monochrome"
        )
    }
    
    object Complexity {
        const val SIMPLE = "simple"
        const val MEDIUM = "medium"
        const val COMPLEX = "complex"
        
        val OPTIONS = mapOf(
            SIMPLE to "Simple",
            MEDIUM to "Medium",
            COMPLEX to "Complex"
        )
    }
}

/**
 * Extension functions to make working with theme settings easier.
 */

/**
 * Extension function to get a slider value as Long.
 */
fun ThemeSettings.getSliderValueLong(settingId: String, default: Long = 0L): Long {
    return getTypedValue<Number>(settingId, default).toLong()
}

/**
 * Extension function to get a slider value as Int.
 */
fun ThemeSettings.getSliderValueInt(settingId: String, default: Int = 0): Int {
    return getTypedValue<Number>(settingId, default).toInt()
}

/**
 * Extension function to get a slider value as Float.
 */
fun ThemeSettings.getSliderValueFloat(settingId: String, default: Float = 0f): Float {
    return getTypedValue<Number>(settingId, default).toFloat()
}

/**
 * Extension function to get a toggle value as Boolean.
 */
fun ThemeSettings.getToggleValue(settingId: String, default: Boolean = false): Boolean {
    return getTypedValue(settingId, default)
}

/**
 * Extension function to get a dropdown value as String.
 */
fun ThemeSettings.getDropdownValue(settingId: String, default: String = ""): String {
    return getTypedValue(settingId, default)
}