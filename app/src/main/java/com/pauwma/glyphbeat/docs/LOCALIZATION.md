# Localization (i18n)

## Supported Languages

| Language | Code | Resource | Notes |
|---|---|---|---|
| English | `en` | `values/strings.xml` | Default fallback |
| Spanish | `es` | `values-es/strings.xml` | Full translation |
| Japanese | `ja` | `values-ja/strings.xml` | Full translation |

## Automatic System Language Detection

On first install, `initializeAppLanguage()` in `MainActivity.onCreate()` detects the system locale and applies the matching language. Only runs when no existing language preference exists.

```kotlin
fun detectSystemLanguage(): String {
    val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Resources.getSystem().configuration.locales[0]
    } else {
        Resources.getSystem().configuration.locale
    }
    return when (systemLocale.language) {
        "es" -> "es"
        "ja" -> "ja"
        else -> "en"
    }
}
```

Applies locale via `AppCompatDelegate.setApplicationLocales()` and marks as auto-detected (not manually set).

## Manual Language Override

User selections always take priority. Tracked via `user_language_manually_set` SharedPreferences boolean. Once manually set, auto-detection is permanently overridden.

```kotlin
fun changeLanguage(languageCode: String) {
    prefs.edit()
        .putString("app_language", languageCode)
        .putBoolean("user_language_manually_set", true)
        .apply()
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
}
```

## Locale-Aware UI Pattern (Critical)

All Compose components must use this pattern for real-time language switching without app restart:

```kotlin
@Composable
fun MyScreen() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localeContext = remember(configuration) { context }
    
    // Updates immediately when locale changes
    Text(text = localeContext.getString(R.string.my_text))
}
```

**Required imports:**
```kotlin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
```

### Singleton Refresh
Singletons (ThemeRepository, TrackControlThemeManager) need explicit refresh on locale change:

```kotlin
val themeRepository = remember(configuration) { 
    ThemeRepository.refreshForLocaleChange(localeContext) 
}
```

## String Resource Categories

| Category | Example keys |
|---|---|
| Screen titles | `screen_animation_themes`, `screen_settings` |
| Navigation | `nav_player`, `nav_controls`, `nav_settigns` |
| Theme names | `theme_vinyl_record_title`, `theme_dancing_duck_title` |
| UI elements | Settings labels, descriptions, button text |
| Language names | `language_english`, `language_spanish`, `language_japanese` |

## Adding a New Language

### Step 1: Create resource directory
```bash
mkdir app/src/main/res/values-[code]/
```

### Step 2: Create `strings.xml` with all translations
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">GlyphBeat</string>
    <string name="screen_settings">Settings</string>
    <!-- Translate ALL string resources -->
</resources>
```

### Step 3: Add to `detectSystemLanguage()` in `MainActivity.kt`
```kotlin
"fr" -> "fr"  // Add new mapping
```

### Step 4: Add to `availableLanguages` in `SettingsScreen.kt`
```kotlin
LanguageInfo("fr", localeContext.getString(R.string.language_french), null)
```

### Step 5: Add to validation checks
```kotlin
currentAppLanguage in listOf("en", "es", "ja", "fr")
```

## Key Files

| File | Role |
|---|---|
| `MainActivity.kt` | Language initialization, `detectSystemLanguage()`, `initializeAppLanguage()` |
| `ui/screens/SettingsScreen.kt` | Language selection UI, `availableLanguages` list |
| `data/ThemeRepository.kt` | `refreshForLocaleChange()` for theme names |
| `services/trackcontrol/TrackControlThemeManager.kt` | Locale refresh for track control themes |

## Rules

- **Never hardcode strings** — always use string resources
- **Use `localeContext`** not `context` for all string access in Compose
- **Add `remember(configuration)`** dependency for any component displaying localized text
- **Test language switching** in all screens after changes
- **Provide fallback text** — graceful degradation to English for missing translations
- Theme names and descriptions must update dynamically on language change
- App preserves selected theme and all settings across language changes
