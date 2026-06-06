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
    private val SENSITIVITY = 45f // Scale factor to turn radians/sec into pixels
    private val NOISE_THRESHOLD = 0.02f // Filter out tiny wrist shakes but keep fine control responsive

    // Exponential smoothing (low-pass filter) to remove sensor jitter/stutter
    private val SMOOTHING_FACTOR = 0.5f
    private var smoothDx = 0f
    private var smoothDy = 0f

    fun start() {
        if (isRunning) return
        if (gyroscope == null) {
            Log.e(TAG, "Device does not have a Gyroscope sensor!")
            return
        }

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME // Game latency is smooth and doesn't require high sampling rate permission
        )
        displayManager.registerDisplayListener(displayListener, null)
        updateRotation()
        isRunning = true
        Log.d(TAG, "Gyroscope sensor listener registered")
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

        // Apply noise filter and scaling
        val targetDx = if (abs(rawDx) > NOISE_THRESHOLD) rawDx * SENSITIVITY else 0f
        val targetDy = if (abs(rawDy) > NOISE_THRESHOLD) rawDy * SENSITIVITY else 0f

        // Apply exponential smoothing only when active to avoid sluggish stops
        smoothDx = if (targetDx == 0f) 0f else smoothDx + SMOOTHING_FACTOR * (targetDx - smoothDx)
        smoothDy = if (targetDy == 0f) 0f else smoothDy + SMOOTHING_FACTOR * (targetDy - smoothDy)

        if (smoothDx != 0f || smoothDy != 0f) {
            onCursorMove(smoothDx, smoothDy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
