# Theme Settings UI Components

This document describes the complete UI component system for GlyphBeat's theme settings interface. The system provides an intuitive, accessible way for users to customize theme parameters following Nothing's design language.

## 🎯 Overview

The Theme Settings UI system consists of five main components that work together to provide a seamless customization experience:

1. **ThemeSettingsSheet** - Modal bottom sheet for settings
2. **SettingsControls** - Individual setting input controls  
3. **ThemePreviewCard** - Enhanced theme cards with settings access
4. **ThemeSelectionScreen** - Updated main screen with settings integration
5. **ThemeSettingsViewModel** - State management (optional)

## 🏗️ Component Architecture

### ThemeSettingsSheet
**File:** `ThemeSettingsSheet.kt`

The main modal bottom sheet that provides the settings interface:

- **Header**: Theme name, description, reset and close buttons
- **Content**: Grouped settings by category with smooth loading states
- **Error Handling**: Comprehensive error states with retry functionality
- **Real-time Updates**: Settings save immediately with visual feedback

**Key Features:**
- Smooth animations and transitions
- Automatic grouping by setting categories
- Reset functionality (individual and all settings)
- Loading and error states
- Real-time preview updates

### SettingsControls
**File:** `SettingsControls.kt`

Three specialized input controls for different setting types:

#### SettingsSlider
- Numeric ranges with live value display
- Min/max labels and step-based snapping
- Custom unit formatting (ms, %, etc.)
- Smooth drag interactions

#### SettingsToggle
- Clean on/off states with custom switch design
- Descriptive labels and state indicators
- Accessibility-friendly interactions

#### SettingsDropdown
- Expandable menu with current selection
- Option descriptions and rich labels
- Smooth expand/collapse animations

### Enhanced ThemePreviewCard
**File:** `ThemePreviewCard.kt` (Updated)

The theme cards now include:
- **Settings Button**: Gear icon in top-right corner
- **Custom Settings Indicator**: Blue dot when settings are modified
- **Real-time Updates**: Refresh when settings change

### Updated ThemeSelectionScreen  
**File:** `ThemeSelectionScreen.kt` (Updated)

Main screen enhancements:
- Settings sheet integration
- State management for sheet visibility
- Automatic refresh triggers for setting changes
- Maintains existing theme selection functionality

### ThemeSettingsViewModel
**File:** `ThemeSettingsViewModel.kt`

Optional ViewModel for complex state management:
- Loading, saving, and validation of settings
- Error handling and retry logic
- Real-time state updates
- Settings validation with ThemeSettingsValidator

## 🎨 Design Language

### Nothing Brand Styling
- **Colors**: Dark theme with white text and primary accents
- **Typography**: Custom font (`ntype82regular`) throughout
- **Spacing**: Consistent 8dp/16dp grid system
- **Shapes**: Rounded corners (8dp-12dp) for cards and inputs
- **Elevation**: Subtle shadows for depth

### Visual Hierarchy
- **Primary**: Theme names and current values
- **Secondary**: Descriptions and helper text
- **Accent**: Selected states and settings indicators
- **Surfaces**: Cards and input backgrounds

### Accessibility
- **High Contrast**: Nothing's black/white design ensures readability
- **Touch Targets**: All interactive elements meet 48dp minimum
- **Semantic Labels**: Proper content descriptions for screen readers
- **Focus Indicators**: Clear focus states for keyboard navigation

## 🔧 Integration Guide

### Basic Setup

1. **Import Components**:
```kotlin
import com.pauwma.glyphbeat.ui.settings.*
```

2. **Add Settings Sheet to Theme Screen**:
```kotlin
@Composable
fun YourThemeScreen() {
    var selectedTheme by remember { mutableStateOf<AnimationTheme?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Your theme grid...
    
    selectedTheme?.let { theme ->
        ThemeSettingsSheet(
            theme = theme,
            isVisible = showSettings,
            onDismiss = { showSettings = false },
            onSettingsChanged = { /* refresh UI */ }
        )
    }
}
```

3. **Update Theme Cards**:
```kotlin
ThemePreviewCard(
    theme = theme,
    isSelected = isSelected,
    onSelect = { /* select theme */ },
    onOpenSettings = { 
        selectedTheme = theme
        showSettings = true 
    }
)
```

