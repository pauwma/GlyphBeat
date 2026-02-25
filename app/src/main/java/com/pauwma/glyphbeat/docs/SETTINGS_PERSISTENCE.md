# Settings Persistence

## Problem

### Issue 1: General Settings Reset
**Symptom:** Settings save correctly in debug builds but reset to defaults after app restart in release builds (Google Play distribution).
**Root cause:** ProGuard/R8 obfuscation breaks Gson JSON serialization of complex data classes. Class names get mangled, causing `ClassNotFoundException` on deserialization.

### Issue 2: AutoStart Whitelist Failure
**Symptom:** Music app whitelist specifically fails to persist across restarts.
**Root cause:** SharedPreferences file mismatch between components + storage format incompatibility (StringSet vs. JSON).

## Solution Architecture

### 1. ProGuard Protection Rules (`app/proguard-rules.pro`)

Keep rules protect all serialized classes from obfuscation:
- Theme settings: `ThemeSettings`, `SliderSetting`, `ToggleSetting`, `DropdownSetting`
- Shake control: `ShakeControlSettings`, `BehaviorSettings`
- AutoStart: `MusicAppWhitelistManager` data classes
- Gson serialization infrastructure

**⚠️ When adding new serialized data classes, ALWAYS add a keep rule:**
```proguard
-keep class com.pauwma.glyphbeat.your.package.NewDataClass { *; }
```

### 2. Enhanced Persistence Layer (`ui/settings/ThemeSettingsPersistence.kt`)

- **Save verification:** automatically reads back after write to confirm success
- **Obfuscation detection:** identifies `ClassNotFoundException` and JSON corruption patterns
- **Graceful fallback:** returns defaults when corruption detected (no crash)
- **Comprehensive logging:** detailed debug info for troubleshooting

### 3. AutoStart Whitelist Fix (`services/autostart/MusicAppWhitelistManager.kt`)

- **Architecture cleanup:** removed legacy SharedPreferences file mismatch
- **Format unification:** consistent JSON storage across all components
- **Legacy migration:** automatic migration from old StringSet format to JSON
- **SettingsDebugUtils integration:** production debugging support

### 4. Settings Validation System (`utils/SettingsValidator.kt`)

Runs automatically on app startup via `MainActivity`:
- Validates all settings across all SharedPreferences files
- Type safety checks — ensures correct types and values
- Corruption detection — identifies obfuscation-related data corruption
- Detailed validation reports with per-file results

**When adding new critical settings:**
```kotlin
// In SettingsValidator.kt
SettingDefinition("new_setting_key", String::class.java, true, "default_value")
```

### 5. Debug Utilities (`utils/SettingsDebugUtils.kt`)

- **SharedPreferences wrapper:** logs all read/write operations in debug builds
- **Obfuscation issue detection:** identifies suspicious patterns in stored data
- **Performance monitoring:** tracks settings operations and success rates

## Validation Output Example
```
I/SettingsValidator: Starting comprehensive settings validation...
I/SettingsValidator: [glyph_settings] PASS - 6 settings checked, 12 total keys
I/SettingsValidator: [theme_settings] PASS - 1 settings checked, 8 total keys
I/SettingsValidator: [music_app_whitelist] PASS - 2 settings checked, 3 total keys
I/SettingsValidator: Settings validation complete: 5 files checked, 0 errors, 0 warnings

D/MusicAppWhitelist: Loading whitelisted apps from preferences
D/MusicAppWhitelist: Found stored whitelist JSON, length: 156 characters
D/MusicAppWhitelist: Successfully loaded 3 whitelisted apps: [com.spotify.music, ...]
```

## Adding New Settings Checklist

1. ✅ Add ProGuard keep rule in `app/proguard-rules.pro`
2. ✅ Add validation in `utils/SettingsValidator.kt`
3. ✅ Test in **debug** build — verify save/load works
4. ✅ Test in **release** build — verify ProGuard doesn't break serialization
5. ✅ Check startup logs for validation errors

## Troubleshooting

| Problem | Check |
|---|---|
| Settings reset on restart | Startup logs in MainActivity for validation errors |
| Corruption suspected | Enable `SettingsDebugUtils.wrapSharedPreferences()` |
| Unknown class errors | Review ProGuard mapping — check if class is obfuscated |
| Garbled JSON content | Look for obfuscated class names in stored JSON |
| Whitelist not persisting | Check MusicAppWhitelistManager logs for migration/save/load operations |

## Key Files

| File | Purpose |
|---|---|
| `app/proguard-rules.pro` | Keep rules for all serialized classes |
| `utils/SettingsValidator.kt` | Startup validation system |
| `utils/SettingsDebugUtils.kt` | Debug logging wrapper |
| `ui/settings/ThemeSettingsPersistence.kt` | Theme settings with save verification |
| `services/autostart/MusicAppWhitelistManager.kt` | Whitelist persistence + legacy migration |
| `MainActivity.kt` | Triggers validation on startup |
| `docs/SETTINGS_PERSISTENCE_FIX.md` | Original root cause analysis (pre-existing) |
