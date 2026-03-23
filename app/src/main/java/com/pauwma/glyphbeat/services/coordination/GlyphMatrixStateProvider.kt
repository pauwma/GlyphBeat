package com.pauwma.glyphbeat.services.coordination

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log

/**
 * Lightweight ContentProvider that exposes GlyphBeat's matrix rendering state
 * to other apps (primarily Glyph Museum) for cross-app coordination.
 *
 * State is stored in-memory only — when the process dies, state resets to inactive,
 * which is the correct behavior (no stale claims survive process death).
 *
 * Authority: com.pauwma.glyphbeat.matrixstate
 * URI: content://com.pauwma.glyphbeat.matrixstate/state
 * Permission: com.pauwma.glyphbeat.permission.READ_MATRIX_STATE (signature-level)
 */
class GlyphMatrixStateProvider : ContentProvider() {

    companion object {
        private const val TAG = "GlyphMatrixState"
        const val AUTHORITY = "com.pauwma.glyphbeat.matrixstate"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/state")

        // Column names
        const val COL_IS_ACTIVE = "is_active"
        const val COL_OWNER = "owner"
        const val COL_SINCE = "since"

        private val COLUMNS = arrayOf(COL_IS_ACTIVE, COL_OWNER, COL_SINCE)

        // In-memory state (no database needed)
        private val lock = Any()
        @Volatile private var isActive = false
        @Volatile private var owner = ""
        @Volatile private var since = 0L

        // Pause flag: set synchronously by external apps via update(),
        // checked by the animation loop every frame for instant cutoff.
        @Volatile var isPauseRequested = false
            private set

        /**
         * Mark the matrix as actively being rendered by GlyphBeat.
         * Called from MediaPlayerToyService when animation starts.
         */
        fun setActive(owner: String) {
            synchronized(lock) {
                this.isActive = true
                this.owner = owner
                this.since = System.currentTimeMillis()
            }
            Log.d(TAG, "Matrix state: ACTIVE (owner=$owner)")
        }

        /**
         * Mark the matrix as no longer being rendered.
         * Called when animation stops or service disconnects.
         */
        fun setInactive() {
            synchronized(lock) {
                this.isActive = false
                this.owner = ""
                this.since = 0L
            }
            Log.d(TAG, "Matrix state: INACTIVE")
        }

        /**
         * Refresh the timestamp to prevent stale detection.
         * Called periodically from the animation loop.
         */
        fun refreshTimestamp() {
            synchronized(lock) {
                if (isActive) {
                    this.since = System.currentTimeMillis()
                }
            }
        }

        /**
         * Check if the matrix is active (for internal GlyphBeat use).
         */
        fun isCurrentlyActive(): Boolean = isActive

        /**
         * Request a pause (called internally by non-MediaPlayer toy services).
         */
        fun requestPause() {
            isPauseRequested = true
        }

        /**
         * Clear the pause request (called by MediaPlayerToyService on RESUME or disconnect).
         */
        fun clearPauseRequest() {
            isPauseRequested = false
        }
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "GlyphMatrixStateProvider created")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(COLUMNS)
        synchronized(lock) {
            cursor.addRow(arrayOf(
                if (isActive) 1 else 0,
                owner,
                since
            ))
        }
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.item/vnd.$AUTHORITY.state"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    /**
     * External apps can request a pause via update().
     * This sets a volatile flag checked by the animation loop every frame,
     * providing instant cutoff without broadcast delay.
     *
     * ContentValues: { "pause_requested": 1 } to pause, { "pause_requested": 0 } to resume.
     */
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val pauseValue = values?.getAsInteger("pause_requested") ?: return 0
        isPauseRequested = pauseValue == 1
        Log.d(TAG, "Pause requested via ContentProvider: $isPauseRequested")
        return 1
    }
}
