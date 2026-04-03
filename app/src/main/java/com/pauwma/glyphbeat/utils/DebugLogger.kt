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
    private const val COMBINED_LOG_FILE = "glyph_beat_full_log.log"
    private const val MAX_LINES = 1000
    private const val LOGCAT_LINES = 3000

    private var logFile: File? = null
    private var logsDir: File? = null

    fun init(context: Context) {
        val dir = File(context.cacheDir, "logs")
        dir.mkdirs()
        logsDir = dir
        logFile = File(dir, LOG_FILE)

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
        try {
            val dir = logsDir ?: return
            val combinedFile = File(dir, COMBINED_LOG_FILE)

            val appLog = buildAppLogSection()
            val logcatOutput = captureLogcat()

            combinedFile.writeText(buildString {
                appendLine("═══════════════════════════════════════════")
                appendLine("  GlyphBeat Debug Log - ${AppConfig.APP_VERSION}")
                appendLine("  Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                appendLine("  Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
                appendLine("  Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                appendLine("═══════════════════════════════════════════")
                appendLine()
                appendLine("──────────── APP EVENTS ────────────")
                appendLine()
                append(appLog)
                appendLine()
                appendLine("──────────── LOGCAT ────────────")
                appendLine()
                append(logcatOutput)
            })

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                combinedFile
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

    private fun buildAppLogSection(): String {
        val file = logFile ?: return "(no app log file)\n"
        if (!file.exists() || file.length() == 0L) return "(empty app log)\n"

        val lines = file.readLines()
        val trimmed = if (lines.size > MAX_LINES) lines.takeLast(MAX_LINES) else lines
        return trimmed.joinToString("\n") + "\n"
    }

    private fun captureLogcat(): String {
        return try {
            val pid = android.os.Process.myPid()
            val process = Runtime.getRuntime().exec(
                arrayOf("logcat", "-d", "-t", LOGCAT_LINES.toString(), "--pid=$pid")
            )
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            if (output.isBlank()) "(logcat empty for pid $pid)\n" else output
        } catch (e: Exception) {
            "(failed to capture logcat: ${e.message})\n"
        }
    }
}
