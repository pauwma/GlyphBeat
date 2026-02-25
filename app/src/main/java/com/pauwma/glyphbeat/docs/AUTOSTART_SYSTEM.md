# AutoStart System

## Overview

Automatic activation system that monitors media playback and starts/stops Glyph visualization when whitelisted music apps play. Provides seamless hands-free experience.

## Architecture

Three core components:

### 1. MusicDetectionService (`services/autostart/MusicDetectionService.kt`)
Foreground service that monitors media sessions and binds/unbinds `MediaPlayerToyService`.

**Monitoring strategy:**
- Primary: callback-based detection via `MediaControlHelper` (immediate, minimal CPU)
- Backup: 2-second periodic checks (reliability fallback)

**Timing:**
- `autoStartDelay = 0L` — instant activation
- `autoStopDelay = 3000L` — 3s delay before stopping (prevents flickering)
- `glyphSuspensionDuration = 10000L` — 10s suspension on manual Glyph conflict

**State variables:**
```kotlin
private var isGlyphServiceActive = false        // MediaPlayerToyService binding status
private var isBindingInProgress = false         // Prevents duplicate bind attempts
private var currentPlayingApp: String? = null   // Tracks triggering app
private var isSuspendedForGlyph = false        // Conflict prevention state
```

**Service binding:**
```kotlin
val serviceIntent = Intent().apply {
    component = ComponentName("com.pauwma.glyphbeat", 
        "com.pauwma.glyphbeat.services.media.MediaPlayerToyService")
    putExtra("auto_activated", true)
}
bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
```

### 2. MusicAppWhitelistManager (`services/autostart/MusicAppWhitelistManager.kt`)
Manages which apps can trigger automatic activation.

**Features:**
- 40+ pre-configured apps in `DEFAULT_MUSIC_APPS` (Spotify, YouTube Music, Apple Music, Tidal, Deezer, etc.)
- Auto-discovery: whitelists popular installed apps on first run
- Full CRUD via settings UI
- 5-second cache with thread-safe collections
- Install status detection (installed vs. available)
- JSON persistence with legacy StringSet→JSON migration
- `SettingsDebugUtils` integration for production debugging

**Adding new apps:**
1. Add to `DEFAULT_MUSIC_APPS` map in `MusicAppWhitelistManager`
2. Test package name detection
3. Update supported apps documentation

### 3. AutoStartBroadcastReceiver (`services/autostart/AutoStartBroadcastReceiver.kt`)
Handles system-level lifecycle events.

**Intent filters:**
- `BOOT_COMPLETED` — starts MusicDetectionService after boot if enabled
- `MY_PACKAGE_REPLACED` — restarts service after app updates
- Manual start/stop intents from UI

## Detection Flow

```
MediaControlHelper callbacks
    → Active MediaController sessions
    → Playback state detection (PLAYING/PAUSED/STOPPED)
    → Whitelist check (MusicAppWhitelistManager.isAppWhitelisted())
    → Blacklist check
    → Bind/unbind MediaPlayerToyService
```

### Blacklisted Packages
System and non-music apps that are always excluded:
```kotlin
private val BLACKLISTED_PACKAGES = setOf(
    "com.nothing.hearthstone",      // Nothing Phone system apps
    "com.android.systemui",         // Android system UI
    "com.android.chrome",           // Chrome browser media
    "com.google.android.apps.maps", // Google Maps navigation
    // ... other system and non-music apps
)
```

## Advanced Features

### Battery Awareness
Prevents activation during low battery.

```kotlin
val batteryAwarenessEnabled = preferences.getBoolean("battery_awareness_enabled", false)
if (batteryAwarenessEnabled) {
    val batteryLevel = batteryManager.getIntProperty(BATTERY_PROPERTY_CAPACITY)
    val batteryThreshold = preferences.getInt("battery_threshold", 10)
    if (batteryLevel < batteryThreshold) return  // Skip activation
}
```

Settings: toggle enable/disable, adjustable threshold (5–50%, default 10%), UI in settings expandable panel.