### Custom Setting Controls

Create custom settings in your themes:

```kotlin
class MyTheme : ThemeTemplate(), ThemeSettingsProvider {
    override fun createSettings(): ThemeSettings {
        return ThemeSettingsBuilder(themeId = "my_theme")
            .addSliderSetting(
                id = "speed",
                displayName = "Animation Speed",
                description = "How fast the animation plays",
                defaultValue = 100,
                minValue = 50,
                maxValue = 500,
                unit = "ms",
                category = "Animation"
            )
            .addToggleSetting(
                id = "effects",
                displayName = "Special Effects",
                description = "Enable particle effects",
                defaultValue = true,
                category = "Visual"
            )
            .build()
    }
}
```

## 📱 User Experience Flow

### Opening Settings
1. User taps settings icon on theme card
2. Bottom sheet slides up with smooth animation
3. Theme info and settings load with progress indicator
4. Settings grouped by category for easy navigation

### Modifying Settings
1. User adjusts slider/toggle/dropdown
2. Value updates immediately with visual feedback
3. Settings auto-save without confirmation needed
4. Theme preview updates in real-time (if visible)
5. Blue dot appears on theme card indicating customization

### Resetting Settings
1. User taps refresh icon in header
2. Confirmation for reset all (individual settings reset immediately)
3. Settings return to defaults with smooth animation
4. Blue indicator dot disappears from theme card

## 🔍 State Management

### Local State (Recommended)
For simple scenarios, use built-in state management in ThemeSettingsSheet:
- Automatic loading and error handling
- Real-time updates and persistence
- Minimal setup required

### ViewModel (Advanced)
For complex scenarios, use ThemeSettingsViewModel:
- Centralized state management
- Advanced error handling and retry logic
- Better testing capabilities
- Lifecycle-aware updates

## 🧪 Testing & Development

### UI Demo
Use `ThemeSettingsUIDemo.kt` to test components in isolation:
```kotlin
@Preview
@Composable 
fun ThemeSettingsUIDemo()
```

### Key Test Scenarios
1. **Loading States**: Slow network/storage
2. **Error Handling**: Invalid settings, storage failures
3. **Edge Cases**: Empty settings, validation errors
4. **Accessibility**: Screen readers, keyboard navigation
5. **Theme Changes**: Real-time updates, state consistency

## 🚀 Performance Considerations

### Optimizations
- **Lazy Loading**: Settings load only when sheet opens
- **Debounced Updates**: Slider changes debounced to reduce saves
- **State Caching**: Settings cached to avoid repeated loads
- **Minimal Recomposition**: Efficient state management prevents unnecessary redraws

### Memory Management
- ViewModels cleared when not needed
- Settings objects properly disposed
- Coroutines cancelled on screen destruction

## 🔮 Future Enhancements

### Planned Features
- **Import/Export**: Save and share custom settings
- **Presets**: Quick-select common configurations  
- **Advanced Preview**: Live animation preview in settings
- **Search/Filter**: Find settings quickly in complex themes
- **Undo/Redo**: Multi-level change history

### Extensibility
- **Custom Controls**: Easy to add new setting types
- **Theme Templates**: Reusable setting groups
- **Validation Rules**: Complex setting interdependencies
- **Localization**: Multi-language support

## 📚 API Reference

### ThemeSettingsSheet
```kotlin
@Composable
fun ThemeSettingsSheet(
    theme: AnimationTheme,           // Theme to configure
    isVisible: Boolean,              // Sheet visibility
    onDismiss: () -> Unit,           // Close callback
    onSettingsChanged: () -> Unit    // Settings change callback
)
```

### Setting Controls
```kotlin
@Composable
fun SettingsSlider(
    setting: SliderSetting,          // Setting configuration
    currentValue: Number,            // Current value
    onValueChange: (Number) -> Unit  // Change callback
)

@Composable
fun SettingsToggle(
    setting: ToggleSetting,
    currentValue: Boolean,
    onValueChange: (Boolean) -> Unit
)

@Composable  
fun SettingsDropdown(
    setting: DropdownSetting,
    currentValue: String,
    onValueChange: (String) -> Unit
)
```

This comprehensive UI system provides users with an intuitive, powerful way to customize their GlyphBeat themes while maintaining Nothing's distinctive design aesthetic.