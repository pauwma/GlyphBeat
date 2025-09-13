package com.pauwma.glyphbeat.services.autostart

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections

/**
 * Manages the whitelist of music apps that can trigger automatic Glyph service activation.
 * Provides functionality to:
 * - Maintain a list of whitelisted apps
 * - Detect installed music apps
 * - Save/load user preferences for each app
 */
class MusicAppWhitelistManager private constructor(private val context: Context) {
    
    private val preferences: SharedPreferences = context.getSharedPreferences("music_app_whitelist", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val packageManager = context.packageManager
    
    // Cache for whitelisted apps - use thread-safe collections
    private var whitelistedApps: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
    private var lastCacheUpdate = 0L
    private val cacheValidityMs = 5000L // Cache for 5 seconds
    
    companion object {
        private const val LOG_TAG = "MusicAppWhitelist"
        private const val PREF_WHITELISTED_APPS = "whitelisted_apps"
        private const val PREF_CUSTOM_APP_NAMES = "custom_app_names"
        
        @Volatile
        private var INSTANCE: MusicAppWhitelistManager? = null
        
        fun getInstance(context: Context): MusicAppWhitelistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MusicAppWhitelistManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Default list of known music app package names
        val DEFAULT_MUSIC_APPS = mapOf(
            "com.spotify.music" to "Spotify",
            "com.google.android.apps.youtube.music" to "YouTube Music",
            "com.google.android.youtube" to "YouTube",
            "com.amazon.mp3" to "Amazon Music",
            "com.apple.android.music" to "Apple Music",
            "com.tidal.android" to "Tidal",
            "com.deezer.android.app" to "Deezer",
            "com.soundcloud.android" to "SoundCloud",
            "com.pandora.android" to "Pandora",
            "com.bandcamp.android" to "Bandcamp",
            "com.aspiro.tidal" to "Tidal HiFi",
            "com.gaana" to "Gaana",
            "com.jio.media.jiobeats" to "JioSaavn",
            "com.shazam.android" to "Shazam",
            "com.mixcloud.player" to "Mixcloud",
            "com.audiomack" to "Audiomack",
            "com.qobuz.music" to "Qobuz",
            "com.napster.android" to "Napster",
            "com.anghami" to "Anghami",
            "com.bsbportal.music" to "Wynk Music",
            "com.clearchannel.iheartradio.controller" to "iHeartRadio",
            "tunein.player" to "TuneIn Radio",
            "com.globaldelight.boom" to "Boom",
            "com.musixmatch.android.lyrify" to "Musixmatch",
            "com.poweramp.v3" to "Poweramp",
            "com.neutronmp" to "Neutron Music Player",
            "com.jetappfactory.jetaudio" to "jetAudio",
            "com.maxmpz.audioplayer" to "Poweramp Music Player",
            "com.google.android.music" to "Google Play Music",
            "com.samsung.android.app.music.chn" to "Samsung Music",
            "com.miui.player" to "Mi Music",
            "com.oppo.music" to "OPPO Music",
            "com.vivo.easyshare" to "Vivo Music",
            "com.oneplus.music" to "OnePlus Music",
            "com.sec.android.app.music" to "Samsung Music",
            "com.sonyericsson.music" to "Sony Music",
            "com.google.android.apps.podcasts" to "Google Podcasts",
            "com.spotify.lite" to "Spotify Lite",
            "fm.castbox.audiobook.radio.podcast" to "Castbox",
            "com.wondery.app" to "Wondery",
            "com.audible.application" to "Audible",
            "tv.plex.labs.plexamp" to "Plexamp"
        )
    }
    
    init {
        loadWhitelistedApps()
        initializeDefaultApps()
    }
    
    /**
     * Check if an app is whitelisted for auto-start
     */
    fun isAppWhitelisted(packageName: String): Boolean {
        refreshCacheIfNeeded()
        return whitelistedApps.contains(packageName)
    }
    
    /**
     * Add an app to the whitelist
     */
    fun addToWhitelist(packageName: String) {
        whitelistedApps.add(packageName)
        saveWhitelistedApps()
        Log.d(LOG_TAG, "Added $packageName to whitelist")
    }
    
    /**
     * Remove an app from the whitelist
     */
    fun removeFromWhitelist(packageName: String) {
        whitelistedApps.remove(packageName)
        saveWhitelistedApps()
        Log.d(LOG_TAG, "Removed $packageName from whitelist")
    }
    
    /**
     * Toggle an app's whitelist status
     */
    fun toggleWhitelist(packageName: String) {
        if (isAppWhitelisted(packageName)) {
            removeFromWhitelist(packageName)
        } else {
            addToWhitelist(packageName)
        }
    }
    
    /**
     * Get all whitelisted apps
     */
    fun getWhitelistedApps(): Set<String> {
        refreshCacheIfNeeded()
        return whitelistedApps.toSet()
    }
    
    /**
     * Debug method to test specific app detection
     */
    fun debugAppDetection() {
        Log.d(LOG_TAG, "=== Debug App Detection ===")
        val testApps = listOf("com.spotify.music", "com.google.android.youtube", "com.google.android.apps.youtube.music")
        testApps.forEach { packageName ->
            val isInstalled = isAppInstalled(packageName)
            Log.d(LOG_TAG, "Debug: $packageName -> installed: $isInstalled")
        }
        
        // Also try to list all installed packages to see what we can detect
        try {
            val installedPackages = packageManager.getInstalledPackages(0)
            val musicApps = installedPackages.filter { packageInfo ->
                val packageName = packageInfo.packageName
                packageName.contains("music", ignoreCase = true) || 
                packageName.contains("spotify", ignoreCase = true) ||
                packageName.contains("youtube", ignoreCase = true)
            }
            Log.d(LOG_TAG, "Found ${musicApps.size} potential music apps:")
            musicApps.take(10).forEach { packageInfo ->
                Log.d(LOG_TAG, "  - ${packageInfo.packageName}")
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error listing installed packages", e)
        }
    }
    
    /**
     * Get all music apps (both installed and popular uninstalled ones) with their whitelist status
     */
    fun getInstalledMusicApps(): List<MusicAppInfo> {
        val allApps = mutableListOf<MusicAppInfo>()
        val processedPackages = mutableSetOf<String>()
        
        // First, add all popular music apps (installed and not installed)
        DEFAULT_MUSIC_APPS.forEach { (packageName, defaultName) ->
            val isInstalled = isAppInstalled(packageName)
            allApps.add(
                MusicAppInfo(
                    packageName = packageName,
                    appName = getAppName(packageName) ?: defaultName,
                    isWhitelisted = isAppWhitelisted(packageName),
                    isInstalled = isInstalled
                )
            )
            processedPackages.add(packageName)
        }
        
        // Then add any whitelisted apps that aren't in our default list
        whitelistedApps.forEach { packageName ->
            if (!processedPackages.contains(packageName)) {
                val isInstalled = isAppInstalled(packageName)
                allApps.add(
                    MusicAppInfo(
                        packageName = packageName,
                        appName = getAppName(packageName) ?: packageName,
                        isWhitelisted = true,
                        isInstalled = isInstalled
                    )
                )
            }
        }
        
        // Sort: installed apps first, then by app name
        return allApps.sortedWith(compareByDescending<MusicAppInfo> { it.isInstalled }.thenBy { it.appName.lowercase() })
    }
    
    /**
     * Get a user-friendly app name from package name
     */
    fun getAppName(packageName: String): String? {
        // First check custom names
        val customNames = getCustomAppNames()
        customNames[packageName]?.let { return it }
        
        // Then check default names
        DEFAULT_MUSIC_APPS[packageName]?.let { return it }
        
        // Finally try to get from PackageManager
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Check if an app is installed
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            // Try multiple approaches to detect installed apps
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            
            Log.v(LOG_TAG, "App $packageName: installed=true")
            true
            
        } catch (e: PackageManager.NameNotFoundException) {
            //Log.v(LOG_TAG, "App $packageName: not found")
            false
        } catch (e: Exception) {
            //Log.w(LOG_TAG, "Error checking if $packageName is installed: ${e.message}")
            
            // Fallback: try to get application info directly
            try {
                packageManager.getApplicationInfo(packageName, 0)
                Log.v(LOG_TAG, "App $packageName: found via getApplicationInfo")
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                Log.v(LOG_TAG, "App $packageName: not installed (fallback confirmed)")
                false
            }
        }
    }
    
    /**
     * Initialize default apps on first run
     */
    private fun initializeDefaultApps() {
        if (preferences.getBoolean("initialized", false)) return
        
        // On first run, whitelist popular apps that are actually installed
        val popularApps = listOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.amazon.mp3",
            "com.apple.android.music",
            "com.soundcloud.android",
            "com.tidal.android",
            "com.deezer.android.app"
        )
        
        var whitelistedCount = 0
        popularApps.forEach { packageName ->
            if (isAppInstalled(packageName)) {
                addToWhitelist(packageName)
                whitelistedCount++
            }
        }
        
        preferences.edit().putBoolean("initialized", true).apply()
        Log.d(LOG_TAG, "Initialized $whitelistedCount default whitelisted apps")
    }
    
