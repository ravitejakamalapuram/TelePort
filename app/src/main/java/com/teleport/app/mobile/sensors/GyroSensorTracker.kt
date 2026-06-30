package com.teleport.app.mobile.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import kotlin.math.abs

class GyroSensorTracker(
    private val context: Context,
    private val onCursorMove: (dx: Float, dy: Float) -> Unit
) : SensorEventListener {

    private val TAG = "GyroSensorTracker"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager

    private var isRunning = false
    private var cachedRotation = Surface.ROTATION_0

    private val displayListener = object : android.hardware.display.DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            updateRotation()
        }
    }

    private fun updateRotation() {
        cachedRotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display?.rotation ?: @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
    }

    // Tuning constants
    private val NOISE_THRESHOLD = 0.008f // Low threshold to capture subtle wrist movement but filter out static noise
    private val EMIT_INTERVAL_MS = 16L // ~60Hz

    private var smoothDx = 0f
    private var smoothDy = 0f

    // Bolt: Variables to accumulate high-frequency raw changes and throttle emitting
    // to prevent GC pressure, thread pool contention, and WebSocket flooding.
    private var accumulatedDx = 0f
    private var accumulatedDy = 0f
    private var lastEmitTime = 0L

    fun start() {
        if (isRunning) return
        if (gyroscope == null) {
            Log.e(TAG, "Device does not have a Gyroscope sensor!")
            return
        }

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_FASTEST // Sample at maximum hardware frequency (100Hz - 200Hz) matching physical air mouses
        )
        displayManager.registerDisplayListener(displayListener, null)
        updateRotation()
        isRunning = true
        Log.d(TAG, "Gyroscope sensor listener registered (DELAY_FASTEST)")
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        displayManager.unregisterDisplayListener(displayListener)
        isRunning = false
        Log.d(TAG, "Gyroscope sensor listener unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        // event.values[0] is X-axis (pitch speed)
        // event.values[1] is Y-axis (roll speed)
        // event.values[2] is Z-axis (yaw speed)
        val pitchVal = event.values[0]
        val rollVal = event.values[1]

        // Bolt: Use cached display rotation to avoid expensive IPC calls in high-frequency sensor callback
        val rotation = cachedRotation

        // Map sensor axes based on screen orientation
        var rawDx = 0f
        var rawDy = 0f

        when (rotation) {
            Surface.ROTATION_0 -> {
                // Portrait (default): steering roll (rollVal) controls X, tilting forward-back (pitchVal) controls Y
                rawDx = -rollVal
                rawDy = -pitchVal // Negated to correct reversed up/down direction
            }
            Surface.ROTATION_90 -> {
                // Landscape Left (rotated 90 counter-clockwise):
                // Pitch becomes horizontal movement, Roll becomes vertical movement
                rawDx = -pitchVal
                rawDy = rollVal
            }
            Surface.ROTATION_180 -> {
                // Reverse Portrait:
                rawDx = rollVal
                rawDy = pitchVal
            }
            Surface.ROTATION_270 -> {
                // Landscape Right (rotated 90 clockwise):
                rawDx = pitchVal
                rawDy = -rollVal
            }
        }

        val speed = kotlin.math.sqrt(rawDx * rawDx + rawDy * rawDy)
        // Dynamic sensitivity scaling based on angular speed (Air Mouse Acceleration)
        val sensitivity = when {
            speed < 0.05f -> 18f // Highly precise slow movement
            speed < 0.2f -> 36f  // Standard speed
            else -> 60f + (speed - 0.2f) * 120f // Fast wrist flick sends cursor far
        }

        // Apply noise filter and scaling
        val targetDx = if (abs(rawDx) > NOISE_THRESHOLD) rawDx * sensitivity else 0f
        val targetDy = if (abs(rawDy) > NOISE_THRESHOLD) rawDy * sensitivity else 0f

        // Dynamic exponential smoothing:
        // Use higher smoothing factor (closer to 1.0) for fast movement to avoid lag.
        // Use lower smoothing factor (closer to 0.0) for slow movement to filter out muscle shakes.
        val smoothingFactor = if (speed > 0.15f) 0.8f else 0.35f
        smoothDx = if (targetDx == 0f) 0f else smoothDx + smoothingFactor * (targetDx - smoothDx)
        smoothDy = if (targetDy == 0f) 0f else smoothDy + smoothingFactor * (targetDy - smoothDy)

        if (smoothDx != 0f || smoothDy != 0f) {
            // Bolt: Accumulate high-frequency (100Hz+) events
            accumulatedDx += smoothDx
            accumulatedDy += smoothDy

            val currentTime = android.os.SystemClock.uptimeMillis()
            // Throttle to ~60Hz to prevent network flooding and jank
            if (currentTime - lastEmitTime >= EMIT_INTERVAL_MS) {
                onCursorMove(accumulatedDx, accumulatedDy)
                accumulatedDx = 0f
                accumulatedDy = 0f
                lastEmitTime = currentTime
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
