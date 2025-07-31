# Theme Settings System

This document explains the comprehensive theme settings system for GlyphBeat, which allows themes to provide user-customizable options.

## Overview

The theme settings system consists of several key components:
- **Data Models**: Type-safe classes for different setting types
- **Persistence Layer**: JSON-based storage with SharedPreferences
- **Theme Integration**: Interface for themes to declare and use settings
- **Repository**: Centralized management of settings and themes

## Architecture

```
ThemeRepository
├── ThemeSettingsPersistence (JSON + SharedPreferences)
└── Themes implementing ThemeSettingsProvider
    ├── getSettingsSchema() → ThemeSettings
    └── applySettings(ThemeSettings)
```

## Core Data Classes

### ThemeSettings
Main container for all theme settings:
```kotlin
data class ThemeSettings(
    val themeId: String,           // Unique theme identifier
    val version: Int,              // Schema version for compatibility
    val settings: Map<String, ThemeSetting>, // Setting definitions
    val userValues: Map<String, Any>         // User-customized values
)
```

### Setting Types

#### SliderSetting (Numeric)
For values like animation speed, brightness, frame count:
```kotlin
SliderSetting(
    id = "animation_speed",
    displayName = "Animation Speed",
    description = "How fast the animation plays",
    defaultValue = 150L,
    minValue = 50L,
    maxValue = 1000L,
    stepSize = 10L,
    unit = "ms",
    category = "Animation"
)
```

#### ToggleSetting (Boolean)
For enable/disable features:
```kotlin
ToggleSetting(
    id = "show_details",
    displayName = "Show Details",
    description = "Display additional visual details",
    defaultValue = true,
    category = "Visual",
    enabledLabel = "Visible",
    disabledLabel = "Hidden"
)
```

#### DropdownSetting (Multiple Choice)
For selecting from predefined options:
```kotlin
DropdownSetting(
    id = "color_scheme",
    displayName = "Color Scheme",
    description = "Choose the visual color theme",
    defaultValue = "classic",
    options = listOf(
        DropdownOption("classic", "Classic"),
        DropdownOption("neon", "Neon"),
        DropdownOption("warm", "Warm")
    ),
    category = "Colors"
)
```

## Theme Integration

### 1. Implement ThemeSettingsProvider

Make your theme class implement the `ThemeSettingsProvider` interface:

```kotlin
class MyCustomTheme : AnimationTheme(), ThemeSettingsProvider {
    
    // Theme runtime properties that can be modified by settings
    private var currentSpeed: Long = 150L
    private var currentBrightness: Int = 255
    private var enableEffect: Boolean = true  
    
    override fun getSettingsSchema(): ThemeSettings {
        return ThemeSettingsBuilder(getSettingsId())
            .addSliderSetting(
                id = CommonSettingIds.ANIMATION_SPEED,
                displayName = "Animation Speed",
                description = "Control how fast the animation plays",
                defaultValue = 150L,
                minValue = 50L,
                maxValue = 1000L,
                unit = "ms",
                category = SettingCategories.ANIMATION
            )
            .addSliderSetting(
                id = CommonSettingIds.BRIGHTNESS,
                displayName = "Brightness",
                description = "Adjust the overall brightness",
                defaultValue = 255,
                minValue = 50,
                maxValue = 255,
                category = SettingCategories.VISUAL
            )
            .addToggleSetting(
                id = "special_effect",
                displayName = "Special Effect",
                description = "Enable additional visual effects",
                defaultValue = true,
                category = SettingCategories.EFFECTS
            )
            .build()
    }
    
    override fun applySettings(settings: ThemeSettings) {
        // Extract and apply user values
        currentSpeed = settings.getSliderValueLong(CommonSettingIds.ANIMATION_SPEED, 150L)
        currentBrightness = settings.getSliderValueInt(CommonSettingIds.BRIGHTNESS, 255)
        enableEffect = settings.getToggleValue("special_effect", true)
        
        // Regenerate frames or update theme behavior based on new settings
        updateThemeBasedOnSettings()
    }
    
    override fun getSettingsId(): String = "my_custom_theme"
    
    // Use settings in theme methods
    override fun getAnimationSpeed(): Long = currentSpeed
    override fun getBrightness(): Int = currentBrightness
    
    private fun updateThemeBasedOnSettings() {
        // Update theme behavior based on current settings
        // e.g., regenerate frames, update effects, etc.
    }
}
```

### 2. Common Setting Categories & IDs

Use predefined constants for consistency:

```kotlin
// Categories
SettingCategories.ANIMATION  // Animation timing and behavior
SettingCategories.VISUAL     // Visual appearance 
SettingCategories.TIMING     // Frame timing and durations
SettingCategories.BEHAVIOR   // Theme behavior modes
SettingCategories.EFFECTS    // Special effects and enhancements
SettingCategories.COLORS     // Color schemes and palettes
SettingCategories.LAYOUT     // Pattern layout and positioning
SettingCategories.AUDIO      // Audio-reactive features
SettingCategories.ADVANCED   // Advanced/technical settings

// Common Setting IDs
CommonSettingIds.ANIMATION_SPEED
CommonSettingIds.BRIGHTNESS
CommonSettingIds.FRAME_COUNT
CommonSettingIds.LOOP_MODE
CommonSettingIds.FADE_TRANSITIONS
CommonSettingIds.AUDIO_REACTIVE
CommonSettingIds.COLOR_SCHEME
// ... and more
```

