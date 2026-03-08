package com.pauwma.glyphbeat.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pauwma.glyphbeat.themes.animation.CustomTheme
import java.io.File

/**
 * Manages local storage of custom themes imported from Glyph Museum.
 * Each theme is stored as a JSON file in internal storage.
 */
class CustomThemeStorage private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CustomThemeStorage"
        private const val THEMES_DIR = "custom_themes"
        private const val MAX_THEMES = 20

        @Volatile
        private var INSTANCE: CustomThemeStorage? = null

        fun getInstance(context: Context): CustomThemeStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CustomThemeStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val gson = Gson()
    private val themesDir: File
        get() = File(context.filesDir, THEMES_DIR).also { it.mkdirs() }

    /**
     * Save an imported theme to local storage.
     * @return true if saved successfully
     */
    fun saveTheme(data: CustomThemeData): Boolean {
        return try {
            val currentCount = getThemeCount()
            if (currentCount >= MAX_THEMES) {
                Log.w(TAG, "Maximum theme limit reached ($MAX_THEMES)")
                return false
            }

            val file = File(themesDir, "theme_${data.postId}.json")
            file.writeText(gson.toJson(data))
            Log.d(TAG, "Saved custom theme: ${data.title} (postId=${data.postId})")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom theme", e)
            false
        }
    }

    /**
     * Load all saved custom themes.
     */
    fun loadAllThemes(): List<CustomTheme> {
        return try {
            themesDir.listFiles { file -> file.extension == "json" }
                ?.mapNotNull { file -> loadThemeFromFile(file) }
                ?.sortedByDescending { it.importDate }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom themes", e)
            emptyList()
        }
    }

    /**
     * Delete a custom theme by postId.
     * @return true if deleted successfully
     */
    fun deleteTheme(postId: Long): Boolean {
        return try {
            val file = File(themesDir, "theme_${postId}.json")
            val deleted = file.delete()
            if (deleted) {
                Log.d(TAG, "Deleted custom theme: postId=$postId")
            }
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete custom theme", e)
            false
        }
    }

    /**
     * Check if a theme with this postId already exists.
     */
    fun themeExists(postId: Long): Boolean {
        return File(themesDir, "theme_${postId}.json").exists()
    }

    fun getThemeCount(): Int {
        return themesDir.listFiles { file -> file.extension == "json" }?.size ?: 0
    }

    private fun loadThemeFromFile(file: File): CustomTheme? {
        return try {
            val json = file.readText()
            val data = gson.fromJson(json, CustomThemeData::class.java)
            data.toCustomTheme()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme from ${file.name}", e)
            null
        }
    }
}

/**
 * Serializable data class for storing custom theme data as JSON.
 * Frame data is stored as a flat list of brightness values.
 */
data class CustomThemeData(
    val postId: Long,
    val title: String,
    val author: String,
    val frames: List<List<Int>>,
    val durations: List<Long>,
    val importDate: Long
) {
    fun toCustomTheme(): CustomTheme {
        return CustomTheme(
            postId = postId,
            title = title,
            author = author,
            frames = frames.map { it.toIntArray() },
            durations = durations.toLongArray(),
            importDate = importDate
        )
    }

    companion object {
        fun fromFrameData(
            postId: Long,
            title: String,
            author: String,
            shapedFrames: List<IntArray>,
            durations: List<Long>
        ): CustomThemeData {
            // Convert 489-pixel shaped frames to 625-pixel flat grid frames
            val flatFrames = shapedFrames.map { shaped ->
                shapedToFlat(shaped).toList()
            }
            return CustomThemeData(
                postId = postId,
                title = title,
                author = author,
                frames = flatFrames,
                durations = durations,
                importDate = System.currentTimeMillis()
            )
        }

        /**
         * Convert 489-element shaped pixel array to 625-element flat grid array.
         * Uses the Phone 3 glyph shape pattern.
         */
        private fun shapedToFlat(src: IntArray): IntArray {
            val shapePattern = intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25,
                25, 25, 25, 25, 25, 25, 23, 23, 21, 21,
                19, 17, 15, 11, 7
            )
            val gridSize = 25
            val out = IntArray(gridSize * gridSize)

            if (src.size == gridSize * gridSize) {
                // Already flat format
                return src.clone()
            }

            var index = 0
            for (row in 0 until gridSize) {
                val leds = shapePattern[row]
                val startCol = (gridSize - leds) / 2
                for (c in 0 until leds) {
                    if (index < src.size) {
                        out[row * gridSize + startCol + c] = src[index++]
                    }
                }
            }
            return out
        }
    }
}
