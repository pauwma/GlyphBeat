package com.pauwma.glyphbeat.services.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Broadcast receiver that handles system events to manage the auto-start service.
 * Listens for:
 * - Boot completed: Starts the music detection service if auto-start is enabled
 * - App updates: Restarts the service after app updates
 * - Custom intents: For manual control from the app UI
 */
class AutoStartBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val LOG_TAG = "AutoStartReceiver"
        const val ACTION_START_SERVICE = "com.pauwma.glyphbeat.START_AUTO_SERVICE"
        const val ACTION_STOP_SERVICE = "com.pauwma.glyphbeat.STOP_AUTO_SERVICE"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(LOG_TAG, "Received broadcast: ${intent.action}")
        
        val prefs = context.getSharedPreferences("glyph_settings", Context.MODE_PRIVATE)
        val isAutoStartEnabled = prefs.getBoolean("auto_start_enabled", false)
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (isAutoStartEnabled) {
                    Log.i(LOG_TAG, "Boot completed - starting music detection service")
                    MusicDetectionService.start(context)
                } else {
                    Log.d(LOG_TAG, "Boot completed - auto-start is disabled")
                }
            }
            
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // App was updated
                if (isAutoStartEnabled) {
                    Log.i(LOG_TAG, "App updated - restarting music detection service")
                    MusicDetectionService.start(context)
                }
            }
            
            ACTION_START_SERVICE -> {
                Log.i(LOG_TAG, "Manual start requested")
                MusicDetectionService.start(context)
            }
            
            ACTION_STOP_SERVICE -> {
                Log.i(LOG_TAG, "Manual stop requested")
                MusicDetectionService.stop(context)
            }
        }
    }
}