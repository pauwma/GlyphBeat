package com.pauwma.glyphbeat.services.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.sqrt

/**
 * Shake detector component for detecting device shake gestures.
 * Can be integrated into services to trigger actions on shake events.
 */
class ShakeDetector(private val context: Context) {
    
    companion object {
        private const val LOG_TAG = "ShakeDetector"
        
        // Shake threshold levels
        const val SENSITIVITY_LOW = 15f
        const val SENSITIVITY_MEDIUM = 12f
        const val SENSITIVITY_HIGH = 9f
        
        // Time between shakes to prevent multiple triggers
        private const val SHAKE_COOLDOWN_MS = 1000L
    }
    
    interface OnShakeListener {
        fun onShake(force: Float)
    }
    
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeListener: OnShakeListener? = null
    
    // Shake detection variables
    private var acceleration = 0f
    private var currentAcceleration = 0f
    private var lastAcceleration = 0f
    private var shakeThreshold = SENSITIVITY_MEDIUM
    private var lastShakeTime = 0L
    
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
                if (currentTime - lastShakeTime > SHAKE_COOLDOWN_MS) {
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
            sensorManager?.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isListening = true
            Log.d(LOG_TAG, "Started listening for shakes")
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
     * Set the shake event listener
     */
    fun setOnShakeListener(listener: OnShakeListener?) {
        shakeListener = listener
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopListening()
        shakeListener = null
        sensorManager = null
        accelerometer = null
        Log.d(LOG_TAG, "Shake detector cleaned up")
    }
    
    /**
     * Check if shake detection is currently active
     */
    fun isActive(): Boolean = isListening
}