package com.pauwma.glyphbeat.services.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.PowerManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Shake detector component for detecting device shake gestures.
 * Can be integrated into services to trigger actions on shake events.
 */
class ShakeDetector(private val context: Context) {
    
    companion object {
        private const val LOG_TAG = "ShakeDetector"
        
        // Shake threshold levels - Lower values = more sensitive
        // Increased gaps for more notable differences
        const val SENSITIVITY_LOW = 20f      // Requires strong shake (was 15f)
        const val SENSITIVITY_MEDIUM = 12f   // Moderate shake (unchanged)
        const val SENSITIVITY_HIGH = 7f      // Light shake (was 9f)
        
        // Default time between shakes to prevent multiple triggers
        private const val DEFAULT_SHAKE_COOLDOWN_MS = 2000L
        
        // Vibration settings
        private const val VIBRATION_DURATION_MS = 1000L // Increased for better perception
        private const val VIBRATION_AMPLITUDE = 250 // Higher amplitude for stronger vibration (0-255)
    }
    
    interface OnShakeListener {
        fun onShake(force: Float)
    }
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeListener: OnShakeListener? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Shake detection variables
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeThreshold = SENSITIVITY_MEDIUM
    private var lastShakeTime = 0L
    private var shakeCooldownMs = DEFAULT_SHAKE_COOLDOWN_MS
    
    // State
    private var isListening = false
    
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            
            // Get x, y, z values
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            
            // Store previous acceleration
            lastAcceleration = currentAcceleration
            
            // Calculate current acceleration (remove gravity)
            currentAcceleration = sqrt(x * x + y * y + z * z)
            
            // Calculate delta
            val delta = currentAcceleration - lastAcceleration
            
            // Low-pass filter to remove gravity and noise
            acceleration = acceleration * 0.9f + delta
            
            // Check if acceleration exceeds threshold
            if (acceleration > shakeThreshold) {
                val currentTime = System.currentTimeMillis()
                
                // Check cooldown to prevent multiple triggers
                if (currentTime - lastShakeTime > shakeCooldownMs) {
                    lastShakeTime = currentTime
                    Log.d(LOG_TAG, "Shake detected! Force: $acceleration, Threshold: $shakeThreshold")
                    shakeListener?.onShake(acceleration)
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Not used
        }
    }
    
    /**
     * Initialize the shake detector with the sensor manager
     */
    fun initialize() {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            
            // Get vibrator service with proper constant
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            // Log vibrator initialization status
            if (vibrator != null) {
                Log.i(LOG_TAG, "Vibrator service initialized successfully")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.i(LOG_TAG, "Vibrator has amplitude control: ${vibrator?.hasAmplitudeControl()}")
                }
            } else {
                Log.e(LOG_TAG, "Failed to initialize vibrator service - vibrator is null")
            }
            
            // Initialize wake lock for sensor operation when screen is locked
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "GlyphBeat:ShakeDetector"
            )
            Log.d(LOG_TAG, "Wake lock initialized for locked screen operation")
            
            if (accelerometer == null) {
                Log.w(LOG_TAG, "Accelerometer not available on this device")
            } else {
                Log.d(LOG_TAG, "Shake detector initialized")
            }
            
            // Initialize acceleration values
            acceleration = 10f
            currentAcceleration = SensorManager.GRAVITY_EARTH
            lastAcceleration = SensorManager.GRAVITY_EARTH
        }
    }
    
    /**
     * Start listening for shake events
     */
    fun startListening() {
        if (isListening) {
            Log.d(LOG_TAG, "Already listening for shakes")
            return
        }
        
        if (sensorManager == null) {
            initialize()
        }
        
        accelerometer?.let { sensor ->
            // Acquire wake lock to keep CPU awake for sensor events
            try {
                wakeLock?.acquire(10*60*1000L) // 10 minutes timeout as safety measure
                Log.d(LOG_TAG, "Wake lock acquired for shake detection")
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to acquire wake lock: ${e.message}")
            }
            
            // Register listener with faster update rate for better responsiveness
            sensorManager?.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_UI // Faster than NORMAL for better responsiveness
            )
            isListening = true
            Log.d(LOG_TAG, "Started listening for shakes with UI delay rate")
        } ?: run {
            Log.w(LOG_TAG, "Cannot start listening - accelerometer not available")
        }
    }
    
    /**
     * Stop listening for shake events
     */
    fun stopListening() {
        if (isListening) {
            sensorManager?.unregisterListener(sensorEventListener)
            isListening = false
            
            // Release wake lock when stopping
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                    Log.d(LOG_TAG, "Wake lock released")
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error releasing wake lock: ${e.message}")
            }
            
            Log.d(LOG_TAG, "Stopped listening for shakes")
        }
    }
    
    /**
     * Set the shake detection sensitivity
     * @param sensitivity One of SENSITIVITY_LOW, SENSITIVITY_MEDIUM, or SENSITIVITY_HIGH
     */
    fun setSensitivity(sensitivity: Float) {
        shakeThreshold = sensitivity
        Log.d(LOG_TAG, "Shake sensitivity set to: $sensitivity")
    }
    
    /**
     * Set the cooldown time between shake detections
     * @param cooldownMs Time in milliseconds to wait before allowing another shake
     */
    fun setCooldown(cooldownMs: Long) {
        shakeCooldownMs = cooldownMs
        Log.d(LOG_TAG, "Shake cooldown set to: ${cooldownMs}ms")
    }
    
    /**
     * Set the shake event listener
     */
    fun setOnShakeListener(listener: OnShakeListener?) {
        shakeListener = listener
    }
    
    /**
     * Provide haptic feedback for shake detection
     * Called when a valid shake is detected and processed
     */
    fun provideHapticFeedback() {
        Log.d(LOG_TAG, "provideHapticFeedback() called")
        
        if (vibrator == null) {
            Log.e(LOG_TAG, "Cannot provide haptic feedback - vibrator is null")
            return
        }
        
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android Q+ - Use predefined effects that should work on all devices
                    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    vib.vibrate(effect)
                    Log.i(LOG_TAG, "Haptic feedback triggered using EFFECT_CLICK predefined effect")
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android O+ - Use simple vibration pattern
                    val timings = longArrayOf(0, 100, 50, 100) // Pattern: wait 0ms, vibrate 100ms, wait 50ms, vibrate 100ms
                    val amplitudes = intArrayOf(0, 255, 0, 200) // Corresponding amplitudes
                    
                    val effect = if (vib.hasAmplitudeControl()) {
                        VibrationEffect.createWaveform(timings, amplitudes, -1)
                    } else {
                        VibrationEffect.createWaveform(timings, -1)
                    }
                    vib.vibrate(effect)
                    Log.i(LOG_TAG, "Haptic feedback triggered using waveform pattern")
                } else {
                    // Legacy devices - Use simple pattern
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 100, 50, 100)
                    vib.vibrate(pattern, -1)
                    Log.i(LOG_TAG, "Haptic feedback triggered (legacy pattern)")
                }
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to provide haptic feedback: ${e.message}", e)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        shakeListener = null
        sensorManager = null
        accelerometer = null
        vibrator = null
        
        // Clean up wake lock
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error releasing wake lock during cleanup: ${e.message}")
        }
        wakeLock = null
        
        Log.d(LOG_TAG, "Shake detector cleaned up")
    }
    
    /**
     * Check if shake detection is currently active
     */
    fun isActive(): Boolean = isListening
}