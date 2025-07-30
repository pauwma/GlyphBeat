package com.pauwma.glyphbeat.sound

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MediaNotificationListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "MediaNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // We don't actually need to process notifications
        // This service just needs to exist for notification access permission
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We don't actually need to process notifications
        // This service just needs to exist for notification access permission
        super.onNotificationRemoved(sbn)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(LOG_TAG, "NotificationListenerService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(LOG_TAG, "NotificationListenerService disconnected")
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            Log.d(LOG_TAG, "MediaNotificationListenerService destroyed")
        } catch (e: IllegalArgumentException) {
            Log.w(LOG_TAG, "Service already unbound during destroy", e)
        }
    }

    private companion object {
        private val LOG_TAG = MediaNotificationListenerService::class.java.simpleName
    }
}