package com.pauwma.glyphbeat.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.pauwma.glyphbeat.core.AppConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {

    private const val TAG = "DebugLogger"
    private const val LOG_FILE = "glyph_beat_debug.log"
    private const val MAX_LINES = 1000

    private var logFile: File? = null

    fun init(context: Context) {
        val logsDir = File(context.cacheDir, "logs")
        logsDir.mkdirs()
        logFile = File(logsDir, LOG_FILE)

        log("--- Session started ---")
        log("App: ${AppConfig.APP_VERSION}")
        log("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        log("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
    }

    fun log(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "[$timestamp] $message"

        try {
            logFile?.appendText("$line\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }

    fun shareLog(context: Context) {
        val file = logFile ?: return

        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "No log file to share")
            return
        }

        try {
            trimIfNeeded()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GlyphBeat Debug Log - ${AppConfig.APP_VERSION}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Send Debug Log"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share log", e)
        }
    }

    private fun trimIfNeeded() {
        val file = logFile ?: return
        try {
            val lines = file.readLines()
            if (lines.size > MAX_LINES) {
                val trimmed = lines.takeLast(MAX_LINES)
                file.writeText(trimmed.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trim log", e)
        }
    }
}
