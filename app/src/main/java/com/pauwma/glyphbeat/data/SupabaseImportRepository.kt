package com.pauwma.glyphbeat.data

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.pauwma.glyphbeat.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight Supabase REST API client for importing themes from Glyph Museum.
 * Read-only access using the anon key — no Supabase SDK dependency needed.
 */
class SupabaseImportRepository {

    companion object {
        private const val TAG = "SupabaseImport"
    }

    private val supabaseUrl: String = BuildConfig.SUPABASE_URL
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Verify that the given UID belongs to a supporter (has a non-null purchase_token).
     */
    suspend fun isSupporter(uid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "$supabaseUrl/rest/v1/profiles?uid=eq.$uid&select=purchase_token"
            val response = makeGetRequest(url)

            if (response == null) {
                return@withContext Result.failure(Exception("Network error"))
            }

            val array = JsonParser.parseString(response).asJsonArray
            if (array.size() == 0) {
                return@withContext Result.failure(Exception("User not found"))
            }

            val profile = array[0].asJsonObject
            val hasPurchaseToken = profile.has("purchase_token") &&
                    !profile.get("purchase_token").isJsonNull
            Result.success(hasPurchaseToken)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify supporter status", e)
            Result.failure(e)
        }
    }

    /**
     * Download post data by ID.
     * Returns the post title, data JSON, and author UID.
     */
    suspend fun getPost(postId: Long): Result<PostImportData> = withContext(Dispatchers.IO) {
        try {
            val url = "$supabaseUrl/rest/v1/posts?id=eq.$postId&select=id,title,data,user_uid"
            val response = makeGetRequest(url)

            if (response == null) {
                return@withContext Result.failure(Exception("Network error"))
            }

            val array = JsonParser.parseString(response).asJsonArray
            if (array.size() == 0) {
                return@withContext Result.failure(Exception("Post not found"))
            }

            val post = array[0].asJsonObject
            val data = PostImportData(
                id = post.get("id").asLong,
                title = post.get("title")?.asString ?: "Untitled",
                dataJson = post.get("data")?.asString ?: "",
                userUid = post.get("user_uid")?.asString
            )
            Result.success(data)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download post $postId", e)
            Result.failure(e)
        }
    }

    /**
     * Get the display name or handle of a user.
     */
    suspend fun getUserDisplayName(uid: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$supabaseUrl/rest/v1/profiles?uid=eq.$uid&select=handle,display_name"
            val response = makeGetRequest(url) ?: return@withContext "Unknown"

            val array = JsonParser.parseString(response).asJsonArray
            if (array.size() == 0) return@withContext "Unknown"

            val profile = array[0].asJsonObject
            profile.get("handle")?.asString
                ?: profile.get("display_name")?.asString
                ?: "Unknown"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user display name", e)
            "Unknown"
        }
    }

    private fun makeGetRequest(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("apikey", supabaseKey)
                setRequestProperty("Authorization", "Bearer $supabaseKey")
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().readText()
            } else {
                Log.e(TAG, "HTTP ${connection.responseCode}: ${connection.responseMessage}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: $urlString", e)
            null
        } finally {
            connection?.disconnect()
        }
    }
}

data class PostImportData(
    val id: Long,
    val title: String,
    val dataJson: String,
    val userUid: String?
)