    /**
     * Load whitelisted apps from preferences with enhanced error handling
     */
    private fun loadWhitelistedApps() {
        Log.d(LOG_TAG, "Loading whitelisted apps from preferences")
        val json = preferences.getString(PREF_WHITELISTED_APPS, null)
        if (json != null) {
            try {
                Log.d(LOG_TAG, "Found stored whitelist JSON, length: ${json.length} characters")
                val type = object : TypeToken<MutableSet<String>>() {}.type
                val loadedApps = gson.fromJson<MutableSet<String>>(json, type)
                whitelistedApps = Collections.synchronizedSet(loadedApps)
                Log.d(LOG_TAG, "Successfully loaded ${whitelistedApps.size} whitelisted apps: ${whitelistedApps.joinToString()}")
            } catch (e: com.google.gson.JsonSyntaxException) {
                Log.e(LOG_TAG, "JSON syntax error loading whitelisted apps - clearing corrupted data", e)
                Log.e(LOG_TAG, "Corrupted JSON preview: ${json.take(200)}")
                whitelistedApps = Collections.synchronizedSet(mutableSetOf())
                clearCorruptedWhitelist()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Unexpected error loading whitelisted apps - clearing data", e)
                Log.e(LOG_TAG, "Error type: ${e::class.java.simpleName}")
                whitelistedApps = Collections.synchronizedSet(mutableSetOf())
                clearCorruptedWhitelist()
            }
        } else {
            Log.d(LOG_TAG, "No stored whitelist found, starting with empty set")
            whitelistedApps = Collections.synchronizedSet(mutableSetOf())
        }
        lastCacheUpdate = System.currentTimeMillis()
    }
    