### Glyph Conflict Prevention
Prevents interference when user manually activates Glyph interface.

- Monitors `GLYPH_BUTTON_PRESSED` broadcasts from manual Glyph usage
- Immediately stops auto-started service on conflict
- 10-second suspension period
- Auto-resumes if music still playing from whitelisted app after suspension

```kotlin
private fun checkResumeFromSuspension() {
    if (currentTime - suspensionStartTime >= glyphSuspensionDuration) {
        if (isPlaying && packageName in whitelist) {
            startGlyphService(packageName)
        }
    }
}
```

### Smart App Switching
Handles multiple playing music apps seamlessly:

```kotlin
when {
    // New whitelisted app starts → update reference, keep service active
    newApp != null && newApp != currentApp && isWhitelisted(newApp) -> {
        currentPlayingApp = newApp
    }
    // Current app removed from whitelist → find alternative or stop
    !isWhitelisted(currentApp) && currentApp == activeApp -> {
        val alternative = findAlternativePlayingApp(excludedPackages)
        if (alternative != null) currentPlayingApp = alternative
        else stopGlyphService()
    }
}
```

## System Integration

### Required Permissions
```xml
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<!-- + NotificationListenerService declaration -->
```

### Manifest Declarations
```xml
<service
    android:name="com.pauwma.glyphbeat.services.autostart.MusicDetectionService"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />

<receiver android:name="com.pauwma.glyphbeat.services.autostart.AutoStartBroadcastReceiver">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

### MainActivity Integration
```kotlin
val prefs = getSharedPreferences("glyph_settings", MODE_PRIVATE)
val isAutoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
if (isAutoStartEnabled) {
    MusicDetectionService.start(this)
}
```

## UI Integration

### Settings Screen (`ui/screens/SettingsScreen.kt`)
- **Master toggle**: enable/disable AutoStart
- **Expandable panel**: battery awareness settings, music app whitelist
- **Whitelist management**: visual list with install status, toggle switches, refresh button
- **Real-time updates**: UI reflects changes from shake controls and other sources

### Shake Control Integration
- Shake `AUTO_START` behavior auto-disabled when AutoStart disabled
- Bidirectional preference synchronization
- Prevents conflicting control methods

### Notification States
| State | Message |
|---|---|
| Idle | "Monitoring for music playback" |
| Active | "Media Player active for: [App Name]" |
| Suspended | "Suspended - Glyph interface in use" |
| Alt suspended | "Monitoring suspended - Glyph interface active" |

## Performance

- Callback-based primary detection (minimal CPU)
- 2s periodic backup checks
- 5s whitelist cache validity
- Thread-safe collections for whitelist management
- Synchronized access to shared state
- Proper coroutine scope management and cleanup in `onDestroy()`

## Integration with Other Features

### MediaControlHelper
- Registers for immediate state notifications (callbacks)
- Shared monitoring — reuses MediaControlHelper for efficient session detection
- Ensures consistent playback state across all features

### Shake Control System
- `AUTO_START` behavior: shake gesture can toggle AutoStart on/off
- Conflict prevention: disables shake AUTO_START when AutoStart disabled
- Bidirectional preference synchronization

## Debugging

### Service Not Starting
- Check `auto_start_enabled` preference
- Verify notification access permission granted
- Confirm MusicDetectionService foreground status

### No Activation on Music Play
- Verify app in whitelist: `MusicAppWhitelistManager.isAppWhitelisted()`
- Check battery awareness settings
- Ensure app not in blacklist
- Verify MediaSession detection

### Frequent Activation/Deactivation
- Check `autoStopDelay` setting (default 3000ms)
- Verify media app state consistency
- Review suspension logic for conflicts

### Testing Checklist
1. Toggle AutoStart in settings → verify MusicDetectionService starts
2. Add/remove apps from whitelist → test activation behavior
3. Set low battery threshold → test prevention at low battery
4. Manually activate Glyph during auto session → test conflict resolution
5. Play music from multiple whitelisted apps → test app switching