## Using the ThemeRepository

### Basic Operations

```kotlin
val repository = ThemeRepository.getInstance(context)

// Get settings for a theme
val settings = repository.getThemeSettings("my_theme")

// Update a single setting
repository.updateThemeSetting("my_theme", "brightness", 200)

// Save complete settings
repository.saveThemeSettings("my_theme", updatedSettings)

// Reset to defaults
repository.resetAllThemeSettings("my_theme")

// Check if theme supports settings
val hasSettings = repository.themeSupportsSettings("my_theme")
```

### Advanced Operations

```kotlin
// Export/Import for backup
val exportJson = repository.exportAllSettings()
repository.importAllSettings(exportJson)

// Get statistics
val stats = repository.getSettingsStorageStats()

// Validate settings
val errors = repository.validateThemeSettings("my_theme")

// Get themes with settings capability
val settingsThemes = repository.getSettingsCapableThemes()
```

## Persistence Details

### Storage Format
Settings are stored as JSON in SharedPreferences:
```json
{
  "theme_id": "vinyl_theme",
  "version": 1,
  "settings": {
    "animation_speed": {
      "id": "animation_speed",
      "displayName": "Animation Speed",
      "defaultValue": 150,
      "minValue": 50,
      "maxValue": 1000,
      "unit": "ms"
    }
  },
  "user_values": {
    "animation_speed": 200
  }
}
```

### Error Handling
- Invalid JSON data is ignored with fallback to defaults
- Type mismatches are automatically coerced when possible
- Unknown settings are silently removed during validation
- Versioning supports future migration scenarios

### Performance
- Settings are loaded lazily and cached
- JSON serialization uses Gson for reliability
- Validation is performed on load and save
- Settings are applied immediately to active themes

## Best Practices

### Theme Design
1. **Start Simple**: Begin with 2-3 essential settings
2. **Logical Grouping**: Use appropriate categories
3. **Meaningful Defaults**: Choose sensible default values
4. **Clear Descriptions**: Write helpful setting descriptions
5. **Validation**: Always validate user input in `applySettings()`

### Setting Types
- Use **SliderSetting** for: speed, brightness, counts, sizes
- Use **ToggleSetting** for: feature enable/disable, boolean options
- Use **DropdownSetting** for: modes, schemes, predefined choices

### Runtime Updates
```kotlin
override fun applySettings(settings: ThemeSettings) {
    // Extract settings
    val newSpeed = settings.getSliderValueLong("speed", defaultSpeed)
    
    // Validate if needed
    val validSpeed = newSpeed.coerceIn(50L, 1000L)
    
    // Apply atomically to avoid partial state
    synchronized(this) {
        currentSpeed = validSpeed
        updateInternalState()
    }
}
```

### Error Handling
```kotlin
override fun applySettings(settings: ThemeSettings) {
    try {
        // Apply settings with fallbacks
        brightness = settings.getSliderValueInt("brightness", defaultBrightness)
        enabled = settings.getToggleValue("enabled", true)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to apply some settings, using defaults", e)
        // Continue with default values
    }
}
```

## Extension Points

### Custom Setting Types
Extend `ThemeSetting` for specialized setting types:
```kotlin
data class ColorSetting(
    override val id: String,
    override val displayName: String,
    override val description: String,
    override val defaultValue: Int, // Color as ARGB
    override val category: String = "Colors"
) : ThemeSetting() {
    
    override fun isValidValue(value: Any?): Boolean {
        return value is Number // Validate color format
    }
    
    override fun coerceValue(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            else -> defaultValue
        }
    }
}
```

### Theme Settings UI
The settings system provides data structures ready for UI implementation:
- Setting categories for grouping
- Display names and descriptions for labels
- Value constraints for validation
- Current vs default values for reset functionality

## Migration Support

When updating setting schemas:
1. Increment `ThemeSettings.CURRENT_VERSION`
2. Implement migration logic in `ThemeSettingsPersistence.migrateSettingsIfNeeded()`
3. Handle missing settings gracefully with defaults
4. Test with existing saved settings

## Debugging

Enable logging to track settings operations:
```kotlin
// Settings are logged with tag "ThemeSettingsPersistence" and "ThemeRepository"
// Check logcat for settings load/save operations and validation errors
```

Common issues:
- **Settings not persisting**: Check theme implements `ThemeSettingsProvider`
- **Settings not applying**: Verify `applySettings()` is implemented correctly
- **Type errors**: Use typed getters like `getSliderValueInt()`
- **Validation failures**: Check setting constraints and default values

This settings system provides a robust foundation for theme customization while maintaining type safety and data integrity.