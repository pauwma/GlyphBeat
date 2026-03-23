package com.pauwma.glyphbeat.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy
import com.pauwma.glyphbeat.utils.DebugLogger

abstract class GlyphMatrixService(private val tag: String) : Service() {

    private val buttonPressedHandler = object : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.let { data ->
                        if (data.containsKey(KEY_DATA)) {
                            data.getString(KEY_DATA)?.let { value ->
                                when (value) {
                                    GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                                    GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                                    GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                                }
                            }
                        }
                    }
                }

                else -> {
                    Log.d(LOG_TAG, "Message: ${msg.what}")
                    super.handleMessage(msg)
                }
            }
        }
    }

    private val serviceMessenger = Messenger(buttonPressedHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    // Track active binding sources so we only tear down GMM when ALL bindings are gone.
    // Android calls onBind/onUnbind per unique intent type:
    //   - Toy system binds with "com.nothing.glyph.TOY" action intent
    //   - MusicDetectionService binds with component intent (programmatic)
    // Without tracking, the toy's onUnbind destroys the shared GMM singleton,
    // killing the programmatic animation that's still running.
    private val activeBindings = mutableSetOf<String>()

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(p0: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                Log.d(LOG_TAG, "$tag: onServiceConnected")
                DebugLogger.log("$tag: SDK onServiceConnected")
                gmm.register(com.pauwma.glyphbeat.core.DeviceManager.getSdkDeviceTarget())
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.w(LOG_TAG, "$tag: SDK onServiceDisconnected - Glyph Matrix system service disconnected unexpectedly")
            DebugLogger.log("$tag: SDK onServiceDisconnected (unexpected)")
        }
    }

    private fun getBindingKey(intent: Intent?): String {
        // Distinguish toy system binding from programmatic binding
        return intent?.action ?: intent?.component?.className ?: "unknown"
    }

    final override fun startService(intent: Intent?): ComponentName? {
        Log.d(LOG_TAG, "$tag: startService")
        return super.startService(intent)
    }

    final override fun onBind(intent: Intent?): IBinder? {
        val bindingKey = getBindingKey(intent)
        activeBindings.add(bindingKey)
        Log.d(LOG_TAG, "$tag: onBind (source: $bindingKey, active bindings: ${activeBindings.size})")
        DebugLogger.log("$tag: onBind (source: $bindingKey, total: ${activeBindings.size})")

        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
            Log.d(LOG_TAG, "$tag: onBind completed")
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        val bindingKey = getBindingKey(intent)
        activeBindings.remove(bindingKey)
        Log.d(LOG_TAG, "$tag: onUnbind (source: $bindingKey, remaining bindings: ${activeBindings.size})")
        DebugLogger.log("$tag: onUnbind (source: $bindingKey, remaining: ${activeBindings.size})")

        if (activeBindings.isEmpty()) {
            // Last binding gone — fully tear down
            Log.d(LOG_TAG, "$tag: Last binding released, tearing down GlyphMatrixManager")

            // Notify MusicDetectionService that this service is fully stopping
            val stopIntent = Intent("com.pauwma.glyphbeat.SERVICE_STOPPED")
            stopIntent.putExtra("service_name", tag)
            sendBroadcast(stopIntent)
            Log.d(LOG_TAG, "$tag: Sent service stopped broadcast")

            glyphMatrixManager?.let {
                Log.d(LOG_TAG, "$tag: onServiceDisconnected")
                performOnServiceDisconnected(applicationContext)
            }
            glyphMatrixManager?.turnOff()
            glyphMatrixManager?.unInit()
            glyphMatrixManager = null
        } else {
            Log.d(LOG_TAG, "$tag: Other bindings still active ($activeBindings), keeping GlyphMatrixManager alive")
            DebugLogger.log("$tag: Toy unbound but programmatic binding still active, GMM preserved")
        }

        return false
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}

    open fun performOnServiceDisconnected(context: Context) {}

    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}

    private companion object {
        private val LOG_TAG = GlyphMatrixService::class.java.simpleName
        private const val KEY_DATA = "data"
    }

}