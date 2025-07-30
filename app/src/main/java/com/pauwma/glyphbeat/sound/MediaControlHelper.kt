package com.pauwma.glyphbeat.sound

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.pauwma.glyphbeat.sound.MediaNotificationListenerService

class MediaControlHelper(private val context: Context) {
    
    private val mediaSessionManager: MediaSessionManager by lazy {
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }
    
    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    
    private var activeController: MediaController? = null
    private var lastLoggedSessionCount = -1
    private var lastLoggedActivePackage: String? = null
    private var lastLoggedActiveState: Int? = null
    private var lastLoggedSessions = mutableMapOf<String, Int>()
    
    fun getActiveMediaController(): MediaController? {
        try {
            // Pass our NotificationListenerService component to get proper permissions
            val componentName = ComponentName(context, MediaNotificationListenerService::class.java)
            val activeSessions = mediaSessionManager.getActiveSessions(componentName)
            
            // Only log session count when it changes
            if (activeSessions.size != lastLoggedSessionCount) {
                Log.d(LOG_TAG, "Found ${activeSessions.size} active sessions")
                lastLoggedSessionCount = activeSessions.size
            }
            
            // Log session states only when they change
            activeSessions.forEach { controller ->
                val packageName = controller.packageName
                val state = controller.playbackState?.state
                val lastState = lastLoggedSessions[packageName]
                
                if (state != lastState) {
                    Log.d(LOG_TAG, "Session $packageName: state changed $lastState -> $state")
                    lastLoggedSessions[packageName] = state ?: -1
                }
            }
            
            // First try to find actively playing sessions
            activeController = activeSessions.firstOrNull { controller ->
                val state = controller.playbackState?.state
                state == PlaybackState.STATE_PLAYING
            }
            
            // If no playing sessions, look for recently active ones (paused/buffering with recent activity)
            if (activeController == null) {
                activeController = activeSessions.firstOrNull { controller ->
                    val state = controller.playbackState?.state
                    val hasMetadata = controller.metadata != null
                    val hasValidTitle = controller.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.isNotBlank() == true
                    
                    // Only consider paused/buffering sessions that have actual media content
                    (state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_BUFFERING) && 
                    hasMetadata && hasValidTitle
                }
            }
            
            val currentPackage = activeController?.packageName
            val currentState = activeController?.playbackState?.state
            
            // Only log active controller when package or state changes
            if (currentPackage != lastLoggedActivePackage || currentState != lastLoggedActiveState) {
                Log.d(LOG_TAG, "Active controller: $currentPackage, state: $currentState")
                lastLoggedActivePackage = currentPackage
                lastLoggedActiveState = currentState
            }
            
            return activeController
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Media control requires notification access permission - please grant notification access in settings")
            return null
        } catch (e: IllegalStateException) {
            Log.w(LOG_TAG, "Media session service not available - notification listener may not be connected")
            return null
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting media sessions: ${e.message}", e)
            return null
        }
    }
    
    fun isPlaying(): Boolean {
        val controller = getActiveMediaController()
        val state = controller?.playbackState?.state
        val playing = state == PlaybackState.STATE_PLAYING
        Log.d(LOG_TAG, "isPlaying check: state=$state, playing=$playing, package=${controller?.packageName}")
        return playing
    }
    
    fun togglePlayPause(): Boolean {
        val controller = getActiveMediaController()
        
        if (controller == null) {
            Log.w(LOG_TAG, "No active media controller found - trying fallback method")
            return sendMediaKeyEvent()
        }
        
        return try {
            val currentState = controller.playbackState?.state
            Log.d(LOG_TAG, "Current playback state: $currentState")
            
            when (currentState) {
                PlaybackState.STATE_PLAYING -> {
                    controller.transportControls.pause()
                    Log.d(LOG_TAG, "Sent pause command")
                    true
                }
                PlaybackState.STATE_PAUSED -> {
                    controller.transportControls.play()
                    Log.d(LOG_TAG, "Sent play command")
                    true
                }
                else -> {
                    controller.transportControls.play()
                    Log.d(LOG_TAG, "Sent play command (unknown state: $currentState)")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error toggling play/pause with MediaController, trying fallback", e)
            return sendMediaKeyEvent()
        }
    }
    
    private fun sendMediaKeyEvent(): Boolean {
        return try {
            val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            val downEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
            
            audioManager.dispatchMediaKeyEvent(downEvent)
            audioManager.dispatchMediaKeyEvent(upEvent)
            
            Log.d(LOG_TAG, "Sent media key event (play/pause)")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error sending media key event", e)
            false
        }
    }
    
    fun skipToNext(): Boolean {
        val controller = getActiveMediaController() ?: return false
        return try {
            controller.transportControls.skipToNext()
            Log.d(LOG_TAG, "Sent skip next command")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error skipping to next", e)
            false
        }
    }
    
    fun skipToPrevious(): Boolean {
        val controller = getActiveMediaController() ?: return false
        return try {
            controller.transportControls.skipToPrevious()
            Log.d(LOG_TAG, "Sent skip previous command")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error skipping to previous", e)
            false
        }
    }
    
    fun getCurrentVolume(): Int {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
    
    fun getMaxVolume(): Int {
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    
    fun setVolume(volume: Int): Boolean {
        return try {
            val clampedVolume = volume.coerceIn(0, getMaxVolume())
            audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                clampedVolume,
                AudioManager.FLAG_SHOW_UI
            )
            Log.d(LOG_TAG, "Set volume to $clampedVolume")
            true
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error setting volume", e)
            false
        }
    }
    
    fun increaseVolume(): Boolean {
        val currentVolume = getCurrentVolume()
        val maxVolume = getMaxVolume()
        return if (currentVolume < maxVolume) {
            setVolume(currentVolume + 1)
        } else false
    }
    
    fun decreaseVolume(): Boolean {
        val currentVolume = getCurrentVolume()
        return if (currentVolume > 0) {
            setVolume(currentVolume - 1)
        } else false
    }
    
    fun getTrackInfo(): TrackInfo? {
        val controller = getActiveMediaController() ?: return null
        val metadata = controller.metadata ?: return null
        
        return try {
            TrackInfo(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown",
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown",
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown",
                appName = controller.packageName ?: "Unknown"
            )
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting track info", e)
            null
        }
    }
    
    data class TrackInfo(
        val title: String,
        val artist: String,
        val album: String,
        val appName: String
    )
    
    private companion object {
        private val LOG_TAG = MediaControlHelper::class.java.simpleName
    }
}