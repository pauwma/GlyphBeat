# Update Popup System

## Overview

One-time update dialog shown after app updates (not on first install). Displays new features and changes with emoji icons in Nothing brand aesthetic.

## Components

### UpdatePreferences (`utils/UpdatePreferences.kt`)
Version tracking and display logic.

- Tracks last shown version code
- Controls when dialog appears (only after updates, not first install)
- **Test flags:**
  - `UpdatePreferences.testMode = true` — bypass version checks
  - `UpdatePreferences.forceShow = true` — always show dialog
- Can also test by temporarily increasing `AppConfig.VERSION_CODE` or clearing app data

### UpdateContent (`data/UpdateContent.kt`)
Content management for version changelogs.

- Defines features with emoji icons (not material icons)
- Version-specific changelogs keyed by `versionCode`
- Categories: features, improvements, bug fixes
- `isHighlight` flag → NothingRed color; regular → gray

### UpdateDialog (`ui/dialogs/UpdateDialog.kt`)
UI implementation.

- Animated entrance with scaling effects
- Emoji-based feature icons using NotoEmoji font
- Close button (X icon) in top-right corner
- "Love it!" dismiss button with sparkle animations
- Version badge with material icon
- Fade and scale animations for elements

## Adding New Version Content

1. **Update `UpdateContent.kt`** — add entry to `updateContents` map:
```kotlin
versionCode to UpdateContent(
    versionName = "X.X.X",
    features = listOf(
        UpdateContent.Feature(
            title = "Feature Name",
            description = "Description",
            emoji = "🎨",           // Use appropriate emoji
            isHighlight = true      // true = NothingRed, false = gray
        )
    )
)
```

2. **Update `AppConfig.VERSION_CODE`** to match

3. **Build and release**

## Testing

| Method | How |
|---|---|
| Test mode | `UpdatePreferences.testMode = true` |
| Force show | `UpdatePreferences.forceShow = true` |
| Version bump | Temporarily increase `AppConfig.VERSION_CODE` |
| Full reset | Clear app data to reset shown state |

## Behavior
- Shows only once per version after update
- Won't show on initial app install (smart detection)
- Matches Nothing brand aesthetic
- Uses NotoEmoji font for cross-device emoji consistency