    /**
     * Save whitelisted apps to preferences with enhanced persistence verification
     */
    private fun saveWhitelistedApps() {
        try {
            Log.d(LOG_TAG, "Saving ${whitelistedApps.size} whitelisted apps: ${whitelistedApps.joinToString()}")
            val json = gson.toJson(whitelistedApps)
            Log.d(LOG_TAG, "Generated whitelist JSON, length: ${json.length} characters")
            
            // Use commit() instead of apply() for immediate verification
            val success = preferences.edit()
                .putString(PREF_WHITELISTED_APPS, json)
                .commit()
            
            if (success) {
                // Verify the save by reading it back immediately
                val verification = preferences.getString(PREF_WHITELISTED_APPS, null)
                if (verification != null && verification == json) {
                    Log.d(LOG_TAG, "Successfully saved and verified whitelist persistence")
                    lastCacheUpdate = System.currentTimeMillis()
                } else {
                    Log.e(LOG_TAG, "Whitelist save verification failed - data mismatch!")
                    Log.e(LOG_TAG, "Expected JSON: $json")
                    Log.e(LOG_TAG, "Retrieved JSON: $verification")
                    
                    // Retry once with apply()
                    preferences.edit()
                        .putString(PREF_WHITELISTED_APPS, json)
                        .apply()
                    
                    Log.w(LOG_TAG, "Retried save with apply() method")
                }
            } else {
                Log.e(LOG_TAG, "SharedPreferences commit() returned false - save may have failed")
                
                // Fallback to apply() method
                preferences.edit()
                    .putString(PREF_WHITELISTED_APPS, json)
                    .apply()
                
                Log.w(LOG_TAG, "Used apply() as fallback after commit() failure")
            }
            
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error saving whitelisted apps", e)
            
            // Last resort: Try to save with basic method
            try {
                val json = gson.toJson(whitelistedApps)
                preferences.edit().putString(PREF_WHITELISTED_APPS, json).apply()
                Log.w(LOG_TAG, "Emergency save attempt completed")
            } catch (e2: Exception) {
                Log.e(LOG_TAG, "Emergency save also failed", e2)
            }
        }
    }
    
    /**
     * Get custom app names from preferences
     */
    private fun getCustomAppNames(): Map<String, String> {
        val json = preferences.getString(PREF_CUSTOM_APP_NAMES, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error loading custom app names", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * Refresh cache if needed
     */
    private fun refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - lastCacheUpdate > cacheValidityMs) {
            loadWhitelistedApps()
        }
    }
    
    /**
     * Clear all whitelisted apps
     */
    fun clearWhitelist() {
        whitelistedApps.clear()
        saveWhitelistedApps()
        Log.d(LOG_TAG, "Cleared all whitelisted apps")
    }
    
    /**
     * Clear corrupted whitelist data
     */
    private fun clearCorruptedWhitelist() {
        Log.w(LOG_TAG, "Clearing corrupted whitelist data")
        try {
            preferences.edit()
                .remove(PREF_WHITELISTED_APPS)
                .remove(PREF_CUSTOM_APP_NAMES)
                .apply()
            Log.d(LOG_TAG, "Successfully cleared corrupted whitelist data")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error clearing corrupted whitelist data", e)
        }
    }
    
    /**
     * Data class representing a music app
     */
    data class MusicAppInfo(
        val packageName: String,
        val appName: String,
        val isWhitelisted: Boolean,
        val isInstalled: Boolean
    )
}