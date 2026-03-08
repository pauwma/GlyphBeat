package com.pauwma.glyphbeat.data

import android.content.Context
import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates the full theme import flow:
 * 1. Verify supporter status
 * 2. Download post data
 * 3. Parse frames and convert to grid format
 * 4. Save to local storage
 */
class ImportThemeManager(context: Context) {

    companion object {
        private const val TAG = "ImportThemeManager"
    }

    private val supabase = SupabaseImportRepository()
    private val storage = CustomThemeStorage.getInstance(context)

    sealed class ImportResult {
        data class Success(val themeName: String) : ImportResult()
        data class Error(val message: String) : ImportResult()
        data object AlreadyImported : ImportResult()
        data object NotSupporter : ImportResult()
        data object ThemeLimitReached : ImportResult()
    }

    suspend fun importTheme(postId: Long, uid: String): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Check if already imported
            if (storage.themeExists(postId)) {
                Log.d(TAG, "Theme already imported: postId=$postId")
                return@withContext ImportResult.AlreadyImported
            }

            // Check theme limit
            if (storage.getThemeCount() >= 20) {
                return@withContext ImportResult.ThemeLimitReached
            }

            // Verify supporter status
            val supporterResult = supabase.isSupporter(uid)
            if (supporterResult.isFailure) {
                return@withContext ImportResult.Error(
                    supporterResult.exceptionOrNull()?.message ?: "Failed to verify supporter status"
                )
            }
            if (supporterResult.getOrDefault(false) == false) {
                return@withContext ImportResult.NotSupporter
            }

            // Download post data
            val postResult = supabase.getPost(postId)
            if (postResult.isFailure) {
                return@withContext ImportResult.Error(
                    postResult.exceptionOrNull()?.message ?: "Failed to download post"
                )
            }
            val post = postResult.getOrThrow()

            // Parse frames from JSON
            val parseResult = parsePostData(post.dataJson)
            if (parseResult == null) {
                return@withContext ImportResult.Error("Invalid post data format")
            }
            val (shapedFrames, durations) = parseResult

            // Get author name
            val author = post.userUid?.let { supabase.getUserDisplayName(it) } ?: "Unknown"

            // Create and save theme data
            val themeData = CustomThemeData.fromFrameData(
                postId = post.id,
                title = post.title,
                author = author,
                shapedFrames = shapedFrames,
                durations = durations
            )

            val saved = storage.saveTheme(themeData)
            if (!saved) {
                return@withContext ImportResult.Error("Failed to save theme locally")
            }

            Log.d(TAG, "Successfully imported theme: ${post.title}")
            ImportResult.Success(post.title)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            ImportResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Parse the Glyph Museum post data JSON into frames and durations.
     * Only supports v1 (Phone 3, 489 pixels).
     */
    private fun parsePostData(dataJson: String): Pair<List<IntArray>, List<Long>>? {
        return try {
            val root = JsonParser.parseString(dataJson).asJsonObject
            val version = root.get("v")?.asInt ?: 1

            if (version != 1) {
                Log.w(TAG, "Unsupported post version: $version (only v1 supported)")
                return null
            }

            val framesArray = root.getAsJsonArray("frames") ?: return null
            val frames = mutableListOf<IntArray>()
            val durations = mutableListOf<Long>()

            for (frameElement in framesArray) {
                val frameObj = frameElement.asJsonObject
                val pixelArray = frameObj.getAsJsonArray("p") ?: continue

                val pixels = IntArray(pixelArray.size()) { pixelArray[it].asInt }
                frames.add(pixels)

                // Duration: use "d" field if present, otherwise default 150ms
                val duration = if (frameObj.has("d") && !frameObj.get("d").isJsonNull) {
                    frameObj.get("d").asLong.coerceIn(50L, 2000L)
                } else {
                    150L
                }
                durations.add(duration)
            }

            if (frames.isEmpty()) {
                Log.w(TAG, "No frames found in post data")
                return null
            }

            Pair(frames, durations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse post data", e)
            null
        }
    }
}
