package com.pauwma.glyphbeat.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Handles persistence of theme settings using SharedPreferences with JSON serialization.
 * Provides robust error handling, versioning, and migration support.
 */
class ThemeSettingsPersistence private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ThemeSettingsPersistence"
        private const val PREFS_NAME = "theme_settings"
        private const val KEY_SETTINGS_PREFIX = "settings_"
        private const val KEY_VERSION_PREFIX = "version_"
        private const val KEY_LAST_UPDATED_PREFIX = "updated_"
        private const val CURRENT_PERSISTENCE_VERSION = 1
        
        @Volatile
        private var INSTANCE: ThemeSettingsPersistence? = null
        
        fun getInstance(context: Context): ThemeSettingsPersistence {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeSettingsPersistence(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .registerTypeHierarchyAdapter(ThemeSetting::class.java, ThemeSettingTypeAdapter())
        .create()
    
    /**
     * Save theme settings to persistent storage.
     * 
     * @param themeSettings The settings to save
     * @return True if saved successfully, false otherwise
     */
    fun saveThemeSettings(themeSettings: ThemeSettings): Boolean {
        return try {
            val json = gson.toJson(themeSettings)
            val timestamp = System.currentTimeMillis()
            
            prefs.edit()
                .putString(KEY_SETTINGS_PREFIX + themeSettings.themeId, json)
                .putInt(KEY_VERSION_PREFIX + themeSettings.themeId, CURRENT_PERSISTENCE_VERSION)
                .putLong(KEY_LAST_UPDATED_PREFIX + themeSettings.themeId, timestamp)
                .apply()
            
            Log.d(TAG, "Saved settings for theme: ${themeSettings.themeId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings for theme: ${themeSettings.themeId}", e)
            false
        }
    }
    
    /**
     * Load theme settings from persistent storage.
     * 
     * @param themeId The ID of the theme to load settings for
     * @param fallbackSchema Optional schema to use if settings don't exist
     * @return Loaded settings, fallback schema, or null if not found
     */
    fun loadThemeSettings(themeId: String, fallbackSchema: ThemeSettings? = null): ThemeSettings? {
        return try {
            val json = prefs.getString(KEY_SETTINGS_PREFIX + themeId, null)
            if (json != null) {
                val settings = gson.fromJson(json, ThemeSettings::class.java)
                val version = prefs.getInt(KEY_VERSION_PREFIX + themeId, 1)
                
                // Handle version migration if needed
                val migratedSettings = migrateSettingsIfNeeded(settings, version)
                
                // Validate and clean the loaded settings
                val validatedSettings = ThemeSettingsValidator.clean(migratedSettings)
                
                Log.d(TAG, "Loaded settings for theme: $themeId")
                validatedSettings
            } else {
                Log.d(TAG, "No saved settings found for theme: $themeId")
                fallbackSchema
            }
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "Invalid JSON for theme settings: $themeId, clearing corrupted data", e)
            clearCorruptedSettings(themeId)
            fallbackSchema
        } catch (e: JsonParseException) {
            Log.w(TAG, "JSON parse error for theme settings: $themeId, clearing corrupted data", e)
            clearCorruptedSettings(themeId)
            fallbackSchema
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings for theme: $themeId, clearing corrupted data", e)
            clearCorruptedSettings(themeId)
            fallbackSchema
        }
    }
    
    /**
     * Check if settings exist for a theme.
     * 
     * @param themeId The theme ID to check
     * @return True if settings exist
     */
    fun hasSettings(themeId: String): Boolean {
        return prefs.contains(KEY_SETTINGS_PREFIX + themeId)
    }
    
    /**
     * Delete settings for a theme.
     * 
     * @param themeId The theme ID to delete settings for
     * @return True if deleted successfully
     */
    fun deleteThemeSettings(themeId: String): Boolean {
        return try {
            prefs.edit()
                .remove(KEY_SETTINGS_PREFIX + themeId)
                .remove(KEY_VERSION_PREFIX + themeId)
                .remove(KEY_LAST_UPDATED_PREFIX + themeId)
                .apply()
            
            Log.d(TAG, "Deleted settings for theme: $themeId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete settings for theme: $themeId", e)
            false
        }
    }
    
    /**
     * Get all theme IDs that have saved settings.
     * 
     * @return List of theme IDs with saved settings
     */
    fun getSavedThemeIds(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith(KEY_SETTINGS_PREFIX) }
            .map { it.removePrefix(KEY_SETTINGS_PREFIX) }
    }
    
    /**
     * Get the last updated timestamp for a theme's settings.
     * 
     * @param themeId The theme ID
     * @return Timestamp in milliseconds, or 0 if not found
     */
    fun getLastUpdated(themeId: String): Long {
        return prefs.getLong(KEY_LAST_UPDATED_PREFIX + themeId, 0L)
    }
    
    /**
     * Reset all settings to defaults by clearing all stored data.
     * This affects all themes.
     * 
     * @return True if reset successfully
     */
    fun resetAllSettings(): Boolean {
        return try {
            prefs.edit().clear().apply()
            Log.d(TAG, "Reset all theme settings to defaults")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset all settings", e)
            false
        }
    }
    
    /**
     * Export all theme settings to a JSON string.
     * Useful for backup or sharing configurations.
     * 
     * @return JSON string containing all settings, or null if export fails
     */
    fun exportAllSettings(): String? {
        return try {
            val allSettings = mutableMapOf<String, ThemeSettings>()
            
            getSavedThemeIds().forEach { themeId ->
                loadThemeSettings(themeId)?.let { settings ->
                    allSettings[themeId] = settings
                }
            }
            
            gson.toJson(allSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export settings", e)
            null
        }
    }
    
    /**
     * Import theme settings from a JSON string.
     * This will overwrite existing settings for matching theme IDs.
     * 
     * @param jsonData JSON string containing theme settings
     * @return True if import was successful
     */
    fun importAllSettings(jsonData: String): Boolean {
        return try {
            val typeToken = object : TypeToken<Map<String, ThemeSettings>>() {}.type
            val allSettings: Map<String, ThemeSettings> = gson.fromJson(jsonData, typeToken)
            
            var successCount = 0
            allSettings.forEach { (themeId, settings) ->
                if (saveThemeSettings(settings)) {
                    successCount++
                }
            }
            
            Log.d(TAG, "Imported $successCount out of ${allSettings.size} theme settings")
            successCount == allSettings.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import settings", e)
            false
        }
    }
    
    /**
     * Get storage usage statistics.
     * 
     * @return Map with storage statistics
     */
    fun getStorageStats(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        val themeIds = getSavedThemeIds()
        
        stats["theme_count"] = themeIds.size
        stats["total_size_bytes"] = calculateTotalSize()
        stats["oldest_theme"] = if (themeIds.isNotEmpty()) themeIds.first() else "none"
        stats["newest_theme"] = if (themeIds.isNotEmpty()) themeIds.last() else "none"
        
        return stats
    }
    
    /**
     * Migrate settings between versions if needed.
     * Currently just returns the settings as-is, but can be extended for future migrations.
     */
    private fun migrateSettingsIfNeeded(settings: ThemeSettings, version: Int): ThemeSettings {
        return when {
            version < CURRENT_PERSISTENCE_VERSION -> {
                Log.d(TAG, "Migrating settings from version $version to $CURRENT_PERSISTENCE_VERSION")
                // Add migration logic here if needed in the future
                settings.copy(version = ThemeSettings.CURRENT_VERSION)
            }
            else -> settings
        }
    }
    
    /**
     * Calculate total storage size used by settings.
     */
    private fun calculateTotalSize(): Long {
        var totalSize = 0L
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_SETTINGS_PREFIX)) {
                totalSize += (value as? String)?.toByteArray()?.size ?: 0
            }
        }
        return totalSize
    }
    
    /**
     * Get the theme ID with the oldest settings.
     */
    private fun getOldestThemeId(): String? {
        var oldestId: String? = null
        var oldestTime = Long.MAX_VALUE
        
        getSavedThemeIds().forEach { themeId ->
            val timestamp = getLastUpdated(themeId)
            if (timestamp < oldestTime) {
                oldestTime = timestamp
                oldestId = themeId
            }
        }
        
        return oldestId
    }
    
    /**
     * Get the theme ID with the newest settings.
     */
    private fun getNewestThemeId(): String? {
        var newestId: String? = null
        var newestTime = 0L
        
        getSavedThemeIds().forEach { themeId ->
            val timestamp = getLastUpdated(themeId)
            if (timestamp > newestTime) {
                newestTime = timestamp
                newestId = themeId
            }
        }
        
        return newestId
    }
    
    /**
     * Clear corrupted settings for a specific theme.
     * This is called automatically when JSON parsing fails.
     */
    private fun clearCorruptedSettings(themeId: String) {
        try {
            prefs.edit()
                .remove(KEY_SETTINGS_PREFIX + themeId)
                .remove(KEY_VERSION_PREFIX + themeId)
                .remove(KEY_LAST_UPDATED_PREFIX + themeId)
                .apply()
            Log.d(TAG, "Cleared corrupted settings for theme: $themeId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear corrupted settings for theme: $themeId", e)
        }
    }
}

