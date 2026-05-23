package com.teleport.app.mobile.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs

class GyroSensorTracker(
    context: Context,
    private val onCursorMove: (dx: Float, dy: Float) -> Unit
) : SensorEventListener {

    private val TAG = "GyroSensorTracker"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isRunning = false

    // Tuning constants
    private val SENSITIVITY = 45f // Scale factor to turn radians/sec into pixels
    private val NOISE_THRESHOLD = 0.05f // Filter out tiny wrist shakes

    fun start() {
        if (isRunning) return
        if (gyroscope == null) {
            Log.e(TAG, "Device does not have a Gyroscope sensor!")
            return
        }

        sensorManager.registerListener(
            this,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME // Game latency is ideal for real-time controllers
        )
        isRunning = true
        Log.d(TAG, "Gyroscope sensor listener registered")
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        Log.d(TAG, "Gyroscope sensor listener unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val pitch = event.values[0]
        val yaw = event.values[1]

        val dx = if (abs(yaw) > NOISE_THRESHOLD) -yaw * SENSITIVITY else 0f
        val dy = if (abs(pitch) > NOISE_THRESHOLD) pitch * SENSITIVITY else 0f

        if (dx != 0f || dy != 0f) {
            onCursorMove(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
}
