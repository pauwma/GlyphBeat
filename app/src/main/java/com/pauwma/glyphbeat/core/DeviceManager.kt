package com.pauwma.glyphbeat.core

import android.content.Context
import android.os.Build
import android.util.Log
import com.nothing.ketchum.Common

/**
 * Singleton for detecting and caching the device's Glyph Matrix resolution.
 * Auto-detects at first launch and caches in SharedPreferences.
 * The result never changes since it's a hardware property.
 */
object DeviceManager {

    private const val TAG = "DeviceManager"
    private const val PREFS_NAME = "device_manager"
    private const val KEY_RESOLUTION = "glyph_resolution"
    private const val KEY_DETECTION_VERSION = "detection_version"
    private const val CURRENT_DETECTION_VERSION = 2

    private var _resolution: GlyphResolution? = null

    // TODO: Remove after testing — forces Phone 4a Pro resolution on any device
    private val DEBUG_FORCE_PHONE_4A = false

    val resolution: GlyphResolution
        get() = if (DEBUG_FORCE_PHONE_4A) GlyphResolution.PHONE_4A else (_resolution ?: GlyphResolution.PHONE_3)

    /** The actual hardware resolution — always the real device, ignoring debug flags. */
    val hardwareResolution: GlyphResolution
        get() = _resolution ?: GlyphResolution.PHONE_3

    /** True when debug mode is simulating a different resolution than the hardware. */
    val isDebugResolutionOverride: Boolean
        get() = DEBUG_FORCE_PHONE_4A && hardwareResolution != resolution

    val isPhone4aPro: Boolean
        get() = resolution == GlyphResolution.PHONE_4A

    val isPhone3: Boolean
        get() = resolution == GlyphResolution.PHONE_3

    /**
     * Initialize device detection. Call once from MainActivity on app start.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cached = prefs.getString(KEY_RESOLUTION, null)
        val cachedVersion = prefs.getInt(KEY_DETECTION_VERSION, 0)

        if (cached != null && cachedVersion >= CURRENT_DETECTION_VERSION) {
            _resolution = GlyphResolution.fromDbValue(cached)
            Log.d(TAG, "Loaded cached resolution: $_resolution")
            return
        }

        // Auto-detect device
        _resolution = detectDevice()
        Log.d(TAG, "Detected device: $_resolution (model=${Build.MODEL}, manufacturer=${Build.MANUFACTURER})")

        // Cache result
        prefs.edit()
            .putString(KEY_RESOLUTION, _resolution!!.dbValue)
            .putInt(KEY_DETECTION_VERSION, CURRENT_DETECTION_VERSION)
            .apply()
    }

    private fun detectDevice(): GlyphResolution {
        // Primary: SDK detection
        try {
            if (Common.is25111p()) return GlyphResolution.PHONE_4A
            if (Common.is23112()) return GlyphResolution.PHONE_3
        } catch (e: Exception) {
            Log.w(TAG, "SDK device detection failed, falling back to Build.MODEL", e)
        }

        // Fallback: Build.MODEL
        val model = Build.MODEL.uppercase()
        return when {
            model.contains("A069P") -> GlyphResolution.PHONE_4A
            model.contains("A024") -> GlyphResolution.PHONE_3
            else -> GlyphResolution.PHONE_3
        }
    }

    /**
     * Get the SDK device target string for GlyphMatrixManager.register().
     */
    fun getSdkDeviceTarget(): String {
        return try {
            when {
                Common.is25111p() -> com.nothing.ketchum.Glyph.DEVICE_25111p
                Common.is23112() -> com.nothing.ketchum.Glyph.DEVICE_23112
                else -> com.nothing.ketchum.Glyph.DEVICE_23112
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get SDK device target", e)
            com.nothing.ketchum.Glyph.DEVICE_23112
        }
    }
}