/**
 * Custom type adapter to handle polymorphic ThemeSetting serialization/deserialization.
 * This is needed because Gson can't handle sealed classes directly.
 */
private class ThemeSettingTypeAdapter : JsonSerializer<ThemeSetting>, JsonDeserializer<ThemeSetting> {
    
    // Create a separate Gson instance without our custom adapter to avoid recursion
    private val gson = Gson()
    
    override fun serialize(src: ThemeSetting, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val result = JsonObject()
        result.addProperty("type", src.javaClass.simpleName)
        // Use the separate gson instance to avoid recursion
        result.add("data", gson.toJsonTree(src))
        return result
    }
    
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ThemeSetting {
        return try {
            val jsonObject = json.asJsonObject
            val typeElement = jsonObject.get("type")
            val dataElement = jsonObject.get("data")
            
            // Check if we have the required fields
            if (typeElement == null || dataElement == null) {
                throw JsonParseException("Missing 'type' or 'data' fields in ThemeSetting JSON")
            }
            
            val type = typeElement.asString
            
            // Use the separate gson instance to avoid recursion
            when (type) {
                "SliderSetting" -> gson.fromJson(dataElement, SliderSetting::class.java)
                "ToggleSetting" -> gson.fromJson(dataElement, ToggleSetting::class.java)
                "DropdownSetting" -> gson.fromJson(dataElement, DropdownSetting::class.java)
                else -> throw JsonParseException("Unknown ThemeSetting type: $type")
            }
        } catch (e: Exception) {
            // Log the error and throw a more descriptive exception
            Log.e("ThemeSettingsPersistence", "Failed to deserialize ThemeSetting: ${e.message}", e)
            throw JsonParseException("Failed to deserialize ThemeSetting: ${e.message}", e)
        }
    }
}